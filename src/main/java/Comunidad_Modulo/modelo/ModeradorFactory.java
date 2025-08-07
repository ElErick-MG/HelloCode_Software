package Comunidad_Modulo.modelo;

import Modulo_Usuario.Clases.UsuarioComunidad;

/**
 * Factory para crear diferentes tipos de moderadores.
 * Implementa el patrón Factory Method para encapsular la creación de moderadores.
 */
public class ModeradorFactory {

    /**
     * Enum para los tipos de moderadores disponibles
     */
    public enum TipoModerador {
        AUTOMATICO,
        MANUAL
    }

    /**
     * Crea un moderador del tipo especificado
     * 
     * @param tipo Tipo de moderador a crear
     * @param nombre Nombre del moderador
     * @param username Nombre de usuario del moderador
     * @return Instancia del moderador creado
     */
    public static Moderador crearModerador(TipoModerador tipo, String nombre, String username) {
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
    public static Moderador crearModerador(TipoModerador tipo, String nombre) {
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
    public static ModeradorAutomatico crearModeradorAutomaticoPorDefecto(String nombreComunidad) {
        return new ModeradorAutomatico("AutoMod_" + nombreComunidad, "automod_" + nombreComunidad.toLowerCase());
    }

    /**
     * Crea un moderador manual por defecto para administración
     */
    public static ModeradorManual crearModeradorManualPorDefecto(String nombreAdmin) {
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

    /**
     * Crea un conjunto híbrido de moderadores (automático + manual) para una comunidad
     */
    public static ModeradorHibrido crearModeradorHibrido(String nombreComunidad, String nombreAdmin) {
        ModeradorAutomatico automatico = crearModeradorAutomaticoPorDefecto(nombreComunidad);
        ModeradorManual manual = crearModeradorManualPorDefecto(nombreAdmin);
        
        return new ModeradorHibrido(nombreComunidad + "_Hybrid", automatico, manual);
    }

    /**
     * Clase que combina moderación automática y manual
     */
    public static class ModeradorHibrido {
        private final String nombre;
        private final ModeradorAutomatico moderadorAutomatico;
        private final ModeradorManual moderadorManual;

        public ModeradorHibrido(String nombre, ModeradorAutomatico automatico, ModeradorManual manual) {
            this.nombre = nombre;
            this.moderadorAutomatico = automatico;
            this.moderadorManual = manual;
        }

        public void asignarComunidad(Comunidad comunidad) {
            moderadorAutomatico.asignarComunidad(comunidad);
            moderadorManual.asignarComunidad(comunidad);
            System.out.println("🔄 Moderación híbrida activada para comunidad: " + comunidad.getNombre());
        }

        public boolean moderarContenido(String contenido, UsuarioComunidad autor) {
            // Primero moderar automáticamente
            boolean aprobadoAutomatico = moderadorAutomatico.moderarContenido(contenido, autor);
            
            if (!aprobadoAutomatico) {
                // Si es rechazado automáticamente, registrar para revisión manual
                System.out.println("⚠️ Contenido marcado para revisión manual por " + moderadorManual.getNombre());
                return false;
            }
            
            return true;
        }

        public String obtenerEstadisticasCompletas() {
            StringBuilder stats = new StringBuilder();
            stats.append("📊 === ESTADÍSTICAS MODERACIÓN HÍBRIDA ===\n");
            stats.append("Sistema: ").append(nombre).append("\n\n");
            
            stats.append(moderadorAutomatico.obtenerEstadisticas()).append("\n");
            stats.append(moderadorManual.obtenerEstadisticas());
            
            return stats.toString();
        }

        // Getters
        public String getNombre() { return nombre; }
        public ModeradorAutomatico getModeradorAutomatico() { return moderadorAutomatico; }
        public ModeradorManual getModeradorManual() { return moderadorManual; }

        @Override
        public String toString() {
            return String.format("ModeradorHibrido: %s [Auto: %s, Manual: %s]", 
                               nombre, moderadorAutomatico.getNombre(), moderadorManual.getNombre());
        }
    }
}
