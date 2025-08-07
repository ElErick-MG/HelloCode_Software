package Comunidad_Modulo.servicios;

import Comunidad_Modulo.controladores.ContextoSistema;
import Comunidad_Modulo.modelo.*;
import Modulo_Usuario.Clases.UsuarioComunidad;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio que encapsula toda la lógica de negocio relacionada con la administración y moderación.
 * Combina operaciones administrativas con funcionalidades de moderación real.
 * Separa la lógica del controlador para mantener una arquitectura más limpia.
 */
public class ModeradorService {

    private final ContextoSistema contexto;

    public ModeradorService() {
        this.contexto = ContextoSistema.getInstance();
    }

    // ========== OPERACIONES ADMINISTRATIVAS ==========

    /**
     * Elimina un usuario de una comunidad específica
     */
    public ResultadoOperacion eliminarUsuarioDeComunidad(String nombreUsuario, String nombreComunidad) {
        // Validar entrada
        if (nombreUsuario == null || nombreUsuario.trim().isEmpty()) {
            return ResultadoOperacion.error("Por favor, ingrese el nombre del usuario que desea eliminar.");
        }

        if (nombreComunidad == null || nombreComunidad.trim().isEmpty()) {
            return ResultadoOperacion.error("Por favor, ingrese el nombre de la comunidad.");
        }

        // Buscar la comunidad específica
        Optional<Comunidad> comunidadOpt = contexto.getComunidades().stream()
                .filter(c -> c.getNombre().equalsIgnoreCase(nombreComunidad.trim()))
                .findFirst();

        if (!comunidadOpt.isPresent()) {
            return ResultadoOperacion.error("No se encontró la comunidad '" + nombreComunidad + "'.");
        }

        Comunidad comunidad = comunidadOpt.get();
        
        // Buscar el usuario en la comunidad específica
        Optional<UsuarioComunidad> usuarioOpt = comunidad.getUsuariosMiembros().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(nombreUsuario.trim()))
                .findFirst();

        if (!usuarioOpt.isPresent()) {
            return ResultadoOperacion.error("El usuario '" + nombreUsuario + "' no se encuentra en la comunidad '" +
                        nombreComunidad + "'.");
        }

        UsuarioComunidad usuario = usuarioOpt.get();

        // Realizar la eliminación
        comunidad.getUsuariosConectados().removeIf(u -> 
            u.getUsername().equalsIgnoreCase(nombreUsuario.trim()));
        comunidad.getUsuariosMiembros().remove(usuario);
        
        // Actualizar la comunidad en el contexto
        contexto.actualizarComunidad(comunidad);
        
        // Desconectar al usuario del sistema
        contexto.desconectarUsuarioDeComunidad(usuario, comunidad);
        
        return ResultadoOperacion.exito("Usuario '" + nombreUsuario + "' eliminado exitosamente de la comunidad '" + 
                           nombreComunidad + "'");
    }

    /**
     * Elimina una comunidad completa del sistema
     */
    public ResultadoOperacion eliminarComunidad(String nombreComunidad) {
        // Validar entrada
        if (nombreComunidad == null || nombreComunidad.trim().isEmpty()) {
            return ResultadoOperacion.error("Por favor, ingrese el nombre de la comunidad que desea eliminar.");
        }

        // Buscar la comunidad
        Optional<Comunidad> comunidadOpt = contexto.getComunidades().stream()
                .filter(c -> c.getNombre().equalsIgnoreCase(nombreComunidad.trim()))
                .findFirst();

        if (!comunidadOpt.isPresent()) {
            return ResultadoOperacion.error("No se encontró la comunidad '" + nombreComunidad + "'.");
        }

        Comunidad comunidad = comunidadOpt.get();

        // Desconectar a todos los usuarios de la comunidad
        for (UsuarioComunidad usuario : comunidad.getUsuariosConectados()) {
            contexto.desconectarUsuarioDeComunidad(usuario, comunidad);
        }

        // Eliminar la comunidad del contexto
        contexto.eliminarComunidad(comunidad);

        return ResultadoOperacion.exito("Comunidad '" + nombreComunidad + "' eliminada exitosamente");
    }

    /**
     * Genera un reporte de usuarios de todas las comunidades
     */
    public String generarReporteUsuarios() {
        StringBuilder reporte = new StringBuilder("👥 Lista de Usuarios por Comunidad:\n\n");

        List<Comunidad> comunidades = contexto.getComunidades();
        
        if (comunidades.isEmpty()) {
            return "📝 No hay comunidades registradas en el sistema.";
        }

        for (Comunidad comunidad : comunidades) {
            reporte.append("🌐 Comunidad: ").append(comunidad.getNombre()).append("\n");
            
            if (comunidad.getUsuariosMiembros().isEmpty()) {
                reporte.append("   • Sin usuarios registrados\n");
            } else {
                for (UsuarioComunidad usuario : comunidad.getUsuariosMiembros()) {
                    boolean estaConectado = comunidad.getUsuariosConectados().stream()
                            .anyMatch(u -> u.getUsername().equals(usuario.getUsername()));
                    
                    reporte.append("   • ").append(usuario.getUsername())
                         .append(" [").append(estaConectado ? "✅ Conectado" : "❌ Desconectado").append("]\n");
                }
            }
            reporte.append("\n");
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
     * Obtiene los grupos de una comunidad específica según el tipo
     */
    public List<String> obtenerGruposDeComunidad(String nombreComunidad, String tipoGrupo) {
        Optional<Comunidad> comunidadOpt = contexto.getComunidades().stream()
                .filter(c -> c.getNombre().equals(nombreComunidad))
                .findFirst();
                
        if (!comunidadOpt.isPresent()) {
            return List.of();
        }

        Comunidad comunidad = comunidadOpt.get();
        ForoGeneral foro = comunidad.getForoGeneral();
        
        if ("Grupos de Discusión".equals(tipoGrupo)) {
            return foro.getGruposDiscusion().stream()
                    .map(GrupoDiscusion::getTitulo)
                    .collect(Collectors.toList());
        } else if ("Grupos de Compartir".equals(tipoGrupo)) {
            return foro.getGruposCompartir().stream()
                    .map(GrupoCompartir::getTitulo)
                    .collect(Collectors.toList());
        }
        
        return List.of();
    }

    /**
     * Genera un historial detallado de un grupo específico
     */
    public String generarHistorialGrupo(String nombreComunidad, String tipoGrupo, String nombreGrupo) {
        try {
            Optional<Comunidad> comunidadOpt = contexto.getComunidades().stream()
                    .filter(c -> c.getNombre().equals(nombreComunidad))
                    .findFirst();
                    
            if (!comunidadOpt.isPresent()) {
                return "❌ No se encontró la comunidad especificada.";
            }
            
            Comunidad comunidad = comunidadOpt.get();
            ForoGeneral foro = comunidad.getForoGeneral();
            
            StringBuilder historial = new StringBuilder();
            historial.append("📜 Historial de la Comunidad\n\n");
            historial.append("🌐 Comunidad: ").append(nombreComunidad).append("\n");
            historial.append("📋 Tipo: ").append(tipoGrupo).append("\n");
            historial.append("📝 Grupo: ").append(nombreGrupo).append("\n\n");
            
            if ("Grupos de Discusión".equals(tipoGrupo)) {
                return generarHistorialGrupoDiscusion(historial, foro, nombreGrupo, comunidad);
            } else {
                return generarHistorialGrupoCompartir(historial, foro, nombreGrupo, comunidad);
            }
            
        } catch (Exception e) {
            return "❌ Error al generar el historial: " + e.getMessage();
        }
    }

    private String generarHistorialGrupoDiscusion(StringBuilder historial, ForoGeneral foro, 
                                                String nombreGrupo, Comunidad comunidad) {
        Optional<GrupoDiscusion> grupoOpt = foro.getGruposDiscusion().stream()
                .filter(g -> g.getTitulo().equals(nombreGrupo))
                .findFirst();
                
        if (!grupoOpt.isPresent()) {
            return "❌ No se encontró el grupo de discusión especificado.";
        }
        
        GrupoDiscusion grupo = grupoOpt.get();
        
        // Información del grupo
        historial.append("Nivel Java: ").append(grupo.getNivelJava()).append("\n");
        historial.append("Tema: ").append(grupo.getTipoTema()).append("\n\n");
        
        // Agregar información de miembros
        agregarInformacionMiembros(historial, grupo.getMiembros(), comunidad);
        
        return historial.toString();
    }

    private String generarHistorialGrupoCompartir(StringBuilder historial, ForoGeneral foro, 
                                                String nombreGrupo, Comunidad comunidad) {
        Optional<GrupoCompartir> grupoOpt = foro.getGruposCompartir().stream()
                .filter(g -> g.getTitulo().equals(nombreGrupo))
                .findFirst();
                
        if (!grupoOpt.isPresent()) {
            return "❌ No se encontró el grupo de compartir especificado.";
        }
        
        GrupoCompartir grupo = grupoOpt.get();
        
        // Información del grupo
        historial.append("Nivel Java: ").append(grupo.getNivelJava()).append("\n");
        historial.append("Tema: ").append(grupo.getTipoTema()).append("\n\n");
        
        // Agregar información de miembros
        agregarInformacionMiembros(historial, grupo.getMiembros(), comunidad);
        
        // Información de soluciones
        historial.append("\n💡 Soluciones Compartidas:\n");
        for (Solucion solucion : grupo.getSoluciones()) {
            historial.append("  👤 Autor: ").append(solucion.getAutor().getUsername()).append("\n");
            historial.append("  👍 Likes: ").append(solucion.getLikes()).append("\n\n");
        }
        
        return historial.toString();
    }

    private void agregarInformacionMiembros(StringBuilder historial, List<UsuarioComunidad> miembrosGrupo, 
                                          Comunidad comunidad) {
        // Mostrar miembros específicos del grupo
        historial.append("👥 Miembros del Grupo: ").append(miembrosGrupo.size()).append("\n");
        for (UsuarioComunidad miembro : miembrosGrupo) {
            historial.append("  • ").append(miembro.getUsername()).append(" [Unido al grupo]\n");
        }
        
        // Mostrar todos los miembros de la comunidad
        historial.append("\n👥 Todos los miembros de la comunidad: ").append(comunidad.getUsuariosMiembros().size()).append("\n");
        for (UsuarioComunidad miembro : comunidad.getUsuariosMiembros()) {
            boolean estaEnGrupo = miembrosGrupo.stream()
                    .anyMatch(m -> m.getUsername().equals(miembro.getUsername()));
            boolean estaConectado = comunidad.getUsuariosConectados().stream()
                    .anyMatch(u -> u.getUsername().equals(miembro.getUsername()));
            
            String estado = estaEnGrupo ? "[✅ En grupo]" : "[⚪ Necesita unirse al grupo]";
            String conexion = estaConectado ? " [🟢 Conectado]" : " [🔴 Desconectado]";
            historial.append("  • ").append(miembro.getUsername()).append(" ").append(estado).append(conexion).append("\n");
        }
        
        if (miembrosGrupo.isEmpty()) {
            historial.append("\n💡 Tip: Los usuarios deben ir a 'Gestión de Foro' y usar 'Unirse a Grupo' para participar en este grupo específico.\n");
        }
    }

    // ========== OPERACIONES DE MODERACIÓN REAL ==========

    /**
     * Aplica una sanción manual a un usuario en una comunidad específica
     */
    public ResultadoOperacion aplicarSancionManual(String nombreUsuario, String nombreComunidad, 
                                                  String razon, int duracionMinutos) {
        try {
            Optional<Comunidad> comunidadOpt = contexto.getComunidades().stream()
                    .filter(c -> c.getNombre().equalsIgnoreCase(nombreComunidad.trim()))
                    .findFirst();

            if (!comunidadOpt.isPresent()) {
                return ResultadoOperacion.error("No se encontró la comunidad '" + nombreComunidad + "'.");
            }

            Comunidad comunidad = comunidadOpt.get();
            
            // Buscar el usuario
            Optional<UsuarioComunidad> usuarioOpt = comunidad.getUsuariosMiembros().stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(nombreUsuario.trim()))
                    .findFirst();

            if (!usuarioOpt.isPresent()) {
                return ResultadoOperacion.error("El usuario '" + nombreUsuario + "' no se encuentra en la comunidad.");
            }

            UsuarioComunidad usuario = usuarioOpt.get();
            Moderador moderador = comunidad.getModerador();

            // Verificar que sea un moderador que soporte sanciones manuales
            if (moderador instanceof ModeradorAutomatico) {
                ModeradorAutomatico modAuto = (ModeradorAutomatico) moderador;
                SancionUsuario sancion = modAuto.aplicarSancionManual(usuario, razon, duracionMinutos);
                return ResultadoOperacion.exito("Sanción aplicada exitosamente: " + sancion.toString());
            } else if (moderador instanceof ModeradorManual) {
                ModeradorManual modManual = (ModeradorManual) moderador;
                SancionUsuario sancion = modManual.aplicarSancionManual(usuario, razon, duracionMinutos);
                return ResultadoOperacion.exito("Sanción aplicada exitosamente: " + sancion.toString());
            } else {
                return ResultadoOperacion.error("El moderador actual no soporta aplicar sanciones manuales.");
            }
            
        } catch (Exception e) {
            return ResultadoOperacion.error("Error al aplicar sanción: " + e.getMessage());
        }
    }

    /**
     * Levanta una sanción de un usuario
     */
    public ResultadoOperacion levantarSancion(String nombreUsuario, String nombreComunidad) {
        try {
            Optional<Comunidad> comunidadOpt = contexto.getComunidades().stream()
                    .filter(c -> c.getNombre().equalsIgnoreCase(nombreComunidad.trim()))
                    .findFirst();

            if (!comunidadOpt.isPresent()) {
                return ResultadoOperacion.error("No se encontró la comunidad '" + nombreComunidad + "'.");
            }

            Comunidad comunidad = comunidadOpt.get();
            
            Optional<UsuarioComunidad> usuarioOpt = comunidad.getUsuariosMiembros().stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(nombreUsuario.trim()))
                    .findFirst();

            if (!usuarioOpt.isPresent()) {
                return ResultadoOperacion.error("El usuario '" + nombreUsuario + "' no se encuentra en la comunidad.");
            }

            UsuarioComunidad usuario = usuarioOpt.get();
            Moderador moderador = comunidad.getModerador();

            boolean sancionLevantada = false;
            if (moderador instanceof ModeradorAutomatico) {
                ModeradorAutomatico modAuto = (ModeradorAutomatico) moderador;
                sancionLevantada = modAuto.levantarSancion(usuario);
            } else if (moderador instanceof ModeradorManual) {
                ModeradorManual modManual = (ModeradorManual) moderador;
                sancionLevantada = modManual.levantarSancion(usuario);
            }
            
            if (sancionLevantada) {
                return ResultadoOperacion.exito("Sanción levantada exitosamente para " + nombreUsuario);
            } else {
                return ResultadoOperacion.error("El usuario no tenía sanciones activas o no se pudo levantar la sanción.");
            }
            
        } catch (Exception e) {
            return ResultadoOperacion.error("Error al levantar sanción: " + e.getMessage());
        }
    }

    /**
     * Obtiene el estado de sanciones de un usuario
     */
    public String obtenerEstadoSanciones(String nombreUsuario, String nombreComunidad) {
        try {
            Optional<Comunidad> comunidadOpt = contexto.getComunidades().stream()
                    .filter(c -> c.getNombre().equalsIgnoreCase(nombreComunidad.trim()))
                    .findFirst();

            if (!comunidadOpt.isPresent()) {
                return "❌ No se encontró la comunidad '" + nombreComunidad + "'.";
            }

            Comunidad comunidad = comunidadOpt.get();
            
            Optional<UsuarioComunidad> usuarioOpt = comunidad.getUsuariosMiembros().stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(nombreUsuario.trim()))
                    .findFirst();

            if (!usuarioOpt.isPresent()) {
                return "❌ El usuario '" + nombreUsuario + "' no se encuentra en la comunidad.";
            }

            UsuarioComunidad usuario = usuarioOpt.get();
            Moderador moderador = comunidad.getModerador();

            boolean estaSancionado = false;
            SancionUsuario sancion = null;
            
            if (moderador instanceof ModeradorAutomatico) {
                ModeradorAutomatico modAuto = (ModeradorAutomatico) moderador;
                estaSancionado = modAuto.usuarioEstaSancionado(usuario);
                if (estaSancionado) {
                    sancion = modAuto.getSancionActiva(usuario);
                }
            } else if (moderador instanceof ModeradorManual) {
                ModeradorManual modManual = (ModeradorManual) moderador;
                estaSancionado = modManual.usuarioEstaSancionado(usuario);
                if (estaSancionado) {
                    sancion = modManual.getSancionActiva(usuario);
                }
            }

            if (estaSancionado && sancion != null) {
                return String.format("🚫 Usuario sancionado: %s\n   Tiempo restante: %d minutos\n   Razón: %s", 
                                    nombreUsuario, sancion.getMinutosRestantes(), sancion.getRazon());
            } else {
                return "✅ Usuario sin sanciones activas: " + nombreUsuario;
            }
            
        } catch (Exception e) {
            return "❌ Error al consultar estado: " + e.getMessage();
        }
    }

    /**
     * Obtiene estadísticas de moderación de una comunidad
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
            stats.append("📊 ESTADÍSTICAS DE MODERACIÓN\n\n");
            stats.append("🌐 Comunidad: ").append(nombreComunidad).append("\n");
            stats.append("🛡️ Moderador: ").append(moderador.getNombre()).append("\n");
            stats.append("📝 Tipo: ").append(moderador.getClass().getSimpleName()).append("\n\n");

            if (moderador instanceof ModeradorAutomatico) {
                ModeradorAutomatico modAuto = (ModeradorAutomatico) moderador;
                List<SancionUsuario> sancionesActivas = modAuto.getSancionesActivas();
                stats.append("🚫 Sanciones activas: ").append(sancionesActivas.size()).append("\n");
                
                if (!sancionesActivas.isEmpty()) {
                    stats.append("\n📋 Detalle de sanciones:\n");
                    for (SancionUsuario sancion : sancionesActivas) {
                        stats.append("  • ").append(sancion.getUsuario().getUsername())
                             .append(" - ").append(sancion.getMinutosRestantes()).append(" min restantes\n");
                    }
                }

                var estadisticas = modAuto.getEstadisticasModeración();
                stats.append("\n").append(estadisticas.toString());
                
            } else if (moderador instanceof ModeradorManual) {
                ModeradorManual modManual = (ModeradorManual) moderador;
                List<SancionUsuario> sancionesActivas = modManual.getSancionesActivas();
                stats.append("🚫 Sanciones activas: ").append(sancionesActivas.size()).append("\n");
                
                if (!sancionesActivas.isEmpty()) {
                    stats.append("\n📋 Detalle de sanciones:\n");
                    for (SancionUsuario sancion : sancionesActivas) {
                        stats.append("  • ").append(sancion.getUsuario().getUsername())
                             .append(" - ").append(sancion.getMinutosRestantes()).append(" min restantes\n");
                    }
                }
                
                stats.append("\n📋 Moderación manual activa");
            } else {
                stats.append("⚠️ Tipo de moderador desconocido");
            }

            return stats.toString();
            
        } catch (Exception e) {
            return "❌ Error al obtener estadísticas: " + e.getMessage();
        }
    }

    /**
     * Clase interna para encapsular el resultado de operaciones
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

        public boolean isExitoso() {
            return exitoso;
        }

        public String getMensaje() {
            return mensaje;
        }
    }
}
