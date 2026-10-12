package com.subtitulo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

final class DibujoSubs {
    record Caja(int izquierda, int arriba, int derecha, int abajo, List<String> lineas, int prefijo) {
    }

    private DibujoSubs() {
    }

    static Caja medir(MinecraftClient client, String nombre, String texto, int anchoPantalla, int centroX, int borde) {
        String prefijoTexto = nombre == null ? "" : nombre + ": ";
        List<String> lineas = new ArrayList<>();
        int anchoMaximo = Math.min(anchoPantalla - 40, 320);
        for (StringVisitable parte : client.textRenderer.getTextHandler().wrapLines(Text.literal(prefijoTexto + texto), anchoMaximo, Style.EMPTY)) {
            lineas.add(parte.getString());
        }
        if (lineas.isEmpty()) {
            return null;
        }
        int ancho = 0;
        for (String linea : lineas) {
            ancho = Math.max(ancho, client.textRenderer.getWidth(linea));
        }
        int alto = lineas.size() * 10 + 4;
        int anchoCaja = ancho + 8;
        int izquierda = Math.max(0, Math.min(centroX - anchoCaja / 2, anchoPantalla - anchoCaja));
        return new Caja(izquierda, borde - alto, izquierda + anchoCaja, borde, lineas, prefijoTexto.length());
    }

    static int dibujar(DrawContext context, MinecraftClient client, String nombre, String texto, int letras, float opacidad, int centroX, int borde) {
        return dibujar(context, client, nombre, texto, letras, opacidad, 0xFFFFFF, 0xFFFFFF, centroX, borde);
    }

    static int dibujar(DrawContext context, MinecraftClient client, String nombre, String texto, int letras, float opacidad, int colorNombre, int colorTexto, int centroX, int borde) {
        int alfa = (int) (255 * Math.max(0.0f, Math.min(1.0f, opacidad)));
        if (alfa <= 3) {
            return borde;
        }
        Caja caja = medir(client, nombre, texto, context.getScaledWindowWidth(), centroX, borde);
        if (caja == null) {
            return borde;
        }
        context.fill(caja.izquierda(), caja.arriba(), caja.derecha(), caja.abajo(), (alfa / 2) << 24);
        int argbNombre = (alfa << 24) | (colorNombre & 0xFFFFFF);
        int argbTexto = (alfa << 24) | (colorTexto & 0xFFFFFF);
        int interior = caja.derecha() - caja.izquierda() - 8;
        int y = caja.arriba() + 3;
        int restante = letras < 0 ? Integer.MAX_VALUE : caja.prefijo() + letras;
        int acumulado = 0;
        for (String linea : caja.lineas()) {
            int visibles = Math.max(0, Math.min(restante, linea.length()));
            int deNombre = Math.max(0, Math.min(caja.prefijo() - acumulado, linea.length()));
            int parteNombre = Math.min(deNombre, visibles);
            int x = caja.izquierda() + 4 + (interior - client.textRenderer.getWidth(linea)) / 2;
            if (parteNombre > 0) {
                String segmento = linea.substring(0, parteNombre);
                context.drawText(client.textRenderer, segmento, x, y, argbNombre, false);
                x += client.textRenderer.getWidth(segmento);
            }
            if (visibles > parteNombre) {
                context.drawText(client.textRenderer, linea.substring(parteNombre, visibles), x, y, argbTexto, false);
            }
            restante -= linea.length();
            acumulado += linea.length();
            y += 10;
        }
        return caja.arriba() - 2;
    }
}