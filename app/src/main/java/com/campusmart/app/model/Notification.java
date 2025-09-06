package com.campusmart.app.model;

import com.google.firebase.Timestamp;

public class Notification {
    private String id; // Firestore document ID
    private String message;
    private Timestamp timestamp;
    private boolean isRead;
    private String notifiedUserId;
    private String triggeringPostId;
    private String wishId;

    // Required empty public constructor for Firestore deserialization
    public Notification() {}

    public Notification(String id, String message, Timestamp timestamp, boolean isRead, String notifiedUserId, String triggeringPostId, String wishId) {
        this.id = id;
        this.message = message;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.notifiedUserId = notifiedUserId;
        this.triggeringPostId = triggeringPostId;
        this.wishId = wishId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public String getNotifiedUserId() {
        return notifiedUserId;
    }

    public void setNotifiedUserId(String notifiedUserId) {
        this.notifiedUserId = notifiedUserId;
    }

    public String getTriggeringPostId() {
        return triggeringPostId;
    }

    public void setTriggeringPostId(String triggeringPostId) {
        this.triggeringPostId = triggeringPostId;
    }

    public String getWishId() {
        return wishId;
    }

    public void setWishId(String wishId) {
        this.wishId = wishId;
    }
}
