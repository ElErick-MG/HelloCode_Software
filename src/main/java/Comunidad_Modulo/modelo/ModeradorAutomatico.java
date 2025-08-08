package Comunidad_Modulo.modelo;

import Modulo_Usuario.Clases.UsuarioComunidad;
import Comunidad_Modulo.utilidades.FiltroContenido;
import java.util.HashSet;
import java.util.Set;

/**
 * Moderador que realiza acciones de moderación automática basadas en filtros y reglas.
 * Responsabilidad: Moderación automática rápida usando algoritmos y filtros de contenido.
 */
public class ModeradorAutomatico extends ModeradorBase {
    
    // === ATRIBUTOS ESPECÍFICOS DE MODERACIÓN AUTOMÁTICA ===
    private boolean filtroActivado;
    private int sensibilidadFiltro; // 1-10, donde 10 es más estricto
    private Set<String> palabrasPersonalizadas;
    
    // === CONSTRUCTORES ===
    
    public ModeradorAutomatico(String nombre, String username) {
        super(nombre, username);
        this.filtroActivado = true;
        this.sensibilidadFiltro = 5; // Sensibilidad media por defecto
        this.palabrasPersonalizadas = new HashSet<>();
    }
    
    public ModeradorAutomatico(String nombre) {
        super(nombre, "mod_auto");
        this.filtroActivado = true;
        this.sensibilidadFiltro = 5;
        this.palabrasPersonalizadas = new HashSet<>();
    }
    
    // === IMPLEMENTACIÓN DEL MÉTODO ABSTRACTO ===
    
    /**
     * Procesa contenido automáticamente usando filtros y reglas predefinidas
     */
    @Override
    public ResultadoModeracion procesarContenido(String contenido, UsuarioComunidad autor) {
        // Verificar si el usuario ya está sancionado
        if (usuarioEstaSancionado(autor)) {
            SancionUsuario sancionActiva = getSancionActiva(autor);
            return new ResultadoModeracion(
                false, 
                "Usuario sancionado automáticamente. Tiempo restante: " + sancionActiva.getMinutosRestantes() + " minutos",
                sancionActiva
            );
        }
        
        // Si el filtro está desactivado, aprobar automáticamente
        if (!filtroActivado) {
            return new ResultadoModeracion(true, "Filtro automático desactivado - Contenido aprobado", null);
        }
        
        // Aplicar filtros automáticos
        return moderarMensajeAutomatico(contenido, autor);
    }
    
    // === MÉTODOS ESPECÍFICOS DE MODERACIÓN AUTOMÁTICA ===
    
    /**
     * Modera un mensaje automáticamente usando filtros
     */
    public ResultadoModeracion moderarMensajeAutomatico(String contenido, UsuarioComunidad autor) {
        // Verificar palabras personalizadas primero
        if (contienepalabrasPersonalizadas(contenido)) {
            SancionUsuario sancion = aplicarSancionAutomatica(autor, "Contenido inapropiado detectado", 30);
            return new ResultadoModeracion(false, "Mensaje bloqueado: contiene palabras prohibidas personalizadas", sancion);
        }
        
        // Usar el filtro de contenido existente
        FiltroContenido.ResultadoModeración resultado = FiltroContenido.analizarContenido(contenido);
        
        if (resultado.esInapropiado()) {
            // Ajustar duración de sanción según sensibilidad
            int duracionBase = resultado.getDuracionSancionMinutos();
            int duracionAjustada = calcularDuracionPorSensibilidad(duracionBase);
            
            SancionUsuario sancion = aplicarSancionAutomatica(
                autor, 
                resultado.getRazonSancion(), 
                duracionAjustada
            );
            
            return new ResultadoModeracion(
                false,
                "Mensaje bloqueado automáticamente: " + resultado.getRazonSancion() + 
                ". Sanción de " + duracionAjustada + " minutos aplicada.",
                sancion
            );
        }
        
        return new ResultadoModeracion(true, "Mensaje aprobado por filtro automático", null);
    }
    
    /**
     * Aplica sanción automática con logging específico
     */
    private SancionUsuario aplicarSancionAutomatica(UsuarioComunidad usuario, String razon, int duracionMinutos) {
        String razonAutomatica = "[AUTO] " + razon;
        SancionUsuario sancion = aplicarSancion(usuario, razonAutomatica, duracionMinutos);
        
        System.out.println("🤖 SANCIÓN AUTOMÁTICA APLICADA");
        System.out.println("   Sistema: " + this.nombre);
        System.out.println("   Usuario: " + usuario.getNombre());
        System.out.println("   Razón: " + razonAutomatica);
        System.out.println("   Duración: " + duracionMinutos + " minutos");
        
        return sancion;
    }
    
    // === MÉTODOS DE CONFIGURACIÓN ===
    
    /**
     * Configura la sensibilidad del filtro automático
     */
    public void configurarSensibilidad(int sensibilidad) {
        if (sensibilidad >= 1 && sensibilidad <= 10) {
            this.sensibilidadFiltro = sensibilidad;
            System.out.println("🔧 Sensibilidad del filtro configurada a: " + sensibilidad);
        } else {
            System.out.println("❌ Sensibilidad debe estar entre 1 y 10");
        }
    }
    
    /**
     * Activa o desactiva el filtro automático
     */
    public void toggleFiltro(boolean activar) {
        this.filtroActivado = activar;
        System.out.println(activar ? "✅ Filtro automático activado" : "❌ Filtro automático desactivado");
    }
    
    /**
     * Añade palabras personalizadas al filtro
     */
    public void agregarPalabraProhibida(String palabra) {
        palabrasPersonalizadas.add(palabra.toLowerCase());
        System.out.println("🚫 Palabra añadida al filtro personalizado: " + palabra);
    }
    
    /**
     * Elimina palabras del filtro personalizado
     */
    public void removerPalabraProhibida(String palabra) {
        if (palabrasPersonalizadas.remove(palabra.toLowerCase())) {
            System.out.println("✅ Palabra removida del filtro personalizado: " + palabra);
        } else {
            System.out.println("❌ Palabra no encontrada en el filtro personalizado: " + palabra);
        }
    }
    
    /**
     * Obtiene las palabras personalizadas configuradas
     */
    public Set<String> getPalabrasPersonalizadas() {
        return new HashSet<>(palabrasPersonalizadas);
    }
    
    // === MÉTODOS AUXILIARES PRIVADOS ===
    
    /**
     * Verifica si el contenido contiene palabras personalizadas prohibidas
     */
    private boolean contienepalabrasPersonalizadas(String contenido) {
        String contenidoLimpio = contenido.toLowerCase().replaceAll("[^a-záéíóúüñ\\s]", "");
        String[] palabras = contenidoLimpio.split("\\s+");
        
        for (String palabra : palabras) {
            if (palabrasPersonalizadas.contains(palabra)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Calcula la duración de sanción ajustada por sensibilidad
     */
    private int calcularDuracionPorSensibilidad(int duracionBase) {
        // Sensibilidad 1-3: reduce duración, 4-6: mantiene, 7-10: aumenta
        double factor = 0.5 + (sensibilidadFiltro * 0.15); // 0.65 a 2.0
        return (int) Math.max(1, duracionBase * factor);
    }
    
    // === GETTERS Y SETTERS ===
    
    public boolean isFiltroActivado() {
        return filtroActivado;
    }
    
    public int getSensibilidadFiltro() {
        return sensibilidadFiltro;
    }
    
    /**
     * Muestra la configuración actual del moderador automático
     */
    public void mostrarConfiguracion() {
        System.out.println("\n🤖 === CONFIGURACIÓN MODERADOR AUTOMÁTICO ===");
        System.out.println("Nombre: " + this.nombre);
        System.out.println("Filtro activado: " + (filtroActivado ? "✅ SÍ" : "❌ NO"));
        System.out.println("Sensibilidad: " + sensibilidadFiltro + "/10");
        System.out.println("Palabras personalizadas: " + palabrasPersonalizadas.size());
        if (!palabrasPersonalizadas.isEmpty()) {
            System.out.println("Lista: " + palabrasPersonalizadas);
        }
        System.out.println("=".repeat(45));
    }
    
    @Override
    public String toString() {
        return String.format("ModeradorAutomatico: %s (@%s) - Filtro: %s, Sensibilidad: %d/10", 
                           nombre, username, 
                           filtroActivado ? "ON" : "OFF", 
                           sensibilidadFiltro);
    }
}
