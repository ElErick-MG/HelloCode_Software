package Comunidad_Modulo.utilidades;

import Modulo_Usuario.Clases.Usuario;
import Modulo_Usuario.Clases.UsuarioComunidad;
import Modulo_Usuario.Clases.Roles;
import Modulo_Usuario.Clases.NivelJava;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Clase utilitaria para gestionar usuarios del sistema
 * Maneja la carga, búsqueda y verificación de roles desde el archivo usuarios.txt
 */
public class GestorUsuarios {
    
    private static final String ARCHIVO_USUARIOS = "src/main/java/Modulo_Usuario/Usuarios/usuarios.txt";
    private static List<Usuario> usuariosSistema = new ArrayList<>();
    private static GestorUsuarios instancia;
    
    private GestorUsuarios() {
        cargarUsuarios();
    }
    
    public static GestorUsuarios getInstance() {
        if (instancia == null) {
            instancia = new GestorUsuarios();
        }
        return instancia;
    }
    
    /**
     * Carga todos los usuarios desde el archivo usuarios.txt
     */
    private void cargarUsuarios() {
        usuariosSistema.clear();
        
        try {
            File file = new File(ARCHIVO_USUARIOS);
            if (!file.exists()) {
                System.err.println("Archivo usuarios.txt no encontrado: " + ARCHIVO_USUARIOS);
                return;
            }
            
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                
                String linea;
                int lineaNumero = 0;
                
                while ((linea = br.readLine()) != null) {
                    lineaNumero++;
                    linea = linea.trim();
                    
                    if (!linea.isEmpty()) {
                        try {
                            Usuario usuario = Usuario.fromString(linea);
                            if (usuario != null) {
                                usuariosSistema.add(usuario);
                            }
                        } catch (Exception e) {
                            System.err.println("Error al procesar línea " + lineaNumero + ": " + linea);
                        }
                    }
                }
                
                System.out.println("Usuarios cargados: " + usuariosSistema.size());
                
            }
            
        } catch (IOException e) {
            System.err.println("❌ Error al cargar archivo de usuarios: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Refresca la lista de usuarios desde el archivo
     */
    public void refrescarUsuarios() {
        cargarUsuarios();
    }
    
    /**
     * Busca un usuario por su username
     */
    public Optional<Usuario> buscarUsuario(String username) {
        return usuariosSistema.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username.trim()))
                .findFirst();
    }
    
    /**
     * Convierte un Usuario a UsuarioComunidad para compatibilidad
     */
    public UsuarioComunidad convertirAUsuarioComunidad(Usuario usuario) {
        if (usuario == null) return null;
        
        UsuarioComunidad usuarioComunidad = new UsuarioComunidad(
            usuario.getUsername(),
            usuario.getPassword(),
            usuario.getNombre(),
            usuario.getEmail()
        );
        
        usuarioComunidad.setIdUsuario(usuario.getUsername());
        usuarioComunidad.setRol(usuario.getRol());
        usuarioComunidad.setXp(usuario.getXp());
        usuarioComunidad.setNivelJava(NivelJava.PRINCIPIANTE); // Por defecto
        usuarioComunidad.setReputacion(0); // Por defecto
        
        return usuarioComunidad;
    }
    
    /**
     * Busca un UsuarioComunidad por username
     */
    public Optional<UsuarioComunidad> buscarUsuarioComunidad(String username) {
        return buscarUsuario(username)
                .map(this::convertirAUsuarioComunidad);
    }
    
    /**
     * Verifica si un usuario tiene un rol específico
     */
    public boolean usuarioTieneRol(String username, Roles rol) {
        return buscarUsuario(username)
                .map(u -> u.getRol() == rol)
                .orElse(false);
    }
    
    /**
     * Obtiene el rol de un usuario
     */
    public Roles obtenerRolUsuario(String username) {
        return buscarUsuario(username)
                .map(Usuario::getRol)
                .orElse(Roles.USUARIO);
    }
    
    /**
     * Lista todos los usuarios con un rol específico
     */
    public List<Usuario> obtenerUsuariosPorRol(Roles rol) {
        return usuariosSistema.stream()
                .filter(u -> u.getRol() == rol)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Obtiene todos los usuarios del sistema
     */
    public List<Usuario> obtenerTodosLosUsuarios() {
        return new ArrayList<>(usuariosSistema);
    }
    
    /**
     * Genera un reporte de usuarios por roles
     */
    public String generarReporteRoles() {
        StringBuilder reporte = new StringBuilder();
        reporte.append("=== REPORTE DE ROLES DEL SISTEMA ===\n\n");
        
        int usuarios = (int) usuariosSistema.stream().filter(u -> u.getRol() == Roles.USUARIO).count();
        int moderadores = (int) usuariosSistema.stream().filter(u -> u.getRol() == Roles.MODERADOR).count();
        int administradores = (int) usuariosSistema.stream().filter(u -> u.getRol() == Roles.ADMINISTRADOR).count();
        int superAdmins = (int) usuariosSistema.stream().filter(u -> u.getRol() == Roles.SUPER_ADMINISTRADOR).count();
        
        reporte.append("Usuarios normales: ").append(usuarios).append("\n");
        reporte.append("Moderadores: ").append(moderadores).append("\n");
        reporte.append("Administradores: ").append(administradores).append("\n");
        reporte.append("Super Administradores: ").append(superAdmins).append("\n");
        reporte.append("Total usuarios: ").append(usuariosSistema.size()).append("\n\n");
        
        // Detalles por rol
        if (moderadores > 0) {
            reporte.append("MODERADORES:\n");
            obtenerUsuariosPorRol(Roles.MODERADOR).forEach(u -> 
                reporte.append("  • ").append(u.getUsername()).append(" (").append(u.getNombre()).append(")\n"));
            reporte.append("\n");
        }
        
        if (administradores > 0) {
            reporte.append("🔑 ADMINISTRADORES:\n");
            obtenerUsuariosPorRol(Roles.ADMINISTRADOR).forEach(u -> 
                reporte.append("  • ").append(u.getUsername()).append(" (").append(u.getNombre()).append(")\n"));
            reporte.append("\n");
        }
        
        if (superAdmins > 0) {
            reporte.append("⭐ SUPER ADMINISTRADORES:\n");
            obtenerUsuariosPorRol(Roles.SUPER_ADMINISTRADOR).forEach(u -> 
                reporte.append("  • ").append(u.getUsername()).append(" (").append(u.getNombre()).append(")\n"));
            reporte.append("\n");
        }
        
        return reporte.toString();
    }
}
