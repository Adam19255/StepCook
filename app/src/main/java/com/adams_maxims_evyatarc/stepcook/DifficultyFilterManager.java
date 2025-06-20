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
 * Class to manage difficulty filter functionality
 */
public class DifficultyFilterManager extends FilterManager {
    private Context context;
    private String selectedDifficulty = "All"; // Default option
    private boolean isActive = false;

    private final String[] DIFFICULTY_OPTIONS = {"All", "Easy", "Medium", "Hard"};

    private final MainActivity activity;

    public DifficultyFilterManager(MainActivity activity, Button filterButton, UIHelper uiHelper) {
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

                LinearLayout allBtn = filterDialog.findViewById(R.id.allDifficulties);
                LinearLayout easyBtn = filterDialog.findViewById(R.id.easyDifficulty);
                LinearLayout mediumBtn = filterDialog.findViewById(R.id.mediumDifficulty);
                LinearLayout hardBtn = filterDialog.findViewById(R.id.hardDifficulty);

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

                    uiHelper.highlightSelectedOption(
                            allBtn, easyBtn, mediumBtn, hardBtn,
                            selectedDifficulty, DIFFICULTY_OPTIONS
                    );

                    uiHelper.changeButtonColor(isActive, filterButton);

                    ((MainActivity) context).applyAllFilters(); // ✅ Apply all filters centrally
                    filterDialog.dismiss();
                };

                allBtn.setOnClickListener(listener);
                easyBtn.setOnClickListener(listener);
                mediumBtn.setOnClickListener(listener);
                hardBtn.setOnClickListener(listener);
            }
            return;
        }

        // Create a new dialog if we don't have one
        filterDialog = new Dialog(context);
        filterDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        filterDialog.setContentView(R.layout.difficulty_filter_layout);

        // Set up dialog window properties
        Window window = filterDialog.getWindow();
        if (window != null) {
            window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            window.setGravity(android.view.Gravity.BOTTOM);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setWindowAnimations(R.style.DialogAnimation);
        }

        // Find UI elements
        LinearLayout allDifficulties = filterDialog.findViewById(R.id.allDifficulties);
        LinearLayout easyDifficulty = filterDialog.findViewById(R.id.easyDifficulty);
        LinearLayout mediumDifficulty = filterDialog.findViewById(R.id.mediumDifficulty);
        LinearLayout hardDifficulty = filterDialog.findViewById(R.id.hardDifficulty);
        ImageView closeDialog = filterDialog.findViewById(R.id.closeDialog);
        ImageView closeButton = filterDialog.findViewById(R.id.closeButton);

        // Highlight the currently selected difficulty
        uiHelper.highlightSelectedOption(allDifficulties, easyDifficulty, mediumDifficulty,
                hardDifficulty, selectedDifficulty, DIFFICULTY_OPTIONS);

        setupClickListeners(allDifficulties, easyDifficulty, mediumDifficulty, hardDifficulty,
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

    private void setupClickListeners(LinearLayout allDifficulties, LinearLayout easyDifficulty,
                                     LinearLayout mediumDifficulty, LinearLayout hardDifficulty,
                                     ImageView closeDialog, ImageView closeButton) {
        allDifficulties.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedDifficulty = DIFFICULTY_OPTIONS[0]; // "All"
                handleDifficultySelection(allDifficulties, easyDifficulty, mediumDifficulty, hardDifficulty);
            }
        });

        easyDifficulty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedDifficulty = DIFFICULTY_OPTIONS[1]; // "Easy"
                handleDifficultySelection(allDifficulties, easyDifficulty, mediumDifficulty, hardDifficulty);
            }
        });

        mediumDifficulty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedDifficulty = DIFFICULTY_OPTIONS[2]; // "Medium"
                handleDifficultySelection(allDifficulties, easyDifficulty, mediumDifficulty, hardDifficulty);
            }
        });

        hardDifficulty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedDifficulty = DIFFICULTY_OPTIONS[3]; // "Hard"
                handleDifficultySelection(allDifficulties, easyDifficulty, mediumDifficulty, hardDifficulty);
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

        // ✅ Mark filter as active if not "All"
        isActive = !selectedDifficulty.equals("All");
        uiHelper.changeButtonColor(isActive, filterButton);

        // ✅ Call the central filter logic
        ((MainActivity) context).applyAllFilters();

        // Close dialog
        closeDialog();
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