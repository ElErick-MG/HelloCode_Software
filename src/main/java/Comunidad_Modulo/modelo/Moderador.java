package Comunidad_Modulo.modelo;

import Modulo_Usuario.Clases.UsuarioComunidad;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Clase padre abstracta que representa a un moderador genérico del sistema.
 * Define la funcionalidad común que comparten todos los tipos de moderadores.
 * 
 * PATRÓN FACTORY INTEGRADO:
 * Esta clase incluye métodos factory estáticos que encapsulan la creación
 * de diferentes tipos de moderadores, eliminando la necesidad de una clase
 * Factory separada y manteniendo el UML más limpio.
 * 
 * TIPOS DE MODERADORES:
 * - ModeradorAutomatico: Moderación automática con filtros y reglas
 * - ModeradorManual: Moderación manual con intervención humana
 * 
 * USO:
 * - Moderador.crear(TipoModerador.AUTOMATICO, "nombre", "username")
 * - Moderador.crearAutomaticoPorDefecto("ComunidadX")
 * - Moderador.crearManualPorDefecto("AdminY")
 */
public abstract class Moderador {
    protected String idModerador;
    protected String nombre;
    protected String username;
    protected List<Comunidad> comunidadesGestionadas;

    /**
     * Enum para los tipos de moderadores disponibles
     */
    public enum TipoModerador {
        AUTOMATICO,
        MANUAL
    }

    public Moderador(String nombre, String username) {
        this.idModerador = UUID.randomUUID().toString();
        this.nombre = nombre;
        this.username = username;
        this.comunidadesGestionadas = new ArrayList<>();
    }

    // Constructor existente para mantener compatibilidad
    public Moderador(String nombre) {
        this(nombre, "mod_" + System.currentTimeMillis()); // Username único por defecto
    }

    // ==========================================
    // MÉTODOS FACTORY ESTÁTICOS
    // ==========================================
    
    /**
     * Crea un moderador del tipo especificado
     * 
     * @param tipo Tipo de moderador a crear
     * @param nombre Nombre del moderador
     * @param username Nombre de usuario del moderador
     * @return Instancia del moderador creado
     */
    public static Moderador crear(TipoModerador tipo, String nombre, String username) {
        switch (tipo) {
            case AUTOMATICO:
                return new ModeradorAutomatico(nombre, username);
            case MANUAL:
                return new ModeradorManual(nombre, username);
            default:
                throw new IllegalArgumentException("Tipo de moderador no soportado: " + tipo);
        }
    }

    /**
     * Crea un moderador del tipo especificado con username automático
     * 
     * @param tipo Tipo de moderador a crear
     * @param nombre Nombre del moderador
     * @return Instancia del moderador creado
     */
    public static Moderador crear(TipoModerador tipo, String nombre) {
        switch (tipo) {
            case AUTOMATICO:
                return new ModeradorAutomatico(nombre);
            case MANUAL:
                return new ModeradorManual(nombre);
            default:
                throw new IllegalArgumentException("Tipo de moderador no soportado: " + tipo);
        }
    }

    /**
     * Crea un moderador automático por defecto para una comunidad
     */
    public static ModeradorAutomatico crearAutomaticoPorDefecto(String nombreComunidad) {
        return new ModeradorAutomatico("AutoMod_" + nombreComunidad, "automod_" + nombreComunidad.toLowerCase());
    }

    /**
     * Crea un moderador manual por defecto para administración
     */
    public static ModeradorManual crearManualPorDefecto(String nombreAdmin) {
        return new ModeradorManual("Admin_" + nombreAdmin, "admin_" + nombreAdmin.toLowerCase());
    }

    /**
     * Obtiene información sobre los tipos de moderadores disponibles
     */
    public static String obtenerInformacionTipos() {
        StringBuilder info = new StringBuilder();
        info.append("🔧 === TIPOS DE MODERADORES DISPONIBLES ===\n\n");
        
        info.append("🤖 MODERADOR AUTOMÁTICO:\n");
        info.append("  - Moderación de contenido en tiempo real\n");
        info.append("  - Aplicación automática de sanciones\n");
        info.append("  - Análisis de sentimientos y filtros\n");
        info.append("  - Estadísticas de moderación automática\n\n");
        
        info.append("👤 MODERADOR MANUAL:\n");
        info.append("  - Gestión administrativa del sistema\n");
        info.append("  - Eliminación de usuarios y comunidades\n");
        info.append("  - Generación de reportes detallados\n");
        info.append("  - Revisión manual de contenido\n\n");
        
        info.append("💡 USO RECOMENDADO:\n");
        info.append("  - Automático: Para comunidades con alto volumen de mensajes\n");
        info.append("  - Manual: Para administración y casos especiales\n");
        info.append("  - Híbrido: Usar ambos tipos en la misma comunidad\n");
        
        return info.toString();
    }

    // Getters y setters comunes
    public String getIdModerador() {
        return idModerador;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<Comunidad> getComunidadesGestionadas() {
        return new ArrayList<>(comunidadesGestionadas);
    }

    // Métodos de gestión de comunidades (común para todos los moderadores)
    public void asignarComunidad(Comunidad comunidad) {
        if (!comunidadesGestionadas.contains(comunidad)) {
            comunidadesGestionadas.add(comunidad);
            onComunidadAsignada(comunidad);
        }
    }

    public void removerComunidad(Comunidad comunidad) {
        if (comunidadesGestionadas.remove(comunidad)) {
            onComunidadRemovida(comunidad);
        }
    }

    // Métodos básicos de moderación comunes
    public void moderarForo(ForoGeneral foro) {
        System.out.println("Moderando foro: " + foro.toString());
        ejecutarModeracionForo(foro);
    }

    public void supervisarChats(List<ChatPrivado> chats) {
        System.out.println("Supervisando " + chats.size() + " chats privados");
        ejecutarSupervisionChats(chats);
    }

    public void cerrarHilo(HiloDiscusion hilo) {
        hilo.cerrar();
        System.out.println("Hilo cerrado por moderador: " + hilo.getTitulo());
        onHiloCerrado(hilo);
    }

    public void eliminarMensaje(ChatPrivado chat, String idMensaje) {
        System.out.println("Mensaje eliminado por moderador en chat: " + chat.getIdChat());
        ejecutarEliminacionMensaje(chat, idMensaje);
    }

    public void suspenderUsuario(UsuarioComunidad usuario) {
        System.out.println("Usuario suspendido: " + usuario.getNombre());
        ejecutarSuspensionUsuario(usuario);
    }

    // Métodos abstractos que deben implementar las clases hijas
    
    /**
     * Método abstracto para moderar contenido específico según el tipo de moderador
     */
    public abstract boolean moderarContenido(String contenido, UsuarioComunidad autor);
    
    /**
     * Método abstracto para obtener estadísticas específicas del tipo de moderador
     */
    public abstract String obtenerEstadisticas();
    
    /**
     * Método abstracto para ejecutar acciones específicas de moderación
     */
    public abstract void ejecutarAccionModeración(String tipoAccion, Object... parametros);

    // Métodos template (hooks) que las clases hijas pueden sobrescribir
    
    protected void onComunidadAsignada(Comunidad comunidad) {
        // Hook para cuando se asigna una comunidad
    }
    
    protected void onComunidadRemovida(Comunidad comunidad) {
        // Hook para cuando se remueve una comunidad
    }
    
    protected void onHiloCerrado(HiloDiscusion hilo) {
        // Hook para cuando se cierra un hilo
    }
    
    protected void ejecutarModeracionForo(ForoGeneral foro) {
        // Implementación base, las clases hijas pueden sobrescribir
    }
    
    protected void ejecutarSupervisionChats(List<ChatPrivado> chats) {
        // Implementación base, las clases hijas pueden sobrescribir
    }
    
    protected void ejecutarEliminacionMensaje(ChatPrivado chat, String idMensaje) {
        // Implementación base, las clases hijas pueden sobrescribir
    }
    
    protected void ejecutarSuspensionUsuario(UsuarioComunidad usuario) {
        // Implementación base, las clases hijas pueden sobrescribir
    }

    // Métodos de utilidad comunes
    public boolean tieneComunidadAsignada(String nombreComunidad) {
        return comunidadesGestionadas.stream()
                .anyMatch(c -> c.getNombre().equalsIgnoreCase(nombreComunidad));
    }

    public int getNumeroComunidadesGestionadas() {
        return comunidadesGestionadas.size();
    }

    public boolean estáActivo() {
        return !comunidadesGestionadas.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("Moderador: %s [%s] (gestiona %d comunidades)",
                           nombre, getClass().getSimpleName(), comunidadesGestionadas.size());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Moderador moderador = (Moderador) obj;
        return idModerador.equals(moderador.idModerador);
    }

    @Override
    public int hashCode() {
        return idModerador.hashCode();
    }
}