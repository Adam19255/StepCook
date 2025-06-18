package com.adams_maxims_evyatarc.stepcook;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
/**
 * Class to manage difficulty filter functionality
 */
public class DifficultyFilterManager extends FilterManager {
    private Context context;
    private String selectedDifficulty = "All"; // Default option
    private boolean isActive = false;

    private final String[] DIFFICULTY_OPTIONS = {"All", "Easy", "Medium", "Hard"};


    private final MainActivity activity;
//    private final Button filterButton;

    public DifficultyFilterManager(MainActivity activity, Button filterButton, UIHelper uiHelper) {
        super(filterButton, uiHelper);
        this.activity = activity;
        this.context = activity;  // ✅ FIX: assign context properly
    }


    @Override
    public void showFilterDialog() {
        uiHelper.changeButtonColor(true, filterButton);

        if (filterDialog == null) {
            // First time: create the dialog
            filterDialog = new Dialog(context);
            filterDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            filterDialog.setContentView(R.layout.difficulty_filter_layout);

            Window window = filterDialog.getWindow();
            if (window != null) {
                window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                window.setGravity(android.view.Gravity.BOTTOM);
                window.setBackgroundDrawableResource(android.R.color.transparent);
                window.setWindowAnimations(R.style.DialogAnimation);
            }

            // Find views only once
            LinearLayout allBtn = filterDialog.findViewById(R.id.allDifficulties);
            LinearLayout easyBtn = filterDialog.findViewById(R.id.easyDifficulty);
            LinearLayout mediumBtn = filterDialog.findViewById(R.id.mediumDifficulty);
            LinearLayout hardBtn = filterDialog.findViewById(R.id.hardDifficulty);
            ImageView closeDialog = filterDialog.findViewById(R.id.closeDialog);
            ImageView closeButton = filterDialog.findViewById(R.id.closeButton);

            // Setup close buttons
            View.OnClickListener closeListener = v -> {
                filterDialog.dismiss();
                uiHelper.changeButtonColor(false, filterButton);
            };
            closeDialog.setOnClickListener(closeListener);
            closeButton.setOnClickListener(closeListener);

            // Main filter click handler
            View.OnClickListener listener = v -> {
                if (v == allBtn) {
                    selectedDifficulty = "All";
                    isActive = false;
                } else if (v == easyBtn) {
                    selectedDifficulty = "Easy";
                    isActive = true;
                } else if (v == mediumBtn) {
                    selectedDifficulty = "Medium";
                    isActive = true;
                } else if (v == hardBtn) {
                    selectedDifficulty = "Hard";
                    isActive = true;
                }

                uiHelper.highlightSelectedOption(allBtn, easyBtn, mediumBtn, hardBtn, selectedDifficulty, DIFFICULTY_OPTIONS);
                uiHelper.changeButtonColor(isActive, filterButton);
                ((MainActivity) context).applyAllFilters();
                filterDialog.dismiss();
            };

            allBtn.setOnClickListener(listener);
            easyBtn.setOnClickListener(listener);
            mediumBtn.setOnClickListener(listener);
            hardBtn.setOnClickListener(listener);
        }

        // Always update highlight before showing
        LinearLayout allBtn = filterDialog.findViewById(R.id.allDifficulties);
        LinearLayout easyBtn = filterDialog.findViewById(R.id.easyDifficulty);
        LinearLayout mediumBtn = filterDialog.findViewById(R.id.mediumDifficulty);
        LinearLayout hardBtn = filterDialog.findViewById(R.id.hardDifficulty);

        uiHelper.highlightSelectedOption(allBtn, easyBtn, mediumBtn, hardBtn, selectedDifficulty, DIFFICULTY_OPTIONS);

        filterDialog.show();
    }


    private void setupClickListeners(LinearLayout allDifficulties, LinearLayout easyDifficulty,
                                     LinearLayout mediumDifficulty, LinearLayout hardDifficulty,
                                     ImageView closeDialog, ImageView closeButton) {
        allDifficulties.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedDifficulty = DIFFICULTY_OPTIONS[0]; // "All"
                handleDifficultySelection(allDifficulties, easyDifficulty, mediumDifficulty, hardDifficulty);
                Toast.makeText(context, "All difficulties selected", Toast.LENGTH_SHORT).show();
            }
        });

        easyDifficulty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedDifficulty = DIFFICULTY_OPTIONS[1]; // "Easy"
                handleDifficultySelection(allDifficulties, easyDifficulty, mediumDifficulty, hardDifficulty);
                Toast.makeText(context, "Easy difficulty selected", Toast.LENGTH_SHORT).show();
            }
        });

        mediumDifficulty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedDifficulty = DIFFICULTY_OPTIONS[2]; // "Medium"
                handleDifficultySelection(allDifficulties, easyDifficulty, mediumDifficulty, hardDifficulty);
                Toast.makeText(context, "Medium difficulty selected", Toast.LENGTH_SHORT).show();
            }
        });

        hardDifficulty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedDifficulty = DIFFICULTY_OPTIONS[3]; // "Hard"
                handleDifficultySelection(allDifficulties, easyDifficulty, mediumDifficulty, hardDifficulty);
                Toast.makeText(context, "Hard difficulty selected", Toast.LENGTH_SHORT).show();
            }
        });

        closeDialog.setOnClickListener(v -> closeDialog());
        closeButton.setOnClickListener(v -> closeDialog());
    }

    private void handleDifficultySelection(LinearLayout allDifficulties, LinearLayout easyDifficulty,
                                           LinearLayout mediumDifficulty, LinearLayout hardDifficulty) {
        // Update the highlight
        uiHelper.highlightSelectedOption(allDifficulties, easyDifficulty, mediumDifficulty,
                hardDifficulty, selectedDifficulty, DIFFICULTY_OPTIONS);

        // Apply the filter
        applyFilter(selectedDifficulty);

        // Close dialog and reset button
        closeDialog();
    }

    public boolean isFilterActive() {
        return isActive;
    }

    public List<Recipe> applyFilter(List<Recipe> recipes) {
        if (!isActive || selectedDifficulty.equals("All")) return recipes;

        List<Recipe> filtered = new ArrayList<>();
        for (Recipe recipe : recipes) {
            if (recipe.getDifficulty().equalsIgnoreCase(selectedDifficulty)) {
                filtered.add(recipe);
            }
        }
        return filtered;
    }

    @Override
    public void applyFilter(String filterValue) {
        // Optional: Keep calling MainActivity if needed
    }

}