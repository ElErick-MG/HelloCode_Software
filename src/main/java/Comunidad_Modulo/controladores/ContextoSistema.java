package Comunidad_Modulo.controladores;

import Modulo_Usuario.Clases.UsuarioComunidad;
import Comunidad_Modulo.modelo.*;
import Comunidad_Modulo.integracion.IModuloUsuarios;
import Comunidad_Modulo.servicios.PersistenciaService;
import Comunidad_Modulo.enums.TipoTema;
import Modulo_Usuario.Clases.NivelJava;

import java.util.*;

/**
 * Contexto central del sistema de comunidad
 * Implementa patrón Singleton para mantener un estado global
 * Gestiona usuarios, comunidades, moderadores y la persistencia
 */
public class ContextoSistema {
    private static ContextoSistema instancia;
    
    // Colecciones principales
    private List<UsuarioComunidad> usuarios;
    private List<Comunidad> comunidades;
    private List<IModerador> moderadores; // Cambiado a interfaz
    private Comunidad comunidadActual;
    
    // Módulo de integración con otros sistemas
    private IModuloUsuarios moduloUsuarios;
    
    // Sistema de métricas y estadísticas
    private Map<String, Integer> participacionesUsuario;
    private Map<String, Integer> puntosUsuario;
    
    // Servicio de persistencia
    private PersistenciaService persistenciaService;
    
    // Constructor privado (Singleton)
    private ContextoSistema() {
        this.usuarios = new ArrayList<>();
        this.comunidades = new ArrayList<>();
        this.moderadores = new ArrayList<>();
        this.participacionesUsuario = new HashMap<>();
        this.puntosUsuario = new HashMap<>();
        
        // Inicializar servicio de persistencia
        this.persistenciaService = new PersistenciaService();
        
        // Cargar datos existentes desde persistencia
        cargarDatosDesdePersistencia();
    }
    
    public static ContextoSistema getInstance() {
        if (instancia == null) {
            instancia = new ContextoSistema();
        }
        return instancia;
    }
    
    // Método existente para compatibilidad
    public static ContextoSistema getInstancia() {
        return getInstance();
    }
    
    // Getters
    public List<UsuarioComunidad> getUsuarios() {
        return new ArrayList<>(usuarios);
    }
    
    public List<Comunidad> getComunidades() {
        return new ArrayList<>(comunidades);
    }
    
    public List<IModerador> getModeradores() {
        return new ArrayList<>(moderadores);
    }
    
    public Comunidad getComunidadActual() {
        return comunidadActual;
    }
    
    // Métodos para gestionar usuarios
    public void agregarUsuario(UsuarioComunidad usuario) {
        if (!usuarios.contains(usuario)) {
            usuarios.add(usuario);
        }
    }
    
    public void eliminarUsuario(UsuarioComunidad usuario) {
        usuarios.remove(usuario);
    }
    
    // Métodos para gestionar comunidades
    public void agregarComunidad(Comunidad comunidad) {
        if (!comunidades.contains(comunidad)) {
            comunidades.add(comunidad);
            
            // Guardar en persistencia
            if (persistenciaService != null) {
                persistenciaService.guardarComunidad(comunidad);
            }
        }
    }
    
    public void eliminarComunidad(Comunidad comunidad) {
        comunidades.remove(comunidad);
        if (comunidadActual == comunidad) {
            comunidadActual = null;
        }
    }
    
    public void setComunidadActual(Comunidad comunidad) {
        this.comunidadActual = comunidad;
    }
    
    // Métodos para gestionar moderadores
    public void agregarModerador(IModerador moderador) {
        if (!moderadores.contains(moderador)) {
            moderadores.add(moderador);
        }
    }

    // En la clase ContextoSistema
    public void actualizarComunidad(Comunidad comunidad) {
        int index = comunidades.indexOf(comunidad);
        if (index != -1) {
            comunidades.set(index, comunidad);
        }
    }
    
    public void eliminarModerador(IModerador moderador) {
        moderadores.remove(moderador);
    }
    
    // Método para limpiar el contexto (útil para testing)
    public void limpiar() {
        usuarios.clear();
        comunidades.clear();
        moderadores.clear();
        comunidadActual = null;
    }
    
    // Métodos de utilidad
    public boolean tieneComunidadActiva() {
        return comunidadActual != null;
    }
    
    public int getTotalUsuarios() {
        return usuarios.size();
    }
    
    public int getTotalComunidades() {
        return comunidades.size();
    }
    
    // Métodos para métricas
    public Map<String, Integer> getParticipacionesUsuarios() {
        return new HashMap<>(participacionesUsuario);
    }
    
    public int getParticipacionesUsuario(String nombreUsuario) {
        return participacionesUsuario.getOrDefault(nombreUsuario, 0);
    }
    
    public void incrementarParticipacion(String nombreUsuario) {
        participacionesUsuario.put(nombreUsuario, getParticipacionesUsuario(nombreUsuario) + 1);
    }
    
    public int getPuntosUsuario(String nombreUsuario) {
        return puntosUsuario.getOrDefault(nombreUsuario, 0);
    }
    
    public void agregarPuntos(String nombreUsuario, int puntos) {
        puntosUsuario.put(nombreUsuario, getPuntosUsuario(nombreUsuario) + puntos);
    }
    
    /**
     * Establece automáticamente una comunidad activa para el usuario actual
     */
    public void establecerComunidadActivaParaUsuario() {
        try {
            MetodosGlobales.SesionManager sesion = MetodosGlobales.SesionManager.getInstancia();

            // ✅ NO reemplazar si ya hay comunidad activa
            if (this.comunidadActual != null) {
                System.out.println("ℹ️ Comunidad activa ya está definida: " + comunidadActual.getNombre());
                return;
            }

            if (sesion.hayUsuarioAutenticado()) {
                UsuarioComunidad usuarioActual = sesion.getUsuarioComunidad();

                // Buscar la primera comunidad donde el usuario es miembro
                for (Comunidad comunidad : comunidades) {
                    if (comunidad.getUsuariosMiembros().stream()
                            .anyMatch(u -> u.getUsername().equals(usuarioActual.getUsername()))) {

                        setComunidadActual(comunidad);
                        
                        // Asegurar que el usuario esté conectado en esta sesión
                        if (!comunidad.getUsuariosConectados().stream()
                                .anyMatch(u -> u.getUsername().equals(usuarioActual.getUsername()))) {
                            comunidad.conectarUsuario(usuarioActual);
                        }
                        
                        System.out.println("✅ Comunidad activa establecida: " + comunidad.getNombre());
                        System.out.println("📊 Usuario conectado - Miembros: " + comunidad.getUsuariosMiembros().size() + 
                                         ", Activos: " + comunidad.getUsuariosConectados().size());
                        return;
                    }
                }
                
                System.out.println("ℹ️ Usuario no pertenece a ninguna comunidad");
            }
        } catch (Exception e) {
            System.err.println("Error al establecer comunidad activa: " + e.getMessage());
        }
    }
    
    // ========== MÉTODOS DE PERSISTENCIA ==========
    
    /**
     * Carga todos los datos desde persistencia al inicializar el sistema
     */
    public void cargarDatosDesdePersistencia() {
        try {
            System.out.println("🔄 Cargando datos desde persistencia...");

            // Limpiar datos antes de recargar para evitar duplicados
            comunidades.clear();
            usuarios.clear();
            moderadores.clear();
            
            // Cargar comunidades
            List<Comunidad> comunidadesCargadas = persistenciaService.cargarComunidades();
            System.out.println("📋 Comunidades encontradas en persistencia: " + comunidadesCargadas.size());
            
            for (Comunidad comunidad : comunidadesCargadas) {
                comunidades.add(comunidad);
                System.out.println("   ✅ " + comunidad.getNombre() + " → " + comunidad.getUsuariosMiembros().size() + " miembros");
                
                // Los usuarios ya fueron cargados como miembros en PersistenciaService.cargarComunidades()
                // Agregar usuarios al contexto global si no existen
                for (UsuarioComunidad miembro : comunidad.getUsuariosMiembros()) {
                    if (!usuarios.contains(miembro)) {
                        usuarios.add(miembro);
                    }
                }
                
                // Cargar foros y chats
                persistenciaService.cargarGruposDeForo(comunidad.getNombre(), comunidad.getForoGeneral());
                
                // Cargar chats privados
                persistenciaService.cargarChatsDeComunidad(comunidad.getNombre(), comunidad);
            }
            
            System.out.println("✅ Datos cargados correctamente desde persistencia");
            
        } catch (Exception e) {
            System.err.println("Error al cargar datos desde persistencia: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Conecta un usuario a una comunidad y guarda en persistencia
     */
    public void conectarUsuarioAComunidad(UsuarioComunidad usuario, Comunidad comunidad) {
        System.out.println("🔗 Conectando usuario: " + usuario.getUsername() + " a comunidad: " + comunidad.getNombre());
        
        // Conectar en memoria
        comunidad.conectarUsuario(usuario);
        
        if (!usuarios.contains(usuario)) {
            usuarios.add(usuario);
        }
        
        System.out.println("📊 Comunidad ahora tiene " + comunidad.getUsuariosMiembros().size() + " miembros");
        
        // Guardar en persistencia
        if (persistenciaService != null) {
            persistenciaService.guardarUsuarioEnComunidad(comunidad.getNombre(), usuario);
        } else {
            System.err.println("❌ PersistenciaService es null!");
        }
    }
    
    /**
     * Desconecta un usuario de una comunidad y actualiza persistencia
     */
    public void desconectarUsuarioDeComunidad(UsuarioComunidad usuario, Comunidad comunidad) {
        // Remover como miembro permanentemente
        comunidad.removerUsuario(usuario);
        
        // Actualizar persistencia
        if (persistenciaService != null) {
            persistenciaService.eliminarUsuarioDeComunidad(comunidad.getNombre(), usuario.getUsername());
        }
    }
    
    /**
     * Guarda un grupo de foro en persistencia
     */
    public void guardarGrupoForo(Comunidad comunidad, String tipoGrupo, String titulo, NivelJava nivel, TipoTema tema, String creador) {
        if (persistenciaService != null) {
            persistenciaService.guardarGrupoForo(comunidad.getNombre(), tipoGrupo, titulo, nivel, tema, creador);
        }
    }
    
    /**
     * Guarda un chat privado en persistencia
     */
    public void guardarChatPrivado(Comunidad comunidad, ChatPrivado chat) {
        if (persistenciaService != null) {
            persistenciaService.guardarChatPrivado(comunidad.getNombre(), chat);
        }
    }
    
    // Getters y setters para módulo de usuarios
    public IModuloUsuarios getModuloUsuarios() {
        return moduloUsuarios;
    }
    
    public void setModuloUsuarios(IModuloUsuarios moduloUsuarios) {
        this.moduloUsuarios = moduloUsuarios;
    }
    
    // Método para establecer usuario activo (compatibilidad)
    public void setUsuarioActivo(UsuarioComunidad usuario) {
        // Este método se mantiene para compatibilidad con código existente
        // El usuario activo se gestiona ahora a través del SesionManager
        System.out.println("⚠️ setUsuarioActivo llamado - usar SesionManager en su lugar");
    }
}
