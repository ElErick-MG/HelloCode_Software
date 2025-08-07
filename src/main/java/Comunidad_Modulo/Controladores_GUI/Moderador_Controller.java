package Comunidad_Modulo.Controladores_GUI;

import Comunidad_Modulo.servicios.ServicioModeracion;
import Comunidad_Modulo.servicios.ServicioModeracion.ResultadoOperacion;
import Modulo_Usuario.Clases.UsuarioComunidad;
import MetodosGlobales.MetodosFrecuentes;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.collections.FXCollections;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.List;

public class Moderador_Controller implements Initializable {

    @FXML private Button btnEliminarUsuario;
    @FXML private Button btnListarUsuarios;
    @FXML private TextField txtNombreUsuarioEliminar;
    @FXML private TextField txtNombreComunidad;
    @FXML private TextArea txtAreaInformacion;
    @FXML private Button btnEliminarComunidad;
    @FXML private TextField txtComunidadEliminar;
    @FXML private Button btnVolver;
    @FXML private ComboBox<String> comboComunidades;
    @FXML private ComboBox<String> comboTipoGrupo;
    @FXML private ComboBox<String> comboGrupos;
    @FXML private Button btnVerHistorial;
    @FXML private TextArea txtAreaHistorial;
    
    // ==========================================
    // CONTROLES PARA GESTIÓN DE SANCIONES
    // ==========================================
    @FXML private TextField txtUsuarioSancion;
    @FXML private TextField txtComunidadSancion;
    @FXML private TextField txtRazonSancion;
    @FXML private TextField txtDuracionSancion;
    @FXML private Button btnAplicarSancion;
    @FXML private Button btnLevantarSancion;
    @FXML private Button btnVerSancionesActivas;
    @FXML private TextArea txtAreaSanciones;

    private ServicioModeracion servicioModeracion;
    private UsuarioComunidad usuarioAdmin; // Usuario administrador por defecto
    private int contadorClicsListar = 0; // Para mostrar menú de sanciones

    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("🔄 Inicializando controlador de Moderador...");
        try {
            inicializarSistema();
            System.out.println("✅ Sistema inicializado correctamente");
            
            configuracionInterfazModerador();
            System.out.println("✅ Interfaz configurada correctamente");
            
            actualizarInformacion();
            System.out.println("✅ Información actualizada correctamente");
            
            configurarControlesHistorial();
            System.out.println("✅ Controles de historial configurados");
            
            System.out.println("🎉 Controlador de Moderador inicializado completamente");
        } catch (Exception e) {
            System.err.println("❌ Error al inicializar controlador de Moderador: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void inicializarSistema() {
        try {
            System.out.println("🔧 Creando ServicioModeracion...");
            this.servicioModeracion = new ServicioModeracion();
            System.out.println("✅ ServicioModeracion creado");
            
            // Crear usuario administrador por defecto
            System.out.println("🔧 Creando usuario administrador...");
            this.usuarioAdmin = new UsuarioComunidad("admin", "admin", "Administrador Sistema", "admin@sistema.com");
            System.out.println("✅ Usuario administrador creado: " + usuarioAdmin.getUsername());
            
        } catch (Exception e) {
            System.err.println("❌ Error en inicializarSistema: " + e.getMessage());
            e.printStackTrace();
            
            // Valores por defecto en caso de error
            if (this.servicioModeracion == null) {
                System.out.println("🔄 Intentando crear ServicioModeracion simple...");
                this.servicioModeracion = new ServicioModeracion();
            }
            if (this.usuarioAdmin == null) {
                this.usuarioAdmin = new UsuarioComunidad("admin", "admin", "Admin", "admin@test.com");
            }
        }
    }

    private void configuracionInterfazModerador() {
        try {
            System.out.println("🔧 Configurando controles básicos...");
            
            if (txtAreaInformacion != null) {
                txtAreaInformacion.setEditable(false);
                txtAreaInformacion.setWrapText(true);
                System.out.println("✅ txtAreaInformacion configurado");
            } else {
                System.out.println("⚠️ txtAreaInformacion es null");
            }
            
            if (txtNombreUsuarioEliminar != null) {
                txtNombreUsuarioEliminar.setPromptText("Username");
                System.out.println("✅ txtNombreUsuarioEliminar configurado");
            }
            
            if (txtNombreComunidad != null) {
                txtNombreComunidad.setPromptText("Nombre Comunidad");
                System.out.println("✅ txtNombreComunidad configurado");
            }
            
            if (txtComunidadEliminar != null) {
                txtComunidadEliminar.setPromptText("Nombre de la Comunidad a Eliminar");
                System.out.println("✅ txtComunidadEliminar configurado");
            }
            
            // Configurar controles de sanciones si existen
            configurarControlesSanciones();
            System.out.println("✅ Controles de sanciones configurados");
            
        } catch (Exception e) {
            System.err.println("❌ Error en configuracionInterfazModerador: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Configura los controles relacionados con sanciones
     */
    private void configurarControlesSanciones() {
        if (txtUsuarioSancion != null) {
            txtUsuarioSancion.setPromptText("Username del usuario a sancionar");
        }
        if (txtComunidadSancion != null) {
            txtComunidadSancion.setPromptText("Nombre de la comunidad");
        }
        if (txtRazonSancion != null) {
            txtRazonSancion.setPromptText("Razón de la sanción");
        }
        if (txtDuracionSancion != null) {
            txtDuracionSancion.setPromptText("Duración en minutos");
        }
        if (txtAreaSanciones != null) {
            txtAreaSanciones.setEditable(false);
            txtAreaSanciones.setWrapText(true);
        }
    }

    /* Eliminación de un Usuario de una Comunidad */
    @FXML
    public void eliminarUsuario() {
        String nombreUsuario = txtNombreUsuarioEliminar.getText();
        String nombreComunidad = txtNombreComunidad.getText();

        ResultadoOperacion resultado = servicioModeracion.eliminarUsuarioDeComunidad(nombreUsuario, nombreComunidad, usuarioAdmin);
        
        if (resultado.isExitoso()) {
            mostrarMensajeExito(resultado.getMensaje());
            // Limpiar los campos
            txtNombreUsuarioEliminar.clear();
            txtNombreComunidad.clear();
            actualizarInformacion();
        } else {
            mostrarMensajeError(resultado.getMensaje());
        }
    }

    /**
     * Eliminacion de una Comunidad
     * */
    @FXML
    public void eliminarComunidad() {
        String nombreComunidad = txtComunidadEliminar.getText();

        if (nombreComunidad == null || nombreComunidad.trim().isEmpty()) {
            mostrarMensajeError("Por favor, ingrese el nombre de la comunidad que desea eliminar.");
            return;
        }

        // Mostrar diálogo de confirmación
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("¿Está seguro que desea eliminar la comunidad '" + nombreComunidad + "'?");
        confirmacion.setContentText("Esta acción no se puede deshacer.");

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            ResultadoOperacion resultadoEliminacion = servicioModeracion.eliminarComunidad(nombreComunidad, usuarioAdmin);
            
            if (resultadoEliminacion.isExitoso()) {
                mostrarMensajeExito(resultadoEliminacion.getMensaje());
                // Limpiar el campo y actualizar la información
                txtComunidadEliminar.clear();
                actualizarInformacion();
                // Actualizar los ComboBox de comunidades
                cargarComunidades();
            } else {
                mostrarMensajeError(resultadoEliminacion.getMensaje());
            }
        }
    }

    @FXML
    public void listarUsuarios() {
        contadorClicsListar++;
        
        // Si se hace clic múltiples veces, mostrar el menú de sanciones
        if (contadorClicsListar >= 3) {
            mostrarMenuSanciones();
            contadorClicsListar = 0; // Resetear contador
            return;
        }
        
        StringBuilder info = new StringBuilder();
        info.append("🛡️ === PANEL DE MODERACIÓN ===\n\n");
        
        info.append("🔧 FUNCIONES DISPONIBLES:\n");
        info.append("• Eliminar usuarios de comunidades\n");
        info.append("• Eliminar comunidades completas\n");
        info.append("• Aplicar sanciones manuales ⭐ NUEVO\n");
        info.append("• Levantar sanciones existentes ⭐ NUEVO\n");
        info.append("• Ver sanciones activas ⭐ NUEVO\n");
        info.append("• Generar reportes de usuarios\n\n");
        
        info.append("💡 TIP: Las funciones de sanciones están implementadas en el backend.\n");
        info.append("Para ver ejemplos de uso, haz clic en 'Listar Usuarios' nuevamente.\n\n");
        
        // Obtener estadísticas del sistema
        String reporteUsuarios = servicioModeracion.generarReporteUsuarios();
        info.append(reporteUsuarios);
        
        // Mostrar sanciones activas
        var sancionesActivas = servicioModeracion.getSancionesActivas();
        info.append("\n🚫 SANCIONES ACTIVAS: ").append(sancionesActivas.size()).append("\n");
        
        if (!sancionesActivas.isEmpty()) {
            info.append("Usuarios sancionados:\n");
            for (var sancion : sancionesActivas) {
                info.append("  • ").append(sancion.getUsuario().getUsername())
                    .append(" (").append(sancion.getMinutosRestantes()).append(" min)\n");
            }
        }
        
        info.append("\n🔄 Clicks en 'Listar Usuarios': ").append(contadorClicsListar).append("/3");
        info.append("\n(Haz clic 3 veces para ver menú de sanciones)");
        
        txtAreaInformacion.setText(info.toString());
    }

    @FXML
    private void volver() {
        try {
            MetodosFrecuentes.cambiarVentana(
                    (Stage) btnVolver.getScene().getWindow(),
                    "/Modulo_Comunidad/Views/Admin/AdminComunidad.fxml",
                    "Sistema de Comunidad"
            );
        } catch (Exception e) {
            mostrarMensajeError("Error al volver: " + e.getMessage());
        }
    }

    private void actualizarInformacion() {
        listarUsuarios();
    }

    private void mostrarMensajeError(String mensaje) {
        txtAreaInformacion.setStyle("-fx-text-fill: red; -fx-background-color: #fff0f0; -fx-border-color: #ffcccc;");
        txtAreaInformacion.setText("❌ " + mensaje);
        restaurarEstilosDespuesDe(4);
    }

    private void mostrarMensajeExito(String mensaje) {
        txtAreaInformacion.setStyle("-fx-text-fill: green; -fx-background-color: #f0fff0; -fx-border-color: #90ee90;");
        txtAreaInformacion.setText("✅ " + mensaje);
        restaurarEstilosDespuesDe(3);
    }

    private void restaurarEstilosDespuesDe(int segundos) {
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(segundos), e -> {
                    txtAreaInformacion.setStyle("");
                    actualizarInformacion();
                })
        );
        timeline.play();
    }
    
    private void configurarControlesHistorial() {
        // Configurar los ComboBox
        comboComunidades.setItems(FXCollections.observableArrayList());
        comboTipoGrupo.setItems(FXCollections.observableArrayList(
            "Grupos de Discusión", "Grupos de Compartir"
        ));
        comboGrupos.setItems(FXCollections.observableArrayList());
        
        // Cargar comunidades disponibles
        cargarComunidades();
        
        // Listeners para actualizar las opciones dependientes
        comboComunidades.setOnAction(e -> comboTipoGrupo.setDisable(false));
        comboTipoGrupo.setOnAction(e -> cargarGruposDeComunidad());
        
        // Deshabilitar controles dependientes inicialmente
        comboTipoGrupo.setDisable(true);
        comboGrupos.setDisable(true);
        btnVerHistorial.setDisable(true);
    }
    
    private void cargarComunidades() {
        List<String> nombresComunidades = servicioModeracion.obtenerNombresComunidades();
        comboComunidades.setItems(FXCollections.observableArrayList(nombresComunidades));
    }
    
    private void cargarGruposDeComunidad() {
        comboGrupos.getItems().clear();
        
        String nombreComunidad = comboComunidades.getValue();
        String tipoGrupo = comboTipoGrupo.getValue();
        if (nombreComunidad == null || tipoGrupo == null) return;
        
        // TODO: Implementar obtenerGruposDeComunidad en ServicioModeracion
        // List<String> nombresGrupos = servicioModeracion.obtenerGruposDeComunidad(nombreComunidad, tipoGrupo);
        List<String> nombresGrupos = servicioModeracion.obtenerGruposDeComunidad(nombreComunidad, tipoGrupo);
        comboGrupos.setItems(FXCollections.observableArrayList(nombresGrupos));
        comboGrupos.setDisable(false);
        btnVerHistorial.setDisable(false);
    }
    
    @FXML
    private void verHistorial() {
        String nombreComunidad = comboComunidades.getValue();
        String tipoGrupo = comboTipoGrupo.getValue();
        String nombreGrupo = comboGrupos.getValue();
        
        if (nombreComunidad == null || tipoGrupo == null || nombreGrupo == null) {
            mostrarMensajeError("Por favor, complete todas las selecciones.");
            return;
        }
        
        // TODO: Implementar generarHistorialGrupo en ServicioModeracion
        // String historial = servicioModeracion.generarHistorialGrupo(nombreComunidad, tipoGrupo, nombreGrupo);
        String historial = servicioModeracion.generarHistorialGrupo(nombreComunidad, tipoGrupo, nombreGrupo);
        txtAreaHistorial.setText(historial);
    }
    
    // ==========================================
    // MÉTODOS PARA GESTIÓN DE SANCIONES
    // ==========================================
    
    /**
     * Aplica una sanción manual a un usuario
     */
    @FXML
    private void aplicarSancion() {
        String nombreUsuario = txtUsuarioSancion.getText().trim();
        String nombreComunidad = txtComunidadSancion.getText().trim();
        String razon = txtRazonSancion.getText().trim();
        String duracionStr = txtDuracionSancion.getText().trim();
        
        // Validar campos
        if (nombreUsuario.isEmpty() || nombreComunidad.isEmpty() || razon.isEmpty() || duracionStr.isEmpty()) {
            mostrarMensajeError("Por favor, complete todos los campos para aplicar la sanción.");
            return;
        }
        
        try {
            int duracionMinutos = Integer.parseInt(duracionStr);
            if (duracionMinutos <= 0) {
                mostrarMensajeError("La duración debe ser un número positivo de minutos.");
                return;
            }
            
            ResultadoOperacion resultado = servicioModeracion.aplicarSancionManual(
                nombreUsuario, nombreComunidad, razon, duracionMinutos, usuarioAdmin);
            
            if (resultado.isExitoso()) {
                mostrarMensajeExito(resultado.getMensaje());
                limpiarCamposSancion();
                actualizarSancionesActivas();
            } else {
                mostrarMensajeError(resultado.getMensaje());
            }
            
        } catch (NumberFormatException e) {
            mostrarMensajeError("La duración debe ser un número válido de minutos.");
        }
    }
    
    /**
     * Levanta una sanción de un usuario
     */
    @FXML
    private void levantarSancion() {
        String nombreUsuario = txtUsuarioSancion.getText().trim();
        String nombreComunidad = txtComunidadSancion.getText().trim();
        
        if (nombreUsuario.isEmpty() || nombreComunidad.isEmpty()) {
            mostrarMensajeError("Por favor, ingrese el nombre de usuario y comunidad.");
            return;
        }
        
        ResultadoOperacion resultado = servicioModeracion.levantarSancion(
            nombreUsuario, nombreComunidad, usuarioAdmin);
        
        if (resultado.isExitoso()) {
            mostrarMensajeExito(resultado.getMensaje());
            limpiarCamposSancion();
            actualizarSancionesActivas();
        } else {
            mostrarMensajeError(resultado.getMensaje());
        }
    }
    
    /**
     * Muestra todas las sanciones activas en el sistema
     */
    @FXML
    private void verSancionesActivas() {
        actualizarSancionesActivas();
    }
    
    /**
     * Actualiza la lista de sanciones activas en la interfaz
     */
    private void actualizarSancionesActivas() {
        try {
            StringBuilder sancionesInfo = new StringBuilder();
            sancionesInfo.append("🚫 === SANCIONES ACTIVAS ===\n\n");
            
            var sancionesActivas = servicioModeracion.getSancionesActivas();
            
            if (sancionesActivas.isEmpty()) {
                sancionesInfo.append("✅ No hay sanciones activas en el sistema.\n");
            } else {
                sancionesInfo.append("Total de sanciones activas: ").append(sancionesActivas.size()).append("\n\n");
                
                for (var sancion : sancionesActivas) {
                    sancionesInfo.append("👤 Usuario: ").append(sancion.getUsuario().getUsername()).append("\n");
                    sancionesInfo.append("📝 Razón: ").append(sancion.getRazon()).append("\n");
                    sancionesInfo.append("⏱️ Tiempo restante: ").append(sancion.getMinutosRestantes()).append(" minutos\n");
                    sancionesInfo.append("🛡️ Aplicada por: ").append(sancion.getModeradorResponsable()).append("\n");
                    sancionesInfo.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
                }
            }
            
            if (txtAreaSanciones != null) {
                txtAreaSanciones.setText(sancionesInfo.toString());
            } else {
                // Si no existe el área de sanciones, mostrar en el área de información
                txtAreaInformacion.setText(sancionesInfo.toString());
            }
            
        } catch (Exception e) {
            mostrarMensajeError("Error al obtener sanciones activas: " + e.getMessage());
        }
    }
    
    /**
     * Limpia los campos relacionados con sanciones
     */
    private void limpiarCamposSancion() {
        if (txtUsuarioSancion != null) txtUsuarioSancion.clear();
        if (txtComunidadSancion != null) txtComunidadSancion.clear();
        if (txtRazonSancion != null) txtRazonSancion.clear();
        if (txtDuracionSancion != null) txtDuracionSancion.clear();
    }
    
    /**
     * Método de demostración para sanciones (sin necesidad de FXML adicional)
     * Se puede llamar desde la consola o para probar funcionalidad
     */
    public void demostrarFuncionalidadSanciones() {
        System.out.println("🔧 === DEMO DE FUNCIONALIDAD DE SANCIONES ===");
        
        // Ejemplo: Aplicar sanción
        ResultadoOperacion resultado1 = servicioModeracion.aplicarSancionManual(
            "usuario1", "Comunidad Java", "Contenido inapropiado", 30, usuarioAdmin);
        System.out.println("Resultado aplicar sanción: " + resultado1.getMensaje());
        
        // Ejemplo: Ver sanciones activas
        var sanciones = servicioModeracion.getSancionesActivas();
        System.out.println("Sanciones activas: " + sanciones.size());
        
        // Ejemplo: Levantar sanción
        ResultadoOperacion resultado2 = servicioModeracion.levantarSancion(
            "usuario1", "Comunidad Java", usuarioAdmin);
        System.out.println("Resultado levantar sanción: " + resultado2.getMensaje());
        
        // Actualizar interfaz
        actualizarSancionesActivas();
    }
    
    /**
     * Método para mostrar funcionalidades de sanciones en el área de texto
     * ESTE MÉTODO PUEDE LLAMARSE DESDE EL BOTÓN "Listar Usuarios" PARA DEMOSTRAR
     */
    public void mostrarMenuSanciones() {
        StringBuilder menu = new StringBuilder();
        menu.append("🚫 === GESTIÓN DE SANCIONES ===\n\n");
        menu.append("Para usar las funciones de sanciones desde la consola:\n\n");
        menu.append("1. APLICAR SANCIÓN:\n");
        menu.append("   servicioModeracion.aplicarSancionManual(\n");
        menu.append("     \"nombreUsuario\", \"nombreComunidad\", \n");
        menu.append("     \"razón\", duraciónMinutos, usuarioAdmin)\n\n");
        
        menu.append("2. LEVANTAR SANCIÓN:\n");
        menu.append("   servicioModeracion.levantarSancion(\n");
        menu.append("     \"nombreUsuario\", \"nombreComunidad\", usuarioAdmin)\n\n");
        
        menu.append("3. VER SANCIONES ACTIVAS:\n");
        menu.append("   servicioModeracion.getSancionesActivas()\n\n");
        
        menu.append("EJEMPLO DE USO:\n");
        menu.append("• Sancionar a 'usuario1' por 30 minutos\n");
        menu.append("• Razón: 'Contenido inapropiado'\n");
        menu.append("• En comunidad: 'Comunidad Java'\n\n");
        
        menu.append("USUARIOS DISPONIBLES EN EL SISTEMA:\n");
        menu.append("• usuario1, comunidad1, admin1, Luis, Prueba\n");
        menu.append("• moderador1, moderador2, superadmin, admin\n\n");
        
        menu.append("NOTA: Para interfaz completa, se necesitan más controles FXML.\n");
        menu.append("El backend está completamente funcional! 🎉");
        
        txtAreaInformacion.setText(menu.toString());
    }
}