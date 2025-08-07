package Comunidad_Modulo.servicios;

import Modulo_Usuario.Clases.UsuarioComunidad;
import Modulo_Usuario.Clases.Roles;
import Comunidad_Modulo.modelo.*;
import Comunidad_Modulo.utilidades.FiltroContenido;
import Comunidad_Modulo.utilidades.GestorUsuarios;
import Comunidad_Modulo.controladores.ContextoSistema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio unificado para todas las operaciones de moderación y administración.
 * Combina análisis de contenido, gestión de sanciones y operaciones administrativas.
 */
public class ServicioModeracion {
    
    private ContextoSistema contexto;
    private GestorUsuarios gestorUsuarios;
    
    // Gestión de sanciones
    private Map<String, List<SancionUsuario>> sancionesPorUsuario;
    private List<SancionUsuario> sancionesActivas;
    
    public ServicioModeracion() {
        this.contexto = ContextoSistema.getInstance();
        this.gestorUsuarios = GestorUsuarios.getInstance();
        this.sancionesPorUsuario = new HashMap<>();
        this.sancionesActivas = new ArrayList<>();
    }
    
    // ==========================================
    // SECCIÓN 1: ANÁLISIS DE CONTENIDO
    // ==========================================
    
    /**
     * Modera un mensaje automáticamente
     */
    public ResultadoModeracion moderarMensaje(String contenido, UsuarioComunidad autor, String moderadorResponsable) {
        // Verificar permisos del usuario
        if (!tienePermisoParaEscribir(autor)) {
            return new ResultadoModeracion(false, "Usuario sin permisos para escribir", null);
        }
        
        // Verificar si el usuario ya está sancionado
        if (usuarioEstaSancionado(autor)) {
            SancionUsuario sancionActiva = obtenerSancionActiva(autor);
            return new ResultadoModeracion(
                false, 
                "Usuario sancionado. Tiempo restante: " + sancionActiva.getMinutosRestantes() + " minutos",
                sancionActiva
            );
        }
        
        // Analizar el contenido
        FiltroContenido.ResultadoModeración resultado = FiltroContenido.analizarContenido(contenido);
        
        if (resultado.esInapropiado()) {
            // Aplicar sanción automática
            SancionUsuario sancion = aplicarSancion(
                autor, 
                resultado.getRazonSancion(), 
                resultado.getDuracionSancionMinutos(),
                moderadorResponsable
            );
            
            return new ResultadoModeracion(
                false,
                "Mensaje bloqueado. " + resultado.getRazonSancion() + 
                ". Sanción de " + resultado.getDuracionSancionMinutos() + " minutos aplicada.",
                sancion
            );
        }
        
        return new ResultadoModeracion(true, "Mensaje aprobado", null);
    }
    
    // ==========================================
    // SECCIÓN 2: GESTIÓN DE SANCIONES
    // ==========================================
    
    /**
     * Aplica una sanción a un usuario
     */
    public SancionUsuario aplicarSancion(UsuarioComunidad usuario, String razon, int duracionMinutos, String moderadorResponsable) {
        SancionUsuario sancion = new SancionUsuario(usuario, razon, duracionMinutos, moderadorResponsable);
        
        // Agregar a la lista de sanciones del usuario
        List<SancionUsuario> sanciones = sancionesPorUsuario.get(usuario.getIdUsuario());
        if (sanciones == null) {
            sanciones = new ArrayList<>();
            sancionesPorUsuario.put(usuario.getIdUsuario(), sanciones);
        }
        sanciones.add(sancion);
        
        // Agregar a la lista de sanciones activas
        sancionesActivas.add(sancion);
        
        System.out.println("SANCION APLICADA: " + sancion.toString());
        
        return sancion;
    }
    
    /**
     * Aplica una sanción manual con validación de permisos y búsqueda de usuarios reales
     */
    public ResultadoOperacion aplicarSancionManual(String nombreUsuario, String nombreComunidad, 
                                                  String razon, int duracionMinutos, UsuarioComunidad moderadorSolicitante) {
        try {
            // Validar permisos del moderador
            if (!esModerador(moderadorSolicitante, nombreComunidad)) {
                return ResultadoOperacion.error("Usuario '" + moderadorSolicitante.getUsername() + 
                    "' sin permisos de moderación en la comunidad '" + nombreComunidad + "'.");
            }
            
            // Buscar la comunidad
            Optional<Comunidad> comunidadOpt = contexto.getComunidades().stream()
                    .filter(c -> c.getNombre().equalsIgnoreCase(nombreComunidad.trim()))
                    .findFirst();

            if (!comunidadOpt.isPresent()) {
                return ResultadoOperacion.error("No se encontró la comunidad '" + nombreComunidad + "'.");
            }

            Comunidad comunidad = comunidadOpt.get();
            
            // Buscar usuario primero en la comunidad
            Optional<UsuarioComunidad> usuarioOpt = comunidad.getUsuariosMiembros().stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(nombreUsuario.trim()))
                    .findFirst();

            // Si no está en la comunidad, buscarlo en el sistema y agregarlo
            if (!usuarioOpt.isPresent()) {
                Optional<UsuarioComunidad> usuarioSistemaOpt = gestorUsuarios.buscarUsuarioComunidad(nombreUsuario);
                if (usuarioSistemaOpt.isPresent()) {
                    UsuarioComunidad usuarioSistema = usuarioSistemaOpt.get();
                    // Agregar el usuario a la comunidad
                    comunidad.getUsuariosMiembros().add(usuarioSistema);
                    usuarioOpt = Optional.of(usuarioSistema);
                    System.out.println("Usuario '" + nombreUsuario + "' agregado a la comunidad '" + nombreComunidad + "'");
                } else {
                    return ResultadoOperacion.error("El usuario '" + nombreUsuario + "' no existe en el sistema.");
                }
            }

            UsuarioComunidad usuario = usuarioOpt.get();
            
            // Verificar que no se esté sancionando a un usuario con mayor jerarquía
            Roles rolObjetivo = gestorUsuarios.obtenerRolUsuario(usuario.getUsername());
            Roles rolModerador = gestorUsuarios.obtenerRolUsuario(moderadorSolicitante.getUsername());
            
            if (esRolSuperior(rolObjetivo, rolModerador)) {
                return ResultadoOperacion.error("No puedes sancionar a un usuario con rol superior al tuyo.");
            }
            
            // Aplicar sanción
            aplicarSancion(usuario, razon, duracionMinutos, moderadorSolicitante.getUsername());
            
            return ResultadoOperacion.exito("Sanción aplicada exitosamente a '" + nombreUsuario + 
                "' (Rol: " + rolObjetivo + ") por " + duracionMinutos + " minutos. Razón: " + razon);
            
        } catch (Exception e) {
            return ResultadoOperacion.error("Error al aplicar sanción: " + e.getMessage());
        }
    }
    
    /**
     * Verifica si un rol es superior a otro en la jerarquía
     */
    private boolean esRolSuperior(Roles rolObjetivo, Roles rolModerador) {
        // Jerarquía: SUPER_ADMINISTRADOR > ADMINISTRADOR > MODERADOR > USUARIO
        int pesoObjetivo = obtenerPesoRol(rolObjetivo);
        int pesoModerador = obtenerPesoRol(rolModerador);
        return pesoObjetivo > pesoModerador;
    }
    
    /**
     * Obtiene el peso numérico de un rol para comparaciones
     */
    private int obtenerPesoRol(Roles rol) {
        switch (rol) {
            case SUPER_ADMINISTRADOR: return 4;
            case ADMINISTRADOR: return 3;
            case MODERADOR: return 2;
            case USUARIO: return 1;
            default: return 0;
        }
    }
    
    /**
     * Levanta una sanción manualmente con búsqueda de usuarios reales
     */
    public ResultadoOperacion levantarSancion(String nombreUsuario, String nombreComunidad, UsuarioComunidad moderadorSolicitante) {
        try {
            // Validar permisos del moderador
            if (!esModerador(moderadorSolicitante, nombreComunidad)) {
                return ResultadoOperacion.error("Usuario '" + moderadorSolicitante.getUsername() + 
                    "' sin permisos de moderación en la comunidad '" + nombreComunidad + "'.");
            }
            
            Optional<Comunidad> comunidadOpt = contexto.getComunidades().stream()
                    .filter(c -> c.getNombre().equalsIgnoreCase(nombreComunidad.trim()))
                    .findFirst();

            if (!comunidadOpt.isPresent()) {
                return ResultadoOperacion.error("No se encontró la comunidad '" + nombreComunidad + "'.");
            }

            Comunidad comunidad = comunidadOpt.get();
            
            // Buscar usuario en la comunidad
            Optional<UsuarioComunidad> usuarioOpt = comunidad.getUsuariosMiembros().stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(nombreUsuario.trim()))
                    .findFirst();

            // Si no está en la comunidad, buscarlo en el sistema
            if (!usuarioOpt.isPresent()) {
                usuarioOpt = gestorUsuarios.buscarUsuarioComunidad(nombreUsuario);
                if (!usuarioOpt.isPresent()) {
                    return ResultadoOperacion.error("El usuario '" + nombreUsuario + "' no existe en el sistema.");
                }
            }

            UsuarioComunidad usuario = usuarioOpt.get();
            Roles rolUsuario = gestorUsuarios.obtenerRolUsuario(usuario.getUsername());
            
            boolean sancionLevantada = levantarSancion(usuario, moderadorSolicitante.getUsername());
            
            if (sancionLevantada) {
                return ResultadoOperacion.exito("Sanción levantada exitosamente para '" + nombreUsuario + 
                    "' (Rol: " + rolUsuario + ")");
            } else {
                return ResultadoOperacion.error("El usuario '" + nombreUsuario + "' no tenía sanciones activas.");
            }
            
        } catch (Exception e) {
            return ResultadoOperacion.error("Error al levantar sanción: " + e.getMessage());
        }
    }
    
    /**
     * Levanta una sanción de un usuario
     */
    public boolean levantarSancion(UsuarioComunidad usuario, String moderadorResponsable) {
        SancionUsuario sancionActiva = obtenerSancionActiva(usuario);
        if (sancionActiva != null) {
            sancionActiva.levantarSancion();
            System.out.println("Sanción levantada por " + moderadorResponsable + ": " + sancionActiva.toString());
            return true;
        }
        return false;
    }
    
    /**
     * Verifica si un usuario está actualmente sancionado
     */
    public boolean usuarioEstaSancionado(UsuarioComunidad usuario) {
        return obtenerSancionActiva(usuario) != null;
    }
    
    /**
     * Obtiene la sanción activa de un usuario
     */
    public SancionUsuario obtenerSancionActiva(UsuarioComunidad usuario) {
        List<SancionUsuario> sanciones = sancionesPorUsuario.get(usuario.getIdUsuario());
        if (sanciones == null) return null;
        
        return sanciones.stream()
                .filter(SancionUsuario::estaActiva)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Obtiene todas las sanciones activas
     */
    public List<SancionUsuario> getSancionesActivas() {
        // Limpiar sanciones expiradas
        sancionesActivas.removeIf(sancion -> !sancion.estaActiva());
        return new ArrayList<>(sancionesActivas);
    }
    
    /**
     * Obtiene el historial de sanciones de un usuario
     */
    public List<SancionUsuario> getHistorialSanciones(UsuarioComunidad usuario) {
        return sancionesPorUsuario.getOrDefault(usuario.getIdUsuario(), new ArrayList<>());
    }
    
    // ==========================================
    // SECCIÓN 3: OPERACIONES ADMINISTRATIVAS
    // ==========================================
    
    /**
     * Elimina un usuario de una comunidad (solo administradores) con verificación de roles reales
     */
    public ResultadoOperacion eliminarUsuarioDeComunidad(String nombreUsuario, String nombreComunidad, UsuarioComunidad solicitante) {
        try {
            // Validar permisos de administrador
            if (!esAdministrador(solicitante, nombreComunidad)) {
                Roles rolSolicitante = gestorUsuarios.obtenerRolUsuario(solicitante.getUsername());
                return ResultadoOperacion.error("Usuario '" + solicitante.getUsername() + 
                    "' (Rol: " + rolSolicitante + ") sin permisos de administración en esta comunidad.");
            }
            
            Optional<Comunidad> comunidadOpt = contexto.getComunidades().stream()
                    .filter(c -> c.getNombre().equalsIgnoreCase(nombreComunidad.trim()))
                    .findFirst();

            if (!comunidadOpt.isPresent()) {
                return ResultadoOperacion.error("No se encontró la comunidad '" + nombreComunidad + "'.");
            }

            Comunidad comunidad = comunidadOpt.get();
            
            // Buscar el usuario a eliminar
            Optional<UsuarioComunidad> usuarioAEliminarOpt = comunidad.getUsuariosMiembros().stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(nombreUsuario.trim()))
                    .findFirst();

            if (!usuarioAEliminarOpt.isPresent()) {
                return ResultadoOperacion.error("El usuario '" + nombreUsuario + "' no se encontró en la comunidad.");
            }

            UsuarioComunidad usuarioAEliminar = usuarioAEliminarOpt.get();
            Roles rolUsuarioAEliminar = gestorUsuarios.obtenerRolUsuario(usuarioAEliminar.getUsername());
            Roles rolSolicitante = gestorUsuarios.obtenerRolUsuario(solicitante.getUsername());
            
            // Verificar que no se esté eliminando a un usuario con mayor jerarquía
            if (esRolSuperior(rolUsuarioAEliminar, rolSolicitante)) {
                return ResultadoOperacion.error("No puedes eliminar a un usuario con rol superior al tuyo. " +
                    "Tu rol: " + rolSolicitante + ", Rol del usuario: " + rolUsuarioAEliminar);
            }

            boolean usuarioEliminado = comunidad.getUsuariosMiembros().removeIf(
                u -> u.getUsername().equalsIgnoreCase(nombreUsuario.trim())
            );

            if (usuarioEliminado) {
                return ResultadoOperacion.exito("Usuario '" + nombreUsuario + "' (Rol: " + rolUsuarioAEliminar + 
                    ") eliminado de la comunidad '" + nombreComunidad + "' por '" + solicitante.getUsername() + 
                    "' (Rol: " + rolSolicitante + ")");
            } else {
                return ResultadoOperacion.error("Error inesperado al eliminar usuario.");
            }

        } catch (Exception e) {
            return ResultadoOperacion.error("Error al eliminar usuario: " + e.getMessage());
        }
    }
    
    /**
     * Elimina una comunidad completa (solo super administradores) con verificación de roles reales
     */
    public ResultadoOperacion eliminarComunidad(String nombreComunidad, UsuarioComunidad solicitante) {
        try {
            // Validar permisos de super administrador
            if (!esSuperAdministrador(solicitante)) {
                Roles rolSolicitante = gestorUsuarios.obtenerRolUsuario(solicitante.getUsername());
                return ResultadoOperacion.error("Usuario '" + solicitante.getUsername() + 
                    "' (Rol: " + rolSolicitante + ") sin permisos de super administración. " +
                    "Solo usuarios con rol SUPER_ADMINISTRADOR pueden eliminar comunidades.");
            }
            
            Roles rolSolicitante = gestorUsuarios.obtenerRolUsuario(solicitante.getUsername());
            
            boolean comunidadEliminada = contexto.getComunidades().removeIf(
                c -> c.getNombre().equalsIgnoreCase(nombreComunidad.trim())
            );

            if (comunidadEliminada) {
                return ResultadoOperacion.exito("Comunidad '" + nombreComunidad + 
                    "' eliminada exitosamente por '" + solicitante.getUsername() + 
                    "' (Rol: " + rolSolicitante + ")");
            } else {
                return ResultadoOperacion.error("No se encontró la comunidad '" + nombreComunidad + "'.");
            }

        } catch (Exception e) {
            return ResultadoOperacion.error("Error al eliminar comunidad: " + e.getMessage());
        }
    }
    
    // ==========================================
    // SECCIÓN 4: GESTIÓN DE ROLES Y PERMISOS
    // ==========================================
    
    /**
     * Verifica si un usuario es moderador de una comunidad
     */
    public boolean esModerador(UsuarioComunidad usuario, String nombreComunidad) {
        // Verificar rol desde archivo de usuarios
        Roles rolUsuario = gestorUsuarios.obtenerRolUsuario(usuario.getUsername());
        
        // Un usuario es moderador si:
        // 1. Tiene rol MODERADOR, ADMINISTRADOR o SUPER_ADMINISTRADOR en el sistema
        // 2. O está asignado como moderador específico de la comunidad
        if (rolUsuario == Roles.MODERADOR || rolUsuario == Roles.ADMINISTRADOR || rolUsuario == Roles.SUPER_ADMINISTRADOR) {
            return true;
        }
        
        // Verificar si está asignado como moderador específico de la comunidad
        Optional<Comunidad> comunidadOpt = contexto.getComunidades().stream()
                .filter(c -> c.getNombre().equalsIgnoreCase(nombreComunidad))
                .findFirst();
        
        if (comunidadOpt.isPresent()) {
            Comunidad comunidad = comunidadOpt.get();
            Moderador moderador = comunidad.getModerador();
            return moderador != null && moderador.getUsername().equals(usuario.getUsername());
        }
        
        return false;
    }
    
    /**
     * Verifica si un usuario es administrador de una comunidad
     */
    public boolean esAdministrador(UsuarioComunidad usuario, String nombreComunidad) {
        // Verificar rol desde archivo de usuarios
        Roles rolUsuario = gestorUsuarios.obtenerRolUsuario(usuario.getUsername());
        
        // Un usuario es administrador si tiene rol ADMINISTRADOR o SUPER_ADMINISTRADOR
        return rolUsuario == Roles.ADMINISTRADOR || rolUsuario == Roles.SUPER_ADMINISTRADOR;
    }
    
    /**
     * Verifica si un usuario es super administrador del sistema
     */
    public boolean esSuperAdministrador(UsuarioComunidad usuario) {
        // Verificar rol desde archivo de usuarios
        Roles rolUsuario = gestorUsuarios.obtenerRolUsuario(usuario.getUsername());
        return rolUsuario == Roles.SUPER_ADMINISTRADOR;
    }
    
    /**
     * Verifica si un usuario tiene permisos para escribir
     */
    public boolean tienePermisoParaEscribir(UsuarioComunidad usuario) {
        // Verificar si está sancionado
        if (usuarioEstaSancionado(usuario)) {
            return false;
        }
        
        // Verificar otros criterios (por ejemplo, estado del usuario)
        return true; // Por defecto, todos pueden escribir si no están sancionados
    }
    
    // ==========================================
    // SECCIÓN 5: REPORTES Y ESTADÍSTICAS
    // ==========================================
    
    /**
     * Busca un usuario del sistema por username
     */
    public Optional<UsuarioComunidad> buscarUsuarioSistema(String username) {
        return gestorUsuarios.buscarUsuarioComunidad(username);
    }
    
    /**
     * Refresca la lista de usuarios desde el archivo
     */
    public void refrescarUsuarios() {
        gestorUsuarios.refrescarUsuarios();
    }
    
    /**
     * Crea un moderador para una comunidad según el tipo especificado
     */
    public Moderador crearModeradorParaComunidad(String nombreComunidad, Moderador.TipoModerador tipo, String nombreModerador) {
        return Moderador.crear(tipo, nombreModerador + "_" + nombreComunidad, 
                              nombreModerador.toLowerCase() + "_" + nombreComunidad.toLowerCase());
    }
    
    /**
     * Obtiene información sobre tipos de moderadores disponibles
     */
    public String obtenerInformacionModerador() {
        return Moderador.obtenerInformacionTipos();
    }
    
    /**
     * Genera un reporte completo de usuarios y roles del sistema
     */
    public String generarReporteUsuariosYRoles() {
        StringBuilder reporte = new StringBuilder();
        reporte.append("=== REPORTE COMPLETO DEL SISTEMA ===\n\n");
        
        // Reporte de roles desde el archivo
        reporte.append(gestorUsuarios.generarReporteRoles());
        
        // Reporte de comunidades
        reporte.append("🌐 === COMUNIDADES ===\n");
        for (Comunidad comunidad : contexto.getComunidades()) {
            reporte.append("📂 Comunidad: ").append(comunidad.getNombre()).append("\n");
            reporte.append("👥 Miembros: ").append(comunidad.getUsuariosMiembros().size()).append("\n");
            
            // Usuarios sancionados en esta comunidad
            long usuariosSancionados = comunidad.getUsuariosMiembros().stream()
                    .filter(this::usuarioEstaSancionado)
                    .count();
            
            reporte.append("Usuarios sancionados: ").append(usuariosSancionados).append("\n");
            reporte.append("Moderador asignado: ").append(
                comunidad.getModerador() != null ? comunidad.getModerador().getNombre() : "Sin moderador"
            ).append("\n\n");
        }
        
        return reporte.toString();
    }
    
    /**
     * Obtiene los nombres de todas las comunidades disponibles
     */
    public List<String> obtenerNombresComunidades() {
        return contexto.getComunidades().stream()
                .map(Comunidad::getNombre)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene los grupos de una comunidad específica por tipo
     */
    public List<String> obtenerGruposDeComunidad(String nombreComunidad, String tipoGrupo) {
        Optional<Comunidad> comunidadOpt = contexto.getComunidades().stream()
                .filter(c -> c.getNombre().equalsIgnoreCase(nombreComunidad))
                .findFirst();
        
        if (!comunidadOpt.isPresent()) {
            return new ArrayList<>();
        }
        
        Comunidad comunidad = comunidadOpt.get();
        ForoGeneral foro = comunidad.getForoGeneral();
        
        if (foro == null) {
            return new ArrayList<>();
        }
        
        List<String> nombresGrupos = new ArrayList<>();
        
        if ("Grupos de Discusión".equalsIgnoreCase(tipoGrupo)) {
            nombresGrupos = foro.getGruposDiscusion().stream()
                    .map(GrupoDiscusion::getTitulo)
                    .collect(Collectors.toList());
        } else if ("Grupos de Compartir".equalsIgnoreCase(tipoGrupo)) {
            nombresGrupos = foro.getGruposCompartir().stream()
                    .map(GrupoCompartir::getTitulo)
                    .collect(Collectors.toList());
        }
        
        return nombresGrupos;
    }
    
    /**
     * Genera historial detallado de un grupo específico
     */
    public String generarHistorialGrupo(String nombreComunidad, String tipoGrupo, String nombreGrupo) {
        StringBuilder historial = new StringBuilder();
        historial.append("📋 === HISTORIAL DEL GRUPO ===\n\n");
        historial.append("🌐 Comunidad: ").append(nombreComunidad).append("\n");
        historial.append("📂 Tipo: ").append(tipoGrupo).append("\n");
        historial.append("👥 Grupo: ").append(nombreGrupo).append("\n\n");
        
        try {
            Optional<Comunidad> comunidadOpt = contexto.getComunidades().stream()
                    .filter(c -> c.getNombre().equalsIgnoreCase(nombreComunidad))
                    .findFirst();
            
            if (!comunidadOpt.isPresent()) {
                return historial.append("❌ Comunidad no encontrada.").toString();
            }
            
            Comunidad comunidad = comunidadOpt.get();
            ForoGeneral foro = comunidad.getForoGeneral();
            
            if (foro == null) {
                return historial.append("❌ Foro no disponible en esta comunidad.").toString();
            }
            
            if ("Grupos de Discusión".equalsIgnoreCase(tipoGrupo)) {
                Optional<GrupoDiscusion> grupoOpt = foro.getGruposDiscusion().stream()
                        .filter(g -> g.getTitulo().equalsIgnoreCase(nombreGrupo))
                        .findFirst();
                
                if (grupoOpt.isPresent()) {
                    GrupoDiscusion grupo = grupoOpt.get();
                    historial.append("INFORMACIÓN DEL GRUPO DE DISCUSIÓN:\n");
                    historial.append("   Título: ").append(grupo.getTitulo()).append("\n");
                    historial.append("   Estado: Activo\n\n");
                    historial.append("Grupo de discusión encontrado y funcionando correctamente.");
                } else {
                    historial.append("Grupo de discusión no encontrado.");
                }
                
            } else if ("Grupos de Compartir".equalsIgnoreCase(tipoGrupo)) {
                Optional<GrupoCompartir> grupoOpt = foro.getGruposCompartir().stream()
                        .filter(g -> g.getTitulo().equalsIgnoreCase(nombreGrupo))
                        .findFirst();
                
                if (grupoOpt.isPresent()) {
                    GrupoCompartir grupo = grupoOpt.get();
                    historial.append("INFORMACIÓN DEL GRUPO DE COMPARTIR:\n");
                    historial.append("   Título: ").append(grupo.getTitulo()).append("\n");
                    historial.append("   Estado: Activo\n\n");
                    historial.append("Grupo de compartir encontrado y funcionando correctamente.");
                } else {
                    historial.append("Grupo de compartir no encontrado.");
                }
            }
            
        } catch (Exception e) {
            historial.append("❌ Error al generar historial: ").append(e.getMessage());
        }
        
        return historial.toString();
    }

    /**
     * Genera un reporte detallado de usuarios
     */
    public String generarReporteUsuarios() {
        return generarReporteUsuariosYRoles();
    }
    
    /**
     * Obtiene estadísticas completas de moderación
     */
    public String obtenerEstadisticasModeración(String nombreComunidad) {
        try {
            Optional<Comunidad> comunidadOpt = contexto.getComunidades().stream()
                    .filter(c -> c.getNombre().equalsIgnoreCase(nombreComunidad.trim()))
                    .findFirst();

            if (!comunidadOpt.isPresent()) {
                return "❌ No se encontró la comunidad '" + nombreComunidad + "'.";
            }

            Comunidad comunidad = comunidadOpt.get();
            Moderador moderador = comunidad.getModerador();

            StringBuilder stats = new StringBuilder();
            stats.append("ESTADÍSTICAS DE MODERACIÓN\n\n");
            stats.append("Comunidad: ").append(nombreComunidad).append("\n");
            stats.append("Moderador: ").append(moderador != null ? moderador.getNombre() : "Sin moderador").append("\n");
            stats.append("Tipo: ").append(moderador != null ? moderador.getClass().getSimpleName() : "N/A").append("\n\n");

            List<SancionUsuario> sancionesActivas = getSancionesActivas();
            stats.append("Sanciones activas globales: ").append(sancionesActivas.size()).append("\n");
            
            if (!sancionesActivas.isEmpty()) {
                stats.append("\n📋 Detalle de sanciones:\n");
                for (SancionUsuario sancion : sancionesActivas) {
                    stats.append("  • ").append(sancion.getUsuario().getUsername())
                         .append(" - ").append(sancion.getMinutosRestantes()).append(" min restantes\n");
                }
            }

            EstadisticasModeración estadisticas = getEstadisticas();
            stats.append("\n").append(estadisticas.toString());

            return stats.toString();
            
        } catch (Exception e) {
            return "❌ Error al obtener estadísticas: " + e.getMessage();
        }
    }
    
    /**
     * Obtiene estadísticas generales de moderación
     */
    public EstadisticasModeración getEstadisticas() {
        int totalSanciones = sancionesPorUsuario.values().stream()
                .mapToInt(List::size)
                .sum();
        
        int sancionesActivas = getSancionesActivas().size();
        
        Map<String, Integer> tiposSanciones = sancionesPorUsuario.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(
                    SancionUsuario::getRazon,
                    Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
        
        return new EstadisticasModeración(totalSanciones, sancionesActivas, tiposSanciones);
    }
    
    // ==========================================
    // CLASES INTERNAS
    // ==========================================
    
    /**
     * Resultado de operaciones administrativas
     */
    public static class ResultadoOperacion {
        private final boolean exitoso;
        private final String mensaje;
        
        private ResultadoOperacion(boolean exitoso, String mensaje) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
        }
        
        public static ResultadoOperacion exito(String mensaje) {
            return new ResultadoOperacion(true, mensaje);
        }
        
        public static ResultadoOperacion error(String mensaje) {
            return new ResultadoOperacion(false, mensaje);
        }
        
        public boolean isExitoso() { return exitoso; }
        public String getMensaje() { return mensaje; }
    }
    
    /**
     * Resultado de moderación de contenido
     */
    public static class ResultadoModeracion {
        private final boolean aprobado;
        private final String mensaje;
        private final SancionUsuario sancion;
        
        public ResultadoModeracion(boolean aprobado, String mensaje, SancionUsuario sancion) {
            this.aprobado = aprobado;
            this.mensaje = mensaje;
            this.sancion = sancion;
        }
        
        public boolean isAprobado() { return aprobado; }
        public String getMensaje() { return mensaje; }
        public SancionUsuario getSancion() { return sancion; }
    }
    
    /**
     * Estadísticas de moderación
     */
    public static class EstadisticasModeración {
        private final int totalSanciones;
        private final int sancionesActivas;
        private final Map<String, Integer> tiposSanciones;
        
        public EstadisticasModeración(int totalSanciones, int sancionesActivas, Map<String, Integer> tiposSanciones) {
            this.totalSanciones = totalSanciones;
            this.sancionesActivas = sancionesActivas;
            this.tiposSanciones = tiposSanciones;
        }
        
        public int getTotalSanciones() { return totalSanciones; }
        public int getSancionesActivas() { return sancionesActivas; }
        public Map<String, Integer> getTiposSanciones() { return tiposSanciones; }
        
        @Override
        public String toString() {
            return String.format("Estadísticas: %d sanciones totales, %d activas", 
                               totalSanciones, sancionesActivas);
        }
    }
}
