package com.example.bibliocloud.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Payment {

    @DocumentId
    private String id;

    @PropertyName("id_orden")
    private String orderId;

    @PropertyName("id_usuario")
    private String userId;

    @PropertyName("nombre_usuario")
    private String userName;

    @PropertyName("correo_usuario")
    private String userEmail;

    @PropertyName("id_cajero")
    private String cashierId;

    @PropertyName("nombre_cajero")
    private String cashierName;

    @PropertyName("id_sucursal")
    private String branchId;

    @PropertyName("nombre_sucursal")
    private String branchName;

    @PropertyName("numero_ticket")
    private String ticketNumber;

    @PropertyName("metodo_pago")
    private String paymentMethod;

    @PropertyName("monto")
    private double amount;

    @PropertyName("subtotal")
    private double subtotal;

    @PropertyName("iva")
    private double tax;

    @PropertyName("libros")
    private String bookTitles;

    @ServerTimestamp
    @PropertyName("fecha_pago")
    private Date paymentDate;

    @PropertyName("estado")
    private String status;

    @PropertyName("timestamp")
    private long timestamp;

    // 🔥 CAMPOS FORMATEADOS (guardados en Firestore para consultas)
    @PropertyName("formattedDate")
    private String formattedDate;

    @PropertyName("formattedTime")
    private String formattedTime;

    @PropertyName("formattedDateTime")
    private String formattedDateTime;

    // Constructor vacío
    public Payment() {
        this.paymentDate = new Date();
        this.status = "Completado";
        this.timestamp = System.currentTimeMillis();
        this.ticketNumber = "BCL" + System.currentTimeMillis();

        // Inicializar campos formateados
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        this.formattedDate = dateFormat.format(this.paymentDate);
        this.formattedTime = timeFormat.format(this.paymentDate);
        this.formattedDateTime = this.formattedDate + " " + this.formattedTime;
    }

    // Constructor completo (13 parámetros)
    public Payment(String orderId, String userId, String userName, String userEmail,
                   String cashierId, String cashierName, String branchId, String branchName,
                   String paymentMethod, double amount, double subtotal, double tax, String bookTitles) {
        this.orderId = orderId;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.cashierId = cashierId;
        this.cashierName = cashierName;
        this.branchId = branchId;
        this.branchName = branchName;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.subtotal = subtotal;
        this.tax = tax;
        this.bookTitles = bookTitles;
        this.paymentDate = new Date();
        this.status = "Completado";
        this.timestamp = System.currentTimeMillis();
        this.ticketNumber = "BCL" + System.currentTimeMillis();

        // Inicializar campos formateados
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        this.formattedDate = dateFormat.format(this.paymentDate);
        this.formattedTime = timeFormat.format(this.paymentDate);
        this.formattedDateTime = this.formattedDate + " " + this.formattedTime;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @PropertyName("id_orden")
    public String getOrderId() { return orderId; }
    @PropertyName("id_orden")
    public void setOrderId(String orderId) { this.orderId = orderId; }

    @PropertyName("id_usuario")
    public String getUserId() { return userId; }
    @PropertyName("id_usuario")
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("nombre_usuario")
    public String getUserName() { return userName; }
    @PropertyName("nombre_usuario")
    public void setUserName(String userName) { this.userName = userName; }

    @PropertyName("correo_usuario")
    public String getUserEmail() { return userEmail; }
    @PropertyName("correo_usuario")
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    @PropertyName("id_cajero")
    public String getCashierId() { return cashierId; }
    @PropertyName("id_cajero")
    public void setCashierId(String cashierId) { this.cashierId = cashierId; }

    @PropertyName("nombre_cajero")
    public String getCashierName() { return cashierName; }
    @PropertyName("nombre_cajero")
    public void setCashierName(String cashierName) { this.cashierName = cashierName; }

    @PropertyName("id_sucursal")
    public String getBranchId() { return branchId; }
    @PropertyName("id_sucursal")
    public void setBranchId(String branchId) { this.branchId = branchId; }

    @PropertyName("nombre_sucursal")
    public String getBranchName() { return branchName; }
    @PropertyName("nombre_sucursal")
    public void setBranchName(String branchName) { this.branchName = branchName; }

    @PropertyName("numero_ticket")
    public String getTicketNumber() { return ticketNumber; }
    @PropertyName("numero_ticket")
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }

    @PropertyName("metodo_pago")
    public String getPaymentMethod() { return paymentMethod; }
    @PropertyName("metodo_pago")
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    @PropertyName("monto")
    public double getAmount() { return amount; }
    @PropertyName("monto")
    public void setAmount(double amount) { this.amount = amount; }

    @PropertyName("subtotal")
    public double getSubtotal() { return subtotal; }
    @PropertyName("subtotal")
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    @PropertyName("iva")
    public double getTax() { return tax; }
    @PropertyName("iva")
    public void setTax(double tax) { this.tax = tax; }

    @PropertyName("libros")
    public String getBookTitles() { return bookTitles; }
    @PropertyName("libros")
    public void setBookTitles(String bookTitles) { this.bookTitles = bookTitles; }

    @PropertyName("fecha_pago")
    public Date getPaymentDate() { return paymentDate; }
    @PropertyName("fecha_pago")
    public void setPaymentDate(Date paymentDate) { this.paymentDate = paymentDate; }

    @PropertyName("estado")
    public String getStatus() { return status; }
    @PropertyName("estado")
    public void setStatus(String status) { this.status = status; }

    @PropertyName("timestamp")
    public long getTimestamp() { return timestamp; }
    @PropertyName("timestamp")
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // Getters y Setters para campos formateados
    @PropertyName("formattedDate")
    public String getFormattedDate() {
        if (formattedDate != null) return formattedDate;
        if (paymentDate == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(paymentDate);
    }

    @PropertyName("formattedDate")
    public void setFormattedDate(String formattedDate) {
        this.formattedDate = formattedDate;
    }

    @PropertyName("formattedTime")
    public String getFormattedTime() {
        if (formattedTime != null) return formattedTime;
        if (paymentDate == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(paymentDate);
    }

    @PropertyName("formattedTime")
    public void setFormattedTime(String formattedTime) {
        this.formattedTime = formattedTime;
    }

    @PropertyName("formattedDateTime")
    public String getFormattedDateTime() {
        if (formattedDateTime != null) return formattedDateTime;
        if (paymentDate == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(paymentDate);
    }

    @PropertyName("formattedDateTime")
    public void setFormattedDateTime(String formattedDateTime) {
        this.formattedDateTime = formattedDateTime;
    }
}