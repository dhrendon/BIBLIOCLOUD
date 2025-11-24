package com.example.bibliocloud.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.bibliocloud.R;
import com.example.bibliocloud.models.Suggestion;
import java.util.List;

public class SuggestionsAdapter extends RecyclerView.Adapter<SuggestionsAdapter.SuggestionViewHolder> {

    private List<Suggestion> suggestionList;

    public SuggestionsAdapter(List<Suggestion> suggestionList) {
        this.suggestionList = suggestionList;
    }

    @NonNull
    @Override
    public SuggestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_suggestion, parent, false);
        return new SuggestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SuggestionViewHolder holder, int position) {
        Suggestion suggestion = suggestionList.get(position);
        holder.bind(suggestion);
    }

    @Override
    public int getItemCount() {
        return suggestionList.size();
    }

    public void updateSuggestions(List<Suggestion> suggestions) {
        this.suggestionList = suggestions;
        notifyDataSetChanged();
    }

    static class SuggestionViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTitle, tvAuthor, tvCategory, tvDate, tvStatus, tvComments;
        private TextView tvEdition, tvIsbn, tvYear; // 🆕 Agregado tvYear
        private ImageView ivCover; // 🆕 Imagen de portada

        public SuggestionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvComments = itemView.findViewById(R.id.tvComments);
            tvEdition = itemView.findViewById(R.id.tvEdition); // 🆕
            tvIsbn = itemView.findViewById(R.id.tvIsbn); // 🆕
            tvYear = itemView.findViewById(R.id.tvYear); // 🆕 Campo año
            ivCover = itemView.findViewById(R.id.ivCover); // 🆕
        }

        public void bind(Suggestion suggestion) {
            tvTitle.setText(suggestion.getTitle());
            tvAuthor.setText("por " + suggestion.getAuthor());
            tvCategory.setText(suggestion.getCategory());
            tvDate.setText(suggestion.getFormattedDate());
            tvStatus.setText(suggestion.getStatus());

            // 🆕 Mostrar Edición si existe
            if (suggestion.getEdition() != null && !suggestion.getEdition().isEmpty()) {
                tvEdition.setText("Edición: " + suggestion.getEdition());
                tvEdition.setVisibility(View.VISIBLE);
            } else {
                tvEdition.setVisibility(View.GONE);
            }

            // 🆕 Mostrar ISBN si existe
            if (suggestion.getIsbn() != null && !suggestion.getIsbn().isEmpty()) {
                tvIsbn.setText("ISBN: " + suggestion.getIsbn());
                tvIsbn.setVisibility(View.VISIBLE);
            } else {
                tvIsbn.setVisibility(View.GONE);
            }

            // 🆕 Mostrar Año si existe
            if (suggestion.getYear() != null && !suggestion.getYear().isEmpty()) {
                tvYear.setText("Año: " + suggestion.getYear());
                tvYear.setVisibility(View.VISIBLE);
            } else {
                tvYear.setVisibility(View.GONE);
            }

            // Mostrar comentarios si existen
            if (suggestion.getComments() != null && !suggestion.getComments().isEmpty()) {
                tvComments.setText("Comentarios: " + suggestion.getComments());
                tvComments.setVisibility(View.VISIBLE);
            } else {
                tvComments.setVisibility(View.GONE);
            }

            // 🆕 Cargar imagen con Glide si existe
            if (suggestion.getCoverImageUrl() != null && !suggestion.getCoverImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(suggestion.getCoverImageUrl())
                        .placeholder(R.drawable.ic_book_placeholder)
                        .error(R.drawable.ic_book_placeholder)
                        .centerCrop()
                        .into(ivCover);
                ivCover.setVisibility(View.VISIBLE);
            } else {
                ivCover.setVisibility(View.GONE);
            }

            // Configurar color del estado
            int statusColor = getStatusColor(suggestion.getStatus());
            tvStatus.setBackgroundColor(statusColor);
        }

        private int getStatusColor(String status) {
            switch (status) {
                case "Pendiente":
                    return ContextCompat.getColor(itemView.getContext(), R.color.orange);
                case "Aprobada":
                    return ContextCompat.getColor(itemView.getContext(), R.color.green);
                case "Rechazada":
                    return ContextCompat.getColor(itemView.getContext(), R.color.red);
                default:
                    return ContextCompat.getColor(itemView.getContext(), R.color.colorTextSecondary);
            }
        }
    }
}