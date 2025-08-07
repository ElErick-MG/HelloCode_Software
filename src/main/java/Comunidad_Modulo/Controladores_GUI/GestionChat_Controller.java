package Comunidad_Modulo.Controladores_GUI;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

// Importar las clases del modelo y controladores
import Comunidad_Modulo.controladores.ContextoSistema;
import Comunidad_Modulo.modelo.*;
import Modulo_Usuario.Clases.UsuarioComunidad;
import MetodosGlobales.MetodosFrecuentes;

/**
 * Controlador para la gestión de chats privados en la interfaz JavaFX
 * Maneja creación de chats, envío de mensajes y visualización de historial
 * Siguiendo la lógica de consola ya implementada
 */
public class GestionChat_Controller implements Initializable {

    // Referencias FXML a los elementos de la interfaz
    @FXML private TextField txtParticipantes;
    @FXML private TextField txtMensaje;
    @FXML private TextField txtIndice;
    @FXML private TextArea txtInformacion;
    @FXML private TextArea txtNotificacionesModerador;
    @FXML private Button btnVolver;
    @FXML private Button btnLimpiarNotificaciones;

    // Controladores de negocio reutilizados del sistema existente
    private ContextoSistema contexto;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Inicializando controlador de Gestión de Chats...");

        // Inicializar los componentes del sistema
        inicializarSistema();

        // Configurar la interfaz
        configurarInterfaz();

        // Actualizar información inicial
        actualizarInformacion();

        System.out.println("✅ Controlador de Gestión de Chats inicializado correctamente");
    }

    /**
     * Inicializar el sistema con los componentes existentes
     */
    private void inicializarSistema() {
        // Obtener la instancia del contexto (singleton)
        this.contexto = ContextoSistema.getInstance();
        
        // Forzar la recarga de datos desde persistencia
        System.out.println("🔄 Forzando recarga de datos desde persistencia...");
        contexto.cargarDatosDesdePersistencia();
        
        // Solo mostrar información de debug si hay una comunidad activa
        if (contexto.tieneComunidadActiva()) {
            Comunidad comunidad = contexto.getComunidadActual();
            int totalChats = comunidad.getChatsPrivados().size();
            System.out.println("✅ Sistema inicializado - Chats disponibles: " + totalChats);
            
            // Debug adicional para verificar usuario actual
            UsuarioComunidad usuarioActual = obtenerUsuarioActual();
            if (usuarioActual != null) {
                System.out.println("🔍 Usuario actual identificado: " + usuarioActual.getUsername() + " (" + usuarioActual.getNombre() + ")");
            } else {
                System.out.println("⚠️ No se pudo identificar usuario actual en inicialización");
            }
        } else {
            System.out.println("⚠️ No hay comunidad activa después de cargar datos");
        }
    }

    /**
     * Configurar elementos de la interfaz
     */
    private void configurarInterfaz() {
        // Configurar área de información como solo lectura
        txtInformacion.setEditable(false);
        txtInformacion.setWrapText(true);

        // Configurar área de notificaciones del moderador
        if (txtNotificacionesModerador != null) {
            txtNotificacionesModerador.setEditable(false);
            txtNotificacionesModerador.setWrapText(true);
            txtNotificacionesModerador.setStyle("-fx-background-color: #fff8dc; -fx-border-color: #ffa500; -fx-text-fill: #8b4513;");
            txtNotificacionesModerador.setText("=== NOTIFICACIONES DE MODERACIÓN ===\n\nAquí aparecerán los mensajes de sanciones, bloqueos y eventos de moderación...");
        }

        // Configurar placeholder texts
        txtParticipantes.setPromptText("No usado - se seleccionarán dinámicamente");
        txtMensaje.setPromptText("No usado - se pedirá en el momento");
        txtIndice.setPromptText("No usado - se seleccionará dinámicamente");

        // Deshabilitar campos ya que usaremos la lógica de selección dinámica
        txtParticipantes.setDisable(true);
        txtMensaje.setDisable(true);
        txtIndice.setDisable(true);

        // Estilo para el área de información
        txtInformacion.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc;");
    }

    /**
     * Método FXML para crear un chat privado - mejorado para usar miembros de comunidad
     */
    @FXML
    private void crearChatPrivado() {
        try {
            // Verificar que hay una comunidad activa
            if (!contexto.tieneComunidadActiva()) {
                mostrarMensajeError("⚠️ No hay una comunidad activa.");
                return;
            }

            // Obtener usuario actual automáticamente
            UsuarioComunidad usuarioActual = obtenerUsuarioActual();
            if (usuarioActual == null) {
                mostrarMensajeError("⚠️ No se pudo identificar el usuario actual.");
                return;
            }

            // Obtener miembros de la comunidad (excluyendo al usuario actual)
            List<UsuarioComunidad> miembrosComunidad = new ArrayList<>();
            for (UsuarioComunidad miembro : contexto.getComunidadActual().getUsuariosMiembros()) {
                if (!miembro.getUsername().equals(usuarioActual.getUsername())) {
                    miembrosComunidad.add(miembro);
                }
            }
            
            // También incluir usuarios conectados que no sean el actual
            for (UsuarioComunidad conectado : contexto.getComunidadActual().getUsuariosConectados()) {
                if (!conectado.getUsername().equals(usuarioActual.getUsername()) && 
                    !miembrosComunidad.contains(conectado)) {
                    miembrosComunidad.add(conectado);
                }
            }

            if (miembrosComunidad.isEmpty()) {
                mostrarMensajeError("⚠️ No hay otros miembros en la comunidad para chatear.");
                return;
            }

            // Mostrar miembros disponibles
            StringBuilder miembrosInfo = new StringBuilder("=== MIEMBROS DE LA COMUNIDAD ===\n\n");
            miembrosInfo.append("👤 Usuario actual: ").append(usuarioActual.getNombre()).append("\n\n");
            miembrosInfo.append("💬 Seleccione con quién quiere chatear:\n\n");
            
            for (int i = 0; i < miembrosComunidad.size(); i++) {
                UsuarioComunidad miembro = miembrosComunidad.get(i);
                miembrosInfo.append(String.format("%d. %s (%s) - Nivel: %s\n",
                        (i + 1), miembro.getNombre(), miembro.getUsername(), miembro.getNivelJava()));
            }

            // Seleccionar destinatario del mensaje
            TextInputDialog destinatarioDialog = new TextInputDialog();
            destinatarioDialog.setTitle("Nuevo Chat Privado");
            destinatarioDialog.setHeaderText(miembrosInfo.toString());
            destinatarioDialog.setContentText("Seleccione el destinatario (número):");

            Optional<String> destinatarioResult = destinatarioDialog.showAndWait();
            if (!destinatarioResult.isPresent()) return;

            int indiceDestinatario;
            try {
                indiceDestinatario = Integer.parseInt(destinatarioResult.get().trim()) - 1;
                if (indiceDestinatario < 0 || indiceDestinatario >= miembrosComunidad.size()) {
                    mostrarMensajeError("❌ Índice de miembro inválido.");
                    return;
                }
            } catch (NumberFormatException e) {
                mostrarMensajeError("❌ Debe ingresar un número válido.");
                return;
            }

            UsuarioComunidad destinatario = miembrosComunidad.get(indiceDestinatario);

            // Verificar si ya existe un chat entre estos usuarios
            ChatPrivado chatExistente = buscarChatExistente(usuarioActual, destinatario);
            if (chatExistente != null) {
                mostrarMensajeInfo("ℹ️ Ya existe un chat con " + destinatario.getNombre() + ". Puede enviar mensajes directamente.");
                actualizarInformacion();
                return;
            }

            // Crear el chat privado
            List<UsuarioComunidad> participantes = new ArrayList<>();
            participantes.add(usuarioActual);
            participantes.add(destinatario);

            ChatPrivado nuevoChat = new ChatPrivado(participantes);
            
            // Agregar el chat a la comunidad usando el método oficial
            Comunidad comunidad = contexto.getComunidadActual();
            comunidad.agregarChatPrivado(nuevoChat);
            
            // Verificar que se agregó correctamente
            int totalChatsPostAgregar = comunidad.getChatsPrivados().size();
            System.out.println("🔍 Chats después de agregar a la comunidad: " + totalChatsPostAgregar);

            // Guardar en persistencia
            contexto.guardarChatPrivado(comunidad, nuevoChat);

            // Verificar una vez más después de guardar
            int totalChatsPostGuardado = contexto.getComunidadActual().getChatsPrivados().size();
            System.out.println("🔍 Chats después de guardar en persistencia: " + totalChatsPostGuardado);

            mostrarMensajeExito("✅ Chat privado creado exitosamente con " + destinatario.getNombre());
            
            // Actualización inmediata de la interfaz - sin Platform.runLater
            actualizarInformacion();
            System.out.println("🔄 Interfaz actualizada después de crear chat");

        } catch (Exception e) {
            mostrarMensajeError("❌ Error al crear chat privado: " + e.getMessage());
        }
    }

    /**
     * Método FXML para enviar un mensaje - siguiendo lógica de consola
     */
    @FXML
    private void enviarMensaje() {
        try {
            // Verificar que hay una comunidad activa
            if (!contexto.tieneComunidadActiva()) {
                mostrarMensajeError("⚠️ No hay una comunidad activa.");
                return;
            }

            List<ChatPrivado> chats = contexto.getComunidadActual().getChatsPrivados();
            System.out.println("🔍 Chats detectados al enviar mensaje: " + chats.size());
            
            if (chats.isEmpty()) {
                mostrarMensajeError("⚠️ No hay chats privados disponibles. Primero cree uno.");
                return;
            }

            // Mostrar chats disponibles
            StringBuilder chatsInfo = new StringBuilder("=== CHATS PRIVADOS DISPONIBLES ===\n\n");
            for (int i = 0; i < chats.size(); i++) {
                ChatPrivado chat = chats.get(i);
                chatsInfo.append(String.format("%d. Chat con %d participantes (%d mensajes)\n",
                        (i + 1), chat.getParticipantes().size(), chat.getMensajes().size()));

                // Mostrar participantes
                chatsInfo.append("   Participantes: ");
                List<UsuarioComunidad> participantes = chat.getParticipantes();
                for (int j = 0; j < participantes.size(); j++) {
                    chatsInfo.append(participantes.get(j).getNombre());
                    if (j < participantes.size() - 1) {
                        chatsInfo.append(", ");
                    }
                }
                chatsInfo.append("\n   ────────────────────────────────\n");
            }

            // Seleccionar chat
            TextInputDialog chatDialog = new TextInputDialog();
            chatDialog.setTitle("Enviar Mensaje");
            chatDialog.setHeaderText(chatsInfo.toString());
            chatDialog.setContentText("Seleccione el chat (número):");

            Optional<String> chatResult = chatDialog.showAndWait();
            if (!chatResult.isPresent()) return;

            int indiceChat;
            try {
                indiceChat = Integer.parseInt(chatResult.get().trim()) - 1;
                if (indiceChat < 0 || indiceChat >= chats.size()) {
                    mostrarMensajeError("❌ Selección de chat inválida.");
                    return;
                }
            } catch (NumberFormatException e) {
                mostrarMensajeError("❌ Debe ingresar un número válido.");
                return;
            }

            ChatPrivado chat = chats.get(indiceChat);
            List<UsuarioComunidad> participantes = chat.getParticipantes();

            // Obtener usuario actual como emisor automáticamente
            UsuarioComunidad usuarioActual = obtenerUsuarioActual();
            if (usuarioActual == null) {
                mostrarMensajeError("⚠️ No se pudo identificar el usuario actual.");
                return;
            }

            // Verificar que el usuario actual es participante del chat usando username
            String usernameEmisor = usuarioActual.getUsername();
            final UsuarioComunidad emisorFinal = participantes.stream()
                    .filter(p -> p.getUsername().equals(usernameEmisor))
                    .findFirst()
                    .orElse(null);
            
            if (emisorFinal == null) {
                mostrarMensajeError("❌ No puedes enviar mensajes en este chat (no eres participante).");
                System.out.println("🔍 Debug - Usuario emisor: " + usernameEmisor);
                System.out.println("🔍 Debug - Participantes del chat: " + 
                    participantes.stream().map(UsuarioComunidad::getUsername).reduce((a, b) -> a + ", " + b).orElse(""));
                return;
            }

            // Mostrar información del chat y destinatarios
            StringBuilder destinatariosInfo = new StringBuilder();
            destinatariosInfo.append("💬 Enviando mensaje en chat con: ");
            participantes.stream()
                    .filter(p -> !p.getUsername().equals(emisorFinal.getUsername()))
                    .forEach(p -> destinatariosInfo.append(p.getNombre()).append(" "));

            // Solicitar contenido del mensaje
            TextInputDialog mensajeDialog = new TextInputDialog();
            mensajeDialog.setTitle("Enviar Mensaje");
            mensajeDialog.setHeaderText("👤 Emisor: " + emisorFinal.getNombre() + "\n" + destinatariosInfo.toString());
            mensajeDialog.setContentText("Escriba el mensaje:");

            Optional<String> mensajeResult = mensajeDialog.showAndWait();
            if (!mensajeResult.isPresent() || mensajeResult.get().trim().isEmpty()) {
                return;
            }

            String contenido = mensajeResult.get().trim();

            // Obtener el moderador automático para análisis de contenido en tiempo real
            Moderador moderador = contexto.getComunidadActual().getModerador();
            
            // Verificar que sea un moderador automático para este contexto
            if (!(moderador instanceof ModeradorAutomatico)) {
                mostrarMensajeError("⚠️ No hay moderador automático disponible para analizar el contenido.");
                return;
            }
            
            ModeradorAutomatico moderadorAutomatico = (ModeradorAutomatico) moderador;
            
            // Capturar el estado inicial para detectar nuevas sanciones
            List<SancionUsuario> sancionesAnteriores = new ArrayList<>(moderadorAutomatico.getSancionesActivas());
            boolean estabaSancionado = moderadorAutomatico.usuarioEstaSancionado(emisorFinal);

            // Enviar mensaje con moderación automática
            boolean mensajeEnviado = chat.enviarMensaje(contenido, emisorFinal, moderadorAutomatico);

            // Detectar nuevas sanciones aplicadas durante el proceso
            List<SancionUsuario> sancionesActuales = moderadorAutomatico.getSancionesActivas();
            boolean seAplicoNuevaSancion = sancionesActuales.size() > sancionesAnteriores.size();

            if (seAplicoNuevaSancion) {
                // Encontrar la nueva sanción
                SancionUsuario nuevaSancion = null;
                for (SancionUsuario sancion : sancionesActuales) {
                    if (!sancionesAnteriores.contains(sancion)) {
                        nuevaSancion = sancion;
                        break;
                    }
                }

                if (nuevaSancion != null) {
                    String notificacion = String.format("🚫 SANCIÓN APLICADA: %s\n🚫 MENSAJE BLOQUEADO EN CHAT PRIVADO\n   Usuario: %s\n   Contenido: \"%s\"\n   Razón: Mensaje bloqueado. %s. Sanción de %d minutos aplicada.",
                            nuevaSancion.toString(),
                            emisorFinal.getNombre(),
                            contenido,
                            nuevaSancion.getRazon(),
                            (int) (nuevaSancion.getFechaFin().toEpochSecond(java.time.ZoneOffset.UTC) - nuevaSancion.getFechaInicio().toEpochSecond(java.time.ZoneOffset.UTC)) / 60);
                    agregarNotificacionModerador(notificacion);
                }
            } else if (!mensajeEnviado && estabaSancionado) {
                // Usuario ya estaba sancionado
                SancionUsuario sancion = moderadorAutomatico.getSancionActiva(emisorFinal);
                String notificacion = String.format("🚫 MENSAJE BLOQUEADO - Usuario %s está sancionado. Tiempo restante: %d minutos\n   Razón: %s",
                        emisorFinal.getNombre(), sancion.getMinutosRestantes(), sancion.getRazon());
                agregarNotificacionModerador(notificacion);
            }

            if (mensajeEnviado) {
                String notificacion = "✅ Mensaje enviado en chat privado por " + emisorFinal.getNombre();
                agregarNotificacionModerador(notificacion);
                mostrarMensajeExito("✅ Mensaje enviado exitosamente por " + emisorFinal.getNombre());
            } else {
                mostrarMensajeError("🚫 El mensaje no pudo ser enviado (contenido inapropiado o usuario sancionado).");
            }

            actualizarInformacion();

        } catch (Exception e) {
            mostrarMensajeError("❌ Error al enviar mensaje: " + e.getMessage());
        }
    }

    /**
     * Método FXML para ver historial de chat - siguiendo lógica de consola
     */
    @FXML
    private void verHistorialChat() {
        try {
            // Verificar que hay una comunidad activa
            if (!contexto.tieneComunidadActiva()) {
                mostrarMensajeError("⚠️ No hay una comunidad activa.");
                return;
            }

            List<ChatPrivado> chats = contexto.getComunidadActual().getChatsPrivados();
            if (chats.isEmpty()) {
                mostrarMensajeError("⚠️ No hay chats privados disponibles.");
                return;
            }

            // Mostrar chats disponibles
            StringBuilder chatsInfo = new StringBuilder("=== CHATS PRIVADOS DISPONIBLES ===\n\n");
            for (int i = 0; i < chats.size(); i++) {
                ChatPrivado chat = chats.get(i);
                chatsInfo.append(String.format("%d. Chat con %d participantes (%d mensajes)\n",
                        (i + 1), chat.getParticipantes().size(), chat.getMensajes().size()));

                // Mostrar participantes
                chatsInfo.append("   Participantes: ");
                List<UsuarioComunidad> participantes = chat.getParticipantes();
                for (int j = 0; j < participantes.size(); j++) {
                    chatsInfo.append(participantes.get(j).getNombre());
                    if (j < participantes.size() - 1) {
                        chatsInfo.append(", ");
                    }
                }
                chatsInfo.append("\n   ────────────────────────────────\n");
            }

            // Seleccionar chat
            TextInputDialog chatDialog = new TextInputDialog();
            chatDialog.setTitle("Ver Historial de Chat");
            chatDialog.setHeaderText(chatsInfo.toString());
            chatDialog.setContentText("Seleccione el chat (número):");

            Optional<String> chatResult = chatDialog.showAndWait();
            if (!chatResult.isPresent()) return;

            try {
                int indiceChat = Integer.parseInt(chatResult.get().trim()) - 1;
                if (indiceChat < 0 || indiceChat >= chats.size()) {
                    mostrarMensajeError("❌ Selección de chat inválida.");
                    return;
                }

                ChatPrivado chat = chats.get(indiceChat);
                List<Mensaje> mensajes = chat.obtenerHistorial();

                // Mostrar historial
                StringBuilder historial = new StringBuilder("=== HISTORIAL DE MENSAJES ===\n\n");
                if (mensajes.isEmpty()) {
                    historial.append("No hay mensajes en este chat.\n");
                } else {
                    for (Mensaje mensaje : mensajes) {
                        historial.append(String.format("[%s] %s: %s\n",
                                mensaje.getFechaEnvio().toString().substring(0, 16),
                                mensaje.getEmisor().getNombre(),
                                mensaje.getContenido()));
                    }
                }

                mostrarInformacion(historial.toString());

            } catch (NumberFormatException e) {
                mostrarMensajeError("❌ Debe ingresar un número válido.");
            }

        } catch (Exception e) {
            mostrarMensajeError("❌ Error al ver historial: " + e.getMessage());
        }
    }

    /**
     * Método FXML para listar todos los chats
     */
    @FXML
    private void listarChats() {
        try {
            if (!contexto.tieneComunidadActiva()) {
                mostrarMensajeError("⚠️ No hay una comunidad activa.");
                return;
            }

            List<ChatPrivado> chats = contexto.getComunidadActual().getChatsPrivados();

            if (chats.isEmpty()) {
                mostrarMensajeInfo("ℹ️ No hay chats privados creados.");
            } else {
                StringBuilder info = new StringBuilder("=== TODOS LOS CHATS PRIVADOS ===\n\n");
                for (int i = 0; i < chats.size(); i++) {
                    ChatPrivado chat = chats.get(i);
                    info.append(String.format("%d. Chat ID: %s\n", (i + 1),
                            chat.getIdChat().substring(0, 8) + "..."));
                    info.append(String.format("   Participantes (%d): ", chat.getParticipantes().size()));

                    List<UsuarioComunidad> participantes = chat.getParticipantes();
                    for (int j = 0; j < participantes.size(); j++) {
                        info.append(participantes.get(j).getNombre());
                        if (j < participantes.size() - 1) {
                            info.append(", ");
                        }
                    }

                    info.append(String.format("\n   Mensajes: %d | Creado: %s\n",
                            chat.getMensajes().size(),
                            chat.getFechaCreacion().toString().substring(0, 16)));
                    info.append("   ════════════════════════════════\n");
                }

                mostrarInformacion(info.toString());
            }

        } catch (Exception e) {
            mostrarMensajeError("❌ Error al listar chats: " + e.getMessage());
        }
    }


    /**
     * Actualiza la información mostrada en la interfaz
     */
    private void actualizarInformacion() {
        StringBuilder info = new StringBuilder();

        info.append("=== INFORMACIÓN DE CHATS PRIVADOS ===\n\n");

        if (contexto.tieneComunidadActiva()) {
            Comunidad comunidad = contexto.getComunidadActual();
            List<ChatPrivado> chats = comunidad.getChatsPrivados();
            
            // Debug para verificar chats
            System.out.println("🔍 actualizarInformacion - Chats detectados: " + chats.size());

            info.append("📌 Comunidad: ").append(comunidad.getNombre()).append("\n");
            info.append("👥 Usuarios conectados: ").append(comunidad.getUsuariosConectados().size()).append("\n");

            // Mostrar usuarios conectados (primeros 3)
            if (!comunidad.getUsuariosConectados().isEmpty()) {
                info.append("   ");
                List<UsuarioComunidad> usuarios = comunidad.getUsuariosConectados();
                for (int i = 0; i < usuarios.size() && i < 3; i++) {
                    info.append(usuarios.get(i).getNombre());
                    if (i < usuarios.size() - 1 && i < 2) {
                        info.append(", ");
                    }
                }
                if (usuarios.size() > 3) {
                    info.append(" y ").append(usuarios.size() - 3).append(" más...");
                }
                info.append("\n");
            }

            info.append("\n💬 Total de chats privados: ").append(chats.size()).append("\n");

            if (!chats.isEmpty()) {
                int totalMensajes = 0;
                for (ChatPrivado chat : chats) {
                    totalMensajes += chat.getMensajes().size();
                }
                info.append("📝 Total de mensajes: ").append(totalMensajes).append("\n");

                info.append("\n📊 Resumen de chats:\n");
                for (int i = 0; i < chats.size() && i < 3; i++) {
                    ChatPrivado chat = chats.get(i);
                    info.append("   • Chat ").append(i + 1).append(": ");
                    info.append(chat.getParticipantes().size()).append(" participantes, ");
                    info.append(chat.getMensajes().size()).append(" mensajes\n");
                }

                if (chats.size() > 3) {
                    info.append("   • Y ").append(chats.size() - 3).append(" chat").append(chats.size() - 3 > 1 ? "s" : "").append(" más...\n");
                }
            } else {
                info.append("\n💡 Tip: Use 'Crear Chat Privado' para comenzar una conversación privada");
            }

        } else {
            info.append("⚠️ No hay comunidad activa\n");
            info.append("Primero debe crear o seleccionar una comunidad");
        }

        txtInformacion.setText(info.toString());
    }

    /**
     * Muestra información general en el área de texto
     */
    private void mostrarInformacion(String mensaje) {
        txtInformacion.setText(mensaje);
    }

    /**
     * Agrega una notificación de moderación al área correspondiente
     */
    private void agregarNotificacionModerador(String notificacion) {
        if (txtNotificacionesModerador != null) {
            String contenidoActual = txtNotificacionesModerador.getText();

            // Si es el mensaje inicial, reemplazarlo
            if (contenidoActual.contains("Aquí aparecerán los mensajes")) {
                txtNotificacionesModerador.setText("=== NOTIFICACIONES DE MODERACIÓN ===\n\n" + notificacion + "\n");
            } else {
                // Agregar la nueva notificación al final
                txtNotificacionesModerador.setText(contenidoActual + notificacion + "\n");
            }

            // Hacer scroll hacia abajo para ver la última notificación
            txtNotificacionesModerador.setScrollTop(Double.MAX_VALUE);

            // Si hay muchas notificaciones, mantener solo las últimas 10
            String[] lineas = txtNotificacionesModerador.getText().split("\n");
            if (lineas.length > 15) { // Header + 10 notificaciones aproximadamente
                StringBuilder nuevoContenido = new StringBuilder("=== NOTIFICACIONES DE MODERACIÓN ===\n\n");
                // Mantener las últimas 10 líneas después del header
                for (int i = Math.max(3, lineas.length - 10); i < lineas.length; i++) {
                    nuevoContenido.append(lineas[i]).append("\n");
                }
                txtNotificacionesModerador.setText(nuevoContenido.toString());
            }
        }

        // También imprimir en consola para compatibilidad
        System.out.println(notificacion);
    }

    /**
     * Método FXML para volver a la pantalla anterior
     */
    @FXML
    private void volver() {
        MetodosFrecuentes.cambiarVentana((Stage) btnVolver.getScene().getWindow(),
                "/Modulo_Comunidad/Views/Comunidad.fxml",
                "Sistema de Comunidad");
    }

    /**
     * Limpia las notificaciones de moderación
     */
    @FXML
    public void limpiarNotificacionesModerador() {
        if (txtNotificacionesModerador != null) {
            txtNotificacionesModerador.setText("=== NOTIFICACIONES DE MODERACIÓN ===\n\nAquí aparecerán los mensajes de sanciones, bloqueos y eventos de moderación...");
        }
    }

    /**
     * Muestra un mensaje de éxito con color verde
     */
    private void mostrarMensajeExito(String mensaje) {
        txtInformacion.setStyle("-fx-text-fill: green; -fx-background-color: #f0fff0; -fx-border-color: #90ee90;");
        txtInformacion.setText(mensaje);

        // Restaurar estilo después de 3 segundos
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            txtInformacion.setStyle("-fx-text-fill: black; -fx-background-color: #f4f4f4; -fx-border-color: #cccccc;");
            actualizarInformacion();
        }));
        timeline.play();
    }

    /**
     * Muestra un mensaje de error con color rojo
     */
    private void mostrarMensajeError(String mensaje) {
        txtInformacion.setStyle("-fx-text-fill: red; -fx-background-color: #fff0f0; -fx-border-color: #ffcccc;");
        txtInformacion.setText(mensaje);

        // Restaurar estilo después de 4 segundos
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(4), e -> {
            txtInformacion.setStyle("-fx-text-fill: black; -fx-background-color: #f4f4f4; -fx-border-color: #cccccc;");
            actualizarInformacion();
        }));
        timeline.play();
    }

    /**
     * Muestra un mensaje de información con color azul
     */
    private void mostrarMensajeInfo(String mensaje) {
        txtInformacion.setStyle("-fx-text-fill: blue; -fx-background-color: #f0f8ff; -fx-border-color: #add8e6;");
        txtInformacion.setText(mensaje);

        // Restaurar estilo después de 3 segundos
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            txtInformacion.setStyle("-fx-text-fill: black; -fx-background-color: #f4f4f4; -fx-border-color: #cccccc;");
            actualizarInformacion();
        }));
        timeline.play();
    }

    /**
     * Obtiene el usuario actual de la sesión
     */
    private UsuarioComunidad obtenerUsuarioActual() {
        try {
            MetodosGlobales.SesionManager sesion = MetodosGlobales.SesionManager.getInstancia();
            if (sesion.hayUsuarioAutenticado()) {
                return sesion.getUsuarioComunidad();
            }
            
            // Si no hay usuario en sesión, intentar obtener del primer usuario conectado
            List<UsuarioComunidad> conectados = contexto.getComunidadActual().getUsuariosConectados();
            if (!conectados.isEmpty()) {
                return conectados.get(0);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Busca si ya existe un chat privado entre dos usuarios
     */
    private ChatPrivado buscarChatExistente(UsuarioComunidad usuario1, UsuarioComunidad usuario2) {
        for (ChatPrivado chat : contexto.getComunidadActual().getChatsPrivados()) {
            List<UsuarioComunidad> participantes = chat.getParticipantes();
            if (participantes.size() == 2) {
                boolean tieneUsuario1 = participantes.stream()
                        .anyMatch(p -> p.getUsername().equals(usuario1.getUsername()));
                boolean tieneUsuario2 = participantes.stream()
                        .anyMatch(p -> p.getUsername().equals(usuario2.getUsername()));
                
                if (tieneUsuario1 && tieneUsuario2) {
                    return chat;
                }
            }
        }
        return null;
    }
}
