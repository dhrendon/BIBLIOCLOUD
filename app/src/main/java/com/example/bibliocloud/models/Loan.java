package com.example.bibliocloud.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Loan {
    @DocumentId
    private String id;

    @PropertyName("id_usuario")
    private String userId;

    @PropertyName("nombre_usuario")
    private String userName;

    @PropertyName("correo_usuario")
    private String userEmail;

    @PropertyName("id_libro")
    private String bookId;

    @PropertyName("titulo_libro")
    private String bookTitle;

    @PropertyName("autor_libro")
    private String bookAuthor;

    @PropertyName("id_inventario")
    private String inventoryId;

    @PropertyName("id_sucursal")
    private String branchId;

    @PropertyName("nombre_sucursal")
    private String branchName;

    @PropertyName("tipo")
    private String type; // "Préstamo" o "Reserva"

    @PropertyName("estado")
    private String status; // "Activo", "Devuelto", "Vencido", "Cancelado"

    @ServerTimestamp
    @PropertyName("fecha_prestamo")
    private Date loanDate;

    @PropertyName("fecha_devolucion")
    private Date returnDate;

    @PropertyName("fecha_devolucion_real")
    private Date actualReturnDate;

    @PropertyName("dias_prestamo")
    private int loanDays; // Días permitidos de préstamo (default: 14)

    @PropertyName("multa")
    private double fine; // Multa por retraso

    @PropertyName("notas")
    private String notes;

    @PropertyName("timestamp")
    private long timestamp;

    // Constructor vacío
    public Loan() {
        this.loanDate = new Date();
        this.loanDays = 14; // 14 días por defecto
        this.status = "Activo";
        this.fine = 0.0;
        this.timestamp = System.currentTimeMillis();

        // Calcular fecha de devolución (14 días después)
        Calendar cal = Calendar.getInstance();
        cal.setTime(this.loanDate);
        cal.add(Calendar.DAY_OF_YEAR, loanDays);
        this.returnDate = cal.getTime();
    }

    // Constructor completo
    public Loan(String userId, String userName, String userEmail,
                String bookId, String bookTitle, String bookAuthor,
                String inventoryId, String branchId, String branchName, String type) {
        this();
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.bookAuthor = bookAuthor;
        this.inventoryId = inventoryId;
        this.branchId = branchId;
        this.branchName = branchName;
        this.type = type;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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

    @PropertyName("id_libro")
    public String getBookId() { return bookId; }
    @PropertyName("id_libro")
    public void setBookId(String bookId) { this.bookId = bookId; }

    @PropertyName("titulo_libro")
    public String getBookTitle() { return bookTitle; }
    @PropertyName("titulo_libro")
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    @PropertyName("autor_libro")
    public String getBookAuthor() { return bookAuthor; }
    @PropertyName("autor_libro")
    public void setBookAuthor(String bookAuthor) { this.bookAuthor = bookAuthor; }

    @PropertyName("id_inventario")
    public String getInventoryId() { return inventoryId; }
    @PropertyName("id_inventario")
    public void setInventoryId(String inventoryId) { this.inventoryId = inventoryId; }

    @PropertyName("id_sucursal")
    public String getBranchId() { return branchId; }
    @PropertyName("id_sucursal")
    public void setBranchId(String branchId) { this.branchId = branchId; }

    @PropertyName("nombre_sucursal")
    public String getBranchName() { return branchName; }
    @PropertyName("nombre_sucursal")
    public void setBranchName(String branchName) { this.branchName = branchName; }

    @PropertyName("tipo")
    public String getType() { return type; }
    @PropertyName("tipo")
    public void setType(String type) { this.type = type; }

    @PropertyName("estado")
    public String getStatus() { return status; }
    @PropertyName("estado")
    public void setStatus(String status) { this.status = status; }

    @PropertyName("fecha_prestamo")
    public Date getLoanDate() { return loanDate; }
    @PropertyName("fecha_prestamo")
    public void setLoanDate(Date loanDate) { this.loanDate = loanDate; }

    @PropertyName("fecha_devolucion")
    public Date getReturnDate() { return returnDate; }
    @PropertyName("fecha_devolucion")
    public void setReturnDate(Date returnDate) { this.returnDate = returnDate; }

    @PropertyName("fecha_devolucion_real")
    public Date getActualReturnDate() { return actualReturnDate; }
    @PropertyName("fecha_devolucion_real")
    public void setActualReturnDate(Date actualReturnDate) { this.actualReturnDate = actualReturnDate; }

    @PropertyName("dias_prestamo")
    public int getLoanDays() { return loanDays; }
    @PropertyName("dias_prestamo")
    public void setLoanDays(int loanDays) { this.loanDays = loanDays; }

    @PropertyName("multa")
    public double getFine() { return fine; }
    @PropertyName("multa")
    public void setFine(double fine) { this.fine = fine; }

    @PropertyName("notas")
    public String getNotes() { return notes; }
    @PropertyName("notas")
    public void setNotes(String notes) { this.notes = notes; }

    @PropertyName("timestamp")
    public long getTimestamp() { return timestamp; }
    @PropertyName("timestamp")
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // Métodos útiles
    public String getFormattedLoanDate() {
        if (loanDate == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(loanDate);
    }

    public String getFormattedReturnDate() {
        if (returnDate == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(returnDate);
    }

    public String getFormattedActualReturnDate() {
        if (actualReturnDate == null) return "No devuelto";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(actualReturnDate);
    }

    public boolean isOverdue() {
        if (status.equals("Devuelto") || status.equals("Cancelado")) {
            return false;
        }
        Date now = new Date();
        return returnDate != null && now.after(returnDate);
    }

    public int getDaysOverdue() {
        if (!isOverdue()) return 0;

        Date now = new Date();
        long diff = now.getTime() - returnDate.getTime();
        return (int) (diff / (1000 * 60 * 60 * 24));
    }

    public void calculateFine() {
        int daysOverdue = getDaysOverdue();
        if (daysOverdue > 0) {
            fine = daysOverdue * 10.0; // $10 MXN por día de retraso
        } else {
            fine = 0.0;
        }
    }

    public int getDaysRemaining() {
        if (status.equals("Devuelto") || status.equals("Cancelado")) {
            return 0;
        }

        Date now = new Date();
        if (returnDate == null) return 0;

        long diff = returnDate.getTime() - now.getTime();
        int days = (int) (diff / (1000 * 60 * 60 * 24));
        return Math.max(0, days);
    }
}