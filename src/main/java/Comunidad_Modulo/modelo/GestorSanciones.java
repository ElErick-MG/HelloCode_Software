package Comunidad_Modulo.modelo;

import Modulo_Usuario.Clases.UsuarioComunidad;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Clase responsable de gestionar todas las sanciones de usuarios.
 * Aplica el principio de Single Responsibility - solo maneja sanciones.
 */
public class GestorSanciones {
    
    // Mapa de usuario -> lista de sanciones
    private Map<String, List<SancionUsuario>> sancionesPorUsuario;
    
    // Lista de todas las sanciones activas
    private List<SancionUsuario> sancionesActivas;
    
    public GestorSanciones() {
        this.sancionesPorUsuario = new HashMap<>();
        this.sancionesActivas = new ArrayList<>();
    }
    
    /**
     * Aplica una sanción a un usuario
     */
    public SancionUsuario aplicarSancion(UsuarioComunidad usuario, String razon, int duracionMinutos, String moderadorResponsable) {
        SancionUsuario sancion = new SancionUsuario(usuario, razon, duracionMinutos, moderadorResponsable);
        
        // Agregar a la lista de sanciones del usuario
        sancionesPorUsuario.computeIfAbsent(usuario.getIdUsuario(), k -> new ArrayList<>()).add(sancion);
        
        // Agregar a la lista de sanciones activas
        sancionesActivas.add(sancion);
        
        System.out.println("🚫 SANCIÓN APLICADA: " + sancion.toString());
        
        return sancion;
    }
    
    /**
     * Verifica si un usuario está actualmente sancionado
     */
    public boolean usuarioEstaSancionado(UsuarioComunidad usuario) {
        return obtenerSancionActiva(usuario) != null;
    }
    
    /**
     * Obtiene la sanción activa de un usuario (si la tiene)
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
     * Levanta una sanción manualmente
     */
    public boolean levantarSancion(UsuarioComunidad usuario, String moderadorResponsable) {
        SancionUsuario sancionActiva = obtenerSancionActiva(usuario);
        if (sancionActiva != null) {
            sancionActiva.levantarSancion();
            System.out.println("✅ Sanción levantada por " + moderadorResponsable + ": " + sancionActiva.toString());
            return true;
        }
        return false;
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
    
    /**
     * Obtiene estadísticas de moderación
     */
    public IModerador.EstadisticasModeración getEstadisticas() {
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
        
        return new IModerador.EstadisticasModeración(totalSanciones, sancionesActivas, tiposSanciones);
    }
    
    /**
     * Limpia sanciones expiradas
     */
    public void limpiarSancionesExpiradas() {
        sancionesActivas.removeIf(sancion -> !sancion.estaActiva());
    }
}
