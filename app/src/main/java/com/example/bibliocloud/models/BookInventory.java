package com.example.bibliocloud.models;

import com.google.firebase.firestore.DocumentId;

public class BookInventory {
    @DocumentId
    private String id;
    private String bookId;           // ID del libro en la colección "libros"
    private String branchId;         // ID de la sucursal
    private String branchName;       // Nombre de la sucursal

    // Ubicación física
    private String shelfNumber;      // Número de anaquel (ej: "A-01")
    private String shelfSection;     // Sección (ej: "Literatura", "Ficción")
    private int shelfLevel;          // Nivel del anaquel (1-5)

    // Disponibilidad
    private int physicalStock;       // Copias físicas totales
    private int availablePhysical;   // Copias físicas disponibles
    private boolean onlineAvailable; // Disponible en formato digital
    private boolean forSale;         // Disponible para compra

    // Precio
    private double salePrice;        // Precio de venta
    private double rentalPrice;      // Precio de préstamo (si aplica)

    // Estado
    private String condition;        // "Nuevo", "Buen estado", "Regular"
    private boolean featured;        // Destacado en sucursal
    private long lastUpdated;

    public BookInventory() {
        this.lastUpdated = System.currentTimeMillis();
    }

    public BookInventory(String bookId, String branchId, String shelfNumber) {
        this.bookId = bookId;
        this.branchId = branchId;
        this.shelfNumber = shelfNumber;
        this.physicalStock = 0;
        this.availablePhysical = 0;
        this.onlineAvailable = false;
        this.forSale = false;
        this.salePrice = 0.0;
        this.rentalPrice = 0.0;
        this.condition = "Buen estado";
        this.featured = false;
        this.lastUpdated = System.currentTimeMillis();
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public String getBranchId() { return branchId; }
    public void setBranchId(String branchId) { this.branchId = branchId; }

    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }

    public String getShelfNumber() { return shelfNumber; }
    public void setShelfNumber(String shelfNumber) { this.shelfNumber = shelfNumber; }

    public String getShelfSection() { return shelfSection; }
    public void setShelfSection(String shelfSection) { this.shelfSection = shelfSection; }

    public int getShelfLevel() { return shelfLevel; }
    public void setShelfLevel(int shelfLevel) { this.shelfLevel = shelfLevel; }

    public int getPhysicalStock() { return physicalStock; }
    public void setPhysicalStock(int physicalStock) {
        this.physicalStock = physicalStock;
        this.lastUpdated = System.currentTimeMillis();
    }

    public int getAvailablePhysical() { return availablePhysical; }
    public void setAvailablePhysical(int availablePhysical) {
        this.availablePhysical = availablePhysical;
        this.lastUpdated = System.currentTimeMillis();
    }

    public boolean isOnlineAvailable() { return onlineAvailable; }
    public void setOnlineAvailable(boolean onlineAvailable) {
        this.onlineAvailable = onlineAvailable;
    }

    public boolean isForSale() { return forSale; }
    public void setForSale(boolean forSale) { this.forSale = forSale; }

    public double getSalePrice() { return salePrice; }
    public void setSalePrice(double salePrice) { this.salePrice = salePrice; }

    public double getRentalPrice() { return rentalPrice; }
    public void setRentalPrice(double rentalPrice) { this.rentalPrice = rentalPrice; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

    // Métodos útiles
    public boolean isAvailable() {
        return availablePhysical > 0 || onlineAvailable;
    }

    public String getAvailabilityText() {
        if (availablePhysical > 0 && onlineAvailable) {
            return "Físico y Digital";
        } else if (availablePhysical > 0) {
            return "Físico (" + availablePhysical + " disponibles)";
        } else if (onlineAvailable) {
            return "Digital";
        } else {
            return "No disponible";
        }
    }

    public String getFullShelfLocation() {
        StringBuilder location = new StringBuilder();
        if (shelfNumber != null) location.append("Anaquel: ").append(shelfNumber);
        if (shelfSection != null) location.append(" - ").append(shelfSection);
        if (shelfLevel > 0) location.append(" - Nivel ").append(shelfLevel);
        return location.toString();
    }

    public void decreaseStock() {
        if (availablePhysical > 0) {
            availablePhysical--;
            lastUpdated = System.currentTimeMillis();
        }
    }

    public void increaseStock() {
        availablePhysical++;
        if (availablePhysical > physicalStock) {
            physicalStock = availablePhysical;
        }
        lastUpdated = System.currentTimeMillis();
    }
}