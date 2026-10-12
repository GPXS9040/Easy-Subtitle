package com.subtitulo;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

final class PantallaPosicion extends Screen {
    private static final String NOMBRE_EJEMPLO = "Narrador";
    private static final String TEXTO_EJEMPLO = "Ejemplo de subtitulo";
    private static final int IMAN = 4;

    private double fx;
    private double fy;
    private boolean arrastrando;
    private boolean modificado;
    private int desfaseX;
    private int desfaseY;

    PantallaPosicion() {
        super(Text.literal("Posicion del subtitulo"));
    }

    @Override
    protected void init() {
        if (!modificado) {
            cargarDesdeConfig();
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Restablecer"), boton -> restablecer())
                .dimensions(width / 2 - 105, 8, 100, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Listo"), boton -> close())
                .dimensions(width / 2 + 5, 8, 100, 20)
                .build());
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (modificado) {
            ConfigSubs.fijar(fx, fy);
            modificado = false;
        }
        super.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int centro = width / 2;
        int colorGuia = centroX() == centro ? 0xFF55FF55 : 0x66FFFFFF;
        for (int y = 0; y < height; y += 6) {
            context.fill(centro, y, centro + 1, y + 3, colorGuia);
        }
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Arrastra el subtitulo para moverlo"), centro, 38, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Flechas: ajuste fino (Shift: x5)  |  Esc: guardar y salir"), centro, 50, 0xAAAAAA);
        DibujoSubs.dibujar(context, client, NOMBRE_EJEMPLO, TEXTO_EJEMPLO, -1, 1.0f, centroX(), borde());
        DibujoSubs.Caja caja = DibujoSubs.medir(client, NOMBRE_EJEMPLO, TEXTO_EJEMPLO, width, centroX(), borde());
        if (caja != null) {
            boolean resaltado = arrastrando || dentro(caja, mouseX, mouseY);
            contorno(context, caja, resaltado ? 0xFF55FF55 : 0xFFFFFFFF);
        }
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0) {
            DibujoSubs.Caja caja = DibujoSubs.medir(client, NOMBRE_EJEMPLO, TEXTO_EJEMPLO, width, centroX(), borde());
            if (caja != null && dentro(caja, mouseX, mouseY)) {
                arrastrando = true;
                desfaseX = (int) mouseX - centroX();
                desfaseY = (int) mouseY - borde();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (arrastrando && button == 0) {
            int nuevoX = (int) mouseX - desfaseX;
            if (Math.abs(nuevoX - width / 2) <= IMAN) {
                nuevoX = width / 2;
            }
            fijarPixeles(nuevoX, (int) mouseY - desfaseY);
            modificado = true;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            arrastrando = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int paso = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? 5 : 1;
        int dx = 0;
        int dy = 0;
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            dx = -paso;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            dx = paso;
        } else if (keyCode == GLFW.GLFW_KEY_UP) {
            dy = -paso;
        } else if (keyCode == GLFW.GLFW_KEY_DOWN) {
            dy = paso;
        }
        if (dx != 0 || dy != 0) {
            fijarPixeles(centroX() + dx, borde() + dy);
            modificado = true;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void cargarDesdeConfig() {
        fx = ConfigSubs.centroX(width) / (double) width;
        fy = ConfigSubs.borde(height) / (double) height;
    }

    private void restablecer() {
        ConfigSubs.restablecer();
        modificado = false;
        cargarDesdeConfig();
    }

    private int centroX() {
        return (int) Math.round(fx * width);
    }

    private int borde() {
        return (int) Math.round(fy * height);
    }

    private void fijarPixeles(int px, int py) {
        int limitadoX = Math.max(0, Math.min(width, px));
        int limitadoY = Math.max(26, Math.min(height, py));
        fx = limitadoX / (double) width;
        fy = limitadoY / (double) height;
    }

    private static boolean dentro(DibujoSubs.Caja caja, double mouseX, double mouseY) {
        return mouseX >= caja.izquierda() && mouseX <= caja.derecha() && mouseY >= caja.arriba() && mouseY <= caja.abajo();
    }

    private static void contorno(DrawContext context, DibujoSubs.Caja caja, int color) {
        context.fill(caja.izquierda() - 1, caja.arriba() - 1, caja.derecha() + 1, caja.arriba(), color);
        context.fill(caja.izquierda() - 1, caja.abajo(), caja.derecha() + 1, caja.abajo() + 1, color);
        context.fill(caja.izquierda() - 1, caja.arriba(), caja.izquierda(), caja.abajo(), color);
        context.fill(caja.derecha(), caja.arriba(), caja.derecha() + 1, caja.abajo(), color);
    }
}