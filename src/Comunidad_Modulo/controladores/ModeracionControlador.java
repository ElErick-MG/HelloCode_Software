package controladores;

import modelo.*;
import servicios.ModeracionService.ResultadoModeracion;
import utilidades.InputHelper;
import utilidades.MenuHelper;

import java.util.List;

/**
 * Controlador para la gestión de moderación en el módulo de comunidad.
 */
public class ModeracionControlador implements IControlador {
    private final Moderador moderador;
    private final ContextoSistema contexto;
    
    public ModeracionControlador(Moderador moderador, ContextoSistema contexto) {
        this.moderador = moderador;
        this.contexto = contexto;
    }
    
    @Override
    public void ejecutar() {
        boolean volver = false;
        while (!volver) {
            mostrarMenuModeración();
            int opcion = InputHelper.leerEntero("Seleccione una opción");
            
            switch (opcion) {
                case 1:
                    moderarMensaje();
                    break;
                case 2:
                    aplicarSancionManual();
                    break;
                case 3:
                    levantarSancion();
                    break;
                case 4:
                    mostrarSancionesActivas();
                    break;
                case 5:
                    mostrarHistorialUsuario();
                    break;
                case 6:
                    mostrarEstadisticas();
                    break;
                case 7:
                    volver = true;
                    break;
                default:
                    System.out.println("Opción inválida.");
            }
        }
    }
    
    @Override
    public String getNombreModulo() {
        return "Sistema de Moderación";
    }
    
    private void mostrarMenuModeración() {
        System.out.println("\n🛡️ === SISTEMA DE MODERACIÓN ===");
        System.out.println("1. Moderar mensaje");
        System.out.println("2. Aplicar sanción manual");
        System.out.println("3. Levantar sanción");
        System.out.println("4. Ver sanciones activas");
        System.out.println("5. Ver historial de usuario");
        System.out.println("6. Ver estadísticas");
        System.out.println("7. Volver");
        System.out.println("=".repeat(35));
    }
    
    private void moderarMensaje() {
        System.out.println("\n=== MODERAR MENSAJE ===");
        
        if (contexto.getUsuarios().isEmpty()) {
            System.out.println("No hay usuarios registrados.");
            InputHelper.presionarEnterParaContinuar();
            return;
        }
        
        // Seleccionar usuario
        MenuHelper.mostrarUsuarios(contexto.getUsuarios());
        int indiceUsuario = InputHelper.leerEntero("Seleccione el usuario autor del mensaje") - 1;
        
        if (indiceUsuario < 0 || indiceUsuario >= contexto.getUsuarios().size()) {
            System.out.println("Selección inválida.");
            InputHelper.presionarEnterParaContinuar();
            return;
        }
        
        UsuarioTemp autor = contexto.getUsuarios().get(indiceUsuario);
        
        // Leer mensaje
        String mensaje = InputHelper.leerTexto("Ingrese el mensaje a moderar");
        
        // Moderar
        ResultadoModeracion resultado = moderador.moderarMensaje(mensaje, autor);
        
        System.out.println("\n📝 Mensaje: \"" + mensaje + "\"");
        System.out.println("👤 Autor: " + autor.getNombre());
        
        if (resultado.isAprobado()) {
            System.out.println("✅ RESULTADO: Mensaje APROBADO");
        } else {
            System.out.println("🚫 RESULTADO: Mensaje RECHAZADO");
            System.out.println("📄 Razón: " + resultado.getMensaje());
        }
        
        InputHelper.presionarEnterParaContinuar();
    }
    
    private void aplicarSancionManual() {
        System.out.println("\n=== APLICAR SANCIÓN MANUAL ===");
        
        if (contexto.getUsuarios().isEmpty()) {
            System.out.println("No hay usuarios registrados.");
            InputHelper.presionarEnterParaContinuar();
            return;
        }
        
        // Seleccionar usuario
        MenuHelper.mostrarUsuarios(contexto.getUsuarios());
        int indiceUsuario = InputHelper.leerEntero("Seleccione el usuario a sancionar") - 1;
        
        if (indiceUsuario < 0 || indiceUsuario >= contexto.getUsuarios().size()) {
            System.out.println("Selección inválida.");
            InputHelper.presionarEnterParaContinuar();
            return;
        }
        
        UsuarioTemp usuario = contexto.getUsuarios().get(indiceUsuario);
        
        // Verificar si ya está sancionado
        if (moderador.usuarioEstaSancionado(usuario)) {
            SancionUsuario sancionActiva = moderador.getSancionActiva(usuario);
            System.out.println("⚠️ El usuario ya está sancionado:");
            System.out.println("   " + sancionActiva.toString());
            InputHelper.presionarEnterParaContinuar();
            return;
        }
        
        // Leer datos de la sanción
        String razon = InputHelper.leerTexto("Razón de la sanción");
        int duracion = InputHelper.leerEntero("Duración en minutos");
        
        // Aplicar sanción
        SancionUsuario sancion = moderador.aplicarSancionManual(usuario, razon, duracion);
        
        System.out.println("\n✅ Sanción aplicada exitosamente:");
        System.out.println("   " + sancion.toString());
        
        InputHelper.presionarEnterParaContinuar();
    }
    
    private void levantarSancion() {
        System.out.println("\n=== LEVANTAR SANCIÓN ===");
        
        List<SancionUsuario> sancionesActivas = moderador.getSancionesActivas();
        
        if (sancionesActivas.isEmpty()) {
            System.out.println("No hay sanciones activas.");
            InputHelper.presionarEnterParaContinuar();
            return;
        }
        
        // Mostrar sanciones activas
        System.out.println("Sanciones activas:");
        for (int i = 0; i < sancionesActivas.size(); i++) {
            System.out.println((i + 1) + ". " + sancionesActivas.get(i).toString());
        }
        
        int indiceSancion = InputHelper.leerEntero("Seleccione la sanción a levantar") - 1;
        
        if (indiceSancion < 0 || indiceSancion >= sancionesActivas.size()) {
            System.out.println("Selección inválida.");
            InputHelper.presionarEnterParaContinuar();
            return;
        }
        
        SancionUsuario sancion = sancionesActivas.get(indiceSancion);
        boolean levantada = moderador.levantarSancion(sancion.getUsuario());
        
        if (levantada) {
            System.out.println("✅ Sanción levantada exitosamente.");
        } else {
            System.out.println("❌ Error al levantar la sanción.");
        }
        
        InputHelper.presionarEnterParaContinuar();
    }
    
    private void mostrarSancionesActivas() {
        System.out.println("\n=== SANCIONES ACTIVAS ===");
        
        List<SancionUsuario> sancionesActivas = moderador.getSancionesActivas();
        
        if (sancionesActivas.isEmpty()) {
            System.out.println("✅ No hay sanciones activas.");
        } else {
            for (SancionUsuario sancion : sancionesActivas) {
                System.out.println("🚫 " + sancion.toString());
            }
        }
        
        InputHelper.presionarEnterParaContinuar();
    }
    
    private void mostrarHistorialUsuario() {
        System.out.println("\n=== HISTORIAL DE USUARIO ===");
        
        if (contexto.getUsuarios().isEmpty()) {
            System.out.println("No hay usuarios registrados.");
            System.out.println("✅ Sin historial disponible.");
            //System.out.println("=".repeat(35));
            InputHelper.presionarEnterParaContinuar();
            return;
        }
        
        // Seleccionar usuario
        MenuHelper.mostrarUsuarios(contexto.getUsuarios());
        int indiceUsuario = InputHelper.leerEntero("Seleccione el usuario") - 1;
        
        if (indiceUsuario < 0 || indiceUsuario >= contexto.getUsuarios().size()) {
            System.out.println("Selección inválida.");
            System.out.println("✅ Sin historial disponible.");
            InputHelper.presionarEnterParaContinuar();
            return;
        }
        
        UsuarioTemp usuario = contexto.getUsuarios().get(indiceUsuario);
        List<SancionUsuario> historial = moderador.getHistorialSanciones(usuario);
        
        System.out.println("\n📋 Historial de " + usuario.getNombre() + ":");
        
        if (historial.isEmpty()) {
            System.out.println("✅ Sin sanciones registradas.");
        } else {
            for (SancionUsuario sancion : historial) {
                String estado = sancion.estaActiva() ? "🔴 ACTIVA" : "✅ FINALIZADA";
                System.out.println("   " + estado + " - " + sancion.toString());
            }
        }
        
        InputHelper.presionarEnterParaContinuar();
    }
    
    private void mostrarEstadisticas() {
        System.out.println("\n=== ESTADÍSTICAS DE MODERACIÓN ===");
        moderador.mostrarEstadoModeración();
        InputHelper.presionarEnterParaContinuar();
    }
}
