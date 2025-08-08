package Comunidad_Modulo.modelo;

import Modulo_Usuario.Clases.UsuarioComunidad;
import java.util.List;

/**
 * Interfaz base para todos los tipos de moderadores.
 * Define el contrato común que deben cumplir todos los moderadores.
 */
public interface IModerador {
    
    // === MÉTODOS DE IDENTIFICACIÓN ===
    String getIdModerador();
    String getNombre();
    String getUsername();
    void setNombre(String nombre);
    void setUsername(String username);
    
    // === MÉTODOS DE GESTIÓN DE COMUNIDADES ===
    void asignarComunidad(Comunidad comunidad);
    void removerComunidad(Comunidad comunidad);
    List<Comunidad> getComunidadesGestionadas();
    
    // === MÉTODO PRINCIPAL DE MODERACIÓN ===
    /**
     * Procesa contenido para determinar si debe ser aprobado o rechazado.
     * Cada tipo de moderador implementará su propia lógica.
     */
    ResultadoModeracion procesarContenido(String contenido, UsuarioComunidad autor);
    
    // === MÉTODOS DE GESTIÓN DE SANCIONES ===
    boolean usuarioEstaSancionado(UsuarioComunidad usuario);
    SancionUsuario getSancionActiva(UsuarioComunidad usuario);
    List<SancionUsuario> getSancionesActivas();
    List<SancionUsuario> getHistorialSanciones(UsuarioComunidad usuario);
    
    // === MÉTODOS DE ESTADÍSTICAS ===
    EstadisticasModeración getEstadisticasModeración();
    void mostrarEstadoModeración();
    
    // === CLASES INTERNAS COMPARTIDAS ===
    
    /**
     * Clase para encapsular el resultado de la moderación
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
     * Clase para estadísticas de moderación
     */
    public static class EstadisticasModeración {
        private final int totalSanciones;
        private final int sancionesActivas;
        private final java.util.Map<String, Integer> tiposSanciones;
        
        public EstadisticasModeración(int totalSanciones, int sancionesActivas, java.util.Map<String, Integer> tiposSanciones) {
            this.totalSanciones = totalSanciones;
            this.sancionesActivas = sancionesActivas;
            this.tiposSanciones = tiposSanciones;
        }
        
        public int getTotalSanciones() { return totalSanciones; }
        public int getSancionesActivas() { return sancionesActivas; }
        public java.util.Map<String, Integer> getTiposSanciones() { return tiposSanciones; }
        
        @Override
        public String toString() {
            return String.format("📊 Estadísticas: %d sanciones totales, %d activas", 
                               totalSanciones, sancionesActivas);
        }
    }
}
