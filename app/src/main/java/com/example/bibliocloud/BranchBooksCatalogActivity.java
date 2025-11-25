package com.example.bibliocloud;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bibliocloud.models.*;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.*;
import java.util.*;
import com.google.firebase.auth.FirebaseAuth;

public class BranchBooksCatalogActivity extends AppCompatActivity {

    private Spinner spinnerBranch;
    private ChipGroup chipGroupFilters;
    private RecyclerView recyclerViewBooks;
    private TextView tvResultsCount;
    private LinearLayout layoutEmpty;

    private FirebaseFirestore db;
    private List<Branch> branches;
    private List<BookWithInventory> booksList;
    private BranchBooksAdapter adapter;
    private String selectedBranchId = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_branch_books_catalog);

        db = FirebaseFirestore.getInstance();
        initializeViews();
        setupToolbar();
        loadBranches();
        setupFilters();
    }

    private void initializeViews() {
        spinnerBranch = findViewById(R.id.spinnerBranch);
        chipGroupFilters = findViewById(R.id.chipGroupFilters);
        recyclerViewBooks = findViewById(R.id.recyclerViewBooks);
        tvResultsCount = findViewById(R.id.tvResultsCount);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        booksList = new ArrayList<>();
        adapter = new BranchBooksAdapter(booksList, this::onBookClick);
        recyclerViewBooks.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerViewBooks.setAdapter(adapter);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Catálogo por Sucursal");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadBranches() {
        db.collection("sucursales")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(snapshots -> {
                    branches = new ArrayList<>();
                    List<String> branchNames = new ArrayList<>();
                    branchNames.add("Todas las sucursales");

                    for (DocumentSnapshot doc : snapshots) {
                        Branch branch = doc.toObject(Branch.class);
                        if (branch != null) {
                            branch.setId(doc.getId());
                            branches.add(branch);
                            branchNames.add(branch.getName());
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, branchNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerBranch.setAdapter(adapter);

                    spinnerBranch.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            if (position == 0) {
                                selectedBranchId = "all";
                            } else {
                                selectedBranchId = branches.get(position - 1).getId();
                            }
                            loadBooks();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });

                    loadBooks();
                });
    }

    private void setupFilters() {
        String[] filters = {"Todos", "En venta", "Digital", "Físico", "Disponibles"};

        for (String filter : filters) {
            Chip chip = new Chip(this);
            chip.setText(filter);
            chip.setCheckable(true);
            chip.setChecked(filter.equals("Todos"));
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) applyFilter(filter);
            });
            chipGroupFilters.addView(chip);
        }
    }

    private void loadBooks() {
        Query query = db.collection("inventario");

        if (!selectedBranchId.equals("all")) {
            query = query.whereEqualTo("branchId", selectedBranchId);
        }

        query.addSnapshotListener((snapshots, error) -> {
            if (error != null || snapshots == null) {
                Toast.makeText(this, "Error al cargar libros", Toast.LENGTH_SHORT).show();
                return;
            }

            booksList.clear();

            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                BookInventory inventory = doc.toObject(BookInventory.class);
                if (inventory != null) {
                    inventory.setId(doc.getId());
                    loadBookData(inventory);
                }
            }
        });
    }

    private void loadBookData(BookInventory inventory) {
        db.collection("libros").document(inventory.getBookId())
                .get()
                .addOnSuccessListener(doc -> {
                    Book book = doc.toObject(Book.class);
                    if (book != null) {
                        book.setId(doc.getId());
                        BookWithInventory bookWithInventory = new BookWithInventory(book, inventory);
                        booksList.add(bookWithInventory);
                        adapter.notifyDataSetChanged();
                        updateResultsCount();
                    }
                });
    }

    private void applyFilter(String filter) {
        // Implementar filtros
        loadBooks();
    }

    private void updateResultsCount() {
        tvResultsCount.setText("Libros encontrados: " + booksList.size());

        if (booksList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerViewBooks.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerViewBooks.setVisibility(View.VISIBLE);
        }
    }

    private void onBookClick(BookWithInventory bookWithInventory) {
        showBookDetailsDialog(bookWithInventory);
    }

    private void showBookDetailsDialog(BookWithInventory item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_book_details, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.tvBookTitle);
        TextView tvAuthor = dialogView.findViewById(R.id.tvBookAuthor);
        TextView tvBranch = dialogView.findViewById(R.id.tvBranch);
        TextView tvLocation = dialogView.findViewById(R.id.tvLocation);
        TextView tvAvailability = dialogView.findViewById(R.id.tvAvailability);
        TextView tvPrice = dialogView.findViewById(R.id.tvPrice);
        Button btnBuy = dialogView.findViewById(R.id.btnBuy);
        Button btnReserve = dialogView.findViewById(R.id.btnReserve);

        Book book = item.getBook();
        BookInventory inventory = item.getInventory();

        tvTitle.setText(book.getTitle());
        tvAuthor.setText("por " + book.getAuthor());
        tvBranch.setText("📍 " + inventory.getBranchName());
        tvLocation.setText("🗄️ " + inventory.getFullShelfLocation());
        tvAvailability.setText(inventory.getAvailabilityText());

        if (inventory.isForSale()) {
            tvPrice.setText(String.format("💰 $%.2f MXN", inventory.getSalePrice()));
            tvPrice.setVisibility(View.VISIBLE);
            btnBuy.setVisibility(View.VISIBLE);
        } else {
            tvPrice.setVisibility(View.GONE);
            btnBuy.setVisibility(View.GONE);
        }

        AlertDialog dialog = builder.create();

        btnBuy.setOnClickListener(v -> {
            dialog.dismiss();
            proceedToPurchase(item);
        });

        btnReserve.setOnClickListener(v -> {
            dialog.dismiss();
            reserveBook(item);
        });

        dialog.show();
    }

    private void proceedToPurchase(BookWithInventory item) {
        Intent intent = new Intent(this, PurchaseActivity.class);
        intent.putExtra("bookId", item.getBook().getId());
        intent.putExtra("inventoryId", item.getInventory().getId());
        intent.putExtra("bookTitle", item.getBook().getTitle());
        intent.putExtra("bookAuthor", item.getBook().getAuthor());
        intent.putExtra("price", item.getInventory().getSalePrice());
        intent.putExtra("branchId", item.getInventory().getBranchId());
        intent.putExtra("branchName", item.getInventory().getBranchName());
        startActivity(intent);
    }

    private void reserveBook(BookWithInventory item) {
        // Validar que el usuario esté autenticado
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "⚠️ Debes iniciar sesión para solicitar un préstamo",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Abrir Activity de solicitud de préstamo
        Intent intent = new Intent(this, LoanRequestActivity.class);
        intent.putExtra("bookId", item.getBook().getId());
        intent.putExtra("inventoryId", item.getInventory().getId());
        intent.putExtra("bookTitle", item.getBook().getTitle());
        intent.putExtra("bookAuthor", item.getBook().getAuthor());
        intent.putExtra("branchId", item.getInventory().getBranchId());
        intent.putExtra("branchName", item.getInventory().getBranchName());
        intent.putExtra("availableStock", item.getInventory().getAvailablePhysical());
        startActivity(intent);
    }

    // Clase auxiliar
    static class BookWithInventory {
        private Book book;
        private BookInventory inventory;

        public BookWithInventory(Book book, BookInventory inventory) {
            this.book = book;
            this.inventory = inventory;
        }

        public Book getBook() { return book; }
        public BookInventory getInventory() { return inventory; }
    }

    // Adaptador simple
    class BranchBooksAdapter extends RecyclerView.Adapter<BranchBooksAdapter.ViewHolder> {
        private List<BookWithInventory> items;
        private OnBookClickListener listener;

        interface OnBookClickListener {
            void onBookClick(BookWithInventory item);
        }

        BranchBooksAdapter(List<BookWithInventory> items, OnBookClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_branch_book, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            BookWithInventory item = items.get(position);
            holder.bind(item, listener);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvPrice, tvBranch, tvStatus;
            CardView card;

            ViewHolder(View itemView) {
                super(itemView);
                card = itemView.findViewById(R.id.card);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvPrice = itemView.findViewById(R.id.tvPrice);
                tvBranch = itemView.findViewById(R.id.tvBranch);
                tvStatus = itemView.findViewById(R.id.tvStatus);
            }

            void bind(BookWithInventory item, OnBookClickListener listener) {
                Book book = item.getBook();
                BookInventory inventory = item.getInventory();

                tvTitle.setText(book.getTitle());
                tvBranch.setText(inventory.getBranchName());
                tvStatus.setText(inventory.isAvailable() ? "✅ Disponible" : "❌ No disponible");

                if (inventory.isForSale()) {
                    tvPrice.setText(String.format("$%.2f", inventory.getSalePrice()));
                    tvPrice.setVisibility(View.VISIBLE);
                } else {
                    tvPrice.setVisibility(View.GONE);
                }

                card.setOnClickListener(v -> listener.onBookClick(item));
            }
        }
    }
}