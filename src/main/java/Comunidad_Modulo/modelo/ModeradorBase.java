package Comunidad_Modulo.modelo;

import Modulo_Usuario.Clases.UsuarioComunidad;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Clase base abstracta que implementa la funcionalidad común de todos los moderadores.
 * Aplica el principio DRY (Don't Repeat Yourself) y Template Method pattern.
 */
public abstract class ModeradorBase implements IModerador {
    
    // === ATRIBUTOS COMUNES ===
    protected String idModerador;
    protected String nombre;
    protected String username;
    protected List<Comunidad> comunidadesGestionadas;
    protected GestorSanciones gestorSanciones;
    
    // === CONSTRUCTORES ===
    public ModeradorBase(String nombre, String username) {
        this.idModerador = UUID.randomUUID().toString();
        this.nombre = nombre;
        this.username = username;
        this.comunidadesGestionadas = new ArrayList<>();
        this.gestorSanciones = new GestorSanciones();
    }

    public ModeradorBase(String nombre) {
        this(nombre, "mod");
    }
    
    // === IMPLEMENTACIÓN DE MÉTODOS COMUNES ===
    
    @Override
    public String getIdModerador() {
        return idModerador;
    }

    @Override
    public String getNombre() {
        return nombre;
    }

    @Override
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public List<Comunidad> getComunidadesGestionadas() {
        return new ArrayList<>(comunidadesGestionadas);
    }

    @Override
    public void asignarComunidad(Comunidad comunidad) {
        if (!comunidadesGestionadas.contains(comunidad)) {
            comunidadesGestionadas.add(comunidad);
        }
    }

    @Override
    public void removerComunidad(Comunidad comunidad) {
        comunidadesGestionadas.remove(comunidad);
    }

    // === DELEGACIÓN A GESTOR DE SANCIONES ===
    
    @Override
    public boolean usuarioEstaSancionado(UsuarioComunidad usuario) {
        return gestorSanciones.usuarioEstaSancionado(usuario);
    }

    @Override
    public SancionUsuario getSancionActiva(UsuarioComunidad usuario) {
        return gestorSanciones.obtenerSancionActiva(usuario);
    }

    @Override
    public List<SancionUsuario> getSancionesActivas() {
        return gestorSanciones.getSancionesActivas();
    }

    @Override
    public List<SancionUsuario> getHistorialSanciones(UsuarioComunidad usuario) {
        return gestorSanciones.getHistorialSanciones(usuario);
    }

    @Override
    public EstadisticasModeración getEstadisticasModeración() {
        return gestorSanciones.getEstadisticas();
    }

    @Override
    public void mostrarEstadoModeración() {
        System.out.println("\n🛡️ === ESTADO DE MODERACIÓN ===");
        System.out.println("Moderador: " + this.nombre + " (Tipo: " + this.getClass().getSimpleName() + ")");

        EstadisticasModeración stats = getEstadisticasModeración();
        System.out.println(stats.toString());

        List<SancionUsuario> sancionesActivas = getSancionesActivas();
        if (!sancionesActivas.isEmpty()) {
            System.out.println("\n🚫 Sanciones activas:");
            for (SancionUsuario sancion : sancionesActivas) {
                System.out.println("  - " + sancion.toString());
            }
        } else {
            System.out.println("\n✅ No hay sanciones activas");
        }
        System.out.println("=".repeat(35));
    }
    
    // === MÉTODOS AUXILIARES PROTEGIDOS ===
    
    /**
     * Aplica una sanción usando el gestor de sanciones
     */
    protected SancionUsuario aplicarSancion(UsuarioComunidad usuario, String razon, int duracionMinutos) {
        return gestorSanciones.aplicarSancion(usuario, razon, duracionMinutos, this.nombre);
    }
    
    /**
     * Levanta una sanción usando el gestor de sanciones
     */
    protected boolean levantarSancion(UsuarioComunidad usuario) {
        return gestorSanciones.levantarSancion(usuario, this.nombre);
    }
    
    // === MÉTODO ABSTRACTO - CADA TIPO IMPLEMENTA SU LÓGICA ===
    
    /**
     * Método abstracto que cada tipo de moderador debe implementar
     * con su lógica específica de moderación.
     */
    @Override
    public abstract ResultadoModeracion procesarContenido(String contenido, UsuarioComunidad autor);
    
    // === MÉTODOS ADICIONALES COMUNES (legacy) ===
    
    public void moderarForo(ForoGeneral foro) {
        System.out.println("Moderando foro: " + foro.toString());
    }

    public void supervisarChats(List<ChatPrivado> chats) {
        System.out.println("Supervisando " + chats.size() + " chats privados");
    }

    public void cerrarHilo(HiloDiscusion hilo) {
        hilo.cerrar();
        System.out.println("Hilo cerrado por moderador: " + hilo.getTitulo());
    }

    public void eliminarMensaje(ChatPrivado chat, String idMensaje) {
        System.out.println("Mensaje eliminado por moderador en chat: " + chat.getIdChat());
    }

    public void suspenderUsuario(UsuarioComunidad usuario) {
        System.out.println("Usuario suspendido: " + usuario.getNombre());
    }
    
    @Override
    public String toString() {
        return String.format("%s: %s (gestiona %d comunidades)",
                           this.getClass().getSimpleName(), nombre, comunidadesGestionadas.size());
    }
}
