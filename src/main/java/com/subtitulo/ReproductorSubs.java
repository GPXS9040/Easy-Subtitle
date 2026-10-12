package com.subtitulo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libc.LibCStdlib;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

final class ReproductorSubs {
    private static final Map<String, SesionSubs> SESIONES = new LinkedHashMap<>();

    private record AudioDecodificado(ShortBuffer pcm, int canales, int frecuencia) {
    }

    private ReproductorSubs() {
    }

    static Collection<SesionSubs> sesiones() {
        return SESIONES.values();
    }

    static void reproducir(MinecraftClient client, String nombre) {
        if (!Subtitulo.nombreValido(nombre)) {
            error(client, "Nombre de carpeta no valido: " + nombre);
            return;
        }
        detener(nombre);
        Path carpeta = Subtitulo.raiz().resolve(nombre);
        Path archivoSubs = buscarSubs(carpeta);
        Path archivoAudio = carpeta.resolve("sound.ogg");
        ArchivoSubs.Subs subs;
        try {
            subs = ArchivoSubs.leer(archivoSubs);
        } catch (NoSuchFileException e) {
            error(client, "Falta sub.json o sub.lrc en " + Subtitulo.CARPETA_RAIZ + "/" + nombre);
            return;
        } catch (Exception e) {
            error(client, "No se pudo leer " + archivoSubs.getFileName() + " de " + nombre + ": " + e.getMessage());
            return;
        }
        if (subs.lineas().isEmpty()) {
            error(client, archivoSubs.getFileName() + " de " + nombre + " no tiene lineas");
            return;
        }
        SesionSubs sesion = new SesionSubs(subs);
        SESIONES.put(nombre, sesion);
        if (!Files.isRegularFile(archivoAudio)) {
            sesion.iniciarSinAudio();
            return;
        }
        CompletableFuture.supplyAsync(() -> decodificar(archivoAudio))
                .whenComplete((audio, fallo) -> client.execute(() -> terminarCarga(client, nombre, sesion, audio, fallo)));
    }

    static void recargar(MinecraftClient client, String nombre) {
        if (!nombre.isEmpty() && !Subtitulo.nombreValido(nombre)) {
            error(client, "Nombre de carpeta no valido: " + nombre);
            return;
        }
        int encontradas = 0;
        int recargadas = 0;
        for (Map.Entry<String, SesionSubs> entrada : SESIONES.entrySet()) {
            String clave = entrada.getKey();
            if (!nombre.isEmpty() && !clave.equals(nombre)) {
                continue;
            }
            encontradas++;
            Path archivoSubs = buscarSubs(Subtitulo.raiz().resolve(clave));
            try {
                ArchivoSubs.Subs subs = ArchivoSubs.leer(archivoSubs);
                if (subs.lineas().isEmpty()) {
                    error(client, archivoSubs.getFileName() + " de " + clave + " no tiene lineas");
                    continue;
                }
                entrada.getValue().reemplazar(subs);
                recargadas++;
            } catch (Exception e) {
                error(client, "No se pudo recargar " + archivoSubs.getFileName() + " de " + clave + ": " + e.getMessage());
            }
        }
        if (encontradas == 0) {
            String detalle = nombre.isEmpty() ? "" : " llamado " + nombre;
            aviso(client, "No hay subtitulos activos" + detalle);
        } else if (recargadas > 0) {
            aviso(client, "Subtitulos recargados (" + recargadas + ")");
        }
    }

    static void saltar(MinecraftClient client, String nombre, double segundos) {
        SesionSubs sesion = SESIONES.get(nombre);
        if (sesion == null) {
            aviso(client, "No hay subtitulos activos llamado " + nombre);
            return;
        }
        sesion.saltar(segundos);
        aviso(client, nombre + ": salto a " + ArchivoSubs.formatearTiempo(segundos));
    }

    static void detener(String nombre) {
        if (nombre == null || nombre.isEmpty()) {
            detenerTodo();
            return;
        }
        SesionSubs sesion = SESIONES.remove(nombre);
        if (sesion != null) {
            sesion.detener();
        }
    }

    static void detenerTodo() {
        for (SesionSubs sesion : SESIONES.values()) {
            sesion.detener();
        }
        SESIONES.clear();
    }

    static void tick(MinecraftClient client) {
        if (SESIONES.isEmpty()) {
            return;
        }
        long ahora = System.currentTimeMillis();
        float volumen = volumen(client);
        boolean pausado = client.isPaused();
        Iterator<SesionSubs> iterador = SESIONES.values().iterator();
        while (iterador.hasNext()) {
            SesionSubs sesion = iterador.next();
            sesion.tick(pausado, volumen, ahora);
            if (sesion.terminada(ahora)) {
                sesion.detener();
                iterador.remove();
            }
        }
    }

    private static Path buscarSubs(Path carpeta) {
        Path json = carpeta.resolve("sub.json");
        if (Files.isRegularFile(json)) {
            return json;
        }
        Path lrc = carpeta.resolve("sub.lrc");
        if (Files.isRegularFile(lrc)) {
            return lrc;
        }
        return json;
    }

    private static void terminarCarga(MinecraftClient client, String nombre, SesionSubs sesion, AudioDecodificado audio, Throwable fallo) {
        boolean vigente = !sesion.cancelada();
        if (fallo != null || audio == null) {
            if (vigente) {
                error(client, "No se pudo cargar sound.ogg de " + nombre + ": " + mensaje(fallo));
                SESIONES.remove(nombre, sesion);
                sesion.detener();
            }
            return;
        }
        if (!vigente) {
            LibCStdlib.free(audio.pcm());
            return;
        }
        try {
            sesion.iniciarConAudio(audio.pcm(), audio.canales(), audio.frecuencia(), volumen(client));
        } catch (RuntimeException e) {
            error(client, "No se pudo reproducir " + nombre + ": " + e.getMessage());
            SESIONES.remove(nombre, sesion);
            sesion.detener();
        }
    }

    private static AudioDecodificado decodificar(Path ruta) {
        try {
            byte[] bytes = Files.readAllBytes(ruta);
            ByteBuffer datos = MemoryUtil.memAlloc(bytes.length);
            try {
                datos.put(bytes).flip();
                try (MemoryStack pila = MemoryStack.stackPush()) {
                    IntBuffer canales = pila.mallocInt(1);
                    IntBuffer frecuencia = pila.mallocInt(1);
                    ShortBuffer pcm = STBVorbis.stb_vorbis_decode_memory(datos, canales, frecuencia);
                    if (pcm == null) {
                        throw new IllegalStateException("archivo ogg no valido");
                    }
                    if (canales.get(0) > 2) {
                        LibCStdlib.free(pcm);
                        throw new IllegalStateException("el audio debe ser mono o estereo");
                    }
                    return new AudioDecodificado(pcm, canales.get(0), frecuencia.get(0));
                }
            } finally {
                MemoryUtil.memFree(datos);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static float volumen(MinecraftClient client) {
        return client.options.getSoundVolume(SoundCategory.MASTER) * client.options.getSoundVolume(SoundCategory.VOICE);
    }

    private static String mensaje(Throwable fallo) {
        if (fallo == null) {
            return "error desconocido";
        }
        Throwable causa = fallo.getCause() != null ? fallo.getCause() : fallo;
        return causa.getMessage() != null ? causa.getMessage() : causa.toString();
    }

    private static void error(MinecraftClient client, String mensaje) {
        Subtitulo.LOGGER.error(mensaje);
        if (client.player != null) {
            client.player.sendMessage(Text.literal("[Subtitulo] " + mensaje).formatted(Formatting.RED), false);
        }
    }

    private static void aviso(MinecraftClient client, String mensaje) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal("[Subtitulo] " + mensaje).formatted(Formatting.GREEN), true);
        }
    }
}