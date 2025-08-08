package Comunidad_Modulo.modelo;

import Modulo_Usuario.Clases.UsuarioComunidad;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Comunidad {
    private String idComunidad;
    private String nombre;
    private String descripcion;
    private ForoGeneral foroGeneral;
    private List<ChatPrivado> chatsPrivados;
    private List<UsuarioComunidad> usuariosMiembros;     // Usuarios que pertenecen permanentemente
    private List<UsuarioComunidad> usuariosConectados;  // Usuarios activos en esta sesión
    private LocalDateTime fechaCreacion;
    private IModerador moderador; // Cambiado a interfaz
    
    public Comunidad(String nombre, String descripcion) {
        this.idComunidad = UUID.randomUUID().toString();
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.foroGeneral = new ForoGeneral();
        this.chatsPrivados = new ArrayList<>();
        this.usuariosMiembros = new ArrayList<>();
        this.usuariosConectados = new ArrayList<>();
        this.fechaCreacion = LocalDateTime.now();
        
        // Inicializar moderador automático para la comunidad
        this.moderador = new ModeradorAutomatico("ModeradorBot_" + nombre);
        this.moderador.asignarComunidad(this);
    }
    
    // Getters y setters
    public String getIdComunidad() {
        return idComunidad;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public String getDescripcion() {
        return descripcion;
    }
    
    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
    
    public ForoGeneral getForoGeneral() {
        return foroGeneral;
    }
    
    public List<ChatPrivado> getChatsPrivados() {
        return new ArrayList<>(chatsPrivados);
    }
    
    public List<UsuarioComunidad> getUsuariosConectados() {
        return new ArrayList<>(usuariosConectados);
    }
    
    public List<UsuarioComunidad> getUsuariosMiembros() {
        return new ArrayList<>(usuariosMiembros);
    }
    
    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
    
    public IModerador getModerador() {
        return moderador;
    }
    
    public void setModerador(IModerador moderador) {
        this.moderador = moderador;
        moderador.asignarComunidad(this);
    }
    
    // Métodos de negocio
    public ForoGeneral accederForo() {
        return foroGeneral;
    }
    
    public ChatPrivado iniciarChatPrivado(List<UsuarioComunidad> participantes) {
        // Verificar que todos los participantes sean miembros de la comunidad
        for (UsuarioComunidad participante : participantes) {
            if (!usuariosMiembros.contains(participante)) {
                throw new IllegalArgumentException("Todos los participantes deben ser miembros de la comunidad");
            }
        }
        
        ChatPrivado chat = new ChatPrivado(participantes);
        chatsPrivados.add(chat);
        return chat;
    }
    
    /**
     * Agrega un chat privado ya existente (para cargar desde persistencia)
     */
    public void agregarChatPrivado(ChatPrivado chat) {
        if (!chatsPrivados.contains(chat)) {
            chatsPrivados.add(chat);
        }
    }
    
    /**
     * Agrega un usuario miembro (para cargar desde persistencia)
     */
    public void agregarUsuarioMiembro(UsuarioComunidad usuario) {
        if (!usuariosMiembros.contains(usuario)) {
            usuariosMiembros.add(usuario);
        }
    }
    
    public List<UsuarioComunidad> buscarUsuarios(String nombre) {
        return usuariosMiembros.stream()
                               .filter(u -> u.getNombre().toLowerCase().contains(nombre.toLowerCase()))
                               .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Une un usuario a la comunidad permanentemente (se guarda en persistencia)
     */
    public void unirUsuario(UsuarioComunidad usuario) {
        if (!usuariosMiembros.contains(usuario)) {
            usuariosMiembros.add(usuario);
        }
    }
    
    /**
     * Remueve un usuario de la comunidad permanentemente
     */
    public void removerUsuario(UsuarioComunidad usuario) {
        usuariosMiembros.remove(usuario);
        usuariosConectados.remove(usuario); // También desconectar si está conectado
    }
    
    public void conectarUsuario(UsuarioComunidad usuario) {
        // Para conectarse, debe ser miembro primero
        if (!usuariosMiembros.contains(usuario)) {
            unirUsuario(usuario); // Automáticamente se une a la comunidad
        }
        
        if (!usuariosConectados.contains(usuario)) {
            usuariosConectados.add(usuario);
        }
    }
    
    public void desconectarUsuario(UsuarioComunidad usuario) {
        usuariosConectados.remove(usuario);
    }
    
    public String obtenerEstadisticas() {
        int totalMiembros = usuariosMiembros.size();
        int usuariosActivos = usuariosConectados.size();
        int totalChats = chatsPrivados.size();
        int totalGruposDiscusion = foroGeneral.getGruposDiscusion().size();
        int totalGruposCompartir = foroGeneral.getGruposCompartir().size();
        
        return String.format(
            "Estadísticas de la Comunidad '%s':\n" +
            "- Miembros totales: %d\n" +
            "- Usuarios activos: %d\n" +
            "- Chats privados: %d\n" +
            "- Grupos de discusión: %d\n" +
            "- Grupos de compartir: %d\n" +
            "- Fecha de creación: %s",
            nombre, totalMiembros, usuariosActivos, totalChats, totalGruposDiscusion, 
            totalGruposCompartir, fechaCreacion.toString()
        );
    }
    
    @Override
    public String toString() {
        return String.format("Comunidad: %s (%d miembros, %d activos)", 
                           nombre, usuariosMiembros.size(), usuariosConectados.size());
    }
}
