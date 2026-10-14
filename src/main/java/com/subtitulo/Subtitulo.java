package com.subtitulo;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Subtitulo implements ModInitializer {
	public static final String MOD_ID = "subtitulo";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Identifier PAQUETE_SUBTITULO = new Identifier(MOD_ID, "mostrar");
	public static final Identifier PAQUETE_CONTROL = new Identifier(MOD_ID, "control");
	public static final String CARPETA_RAIZ = "subsSound";
	public static final byte ACCION_PLAY = 0;
	public static final byte ACCION_STOP = 1;
	public static final byte ACCION_RELOAD = 2;
	public static final byte ACCION_SEEK = 3;

	private static final Pattern NOMBRE_VALIDO = Pattern.compile("[A-Za-z0-9_-]+");


	private static final String EJEMPLO = """
            // =====================================================================
            //             EJEMPLO DE SUBTITULOS / SUBTITLES EXAMPLE
            // =====================================================================
            //
            // [ES] Las lineas que empiezan con // o # son comentarios y se ignoran.
            //      No escribas comentarios al final de las opciones o subtitulos.
            // [EN] Lines starting with // or # are comments and are ignored.
            //      Do not add comments at the end of options or subtitle lines.
            //
            // ---------------------------------------------------------------------
            // COMO USARLO / HOW TO USE
            // ---------------------------------------------------------------------
            //
            // [ES] 1. Crea una carpeta dentro de subsSound.
            //         Ejemplo: subsSound/mi_audio
            // [EN] 1. Create a folder inside subsSound.
            //         Example: subsSound/my_audio
            //
            // [ES] 2. Coloca el audio sound.ogg y el archivo de subtitulos
            //         dentro de la carpeta. Usa sub.lrc o sub.json.
            //         Si existen ambos archivos, se utiliza sub.json.
            // [EN] 2. Place sound.ogg and the subtitle file inside the folder.
            //         Use sub.lrc or sub.json. If both exist, sub.json is used.
            //
            // [ES] 3. Inicia la reproduccion dentro del juego:
            //         /subtitulo play @a mi_audio
            // [EN] 3. Start playback in game using the command above.
            //
            // [ES] 4. Si no existe sound.ogg, los subtitulos utilizan
            //         un reloj interno.
            // [EN] 4. If sound.ogg is missing, subtitles use an internal clock.
            //
            // Guarda el archivo en UTF-8 para mostrar correctamente las tildes.
            // Save the file as UTF-8 to display special characters correctly.
            //
            // ---------------------------------------------------------------------
            // COMANDOS / COMMANDS
            // ---------------------------------------------------------------------
            //
            // /subtitulo play <jugadores> <carpeta>
            // Inicia el audio y los subtitulos para los jugadores seleccionados.
            // Starts audio and subtitles for the selected players.
            //
            // /subtitulo stop <jugadores> [carpeta]
            // Detiene la reproduccion. Sin carpeta, detiene todas las reproducciones
            // de los jugadores seleccionados.
            // Stops playback. Without a folder, stops all playback for those players.
            //
            // /subtitulo reload [carpeta]
            // Recarga los subtitulos sin reiniciar el audio.
            // Reloads subtitles without restarting the audio.
            //
            // /subtitulo seek <jugadores> <carpeta> <tiempo>
            // Salta a un momento especifico, por ejemplo 01:15 o 75.5.
            // Jumps to a specific timestamp, for example 01:15 or 75.5.
            //
            // /subtitulo mostrar <jugadores> <segundos> <texto>
            // Muestra un mensaje independiente durante el tiempo indicado.
            // Displays a standalone message for the specified duration.
            //
            // Tecla K: permite mover el subtitulo por la pantalla.
            // Puedes cambiar la tecla desde el menu Controles.
            // Key K: moves the subtitle on screen. Change it in Controls.
            //
            // ---------------------------------------------------------------------
            // FORMATO DE LOS SUBTITULOS / SUBTITLE FORMAT
            // ---------------------------------------------------------------------
            //
            // [minutos:segundos.decimales] Texto
            // [minutos:segundos.decimales][Nombre] Texto
            //
            // [ES] El tiempo indica cuando aparece el subtitulo.
            //      Ejemplos: [00:10.00] o [01:02.50]
            //      Tambien se admiten tiempos sin corchetes: 00:10 Texto
            //      y formatos con horas: 1:02:30.
            //
            // [EN] The timestamp determines when the subtitle appears.
            //      Examples: [00:10.00] or [01:02.50]
            //      Timestamps without brackets and hour formats are also supported.
            //
            // [ES] El nombre es opcional. Si se incluye, se muestra como:
            //      Nombre: Texto
            //      Si se omite, solo aparece el texto.
            //
            // [EN] The character name is optional. If included, it appears as:
            //      Name: Text
            //      If omitted, only the text is displayed.
            //
            // [ES] Una linea que contiene solo un tiempo borra el subtitulo.
            //      Ejemplo: [00:15.00]
            //      Util para silencios o partes instrumentales.
            //
            // [EN] A line containing only a timestamp clears the subtitle.
            //      Useful for silences or instrumental sections.
            //
            // [ES] Cada linea permanece visible hasta que comienza la siguiente.
            //      La ultima linea permanece 2 segundos tras terminar el audio.
            //
            // [EN] Each line stays visible until the next one begins.
            //      The last line remains visible for 2 seconds after the audio ends.
            //
            // [ES] Tambien puedes utilizar archivos de letras .lrc existentes.
            //      Se ignoran etiquetas como [ar:...] y [ti:...],
            //      asi como los tiempos por palabra, por ejemplo <00:10.50>.
            //
            // [EN] Existing .lrc lyric files are also supported.
            //      Tags such as [ar:...] and [ti:...], as well as word-level
            //      timestamps such as <00:10.50>, are ignored.
            //
            // ---------------------------------------------------------------------
            // OPCIONES / OPTIONS
            // ---------------------------------------------------------------------
            //
            // Escribe cada opcion en una linea separada usando clave = valor.
            // Write each option on a separate line using key = value.
            //
            // fade = true
            // [ES] Activa la aparicion y desaparicion gradual.
            // [EN] Enables subtitle fade-in and fade-out.
            //
            // letra_por_letra = true
            // [ES] Muestra el texto letra por letra.
            // [EN] Displays the text one letter at a time.
            //
            // velocidad = 30
            // [ES] Velocidad del efecto anterior, de 1 a 200 letras por segundo.
            // [EN] Typing speed, from 1 to 200 letters per second.
            //
            // bucle = true
            // [ES] Repite el audio y los subtitulos hasta detenerlos.
            // [EN] Loops the audio and subtitles until stopped.
            //
            // volumen = 1.0
            // [ES] Volumen del audio, entre 0 y 1.
            // [EN] Audio volume, from 0 to 1.
            //
            // color = blanco
            // [ES] Color predeterminado del texto.
            // [EN] Default text color.
            //
            // color_nombre = amarillo
            // [ES] Color predeterminado de los nombres.
            // [EN] Default character name color.
            //
            // [Nombre] = color
            // [ES] Establece un color propio para el nombre y texto del personaje.
            // [EN] Sets a custom color for a character's name and text.
            //
            // COLORES / COLORS
            //
            // blanco, gris, gris_oscuro, negro, rojo, rojo_oscuro, naranja,
            // dorado, amarillo, verde, verde_oscuro, celeste, cian, azul,
            // azul_oscuro, morado, magenta, rosa.
            //
            // white, gray, darkgray, black, red, darkred, orange, gold,
            // yellow, green, darkgreen, aqua, cyan, blue, darkblue, purple,
            // magenta, pink.
            //
            // Tambien puedes utilizar colores hexadecimales, como #FF8800.
            // Hexadecimal colors are also supported, for example #FF8800.
            //
            // Prioridad / Priority:
            // 1. Color del personaje / Character color
            // 2. color_nombre
            // 3. color
            //
            // Para activar una opcion de ejemplo, elimina el // inicial.
            // To enable an example option, remove the leading //.
            //
            // =====================================================================
            // EJEMPLO DE CONFIGURACION / CONFIGURATION EXAMPLE
            // =====================================================================
            //
            // ---------- OPCIONES / OPTIONS ----------
            fade = false
            letra_por_letra = false
            velocidad = 30
            bucle = false
            volumen = 1.0
            color = blanco
            color_nombre = amarillo
            [Narrador] = celeste
            [Maria] = #FF69B4
            //
            // ---------- SUBTITULOS / SUBTITLES ----------
            [00:01.00][Narrador] Hola, este es un subtitulo de ejemplo.
            [00:05.00] Una linea sin nombre solo muestra el texto.
            [00:09.00][Maria] Y esta tiene el nombre de otro personaje.
            [00:13.00]
            [00:14.50] Esta aparece despues de una linea vacia.
            [00:18.00][Narrador] Fin del ejemplo.
            """;


	public static Path raiz() {
		return FabricLoader.getInstance().getGameDir().resolve(CARPETA_RAIZ);
	}

	public static boolean nombreValido(String nombre) {
		return NOMBRE_VALIDO.matcher(nombre).matches();
	}

	public static double parsearTiempo(String texto) {
		String limpio = texto.trim().replace(',', '.');
		String[] partes = limpio.split(":");
		if (partes.length > 3) {
			throw new IllegalArgumentException("tiempo no valido: " + texto);
		}
		double total = 0;
		for (String parte : partes) {
			total = total * 60 + Double.parseDouble(parte);
		}
		if (total < 0 || Double.isNaN(total) || Double.isInfinite(total)) {
			throw new IllegalArgumentException("tiempo no valido: " + texto);
		}
		return total;
	}

	public static List<String> listarCarpetas() {
		Path raiz = raiz();
		if (!Files.isDirectory(raiz)) {
			return List.of();
		}
		try (Stream<Path> flujo = Files.list(raiz)) {
			return flujo.filter(Files::isDirectory)
					.map(ruta -> ruta.getFileName().toString())
					.filter(Subtitulo::nombreValido)
					.sorted()
					.collect(Collectors.toList());
		} catch (IOException e) {
			LOGGER.warn("No se pudo listar {}", CARPETA_RAIZ, e);
			return List.of();
		}
	}

	@Override
	public void onInitialize() {
		crearEjemplo();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registrarComandos(dispatcher));
		LOGGER.info("Subtitulo cargado");
	}

	private static void crearEjemplo() {
		Path ejemplo = raiz().resolve("ejemplo");
		if (Files.exists(ejemplo)) {
			return;
		}
		try {
			Files.createDirectories(ejemplo);
			Files.writeString(ejemplo.resolve("sub.lrc"), EJEMPLO, StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOGGER.error("No se pudo crear la carpeta de ejemplo en {}", CARPETA_RAIZ, e);
		}
	}

	private static void registrarComandos(CommandDispatcher<ServerCommandSource> dispatcher) {
		SuggestionProvider<ServerCommandSource> carpetas = (contexto, builder) ->
				CommandSource.suggestMatching(listarCarpetas(), builder);

		dispatcher.register(CommandManager.literal("subtitulo")
				.requires(source -> source.hasPermissionLevel(2))
				.then(CommandManager.literal("mostrar")
						.then(CommandManager.argument("jugadores", EntityArgumentType.players())
								.then(CommandManager.argument("segundos", DoubleArgumentType.doubleArg(0.1, 3600))
										.then(CommandManager.argument("texto", StringArgumentType.greedyString())
												.executes(Subtitulo::mostrar)))))
				.then(CommandManager.literal("play")
						.then(CommandManager.argument("jugadores", EntityArgumentType.players())
								.then(CommandManager.argument("nombre", StringArgumentType.word())
										.suggests(carpetas)
										.executes(Subtitulo::play))))
				.then(CommandManager.literal("stop")
						.then(CommandManager.argument("jugadores", EntityArgumentType.players())
								.executes(contexto -> stop(contexto, ""))
								.then(CommandManager.argument("nombre", StringArgumentType.word())
										.suggests(carpetas)
										.executes(contexto -> stop(contexto, StringArgumentType.getString(contexto, "nombre"))))))
				.then(CommandManager.literal("reload")
						.executes(contexto -> reload(contexto, ""))
						.then(CommandManager.argument("nombre", StringArgumentType.word())
								.suggests(carpetas)
								.executes(contexto -> reload(contexto, StringArgumentType.getString(contexto, "nombre")))))
				.then(CommandManager.literal("seek")
						.then(CommandManager.argument("jugadores", EntityArgumentType.players())
								.then(CommandManager.argument("nombre", StringArgumentType.word())
										.suggests(carpetas)
										.then(CommandManager.argument("tiempo", StringArgumentType.greedyString())
												.executes(Subtitulo::seek))))));
	}

	private static int mostrar(CommandContext<ServerCommandSource> contexto) throws CommandSyntaxException {
		Collection<ServerPlayerEntity> jugadores = EntityArgumentType.getPlayers(contexto, "jugadores");
		double segundos = DoubleArgumentType.getDouble(contexto, "segundos");
		int ticks = (int) Math.max(1, Math.round(segundos * 20));
		String texto = StringArgumentType.getString(contexto, "texto");
		for (ServerPlayerEntity jugador : jugadores) {
			PacketByteBuf buf = PacketByteBufs.create();
			buf.writeString(texto);
			buf.writeVarInt(ticks);
			ServerPlayNetworking.send(jugador, PAQUETE_SUBTITULO, buf);
		}
		return jugadores.size();
	}

	private static int play(CommandContext<ServerCommandSource> contexto) throws CommandSyntaxException {
		Collection<ServerPlayerEntity> jugadores = EntityArgumentType.getPlayers(contexto, "jugadores");
		String nombre = StringArgumentType.getString(contexto, "nombre");
		if (!nombreValido(nombre) || !Files.isDirectory(raiz().resolve(nombre))) {
			contexto.getSource().sendError(Text.literal("No existe la carpeta " + nombre + " en " + CARPETA_RAIZ));
			return 0;
		}
		int enviados = enviarControl(jugadores, ACCION_PLAY, nombre, 0.0);
		contexto.getSource().sendFeedback(() -> Text.literal("Reproduciendo " + nombre + " para " + enviados + " jugador(es)"), true);
		return enviados;
	}

	private static int stop(CommandContext<ServerCommandSource> contexto, String nombre) throws CommandSyntaxException {
		Collection<ServerPlayerEntity> jugadores = EntityArgumentType.getPlayers(contexto, "jugadores");
		if (!nombre.isEmpty() && !nombreValido(nombre)) {
			contexto.getSource().sendError(Text.literal("Nombre de carpeta no valido: " + nombre));
			return 0;
		}
		int enviados = enviarControl(jugadores, ACCION_STOP, nombre, 0.0);
		String detalle = nombre.isEmpty() ? "todos los subtitulos" : nombre;
		contexto.getSource().sendFeedback(() -> Text.literal("Detenido " + detalle + " para " + enviados + " jugador(es)"), true);
		return enviados;
	}

	private static int reload(CommandContext<ServerCommandSource> contexto, String nombre) {
		if (!nombre.isEmpty() && !nombreValido(nombre)) {
			contexto.getSource().sendError(Text.literal("Nombre de carpeta no valido: " + nombre));
			return 0;
		}
		int enviados = enviarControl(contexto.getSource().getServer().getPlayerManager().getPlayerList(), ACCION_RELOAD, nombre, 0.0);
		String detalle = nombre.isEmpty() ? "todos los subtitulos activos" : nombre;
		contexto.getSource().sendFeedback(() -> Text.literal("Recargando " + detalle + " para " + enviados + " jugador(es)"), true);
		return enviados;
	}

	private static int seek(CommandContext<ServerCommandSource> contexto) throws CommandSyntaxException {
		Collection<ServerPlayerEntity> jugadores = EntityArgumentType.getPlayers(contexto, "jugadores");
		String nombre = StringArgumentType.getString(contexto, "nombre");
		String tiempo = StringArgumentType.getString(contexto, "tiempo").trim();
		if (!nombreValido(nombre)) {
			contexto.getSource().sendError(Text.literal("Nombre de carpeta no valido: " + nombre));
			return 0;
		}
		double segundos;
		try {
			segundos = parsearTiempo(tiempo);
		} catch (RuntimeException e) {
			contexto.getSource().sendError(Text.literal("Tiempo no valido: " + tiempo + " (usa 75.5 o 01:15)"));
			return 0;
		}
		int enviados = enviarControl(jugadores, ACCION_SEEK, nombre, segundos);
		contexto.getSource().sendFeedback(() -> Text.literal("Saltando " + nombre + " a " + tiempo + " para " + enviados + " jugador(es)"), true);
		return enviados;
	}

	private static int enviarControl(Collection<ServerPlayerEntity> jugadores, byte accion, String nombre, double valor) {
		for (ServerPlayerEntity jugador : jugadores) {
			PacketByteBuf buf = PacketByteBufs.create();
			buf.writeByte(accion);
			buf.writeString(nombre);
			buf.writeDouble(valor);
			ServerPlayNetworking.send(jugador, PAQUETE_CONTROL, buf);
		}
		return jugadores.size();
	}

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}