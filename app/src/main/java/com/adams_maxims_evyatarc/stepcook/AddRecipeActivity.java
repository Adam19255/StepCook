package com.adams_maxims_evyatarc.stepcook;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class AddRecipeActivity extends AppCompatActivity implements TimerDialog.OnTimeSetListener, DialogManager.ImageDeleteListener {

    private ImageView backButton;
    private LinearLayout stepsContainer;
    private ImageView addStepButton;
    private ImageButton recipeImageButton;
    private Button saveRecipeButton;
    private EditText recipeTitleEditText;
    private TextView cookTimeText;
    private AutoCompleteTextView difficultySelect;
    private String selectedDifficulty;
    private Bitmap capturedImage = null;
    private Uri selectedImageUri = null;
    private FrameLayout loadingLayout;

    private DialogManager dialogManager;
    private UIHelper uiHelper;
    private RecipeManager recipeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Force Light Mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);

        uiHelper = new UIHelper(this);
        dialogManager = new DialogManager(this);
        dialogManager.setImageDeleteListener(this);
        recipeManager = RecipeManager.getInstance();

        // Initialize views
        backButton = findViewById(R.id.backButton);
        stepsContainer = findViewById(R.id.stepsContainer);
        addStepButton = findViewById(R.id.addStepButton);
        recipeImageButton = findViewById(R.id.recipeImageButton);
        recipeImageButton.setAdjustViewBounds(true);
        recipeImageButton.setScaleType(ImageView.ScaleType.CENTER_CROP);
        recipeImageButton.setBackgroundResource(R.drawable.rounded_image_background);
        saveRecipeButton = findViewById(R.id.saveRecipeButton);
        recipeTitleEditText = findViewById(R.id.recipeTitle);
        cookTimeText = findViewById(R.id.cookTime);
        loadingLayout = findViewById(R.id.loadingLayout);

        backButton.setOnClickListener(view -> finish());

        addStepButton.setOnClickListener(view -> addNewStep());

        recipeImageButton.setOnClickListener(v -> dialogManager.showImageSourceDialog());

        saveRecipeButton.setOnClickListener(view -> saveRecipe());

        setupDifficultyDropdown();

        addNewStep();

        uiHelper.updateTotalCookTime(stepsContainer, cookTimeText);
    }

    @Override
    public void onImageDeleted() {
        // Clear the selected image
        capturedImage = null;
        selectedImageUri = null;

        // Reset the image button to default state
        recipeImageButton.setImageResource(R.drawable.image_placeholder);
        recipeImageButton.setBackgroundResource(R.drawable.rounded_image_background);

        // Show confirmation
        Toast.makeText(this, "Image deleted", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Handle image selection results
        if (resultCode == RESULT_OK) {
            if (requestCode == DialogManager.REQUEST_IMAGE_CAPTURE) {
                // For camera capture - get the thumbnail bitmap
                if (data != null && data.getExtras() != null) {
                    capturedImage = (Bitmap) data.getExtras().get("data");
                    if (capturedImage != null) {
                        recipeImageButton.setImageBitmap(capturedImage);
                        selectedImageUri = null; // Clear any existing URI
                        // Change background to transparent after image is set
                        recipeImageButton.setBackgroundResource(android.R.color.transparent);
                    }
                }
            } else if (requestCode == DialogManager.REQUEST_PICK_IMAGE) {
                // For gallery selection
                if (data != null && data.getData() != null) {
                    selectedImageUri = data.getData();
                    capturedImage = null; // Clear any existing bitmap
                    recipeImageButton.setImageURI(selectedImageUri);
                    // Change background to transparent after image is set
                    recipeImageButton.setBackgroundResource(android.R.color.transparent);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == DialogManager.REQUEST_CAMERA_PERMISSION) {
            // If request is cancelled, the result arrays are empty
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, launch camera
                dialogManager.launchCamera();
            } else {
                // Permission denied, show a message
                Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == DialogManager.REQUEST_STORAGE_PERMISSION) {
            // Check if storage permission was granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, open gallery
                dialogManager.openGallery();
            } else {
                // Permission denied, show a message
                Toast.makeText(this, "Storage permission is required to select images", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupDifficultyDropdown() {
        difficultySelect = findViewById(R.id.difficultySelect);
        String[] difficulties = getResources().getStringArray(R.array.difficulties);

        // Create an adapter with the predefined dropdown layout and custom background
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                R.layout.dropdown_item,
                difficulties
        );

        // Set the adapter to the AutoCompleteTextView
        difficultySelect.setAdapter(adapter);

        // Set dropdown background color programmatically as a backup
        difficultySelect.setDropDownBackgroundResource(android.R.color.white);

        difficultySelect.setOnItemClickListener((parent, view, position, id) -> {
            selectedDifficulty = difficulties[position];
            difficultySelect.setError(null);
        });

        difficultySelect.setOnClickListener(v -> difficultySelect.setError(null));
    }

    private void showLoading() {
        loadingLayout.setVisibility(View.VISIBLE);
        saveRecipeButton.setEnabled(false);
        addStepButton.setEnabled(false);
    }

    private void hideLoading() {
        loadingLayout.setVisibility(View.GONE);
        saveRecipeButton.setEnabled(true);
        addStepButton.setEnabled(true);
    }

    private void saveRecipe() {
        String title = recipeTitleEditText.getText().toString().trim();
        selectedDifficulty = difficultySelect.getText().toString().trim();

        if (title.isEmpty()) {
            recipeTitleEditText.setError("Please enter a recipe title");
            recipeTitleEditText.requestFocus();
            return;
        }

        if (selectedDifficulty.isEmpty()) {
            difficultySelect.setError("Please select a difficulty level");
            difficultySelect.requestFocus();
            return;
        }

        // Check if we have at least one step
        if (!uiHelper.validateSteps(stepsContainer)) {
            return;
        }

        // Show loading indicator
        showLoading();

        // Calculate total cook time in minutes
        int totalCookTimeMinutes = uiHelper.calculateTotalTime(stepsContainer);

        // If we have a bitmap from camera but no URI, we need to save it to get a URI
        if (capturedImage != null && selectedImageUri == null) {
            // Convert bitmap to URI by saving it to MediaStore
            selectedImageUri = saveBitmapToMediaStore(capturedImage);
        }

        // Use RecipeManager to save the recipe
        recipeManager.saveRecipe(
                title,
                selectedDifficulty,
                totalCookTimeMinutes,
                stepsContainer,
                selectedImageUri,
                new RecipeManager.RecipeOperationCallback() {
                    @Override
                    public void onSuccess(String recipeId) {
                        // Hide loading indicator
                        runOnUiThread(() -> {
                            hideLoading();

                            // Show success message
                            Toast.makeText(AddRecipeActivity.this,
                                    "Recipe saved successfully!", Toast.LENGTH_SHORT).show();

                            // Return to previous screen
                            finish();
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        // Hide loading indicator
                        runOnUiThread(() -> {
                            hideLoading();

                            // Show error message
                            Toast.makeText(AddRecipeActivity.this,
                                    "Error saving recipe: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                }
        );
    }

    private Uri saveBitmapToMediaStore(Bitmap bitmap) {
        try {
            // Create a filename for the image
            String timestamp = String.valueOf(System.currentTimeMillis());
            String imageFileName = "JPEG_" + timestamp + ".jpg";

            // Save bitmap to MediaStore - this returns a String URI
            String stringUri = MediaStore.Images.Media.insertImage(
                    getContentResolver(),
                    bitmap,
                    imageFileName,
                    "Image captured for recipe");

            // Convert the String URI to a Uri object
            return stringUri != null ? Uri.parse(stringUri) : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void clickTimer(View view) {
        // Find the parent view (step_field LinearLayout)
        View stepView = (View) view.getParent();

        // Show the timer dialog
        TimerDialog dialog = new TimerDialog(this, stepView, this);
        dialog.show();
    }

    @Override
    public void onTimeSet(int hours, int minutes, View stepView) {
        // Only process if there's actually a time set (hours + minutes > 0)
        if (hours == 0 && minutes == 0) {
            // If user set both to zero, clear the timer
            uiHelper.clearTimer(stepView, this::clickTimer);

            uiHelper.updateTotalCookTime(stepsContainer, cookTimeText);
            return;
        }

        // Format the time string
        String timeText = uiHelper.formatTime(0, hours, minutes);

        // Find the timer icon in the step view and update its state
        ImageView timerIcon = stepView.findViewById(R.id.timerIcon);

        // Add a tag to the timer icon to store the time
        timerIcon.setTag(hours * 60 + minutes); // Store total minutes as a tag;

        // Add or update the time label
        uiHelper.addTimeLabel(stepView, timeText);

        uiHelper.updateTotalCookTime(stepsContainer, cookTimeText);
    }

    private void addNewStep() {
        View.OnClickListener removeStepListener = v -> {
            View stepView = (View) v.getParent();

            // Remove the step view
            stepsContainer.removeView(stepView);

            // Update all remaining step numbers
            uiHelper.updateStepNumbers(stepsContainer);

            uiHelper.updateTotalCookTime(stepsContainer, cookTimeText);
        };

        View stepView = uiHelper.addNewStep(stepsContainer, removeStepListener);

        ImageView timerIcon = stepView.findViewById(R.id.timerIcon);
        timerIcon.setOnClickListener(this::clickTimer);
    }
}