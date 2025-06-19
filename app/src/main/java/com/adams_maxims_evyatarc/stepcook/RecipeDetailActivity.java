package com.adams_maxims_evyatarc.stepcook;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Locale;

public class RecipeDetailActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private UserManager userManager = UserManager.getInstance();
    private NotificationManager notificationManager;
    private static final String CHANNEL_ID = "notificaionChannel";
    private static final int NOTIFICATION_ID = 1001;

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
    private TextView currentStepTimerTextView;

    private boolean isActive = false;
    private boolean isAutoPlaying = false;
    private int currentStepIndex = 0;
    private boolean isPlaying = false;

    private TextToSpeech textToSpeech;
    private Handler handler = new Handler();
    private CountDownTimer countDownTimer;

    private long remainingTimeInMillis = 0;

    private boolean canProcessCommand = true;
    private final long COMMAND_COOLDOWN_MS = 1500;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private String lastProcessedCommand = "";
    private long lastCommandTime = 0;
    private final long DUPLICATE_COMMAND_THRESHOLD_MS = 2000;

    // Broadcast receiver for voice commands
    private BroadcastReceiver voiceCommandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("VoiceService", "Broadcast Trigger");
            if (RecipeVoiceService.ACTION_VOICE_COMMAND.equals(intent.getAction())) {
                String command = intent.getStringExtra(RecipeVoiceService.EXTRA_COMMAND);
                if (command != null) {
                    Log.d("VoiceService", "Command received outside: " + command);
                    handleVoiceCommand(command);
                }
            }
        }
    };

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

        initializeViews();
        setupListeners();
        initializeTextToSpeech();

        // Register broadcast receiver for voice commands
        IntentFilter filter = new IntentFilter(RecipeVoiceService.ACTION_VOICE_COMMAND);
        registerReceiver(voiceCommandReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        // Check permissions and start voice service
        checkAudioPermissionAndStartService();

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Step Completion",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }

        // Load user setting
        userManager.loadUserData(new UserManager.UserDataCallback() {
            @Override
            public void onUserDataLoaded(User user) {
                isAutoPlaying = user.isAutoPlayNextStep();
                Log.d("RecipeDetailActivity", "Loaded autoPlayNextStep = " + isAutoPlaying);
            }

            @Override
            public void onError(Exception e) {
                Log.e("RecipeDetailActivity", "Failed to load user preferences", e);
                isAutoPlaying = false;
            }
        });

        loadRecipeDetails();
    }

    private void checkAudioPermissionAndStartService() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            startVoiceService();
        }
    }

    private void startVoiceService() {
        Intent serviceIntent = new Intent(this, RecipeVoiceService.class);
        startService(serviceIntent);
        Log.d("RecipeDetailActivity", "Voice service started");
    }

    private void stopVoiceService() {
        Intent serviceIntent = new Intent(this, RecipeVoiceService.class);
        stopService(serviceIntent);
        Log.d("RecipeDetailActivity", "Voice service stopped");
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

        playButton.setOnClickListener(v -> {
            if (currentRecipe == null || currentStepIndex >= currentRecipe.getSteps().size()) {
                Toast.makeText(this, "All steps completed, restarting", Toast.LENGTH_SHORT).show();
                currentStepIndex = 0;
                return;
            }
            if(!isPlaying) {
                isPlaying = !isPlaying;
                Toast.makeText(this, "Now playing", Toast.LENGTH_SHORT).show();
            }
            playStep(currentRecipe.getSteps().get(currentStepIndex));
        });
    }

    private void sendStepNotification(String stepText) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.all_cook_svg)
                .setContentTitle("Step Complete!")
                .setContentText(stepText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void playStep(Recipe.Step step) {
        new Thread(() -> {
            Log.d("VoiceService", "playing step " + currentStepIndex);
            textToSpeech.speak(step.getDescription(), TextToSpeech.QUEUE_FLUSH, null, null);
            runOnUiThread(() -> {
                long delayMillis = step.getTimerMinutes() != null ? step.getTimerMinutes() * 60000L : 0;
                View currentStepView = stepsContainer.findViewWithTag("step_" + currentStepIndex);
                currentStepTimerTextView = currentStepView != null ? currentStepView.findViewById(R.id.stepTimer) : null;
                if (delayMillis > 0 && currentStepTimerTextView != null) startInlineCountdown(delayMillis);
                if (isAutoPlaying) {
                    if (delayMillis > 0) {
                        handler.postDelayed(() -> {
                            sendStepNotification("Step " + (currentStepIndex + 1) + " completed. Ready for next!");
                            currentStepIndex++;
                            if (currentStepIndex < currentRecipe.getSteps().size()) {
                                playStep(currentRecipe.getSteps().get(currentStepIndex));
                            }
                        }, delayMillis);
                    } else {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!textToSpeech.isSpeaking()) {
                                    sendStepNotification("Step " + (currentStepIndex + 1) + " completed. Ready for next!");
                                    currentStepIndex++;
                                    if (currentStepIndex < currentRecipe.getSteps().size()) {
                                        playStep(currentRecipe.getSteps().get(currentStepIndex));
                                    }
                                } else {
                                    handler.postDelayed(this, 300);
                                }
                            }
                        }, 300);
                    }
                }
            });
        }).start();
    }

    private void startInlineCountdown(long millis) {
        remainingTimeInMillis = millis;

        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(millis, 1000) {
            public void onTick(long millisUntilFinished) {
                remainingTimeInMillis = millisUntilFinished;
                long minutes = millisUntilFinished / 60000;
                long seconds = (millisUntilFinished / 1000) % 60;
                if (currentStepTimerTextView != null) {
                    currentStepTimerTextView.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
                }
            }

            public void onFinish() {
                remainingTimeInMillis = 0;
                if (currentStepTimerTextView != null) currentStepTimerTextView.setText("");
            }
        };
        countDownTimer.start();
    }

    private void loadRecipeDetails() {
        new Thread(() -> {
            db.collection("Recipes").document(recipeId).get().addOnSuccessListener(doc -> {
                new Thread(() -> {
                    if (!doc.exists()) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                        return;
                    }
                    currentRecipe = doc.toObject(Recipe.class);
                    if (currentRecipe != null) {
                        currentRecipe.setId(doc.getId());
                        runOnUiThread(this::displayRecipeDetails);
                    }
                }).start();
            }).addOnFailureListener(e -> runOnUiThread(() -> {
                Toast.makeText(this, "Error loading recipe: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }));
        }).start();
    }

    private void displayRecipeDetails() {
        recipeTitle.setText(currentRecipe.getTitle());
        recipeDifficulty.setText(currentRecipe.getDifficulty());
        recipeCookTime.setText(currentRecipe.getFormattedCookTime());
        recipeAuthor.setText(currentRecipe.getAuthorName());
        recipeDate.setText(currentRecipe.getFormattedDate());

        if (currentRecipe.getImageUrl() != null && !currentRecipe.getImageUrl().isEmpty()) {
            Glide.with(this).load(currentRecipe.getImageUrl())
                    .placeholder(R.drawable.image_placeholder).into(recipeImage);
        } else {
            recipeImage.setImageResource(R.drawable.image_placeholder);
        }

        displayRecipeSteps();
    }

    private void displayRecipeSteps() {
        stepsContainer.removeAllViews();
        List<Recipe.Step> steps = currentRecipe.getSteps();
        if (steps == null || steps.isEmpty()) {
            TextView noSteps = new TextView(this);
            noSteps.setText("No steps available for this recipe");
            noSteps.setPadding(16, 16, 16, 16);
            stepsContainer.addView(noSteps);
            return;
        }

        for (int i = 0; i < steps.size(); i++) {
            Recipe.Step step = steps.get(i);
            View stepView = getLayoutInflater().inflate(R.layout.recipe_step_item, stepsContainer, false);

            TextView stepNumber = stepView.findViewById(R.id.stepNumber);
            TextView stepDescription = stepView.findViewById(R.id.stepDescription);
            TextView stepTimer = stepView.findViewById(R.id.stepTimer);

            stepNumber.setText(String.valueOf(step.getOrder()));
            stepDescription.setText(step.getDescription());

            String formattedTime = step.getFormattedTime();
            stepTimer.setVisibility(formattedTime.isEmpty() ? View.GONE : View.VISIBLE);
            stepTimer.setText(formattedTime);

            stepView.setTag("step_" + i);
            stepsContainer.addView(stepView);
        }
    }

    private void toggleFavorite(boolean isActive) {
        favoriteButton.setImageResource(isActive
                ? R.drawable.favorite_pressed_svg
                : R.drawable.favorite_unpressed_svg);
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.getDefault());
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported");
                    textToSpeech.setLanguage(Locale.ENGLISH);
                }
            } else {
                Log.e("TTS", "TextToSpeech initialization failed");
            }
        });
    }

    private void stopStep() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        if (textToSpeech != null && textToSpeech.isSpeaking()) {
            textToSpeech.stop();
        }
    }

    private void resetStep() {
        if (currentRecipe != null &&
                currentStepIndex >= 0 &&
                currentStepIndex < currentRecipe.getSteps().size()) {

            stopStep();

            Recipe.Step step = currentRecipe.getSteps().get(currentStepIndex);
            remainingTimeInMillis = step.getTimerMinutes() != null
                    ? step.getTimerMinutes() * 60L * 1000L
                    : 0;

            textToSpeech.speak(step.getDescription(), TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void handleVoiceCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return;
        }

        // Check for duplicate commands within threshold
        long currentTime = System.currentTimeMillis();
        if (command.equals(lastProcessedCommand) &&
                (currentTime - lastCommandTime) < DUPLICATE_COMMAND_THRESHOLD_MS) {
            Log.d("VoiceService", "Ignoring duplicate command: " + command);
            return;
        }

        if (!canProcessCommand) return;
        canProcessCommand = false;
        handler.postDelayed(() -> canProcessCommand = true, COMMAND_COOLDOWN_MS);

        // Store command info for duplicate detection
        lastProcessedCommand = command;
        lastCommandTime = currentTime;

        Log.d("VoiceService", "💻 Processing command: " + command);

        if (command.contains("next")) {
            resetStep();
            currentStepIndex++;

            if (currentRecipe != null && currentStepIndex < currentRecipe.getSteps().size()) {
                playStep(currentRecipe.getSteps().get(currentStepIndex));
                Toast.makeText(this, "Next step", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No more steps", Toast.LENGTH_SHORT).show();
                currentStepIndex = Math.max(0, currentRecipe.getSteps().size() - 1);
            }
        } else if (command.contains("previous") || command.contains("back")) {
            Log.d("VoiceService", "Current step is " + currentStepIndex);
            resetStep();
            Log.d("VoiceService", "Current step after reset " + currentStepIndex);
            if (currentStepIndex > 0) {
                currentStepIndex--;
                Log.d("VoiceService", "Current step after decrease " + currentStepIndex);
                playStep(currentRecipe.getSteps().get(currentStepIndex));
                Toast.makeText(this, "Previous step", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Already at first step", Toast.LENGTH_SHORT).show();
            }
        } else if (command.contains("pause") || command.contains("stop")) {
            stopStep();
            Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show();
        } else if (command.contains("resume") || command.contains("continue")) {
            if (remainingTimeInMillis > 0 && currentStepTimerTextView != null) {
                startInlineCountdown(remainingTimeInMillis);
                Recipe.Step step = currentRecipe.getSteps().get(currentStepIndex);
                textToSpeech.speak(step.getDescription(), TextToSpeech.QUEUE_FLUSH, null, null);
                Toast.makeText(this, "Resumed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Nothing to resume", Toast.LENGTH_SHORT).show();
            }
        } else if (command.contains("favorite")) {
            isActive = !isActive;
            toggleFavorite(isActive);
            Toast.makeText(this, isActive ? "Added to favorites" : "Removed from favorites", Toast.LENGTH_SHORT).show();
        } else if (command.contains("repeat") || command.contains("again")) {
            resetStep();
            if (currentRecipe != null && currentStepIndex >= 0 && currentStepIndex < currentRecipe.getSteps().size()) {
                playStep(currentRecipe.getSteps().get(currentStepIndex));
                Toast.makeText(this, "Repeating current step", Toast.LENGTH_SHORT).show();
            }
        } else if(command.contains("play")) {
            Toast.makeText(this, "Playing recipe", Toast.LENGTH_SHORT).show();
            if (currentRecipe == null || currentStepIndex >= currentRecipe.getSteps().size()) {
                Toast.makeText(this, "All steps completed, restarting", Toast.LENGTH_SHORT).show();
                currentStepIndex = 0;
            }
            if(!isPlaying) {
                isPlaying = true;
                Toast.makeText(this, "Now playing", Toast.LENGTH_SHORT).show();
            }
            playStep(currentRecipe.getSteps().get(currentStepIndex));
        } else if (command.contains("restart")) {
            Toast.makeText(this, "Restarting recipe", Toast.LENGTH_SHORT).show();
            currentStepIndex = 0;
            resetStep();
            playStep(currentRecipe.getSteps().get(currentStepIndex));
        } else {
            Log.d("RecipeDetailActivity", "Unknown command: " + command);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Service continues running in background
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Service automatically continues listening
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister broadcast receiver
        if (voiceCommandReceiver != null) {
            unregisterReceiver(voiceCommandReceiver);
        }

        // Stop voice service
        stopVoiceService();

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Voice commands enabled", Toast.LENGTH_SHORT).show();
                startVoiceService();
            } else {
                Toast.makeText(this, "Microphone permission required for voice commands", Toast.LENGTH_LONG).show();
            }
        }
    }
}
