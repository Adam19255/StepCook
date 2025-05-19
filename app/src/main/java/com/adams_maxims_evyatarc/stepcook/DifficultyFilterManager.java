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

/**
 * Class to manage difficulty filter functionality
 */
public class DifficultyFilterManager extends FilterManager {
    private Context context;
    private String selectedDifficulty = "All"; // Default option
    private final String[] DIFFICULTY_OPTIONS = {"All", "Easy", "Medium", "Hard"};

    public DifficultyFilterManager(Context context, Button difficultyFilterButton) {
        super(difficultyFilterButton, new UIHelper(context));
        this.context = context;
    }

    @Override
    public void showFilterDialog() {
        uiHelper.changeButtonColor(true, filterButton);

        // If we already have a dialog, just show it if not showing
        if (filterDialog != null) {
            if (!filterDialog.isShowing()) {
                filterDialog.show();
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
                uiHelper.changeButtonColor(false, filterButton);
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

    @Override
    public void applyFilter(String filterValue) {
        // Implement actual filtering logic
    }
}