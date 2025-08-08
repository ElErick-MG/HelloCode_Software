package Comunidad_Modulo.modelo;

import Modulo_Usuario.Clases.UsuarioComunidad;

/**
 * Moderador que realiza acciones de moderación manual basadas en decisiones humanas.
 * Responsabilidad: Moderación deliberada y contextual por parte de un humano.
 */
public class ModeradorManual extends ModeradorBase {
    
    public ModeradorManual(String nombre, String username) {
        super(nombre, username);
    }
    
    public ModeradorManual(String nombre) {
        super(nombre, "mod_manual");
    }
    
    // === IMPLEMENTACIÓN DEL MÉTODO ABSTRACTO ===
    
    /**
     * En el moderador manual, el procesamiento de contenido requiere intervención humana.
     * Por defecto, aprueba el contenido ya que la decisión manual se toma posteriormente.
     */
    @Override
    public ResultadoModeracion procesarContenido(String contenido, UsuarioComunidad autor) {
        // Verificar si el usuario ya está sancionado
        if (usuarioEstaSancionado(autor)) {
            SancionUsuario sancionActiva = getSancionActiva(autor);
            return new ResultadoModeracion(
                false, 
                "Usuario sancionado. Tiempo restante: " + sancionActiva.getMinutosRestantes() + " minutos",
                sancionActiva
            );
        }
        
        // En moderación manual, el contenido se aprueba por defecto
        // La moderación ocurre después mediante revisión manual
        return new ResultadoModeracion(true, "Contenido pendiente de revisión manual", null);
    }
    
    // === MÉTODOS ESPECÍFICOS DE MODERACIÓN MANUAL ===
    
    /**
     * Aplica una sanción manual con justificación detallada
     */
    public SancionUsuario aplicarSancionManual(UsuarioComunidad usuario, String razon, int duracionMinutos, String justificacion) {
        String razonCompleta = razon + " - Justificación: " + justificacion;
        SancionUsuario sancion = aplicarSancion(usuario, razonCompleta, duracionMinutos);
        
        System.out.println("🔨 SANCIÓN MANUAL APLICADA por " + this.nombre);
        System.out.println("   Usuario: " + usuario.getNombre());
        System.out.println("   Razón: " + razonCompleta);
        System.out.println("   Duración: " + duracionMinutos + " minutos");
        
        return sancion;
    }
    
    /**
     * Aplica una sanción manual simple (método de conveniencia)
     */
    public SancionUsuario aplicarSancionManual(UsuarioComunidad usuario, String razon, int duracionMinutos) {
        return aplicarSancion(usuario, razon, duracionMinutos);
    }
    
    /**
     * Levanta una sanción con justificación
     */
    public boolean levantarSancionConJustificacion(UsuarioComunidad usuario, String justificacion) {
        boolean resultado = levantarSancion(usuario);
        if (resultado) {
            System.out.println("✅ Sanción levantada manualmente por " + this.nombre);
            System.out.println("   Justificación: " + justificacion);
        }
        return resultado;
    }
    
    /**
     * Revisa contenido reportado y toma una decisión
     */
    public ResultadoModeracion revisarContenidoReportado(String contenido, UsuarioComunidad autor, String motivoReporte) {
        System.out.println("🔍 REVISIÓN MANUAL DE CONTENIDO");
        System.out.println("   Moderador: " + this.nombre);
        System.out.println("   Autor: " + autor.getNombre());
        System.out.println("   Motivo del reporte: " + motivoReporte);
        System.out.println("   Contenido: " + contenido.substring(0, Math.min(50, contenido.length())) + "...");
        
        // En una implementación real, aquí habría una interfaz para que el moderador tome la decisión
        // Por ahora, simulamos que aprueba el contenido
        return new ResultadoModeracion(true, "Contenido revisado y aprobado manualmente", null);
    }
    
    /**
     * Aplica una advertencia sin sanción
     */
    public void aplicarAdvertencia(UsuarioComunidad usuario, String motivo) {
        System.out.println("⚠️ ADVERTENCIA APLICADA por " + this.nombre);
        System.out.println("   Usuario: " + usuario.getNombre());
        System.out.println("   Motivo: " + motivo);
        // En una implementación real, esto se registraría en un sistema de advertencias
    }
    
    /**
     * Escalada de problemas graves a administradores
     */
    public void escalarProblema(UsuarioComunidad usuario, String descripcionProblema) {
        System.out.println("🚨 PROBLEMA ESCALADO por " + this.nombre);
        System.out.println("   Usuario involucrado: " + usuario.getNombre());
        System.out.println("   Descripción: " + descripcionProblema);
        // En una implementación real, esto notificaría a los administradores
    }
    
    @Override
    public String toString() {
        return String.format("ModeradorManual: %s (@%s) - %d comunidades", 
                           nombre, username, comunidadesGestionadas.size());
    }
}
