## ![](src/main/resources/assets/imagenes/imagenlog.png)

**Easy Subtitle** is a Minecraft mod that adds support for synchronized subtitles and audio playback, allowing you to display timed dialogue, character lines, and other text directly on the screen.

The mod is designed for map makers, server developers, content creators, and anyone who wants to create more immersive experiences in Minecraft, such as cinematic scenes, story-driven adventures, character dialogue, music performances, or interactive events.

## ![](src/main/resources/assets/imagenes/features.png)

- **Synchronized subtitles:** Display text at specific timestamps, synchronized with an audio file.
- **Built-in audio playback:** Play an OGG audio file alongside its subtitles.
- **Multiple subtitle formats:** Supports custom `.lrc` and `.json` subtitle files `Recommended .lrc format`.
- **Character names:** Assign names to subtitle lines to identify speakers.
- **Custom colors:** Customize subtitle text and character name colors using predefined colors or hexadecimal color codes.
- **Fade effects:** Enable gradual subtitle appearance and disappearance.
- **Typewriter effect:** Display text one character at a time with adjustable typing speed.
- **Loop playback:** Repeat audio and subtitles until stopped.
- **Subtitle positioning:** Move subtitles on the screen using a configurable keybind.
- **Playback controls:** Start, stop, reload, and seek through subtitle playback.
- **Flexible timing:** If no audio file is provided, subtitles can run using an internal clock.

## ![](src/main/resources/assets/imagenes/GettingStartet.png)

When you launch Minecraft, Easy Subtitle creates a `SubsSounds` folder inside your `.minecraft` directory. Inside this folder, you can organize your audio and subtitle files into separate folders.

Each subtitle folder can contain an audio file named `sound.ogg` and a subtitle file named `sub.lrc` or `sub.json`. If both subtitle files exist, `sub.json` takes priority.

Once your files are ready, use the `/subtitulo play` command to start playback for the selected players.

Easy Subtitle makes it simple to add timed dialogue and audio-based storytelling to Minecraft without manually displaying every subtitle in-game.

![](src/main/resources/assets/imagenes/HowToUse.png)

# Commands
- **Play Subtitle:**`/subtitulo play (user or @) (name)`
- **Stop Subtitle:**`/subtitulo stop (user or @) (name)`
- **Reload Subtitle**`/subtitulo reload`
- **Specific part of the sub**`/subtitulo seek (user or @) (nombre) (time)`
## Setting Up Custom Subtitles
# [EN]

When you launch Minecraft, Easy Subtitle automatically creates a `SubsSounds` folder inside your `.minecraft` directory.
# ![](src/main/resources/assets/imagenes/carpeta.png)

Inside `SubsSounds`, create a folder for each subtitle track you want to use. Give the folder a name of your choice. (The name must be in lowercase.) Inside that folder, add the audio file you want to play and name it `sound.ogg`.

You can convert your audio to OGG format using any online audio converter, or export it directly as an `.ogg` file from your audio editing software.

Next, create a file named `sub.lrc` in the same folder. You can edit this file using Notepad, Visual Studio Code, or any other text editor. This is where you will configure your subtitles, animations, colors, and synchronization.

Your folder structure should look like this:

```text
.minecraft/
└── SubsSounds/
    └── MySubtitle/
        ├── sound.ogg
        └── sub.lrc
```

Replace `MySubtitle` with the name you want to use for your subtitle track.

## Subtitle Configuration

At the beginning of your `sub.lrc` file, you can configure the following options.

### Animations

**Fade effect**

```properties
fade = true
```

Set this to `false` to disable the fade effect.

**Typewriter effect**

```properties
letra_por_letra = true
```

Set this to `false` to disable the typewriter effect, which displays the text one character at a time.

You can adjust the typing speed using the following option:

```properties
velocidad = 30
```

Change the value to adjust the speed of the typewriter effect.

### Loop Playback

To repeat the audio and subtitles continuously, enable looping:

```properties
bucle = true
```

Set this to `false` if you do not want playback to loop. When looping is enabled, playback will continue until you stop it manually.

### Volume

Adjust the audio playback volume using:

```properties
volumen = 1.0
```

For example, `1.0` represents the default volume level. You can change this value to adjust the playback volume.

### Text Colors

To change the color of the subtitle text, use:

```properties
color = blanco
```

To change the color of the speaker's name, use:

```properties
color_nombre = amarillo
```

Easy Subtitle supports the following predefined colors:

- `blanco`
- `gris`
- `gris_oscuro`
- `negro`
- `rojo`
- `rojo_oscuro`
- `naranja`
- `dorado`
- `amarillo`
- `verde`
- `verde_oscuro`
- `celeste`
- `cian`
- `azul`
- `azul_oscuro`
- `morado`
- `magenta`
- `rosa`

You can also use hexadecimal color codes, such as `#FF8800`, to choose a custom color.

### Complete Configuration Example

Here is an example of a complete configuration for your `sub.lrc` file:

```properties
fade = false
letra_por_letra = false
velocidad = 30
bucle = false
volumen = 1.0
color = blanco
color_nombre = blanco
```

You can modify these values according to your preferences.

## Subtitle Synchronization

After configuring the options, you can add your subtitle lines below them. Each line can contain a timestamp, an optional speaker name, and the text you want to display.

The timestamp determines when the subtitle appears during audio playback.

### Understanding Timestamps

Easy Subtitle supports timestamps such as:

```text
[00:07.27]
[00:02:00]
[01:00.00]
```

Timestamps use the following format:

```text
[minutes:seconds.centiseconds]
```

For example:

- `00` before the first colon represents the minutes.
- `07` after the first colon represents the seconds.
- `27` after the decimal point represents the centiseconds.

A timestamp such as `[01:04.00]` means 1 minute and 4 seconds.

**Note:** Timestamps can use different formats, depending on the precision and duration required. For example, `[00:07.27]` represents 7.27 seconds, while `[00:02:00]` represents 2 minutes.

### Adding Subtitle Text

You can optionally add the name of the person speaking before the subtitle text. This is useful for dialogues, narrated scenes, or situations where multiple characters speak.

For example:

```text
[00:10.00] [Narrator] Wait, let me check.
[00:15.00] [Narrator] It's right here.
```

If you do not want to display a speaker name, simply write the text after the timestamp:

```text
[00:10.00] Wait, let me check.
```

Both formats are supported:

- `[minutes:seconds.centiseconds] Text`
- `[minutes:seconds.centiseconds] [Name] Text`

### Empty Subtitle Lines

If a timestamp has no text after it, no subtitle will be displayed at that timestamp. This can be useful when you want to create pauses between dialogue lines.

For example:

```text
[00:10.00] [Narrator] Wait, let me check.
[00:12.00]
[00:15.00] [Narrator] It's right here.
```

In this example, the line at `00:12.00` contains no subtitle text.

### Testing Your Subtitles

Once your file is ready, you can test your subtitles in-game using the mod's commands. You can make changes to your subtitle file and reload it to test different timings, text, and configuration options.

This makes it easier to fine-tune your subtitles without having to prepare the entire sequence perfectly before testing it.

## Using Existing LRC Files

You can also use existing `.lrc` lyric files instead of creating every timestamp manually. This is especially useful for songs, since many lyric websites provide lyrics with timestamps already included.

For example, you may find timed lyrics on websites such as Lyricsify.

Easy Subtitle ignores metadata tags such as `[ar:...]` and `[ti:...]`, as well as word-level timestamps such as `<00:10.50>`.

This allows you to reuse compatible LRC files and adapt them for your own Minecraft projects.

## Additional Resources

That's everything you need to get started with Easy Subtitle!

If you prefer a visual guide, we also have tutorial videos explaining how to set everything up and use the mod. Feel free to check them out if you need additional help.

# [ES]

Cuando inicies Minecraft, Easy Subtitle creará automáticamente una carpeta llamada `SubsSounds` dentro del directorio `.minecraft`.
# ![](src/main/resources/assets/imagenes/carpeta.png)
Dentro de `SubsSounds`, crea una carpeta para cada pista de subtítulos que quieras utilizar. Puedes darle el nombre que prefieras. (El Nombre tiene que ser en minusculas) Dentro de esa carpeta, agrega el archivo de audio que quieras reproducir y nómbralo `sound.ogg`.

Puedes convertir tu audio al formato OGG utilizando cualquier convertidor de audio en línea, o exportarlo directamente como un archivo `.ogg` desde tu programa de edición de audio.

A continuación, crea un archivo llamado `sub.lrc` en la misma carpeta. Puedes editarlo con el Bloc de notas, Visual Studio Code o cualquier otro editor de texto. En este archivo configurarás los subtítulos, las animaciones, los colores y la sincronización.

La estructura de las carpetas debería verse así:

```text
.minecraft/
└── SubsSounds/
    └── MisSubtitulos/
        ├── sound.ogg
        └── sub.lrc
```

Reemplaza `MisSubtitulos` por el nombre que quieras utilizar para tu pista de subtítulos.

## Configuración de los subtítulos

Al principio de tu archivo `sub.lrc`, puedes configurar las siguientes opciones.

### Animaciones

**Efecto de desvanecimiento**

```properties
fade = true
```

Cambia el valor a `false` si no quieres utilizar el efecto de desvanecimiento.

**Efecto de escritura letra por letra**

```properties
letra_por_letra = true
```

Cambia el valor a `false` para desactivar este efecto, que muestra el texto un carácter a la vez.

Puedes ajustar la velocidad de escritura mediante la siguiente opción:

```properties
velocidad = 30
```

Modifica el valor para ajustar la velocidad del efecto letra por letra.

### Reproducción en bucle

Para repetir continuamente el audio y los subtítulos, activa el bucle:

```properties
bucle = true
```

Cambia el valor a `false` si no quieres que la reproducción se repita. Cuando el bucle está activado, la reproducción continuará hasta que la detengas manualmente.

### Volumen

Puedes ajustar el volumen del audio mediante la siguiente opción:

```properties
volumen = 1.0
```

Por ejemplo, `1.0` representa el nivel de volumen predeterminado. Puedes modificar este valor para ajustar el volumen de reproducción.

### Colores del texto

Para cambiar el color del texto de los subtítulos, utiliza:

```properties
color = blanco
```

Para cambiar el color del nombre del personaje o de la persona que habla, utiliza:

```properties
color_nombre = amarillo
```

Easy Subtitle admite los siguientes colores predefinidos:

- `blanco`
- `gris`
- `gris_oscuro`
- `negro`
- `rojo`
- `rojo_oscuro`
- `naranja`
- `dorado`
- `amarillo`
- `verde`
- `verde_oscuro`
- `celeste`
- `cian`
- `azul`
- `azul_oscuro`
- `morado`
- `magenta`
- `rosa`

También puedes utilizar códigos de color hexadecimales para elegir un color personalizado. Por ejemplo: `#FF8800`.

### Ejemplo de configuración completa

Este es un ejemplo de cómo podría quedar la configuración de tu archivo `sub.lrc`:

```properties
fade = false
letra_por_letra = false
velocidad = 30
bucle = false
volumen = 1.0
color = blanco
color_nombre = blanco
```

Puedes modificar estos valores según tus preferencias.

## Sincronización de los subtítulos

Después de configurar las opciones, puedes agregar las líneas de tus subtítulos debajo de ellas. Cada línea puede contener una marca de tiempo, un nombre opcional para quien habla y el texto que quieres mostrar.

La marca de tiempo determina cuándo aparecerá el subtítulo durante la reproducción del audio.

### Cómo funcionan las marcas de tiempo

Easy Subtitle admite marcas de tiempo como las siguientes:

```text
[00:07.27]
[00:02:00]
[01:00.00]
```

El formato de las marcas de tiempo es el siguiente:

```text
[minutos:segundos.centésimas]
```

Por ejemplo:

- `00` antes de los dos puntos representa los minutos.
- `07` después de los primeros dos puntos representa los segundos.
- `27` después del punto representa las centésimas de segundo.

Una marca de tiempo como `[01:04.00]` representa 1 minuto y 4 segundos.

**Nota:** Las marcas de tiempo pueden utilizar distintos formatos, dependiendo de la precisión y la duración necesarias. Por ejemplo, `[00:07.27]` representa 7,27 segundos, mientras que `[00:02:00]` representa 2 minutos.

### Cómo agregar texto a los subtítulos

Puedes añadir opcionalmente el nombre de la persona que habla antes del texto del subtítulo. Esto resulta útil para diálogos, escenas narradas o situaciones en las que hablan varios personajes.

Por ejemplo:

```text
[00:10.00] [Narrador] Espera, déjame revisar.
[00:15.00] [Narrador] Está justo aquí.
```

Si no quieres mostrar el nombre de quien habla, simplemente escribe el texto después de la marca de tiempo:

```text
[00:10.00] Espera, déjame revisar.
```

Ambos formatos son compatibles:

- `[minutos:segundos.centésimas] Texto`
- `[minutos:segundos.centésimas] [Nombre] Texto`

### Líneas de subtítulos vacías

Si una marca de tiempo no tiene texto después de ella, no se mostrará ningún subtítulo en ese momento. Esto puede ser útil para crear pausas entre las líneas de un diálogo.

Por ejemplo:

```text
[00:10.00] [Narrador] Espera, déjame revisar.
[00:12.00]
[00:15.00] [Narrador] Está justo aquí.
```

En este ejemplo, la línea correspondiente al segundo 12 no contiene texto, por lo que no se mostrará ningún subtítulo en ese momento.

### Cómo probar tus subtítulos

Una vez que tu archivo esté listo, puedes probar los subtítulos dentro del juego utilizando los comandos del mod. También puedes modificar el archivo y volver a cargarlo para probar diferentes tiempos, textos y opciones de configuración.

De esta manera, podrás ajustar la sincronización y el aspecto de los subtítulos sin necesidad de preparar toda la secuencia a la perfección antes de probarla.

## Uso de archivos LRC existentes

También puedes utilizar archivos de letras `.lrc` existentes en lugar de crear manualmente cada marca de tiempo. Esto resulta especialmente útil para las canciones, ya que muchas páginas de letras ofrecen archivos con los tiempos de cada línea ya incluidos.

Por ejemplo, puedes encontrar letras sincronizadas en páginas como Lyricsify.

Easy Subtitle ignora etiquetas de metadatos como `[ar:...]` y `[ti:...]`, así como las marcas de tiempo por palabra, por ejemplo `<00:10.50>`.

Esto te permite reutilizar archivos LRC compatibles y adaptarlos a tus propios proyectos de Minecraft.

## Recursos adicionales

¡Eso es todo lo que necesitas para comenzar a utilizar Easy Subtitle!

Si prefieres una guía visual, también contamos con videos tutoriales que explican cómo configurar y utilizar el mod. Puedes verlos si necesitas ayuda adicional.
