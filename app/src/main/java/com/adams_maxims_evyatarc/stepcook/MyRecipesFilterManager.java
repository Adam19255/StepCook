package com.adams_maxims_evyatarc.stepcook;

import android.content.Context;
import android.widget.Button;
import android.widget.Toast;

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


    /**
     * Toggle the my recipes filter on/off
     */
    public void toggleFilter() {
        isActive = !isActive;
        uiHelper.changeButtonColor(isActive, filterButton);

        Toast.makeText(context,
                isActive ? "My recipes filter activated" : "My recipes filter deactivated",
                Toast.LENGTH_SHORT).show();

        applyFilter(isActive ? "active" : "inactive");
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
    }

    @Override
    public void applyFilter(String filterValue) {
        boolean onlyMine = filterValue.equalsIgnoreCase("true");
        activity.onMyRecipesFilterToggled(onlyMine);
    }

}
