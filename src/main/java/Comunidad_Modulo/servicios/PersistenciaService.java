package Comunidad_Modulo.servicios;

import Comunidad_Modulo.modelo.*;
import Modulo_Usuario.Clases.UsuarioComunidad;
import Modulo_Usuario.Clases.NivelJava;
import Comunidad_Modulo.enums.TipoTema;
import Comunidad_Modulo.enums.TipoSolucion;
import Comunidad_Modulo.enums.EstadoHilo;
import Comunidad_Modulo.controladores.ContextoSistema;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Servicio para manejar la persistencia de datos de comunidades en archivos .txt
 * Actúa como una base de datos simple para el módulo de comunidad
 */
public class PersistenciaService {

    // Rutas de los archivos de persistencia
    private static final String BASE_PATH = "src/main/java/Comunidad_Modulo/data/";
    private static final String ARCHIVO_COMUNIDADES = BASE_PATH + "comunidades.txt";
    private static final String ARCHIVO_USUARIOS_COMUNIDADES = BASE_PATH + "usuarios_comunidades.txt";
    private static final String ARCHIVO_FOROS = BASE_PATH + "foros.txt";
    private static final String ARCHIVO_CHATS = BASE_PATH + "chats.txt";
    private static final String ARCHIVO_GRUPOS_MIEMBROS = BASE_PATH + "grupos_miembros.txt";
    private static final String ARCHIVO_HILOS = BASE_PATH + "hilos.txt";
    private static final String ARCHIVO_SOLUCIONES = BASE_PATH + "soluciones.txt";
    private static final String ARCHIVO_RESPUESTAS = BASE_PATH + "respuestas.txt";
    private static final String ARCHIVO_COMENTARIOS = BASE_PATH + "comentarios.txt";

    // Formato para fechas
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PersistenciaService() {
        // Crear directorio data si no existe
        crearDirectorioSiNoExiste();
    }

    /**
     * Crea el directorio de datos si no existe
     */
    private void crearDirectorioSiNoExiste() {
        try {
            Files.createDirectories(Paths.get(BASE_PATH));
        } catch (IOException e) {
            System.err.println("Error al crear directorio de datos: " + e.getMessage());
        }
    }

    // ========== PERSISTENCIA DE COMUNIDADES ==========

    /**
     * Guarda una comunidad en el archivo
     * Formato: nombre;descripcion;fechaCreacion;moderador
     */
    public void guardarComunidad(Comunidad comunidad) {
        try {
            String linea = String.format("%s;%s;%s;%s%n",
                    comunidad.getNombre(),
                    comunidad.getDescripcion().replace(";", "SEMICOLON"),
                    comunidad.getFechaCreacion().format(FORMATO_FECHA),
                    comunidad.getModerador().getNombre()
            );

            // Verificar si ya existe
            if (!existeComunidad(comunidad.getNombre())) {
                escribirArchivo(ARCHIVO_COMUNIDADES, linea, true);
                System.out.println("✅ Comunidad '" + comunidad.getNombre() + "' guardada en persistencia");
            }

        } catch (Exception e) {
            System.err.println("Error al guardar comunidad: " + e.getMessage());
        }
    }

    /**
     * Verifica si una comunidad ya existe en el archivo
     */
    public boolean existeComunidad(String nombreComunidad) {
        try {
            List<String> lineas = leerArchivo(ARCHIVO_COMUNIDADES);
            return lineas.stream()
                    .anyMatch(linea -> linea.startsWith(nombreComunidad + ";"));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Carga todas las comunidades desde el archivo
     */
    public List<Comunidad> cargarComunidades() {
        List<Comunidad> comunidades = new ArrayList<>();

        try {
            List<String> lineas = leerArchivo(ARCHIVO_COMUNIDADES);

            for (String linea : lineas) {
                // Ignorar líneas vacías y comentarios
                if (!linea.trim().isEmpty() && !linea.startsWith("#")) {
                    String[] partes = linea.split(";");
                    if (partes.length >= 4) {
                        String nombre = partes[0];
                        String descripcion = partes[1].replace("SEMICOLON", ";");
                        // String fechaStr = partes[2]; // Por ahora no necesitamos cargar la fecha
                        String nombreModerador = partes[3];

                        // Crear comunidad
                        Comunidad comunidad = new Comunidad(nombre, descripcion);

                        // Asignar moderador personalizado si es diferente al automático
                        if (!nombreModerador.equals("ModeradorBot_" + nombre)) {
                            ModeradorManual moderadorPersonalizado = new ModeradorManual(nombreModerador);
                            moderadorPersonalizado.asignarComunidad(comunidad);
                            // La comunidad ya tiene un moderador automático, así que lo mantenemos
                        }

                        // Cargar usuarios miembros de la comunidad
                        List<UsuarioComunidad> miembros = cargarUsuariosDeComunidad(nombre);
                        for (UsuarioComunidad miembro : miembros) {
                            comunidad.agregarUsuarioMiembro(miembro);
                        }

                        comunidades.add(comunidad);
                    }
                }
            }

            System.out.println("✅ Cargadas " + comunidades.size() + " comunidades desde persistencia");

        } catch (Exception e) {
            System.err.println("Error al cargar comunidades: " + e.getMessage());
        }

        return comunidades;
    }

    // ========== PERSISTENCIA DE USUARIOS EN COMUNIDADES ==========

    /**
     * Guarda la conexión de un usuario a una comunidad
     * Formato: nombreComunidad;usernameUsuario;nombreUsuario;nivelJava;reputacion
     */
    public void guardarUsuarioEnComunidad(String nombreComunidad, UsuarioComunidad usuario) {
        try {
            System.out.println(" Guardando usuario '" + usuario.getUsername() + "' en comunidad '" + nombreComunidad + "'");

            String linea = String.format("%s;%s;%s;%s;%d%n",
                    nombreComunidad,
                    usuario.getUsername(),
                    usuario.getNombre(),
                    usuario.getNivelJava().toString(),
                    usuario.getReputacion()
            );

            // Verificar si ya existe esta conexión
            boolean yaExiste = existeUsuarioEnComunidad(nombreComunidad, usuario.getUsername());
            System.out.println("   ¿Ya existe?: " + yaExiste);

            if (!yaExiste) {
                escribirArchivo(ARCHIVO_USUARIOS_COMUNIDADES, linea, true);
                System.out.println("✅ Usuario guardado en persistencia");

                // Verificar que se guardó correctamente
                boolean verificacion = existeUsuarioEnComunidad(nombreComunidad, usuario.getUsername());
                System.out.println("   Verificación post-guardado: " + verificacion);
            } else {
                System.out.println("⚠️ Usuario ya existe en la comunidad, no se guardó duplicado");
            }

        } catch (Exception e) {
            System.err.println("❌ Error al guardar usuario en comunidad: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Elimina la conexión de un usuario a una comunidad
     */
    public void eliminarUsuarioDeComunidad(String nombreComunidad, String username) {
        try {
            List<String> lineas = leerArchivo(ARCHIVO_USUARIOS_COMUNIDADES);
            List<String> lineasFiltradas = new ArrayList<>();

            for (String linea : lineas) {
                String[] partes = linea.split(";");
                if (partes.length >= 2) {
                    // Mantener todas las líneas excepto la que coincida
                    if (!(partes[0].equals(nombreComunidad) && partes[1].equals(username))) {
                        lineasFiltradas.add(linea);
                    }
                }
            }

            // Reescribir archivo sin la línea eliminada
            escribirTodasLasLineas(ARCHIVO_USUARIOS_COMUNIDADES, lineasFiltradas);
            System.out.println("✅ Usuario '" + username + "' desconectado de '" + nombreComunidad + "' eliminado de persistencia");

        } catch (Exception e) {
            System.err.println("Error al eliminar usuario de comunidad: " + e.getMessage());
        }
    }

    /**
     * Verifica si un usuario ya está conectado a una comunidad
     */
    public boolean existeUsuarioEnComunidad(String nombreComunidad, String username) {
        try {
            List<String> lineas = leerArchivo(ARCHIVO_USUARIOS_COMUNIDADES);
            return lineas.stream()
                    .anyMatch(linea -> {
                        String[] partes = linea.split(";");
                        return partes.length >= 2 &&
                                partes[0].equals(nombreComunidad) &&
                                partes[1].equals(username);
                    });
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Carga todos los usuarios conectados a una comunidad específica
     */
    public List<UsuarioComunidad> cargarUsuariosDeComunidad(String nombreComunidad) {
        List<UsuarioComunidad> usuarios = new ArrayList<>();

        try {
            List<String> lineas = leerArchivo(ARCHIVO_USUARIOS_COMUNIDADES);

            for (String linea : lineas) {
                // Ignorar líneas vacías y comentarios
                if (!linea.trim().isEmpty() && !linea.startsWith("#")) {
                    String[] partes = linea.split(";");
                    if (partes.length >= 5 && partes[0].equals(nombreComunidad)) {
                        String username = partes[1];
                        String nombre = partes[2];
                        String nivelDescripcion = partes[3];
                        int reputacion = Integer.parseInt(partes[4]);

                        // Convertir descripción a enum
                        NivelJava nivel;
                        try {
                            // Primero intentar como nombre del enum
                            nivel = NivelJava.valueOf(nivelDescripcion);
                        } catch (IllegalArgumentException e) {
                            // Si falla, intentar como descripción
                            nivel = NivelJava.fromDescripcion(nivelDescripcion);
                        }

                        // Crear usuario
                        UsuarioComunidad usuario = new UsuarioComunidad(username, "", nombre, "");
                        usuario.setIdUsuario(username);
                        usuario.setNivelJava(nivel);
                        usuario.setReputacion(reputacion);

                        usuarios.add(usuario);
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error al cargar usuarios de comunidad: " + e.getMessage());
            e.printStackTrace();
        }

        return usuarios;
    }

    // ========== PERSISTENCIA DE FOROS ==========

    /**
     * Guarda un grupo de discusión o compartir
     * Formato: nombreComunidad;tipoGrupo;titulo;nivelJava;tipoTema;creador
     */
    public void guardarGrupoForo(String nombreComunidad, String tipoGrupo, String titulo,
                                 NivelJava nivel, TipoTema tema, String creador) {
        try {
            String linea = String.format("%s;%s;%s;%s;%s;%s%n",
                    nombreComunidad,
                    tipoGrupo, // "DISCUSION" o "COMPARTIR"
                    titulo,
                    nivel.toString(),
                    tema.toString(),
                    creador
            );

            escribirArchivo(ARCHIVO_FOROS, linea, true);
            System.out.println("✅ Grupo de " + tipoGrupo.toLowerCase() + " '" + titulo + "' guardado en persistencia");

        } catch (Exception e) {
            System.err.println("Error al guardar grupo de foro: " + e.getMessage());
        }
    }

    /**
     * Carga los grupos de foro de una comunidad
     */
    public void cargarGruposDeForo(String nombreComunidad, ForoGeneral foro) {
        try {
            List<String> lineas = leerArchivo(ARCHIVO_FOROS);
            int gruposCargados = 0;

            for (String linea : lineas) {
                // Ignorar líneas vacías y comentarios
                if (!linea.trim().isEmpty() && !linea.startsWith("#")) {
                    String[] partes = linea.split(";");
                    if (partes.length >= 6 && partes[0].equals(nombreComunidad)) {
                        String tipoGrupo = partes[1];
                        String titulo = partes[2];
                        String nivelDesc = partes[3];
                        String temaDesc = partes[4];
                        // String creador = partes[5]; // Para futuras funcionalidades

                        // Convertir descripciones a enums de forma robusta
                        NivelJava nivel;
                        TipoTema tema;

                        try {
                            // Intentar primero como nombre del enum
                            nivel = NivelJava.valueOf(nivelDesc);
                        } catch (IllegalArgumentException e) {
                            // Si falla, intentar como descripción
                            nivel = NivelJava.fromDescripcion(nivelDesc);
                        }

                        try {
                            // Intentar primero como nombre del enum
                            tema = TipoTema.valueOf(temaDesc);
                        } catch (IllegalArgumentException e) {
                            // Si falla, convertir descripción
                            tema = TipoTema.fromDescripcion(temaDesc);
                        }

                        // Crear el grupo correspondiente
                        if ("DISCUSION".equals(tipoGrupo)) {
                            foro.crearGrupoDiscusion(titulo, nivel, tema);
                            gruposCargados++;
                        } else if ("COMPARTIR".equals(tipoGrupo)) {
                            foro.crearGrupoCompartir(titulo, nivel, tema);
                            gruposCargados++;
                        }
                    }
                }
            }

            if (gruposCargados > 0) {
                System.out.println("✅ Cargados " + gruposCargados + " grupos de foro para comunidad: " + nombreComunidad);
            }

        } catch (Exception e) {
            System.err.println("Error al cargar grupos de foro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========== PERSISTENCIA DE CHATS PRIVADOS ==========

    /**
     * Guarda un chat privado
     * Formato: nombreComunidad;participantes(separados por ,);fechaCreacion
     */
    public void guardarChatPrivado(String nombreComunidad, ChatPrivado chat) {
        try {
            StringBuilder participantes = new StringBuilder();
            for (int i = 0; i < chat.getParticipantes().size(); i++) {
                participantes.append(chat.getParticipantes().get(i).getUsername());
                if (i < chat.getParticipantes().size() - 1) {
                    participantes.append(",");
                }
            }

            String linea = String.format("%s;%s;%s%n",
                    nombreComunidad,
                    participantes.toString(),
                    chat.getFechaCreacion().format(FORMATO_FECHA)
            );

            escribirArchivo(ARCHIVO_CHATS, linea, true);
            System.out.println("✅ Chat privado con " + chat.getParticipantes().size() + " participantes guardado en persistencia");

        } catch (Exception e) {
            System.err.println("Error al guardar chat privado: " + e.getMessage());
        }
    }

    /**
     * Carga los chats privados de una comunidad
     */
    public void cargarChatsDeComunidad(String nombreComunidad, Comunidad comunidad) {
        try {
            List<String> lineas = leerArchivo(ARCHIVO_CHATS);
            int chatsEncontrados = 0;
            System.out.println("🔍 Cargando chats para comunidad: " + nombreComunidad);

            for (String linea : lineas) {
                // Ignorar líneas vacías y comentarios
                if (!linea.trim().isEmpty() && !linea.startsWith("#")) {
                    String[] partes = linea.split(";");
                    if (partes.length >= 3 && partes[0].equals(nombreComunidad)) {
                        String[] usernames = partes[1].split(",");
                        System.out.println("🔍 Procesando chat con participantes: " + String.join(", ", usernames));

                        // Buscar usuarios en los miembros de la comunidad (no solo conectados)
                        List<UsuarioComunidad> participantes = new ArrayList<>();
                        for (String username : usernames) {
                            for (UsuarioComunidad usuario : comunidad.getUsuariosMiembros()) {
                                if (usuario.getUsername().equals(username.trim())) {
                                    participantes.add(usuario);
                                    break;
                                }
                            }
                        }

                        // Solo crear el chat si encontramos a todos los participantes
                        if (participantes.size() == usernames.length && participantes.size() >= 2) {
                            ChatPrivado chat = new ChatPrivado(participantes);
                            comunidad.agregarChatPrivado(chat);
                            chatsEncontrados++;
                            System.out.println("✅ Chat cargado con " + participantes.size() + " participantes");
                        } else {
                            System.out.println("⚠️ Chat no cargado - participantes encontrados: " + participantes.size() + "/" + usernames.length);
                        }
                    }
                }
            }

            System.out.println("✅ Total chats cargados para " + nombreComunidad + ": " + chatsEncontrados);

        } catch (Exception e) {
            System.err.println("Error al cargar chats de comunidad: " + e.getMessage());
        }
    }


    // ===== MÉTODOS DE PERSISTENCIA DE MEMBRESÍA DE GRUPOS =====

    /**
     * Guarda la membresía de un usuario a un grupo
     */
    public static void guardarMiembroGrupo(String nombreComunidad, String tipoGrupo, String tituloGrupo, String username) {
        System.out.println("📝 Guardando membresía: " + username + " se unió al grupo " + tituloGrupo);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_GRUPOS_MIEMBROS, true))) {
            String fechaUnion = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String linea = String.join(";", nombreComunidad, tipoGrupo, tituloGrupo, username, fechaUnion);
            writer.write(linea);
            writer.newLine();
            System.out.println("✅ Membresía guardada exitosamente");
        } catch (IOException e) {
            System.err.println("❌ Error al guardar membresía: " + e.getMessage());
        }
    }

    /**
     * Verifica si un usuario es miembro de un grupo específico
     */
    public static boolean esUsuarioMiembroDelGrupo(String nombreComunidad, String tituloGrupo, String username) {
        try (BufferedReader reader = new BufferedReader(new FileReader(ARCHIVO_GRUPOS_MIEMBROS))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.trim().isEmpty() || linea.startsWith("#")) continue;

                String[] datos = linea.split(";");
                if (datos.length >= 4) {
                    String comunidad = datos[0];
                    String titulo = datos[2];
                    String user = datos[3];

                    if (comunidad.equals(nombreComunidad) && titulo.equals(tituloGrupo) && user.equals(username)) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("⚠️ Error al verificar membresía: " + e.getMessage());
        }
        return false;
    }

    /**
     * Obtiene todos los miembros de un grupo
     */
    public static List<String> obtenerMiembrosDelGrupo(String nombreComunidad, String tituloGrupo) {
        List<String> miembros = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(ARCHIVO_GRUPOS_MIEMBROS))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.trim().isEmpty() || linea.startsWith("#")) continue;

                String[] datos = linea.split(";");
                if (datos.length >= 4) {
                    String comunidad = datos[0];
                    String titulo = datos[2];
                    String username = datos[3];

                    if (comunidad.equals(nombreComunidad) && titulo.equals(tituloGrupo)) {
                        miembros.add(username);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("⚠️ Error al obtener miembros del grupo: " + e.getMessage());
        }

        return miembros;
    }

    // ===== MÉTODOS DE PERSISTENCIA DE HILOS Y SOLUCIONES =====

    /**
     * Guarda un hilo de discusión
     * Formato: nombreComunidad;tituloGrupo;idHilo;titulo;contenido;autor;fechaCreacion;estado;votosUsuarios
     */
    public static String guardarHiloDiscusion(String nombreComunidad, String tituloGrupo, String tituloHilo, String contenido, String autor, EstadoHilo estado, Map<String, Integer> votosUsuarios) {
        String idHilo = generarIdUnico("HILO");
        System.out.println("📝 Guardando hilo de discusión: " + tituloHilo + " (ID: " + idHilo + ")");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_HILOS, true))) {
            String fechaCreacion = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String votosStr = votosUsuarios.entrySet().stream()
                    .map(entry -> entry.getKey() + ":" + entry.getValue())
                    .collect(Collectors.joining(","));

            String linea = String.join(";", nombreComunidad, tituloGrupo, idHilo, tituloHilo, contenido, autor, fechaCreacion, estado.toString(), votosStr);
            writer.write(linea);
            writer.newLine();
            System.out.println("✅ Hilo guardado exitosamente con ID: " + idHilo);
        } catch (IOException e) {
            System.err.println("❌ Error al guardar hilo: " + e.getMessage());
        }

        return idHilo;
    }

    /**
     * Actualiza un hilo existente en el archivo de persistencia.
     * Formato: nombreComunidad;tituloGrupo;idHilo;titulo;contenido;autor;fechaCreacion;estado;votosUsuarios
     */
    public static void actualizarHilo(String nombreComunidad, String tituloGrupo, HiloDiscusion hilo) {
        try {
            List<String> lineas = leerArchivo(ARCHIVO_HILOS);
            List<String> nuevasLineas = new ArrayList<>();
            boolean encontrada = false;

            String votosStr = hilo.getVotosUsuarios().entrySet().stream()
                    .map(entry -> entry.getKey() + ":" + entry.getValue())
                    .collect(Collectors.joining(","));

            String nuevaLinea = String.join(";",
                    nombreComunidad,
                    tituloGrupo,
                    hilo.getIdHilo(),
                    hilo.getTitulo(),
                    hilo.getProblema(),
                    hilo.getAutor().getUsername(),
                    hilo.getFechaCreacion().format(FORMATO_FECHA),
                    hilo.getEstado().toString(),
                    votosStr);

            for (String linea : lineas) {
                String[] datos = linea.split(";");
                if (datos.length >= 3 && datos[0].equals(nombreComunidad) && datos[1].equals(tituloGrupo) && datos[2].equals(hilo.getIdHilo())) {
                    nuevasLineas.add(nuevaLinea);
                    encontrada = true;
                } else {
                    nuevasLineas.add(linea);
                }
            }

            if (!encontrada) {
                // Esto no debería ocurrir si solo se usa para actualizar, pero como fallback
                nuevasLineas.add(nuevaLinea);
            }

            escribirTodasLasLineas(ARCHIVO_HILOS, nuevasLineas);
            System.out.println("✅ Hilo '" + hilo.getTitulo() + "' actualizado en persistencia.");

        } catch (IOException e) {
            System.err.println("❌ Error al actualizar hilo: " + e.getMessage());
        }
    }


    /**
     * Guarda una solución compartida
     * Formato: nombreComunidad;tituloGrupo;idSolucion;titulo;contenido;autor;fechaCreacion;tipoSolucion;votosUsuarios
     */
    public static String guardarSolucionCompartida(String nombreComunidad, String tituloGrupo, String titulo, String contenido, String autor, String tipoSolucion, Map<String, Integer> votosUsuarios) {
        String idSolucion = generarIdUnico("SOL"); // Generar ID aquí
        System.out.println("📝 Guardando solución compartida: " + titulo + " (ID: " + idSolucion + ")");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_SOLUCIONES, true))) {
            String fechaCreacion = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String votosStr = votosUsuarios.entrySet().stream()
                    .map(entry -> entry.getKey() + ":" + entry.getValue())
                    .collect(Collectors.joining(","));

            String linea = String.join(";", nombreComunidad, tituloGrupo, idSolucion, titulo, contenido, autor, fechaCreacion, tipoSolucion, votosStr);
            writer.write(linea);
            writer.newLine();
            System.out.println("✅ Solución guardada exitosamente con ID: " + idSolucion);
        } catch (IOException e) {
            System.err.println("❌ Error al guardar solución: " + e.getMessage());
        }

        return idSolucion;
    }

    /**
     * Actualiza una solución existente en el archivo de persistencia.
     */
    public static void actualizarSolucion(String nombreComunidad, String tituloGrupo, Solucion solucion) {
        try {
            List<String> lineas = leerArchivo(ARCHIVO_SOLUCIONES);
            List<String> nuevasLineas = new ArrayList<>();
            boolean encontrada = false;

            String votosStr = solucion.getVotosUsuarios().entrySet().stream()
                    .map(entry -> entry.getKey() + ":" + entry.getValue())
                    .collect(Collectors.joining(","));

            String nuevaLinea = String.join(";",
                    nombreComunidad,
                    tituloGrupo,
                    solucion.getIdSolucion(),
                    solucion.getTitulo(),
                    solucion.getContenido(),
                    solucion.getAutor().getUsername(),
                    solucion.getFechaPublicacion().format(FORMATO_FECHA),
                    solucion.getTipoSolucion().getDescripcion(),
                    votosStr);

            for (String linea : lineas) {
                String[] datos = linea.split(";");
                if (datos.length >= 3 && datos[0].equals(nombreComunidad) && datos[1].equals(tituloGrupo) && datos[2].equals(solucion.getIdSolucion())) {
                    nuevasLineas.add(nuevaLinea);
                    encontrada = true;
                } else {
                    nuevasLineas.add(linea);
                }
            }

            if (!encontrada) {
                System.err.println("⚠️ Solución con ID '" + solucion.getIdSolucion() + "' no encontrada para actualizar. No se agregó nueva línea.");
                return;
            }

            escribirTodasLasLineas(ARCHIVO_SOLUCIONES, nuevasLineas);
            System.out.println("✅ Solución '" + solucion.getTitulo() + "' actualizada en persistencia.");

        } catch (IOException e) {
            System.err.println("❌ Error al actualizar solución: " + e.getMessage());
        }
    }

    /**
     * Guarda una respuesta a un hilo
     * Formato: nombreComunidad;tituloGrupo;idHilo;idRespuesta;contenido;autor;fechaCreacion;esSolucion;votosUsuarios
     */
    public static String guardarRespuestaHilo(String nombreComunidad, String tituloGrupo, String idHilo, String contenido, String autor, Map<String, Integer> votosUsuarios, boolean esSolucion) {
        String idRespuesta = generarIdUnico("RESP");
        System.out.println("📝 Guardando respuesta al hilo " + idHilo + " (ID: " + idRespuesta + ")");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_RESPUESTAS, true))) {
            String fechaCreacion = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String votosStr = votosUsuarios.entrySet().stream()
                    .map(entry -> entry.getKey() + ":" + entry.getValue())
                    .collect(Collectors.joining(","));

            String linea = String.join(";", nombreComunidad, tituloGrupo, idHilo, idRespuesta, contenido, autor, fechaCreacion, String.valueOf(esSolucion), votosStr);
            writer.write(linea);
            writer.newLine();
            System.out.println("✅ Respuesta guardada exitosamente con ID: " + idRespuesta);
        } catch (IOException e) {
            System.err.println("❌ Error al guardar respuesta: " + e.getMessage());
        }

        return idRespuesta;
    }

    /**
     * Actualiza una respuesta existente en el archivo de persistencia.
     * Formato: nombreComunidad;tituloGrupo;idHilo;idRespuesta;contenido;autor;fechaCreacion;esSolucion;votosUsuarios
     */
    public static void actualizarRespuesta(String nombreComunidad, String tituloGrupo, String idHilo, Respuesta respuesta) {
        try {
            List<String> lineas = leerArchivo(ARCHIVO_RESPUESTAS);
            List<String> nuevasLineas = new ArrayList<>();
            boolean encontrada = false;

            String votosStr = respuesta.getVotosUsuarios().entrySet().stream()
                    .map(entry -> entry.getKey() + ":" + entry.getValue())
                    .collect(Collectors.joining(","));

            String nuevaLinea = String.join(";",
                    nombreComunidad,
                    tituloGrupo,
                    idHilo,
                    respuesta.getIdRespuesta(),
                    respuesta.getContenido(),
                    respuesta.getAutor().getUsername(),
                    respuesta.getFechaPublicacion().format(FORMATO_FECHA),
                    String.valueOf(respuesta.getEsSolucion()),
                    votosStr);

            for (String linea : lineas) {
                String[] datos = linea.split(";");
                if (datos.length >= 4 && datos[0].equals(nombreComunidad) && datos[1].equals(tituloGrupo) && datos[2].equals(idHilo) && datos[3].equals(respuesta.getIdRespuesta())) {
                    nuevasLineas.add(nuevaLinea);
                    encontrada = true;
                } else {
                    nuevasLineas.add(linea);
                }
            }

            if (!encontrada) {
                System.err.println("⚠️ Respuesta con ID '" + respuesta.getIdRespuesta() + "' no encontrada para actualizar. Agregando como nueva línea.");
            }

            escribirTodasLasLineas(ARCHIVO_RESPUESTAS, nuevasLineas);
            System.out.println("✅ Respuesta '" + respuesta.getIdRespuesta() + "' actualizada en persistencia.");

        } catch (IOException e) {
            System.err.println("❌ Error al actualizar respuesta: " + e.getMessage());
        }
    }


    /**
     * Guarda un comentario a una solución.
     * Formato: nombreComunidad;tituloGrupo;idSolucion;idComentario;contenido;autor;fechaCreacion;votosUsuarios
     */
    public static String guardarComentarioSolucion(String nombreComunidad, String tituloGrupo, String idSolucion, String contenido, String autor, Map<String, Integer> votosUsuarios) {
        String idComentario = generarIdUnico("COM");
        System.out.println("📝 Guardando comentario a la solución " + idSolucion + " (ID: " + idComentario + ")");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ARCHIVO_COMENTARIOS, true))) {
            String fechaCreacion = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String votosStr = votosUsuarios.entrySet().stream()
                    .map(entry -> entry.getKey() + ":" + entry.getValue())
                    .collect(Collectors.joining(","));

            String linea = String.join(";", nombreComunidad, tituloGrupo, idSolucion, idComentario, contenido, autor, fechaCreacion, votosStr);
            writer.write(linea);
            writer.newLine();
            System.out.println("✅ Comentario guardado exitosamente con ID: " + idComentario);
        } catch (IOException e) {
            System.err.println("❌ Error al guardar comentario: " + e.getMessage());
        }

        return idComentario;
    }

    /**
     * Actualiza un comentario existente en el archivo de persistencia.
     */
    public static void actualizarComentario(String nombreComunidad, String tituloGrupo, String idSolucion, Comentario comentario) {
        try {
            List<String> lineas = leerArchivo(ARCHIVO_COMENTARIOS);
            List<String> nuevasLineas = new ArrayList<>();
            boolean encontrada = false;

            String votosStr = comentario.getVotosUsuarios().entrySet().stream()
                    .map(entry -> entry.getKey() + ":" + entry.getValue())
                    .collect(Collectors.joining(","));

            String nuevaLinea = String.join(";",
                    nombreComunidad,
                    tituloGrupo,
                    idSolucion,
                    comentario.getIdComentario(),
                    comentario.getContenido(),
                    comentario.getAutor().getUsername(),
                    comentario.getFechaPublicacion().format(FORMATO_FECHA),
                    votosStr);

            for (String linea : lineas) {
                String[] datos = linea.split(";");
                if (datos.length >= 4 && datos[0].equals(nombreComunidad) && datos[1].equals(tituloGrupo) && datos[2].equals(idSolucion) && datos[3].equals(comentario.getIdComentario())) {
                    nuevasLineas.add(nuevaLinea);
                    encontrada = true;
                } else {
                    nuevasLineas.add(linea);
                }
            }

            if (!encontrada) {
                System.err.println("⚠️ Comentario con ID '" + comentario.getIdComentario() + "' no encontrado para actualizar. Agregando como nueva línea.");
            }

            escribirTodasLasLineas(ARCHIVO_COMENTARIOS, nuevasLineas);
            System.out.println("✅ Comentario '" + comentario.getIdComentario() + "' actualizado en persistencia.");

        } catch (IOException e) {
            System.err.println("❌ Error al actualizar comentario: " + e.getMessage());
        }
    }

    /**
     * Elimina un comentario del archivo de persistencia.
     */
    public static void eliminarComentarioPersistido(String nombreComunidad, String tituloGrupo, String idSolucion, String idComentario) {
        try {
            List<String> lineas = leerArchivo(ARCHIVO_COMENTARIOS);
            List<String> nuevasLineas = new ArrayList<>();
            boolean eliminada = false;

            for (String linea : lineas) {
                String[] datos = linea.split(";");
                if (datos.length >= 4 && datos[0].equals(nombreComunidad) && datos[1].equals(tituloGrupo) && datos[2].equals(idSolucion) && datos[3].equals(idComentario)) {
                    eliminada = true;
                } else {
                    nuevasLineas.add(linea);
                }
            }

            if (eliminada) {
                escribirTodasLasLineas(ARCHIVO_COMENTARIOS, nuevasLineas);
                System.out.println("✅ Comentario '" + idComentario + "' eliminado de persistencia.");
            } else {
                System.out.println("⚠️ Comentario '" + idComentario + "' no encontrado para eliminar.");
            }

        } catch (IOException e) {
            System.err.println("❌ Error al eliminar comentario: " + e.getMessage());
        }
    }

    /**
     * Genera un ID único para hilos, soluciones o respuestas
     */
    private static String generarIdUnico(String prefijo) {
        return prefijo + "_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }

    /**
     * Obtiene todos los hilos de un grupo
     */
    public static List<Map<String, String>> obtenerHilosDelGrupo(String nombreComunidad, String tituloGrupo) {
        List<Map<String, String>> hilos = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(ARCHIVO_HILOS))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.trim().isEmpty() || linea.startsWith("#")) continue;

                String[] datos = linea.split(";");
                if (datos.length >= 9) {
                    String comunidad = datos[0];
                    String grupo = datos[1];

                    if (comunidad.equals(nombreComunidad) && grupo.equals(tituloGrupo)) {
                        Map<String, String> hilo = new HashMap<>();
                        hilo.put("id", datos[2]);
                        hilo.put("titulo", datos[3]);
                        hilo.put("contenido", datos[4]);
                        hilo.put("autor", datos[5]);
                        hilo.put("fecha", datos[6]);
                        hilo.put("estado", datos[7]);
                        hilo.put("votosUsuarios", datos[8]);
                        hilos.add(hilo);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("⚠️ Error al obtener hilos del grupo: " + e.getMessage());
        }

        return hilos;
    }

    /**
     * Obtiene todas las soluciones de un grupo
     */
    public static List<Map<String, String>> obtenerSolucionesDelGrupo(String nombreComunidad, String tituloGrupo) {
        List<Map<String, String>> soluciones = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(ARCHIVO_SOLUCIONES))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.trim().isEmpty() || linea.startsWith("#")) continue;

                String[] datos = linea.split(";");
                // Asegurarse de que haya al menos 8 partes para los datos básicos (sin votos)
                // y 9 si se incluyen los votos.
                if (datos.length >= 8) {
                    String comunidad = datos[0];
                    String grupo = datos[1];

                    if (comunidad.equals(nombreComunidad) && grupo.equals(tituloGrupo)) {
                        Map<String, String> solucion = new HashMap<>();
                        solucion.put("id", datos[2]);
                        solucion.put("titulo", datos[3]);
                        solucion.put("contenido", datos[4]);
                        solucion.put("autor", datos[5]);
                        solucion.put("fecha", datos[6]);
                        solucion.put("tipo", datos[7]);
                        // Si hay votos, se añaden. Si no, la clave estará ausente o vacía.
                        solucion.put("votosUsuarios", datos.length > 8 ? datos[8] : "");
                        soluciones.add(solucion);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("⚠️ Error al obtener soluciones del grupo: " + e.getMessage());
        }

        return soluciones;
    }

    /**
     * Obtiene todas las respuestas de un hilo específico
     * Formato: nombreComunidad;tituloGrupo;idHilo;idRespuesta;contenido;autor;fechaCreacion;esSolucion;votosUsuarios
     */
    public static List<Map<String, String>> obtenerRespuestasDelHilo(String nombreComunidad, String tituloGrupo, String idHilo) {
        List<Map<String, String>> respuestas = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(ARCHIVO_RESPUESTAS))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.trim().isEmpty() || linea.startsWith("#")) continue;

                String[] datos = linea.split(";");
                // Asegurarse de que haya al menos 8 partes para los datos básicos (sin votos)
                // y 9 si se incluyen los votos.
                if (datos.length >= 8) {
                    String comunidad = datos[0];
                    String grupo = datos[1];
                    String hiloId = datos[2];

                    if (comunidad.equals(nombreComunidad) && grupo.equals(tituloGrupo) && hiloId.equals(idHilo)) {
                        Map<String, String> respuesta = new HashMap<>();
                        respuesta.put("id", datos[3]);
                        respuesta.put("contenido", datos[4]);
                        respuesta.put("autor", datos[5]);
                        respuesta.put("fecha", datos[6]);
                        respuesta.put("esSolucion", datos[7]);
                        // Si hay votos, se añaden. Si no, la clave estará ausente o vacía.
                        respuesta.put("votosUsuarios", datos.length > 8 ? datos[8] : "");
                        respuestas.add(respuesta);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("⚠️ Error al obtener respuestas del hilo: " + e.getMessage());
        }

        return respuestas;
    }

    /**
     * Obtiene todos los comentarios de una solución específica.
     */
    public static List<Map<String, String>> obtenerComentariosDeSolucion(String nombreComunidad, String tituloGrupo, String idSolucion) {
        List<Map<String, String>> comentarios = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(ARCHIVO_COMENTARIOS))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.trim().isEmpty() || linea.startsWith("#")) continue;

                String[] datos = linea.split(";");
                // Asegurarse de que haya al menos 7 partes para los datos básicos (sin votos)
                // y 8 si se incluyen los votos.
                if (datos.length >= 7) {
                    String comunidad = datos[0];
                    String grupo = datos[1];
                    String solucionId = datos[2];

                    if (comunidad.equals(nombreComunidad) && grupo.equals(tituloGrupo) && solucionId.equals(idSolucion)) {
                        Map<String, String> comentario = new HashMap<>();
                        comentario.put("id", datos[3]);
                        comentario.put("contenido", datos[4]);
                        comentario.put("autor", datos[5]);
                        comentario.put("fecha", datos[6]);
                        // Si hay votos, se añaden. Si no, la clave estará ausente o vacía.
                        comentario.put("votosUsuarios", datos.length > 7 ? datos[7] : "");
                        comentarios.add(comentario);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("⚠️ Error al obtener comentarios de la solución: " + e.getMessage());
        }
        return comentarios;
    }

    // ===== MÉTODOS DE CARGA DE DATOS PERSISTIDOS =====

    /**
     * Carga las membresías persistidas y las aplica a los grupos en memoria
     */
    public static void cargarMembresiasPersistidas(ForoGeneral foro, String nombreComunidad) {
        System.out.println("🔄 Cargando membresías persistidas para la comunidad: " + nombreComunidad);

        try (BufferedReader reader = new BufferedReader(new FileReader(ARCHIVO_GRUPOS_MIEMBROS))) {
            String linea;
            int membresiasCargadas = 0;

            while ((linea = reader.readLine()) != null) {
                if (linea.trim().isEmpty() || linea.startsWith("#")) continue;

                String[] datos = linea.split(";");
                if (datos.length >= 4) {
                    String comunidad = datos[0];
                    String tipoGrupo = datos[1];
                    String tituloGrupo = datos[2];
                    String username = datos[3];

                    if (comunidad.equals(nombreComunidad)) {
                        // Buscar el grupo correspondiente
                        if ("DISCUSION".equals(tipoGrupo)) {
                            for (GrupoDiscusion grupo : foro.getGruposDiscusion()) {
                                if (grupo.getTitulo().equals(tituloGrupo)) {
                                    // Buscar el usuario en el sistema
                                    UsuarioComunidad usuario = buscarUsuarioPorUsername(username);
                                    if (usuario != null) {
                                        // Acceder directamente a la lista privada de miembros usando reflexión
                                        try {
                                            java.lang.reflect.Field miembrosField = GrupoDiscusion.class.getDeclaredField("miembros");
                                            miembrosField.setAccessible(true);
                                            @SuppressWarnings("unchecked")
                                            List<UsuarioComunidad> miembrosReales = (List<UsuarioComunidad>) miembrosField.get(grupo);
                                            if (!miembrosReales.contains(usuario)) {
                                                miembrosReales.add(usuario);
                                                membresiasCargadas++;
                                                System.out.println("  ✅ Cargada membresía: " + username + " -> " + tituloGrupo);
                                            }
                                        } catch (Exception e) {
                                            System.err.println("⚠️ No se pudo acceder a la lista de miembros (discusión): " + e.getMessage());
                                        }
                                    }
                                    break;
                                }
                            }
                        } else if ("COMPARTIR".equals(tipoGrupo)) {
                            for (GrupoCompartir grupo : foro.getGruposCompartir()) {
                                if (grupo.getTitulo().equals(tituloGrupo)) {
                                    UsuarioComunidad usuario = buscarUsuarioPorUsername(username);
                                    if (usuario != null) {
                                        // Acceder directamente a la lista privada de miembros usando reflexión
                                        try {
                                            java.lang.reflect.Field miembrosField = GrupoCompartir.class.getDeclaredField("miembros");
                                            miembrosField.setAccessible(true);
                                            @SuppressWarnings("unchecked")
                                            List<UsuarioComunidad> miembrosReales = (List<UsuarioComunidad>) miembrosField.get(grupo);
                                            if (!miembrosReales.contains(usuario)) {
                                                miembrosReales.add(usuario);
                                                membresiasCargadas++;
                                                System.out.println("  ✅ Cargada membresía: " + username + " -> " + tituloGrupo);
                                            }
                                        } catch (Exception e) {
                                            System.err.println("⚠️ No se pudo acceder a la lista de miembros (compartir): " + e.getMessage());
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            System.out.println("✅ Cargadas " + membresiasCargadas + " membresías persistidas");

        } catch (IOException e) {
            System.err.println("⚠️ Error al cargar membresías persistidas: " + e.getMessage());
        }
    }

    /**
     * Carga los hilos persistidos y los agrega a los grupos de discusión
     */
    public static void cargarHilosPersistidos(ForoGeneral foro, String nombreComunidad) {
        System.out.println("🔄 Cargando hilos persistidos para la comunidad: " + nombreComunidad);

        try (BufferedReader reader = new BufferedReader(new FileReader(ARCHIVO_HILOS))) {
            String linea;
            int hilosCargados = 0;

            while ((linea = reader.readLine()) != null) {
                if (linea.trim().isEmpty() || linea.startsWith("#")) continue;

                String[] datos = linea.split(";");
                if (datos.length >= 8) {
                    String comunidad = datos[0];
                    String tituloGrupo = datos[1];
                    String idHilo = datos[2];
                    String tituloHilo = datos[3];
                    String contenido = datos[4];
                    String autorUsername = datos[5];
                    String fechaCreacionStr = datos[6];
                    String estadoStr = datos[7];
                    String votosUsuariosStr = (datos.length > 8) ? datos[8] : ""; // Leer votos si existen, sino cadena vacía

                    if (comunidad.equals(nombreComunidad)) {
                        // Buscar el grupo de discusión correspondiente
                        for (GrupoDiscusion grupo : foro.getGruposDiscusion()) {
                            if (grupo.getTitulo().equals(tituloGrupo)) {
                                // Verificar si el hilo ya existe (por ID)
                                boolean hiloExiste = grupo.getHilos().stream()
                                        .anyMatch(h -> h.getIdHilo().equals(idHilo));

                                if (!hiloExiste) {
                                    // Buscar el autor
                                    UsuarioComunidad autor = buscarUsuarioPorUsername(autorUsername);
                                    if (autor != null) {
                                        // Convertir estado
                                        EstadoHilo estado = EstadoHilo.fromDescripcion(estadoStr);
                                        LocalDateTime fechaCreacion = LocalDateTime.parse(fechaCreacionStr, FORMATO_FECHA);

                                        // Parsear votosUsuarios
                                        Map<String, Integer> votosUsuarios = new HashMap<>();
                                        if (!votosUsuariosStr.isEmpty()) {
                                            for (String votoEntry : votosUsuariosStr.split(",")) {
                                                String[] parts = votoEntry.split(":");
                                                if (parts.length == 2) {
                                                    votosUsuarios.put(parts[0], Integer.parseInt(parts[1]));
                                                }
                                            }
                                        }

                                        // Crear el hilo usando el constructor de carga
                                        HiloDiscusion hilo = new HiloDiscusion(idHilo, tituloHilo, contenido, autor, estado, votosUsuarios, fechaCreacion);

                                        // Acceder directamente a la lista privada de hilos usando reflexión
                                        try {
                                            java.lang.reflect.Field hilosField = GrupoDiscusion.class.getDeclaredField("hilos");
                                            hilosField.setAccessible(true);
                                            @SuppressWarnings("unchecked")
                                            List<HiloDiscusion> hilosReales = (List<HiloDiscusion>) hilosField.get(grupo);
                                            hilosReales.add(hilo);
                                        } catch (Exception e) {
                                            System.err.println("⚠️ No se pudo acceder a la lista de hilos: " + e.getMessage());
                                            // Fallback: intentar usando el método normal (aunque sea una copia)
                                            grupo.getHilos().add(hilo);
                                        }

                                        hilosCargados++;
                                        System.out.println("  ✅ Cargado hilo: " + tituloHilo + " (ID: " + idHilo + ")");

                                        // Cargar respuestas para este hilo
                                        cargarRespuestasDelHilo(hilo, nombreComunidad, tituloGrupo, idHilo);
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }

            System.out.println("✅ Cargados " + hilosCargados + " hilos persistidos");

        } catch (IOException e) {
            System.err.println("⚠️ Error al cargar hilos persistidos: " + e.getMessage());
        }
    }

    /**
     * Carga las respuestas persistidas para un hilo específico
     * Formato: nombreComunidad;tituloGrupo;idHilo;idRespuesta;contenido;autor;fechaCreacion;esSolucion;votosUsuarios
     */
    private static void cargarRespuestasDelHilo(HiloDiscusion hilo, String nombreComunidad, String tituloGrupo, String idHilo) {
        try (BufferedReader reader = new BufferedReader(new FileReader(ARCHIVO_RESPUESTAS))) {
            String linea;
            int respuestasCargadas = 0;

            while ((linea = reader.readLine()) != null) {
                if (linea.trim().isEmpty() || linea.startsWith("#")) continue;

                String[] datos = linea.split(";");
                if (datos.length >= 8) {
                    String comunidad = datos[0];
                    String grupo = datos[1];
                    String hiloId = datos[2];
                    String idRespuesta = datos[3];
                    String contenido = datos[4];
                    String autorUsername = datos[5];
                    String fechaCreacionStr = datos[6];
                    boolean esSolucion = Boolean.parseBoolean(datos[7]);
                    String votosUsuariosStr = (datos.length > 8) ? datos[8] : ""; // Leer votos si existen, sino cadena vacía

                    if (comunidad.equals(nombreComunidad) && grupo.equals(tituloGrupo) && hiloId.equals(idHilo)) {
                        UsuarioComunidad autor = buscarUsuarioPorUsername(autorUsername);
                        if (autor != null) {
                            LocalDateTime fechaCreacion = LocalDateTime.parse(fechaCreacionStr, FORMATO_FECHA);

                            // Parsear votosUsuarios
                            Map<String, Integer> votosUsuarios = new HashMap<>();
                            if (!votosUsuariosStr.isEmpty()) {
                                for (String votoEntry : votosUsuariosStr.split(",")) {
                                    String[] parts = votoEntry.split(":");
                                    if (parts.length == 2) {
                                        votosUsuarios.put(parts[0], Integer.parseInt(parts[1]));
                                    }
                                }
                            }

                            // Crear la respuesta usando el constructor de carga
                            Respuesta respuesta = new Respuesta(idRespuesta, contenido, autor, fechaCreacion, votosUsuarios, esSolucion);

                            // Acceder directamente a la lista privada de respuestas usando reflexión
                            try {
                                java.lang.reflect.Field respuestasField = HiloDiscusion.class.getDeclaredField("respuestas");
                                respuestasField.setAccessible(true);
                                @SuppressWarnings("unchecked")
                                List<Respuesta> respuestasReales = (List<Respuesta>) respuestasField.get(hilo);
                                respuestasReales.add(respuesta);
                            } catch (Exception e) {
                                System.err.println("⚠️ No se pudo acceder a la lista de respuestas: " + e.getMessage());
                                // Fallback: intentar usando el método normal (aunque sea una copia)
                                hilo.addRespuesta(respuesta); // Usar el método público si existe
                            }

                            respuestasCargadas++;
                            System.out.println("    ✅ Cargada respuesta de: " + autorUsername);
                        }
                    }
                }
            }

            if (respuestasCargadas > 0) {
                System.out.println("  📝 Cargadas " + respuestasCargadas + " respuestas para el hilo: " + hilo.getTitulo());
            }

        } catch (IOException e) {
            System.err.println("⚠️ Error al cargar respuestas del hilo: " + e.getMessage());
        }
    }

    /**
     * Carga las soluciones persistidas y las agrega a los grupos de compartir
     */
    public static void cargarSolucionesPersistidas(ForoGeneral foro, String nombreComunidad) {
        System.out.println("🔄 Cargando soluciones persistidas para la comunidad: " + nombreComunidad);

        try (BufferedReader reader = new BufferedReader(new FileReader(ARCHIVO_SOLUCIONES))) {
            String linea;
            int solucionesCargadas = 0;

            while ((linea = reader.readLine()) != null) {
                if (linea.trim().isEmpty() || linea.startsWith("#")) continue;

                String[] datos = linea.split(";");
                if (datos.length >= 8) {
                    String comunidad = datos[0];
                    String tituloGrupo = datos[1];
                    String idSolucion = datos[2];
                    String titulo = datos[3];
                    String contenido = datos[4];
                    String autorUsername = datos[5];
                    String fechaCreacionStr = datos[6];
                    String tipoSolucionStr = datos[7];
                    String votosUsuariosStr = (datos.length > 8) ? datos[8] : ""; // Leer votos si existen, sino cadena vacía

                    if (comunidad.equals(nombreComunidad)) {
                        // Buscar el grupo de compartir correspondiente
                        for (GrupoCompartir grupo : foro.getGruposCompartir()) {
                            if (grupo.getTitulo().equals(tituloGrupo)) {
                                // Verificar si la solución ya existe (por ID)
                                boolean solucionExiste = grupo.getSoluciones().stream()
                                        .anyMatch(s -> s.getIdSolucion().equals(idSolucion));

                                if (!solucionExiste) {
                                    // Buscar el autor
                                    UsuarioComunidad autor = buscarUsuarioPorUsername(autorUsername);
                                    if (autor != null) {
                                        // Convertir tipo de solución
                                        TipoSolucion tipo = TipoSolucion.fromDescripcion(tipoSolucionStr);
                                        LocalDateTime fecha = LocalDateTime.parse(fechaCreacionStr, FORMATO_FECHA);

                                        // Inicializar votosUsuarios como un mapa vacío si no hay votos
                                        Map<String, Integer> votosUsuarios = new HashMap<>();
                                        if (!votosUsuariosStr.isEmpty()) {
                                            for (String votoEntry : votosUsuariosStr.split(",")) {
                                                String[] parts = votoEntry.split(":");
                                                if (parts.length == 2) {
                                                    votosUsuarios.put(parts[0], Integer.parseInt(parts[1]));
                                                }
                                            }
                                        }

                                        // Crear la solución usando el constructor de carga
                                        Solucion solucion = new Solucion(idSolucion, titulo, contenido, autor, tipo, null, fecha, votosUsuarios, null);

                                        // Acceder directamente a la lista privada de soluciones usando reflexión
                                        try {
                                            java.lang.reflect.Field solucionesField = GrupoCompartir.class.getDeclaredField("soluciones");
                                            solucionesField.setAccessible(true);
                                            @SuppressWarnings("unchecked")
                                            List<Solucion> solucionesReales = (List<Solucion>) solucionesField.get(grupo);
                                            solucionesReales.add(solucion);
                                        } catch (Exception e) {
                                            System.err.println("⚠️ No se pudo acceder a la lista de soluciones: " + e.getMessage());
                                            // Fallback: intentar usando el método normal (aunque sea una copia)
                                            grupo.getSoluciones().add(solucion);
                                        }

                                        solucionesCargadas++;
                                        System.out.println("  ✅ Cargada solución: " + titulo + " (ID: " + idSolucion + ")");

                                        // Cargar comentarios para esta solución
                                        cargarComentariosDeSolucion(solucion, nombreComunidad, tituloGrupo, idSolucion);
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }

            System.out.println("✅ Cargadas " + solucionesCargadas + " soluciones persistidas");

        } catch (IOException e) {
            System.err.println("⚠️ Error al cargar soluciones persistidas: " + e.getMessage());
        }
    }



    /**
     * Carga los comentarios persistidos para una solución específica.
     */
    private static void cargarComentariosDeSolucion(Solucion solucion, String nombreComunidad, String tituloGrupo, String idSolucion) {
        try (BufferedReader reader = new BufferedReader(new FileReader(ARCHIVO_COMENTARIOS))) {
            String linea;
            int comentariosCargados = 0;

            while ((linea = reader.readLine()) != null) {
                if (linea.trim().isEmpty() || linea.startsWith("#")) continue;

                String[] datos = linea.split(";");
                if (datos.length >= 7) {
                    String comunidad = datos[0];
                    String grupo = datos[1];
                    String solucionId = datos[2];
                    String idComentario = datos[3];
                    String contenido = datos[4];
                    String autorUsername = datos[5];
                    String fechaCreacionStr = datos[6];
                    String votosUsuariosStr = (datos.length > 7) ? datos[7] : ""; // Leer votos si existen, sino cadena vacía

                    if (comunidad.equals(nombreComunidad) && grupo.equals(tituloGrupo) && solucionId.equals(idSolucion)) {
                        UsuarioComunidad autor = buscarUsuarioPorUsername(autorUsername);
                        if (autor != null) {
                            LocalDateTime fecha = LocalDateTime.parse(fechaCreacionStr, FORMATO_FECHA);

                            // Parsear votosUsuarios
                            Map<String, Integer> votosUsuarios = new HashMap<>();
                            if (!votosUsuariosStr.isEmpty()) {
                                for (String votoEntry : votosUsuariosStr.split(",")) {
                                    String[] parts = votoEntry.split(":");
                                    if (parts.length == 2) {
                                        votosUsuarios.put(parts[0], Integer.parseInt(parts[1]));
                                    }
                                }
                            }

                            // Crear el comentario usando el constructor de carga
                            Comentario comentario = new Comentario(idComentario, contenido, autor, fecha, votosUsuarios);

                            // Acceder directamente a la lista privada de comentarios usando reflexión
                            try {
                                java.lang.reflect.Field comentariosField = Solucion.class.getDeclaredField("comentarios");
                                comentariosField.setAccessible(true);
                                @SuppressWarnings("unchecked")
                                List<Comentario> comentariosReales = (List<Comentario>) comentariosField.get(solucion);
                                comentariosReales.add(comentario);
                            } catch (Exception e) {
                                System.err.println("⚠️ No se pudo acceder a la lista de comentarios: " + e.getMessage());
                                // Fallback: intentar usando el método normal (aunque sea una copia)
                                solucion.getComentarios().add(comentario);
                            }

                            comentariosCargados++;
                            System.out.println("    ✅ Cargado comentario de: " + autorUsername);
                        }
                    }
                }
            }

            if (comentariosCargados > 0) {
                System.out.println("  📝 Cargados " + comentariosCargados + " comentarios para la solución: " + solucion.getTitulo());
            }

        } catch (IOException e) {
            System.err.println("⚠️ Error al cargar comentarios de la solución: " + e.getMessage());
        }
    }

    /**
     * Busca un usuario por su username en el sistema
     */
    private static UsuarioComunidad buscarUsuarioPorUsername(String username) {
        try {
            ContextoSistema contexto = ContextoSistema.getInstance();
            if (contexto.tieneComunidadActiva()) {
                Comunidad comunidad = contexto.getComunidadActual();

                // Buscar en usuarios conectados
                for (UsuarioComunidad usuario : comunidad.getUsuariosConectados()) {
                    if (usuario.getUsername().equals(username)) {
                        return usuario;
                    }
                }

                // Buscar en usuarios miembros
                for (UsuarioComunidad usuario : comunidad.getUsuariosMiembros()) {
                    if (usuario.getUsername().equals(username)) {
                        return usuario;
                    }
                }

                // Si no se encuentra, crear un usuario temporal para mantener la referencia
                UsuarioComunidad usuarioTemp = new UsuarioComunidad(username, "temp", username, username + "@temp.com");
                usuarioTemp.setNivelJava(NivelJava.PRINCIPIANTE);
                return usuarioTemp;
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error al buscar usuario por username: " + e.getMessage());
        }

        // Crear usuario temporal si no se encuentra
        UsuarioComunidad usuarioTemp = new UsuarioComunidad(username, "temp", username, username + "@temp.com");
        usuarioTemp.setNivelJava(NivelJava.PRINCIPIANTE);
        return usuarioTemp;
    }

    /**
     * Método principal para cargar todos los datos persistidos
     */
    public static void cargarTodosLosDatosPersistidos(ForoGeneral foro, String nombreComunidad) {
        System.out.println("🚀 Iniciando carga completa de datos persistidos...");

        cargarMembresiasPersistidas(foro, nombreComunidad);
        cargarHilosPersistidos(foro, nombreComunidad);
        cargarSolucionesPersistidas(foro, nombreComunidad);

        System.out.println("🎯 Carga completa de datos persistidos finalizada");
    }

    // ========== MÉTODOS AUXILIARES ==========

    /**
     * Lee todas las líneas de un archivo
     */
    private static List<String> leerArchivo(String rutaArchivo) throws IOException {
        File archivo = new File(rutaArchivo);
        if (!archivo.exists()) {
            return new ArrayList<>();
        }

        return Files.readAllLines(Paths.get(rutaArchivo), StandardCharsets.UTF_8);
    }

    /**
     * Escribe una línea en un archivo
     */
    private void escribirArchivo(String rutaArchivo, String contenido, boolean append) throws IOException {
        try (FileWriter writer = new FileWriter(rutaArchivo, StandardCharsets.UTF_8, append)) {
            writer.write(contenido);
        }
    }

    /**
     * Reescribe completamente un archivo con una lista de líneas
     */
    private static void escribirTodasLasLineas(String rutaArchivo, List<String> lineas) throws IOException {
        try (FileWriter writer = new FileWriter(rutaArchivo, StandardCharsets.UTF_8, false)) {
            for (String linea : lineas) {
                writer.write(linea + "\n");
            }
        }
    }

    /**
     * Obtiene estadísticas de persistencia para debugging
     */
    public void mostrarEstadisticasPersistencia() {
        System.out.println("\n=== ESTADÍSTICAS DE PERSISTENCIA ===");
        try {
            System.out.println("Comunidades guardadas: " + leerArchivo(ARCHIVO_COMUNIDADES).size());
            System.out.println("Conexiones usuario-comunidad: " + leerArchivo(ARCHIVO_USUARIOS_COMUNIDADES).size());
            System.out.println("Grupos de foro: " + leerArchivo(ARCHIVO_FOROS).size());
            System.out.println("Chats privados: " + leerArchivo(ARCHIVO_CHATS).size());
            System.out.println("Membresías de grupos: " + leerArchivo(ARCHIVO_GRUPOS_MIEMBROS).size());
            System.out.println("Hilos de discusión: " + leerArchivo(ARCHIVO_HILOS).size());
            System.out.println("Soluciones compartidas: " + leerArchivo(ARCHIVO_SOLUCIONES).size());
            System.out.println("Respuestas a hilos: " + leerArchivo(ARCHIVO_RESPUESTAS).size());
            System.out.println("Comentarios a soluciones: " + leerArchivo(ARCHIVO_COMENTARIOS).size());
        } catch (Exception e) {
            System.out.println("Error al obtener estadísticas: " + e.getMessage());
        }
        System.out.println("========================================\n");
    }
}
