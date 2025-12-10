package com.diegodev.marketplace.model;

/**
 * Modelo para representar una entrada en la lista de chats.
 * Es el índice de la conversación que se guarda en /ListaChats/[uid]
 */
public class Conversacion {

    private String otroUserId;
    private String productoId;
    private String ultimoMensaje;
    private long timestamp;
    private String nombreCompanero; // Se llena localmente con otra consulta

    public Conversacion() {
        // Constructor vacío requerido por Firebase
    }

    // Getters y Setters
    public String getOtroUserId() {
        return otroUserId;
    }

    public void setOtroUserId(String otroUserId) {
        this.otroUserId = otroUserId;
    }

    public String getProductoId() {
        return productoId;
    }

    public void setProductoId(String productoId) {
        this.productoId = productoId;
    }

    public String getUltimoMensaje() {
        return ultimoMensaje;
    }

    public void setUltimoMensaje(String ultimoMensaje) {
        this.ultimoMensaje = ultimoMensaje;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getNombreCompanero() {
        return nombreCompanero;
    }

    public void setNombreCompanero(String nombreCompanero) {
        this.nombreCompanero = nombreCompanero;
    }
}