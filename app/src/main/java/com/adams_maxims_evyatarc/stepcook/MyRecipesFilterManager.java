package com.adams_maxims_evyatarc.stepcook;

import android.content.Context;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;
/**
 * Class to manage user recipes filter functionality
 */
public class MyRecipesFilterManager extends FilterManager{

    private Context context;
    private boolean isActive = false;

    private final MainActivity activity;

    public MyRecipesFilterManager(MainActivity activity, Button filterButton, UIHelper uiHelper) {
        super(filterButton, uiHelper);
        this.activity = activity;
        this.context = activity;
    }

    @Override
    public void showFilterDialog() {
        // No dialog for my recipes filter - it's a simple toggle
    }

    public boolean isFilterActive() {
        return isActive;
    }

    public void setFilterActive(boolean active) {
        this.isActive = active;
        uiHelper.changeButtonColor(isActive, filterButton);


        // ✅ Add this:
        ((MainActivity) context).applyAllFilters();
    }

    public List<Recipe> applyFilter(List<Recipe> recipes) {
        if (!isActive || activity.getCurrentUserId() == null) return recipes;

        String currentUserId = activity.getCurrentUserId();

        List<Recipe> filtered = new ArrayList<>();
        for (Recipe recipe : recipes) {
            if (recipe.getAuthorId().equals(currentUserId)) {
                filtered.add(recipe);
            }
        }
        return filtered;
    }


    @Override
    public void applyFilter(String filterValue) {
        // no implementation needed here
    }

}
