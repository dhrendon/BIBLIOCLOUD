package com.example.bibliocloud;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.bibliocloud.models.Book;
import com.example.bibliocloud.models.BookInventory;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.*;
import java.util.*;

public class BranchBooksActivity extends AppCompatActivity {

    private String branchId, branchName;
    private Spinner spinnerBook, spinnerShelfSection;
    private EditText etShelfNumber, etShelfLevel, etPhysicalStock, etSalePrice, etRentalPrice;
    private CheckBox cbOnlineAvailable, cbForSale;
    private MaterialButton btnAddInventory, btnBack;
    private LinearLayout layoutInventoryList;
    private TextView tvBranchName, tvTotalBooks;

    private FirebaseFirestore db;
    private List<Book> availableBooks;
    private Map<String, String> bookMap; // bookTitle -> bookId

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_branch_books);

        branchId = getIntent().getStringExtra("branchId");
        branchName = getIntent().getStringExtra("branchName");

        db = FirebaseFirestore.getInstance();
        initializeViews();
        setupSpinners();
        setupListeners();
        loadBooks();
        loadInventory();
    }

    private void initializeViews() {
        tvBranchName = findViewById(R.id.tvBranchName);
        tvTotalBooks = findViewById(R.id.tvTotalBooks);
        spinnerBook = findViewById(R.id.spinnerBook);
        spinnerShelfSection = findViewById(R.id.spinnerShelfSection);
        etShelfNumber = findViewById(R.id.etShelfNumber);
        etShelfLevel = findViewById(R.id.etShelfLevel);
        etPhysicalStock = findViewById(R.id.etPhysicalStock);
        etSalePrice = findViewById(R.id.etSalePrice);
        etRentalPrice = findViewById(R.id.etRentalPrice);
        cbOnlineAvailable = findViewById(R.id.cbOnlineAvailable);
        cbForSale = findViewById(R.id.cbForSale);
        btnAddInventory = findViewById(R.id.btnAddInventory);
        btnBack = findViewById(R.id.btnBack);
        layoutInventoryList = findViewById(R.id.layoutInventoryList);

        tvBranchName.setText("📚 Inventario: " + branchName);
    }

    private void setupSpinners() {
        // Secciones de anaqueles
        String[] sections = {"Literatura", "Ciencia Ficción", "Historia",
                "Biografía", "Infantil", "Referencia", "Novela", "Poesía"};
        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, sections);
        sectionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerShelfSection.setAdapter(sectionAdapter);
    }

    private void setupListeners() {
        btnAddInventory.setOnClickListener(v -> addInventory());
        btnBack.setOnClickListener(v -> finish());

        cbForSale.setOnCheckedChangeListener((buttonView, isChecked) -> {
            etSalePrice.setEnabled(isChecked);
            if (!isChecked) etSalePrice.setText("");
        });
    }

    private void loadBooks() {
        db.collection("libros")
                .get()
                .addOnSuccessListener(snapshots -> {
                    availableBooks = new ArrayList<>();
                    bookMap = new HashMap<>();
                    List<String> bookTitles = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshots) {
                        Book book = doc.toObject(Book.class);
                        if (book != null) {
                            book.setId(doc.getId());
                            availableBooks.add(book);
                            String displayText = book.getTitle() + " - " + book.getAuthor();
                            bookTitles.add(displayText);
                            bookMap.put(displayText, book.getId());
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, bookTitles);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerBook.setAdapter(adapter);
                });
    }

    private void addInventory() {
        String selectedBookTitle = spinnerBook.getSelectedItem().toString();
        String bookId = bookMap.get(selectedBookTitle);
        String shelfNumber = etShelfNumber.getText().toString().trim();
        String shelfSection = spinnerShelfSection.getSelectedItem().toString();
        String shelfLevelStr = etShelfLevel.getText().toString().trim();
        String physicalStockStr = etPhysicalStock.getText().toString().trim();
        String salePriceStr = etSalePrice.getText().toString().trim();
        String rentalPriceStr = etRentalPrice.getText().toString().trim();

        if (!validateInputs(bookId, shelfNumber, physicalStockStr)) return;

        int shelfLevel = shelfLevelStr.isEmpty() ? 1 : Integer.parseInt(shelfLevelStr);
        int physicalStock = Integer.parseInt(physicalStockStr);
        double salePrice = salePriceStr.isEmpty() ? 0 : Double.parseDouble(salePriceStr);
        double rentalPrice = rentalPriceStr.isEmpty() ? 0 : Double.parseDouble(rentalPriceStr);

        BookInventory inventory = new BookInventory(bookId, branchId, shelfNumber);
        inventory.setBranchName(branchName);
        inventory.setShelfSection(shelfSection);
        inventory.setShelfLevel(shelfLevel);
        inventory.setPhysicalStock(physicalStock);
        inventory.setAvailablePhysical(physicalStock);
        inventory.setOnlineAvailable(cbOnlineAvailable.isChecked());
        inventory.setForSale(cbForSale.isChecked());
        inventory.setSalePrice(salePrice);
        inventory.setRentalPrice(rentalPrice);
        inventory.setCondition("Buen estado");

        db.collection("inventario")
                .add(inventory)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "✅ Libro agregado al inventario", Toast.LENGTH_SHORT).show();
                    updateBranchBookCount();
                    clearForm();
                    loadInventory();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private boolean validateInputs(String bookId, String shelfNumber, String physicalStock) {
        if (bookId == null) {
            Toast.makeText(this, "Selecciona un libro", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (shelfNumber.isEmpty()) {
            etShelfNumber.setError("Ingresa el número de anaquel");
            return false;
        }
        if (physicalStock.isEmpty()) {
            etPhysicalStock.setError("Ingresa la cantidad");
            return false;
        }
        if (cbForSale.isChecked() && etSalePrice.getText().toString().trim().isEmpty()) {
            etSalePrice.setError("Ingresa el precio de venta");
            return false;
        }
        return true;
    }

    private void loadInventory() {
        layoutInventoryList.removeAllViews();

        db.collection("inventario")
                .whereEqualTo("branchId", branchId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    List<BookInventory> inventoryList = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        BookInventory inventory = doc.toObject(BookInventory.class);
                        if (inventory != null) {
                            inventory.setId(doc.getId());
                            inventoryList.add(inventory);
                        }
                    }

                    tvTotalBooks.setText("Total de libros: " + inventoryList.size());

                    if (inventoryList.isEmpty()) {
                        TextView tvEmpty = new TextView(this);
                        tvEmpty.setText("No hay libros en el inventario");
                        tvEmpty.setPadding(16, 32, 16, 16);
                        layoutInventoryList.addView(tvEmpty);
                    } else {
                        for (BookInventory inventory : inventoryList) {
                            loadBookDetails(inventory);
                        }
                    }
                });
    }

    private void loadBookDetails(BookInventory inventory) {
        db.collection("libros").document(inventory.getBookId())
                .get()
                .addOnSuccessListener(doc -> {
                    Book book = doc.toObject(Book.class);
                    if (book != null) {
                        book.setId(doc.getId());
                        layoutInventoryList.addView(createInventoryCard(inventory, book));
                    }
                });
    }

    private CardView createInventoryCard(BookInventory inventory, Book book) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setCardElevation(4);
        card.setRadius(8);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        // Título del libro
        TextView tvTitle = new TextView(this);
        tvTitle.setText("📖 " + book.getTitle());
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvTitle);

        // Autor
        TextView tvAuthor = new TextView(this);
        tvAuthor.setText("✍️ " + book.getAuthor());
        layout.addView(tvAuthor);

        // Ubicación
        TextView tvLocation = new TextView(this);
        tvLocation.setText("📍 " + inventory.getFullShelfLocation());
        layout.addView(tvLocation);

        // Stock
        TextView tvStock = new TextView(this);
        tvStock.setText(String.format("📦 Stock: %d disponibles / %d total",
                inventory.getAvailablePhysical(), inventory.getPhysicalStock()));
        layout.addView(tvStock);

        // Disponibilidad
        TextView tvAvailability = new TextView(this);
        tvAvailability.setText("🌐 " + inventory.getAvailabilityText());
        layout.addView(tvAvailability);

        // Precio
        if (inventory.isForSale()) {
            TextView tvPrice = new TextView(this);
            tvPrice.setText(String.format("💰 Precio: $%.2f MXN", inventory.getSalePrice()));
            layout.addView(tvPrice);
        }

        // Botones
        LinearLayout buttonsLayout = new LinearLayout(this);
        buttonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonsLayout.setPadding(0, 12, 0, 0);

        Button btnEdit = new Button(this);
        btnEdit.setText("Editar");
        btnEdit.setOnClickListener(v -> showEditDialog(inventory, book));

        Button btnDelete = new Button(this);
        btnDelete.setText("Eliminar");
        btnDelete.setOnClickListener(v -> deleteInventory(inventory));

        buttonsLayout.addView(btnEdit);
        buttonsLayout.addView(btnDelete);
        layout.addView(buttonsLayout);

        card.addView(layout);
        return card;
    }

    private void showEditDialog(BookInventory inventory, Book book) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Actualizar Stock");

        EditText input = new EditText(this);
        input.setHint("Nueva cantidad disponible");
        input.setText(String.valueOf(inventory.getAvailablePhysical()));
        builder.setView(input);

        builder.setPositiveButton("Actualizar", (dialog, which) -> {
            String newStockStr = input.getText().toString().trim();
            if (!newStockStr.isEmpty()) {
                int newStock = Integer.parseInt(newStockStr);
                updateInventoryStock(inventory.getId(), newStock);
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void updateInventoryStock(String inventoryId, int newStock) {
        db.collection("inventario").document(inventoryId)
                .update("availablePhysical", newStock, "lastUpdated", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Stock actualizado", Toast.LENGTH_SHORT).show();
                    loadInventory();
                });
    }

    private void deleteInventory(BookInventory inventory) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar del inventario")
                .setMessage("¿Estás seguro de eliminar este libro del inventario?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    db.collection("inventario").document(inventory.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Libro eliminado", Toast.LENGTH_SHORT).show();
                                updateBranchBookCount();
                                loadInventory();
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void updateBranchBookCount() {
        db.collection("inventario")
                .whereEqualTo("branchId", branchId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    int count = snapshots.size();
                    db.collection("sucursales").document(branchId)
                            .update("totalBooks", count);
                });
    }

    private void clearForm() {
        etShelfNumber.setText("");
        etShelfLevel.setText("");
        etPhysicalStock.setText("");
        etSalePrice.setText("");
        etRentalPrice.setText("");
        cbOnlineAvailable.setChecked(false);
        cbForSale.setChecked(false);
        spinnerBook.setSelection(0);
        spinnerShelfSection.setSelection(0);
    }
}