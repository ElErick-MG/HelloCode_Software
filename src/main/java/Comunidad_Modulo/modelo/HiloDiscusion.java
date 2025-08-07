package Comunidad_Modulo.modelo;

import Modulo_Usuario.Clases.UsuarioComunidad;
import Comunidad_Modulo.enums.EstadoHilo;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HiloDiscusion {
    private String idHilo;
    private String titulo;
    private String problema;
    private UsuarioComunidad autor;
    private EstadoHilo estado;
    private List<Respuesta> respuestas;
    private Map<String, Integer> votosUsuarios;
    private LocalDateTime fechaCreacion;

    // Constructor para crear un nuevo hilo
    public HiloDiscusion(String idHilo, String titulo, String problema, UsuarioComunidad autor) {
        this.idHilo = idHilo;
        this.titulo = titulo;
        this.problema = problema;
        this.autor = autor;
        this.estado = EstadoHilo.ABIERTO;
        this.respuestas = new ArrayList<>();
        this.votosUsuarios = new HashMap<>();
        this.fechaCreacion = LocalDateTime.now();
    }

    // Constructor para cargar desde persistencia
    public HiloDiscusion(String idHilo, String titulo, String problema, UsuarioComunidad autor, EstadoHilo estado,
                         Map<String, Integer> votosUsuarios, LocalDateTime fechaCreacion) {
        this.idHilo = idHilo;
        this.titulo = titulo;
        this.problema = problema;
        this.autor = autor;
        this.estado = estado;
        this.respuestas = new ArrayList<>();
        this.votosUsuarios = (votosUsuarios != null) ? new HashMap<>(votosUsuarios) : new HashMap<>();
        this.fechaCreacion = fechaCreacion;
    }

    // Getters y setters
    public String getIdHilo() {
        return idHilo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getProblema() {
        return problema;
    }

    public UsuarioComunidad getAutor() {
        return autor;
    }

    public EstadoHilo getEstado() {
        return estado;
    }

    public void setEstado(EstadoHilo estado) {
        this.estado = estado;
    }

    public List<Respuesta> getRespuestas() {
        return new ArrayList<>(respuestas);
    }

    // Método para añadir una respuesta
    public void addRespuesta(Respuesta respuesta) {
        this.respuestas.add(respuesta);
    }

    public Map<String, Integer> getVotosUsuarios() {
        return new HashMap<>(votosUsuarios);
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    // Métodos de negocio
    public boolean responder(String contenido, UsuarioComunidad autorRespuesta, Moderador moderador) {
        if (estado == EstadoHilo.CERRADO) {
            System.out.println("🚫 No se puede responder a un hilo cerrado");
            return false;
        }

        // Moderar el contenido de la respuesta usando el método abstracto
        boolean contenidoAprobado = moderador.moderarContenido(contenido, autorRespuesta);

        if (!contenidoAprobado) {
            System.out.println("🚫 RESPUESTA BLOQUEADA EN HILO DE DISCUSIÓN");
            System.out.println("   Usuario: " + autorRespuesta.getNombre());
            System.out.println("   Hilo: " + this.titulo);
            System.out.println("   Contenido: \"" + contenido + "\"");
            System.out.println("   Razón: Contenido no aprobado por moderación");
            return false;
        }

        Respuesta respuesta = new Respuesta(UUID.randomUUID().toString(), contenido, autorRespuesta);
        respuestas.add(respuesta);

        // Otorgar puntos de reputación por participar
        autorRespuesta.incrementarReputacion(2);

        System.out.println("✅ Respuesta agregada al hilo \"" + this.titulo + "\" por " + autorRespuesta.getNombre());
        return true;
    }

    // Método legacy para compatibilidad (sin moderación)
    @Deprecated
    public void responder(String contenido, UsuarioComunidad autorRespuesta) {
        if (estado == EstadoHilo.CERRADO) {
            throw new IllegalStateException("No se puede responder a un hilo cerrado");
        }

        Respuesta respuesta = new Respuesta(UUID.randomUUID().toString(), contenido, autorRespuesta);
        respuestas.add(respuesta);

        // Otorgar puntos de reputación por participar
        autorRespuesta.incrementarReputacion(2);
    }

    /**
     * Permite a un usuario votar por el hilo (votos positivos, votos negativos, o quitar voto).
     * @param usuarioVotante El usuario que vota.
     * @param tipoVoto 1 para positivo, -1 para negativo, 0 para quitar voto.
     */
    public void votar(UsuarioComunidad usuarioVotante, int tipoVoto) {
        String username = usuarioVotante.getUsername();
        int votoAnterior = votosUsuarios.getOrDefault(username, 0);

        if (tipoVoto == 1) {
            if (votoAnterior == 1) {
                System.out.println("ℹ️ El usuario " + username + " ya había dado voto positivo a este hilo.");
            } else {
                votosUsuarios.put(username, 1);
                if (votoAnterior == -1) {
                    autor.incrementarReputacion(2);
                } else {
                    autor.incrementarReputacion(1);
                }
                System.out.println("✅ Usuario " + username + " dio voto positivo al hilo.");
            }
        } else if (tipoVoto == -1) {
            if (votoAnterior == -1) {
                System.out.println("ℹ️ El usuario " + username + " ya había dado voto negativo a este hilo.");
            } else {
                votosUsuarios.put(username, -1);
                if (votoAnterior == 1) {
                    autor.decrementarReputacion(2);
                } else {
                    autor.decrementarReputacion(1);
                }
                System.out.println("✅ Usuario " + username + " dio voto negativo al hilo.");
            }
        } else if (tipoVoto == 0) {
            if (votoAnterior == 0) {
                System.out.println("ℹ️ El usuario " + username + " no había votado este hilo.");
            } else {
                votosUsuarios.remove(username);
                if (votoAnterior == 1) {
                    autor.decrementarReputacion(1);
                } else if (votoAnterior == -1) {
                    autor.incrementarReputacion(1);
                }
                System.out.println("✅ Usuario " + username + " quitó su voto del hilo.");
            }
        } else {
            System.out.println("⚠️ Tipo de voto inválido. Use 1 (voto positivo), -1 (voto negativo) o 0 (quitar voto).");
        }
    }

    /**
     * Calcula el número total de votos positivos.
     * @return El número de votos positivos.
     */
    public int getVotosPositivos() {
        return (int) votosUsuarios.values().stream().filter(v -> v == 1).count();
    }

    /**
     * Calcula el número total de votos negativos.
     * @return El número de votos negativos.
     */
    public int getVotosNegativos() {
        return (int) votosUsuarios.values().stream().filter(v -> v == -1).count();
    }

    public void marcarResuelto() {
        this.estado = EstadoHilo.RESUELTO;
        // Otorgar puntos de reputación por resolver problema
        autor.incrementarReputacion(5);
    }

    public void cerrar() {
        this.estado = EstadoHilo.CERRADO;
    }

    public void reabrir() {
        if (estado == EstadoHilo.CERRADO || estado == EstadoHilo.RESUELTO) {
            this.estado = EstadoHilo.ABIERTO;
        }
    }

    public List<Respuesta> getSoluciones() {
        return respuestas.stream()
                .filter(Respuesta::getEsSolucion)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    @Override
    public String toString() {
        return String.format("Hilo: %s por %s [%s] (\u2B06%d \u2B07%d, %d respuestas)",
                titulo, autor.getNombre(), estado, getVotosPositivos(), getVotosNegativos(), respuestas.size());
    }
}
