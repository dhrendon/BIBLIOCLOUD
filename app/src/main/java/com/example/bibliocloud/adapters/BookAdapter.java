package com.example.bibliocloud.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bibliocloud.R;
import com.example.bibliocloud.models.Book;
import java.util.List;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {

    private List<Book> bookList;
    private OnBookClickListener listener;

    public interface OnBookClickListener {
        void onBookClick(Book book);
        void onFavoriteClick(Book book);
        void onBookLongClick(Book book);
    }

    public BookAdapter(List<Book> bookList, OnBookClickListener listener) {
        this.bookList = bookList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_book, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = bookList.get(position);
        holder.bind(book, listener);
    }

    @Override
    public int getItemCount() {
        return bookList.size();
    }

    public void updateBooks(List<Book> books) {
        this.bookList = books;
        notifyDataSetChanged();
    }

    static class BookViewHolder extends RecyclerView.ViewHolder {
        private ImageView imgBook;
        private TextView tvTitle, tvAuthor, tvCategory, tvYear, tvStatus, tvRating;
        private ImageButton btnFavorite;

        public BookViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBook = itemView.findViewById(R.id.imgBook);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvYear = itemView.findViewById(R.id.tvYear);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvRating = itemView.findViewById(R.id.tvRating);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }

        public void bind(Book book, OnBookClickListener listener) {
            try {
                // Validar y establecer datos con valores por defecto
                tvTitle.setText(book.getTitle() != null ? book.getTitle() : "Título no disponible");
                tvAuthor.setText(book.getAuthor() != null ? book.getAuthor() : "Autor no disponible");
                tvCategory.setText(book.getCategory() != null ? book.getCategory() : "Sin categoría");

                // Validar año
                String yearText = "Año no disponible";
                if (book.getYear() != null) {
                    try {
                        yearText = String.valueOf(book.getYear());
                    } catch (Exception e) {
                        yearText = book.getYear(); // Si es String
                    }
                }
                tvYear.setText(yearText);

                // Validar estado - CRÍTICO para evitar el NullPointerException
                String status = book.getStatus();
                if (status == null || status.trim().isEmpty()) {
                    status = "Disponible"; // Valor por defecto
                    Log.w("BookAdapter", "Estado nulo encontrado, usando valor por defecto");
                }
                tvStatus.setText(status);

                // Cargar imagen del libro
                loadBookImage(book.getFotoBase64());

                // Configurar color del estado - ahora seguro
                int statusColor = getStatusColor(status);
                tvStatus.setBackgroundColor(statusColor);

                // Configurar favorito
                btnFavorite.setImageResource(book.isFavorite() ?
                        android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
                btnFavorite.setColorFilter(book.isFavorite() ?
                        ContextCompat.getColor(itemView.getContext(), R.color.colorPrimary) :
                        ContextCompat.getColor(itemView.getContext(), R.color.colorTextSecondary));

                // Mostrar valoración
                showRating(book);

                // Listeners
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onBookClick(book);
                    }
                });

                itemView.setOnLongClickListener(v -> {
                    if (listener != null) {
                        listener.onBookLongClick(book);
                    }
                    return true;
                });

                btnFavorite.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onFavoriteClick(book);
                    }
                });

            } catch (Exception e) {
                Log.e("BookAdapter", "Error en bind: " + e.getMessage(), e);
                // Mostrar datos de error
                tvTitle.setText("Error al cargar");
                tvStatus.setText("Disponible");
                tvStatus.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.green));
            }
        }

        private void showRating(Book book) {
            if (tvRating != null) {
                try {
                    if (book.getRatingCount() > 0) {
                        String ratingText = String.format("⭐ %.1f (%d)", book.getRating(), book.getRatingCount());
                        tvRating.setText(ratingText);
                        tvRating.setVisibility(View.VISIBLE);
                    } else {
                        tvRating.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    tvRating.setVisibility(View.GONE);
                    Log.e("BookAdapter", "Error mostrando rating: " + e.getMessage());
                }
            }
        }

        private void loadBookImage(String fotoBase64) {
            try {
                if (fotoBase64 != null && !fotoBase64.isEmpty()) {
                    // 🔥 SOLUCIÓN: Decodificar Base64 a Bitmap
                    byte[] decodedBytes = android.util.Base64.decode(fotoBase64, android.util.Base64.DEFAULT);
                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(
                            decodedBytes, 0, decodedBytes.length
                    );

                    if (bitmap != null) {
                        imgBook.setImageBitmap(bitmap);
                    } else {
                        // Si falla la decodificación, mostrar placeholder
                        imgBook.setImageResource(R.drawable.ic_book_placeholder);
                        Log.w("BookAdapter", "No se pudo decodificar la imagen Base64");
                    }
                } else {
                    // Sin imagen, mostrar placeholder
                    imgBook.setImageResource(R.drawable.ic_book_placeholder);
                }
            } catch (Exception e) {
                // Error al decodificar, mostrar placeholder
                imgBook.setImageResource(R.drawable.ic_book_placeholder);
                Log.e("BookAdapter", "Error cargando imagen Base64: " + e.getMessage(), e);
            }
        }

        private int getStatusColor(String status) {
            // VALIDACIÓN CRÍTICA - evitar NullPointerException
            if (status == null) {
                Log.w("BookAdapter", "Status es null en getStatusColor");
                return ContextCompat.getColor(itemView.getContext(), R.color.green);
            }

            try {
                switch (status) {
                    case "Disponible":
                        return ContextCompat.getColor(itemView.getContext(), R.color.green);
                    case "Prestado":
                        return ContextCompat.getColor(itemView.getContext(), R.color.red);
                    case "Reservado":
                        return ContextCompat.getColor(itemView.getContext(), R.color.orange);
                    default:
                        Log.w("BookAdapter", "Estado desconocido: " + status);
                        return ContextCompat.getColor(itemView.getContext(), R.color.colorTextSecondary);
                }
            } catch (Exception e) {
                Log.e("BookAdapter", "Error en getStatusColor: " + e.getMessage());
                return ContextCompat.getColor(itemView.getContext(), R.color.green);
            }
        }
    }
}