package com.adams_maxims_evyatarc.stepcook;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to manage cook time filter functionality
 */
public class CookTimeFilterManager extends FilterManager {
    private Context context;
    private String selectedCookTime = "All"; // Default option
    private boolean isActive = false;

    private final String[] COOK_TIME_OPTIONS = {"All", "Fast", "Medium", "Long"};

    private final MainActivity activity;

    public CookTimeFilterManager(MainActivity activity, Button filterButton, UIHelper uiHelper) {
        super(filterButton, uiHelper);
        this.activity = activity;
        this.context = activity;
    }

    @Override
    public void showFilterDialog() {
        uiHelper.changeButtonColor(true, filterButton);

        // If we already have a dialog, just show it if not showing
        if (filterDialog != null) {
            if (!filterDialog.isShowing()) {
                filterDialog.show();

                LinearLayout allBtn = filterDialog.findViewById(R.id.allCookTimes);
                LinearLayout fastBtn = filterDialog.findViewById(R.id.fastCookTime);
                LinearLayout mediumBtn = filterDialog.findViewById(R.id.mediumCookTime);
                LinearLayout longBtn = filterDialog.findViewById(R.id.longCookTime);

                View.OnClickListener listener = v -> {
                    if (v == allBtn) {
                        selectedCookTime = "All";
                        isActive = false;
                    } else if (v == fastBtn) {
                        selectedCookTime = "Fast";
                        isActive = true;
                    } else if (v == mediumBtn) {
                        selectedCookTime = "Medium";
                        isActive = true;
                    } else if (v == longBtn) {
                        selectedCookTime = "Long";
                        isActive = true;
                    }

                    uiHelper.highlightSelectedOption(
                            allBtn, fastBtn, mediumBtn, longBtn,
                            selectedCookTime, COOK_TIME_OPTIONS
                    );

                    uiHelper.changeButtonColor(isActive, filterButton);

                    ((MainActivity) context).applyAllFilters(); // Apply all filters centrally
                    filterDialog.dismiss();
                };

                allBtn.setOnClickListener(listener);
                fastBtn.setOnClickListener(listener);
                mediumBtn.setOnClickListener(listener);
                longBtn.setOnClickListener(listener);
            }
            return;
        }

        // Create a new dialog if we don't have one
        filterDialog = new Dialog(context);
        filterDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        filterDialog.setContentView(R.layout.time_filter_layout);

        // Set up dialog window properties
        Window window = filterDialog.getWindow();
        if (window != null) {
            window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            window.setGravity(android.view.Gravity.BOTTOM);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setWindowAnimations(R.style.DialogAnimation);
        }

        // Find UI elements
        LinearLayout allCookTimes = filterDialog.findViewById(R.id.allCookTimes);
        LinearLayout fastCookTime = filterDialog.findViewById(R.id.fastCookTime);
        LinearLayout mediumCookTime = filterDialog.findViewById(R.id.mediumCookTime);
        LinearLayout longCookTime = filterDialog.findViewById(R.id.longCookTime);
        ImageView closeDialog = filterDialog.findViewById(R.id.closeDialog);
        ImageView closeButton = filterDialog.findViewById(R.id.closeButton);

        // Highlight the currently selected cook time
        uiHelper.highlightSelectedOption(allCookTimes, fastCookTime, mediumCookTime,
                longCookTime, selectedCookTime, COOK_TIME_OPTIONS);

        setupClickListeners(allCookTimes, fastCookTime, mediumCookTime, longCookTime,
                closeDialog, closeButton);

        filterDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // Reset button color when dialog is dismissed by clicking outside
                uiHelper.changeButtonColor(isActive, filterButton);
            }
        });

        filterDialog.show();
    }

    private void setupClickListeners(LinearLayout allCookTimes, LinearLayout fastCookTime,
                                     LinearLayout mediumCookTime, LinearLayout longCookTime,
                                     ImageView closeDialog, ImageView closeButton) {
        allCookTimes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedCookTime = COOK_TIME_OPTIONS[0]; // "All"
                handleCookTimeSelection(allCookTimes, fastCookTime, mediumCookTime, longCookTime);
            }
        });

        fastCookTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedCookTime = COOK_TIME_OPTIONS[1]; // "Fast"
                handleCookTimeSelection(allCookTimes, fastCookTime, mediumCookTime, longCookTime);
            }
        });

        mediumCookTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedCookTime = COOK_TIME_OPTIONS[2]; // "Medium"
                handleCookTimeSelection(allCookTimes, fastCookTime, mediumCookTime, longCookTime);
            }
        });

        longCookTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedCookTime = COOK_TIME_OPTIONS[3]; // "Long"
                handleCookTimeSelection(allCookTimes, fastCookTime, mediumCookTime, longCookTime);
            }
        });

        closeDialog.setOnClickListener(v -> closeDialog());
        closeButton.setOnClickListener(v -> closeDialog());
    }

    private void handleCookTimeSelection(LinearLayout allCookTimes, LinearLayout fastCookTime,
                                         LinearLayout mediumCookTime, LinearLayout longCookTime) {
        // Update the highlight
        uiHelper.highlightSelectedOption(allCookTimes, fastCookTime, mediumCookTime,
                longCookTime, selectedCookTime, COOK_TIME_OPTIONS);

        // Mark filter as active if not "All"
        isActive = !selectedCookTime.equals("All");
        uiHelper.changeButtonColor(isActive, filterButton);

        // Call the central filter logic
        ((MainActivity) context).applyAllFilters();

        // Close dialog
        closeDialog();
    }

    public List<Recipe> applyFilter(List<Recipe> recipes) {
        if (!isActive || selectedCookTime.equals("All")) return recipes;

        List<Recipe> filtered = new ArrayList<>();
        for (Recipe recipe : recipes) {
            int cookTime = recipe.getTotalCookTimeMinutes();
            switch (selectedCookTime) {
                case "Fast":
                    if (cookTime <= 15) filtered.add(recipe);
                    break;
                case "Medium":
                    if (cookTime >= 16 && cookTime <= 45) filtered.add(recipe);
                    break;
                case "Long":
                    if (cookTime >= 46) filtered.add(recipe);
                    break;
            }
        }
        return filtered;
    }

    @Override
    public void applyFilter(String filterValue) {
        // Optional: Keep calling MainActivity if needed
    }
}