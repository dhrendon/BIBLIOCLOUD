package com.example.bibliocloud;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.bibliocloud.models.Book;
import com.example.bibliocloud.models.BookInventory;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.*;
import java.util.*;

public class CashierInventoryActivity extends AppCompatActivity {

    private EditText etSearchBook;
    private Spinner spinnerSection;
    private LinearLayout layoutInventoryList;
    private TextView tvTotalBooks, tvBranchName;
    private MaterialButton btnBack;

    private FirebaseFirestore db;
    private List<BookInventoryInfo> allInventory;
    private String branchId;
    private String branchName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cashier_inventory);

        db = FirebaseFirestore.getInstance();
        branchId = getIntent().getStringExtra("branchId");
        branchName = getIntent().getStringExtra("branchName");

        initializeViews();
        setupSectionFilter();
        setupSearchListener();
        loadInventory();
    }

    private void initializeViews() {
        etSearchBook = findViewById(R.id.etSearchBook);
        spinnerSection = findViewById(R.id.spinnerSection);
        layoutInventoryList = findViewById(R.id.layoutInventoryList);
        tvTotalBooks = findViewById(R.id.tvTotalBooks);
        tvBranchName = findViewById(R.id.tvBranchName);
        btnBack = findViewById(R.id.btnBack);

        tvBranchName.setText("📚 Inventario - " + branchName);
        btnBack.setOnClickListener(v -> finish());

        allInventory = new ArrayList<>();
    }

    private void setupSectionFilter() {
        String[] sections = {"Todas", "Literatura", "Ciencia Ficción", "Historia",
                "Biografía", "Infantil", "Referencia", "Novela", "Poesía"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, sections);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSection.setAdapter(adapter);

        spinnerSection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedSection = (String) parent.getItemAtPosition(position);
                filterBySection(selectedSection);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSearchListener() {
        etSearchBook.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchBooks(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadInventory() {
        Query query = db.collection("inventario");

        // Filtrar por sucursal si está asignado
        if (!branchId.isEmpty()) {
            query = query.whereEqualTo("branchId", branchId);
        }

        query.addSnapshotListener((snapshots, error) -> {
            if (error != null || snapshots == null) {
                Toast.makeText(this, "Error al cargar inventario", Toast.LENGTH_SHORT).show();
                return;
            }

            allInventory.clear();

            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                BookInventory inventory = doc.toObject(BookInventory.class);
                if (inventory != null) {
                    inventory.setId(doc.getId());
                    loadBookInfo(inventory);
                }
            }

            tvTotalBooks.setText("Total de libros: " + snapshots.size());
        });
    }

    private void loadBookInfo(BookInventory inventory) {
        db.collection("libros").document(inventory.getBookId())
                .get()
                .addOnSuccessListener(doc -> {
                    Book book = doc.toObject(Book.class);
                    if (book != null) {
                        book.setId(doc.getId());
                        BookInventoryInfo info = new BookInventoryInfo(book, inventory);
                        allInventory.add(info);
                        displayInventory(allInventory);
                    }
                });
    }

    private void filterBySection(String section) {
        if (section.equals("Todas")) {
            displayInventory(allInventory);
            return;
        }

        List<BookInventoryInfo> filtered = new ArrayList<>();
        for (BookInventoryInfo info : allInventory) {
            if (section.equals(info.inventory.getShelfSection())) {
                filtered.add(info);
            }
        }
        displayInventory(filtered);
    }

    private void searchBooks(String searchText) {
        if (searchText.trim().isEmpty()) {
            displayInventory(allInventory);
            return;
        }

        searchText = searchText.toLowerCase();
        List<BookInventoryInfo> filtered = new ArrayList<>();

        for (BookInventoryInfo info : allInventory) {
            if (info.book.getTitle().toLowerCase().contains(searchText) ||
                    info.book.getAuthor().toLowerCase().contains(searchText) ||
                    info.inventory.getShelfNumber().toLowerCase().contains(searchText)) {
                filtered.add(info);
            }
        }

        displayInventory(filtered);
    }

    private void displayInventory(List<BookInventoryInfo> inventory) {
        layoutInventoryList.removeAllViews();

        if (inventory.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No hay libros en el inventario");
            tvEmpty.setPadding(16, 32, 16, 16);
            tvEmpty.setTextSize(16);
            tvEmpty.setGravity(android.view.Gravity.CENTER);
            layoutInventoryList.addView(tvEmpty);
            return;
        }

        for (BookInventoryInfo info : inventory) {
            layoutInventoryList.addView(createInventoryCard(info));
        }
    }

    private CardView createInventoryCard(BookInventoryInfo info) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setCardElevation(4);
        card.setRadius(8);

        // Color según disponibilidad
        int backgroundColor = info.inventory.isAvailable() ?
                getResources().getColor(R.color.white) :
                getResources().getColor(R.color.light_brown);
        card.setCardBackgroundColor(backgroundColor);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        // Título
        TextView tvTitle = new TextView(this);
        tvTitle.setText("📖 " + info.book.getTitle());
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(getResources().getColor(R.color.colorPrimary));
        layout.addView(tvTitle);

        // Autor
        TextView tvAuthor = new TextView(this);
        tvAuthor.setText("✍️ " + info.book.getAuthor());
        tvAuthor.setTextSize(14);
        layout.addView(tvAuthor);

        // Categoría
        TextView tvCategory = new TextView(this);
        tvCategory.setText("📚 " + info.book.getCategory());
        tvCategory.setTextSize(14);
        layout.addView(tvCategory);

        // Separador
        View separator = new View(this);
        LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2);
        sepParams.setMargins(0, 12, 0, 12);
        separator.setLayoutParams(sepParams);
        separator.setBackgroundColor(getResources().getColor(R.color.colorTextSecondary));
        layout.addView(separator);

        // UBICACIÓN (destacada)
        TextView tvLocationHeader = new TextView(this);
        tvLocationHeader.setText("📍 UBICACIÓN:");
        tvLocationHeader.setTextSize(16);
        tvLocationHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        tvLocationHeader.setTextColor(getResources().getColor(R.color.colorPrimary));
        layout.addView(tvLocationHeader);

        TextView tvLocation = new TextView(this);
        tvLocation.setText(info.inventory.getFullShelfLocation());
        tvLocation.setTextSize(18);
        tvLocation.setTypeface(null, android.graphics.Typeface.BOLD);
        tvLocation.setTextColor(getResources().getColor(R.color.green));
        tvLocation.setPadding(0, 8, 0, 8);
        layout.addView(tvLocation);

        // Stock
        TextView tvStock = new TextView(this);
        tvStock.setText(String.format("📦 Stock: %d disponibles / %d total",
                info.inventory.getAvailablePhysical(),
                info.inventory.getPhysicalStock()));
        tvStock.setTextSize(14);

        if (info.inventory.getAvailablePhysical() == 0) {
            tvStock.setTextColor(getResources().getColor(R.color.red));
        } else if (info.inventory.getAvailablePhysical() <= 2) {
            tvStock.setTextColor(getResources().getColor(R.color.orange));
        } else {
            tvStock.setTextColor(getResources().getColor(R.color.green));
        }

        layout.addView(tvStock);

        // Disponibilidad
        TextView tvAvailability = new TextView(this);
        tvAvailability.setText("🌐 " + info.inventory.getAvailabilityText());
        tvAvailability.setTextSize(14);
        layout.addView(tvAvailability);

        // Precio (si está a la venta)
        if (info.inventory.isForSale()) {
            TextView tvPrice = new TextView(this);
            tvPrice.setText(String.format("💰 Precio: $%.2f MXN", info.inventory.getSalePrice()));
            tvPrice.setTextSize(16);
            tvPrice.setTypeface(null, android.graphics.Typeface.BOLD);
            tvPrice.setTextColor(getResources().getColor(R.color.colorPrimary));
            layout.addView(tvPrice);
        }

        card.addView(layout);
        return card;
    }

    // Clase auxiliar para combinar Book y BookInventory
    private static class BookInventoryInfo {
        Book book;
        BookInventory inventory;

        BookInventoryInfo(Book book, BookInventory inventory) {
            this.book = book;
            this.inventory = inventory;
        }
    }
}