package Comunidad_Modulo.modelo;

import Modulo_Usuario.Clases.UsuarioComunidad;
import Comunidad_Modulo.servicios.ModeradorService;
import Comunidad_Modulo.servicios.ModeradorService.ResultadoOperacion;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Moderador especializado en operaciones administrativas manuales.
 * Se encarga de la gestión directa de usuarios, comunidades y contenido mediante intervención humana.
 */
public class ModeradorManual extends Moderador {
    
    private ModeradorService moderadorService;
    private int operacionesRealizadas;
    private Map<String, Integer> tiposOperaciones;

    public ModeradorManual(String nombre, String username) {
        super(nombre, username);
        this.moderadorService = new ModeradorService();
        this.operacionesRealizadas = 0;
        this.tiposOperaciones = new HashMap<>();
    }

    public ModeradorManual(String nombre) {
        this(nombre, "manualmod_" + System.currentTimeMillis());
    }

    // Implementación de métodos abstractos
    
    @Override
    public boolean moderarContenido(String contenido, UsuarioComunidad autor) {
        // En moderación manual, se asume que el moderador revisa manualmente
        System.out.println("👤 MODERACIÓN MANUAL: Contenido revisado por " + this.nombre);
        incrementarOperacion("revision_contenido");
        
        // Por defecto, en moderación manual se aprueba (el moderador decide)
        return true;
    }

    @Override
    public String obtenerEstadisticas() {
        StringBuilder estadisticas = new StringBuilder();
        
        estadisticas.append("📊 === ESTADÍSTICAS MODERACIÓN MANUAL ===\n");
        estadisticas.append("Moderador: ").append(this.nombre).append("\n");
        estadisticas.append("Operaciones realizadas: ").append(operacionesRealizadas).append("\n");
        estadisticas.append("Comunidades gestionadas: ").append(getComunidadesGestionadas().size()).append("\n");
        
        if (!tiposOperaciones.isEmpty()) {
            estadisticas.append("\nDesglose de operaciones:\n");
            tiposOperaciones.forEach((tipo, cantidad) -> 
                estadisticas.append("  - ").append(tipo).append(": ").append(cantidad).append("\n")
            );
        }
        
        return estadisticas.toString();
    }

    @Override
    public void ejecutarAccionModeración(String tipoAccion, Object... parametros) {
        switch (tipoAccion.toLowerCase()) {
            case "eliminar_usuario":
                if (parametros.length >= 2) {
                    String nombreUsuario = (String) parametros[0];
                    String nombreComunidad = (String) parametros[1];
                    eliminarUsuarioDeComunidad(nombreUsuario, nombreComunidad);
                }
                break;
            case "eliminar_comunidad":
                if (parametros.length >= 1) {
                    String nombreComunidad = (String) parametros[0];
                    eliminarComunidad(nombreComunidad);
                }
                break;
            case "generar_reporte":
                generarReporteUsuarios();
                break;
            case "suspender_usuario_manual":
                if (parametros.length >= 1) {
                    UsuarioComunidad usuario = (UsuarioComunidad) parametros[0];
                    suspenderUsuarioManual(usuario);
                }
                break;
            default:
                System.out.println("⚠️ Acción de moderación manual no reconocida: " + tipoAccion);
        }
    }

    // Métodos específicos de moderación manual
    
    /**
     * Elimina un usuario de una comunidad específica
     */
    public ResultadoOperacion eliminarUsuarioDeComunidad(String nombreUsuario, String nombreComunidad) {
        ResultadoOperacion resultado = moderadorService.eliminarUsuarioDeComunidad(nombreUsuario, nombreComunidad);
        incrementarOperacion("eliminacion_usuario");
        
        if (resultado.isExitoso()) {
            System.out.println("👤 MODERACIÓN MANUAL: Usuario eliminado por " + this.nombre);
        }
        
        return resultado;
    }

    /**
     * Elimina una comunidad completa del sistema
     */
    public ResultadoOperacion eliminarComunidad(String nombreComunidad) {
        ResultadoOperacion resultado = moderadorService.eliminarComunidad(nombreComunidad);
        incrementarOperacion("eliminacion_comunidad");
        
        if (resultado.isExitoso()) {
            System.out.println("👤 MODERACIÓN MANUAL: Comunidad eliminada por " + this.nombre);
        }
        
        return resultado;
    }

    /**
     * Genera un reporte detallado de usuarios
     */
    public String generarReporteUsuarios() {
        String reporte = moderadorService.generarReporteUsuarios();
        incrementarOperacion("generacion_reporte");
        
        System.out.println("👤 MODERACIÓN MANUAL: Reporte generado por " + this.nombre);
        return reporte;
    }

    /**
     * Genera historial de un grupo específico
     */
    public String generarHistorialGrupo(String nombreComunidad, String tipoGrupo, String nombreGrupo) {
        String historial = moderadorService.generarHistorialGrupo(nombreComunidad, tipoGrupo, nombreGrupo);
        incrementarOperacion("generacion_historial");
        
        System.out.println("👤 MODERACIÓN MANUAL: Historial generado por " + this.nombre);
        return historial;
    }

    /**
     * Obtiene nombres de comunidades disponibles
     */
    public List<String> obtenerNombresComunidades() {
        incrementarOperacion("consulta_comunidades");
        return moderadorService.obtenerNombresComunidades();
    }

    /**
     * Obtiene grupos de una comunidad específica
     */
    public List<String> obtenerGruposDeComunidad(String nombreComunidad, String tipoGrupo) {
        incrementarOperacion("consulta_grupos");
        return moderadorService.obtenerGruposDeComunidad(nombreComunidad, tipoGrupo);
    }

    /**
     * Suspende un usuario manualmente con razón específica
     */
    public void suspenderUsuarioManual(UsuarioComunidad usuario) {
        super.suspenderUsuario(usuario);
        incrementarOperacion("suspension_manual");
        
        System.out.println("👤 MODERACIÓN MANUAL: Usuario " + usuario.getUsername() + 
                         " suspendido manualmente por " + this.nombre);
    }

    /**
     * Cierra un hilo manualmente con registro de la acción
     */
    public void cerrarHiloManual(HiloDiscusion hilo, String razon) {
        super.cerrarHilo(hilo);
        incrementarOperacion("cierre_hilo");
        
        System.out.println("👤 MODERACIÓN MANUAL: Hilo '" + hilo.getTitulo() + 
                         "' cerrado por " + this.nombre + ". Razón: " + razon);
    }

    /**
     * Elimina un mensaje manualmente con registro de la acción
     */
    public void eliminarMensajeManual(ChatPrivado chat, String idMensaje, String razon) {
        super.eliminarMensaje(chat, idMensaje);
        incrementarOperacion("eliminacion_mensaje");
        
        System.out.println("👤 MODERACIÓN MANUAL: Mensaje eliminado por " + this.nombre + 
                         " en chat " + chat.getIdChat() + ". Razón: " + razon);
    }

    /**
     * Realiza una revisión manual completa de una comunidad
     */
    public String revisarComunidadCompleta(String nombreComunidad) {
        StringBuilder reporte = new StringBuilder();
        reporte.append("🔍 === REVISIÓN MANUAL COMPLETA ===\n");
        reporte.append("Moderador: ").append(this.nombre).append("\n");
        reporte.append("Comunidad: ").append(nombreComunidad).append("\n\n");
        
        // Generar reporte de usuarios
        String reporteUsuarios = generarReporteUsuarios();
        reporte.append("👥 USUARIOS:\n").append(reporteUsuarios).append("\n");
        
        // Obtener información de grupos
        List<String> gruposDiscusion = obtenerGruposDeComunidad(nombreComunidad, "Grupos de Discusión");
        List<String> gruposCompartir = obtenerGruposDeComunidad(nombreComunidad, "Grupos de Compartir");
        
        reporte.append("💬 GRUPOS DE DISCUSIÓN: ").append(gruposDiscusion.size()).append("\n");
        gruposDiscusion.forEach(grupo -> reporte.append("  - ").append(grupo).append("\n"));
        
        reporte.append("\n📤 GRUPOS DE COMPARTIR: ").append(gruposCompartir.size()).append("\n");
        gruposCompartir.forEach(grupo -> reporte.append("  - ").append(grupo).append("\n"));
        
        incrementarOperacion("revision_completa");
        
        System.out.println("👤 MODERACIÓN MANUAL: Revisión completa realizada por " + this.nombre);
        return reporte.toString();
    }

    /**
     * Aprueba contenido manualmente
     */
    public void aprobarContenido(String contenido, UsuarioComunidad autor, String razon) {
        incrementarOperacion("aprobacion_contenido");
        System.out.println("✅ APROBACIÓN MANUAL por " + this.nombre + 
                         ": Contenido de " + autor.getUsername() + " aprobado. Razón: " + razon);
    }

    /**
     * Rechaza contenido manualmente
     */
    public void rechazarContenido(String contenido, UsuarioComunidad autor, String razon) {
        incrementarOperacion("rechazo_contenido");
        System.out.println("❌ RECHAZO MANUAL por " + this.nombre + 
                         ": Contenido de " + autor.getUsername() + " rechazado. Razón: " + razon);
    }

    /**
     * Aplica una sanción manual a un usuario
     */
    public SancionUsuario aplicarSancionManual(UsuarioComunidad usuario, String razon, int duracionMinutos) {
        incrementarOperacion("aplicar_sancion");
        System.out.println("👤 SANCIÓN MANUAL aplicada por " + this.nombre + 
                         " a " + usuario.getUsername() + ": " + razon);
        
        // Crear una sanción temporal (sin ModeracionService)
        SancionUsuario sancion = new SancionUsuario(usuario, razon, duracionMinutos, this.nombre);
        return sancion;
    }

    /**
     * Levanta una sanción de un usuario
     */
    public boolean levantarSancion(UsuarioComunidad usuario) {
        incrementarOperacion("levantar_sancion");
        System.out.println("👤 SANCIÓN LEVANTADA por " + this.nombre + 
                         " para usuario " + usuario.getUsername());
        
        // Por simplicidad, siempre retorna true (sanción levantada)
        return true;
    }

    /**
     * Verifica si un usuario está sancionado
     */
    public boolean usuarioEstaSancionado(UsuarioComunidad usuario) {
        // En moderación manual, por simplicidad asumimos que no hay sanciones automáticas
        return false;
    }

    /**
     * Obtiene la sanción activa de un usuario
     */
    public SancionUsuario getSancionActiva(UsuarioComunidad usuario) {
        // En moderación manual, retorna null si no hay sanciones
        return null;
    }

    /**
     * Obtiene todas las sanciones activas
     */
    public List<SancionUsuario> getSancionesActivas() {
        // En moderación manual, retorna lista vacía
        return new java.util.ArrayList<>();
    }

    // Métodos auxiliares
    
    private void incrementarOperacion(String tipoOperacion) {
        operacionesRealizadas++;
        tiposOperaciones.put(tipoOperacion, tiposOperaciones.getOrDefault(tipoOperacion, 0) + 1);
    }

    // Sobrescribir hooks del padre para comportamiento específico
    
    @Override
    protected void onComunidadAsignada(Comunidad comunidad) {
        System.out.println("👤 Moderador manual asignado a comunidad: " + comunidad.getNombre());
        incrementarOperacion("asignacion_comunidad");
        
        // Realizar revisión inicial
        revisarComunidadCompleta(comunidad.getNombre());
        System.out.println("📋 Revisión inicial completada");
    }

    @Override
    protected void onComunidadRemovida(Comunidad comunidad) {
        System.out.println("👤 Moderador manual removido de comunidad: " + comunidad.getNombre());
        incrementarOperacion("remocion_comunidad");
    }

    @Override
    protected void ejecutarModeracionForo(ForoGeneral foro) {
        System.out.println("👤 MODERACIÓN MANUAL: Revisando foro manualmente...");
        incrementarOperacion("moderacion_foro");
        // El moderador manual requiere intervención humana, solo registra la acción
    }

    @Override
    protected void ejecutarSupervisionChats(List<ChatPrivado> chats) {
        System.out.println("👤 MODERACIÓN MANUAL: Supervisando " + chats.size() + " chats manualmente");
        incrementarOperacion("supervision_chats");
        // Registrar para revisión manual posterior
    }

    // Getters específicos
    
    public ModeradorService getModeradorService() {
        return moderadorService;
    }

    public int getOperacionesRealizadas() {
        return operacionesRealizadas;
    }

    public Map<String, Integer> getTiposOperaciones() {
        return new HashMap<>(tiposOperaciones);
    }

    public int getOperacionesPorTipo(String tipo) {
        return tiposOperaciones.getOrDefault(tipo, 0);
    }

    /**
     * Reinicia las estadísticas de operaciones
     */
    public void reiniciarEstadisticas() {
        this.operacionesRealizadas = 0;
        this.tiposOperaciones.clear();
    }

    @Override
    public String toString() {
        return String.format("ModeradorManual: %s (realizó %d operaciones en %d comunidades)",
                           nombre, operacionesRealizadas, getComunidadesGestionadas().size());
    }
}
