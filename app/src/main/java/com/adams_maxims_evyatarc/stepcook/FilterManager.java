package com.adams_maxims_evyatarc.stepcook;

import android.app.Dialog;
import android.widget.Button;

/**
 * Abstract base class for filter management
 */
public abstract class FilterManager {
    protected Dialog filterDialog;
    protected Button filterButton;
    protected UIHelper uiHelper;

    public FilterManager(Button filterButton, UIHelper uiHelper) {
        this.filterButton = filterButton;
        this.uiHelper = uiHelper;
    }

    /**
     * Show the filter dialog for this filter type
     */
    public abstract void showFilterDialog();

    /**
     * Apply the filter with the given value
     */
    public abstract void applyFilter(String filterValue);

    /**
     * Close the dialog and reset UI state
     */
    protected void closeDialog() {
        if (filterDialog != null && filterDialog.isShowing()) {
            filterDialog.dismiss();
        }
        uiHelper.changeButtonColor(false, filterButton);
    }
}