package com.adams_maxims_evyatarc.stepcook;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
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
import java.util.Locale;

public class RecipeDetailActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private UserManager userManager = UserManager.getInstance();
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
    private TextView countdownTextView;

    private boolean isActive = false;

    private TextToSpeech textToSpeech;
    private int currentStepIndex = 0;
    private boolean isAutoPlaying = false;
    private Handler handler = new Handler();
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        db = FirebaseFirestore.getInstance();

        recipeId = getIntent().getStringExtra("RECIPE_ID");
        if (recipeId == null) {
            Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userManager.loadUserData(new UserManager.UserDataCallback() {
            @Override
            public void onUserDataLoaded(User user) {
                isAutoPlaying = user.isAutoPlayNextStep();
                Log.d("RecipeDebug", "Autoplay from user profile: " + isAutoPlaying);
            }

            @Override
            public void onError(Exception e) {
                Log.e("RecipeDebug", "Failed to load user data: " + e.getMessage());
                Toast.makeText(RecipeDetailActivity.this, "Failed to load user settings", Toast.LENGTH_SHORT).show();
            }
        });

        initializeViews();
        setupListeners();
        loadRecipeDetails();
        initializeTextToSpeech();
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
        countdownTextView = findViewById(R.id.countdownTextView);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        favoriteButton.setOnClickListener(v -> {
            isActive = !isActive;
            toggleFavorite(isActive);
        });

        playButton.setOnClickListener(v -> {
            List<Recipe.Step> steps = currentRecipe.getSteps();

            if (currentStepIndex < steps.size()) {
                Recipe.Step step = steps.get(currentStepIndex);
                playStep(step);
            } else {
                Toast.makeText(this, "All steps completed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void playStep(Recipe.Step step) {
        String description = step.getDescription();
        textToSpeech.speak(description, TextToSpeech.QUEUE_FLUSH, null, null);

        Integer timer = step.getTimerMinutes();
        long delayMillis = timer != null ? timer * 60L * 1000L : 0;

        if (delayMillis > 0) {
            Toast.makeText(this, "Timer: " + step.getFormattedTime(), Toast.LENGTH_SHORT).show();
            startCountdown(delayMillis);
        } else {
            countdownTextView.setText("");
        }

        if (isAutoPlaying && delayMillis > 0) {
            handler.postDelayed(() -> {
                currentStepIndex++;
                if (currentStepIndex < currentRecipe.getSteps().size()) {
                    playStep(currentRecipe.getSteps().get(currentStepIndex));
                } else {
                    countdownTextView.setText("");
                }
            }, delayMillis);
        } else {
            currentStepIndex++;
        }
    }

    private void startCountdown(long millis) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(millis, 1000) {
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                long minutes = seconds / 60;
                long remSeconds = seconds % 60;
                countdownTextView.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, remSeconds));
            }

            public void onFinish() {
                countdownTextView.setText("");
            }
        };
        countDownTimer.start();
    }

    private void loadRecipeDetails() {
        DocumentReference recipeRef = db.collection("Recipes").document(recipeId);

        recipeRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                currentRecipe = documentSnapshot.toObject(Recipe.class);
                if (currentRecipe != null){
                    currentRecipe.setId(documentSnapshot.getId());
                }
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
        stepsContainer.removeAllViews();
        List<Recipe.Step> steps = currentRecipe.getSteps();

        if (steps != null && !steps.isEmpty()) {
            for (int i = 0; i < steps.size(); i++) {
                Recipe.Step step = steps.get(i);
                View stepView = getLayoutInflater().inflate(R.layout.recipe_step_item, stepsContainer, false);

                TextView stepNumber = stepView.findViewById(R.id.stepNumber);
                TextView stepDescription = stepView.findViewById(R.id.stepDescription);
                TextView stepTimer = stepView.findViewById(R.id.stepTimer);

                stepNumber.setText(String.valueOf(step.getOrder()));
                stepDescription.setText(step.getDescription());

                String formattedTime = step.getFormattedTime();
                if (!formattedTime.isEmpty()) {
                    stepTimer.setVisibility(View.VISIBLE);
                    stepTimer.setText(formattedTime);
                } else {
                    stepTimer.setVisibility(View.GONE);
                }

                stepsContainer.addView(stepView);
            }
        } else {
            TextView noSteps = new TextView(this);
            noSteps.setText("No steps available for this recipe");
            noSteps.setPadding(16, 16, 16, 16);
            stepsContainer.addView(noSteps);
        }
    }

    private void toggleFavorite(boolean isActive){
        favoriteButton.setImageResource(isActive ?
                R.drawable.favorite_pressed_svg :
                R.drawable.favorite_unpressed_svg
        );
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        super.onDestroy();
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.getDefault());
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "TTS language not supported", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "TTS initialization failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
