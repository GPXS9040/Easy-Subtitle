package com.subtitulo;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SubtituloCliente implements ClientModInitializer {
    private static final int TICKS_FADE = 10;

    private static String textoManual = null;
    private static int ticksRestantes = 0;
    private static KeyBinding teclaPosicion;

    @Override
    public void onInitializeClient() {
        ConfigSubs.cargar();

        teclaPosicion = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.subtitulo.posicion",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "category.subtitulo"));

        ClientPlayNetworking.registerGlobalReceiver(Subtitulo.PAQUETE_SUBTITULO, (client, handler, buf, responseSender) -> {
            String texto = buf.readString();
            int ticks = buf.readVarInt();
            client.execute(() -> {
                textoManual = texto;
                ticksRestantes = ticks;
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(Subtitulo.PAQUETE_CONTROL, (client, handler, buf, responseSender) -> {
            byte accion = buf.readByte();
            String nombre = buf.readString(64);
            double valor = buf.readDouble();
            client.execute(() -> {
                if (accion == Subtitulo.ACCION_PLAY) {
                    ReproductorSubs.reproducir(client, nombre);
                } else if (accion == Subtitulo.ACCION_RELOAD) {
                    ReproductorSubs.recargar(client, nombre);
                } else if (accion == Subtitulo.ACCION_SEEK) {
                    ReproductorSubs.saltar(client, nombre, valor);
                } else {
                    ReproductorSubs.detener(nombre);
                }
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (teclaPosicion.wasPressed()) {
                if (client.currentScreen == null && client.world != null) {
                    client.setScreen(new PantallaPosicion());
                }
            }
            if (ticksRestantes > 0) {
                ticksRestantes--;
                if (ticksRestantes == 0) {
                    textoManual = null;
                }
            }
            ReproductorSubs.tick(client);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            textoManual = null;
            ticksRestantes = 0;
            ReproductorSubs.detenerTodo();
        }));

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ReproductorSubs.detenerTodo());

        HudRenderCallback.EVENT.register(SubtituloCliente::dibujar);
    }

    private static void dibujar(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        long ahora = System.currentTimeMillis();
        int centroX = ConfigSubs.centroX(context.getScaledWindowWidth());
        int borde = ConfigSubs.borde(context.getScaledWindowHeight());
        if (textoManual != null && ticksRestantes > 0) {
            float opacidad = Math.min(1.0f, ticksRestantes / (float) TICKS_FADE);
            borde = DibujoSubs.dibujar(context, client, null, textoManual, -1, opacidad, centroX, borde);
        }
        for (SesionSubs sesion : ReproductorSubs.sesiones()) {
            SesionSubs.Vista vista = sesion.vista(ahora);
            if (vista == null) {
                continue;
            }
            borde = DibujoSubs.dibujar(context, client, vista.nombre(), vista.texto(), vista.letras(), vista.opacidad(),
                    vista.colorNombre(), vista.colorTexto(), centroX, borde);
        }
    }
}