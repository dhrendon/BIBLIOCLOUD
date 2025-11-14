package com.example.bibliocloud.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Branch {
    @DocumentId
    private String id;
    private String name;
    private String address;
    private String phone;
    private String schedule;
    private GeoPoint location; // Coordenadas lat/lng
    private boolean active;
    private String managerName;
    private String managerEmail;
    private int totalBooks;
    private int totalShelves;

    @ServerTimestamp
    private Date createdAt;

    public Branch() {}

    public Branch(String name, String address, String phone, String schedule,
                  double latitude, double longitude) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.schedule = schedule;
        this.location = new GeoPoint(latitude, longitude);
        this.active = true;
        this.totalBooks = 0;
        this.totalShelves = 0;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getSchedule() { return schedule; }
    public void setSchedule(String schedule) { this.schedule = schedule; }

    public GeoPoint getLocation() { return location; }
    public void setLocation(GeoPoint location) { this.location = location; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }

    public String getManagerEmail() { return managerEmail; }
    public void setManagerEmail(String managerEmail) { this.managerEmail = managerEmail; }

    public int getTotalBooks() { return totalBooks; }
    public void setTotalBooks(int totalBooks) { this.totalBooks = totalBooks; }

    public int getTotalShelves() { return totalShelves; }
    public void setTotalShelves(int totalShelves) { this.totalShelves = totalShelves; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public double getLatitude() {
        return location != null ? location.getLatitude() : 0.0;
    }

    public double getLongitude() {
        return location != null ? location.getLongitude() : 0.0;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("address", address);
        map.put("phone", phone);
        map.put("schedule", schedule);
        map.put("location", location);
        map.put("active", active);
        map.put("managerName", managerName);
        map.put("managerEmail", managerEmail);
        map.put("totalBooks", totalBooks);
        map.put("totalShelves", totalShelves);
        return map;
    }
}