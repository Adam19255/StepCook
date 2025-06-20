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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Locale;

public class RecipeDetailActivity extends AppCompatActivity implements CookingInterruptionCallback {

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

    private BroadcastManager broadcastManager;
    private boolean wasPlayingBeforeInterruption = false;
    private boolean isPausedByInterruption = false;

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
        initializeBroadcastManager();

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

    private void initializeBroadcastManager() {
        broadcastManager = BroadcastManager.getInstance(this);
        broadcastManager.setCookingInterruptionCallback(this);
        broadcastManager.startListening();
        Log.d("RecipeDetailActivity", "Broadcast manager initialized and listening");
    }

    @Override
    public void onCookingInterrupted(InterruptionType type, String message) {
        Log.i("RecipeDetailActivity", "Cooking interrupted: " + type + " - " + message);

        // Save current playing state before interruption
        wasPlayingBeforeInterruption = isPlaying || (textToSpeech != null && textToSpeech.isSpeaking());

        switch (type) {
            case INCOMING_CALL:
                handleIncomingCallInterruption(message);
                break;
            case SCREEN_OFF:
                handleScreenOffInterruption(message);
                break;
            case HEADPHONES_DISCONNECTED:
                handleHeadphonesDisconnectedInterruption(message);
                break;
            case INTERNET_DISCONNECTED:
                handleInternetDisconnectedInterruption(message);
                break;
            case BATTERY_LOW:
                handleBatteryLowInterruption(message);
                break;
        }
    }

    private void handleIncomingCallInterruption(String message) {
        // Pause current step and timer
        stopStep();
        isPausedByInterruption = true;

        // Show alert dialog with options
        new AlertDialog.Builder(this)
                .setTitle("Incoming Call")
                .setMessage(message + "\n\nWould you like to pause your cooking session?")
                .setPositiveButton("Pause Cooking", (dialog, which) -> {
                    stopStep();
                    Toast.makeText(this, "Cooking paused for call", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Continue Cooking", (dialog, which) -> {
                    if (wasPlayingBeforeInterruption) {
                        resumeCurrentStep();
                    }
                    isPausedByInterruption = false;
                })
                .setCancelable(false)
                .show();
    }

    private void handleScreenOffInterruption(String message) {
        // Show notification instead of toast when screen is off
        showInterruptionNotification("Screen Off", message);

        // Don't pause automatically - let cooking continue in background
        // but inform user that timers are still running
        Log.i("RecipeDetailActivity", "Screen off - cooking continues in background");
    }

    private void handleHeadphonesDisconnectedInterruption(String message) {
        // Pause TTS and show alert
        if (textToSpeech != null && textToSpeech.isSpeaking()) {
            textToSpeech.stop();
        }

        new AlertDialog.Builder(this)
                .setTitle("Headphones Disconnected")
                .setMessage(message + "\n\nAudio will now play through the speaker. Continue?")
                .setPositiveButton("Continue", (dialog, which) -> {
                    if (wasPlayingBeforeInterruption && currentRecipe != null &&
                            currentStepIndex < currentRecipe.getSteps().size()) {
                        // Replay current step description through speaker
                        Recipe.Step currentStep = currentRecipe.getSteps().get(currentStepIndex);
                        textToSpeech.speak(currentStep.getDescription(), TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                })
                .setNegativeButton("Pause", (dialog, which) -> {
                    stopStep();
                    isPausedByInterruption = true;
                })
                .show();
    }

    private void handleInternetDisconnectedInterruption(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        showInterruptionNotification("Internet Disconnected", message);

        // Cooking can continue offline, just inform user
        Log.w("RecipeDetailActivity", "Internet disconnected - continuing offline");
    }

    private void handleBatteryLowInterruption(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Battery Low")
                .setMessage(message + "\n\nRecommend connecting to power source.")
                .setPositiveButton("Continue Cooking", (dialog, which) -> {
                    // Continue cooking but user is warned
                })
                .show();
    }

    @Override
    public void onCallEnded() {
        Log.d("RecipeDetailActivity", "Call ended");

        if (isPausedByInterruption) {
            new AlertDialog.Builder(this)
                    .setTitle("Call Ended")
                    .setMessage("Would you like to resume cooking?")
                    .setPositiveButton("Resume", (dialog, which) -> {
                        resumeCurrentStep();
                        isPausedByInterruption = false;
                        Toast.makeText(this, "Cooking resumed", Toast.LENGTH_SHORT).show();
                    })
                    .show();
        }
    }

    @Override
    public void onScreenStateChanged(boolean isScreenOn) {
        Log.d("RecipeDetailActivity", "Screen state changed: " + (isScreenOn ? "ON" : "OFF"));

        if (isScreenOn && isPausedByInterruption) {
            // Screen is back on, check if user wants to resume
            Toast.makeText(this, "Welcome back! Tap play to resume cooking.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onHeadphonesStateChanged(boolean areConnected) {
        Log.d("RecipeDetailActivity", "Headphones state changed: " + (areConnected ? "CONNECTED" : "DISCONNECTED"));

        if (areConnected && isPausedByInterruption) {
            Toast.makeText(this, "Headphones connected. Ready to resume audio guidance.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onInternetStateChanged(boolean isConnected) {
        Log.d("RecipeDetailActivity", "Internet state changed: " + (isConnected ? "CONNECTED" : "DISCONNECTED"));

        if (isConnected) {
            Toast.makeText(this, "Internet reconnected. All features available.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPowerStateChanged(boolean isPowerConnected) {
        Log.d("RecipeDetailActivity", "Power state changed: " + (isPowerConnected ? "CONNECTED" : "DISCONNECTED"));

        if (isPowerConnected && isPausedByInterruption) {
            Toast.makeText(this, "Power connected. Safe to resume long cooking sessions.", Toast.LENGTH_SHORT).show();
        }
    }

    private void resumeCurrentStep() {
        if (currentRecipe == null || currentStepIndex >= currentRecipe.getSteps().size()) {
            return;
        }
        Recipe.Step step = currentRecipe.getSteps().get(currentStepIndex);
        if (remainingTimeInMillis > 0 && currentStepTimerTextView != null) {
            // Resume timer from where it left off
            startInlineCountdown(remainingTimeInMillis);
        }

        // Replay the step description
        textToSpeech.speak(step.getDescription(), TextToSpeech.QUEUE_FLUSH, null, null);
        isPlaying = true;
        updatePlayButtonIcon();
    }
    private void showInterruptionNotification(String title, String message) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.all_cook_svg)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(NOTIFICATION_ID + 1, notification);
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
            if (currentRecipe == null) {
                Toast.makeText(this, "Recipe not loaded", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentStepIndex >= currentRecipe.getSteps().size()) {
                Toast.makeText(this, "All steps completed, restarting", Toast.LENGTH_SHORT).show();
                currentStepIndex = 0;
                remainingTimeInMillis = 0; // Reset timer
                isPlaying = false;
            }

            Recipe.Step currentStep = currentRecipe.getSteps().get(currentStepIndex);

            if (isPlaying) {
                // Currently playing - pause the step
                stopStep();
                isPlaying = false;
                isPausedByInterruption = false;
                Toast.makeText(this, "Paused", Toast.LENGTH_SHORT).show();
            } else {
                // Not playing - start/resume the step
                isPlaying = true;
                isPausedByInterruption = false;

                Log.d("RecipeDetailActivity", "Remaining time in milis:" + remainingTimeInMillis);
                if (remainingTimeInMillis > 0) {
                    // Resume from where we left off
                    resumeCurrentStep();
                    Toast.makeText(this, "Resumed", Toast.LENGTH_SHORT).show();
                } else {
                    // Start the current step from the beginning
                    playStep(currentStep);
                    Toast.makeText(this, "Playing step " + (currentStepIndex + 1), Toast.LENGTH_SHORT).show();
//                    currentStepIndex++;
                }
            }

            updatePlayButtonIcon();
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
                isPlaying = true;
                updatePlayButtonIcon();

                long delayMillis = step.getTimerMinutes() != null ? step.getTimerMinutes() * 60000L : 0;
                View currentStepView = stepsContainer.findViewWithTag("step_" + currentStepIndex);
                currentStepTimerTextView = currentStepView != null ? currentStepView.findViewById(R.id.stepTimer) : null;

                if (delayMillis > 0 && currentStepTimerTextView != null) {
                    startInlineCountdown(delayMillis);

                    // Timer step - increment index when timer completes
                    handler.postDelayed(() -> {
                        if (isPlaying) { // Only increment if still playing
                            sendStepNotification("Step " + (currentStepIndex + 1) + " completed. Ready for next!");
                            currentStepIndex++;
                            isPlaying = false;
                            updatePlayButtonIcon();
                        }
                    }, delayMillis);
                } else {
                    // No timer step - increment index when TTS finishes
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!textToSpeech.isSpeaking()) {
                                if (isPlaying) { // Only increment if still playing
                                    sendStepNotification("Step " + (currentStepIndex + 1) + " completed. Ready for next!");
                                    currentStepIndex++;
                                    isPlaying = false;
                                    updatePlayButtonIcon();
                                }
                            } else {
                                handler.postDelayed(this, 300);
                            }
                        }
                    }, 300);
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

        updatePlayButtonIcon();
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

    private void updatePlayButtonIcon() {
        boolean hasTimer = false;

        if (currentRecipe != null &&
                currentStepIndex >= 0 &&
                currentStepIndex < currentRecipe.getSteps().size()) {
            Recipe.Step step = currentRecipe.getSteps().get(currentStepIndex);
            Integer timerMinutes = step.getTimerMinutes();  // FIXED: use Integer
            hasTimer = (timerMinutes != null && timerMinutes > 0);
        }

        if (isPlaying && hasTimer) {
            playButton.setImageResource(R.drawable.pause_svg);
        } else {
            playButton.setImageResource(R.drawable.play_svg);
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

        // Reset interruption state when processing voice commands
        isPausedByInterruption = false;

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
            resumeCurrentStep();
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

        // Stop broadcast manager
        if (broadcastManager != null) {
            broadcastManager.stopListening();
            Log.d("RecipeDetailActivity", "Broadcast manager stopped");
        }

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
