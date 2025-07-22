package utilidades;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import enums.TipoInfraccion;

/**
 * Filtro de contenido para detectar palabras inapropiadas y contenido ofensivo.
 */
public class FiltroContenido {
    
    private static final Set<String> PALABRAS_PROHIBIDAS = new HashSet<>(Arrays.asList(
        // Palabras ofensivas comunes (pendiente de agregar más)
        "idiota", "estupido", "tonto", "imbecil", "maldito", "basura", "porqueria",
        "cretino", "baboso", "payaso", "fracasado", "perdedor", "inutil", "patético",
        "odio", "mierda", "jodido", "maldita", "puto", "puta", "cabrón", "cabrona",
        "pendejo", "pendeja", "marica", "maricon", "gay", "homosexual", "lesbiana",
        "negro", "negra", "gordo", "gorda", "feo", "fea", "horrible", "asqueroso",
        "kill", "die", "muerte", "matar", "suicidio", "violencia", "golpear"
    ));
    
    private static final Set<String> PALABRAS_SPAM = new HashSet<>(Arrays.asList(
        "spam", "publicidad", "compra", "vende", "dinero", "gratis", "oferta",
        "promocion", "descuento", "regalo", "sorteo", "ganador", "premio"
    ));
    
    /**
     * Analiza el contenido y determina si contiene palabras prohibidas
     */
    public static ResultadoModeración analizarContenido(String contenido) {
        if (contenido == null || contenido.trim().isEmpty()) {
            return new ResultadoModeración(false, "", TipoInfraccion.NINGUNA);
        }
        
        String contenidoLimpio = limpiarTexto(contenido);
        String[] palabras = contenidoLimpio.split("\\s+");
        
        // Verificar palabras prohibidas
        for (String palabra : palabras) {
            if (PALABRAS_PROHIBIDAS.contains(palabra.toLowerCase())) {
                return new ResultadoModeración(true, palabra, TipoInfraccion.LENGUAJE_OFENSIVO);
            }
        }
        
        // Verificar spam
        for (String palabra : palabras) {
            if (PALABRAS_SPAM.contains(palabra.toLowerCase())) {
                return new ResultadoModeración(true, palabra, TipoInfraccion.SPAM);
            }
        }
        
        // Verificar CAPS LOCK excesivo (más del 70% en mayúsculas)
        if (esCapsLockExcesivo(contenido)) {
            return new ResultadoModeración(true, "MAYÚSCULAS EXCESIVAS", TipoInfraccion.CAPS_LOCK);
        }
        
        // Verificar repetición excesiva de caracteres
        if (tieneRepeticionExcesiva(contenido)) {
            return new ResultadoModeración(true, "REPETICIÓN EXCESIVA", TipoInfraccion.REPETICION);
        }
        
        return new ResultadoModeración(false, "", TipoInfraccion.NINGUNA);
    }
    
    /**
     * Limpia el texto de caracteres especiales para el análisis
     */
    private static String limpiarTexto(String texto) {
        return texto.replaceAll("[^a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]", " ");
    }
    
    /**
     * Verifica si el texto tiene un uso excesivo de mayúsculas
     */
    private static boolean esCapsLockExcesivo(String texto) {
        if (texto.length() < 10) return false; // Textos cortos no cuentan
        
        long mayusculas = texto.chars().filter(Character::isUpperCase).count();
        long letras = texto.chars().filter(Character::isLetter).count();
        
        return letras > 0 && (mayusculas * 100 / letras) > 70;
    }
    
    /**
     * Verifica si hay repetición excesiva de caracteres
     */
    private static boolean tieneRepeticionExcesiva(String texto) {
        // Buscar patrones como "aaaaaaa" o "!!!!!!"
        return texto.matches(".*(..)\\1{4,}.*");
    }
    
    /**
     * Clase para encapsular el resultado del análisis de moderación
     */
    public static class ResultadoModeración {
        private final boolean esInapropiado;
        private final String palabraProblematica;
        private final TipoInfraccion tipoInfraccion;
        
        public ResultadoModeración(boolean esInapropiado, String palabraProblematica, TipoInfraccion tipoInfraccion) {
            this.esInapropiado = esInapropiado;
            this.palabraProblematica = palabraProblematica;
            this.tipoInfraccion = tipoInfraccion;
        }
        
        public boolean esInapropiado() { return esInapropiado; }
        public String getPalabraProblematica() { return palabraProblematica; }
        public TipoInfraccion getTipoInfraccion() { return tipoInfraccion; }
        
        public int getDuracionSancionMinutos() {
            switch (tipoInfraccion) {
                case LENGUAJE_OFENSIVO: return 5;
                case SPAM: return 3;
                case CAPS_LOCK: return 2;
                case REPETICION: return 1;
                default: return 0;
            }
        }
        
        public String getRazonSancion() {
            switch (tipoInfraccion) {
                case LENGUAJE_OFENSIVO: return "Uso de lenguaje ofensivo: " + palabraProblematica;
                case SPAM: return "Contenido considerado como spam: " + palabraProblematica;
                case CAPS_LOCK: return "Uso excesivo de mayúsculas";
                case REPETICION: return "Repetición excesiva de caracteres";
                default: return "Contenido inapropiado";
            }
        }
    }
}
