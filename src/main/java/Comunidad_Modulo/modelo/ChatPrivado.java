package Comunidad_Modulo.modelo;

import Modulo_Usuario.Clases.UsuarioComunidad;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatPrivado {
    private String idChat;
    private List<UsuarioComunidad> participantes;
    private List<Mensaje> mensajes;
    private LocalDateTime fechaCreacion;
    
    public ChatPrivado(List<UsuarioComunidad> participantes) {
        this.idChat = UUID.randomUUID().toString();
        this.participantes = new ArrayList<>(participantes);
        this.mensajes = new ArrayList<>();
        this.fechaCreacion = LocalDateTime.now();
    }
    
    // Getters
    public String getIdChat() {
        return idChat;
    }
    
    public List<UsuarioComunidad> getParticipantes() {
        return new ArrayList<>(participantes);
    }
    
    public List<Mensaje> getMensajes() {
        return new ArrayList<>(mensajes);
    }
    
    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
    
    // Métodos de negocio
    public boolean enviarMensaje(String contenido, UsuarioComunidad emisor, Moderador moderador) {
        if (!participantes.contains(emisor)) {
            throw new IllegalArgumentException("El usuario no es participante del chat");
        }
        
        // Moderar el contenido del mensaje usando el método abstracto
        boolean contenidoAprobado = moderador.moderarContenido(contenido, emisor);
        
        if (!contenidoAprobado) {
            System.out.println("🚫 MENSAJE BLOQUEADO EN CHAT PRIVADO");
            System.out.println("   Usuario: " + emisor.getNombre());
            System.out.println("   Contenido: \"" + contenido + "\"");
            System.out.println("   Razón: Contenido no aprobado por moderación");
            return false;
        }
        
        // Si el mensaje es aprobado, agregarlo al chat
        Mensaje mensaje = new Mensaje(contenido, emisor);
        mensajes.add(mensaje);
        
        System.out.println("✅ Mensaje enviado en chat privado por " + emisor.getNombre());
        return true;
    }
    
    // Método legacy para compatibilidad (sin moderación)
    @Deprecated
    public void enviarMensaje(String contenido, UsuarioComunidad emisor) {
        if (participantes.contains(emisor)) {
            Mensaje mensaje = new Mensaje(contenido, emisor);
            mensajes.add(mensaje);
        } else {
            throw new IllegalArgumentException("El usuario no es participante del chat");
        }
    }
    
    public List<Mensaje> obtenerHistorial() {
        // Marcar todos los mensajes como leídos
        mensajes.forEach(Mensaje::marcarComoLeido);
        return new ArrayList<>(mensajes);
    }
    
    public void agregarParticipante(UsuarioComunidad usuario) {
        if (!participantes.contains(usuario)) {
            participantes.add(usuario);
        }
    }
    
    public void eliminarParticipante(UsuarioComunidad usuario) {
        participantes.remove(usuario);
    }
    
    public int getMensajesNoLeidos() {
        return (int) mensajes.stream()
                           .filter(m -> !m.isLeido())
                           .count();
    }
    
    @Override
    public String toString() {
        return String.format("Chat: %s participantes, %d mensajes", 
                           participantes.size(), mensajes.size());
    }
}
