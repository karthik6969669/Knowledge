package com.example.myapplicationtt.models;

public class ModelChat {
    String messageId;

    String messageType;
    String message;
    String fromUid;
    String toUid;
    long  timestamp;

    public ModelChat() {

    }

    public ModelChat(long timestamp, String toUid, String fromUid, String message, String messageType, String messageId) {
        this.timestamp = timestamp;
        this.toUid = toUid;
        this.fromUid = fromUid;
        this.message = message;
        this.messageType = messageType;
        this.messageId = messageId;
    }

    public String getToUid() {
        return toUid;
    }

    public void setToUid(String toUid) {
        this.toUid = toUid;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFromUid() {
        return fromUid;
    }

    public void setFromUid(String fromUid) {
        this.fromUid = fromUid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
