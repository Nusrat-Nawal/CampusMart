package com.campusmart.app.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class User {
    private String uid;
    private String name;
    private String email;
    private String studentId;
    private String address;
    private String phoneNumber;
    private String university;
    private boolean verified;
    private @ServerTimestamp Date createdAt;

    public User() {
        // Public no-argument constructor needed for Firestore
    }

    public User(String uid, String name, String email, String studentId, String address, String phoneNumber, String university) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.studentId = studentId;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.university = university;
        this.verified = false; // Set to true after successful verification
    }

    @Exclude
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getAddress() {
        return address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getUniversity() {
        return university;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public Date getCreatedAt() {
        return createdAt;
    }
}