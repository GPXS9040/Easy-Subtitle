package com.subtitulo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class ConfigSubs {
    static final int BORDE_DEFECTO = 54;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Double x = null;
    private static Double y = null;

    private ConfigSubs() {
    }

    static void cargar() {
        x = null;
        y = null;
        Path ruta = archivo();
        if (!Files.isRegularFile(ruta)) {
            return;
        }
        try {
            JsonObject objeto = JsonParser.parseString(Files.readString(ruta, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonElement valorX = objeto.get("x");
            JsonElement valorY = objeto.get("y");
            if (valorX != null && valorY != null) {
                x = limitar(valorX.getAsDouble());
                y = limitar(valorY.getAsDouble());
            }
        } catch (IOException | RuntimeException e) {
            Subtitulo.LOGGER.warn("No se pudo leer la posicion guardada del subtitulo", e);
            x = null;
            y = null;
        }
    }

    static boolean personalizada() {
        return x != null && y != null;
    }

    static int centroX(int anchoPantalla) {
        return personalizada() ? (int) Math.round(x * anchoPantalla) : anchoPantalla / 2;
    }

    static int borde(int altoPantalla) {
        return personalizada() ? (int) Math.round(y * altoPantalla) : altoPantalla - BORDE_DEFECTO;
    }

    static void fijar(double nuevoX, double nuevoY) {
        x = limitar(nuevoX);
        y = limitar(nuevoY);
        guardar();
    }

    static void restablecer() {
        x = null;
        y = null;
        guardar();
    }

    private static double limitar(double valor) {
        return Math.max(0.0, Math.min(1.0, valor));
    }

    private static Path archivo() {
        return FabricLoader.getInstance().getConfigDir().resolve("subtitulo.json");
    }

    private static void guardar() {
        Path ruta = archivo();
        try {
            if (!personalizada()) {
                Files.deleteIfExists(ruta);
                return;
            }
            JsonObject objeto = new JsonObject();
            objeto.addProperty("x", x);
            objeto.addProperty("y", y);
            Files.createDirectories(ruta.getParent());
            Files.writeString(ruta, GSON.toJson(objeto), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Subtitulo.LOGGER.error("No se pudo guardar la posicion del subtitulo", e);
        }
    }
}