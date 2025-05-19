package com.adams_maxims_evyatarc.stepcook;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * Activity to display the details of a selected recipe
 */
public class RecipeDetailActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String recipeId;
    private Recipe currentRecipe;

    private ImageView recipeImage;
    private TextView recipeTitle;
    private TextView recipeDifficulty;
    private TextView recipeCookTime;
    private TextView recipeAuthor;
    private TextView recipeDate;
    private LinearLayout stepsContainer;
    private ImageView backButton;
    private ImageView favoriteButton;
    private ImageView playButton;

    private boolean isActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        db = FirebaseFirestore.getInstance();

        // Get recipe ID from intent
        recipeId = getIntent().getStringExtra("RECIPE_ID");
        if (recipeId == null) {
            Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupListeners();
        loadRecipeDetails();
    }

    private void initializeViews() {
        recipeImage = findViewById(R.id.recipeImage);
        recipeTitle = findViewById(R.id.recipeTitle);
        recipeDifficulty = findViewById(R.id.recipeDifficulty);
        recipeCookTime = findViewById(R.id.recipeCookTime);
        recipeAuthor = findViewById(R.id.recipeAuthor);
        recipeDate = findViewById(R.id.recipeDate);
        stepsContainer = findViewById(R.id.stepsContainer);
        backButton = findViewById(R.id.backButton);
        favoriteButton = findViewById(R.id.favoriteButton);
        playButton = findViewById(R.id.playButton);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        favoriteButton.setOnClickListener(v -> {
            isActive = !isActive;
            toggleFavorite(isActive);
        });

        playButton.setOnClickListener(v -> Toast.makeText(this, "Play button clicked", Toast.LENGTH_SHORT).show());
    }

    private void loadRecipeDetails() {
        DocumentReference recipeRef = db.collection("Recipes").document(recipeId);

        recipeRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                currentRecipe = documentSnapshot.toObject(Recipe.class);
                if (currentRecipe != null){
                    currentRecipe.setId(documentSnapshot.getId());
                }

                // Display the recipe details
                displayRecipeDetails();
            } else {
                Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error loading recipe: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void displayRecipeDetails() {
        recipeTitle.setText(currentRecipe.getTitle());
        recipeDifficulty.setText(currentRecipe.getDifficulty());
        recipeCookTime.setText(currentRecipe.getFormattedCookTime());
        recipeAuthor.setText(currentRecipe.getAuthorName());
        recipeDate.setText(currentRecipe.getFormattedDate());

        // Load image if available
        if (currentRecipe.getImageUrl() != null && !currentRecipe.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentRecipe.getImageUrl())
                    .placeholder(R.drawable.image_placeholder)
                    .into(recipeImage);
        } else {
            recipeImage.setImageResource(R.drawable.image_placeholder);
        }

        displayRecipeSteps();
    }

    private void displayRecipeSteps() {
        // Clear existing steps first
        stepsContainer.removeAllViews();

        // Get steps from the recipe
        List<Recipe.Step> steps = currentRecipe.getSteps();

        if (steps != null && !steps.isEmpty()) {
            for (int i = 0; i < steps.size(); i++) {
                Recipe.Step step = steps.get(i);

                // Inflate the step layout
                View stepView = getLayoutInflater().inflate(R.layout.recipe_step_item, stepsContainer, false);

                // Find views in the step layout
                TextView stepNumber = stepView.findViewById(R.id.stepNumber);
                TextView stepDescription = stepView.findViewById(R.id.stepDescription);
                TextView stepTimer = stepView.findViewById(R.id.stepTimer);

                // Set the step content
                stepNumber.setText(String.valueOf(step.getOrder()));
                stepDescription.setText(step.getDescription());

                // Show timer if available
                String formattedTime = step.getFormattedTime();
                if (!formattedTime.isEmpty()) {
                    stepTimer.setVisibility(View.VISIBLE);
                    stepTimer.setText(formattedTime);
                } else {
                    stepTimer.setVisibility(View.GONE);
                }

                // Add the step view to the container
                stepsContainer.addView(stepView);
            }
        } else {
            // If no steps, show a message
            TextView noSteps = new TextView(this);
            noSteps.setText("No steps available for this recipe");
            noSteps.setPadding(16, 16, 16, 16);
            stepsContainer.addView(noSteps);
        }
    }

    private void toggleFavorite(boolean isActive){
        if (isActive){
            favoriteButton.setImageResource(R.drawable.favorite_pressed_svg);
        }
        else {
            favoriteButton.setImageResource(R.drawable.favorite_unpressed_svg);
        }
    }
}