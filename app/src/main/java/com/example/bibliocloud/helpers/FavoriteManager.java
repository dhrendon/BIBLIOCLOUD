package com.example.bibliocloud.helpers;

import android.content.Context;
import android.content.SharedPreferences;

public class FavoriteManager {
    private static final String PREF_NAME = "Favorites";
    private SharedPreferences prefs;

    public FavoriteManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void setFavorite(String bookId, boolean isFavorite) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(bookId, isFavorite);
        editor.apply();
    }

    public boolean isFavorite(String bookId) {
        return prefs.getBoolean(bookId, false);
    }

    public void clearAllFavorites() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }
}