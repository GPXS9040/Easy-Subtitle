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
            // EJEMPLO DE SUBTITULOS  /  SUBTITLES EXAMPLE
            // =====================================================================
            // [ES] Las lineas que empiezan con // o con # son comentarios: el mod las ignora.
            //      No escribas comentarios al final de una linea de opciones o de subtitulo.
            // [EN] Lines starting with // or # are comments: the mod ignores them.
            //      Do not put comments at the end of an option or subtitle line.
            //
            // ---------------------------------------------------------------------
            // [ES] COMO USARLO
            // ---------------------------------------------------------------------
            // 1. Crea una carpeta dentro de subsSound (por ejemplo subsSound/mi_audio).
            //    El nombre solo puede tener letras, numeros, _ y -.
            // 2. Dentro pon tu audio como sound.ogg (mono o estereo) y este archivo
            //    como sub.lrc (tambien funciona sub.json). Si existen los dos, se usa sub.json.
            // 3. En el juego escribe: /subtitulo play @a mi_audio
            //    Si no hay sound.ogg, los subtitulos corren igual con un reloj interno.
            // 4. Guarda este archivo en UTF-8 para que las tildes se vean bien.
            //
            // [ES] COMANDOS
            //    /subtitulo play <jugadores> <carpeta>                iniciar
            //    /subtitulo stop <jugadores> [carpeta]                detener (sin carpeta: todos)
            //    /subtitulo reload [carpeta]                          recargar este archivo sin reiniciar el audio
            //    /subtitulo seek <jugadores> <carpeta> <tiempo>       saltar a un momento (01:15 o 75.5)
            //    /subtitulo mostrar <jugadores> <segundos> <texto>    mensaje suelto
            //    Tecla K: mover el subtitulo en pantalla (se puede cambiar en Controles).
            //
            // ---------------------------------------------------------------------
            // [EN] HOW TO USE IT
            // ---------------------------------------------------------------------
            // 1. Create a folder inside subsSound (for example subsSound/my_audio).
            //    The name can only contain letters, numbers, _ and -.
            // 2. Put your audio inside as sound.ogg (mono or stereo) and this file
            //    as sub.lrc (sub.json also works). If both exist, sub.json is used.
            // 3. In game type: /subtitulo play @a my_audio
            //    Without sound.ogg the subtitles still run using an internal clock.
            // 4. Save this file as UTF-8 so accents display correctly.
            //
            // [EN] COMMANDS
            //    /subtitulo play <players> <folder>                   start
            //    /subtitulo stop <players> [folder]                   stop (no folder: everything)
            //    /subtitulo reload [folder]                           reload this file without restarting the audio
            //    /subtitulo seek <players> <folder> <time>            jump to a moment (01:15 or 75.5)
            //    /subtitulo mostrar <players> <seconds> <text>        one-off message
            //    Key K: move the subtitle on screen (can be changed in Controls).
            //
            // ---------------------------------------------------------------------
            // [ES] FORMATO DE CADA LINEA
            // ---------------------------------------------------------------------
            //    [minutos:segundos.decimales] texto
            //    [minutos:segundos.decimales][Nombre] texto
            //
            // - El tiempo es el momento en que aparece la linea. Ejemplos: [00:10] [01:02.50]
            //   Tambien vale sin corchetes: 00:10 texto   (y horas: 1:02:30)
            // - El [Nombre] es opcional. Si lo pones, sale como "Nombre: texto".
            //   Si no lo pones, solo sale el texto.
            // - Una linea sin texto, solo con el tiempo, BORRA el subtitulo:   [00:15.00]
            //   Sirve para silencios o partes instrumentales.
            // - Cada linea se queda en pantalla hasta que empieza la siguiente.
            // - La ultima linea se queda 2 segundos despues de que termina el audio.
            // - Puedes repetir una linea en varios momentos: [00:10.00][01:30.00] Estribillo
            // - Puedes usar archivos .lrc de letras ya hechos. Se ignoran las etiquetas
            //   como [ar:...] o [ti:...] y los tiempos por palabra <00:10.50>.
            //
            // ---------------------------------------------------------------------
            // [EN] LINE FORMAT
            // ---------------------------------------------------------------------
            //    [minutes:seconds.decimals] text
            //    [minutes:seconds.decimals][Name] text
            //
            // - The time is the moment the line appears. Examples: [00:10] [01:02.50]
            //   Brackets are optional: 00:10 text   (hours also work: 1:02:30)
            // - The [Name] is optional. If present, it shows as "Name: text".
            //   If missing, only the text is shown.
            // - A line with no text, only the time, CLEARS the subtitle:   [00:15.00]
            //   Useful for silences or instrumental parts.
            // - Each line stays on screen until the next one starts.
            // - The last line stays 2 seconds after the audio ends.
            // - You can repeat a line at several moments: [00:10.00][01:30.00] Chorus
            // - Ready-made .lrc lyric files work too. Tags like [ar:...] or [ti:...]
            //   and per-word times <00:10.50> are ignored.
            //
            // ---------------------------------------------------------------------
            // [ES] OPCIONES (todas son opcionales, van en lineas aparte: clave = valor)
            // [EN] OPTIONS (all optional, one per line: key = value)
            // ---------------------------------------------------------------------
            // fade             [ES] true: cada linea aparece y se desvanece suavemente.
            //                  [EN] true: each line fades in and out smoothly.
            // letra_por_letra  [ES] true: el texto se escribe letra por letra.
            //                  [EN] true: the text is typed letter by letter.
            // velocidad        [ES] letras por segundo del efecto anterior (1 a 200).
            //                  [EN] letters per second for the effect above (1 to 200).
            // bucle            [ES] true: el audio y los subtitulos se repiten hasta usar stop.
            //                  [EN] true: audio and subtitles loop until you use stop.
            // volumen          [ES] volumen de este audio, de 0 a 1 (se suma a los sliders del juego).
            //                  [EN] volume of this audio, from 0 to 1 (on top of the game sliders).
            // color            [ES] color del texto. Por defecto blanco.
            //                  [EN] text color. White by default.
            // color_nombre     [ES] color de los nombres. Si no lo pones, usan el mismo color del texto.
            //                  [EN] color of the names. If missing, they use the text color.
            // [Nombre] = color [ES] color propio de un personaje: toda su linea (nombre y texto).
            //                  [EN] a character's own color: their whole line (name and text).
            //
            // [ES] COLORES: blanco, gris, gris_oscuro, negro, rojo, rojo_oscuro, naranja, dorado,
            //      amarillo, verde, verde_oscuro, celeste, cian, azul, azul_oscuro, morado, magenta, rosa.
            //      Tambien cualquier color con codigo, por ejemplo #FF8800.
            //      Prioridad: color del personaje, luego color_nombre, luego color.
            // [EN] COLORS: white, gray, darkgray, black, red, darkred, orange, gold, yellow,
            //      green, darkgreen, aqua, cyan, blue, darkblue, purple, magenta, pink.
            //      Any hex color also works, for example #FF8800.
            //      Priority: character color, then color_nombre, then color.
            //
            // [ES] Para activar una opcion comentada, borra el // del inicio.
            // [EN] To enable a commented option, remove the leading //.
            // =====================================================================

            // ---------- OPCIONES / OPTIONS ----------
            fade = false
            letra_por_letra = false
            velocidad = 30
            bucle = false
            volumen = 1.0
            color = blanco
            // color_nombre = amarillo
            // [Narrador] = celeste
            // [Maria] = #FF69B4

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