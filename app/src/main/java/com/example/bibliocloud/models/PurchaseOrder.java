package com.example.bibliocloud.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PurchaseOrder {
    @DocumentId
    private String id;
    private String userId;
    private String userEmail;
    private String userName;

    private List<PurchaseItem> items;
    private double subtotal;
    private double tax;
    private double total;

    private String status; // "Pendiente", "Procesando", "Enviado", "Entregado", "Cancelado"
    private String paymentMethod; // "Tarjeta", "PayPal", "Efectivo", "Transferencia"
    private boolean paid;

    // Dirección de envío
    private String shippingAddress;
    private String shippingCity;
    private String shippingState;
    private String shippingZipCode;
    private String shippingPhone;

    @ServerTimestamp
    private Date orderDate;
    private Date estimatedDelivery;
    private String trackingNumber;

    public PurchaseOrder() {
        this.items = new ArrayList<>();
        this.status = "Pendiente";
        this.paid = false;
    }

    public PurchaseOrder(String userId, String userEmail, String userName) {
        this();
        this.userId = userId;
        this.userEmail = userEmail;
        this.userName = userName;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public List<PurchaseItem> getItems() { return items; }
    public void setItems(List<PurchaseItem> items) { this.items = items; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getTax() { return tax; }
    public void setTax(double tax) { this.tax = tax; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getShippingCity() { return shippingCity; }
    public void setShippingCity(String shippingCity) { this.shippingCity = shippingCity; }

    public String getShippingState() { return shippingState; }
    public void setShippingState(String shippingState) { this.shippingState = shippingState; }

    public String getShippingZipCode() { return shippingZipCode; }
    public void setShippingZipCode(String shippingZipCode) { this.shippingZipCode = shippingZipCode; }

    public String getShippingPhone() { return shippingPhone; }
    public void setShippingPhone(String shippingPhone) { this.shippingPhone = shippingPhone; }

    public Date getOrderDate() { return orderDate; }
    public void setOrderDate(Date orderDate) { this.orderDate = orderDate; }

    public Date getEstimatedDelivery() { return estimatedDelivery; }
    public void setEstimatedDelivery(Date estimatedDelivery) { this.estimatedDelivery = estimatedDelivery; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    // Métodos útiles
    public void addItem(PurchaseItem item) {
        items.add(item);
        calculateTotals();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            calculateTotals();
        }
    }

    public void calculateTotals() {
        subtotal = 0;
        for (PurchaseItem item : items) {
            subtotal += item.getTotalPrice();
        }
        tax = subtotal * 0.16; // IVA 16%
        total = subtotal + tax;
    }

    public int getTotalItems() {
        int count = 0;
        for (PurchaseItem item : items) {
            count += item.getQuantity();
        }
        return count;
    }

    public String getFormattedOrderDate() {
        if (orderDate == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(orderDate);
    }

    public String getFormattedEstimatedDelivery() {
        if (estimatedDelivery == null) return "Por confirmar";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(estimatedDelivery);
    }

    public String getFullShippingAddress() {
        return String.format("%s, %s, %s, CP: %s",
                shippingAddress, shippingCity, shippingState, shippingZipCode);
    }

    // Clase interna para items de compra
    public static class PurchaseItem {
        private String bookId;
        private String bookTitle;
        private String bookAuthor;
        private String branchId;
        private String branchName;
        private double unitPrice;
        private int quantity;
        private String format; // "Físico" o "Digital"

        public PurchaseItem() {}

        public PurchaseItem(String bookId, String bookTitle, String bookAuthor,
                            double unitPrice, int quantity, String format) {
            this.bookId = bookId;
            this.bookTitle = bookTitle;
            this.bookAuthor = bookAuthor;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
            this.format = format;
        }

        // Getters y Setters
        public String getBookId() { return bookId; }
        public void setBookId(String bookId) { this.bookId = bookId; }

        public String getBookTitle() { return bookTitle; }
        public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

        public String getBookAuthor() { return bookAuthor; }
        public void setBookAuthor(String bookAuthor) { this.bookAuthor = bookAuthor; }

        public String getBranchId() { return branchId; }
        public void setBranchId(String branchId) { this.branchId = branchId; }

        public String getBranchName() { return branchName; }
        public void setBranchName(String branchName) { this.branchName = branchName; }

        public double getUnitPrice() { return unitPrice; }
        public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        public double getTotalPrice() {
            return unitPrice * quantity;
        }
    }
}