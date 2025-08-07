package Comunidad_Modulo.Controladores_GUI;

import Comunidad_Modulo.servicios.ModeradorService;
import Comunidad_Modulo.servicios.ModeradorService.ResultadoOperacion;
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

    private ModeradorService moderadorService;

    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("🔄 Inicializando controlador de Moderador...");
        inicializarSistema();
        configuracionInterfazModerador();
        actualizarInformacion();
        configurarControlesHistorial();
    }

    private void inicializarSistema() {
        this.moderadorService = new ModeradorService();
    }

    private void configuracionInterfazModerador() {
        txtAreaInformacion.setEditable(false);
        txtAreaInformacion.setWrapText(true);
        txtNombreUsuarioEliminar.setPromptText("Username");
        txtNombreComunidad.setPromptText("Nombre Comunidad");
        txtComunidadEliminar.setPromptText("Nombre de la Comunidad a Eliminar");
    }

    /* Eliminación de un Usuario de una Comunidad */
    @FXML
    public void eliminarUsuario() {
        String nombreUsuario = txtNombreUsuarioEliminar.getText();
        String nombreComunidad = txtNombreComunidad.getText();

        ResultadoOperacion resultado = moderadorService.eliminarUsuarioDeComunidad(nombreUsuario, nombreComunidad);
        
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
            ResultadoOperacion resultadoEliminacion = moderadorService.eliminarComunidad(nombreComunidad);
            
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
        String reporte = moderadorService.generarReporteUsuarios();
        txtAreaInformacion.setText(reporte);
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
        List<String> nombresComunidades = moderadorService.obtenerNombresComunidades();
        comboComunidades.setItems(FXCollections.observableArrayList(nombresComunidades));
    }
    
    private void cargarGruposDeComunidad() {
        comboGrupos.getItems().clear();
        
        String nombreComunidad = comboComunidades.getValue();
        String tipoGrupo = comboTipoGrupo.getValue();
        if (nombreComunidad == null || tipoGrupo == null) return;
        
        List<String> nombresGrupos = moderadorService.obtenerGruposDeComunidad(nombreComunidad, tipoGrupo);
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
        
        String historial = moderadorService.generarHistorialGrupo(nombreComunidad, tipoGrupo, nombreGrupo);
        txtAreaHistorial.setText(historial);
    }
}