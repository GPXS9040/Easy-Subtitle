package com.subtitulo;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ArchivoSubs {
    public record Linea(double inicio, String nombre, String texto) {
    }

    public static final class Opciones {
        public boolean fade = false;
        public boolean letraPorLetra = false;
        public double velocidad = 30.0;
        public boolean bucle = false;
        public double volumen = 1.0;
        public int color = 0xFFFFFF;
        public Integer colorNombre = null;
        public final Map<String, Integer> personajes = new HashMap<>();
    }

    public record Subs(List<Linea> lineas, Opciones opciones) {
    }

    private static final String TIEMPO = "\\d+(?::\\d+){1,2}(?:[.,]\\d+)?";
    private static final Pattern PLANA = Pattern.compile("^(" + TIEMPO + ")(?:\\s+(.*))?$");
    private static final Pattern LRC_TIEMPO = Pattern.compile("^\\[(" + TIEMPO + ")\\]");
    private static final Pattern METADATO = Pattern.compile("^\\[[A-Za-z]+:[^\\]]*\\]\\s*$");
    private static final Pattern OPCION = Pattern.compile("^([A-Za-z_]+)\\s*=\\s*(.+)$");
    private static final Pattern PERSONAJE = Pattern.compile("^\\[([^\\]]{1,32})\\]\\s*=\\s*(.+)$");
    private static final Pattern NOMBRE = Pattern.compile("^\\[([^\\]]{1,32})\\]\\s*(.*)$");
    private static final Pattern PALABRA_LRC = Pattern.compile("<\\d+(?::\\d+){1,2}(?:[.,]\\d+)?>");
    private static final Pattern HEX = Pattern.compile("^#([0-9A-Fa-f]{6})$");

    private static final Map<String, Integer> COLORES = Map.ofEntries(
            Map.entry("blanco", 0xFFFFFF),
            Map.entry("white", 0xFFFFFF),
            Map.entry("negro", 0x000000),
            Map.entry("black", 0x000000),
            Map.entry("gris", 0xAAAAAA),
            Map.entry("gray", 0xAAAAAA),
            Map.entry("grey", 0xAAAAAA),
            Map.entry("grisoscuro", 0x555555),
            Map.entry("darkgray", 0x555555),
            Map.entry("rojo", 0xFF5555),
            Map.entry("red", 0xFF5555),
            Map.entry("rojooscuro", 0xAA0000),
            Map.entry("darkred", 0xAA0000),
            Map.entry("naranja", 0xFF8000),
            Map.entry("orange", 0xFF8000),
            Map.entry("dorado", 0xFFAA00),
            Map.entry("gold", 0xFFAA00),
            Map.entry("amarillo", 0xFFFF55),
            Map.entry("yellow", 0xFFFF55),
            Map.entry("verde", 0x55FF55),
            Map.entry("green", 0x55FF55),
            Map.entry("verdeoscuro", 0x00AA00),
            Map.entry("darkgreen", 0x00AA00),
            Map.entry("celeste", 0x55FFFF),
            Map.entry("aqua", 0x55FFFF),
            Map.entry("cian", 0x00AAAA),
            Map.entry("cyan", 0x00AAAA),
            Map.entry("azul", 0x5555FF),
            Map.entry("blue", 0x5555FF),
            Map.entry("azuloscuro", 0x0000AA),
            Map.entry("darkblue", 0x0000AA),
            Map.entry("morado", 0xAA00AA),
            Map.entry("purple", 0xAA00AA),
            Map.entry("magenta", 0xFF55FF),
            Map.entry("rosa", 0xFF69B4),
            Map.entry("pink", 0xFF69B4)
    );

    private ArchivoSubs() {
    }

    public static Subs leer(Path archivo) throws IOException {
        String contenido = new String(Files.readAllBytes(archivo), StandardCharsets.UTF_8);
        if (!contenido.isEmpty() && contenido.charAt(0) == '\uFEFF') {
            contenido = contenido.substring(1);
        }
        Opciones opciones = new Opciones();
        List<Linea> lineas = contenido.trim().startsWith("{") ? leerJson(contenido, opciones) : leerTexto(contenido, opciones);
        lineas.sort(Comparator.comparingDouble(Linea::inicio));
        return new Subs(lineas, opciones);
    }

    public static String formatearTiempo(double segundos) {
        int total = (int) Math.max(0, segundos);
        return String.format(Locale.ROOT, "%d:%02d", total / 60, total % 60);
    }

    public static String claveNombre(String nombre) {
        return nombre.trim().toLowerCase(Locale.ROOT);
    }

    private static List<Linea> leerTexto(String contenido, Opciones opciones) {
        List<Linea> lineas = new ArrayList<>();
        String[] filas = contenido.split("\\R");
        for (int i = 0; i < filas.length; i++) {
            String fila = filas[i].trim();
            if (fila.isEmpty() || fila.startsWith("#") || fila.startsWith("//") || METADATO.matcher(fila).matches()) {
                continue;
            }
            try {
                Matcher opcion = OPCION.matcher(fila);
                if (opcion.matches()) {
                    aplicarOpcion(opciones, opcion.group(1), opcion.group(2));
                    continue;
                }
                Matcher personaje = PERSONAJE.matcher(fila);
                if (personaje.matches() && !LRC_TIEMPO.matcher(fila).find()) {
                    opciones.personajes.put(claveNombre(personaje.group(1)), color(personaje.group(2)));
                    continue;
                }
                analizarLinea(fila, lineas);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("linea " + (i + 1) + ": " + e.getMessage());
            }
        }
        return lineas;
    }

    private static List<Linea> leerJson(String contenido, Opciones opciones) {
        List<Linea> lineas = new ArrayList<>();
        JsonObject objeto = JsonParser.parseString(contenido).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entrada : objeto.entrySet()) {
            String clave = entrada.getKey().trim();
            String valor = entrada.getValue().getAsString();
            if (!clave.isEmpty() && Character.isDigit(clave.charAt(0))) {
                List<Double> tiempos = new ArrayList<>();
                tiempos.add(Subtitulo.parsearTiempo(clave));
                agregar(lineas, tiempos, valor);
            } else if (clave.length() > 2 && clave.startsWith("[") && clave.endsWith("]")) {
                opciones.personajes.put(claveNombre(clave.substring(1, clave.length() - 1)), color(valor));
            } else {
                aplicarOpcion(opciones, clave, valor);
            }
        }
        return lineas;
    }

    private static void analizarLinea(String fila, List<Linea> destino) {
        List<Double> tiempos = new ArrayList<>();
        String pendiente = fila;
        while (true) {
            Matcher coincidencia = LRC_TIEMPO.matcher(pendiente);
            if (!coincidencia.find()) {
                break;
            }
            tiempos.add(Subtitulo.parsearTiempo(coincidencia.group(1)));
            pendiente = pendiente.substring(coincidencia.end()).stripLeading();
        }
        if (tiempos.isEmpty()) {
            Matcher plana = PLANA.matcher(fila);
            if (!plana.matches()) {
                throw new IllegalArgumentException("no valida: " + fila);
            }
            tiempos.add(Subtitulo.parsearTiempo(plana.group(1)));
            pendiente = plana.group(2) == null ? "" : plana.group(2);
        }
        agregar(destino, tiempos, pendiente);
    }

    private static void agregar(List<Linea> destino, List<Double> tiempos, String bruto) {
        String resto = PALABRA_LRC.matcher(bruto).replaceAll("").trim();
        String nombre = null;
        String texto = resto;
        Matcher coincidencia = NOMBRE.matcher(resto);
        if (coincidencia.matches()) {
            String candidato = coincidencia.group(1).trim();
            String despues = coincidencia.group(2).trim();
            if (!candidato.isEmpty() && !despues.isEmpty()) {
                nombre = candidato;
                texto = despues;
            }
        }
        for (double tiempo : tiempos) {
            destino.add(new Linea(tiempo, nombre, texto));
        }
    }

    private static void aplicarOpcion(Opciones opciones, String clave, String valor) {
        String normalizada = clave.trim().toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
        String texto = valor.trim();
        switch (normalizada) {
            case "fade":
                opciones.fade = booleano(texto);
                break;
            case "letraporletra":
                opciones.letraPorLetra = booleano(texto);
                break;
            case "velocidad":
                opciones.velocidad = numero(texto, 1.0, 200.0);
                break;
            case "bucle":
                opciones.bucle = booleano(texto);
                break;
            case "volumen":
                opciones.volumen = numero(texto, 0.0, 1.0);
                break;
            case "color":
            case "colortexto":
                opciones.color = color(texto);
                break;
            case "colornombre":
                opciones.colorNombre = color(texto);
                break;
            default:
                throw new IllegalArgumentException("opcion desconocida: " + clave);
        }
    }

    private static int color(String texto) {
        String limpio = texto.trim();
        Matcher hexadecimal = HEX.matcher(limpio);
        if (hexadecimal.matches()) {
            return Integer.parseInt(hexadecimal.group(1), 16);
        }
        String clave = Normalizer.normalize(limpio, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s_\\-]", "");
        Integer valor = COLORES.get(clave);
        if (valor == null) {
            throw new IllegalArgumentException("color desconocido: " + texto + " (usa un nombre como rojo o un codigo como #FF8800)");
        }
        return valor;
    }

    private static boolean booleano(String texto) {
        String valor = texto.toLowerCase(Locale.ROOT);
        if (valor.equals("true") || valor.equals("si") || valor.equals("sí") || valor.equals("yes") || valor.equals("1")) {
            return true;
        }
        if (valor.equals("false") || valor.equals("no") || valor.equals("0")) {
            return false;
        }
        throw new IllegalArgumentException("valor no valido (usa true o false): " + texto);
    }

    private static double numero(String texto, double minimo, double maximo) {
        double valor;
        try {
            valor = Double.parseDouble(texto.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("numero no valido: " + texto);
        }
        if (Double.isNaN(valor)) {
            throw new IllegalArgumentException("numero no valido: " + texto);
        }
        return Math.max(minimo, Math.min(maximo, valor));
    }
}