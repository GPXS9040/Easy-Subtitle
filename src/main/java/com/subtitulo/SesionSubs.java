package com.subtitulo;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.system.libc.LibCStdlib;

import java.nio.ShortBuffer;
import java.util.List;

final class SesionSubs {
    private static final double FADE_S = 0.3;
    private static final long FADE_MS = 300;
    private static final long COLA_FINAL_MS = 2000;

    record Vista(String nombre, String texto, int letras, float opacidad, int colorNombre, int colorTexto) {
    }

    private enum Estado {
        CARGANDO,
        ACTIVA,
        FINALIZADA
    }

    private List<ArchivoSubs.Linea> lineas;
    private ArchivoSubs.Opciones opciones;
    private Estado estado = Estado.CARGANDO;
    private boolean cancelada;
    private boolean enPausa;
    private int fuente;
    private int buffer;
    private double duracion;
    private double posicion;
    private double saltoPendiente = -1.0;
    private long ultimoNano;
    private long momentoPosicionMs;
    private long finMs;
    private Vista vistaFinal;

    SesionSubs(ArchivoSubs.Subs subs) {
        this.lineas = subs.lineas();
        this.opciones = subs.opciones();
    }

    boolean cancelada() {
        return cancelada;
    }

    void reemplazar(ArchivoSubs.Subs nuevos) {
        lineas = nuevos.lineas();
        opciones = nuevos.opciones();
        if (estado == Estado.ACTIVA && fuente == 0) {
            duracion = duracionSinAudio();
        }
        if (fuente != 0) {
            AL10.alSourcei(fuente, AL10.AL_LOOPING, opciones.bucle ? AL10.AL_TRUE : AL10.AL_FALSE);
        }
    }

    void saltar(double segundos) {
        double destino = Math.max(0.0, segundos);
        if (estado == Estado.CARGANDO) {
            saltoPendiente = destino;
            return;
        }
        if (estado != Estado.ACTIVA) {
            return;
        }
        if (duracion > 0) {
            destino = Math.min(destino, Math.max(0.0, duracion - 0.05));
        }
        if (fuente != 0) {
            AL10.alSourcef(fuente, AL11.AL_SEC_OFFSET, (float) destino);
        }
        posicion = destino;
        ultimoNano = System.nanoTime();
        momentoPosicionMs = System.currentTimeMillis();
    }

    void iniciarSinAudio() {
        duracion = duracionSinAudio();
        posicion = 0.0;
        ultimoNano = System.nanoTime();
        momentoPosicionMs = System.currentTimeMillis();
        estado = Estado.ACTIVA;
    }

    void iniciarConAudio(ShortBuffer pcm, int canales, int frecuencia, float volumenJuego) {
        try {
            AL10.alGetError();
            duracion = pcm.remaining() / (double) canales / frecuencia;
            buffer = AL10.alGenBuffers();
            AL10.alBufferData(buffer, canales == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16, pcm, frecuencia);
            fuente = AL10.alGenSources();
            AL10.alSourcei(fuente, AL10.AL_BUFFER, buffer);
            AL10.alSourcei(fuente, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
            AL10.alSource3f(fuente, AL10.AL_POSITION, 0.0f, 0.0f, 0.0f);
            AL10.alSourcei(fuente, AL10.AL_LOOPING, opciones.bucle ? AL10.AL_TRUE : AL10.AL_FALSE);
            AL10.alSourcef(fuente, AL10.AL_GAIN, volumenJuego * (float) opciones.volumen);
            if (saltoPendiente >= 0) {
                posicion = Math.min(saltoPendiente, Math.max(0.0, duracion - 0.05));
                AL10.alSourcef(fuente, AL11.AL_SEC_OFFSET, (float) posicion);
                saltoPendiente = -1.0;
            }
            AL10.alSourcePlay(fuente);
            if (AL10.alGetError() != AL10.AL_NO_ERROR) {
                liberarAudio();
                throw new IllegalStateException("OpenAL no pudo reproducir el audio");
            }
            momentoPosicionMs = System.currentTimeMillis();
            estado = Estado.ACTIVA;
        } finally {
            LibCStdlib.free(pcm);
        }
    }

    void tick(boolean pausado, float volumenJuego, long ahora) {
        if (estado != Estado.ACTIVA) {
            return;
        }
        enPausa = pausado;
        if (fuente != 0) {
            avanzarConAudio(pausado, volumenJuego, ahora);
        } else {
            avanzarConReloj(pausado, ahora);
        }
    }

    Vista vista(long ahora) {
        if (estado == Estado.CARGANDO) {
            return null;
        }
        if (estado == Estado.FINALIZADA) {
            if (vistaFinal == null) {
                return null;
            }
            float opacidad = opacidadFinal(ahora);
            if (opacidad <= 0.0f) {
                return null;
            }
            return new Vista(vistaFinal.nombre(), vistaFinal.texto(), -1, opacidad, vistaFinal.colorNombre(), vistaFinal.colorTexto());
        }
        double actual = posicion;
        if (!enPausa) {
            actual = Math.min(duracion, posicion + (ahora - momentoPosicionMs) / 1000.0);
        }
        return calcular(actual);
    }

    boolean terminada(long ahora) {
        if (estado != Estado.FINALIZADA) {
            return false;
        }
        long total = COLA_FINAL_MS + (opciones.fade ? FADE_MS : 0);
        return vistaFinal == null || ahora - finMs >= total;
    }

    void detener() {
        cancelada = true;
        liberarAudio();
    }

    private double duracionSinAudio() {
        return lineas.get(lineas.size() - 1).inicio() + 1.0;
    }

    private int indice(double pos) {
        int resultado = -1;
        for (int i = 0; i < lineas.size(); i++) {
            if (lineas.get(i).inicio() > pos) {
                break;
            }
            resultado = i;
        }
        return resultado;
    }

    private Vista vistaDe(ArchivoSubs.Linea linea, int letras, float opacidad) {
        Integer propio = linea.nombre() == null ? null : opciones.personajes.get(ArchivoSubs.claveNombre(linea.nombre()));
        int colorTexto = propio != null ? propio : opciones.color;
        int colorNombre;
        if (propio != null) {
            colorNombre = propio;
        } else if (opciones.colorNombre != null) {
            colorNombre = opciones.colorNombre;
        } else {
            colorNombre = opciones.color;
        }
        return new Vista(linea.nombre(), linea.texto(), letras, opacidad, colorNombre, colorTexto);
    }

    private Vista calcular(double pos) {
        int indice = indice(pos);
        if (indice < 0) {
            return null;
        }
        ArchivoSubs.Linea linea = lineas.get(indice);
        double transcurrido = pos - linea.inicio();
        if (!linea.texto().isEmpty()) {
            float opacidad = opciones.fade ? (float) Math.min(1.0, transcurrido / FADE_S) : 1.0f;
            int letras = opciones.letraPorLetra ? (int) (transcurrido * opciones.velocidad) + 1 : -1;
            return vistaDe(linea, letras, opacidad);
        }
        if (opciones.fade && indice > 0 && transcurrido < FADE_S) {
            ArchivoSubs.Linea anterior = lineas.get(indice - 1);
            if (!anterior.texto().isEmpty()) {
                return vistaDe(anterior, -1, (float) (1.0 - transcurrido / FADE_S));
            }
        }
        return null;
    }

    private float opacidadFinal(long ahora) {
        long transcurrido = ahora - finMs;
        if (transcurrido < COLA_FINAL_MS) {
            return 1.0f;
        }
        if (!opciones.fade) {
            return 0.0f;
        }
        return Math.max(0.0f, 1.0f - (transcurrido - COLA_FINAL_MS) / (float) FADE_MS);
    }

    private void avanzarConAudio(boolean pausado, float volumenJuego, long ahora) {
        AL10.alSourcef(fuente, AL10.AL_GAIN, volumenJuego * (float) opciones.volumen);
        int estadoAl = AL10.alGetSourcei(fuente, AL10.AL_SOURCE_STATE);
        if (pausado && estadoAl == AL10.AL_PLAYING) {
            AL10.alSourcePause(fuente);
            estadoAl = AL10.AL_PAUSED;
        } else if (!pausado && estadoAl == AL10.AL_PAUSED) {
            AL10.alSourcePlay(fuente);
            estadoAl = AL10.AL_PLAYING;
        }
        if (estadoAl == AL10.AL_STOPPED) {
            if (!opciones.bucle) {
                finalizar(ahora);
                return;
            }
            AL10.alSourcePlay(fuente);
            posicion = 0.0;
        } else {
            posicion = AL10.alGetSourcef(fuente, AL11.AL_SEC_OFFSET);
        }
        momentoPosicionMs = ahora;
    }

    private void avanzarConReloj(boolean pausado, long ahora) {
        long nano = System.nanoTime();
        if (!pausado) {
            posicion += (nano - ultimoNano) / 1.0e9;
        }
        ultimoNano = nano;
        if (posicion >= duracion) {
            if (opciones.bucle && duracion > 0) {
                posicion = posicion % duracion;
            } else {
                finalizar(ahora);
                return;
            }
        }
        momentoPosicionMs = ahora;
    }

    private void finalizar(long ahora) {
        liberarAudio();
        posicion = duracion;
        int indice = indice(duracion);
        if (indice >= 0 && !lineas.get(indice).texto().isEmpty()) {
            vistaFinal = vistaDe(lineas.get(indice), -1, 1.0f);
        } else {
            vistaFinal = null;
        }
        estado = Estado.FINALIZADA;
        finMs = ahora;
    }

    private void liberarAudio() {
        if (fuente != 0) {
            AL10.alSourceStop(fuente);
            AL10.alDeleteSources(fuente);
            fuente = 0;
        }
        if (buffer != 0) {
            AL10.alDeleteBuffers(buffer);
            buffer = 0;
        }
    }
}