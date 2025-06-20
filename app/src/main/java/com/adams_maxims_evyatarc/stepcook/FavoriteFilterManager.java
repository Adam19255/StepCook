package com.adams_maxims_evyatarc.stepcook;

import android.content.Context;
import android.widget.Button;

/**
 * Class to manage favorite filter functionality
 */
public class FavoriteFilterManager extends FilterManager {
    private Context context;
    private boolean isActive = false;
    private MainActivity activity;


    public FavoriteFilterManager(MainActivity activity, Button filterButton, UIHelper uiHelper) {
        super(filterButton, uiHelper);
        this.activity = activity;
        this.context = activity;
    }


    /**
     * Toggle the favorite filter on/off
     */
    public void toggleFilter() {
        isActive = !isActive;
        uiHelper.changeButtonColor(isActive, filterButton);

        ((MainActivity) context).applyAllFilters(); // Call centralized filtering
    }


    @Override
    public void showFilterDialog() {
        // No dialog for favorite filter - it's a simple toggle
    }

    public boolean isFilterActive() {
        return isActive;
    }

    @Override
    public void applyFilter(String filterValue) {
        boolean onlyFavs = filterValue.equalsIgnoreCase("active");

        activity.onFavoriteFilterToggled(onlyFavs);
    }
}