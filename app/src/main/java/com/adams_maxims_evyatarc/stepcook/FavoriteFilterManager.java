package com.adams_maxims_evyatarc.stepcook;

import android.content.Context;
import android.widget.Button;
import android.widget.Toast;

/**
 * Class to manage favorite filter functionality
 */
public class FavoriteFilterManager extends FilterManager {
    private Context context;
    private boolean isActive = false;

    public FavoriteFilterManager(Context context, Button favoriteFilterButton) {
        super(favoriteFilterButton, new UIHelper(context));
        this.context = context;
    }

    /**
     * Toggle the favorite filter on/off
     */
    public void toggleFilter() {
        isActive = !isActive;
        uiHelper.changeButtonColor(isActive, filterButton);

        Toast.makeText(context,
                isActive ? "Favorites filter activated" : "Favorites filter deactivated",
                Toast.LENGTH_SHORT).show();

        applyFilter(isActive ? "active" : "inactive");
    }

    @Override
    public void showFilterDialog() {
        // No dialog for favorite filter - it's a simple toggle
    }

    @Override
    public void applyFilter(String filterValue) {
        // Implement actual filtering logic
    }

    /**
     * Get current filter state
     */
    public boolean isActive() {
        return isActive;
    }
}