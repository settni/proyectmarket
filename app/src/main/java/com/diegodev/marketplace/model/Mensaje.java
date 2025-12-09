package com.diegodev.marketplace.model;

import com.google.firebase.database.ServerValue;
import java.util.HashMap;
import java.util.Map;

/**
 * Clase de modelo (POJO) para representar un mensaje en el chat,
 * incluyendo métodos para usar ServerValue.TIMESTAMP.
 */
public class Mensaje {

    private String mensajeId;
    private String contenido;
    private String remitenteId;
    private String receptorId;
    private Object timestamp; // Tipo Object para manejar Long (lectura) o ServerValue.TIMESTAMP (escritura)
    private String productoId;
    private String chatId; // Nuevo: ID del chat para referencia en la Actividad

    // Constructor vacío requerido por Firebase
    public Mensaje() {
        // Inicialización por defecto
    }

    // Constructor para cuando se lee desde Firebase (opcional, pero buena práctica)
    public Mensaje(String mensajeId, String contenido, String remitenteId, String receptorId, Object timestamp, String productoId, String chatId) {
        this.mensajeId = mensajeId;
        this.contenido = contenido;
        this.remitenteId = remitenteId;
        this.receptorId = receptorId;
        this.timestamp = timestamp;
        this.productoId = productoId;
        this.chatId = chatId;
    }

    // Método para preparar el objeto para guardarlo en Firebase (usa ServerValue.TIMESTAMP)
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("contenido", getContenido());
        result.put("remitenteId", getRemitenteId());
        result.put("receptorId", getReceptorId());
        result.put("productoId", getProductoId());
        result.put("chatId", getChatId());

        // **Clave**: Usa ServerValue.TIMESTAMP para que Firebase ponga la hora exacta
        result.put("timestamp", ServerValue.TIMESTAMP);
        return result;
    }


    // ***************************************************************
    // Getters y Setters
    // ***************************************************************

    public String getMensajeId() {
        return mensajeId;
    }

    // Corregido: setMensajeId() en lugar de setId()
    public void setMensajeId(String mensajeId) {
        this.mensajeId = mensajeId;
    }

    public void setRemitenteId(String remitenteId) {
        this.remitenteId = remitenteId;
    }

    public String getRemitenteId() {
        return remitenteId;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getReceptorId() {
        return receptorId;
    }

    public void setReceptorId(String receptorId) {
        this.receptorId = receptorId;
    }

    // El timestamp debe ser Object para que funcione la lectura/escritura con ServerValue.TIMESTAMP
    public Object getTimestamp() {
        return timestamp;
    }

    // Método auxiliar para obtener el timestamp como Long (útil al leer de Firebase)
    public long getTimestampLong() {
        if (timestamp instanceof Long) {
            return (long) timestamp;
        }
        return 0;
    }

    public void setTimestamp(Object timestamp) {
        this.timestamp = timestamp;
    }

    public String getProductoId() {
        return productoId;
    }

    public void setProductoId(String productoId) {
        this.productoId = productoId;
    }

    // Getter y Setter para chatId
    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }
}