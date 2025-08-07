package Comunidad_Modulo.modelo;

import Modulo_Usuario.Clases.UsuarioComunidad;
import Comunidad_Modulo.servicios.ModeracionService;
import Comunidad_Modulo.servicios.ModeracionService.ResultadoModeracion;

import java.util.List;

/**
 * Moderador especializado en moderación automática de contenido.
 * Se encarga de analizar y filtrar contenido en tiempo real usando algoritmos automáticos.
 */
public class ModeradorAutomatico extends Moderador {
    
    private ModeracionService moderacionService;
    private int mensajesModerados;
    private int sancionesAplicadas;

    public ModeradorAutomatico(String nombre, String username) {
        super(nombre, username);
        this.moderacionService = new ModeracionService();
        this.mensajesModerados = 0;
        this.sancionesAplicadas = 0;
    }

    public ModeradorAutomatico(String nombre) {
        this(nombre, "automod_" + System.currentTimeMillis());
    }

    // Implementación de métodos abstractos
    
    @Override
    public boolean moderarContenido(String contenido, UsuarioComunidad autor) {
        ResultadoModeracion resultado = moderacionService.moderarMensaje(contenido, autor, this.nombre);
        mensajesModerados++;
        
        if (!resultado.isAprobado()) {
            sancionesAplicadas++;
            System.out.println("🤖 MODERACIÓN AUTOMÁTICA: " + resultado.getMensaje());
        }
        
        return resultado.isAprobado();
    }

    @Override
    public String obtenerEstadisticas() {
        ModeracionService.EstadisticasModeración stats = moderacionService.getEstadisticas();
        StringBuilder estadisticas = new StringBuilder();
        
        estadisticas.append("📊 === ESTADÍSTICAS MODERACIÓN AUTOMÁTICA ===\n");
        estadisticas.append("Moderador: ").append(this.nombre).append("\n");
        estadisticas.append("Mensajes moderados: ").append(mensajesModerados).append("\n");
        estadisticas.append("Sanciones aplicadas: ").append(sancionesAplicadas).append("\n");
        estadisticas.append("Tasa de rechazo: ").append(
            mensajesModerados > 0 ? String.format("%.2f%%", (sancionesAplicadas * 100.0) / mensajesModerados) : "0%"
        ).append("\n");
        estadisticas.append(stats.toString());
        
        return estadisticas.toString();
    }

    @Override
    public void ejecutarAccionModeración(String tipoAccion, Object... parametros) {
        switch (tipoAccion.toLowerCase()) {
            case "moderar_mensaje":
                if (parametros.length >= 2) {
                    String contenido = (String) parametros[0];
                    UsuarioComunidad autor = (UsuarioComunidad) parametros[1];
                    moderarContenido(contenido, autor);
                }
                break;
            case "aplicar_sancion":
                if (parametros.length >= 3) {
                    UsuarioComunidad usuario = (UsuarioComunidad) parametros[0];
                    String razon = (String) parametros[1];
                    int duracion = (Integer) parametros[2];
                    aplicarSancionAutomatica(usuario, razon, duracion);
                }
                break;
            case "verificar_usuario":
                if (parametros.length >= 1) {
                    UsuarioComunidad usuario = (UsuarioComunidad) parametros[0];
                    verificarEstadoUsuario(usuario);
                }
                break;
            default:
                System.out.println("⚠️ Acción de moderación automática no reconocida: " + tipoAccion);
        }
    }

    // Métodos específicos de moderación automática
    
    /**
     * Modera un mensaje automáticamente antes de publicarlo
     */
    public ResultadoModeracion moderarMensaje(String contenido, UsuarioComunidad autor) {
        return moderacionService.moderarMensaje(contenido, autor, this.nombre);
    }

    /**
     * Aplica una sanción automática basada en el análisis de contenido
     */
    public SancionUsuario aplicarSancionAutomatica(UsuarioComunidad usuario, String razon, int duracionMinutos) {
        sancionesAplicadas++;
        return moderacionService.aplicarSancion(usuario, razon, duracionMinutos, this.nombre);
    }

    /**
     * Aplica una sanción manual desde el moderador automático
     */
    public SancionUsuario aplicarSancionManual(UsuarioComunidad usuario, String razon, int duracionMinutos) {
        sancionesAplicadas++;
        return moderacionService.aplicarSancion(usuario, razon, duracionMinutos, this.nombre + " (Manual)");
    }

    /**
     * Verifica si un usuario está sancionado
     */
    public boolean usuarioEstaSancionado(UsuarioComunidad usuario) {
        return moderacionService.usuarioEstaSancionado(usuario);
    }

    /**
     * Levanta una sanción de un usuario
     */
    public boolean levantarSancion(UsuarioComunidad usuario) {
        return moderacionService.levantarSancion(usuario, this.nombre);
    }

    /**
     * Obtiene información de la sanción activa de un usuario
     */
    public SancionUsuario getSancionActiva(UsuarioComunidad usuario) {
        return moderacionService.obtenerSancionActiva(usuario);
    }

    /**
     * Obtiene todas las sanciones activas
     */
    public List<SancionUsuario> getSancionesActivas() {
        return moderacionService.getSancionesActivas();
    }

    /**
     * Obtiene el historial de sanciones de un usuario
     */
    public List<SancionUsuario> getHistorialSanciones(UsuarioComunidad usuario) {
        return moderacionService.getHistorialSanciones(usuario);
    }

    /**
     * Verifica el estado actual de un usuario en el sistema
     */
    public void verificarEstadoUsuario(UsuarioComunidad usuario) {
        boolean sancionado = usuarioEstaSancionado(usuario);
        if (sancionado) {
            SancionUsuario sancion = getSancionActiva(usuario);
            System.out.println("🚫 Usuario " + usuario.getUsername() + " está sancionado: " + sancion.getRazon());
        } else {
            System.out.println("✅ Usuario " + usuario.getUsername() + " sin sanciones activas");
        }
    }

    /**
     * Ejecuta un análisis automático de todo el contenido de un foro
     */
    public void analizarForoCompleto(ForoGeneral foro) {
        System.out.println("🔍 Iniciando análisis automático del foro...");
        
        // Analizar grupos de discusión
        for (GrupoDiscusion grupo : foro.getGruposDiscusion()) {
            analizarGrupoDiscusion(grupo);
        }
        
        // Analizar grupos de compartir
        for (GrupoCompartir grupo : foro.getGruposCompartir()) {
            analizarGrupoCompartir(grupo);
        }
        
        System.out.println("✅ Análisis automático completado");
    }

    private void analizarGrupoDiscusion(GrupoDiscusion grupo) {
        System.out.println("📝 Analizando grupo de discusión: " + grupo.getTitulo());
        // Aquí se implementaría la lógica de análisis automático
    }

    private void analizarGrupoCompartir(GrupoCompartir grupo) {
        System.out.println("📤 Analizando grupo de compartir: " + grupo.getTitulo());
        // Aquí se implementaría la lógica de análisis automático
    }

    // Sobrescribir hooks del padre para comportamiento específico
    
    @Override
    protected void onComunidadAsignada(Comunidad comunidad) {
        System.out.println("🤖 Moderador automático asignado a comunidad: " + comunidad.getNombre());
        // Iniciar monitoreo automático
        if (comunidad.getForoGeneral() != null) {
            analizarForoCompleto(comunidad.getForoGeneral());
        }
    }

    @Override
    protected void ejecutarModeracionForo(ForoGeneral foro) {
        analizarForoCompleto(foro);
    }

    @Override
    protected void ejecutarSupervisionChats(List<ChatPrivado> chats) {
        System.out.println("🤖 Supervisión automática de " + chats.size() + " chats iniciada");
        for (ChatPrivado chat : chats) {
            analizarChat(chat);
        }
    }

    private void analizarChat(ChatPrivado chat) {
        System.out.println("💬 Analizando chat: " + chat.getIdChat());
        // Implementar análisis automático de mensajes del chat
    }

    /**
     * Obtiene estadísticas detalladas de moderación
     */
    public ModeracionService.EstadisticasModeración getEstadisticasModeración() {
        return moderacionService.getEstadisticas();
    }

    // Getters específicos
    
    public ModeracionService getModeracionService() {
        return moderacionService;
    }

    public int getMensajesModerados() {
        return mensajesModerados;
    }

    public int getSancionesAplicadas() {
        return sancionesAplicadas;
    }

    public double getTasaRechazo() {
        return mensajesModerados > 0 ? (sancionesAplicadas * 100.0) / mensajesModerados : 0.0;
    }

    /**
     * Reinicia las estadísticas de moderación
     */
    public void reiniciarEstadisticas() {
        this.mensajesModerados = 0;
        this.sancionesAplicadas = 0;
    }

    @Override
    public String toString() {
        return String.format("ModeradorAutomatico: %s (moderó %d mensajes, aplicó %d sanciones)",
                           nombre, mensajesModerados, sancionesAplicadas);
    }
}
