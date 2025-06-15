package com.adams_maxims_evyatarc.stepcook;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
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
    private boolean isListening = false;

    private TextToSpeech textToSpeech;
    private Handler handler = new Handler();
    private CountDownTimer countDownTimer;
    private SpeechRecognizer speechRecognizer;
    private boolean isPlaying = false;

    private long remainingTimeInMillis = 0;

    private boolean canProcessCommand = true;
    private final long COMMAND_COOLDOWN_MS = 1500;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private final RecognitionListener recognitionListener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.d("SpeechRecognition", "Ready for speech");
            isListening = true;
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d("SpeechRecognition", "Beginning of speech");
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {}

        @Override
        public void onEndOfSpeech() {
            Log.d("SpeechRecognition", "End of speech");
            isListening = false;
        }

        @Override
        public void onError(int error) {
            isListening = false;
            String message = getErrorMessage(error);

            Log.e("SpeechRecognition", "Error: " + error + " - " + message);

            // Only show error toast for significant errors
            if (error != SpeechRecognizer.ERROR_NO_MATCH &&
                    error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                Toast.makeText(RecipeDetailActivity.this, message, Toast.LENGTH_SHORT).show();
            }

            // Restart listening after a delay, but only if activity is still active
            if (!isFinishing()) {
                handler.postDelayed(() -> {
                    if (!isFinishing()) {
                        restartSpeechRecognition();
                    }
                }, 2000);
            }
        }

        @Override
        public void onResults(Bundle results) {
            isListening = false;
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String command = matches.get(0).toLowerCase().trim();
                Log.d("SpeechRecognition", "Recognized: " + command);
                handleVoiceCommand(command);
            }

            // Restart listening after processing command
            if (!isFinishing()) {
                handler.postDelayed(() -> {
                    if (!isFinishing()) {
                        restartSpeechRecognition();
                    }
                }, 1000);
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            Log.d("SpeechRecognition", SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                for (String partial : matches) {
                    Log.d("SpeechRecognition", "🟡 Heard (partial): " + partial);
                    handleVoiceCommand(partial.toLowerCase(Locale.ROOT)); // ✅ Run command immediately
                }
            } else {
                Log.d("SpeechRecognition", "Partial Results: None");
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) {}
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

        userManager.loadUserData(new UserManager.UserDataCallback() {
            @Override
            public void onUserDataLoaded(User user) {
                isAutoPlaying = user.isAutoPlayNextStep();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(RecipeDetailActivity.this, "Failed to load user settings", Toast.LENGTH_SHORT).show();
            }
        });

        initializeViews();
        setupListeners();
        loadRecipeDetails();
        initializeTextToSpeech();

        // Check and request permission before initializing speech recognition
        checkAudioPermission();

        // Notification Manager channel creation
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Step Completion",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            initializeSpeechRecognition();
        }
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
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.all_cook_svg).setSmallIcon(R.drawable.all_cook_svg)
                .setContentTitle("Step Complete!")
                .setContentText(stepText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
        notificationManager.notify(NOTIFICATION_ID, notification);
    }
    private void playStep(Recipe.Step step) {
        textToSpeech.speak(step.getDescription(), TextToSpeech.QUEUE_FLUSH, null, null);
        Log.d("SpeechRecognition", "Playing step " + currentStepIndex + " " + step.getDescription());

        long delayMillis = step.getTimerMinutes() != null ? step.getTimerMinutes() * 60L * 1000L : 0;

        View currentStepView = stepsContainer.findViewWithTag("step_" + currentStepIndex);
        currentStepTimerTextView = currentStepView != null ? currentStepView.findViewById(R.id.stepTimer) : null;

        if (delayMillis > 0 && currentStepTimerTextView != null) {
            startInlineCountdown(delayMillis);
        }

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
                // No timer — wait for TTS to finish
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
                            handler.postDelayed(this, 300); // check again in 300ms
                        }
                    }
                }, 300);
            }
        } else {
            currentStepIndex++;
        }
    }

    private void startInlineCountdown(long millis) {
        remainingTimeInMillis = millis;

        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(millis, 1000) {
            public void onTick(long millisUntilFinished) {
                remainingTimeInMillis = millisUntilFinished; // 💾 Save remaining time
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
        db.collection("Recipes").document(recipeId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            currentRecipe = doc.toObject(Recipe.class);
            if (currentRecipe != null) {
                currentRecipe.setId(doc.getId());
                displayRecipeDetails();
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
                    // Try English as fallback
                    textToSpeech.setLanguage(Locale.ENGLISH);
                }
            } else {
                Log.e("TTS", "TextToSpeech initialization failed");
            }
        });
    }

    private void initializeSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition not available on this device", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            if (speechRecognizer != null) {
                speechRecognizer.destroy();
            }
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            if (speechRecognizer == null) {
                Log.e("SpeechRecognition", "Failed to create SpeechRecognizer");
                Toast.makeText(this, "Failed to initialize speech recognition", Toast.LENGTH_SHORT).show();
                return;
            }

            speechRecognizer.setRecognitionListener(recognitionListener);
            startSpeechRecognition();

            Log.d("SpeechRecognition", "Speech recognition initialized successfully");
        } catch (Exception e) {
            Log.e("SpeechRecognition", "Error initializing speech recognition", e);
            Toast.makeText(this, "Error setting up voice commands", Toast.LENGTH_SHORT).show();
        }
    }

    private void startSpeechRecognition() {
        if (speechRecognizer == null || isListening) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w("SpeechRecognition", "Audio permission not granted");
            return;
        }

        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            // Add these for better recognition
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000);
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000);


            speechRecognizer.startListening(intent);
            Log.d("SpeechRecognition", "Started listening");
        } catch (Exception e) {
            Log.e("SpeechRecognition", "Error starting speech recognition", e);
            isListening = false;
        }
    }

    private void restartSpeechRecognition() {
        if (speechRecognizer != null && !isListening && !isFinishing()) {
            startSpeechRecognition();
        }
    }

    private String getErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Insufficient permissions";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No match found";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Recognition service busy";
            case SpeechRecognizer.ERROR_SERVER:
                return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech input";
            default:
                return "Unknown error (" + error + ")";
        }
    }


    private void stopStep() {
        // Stop timer but don't reset remainingTimeInMillis
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

            stopStep(); // Stop TTS and timer

            Recipe.Step step = currentRecipe.getSteps().get(currentStepIndex);
            remainingTimeInMillis = step.getTimerMinutes() != null
                    ? step.getTimerMinutes() * 60L * 1000L
                    : 0;

            // Speak step again
            textToSpeech.speak(step.getDescription(), TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void handleVoiceCommand(String command) {
        if (!canProcessCommand) return;
        handler.postDelayed(() -> canProcessCommand = true, COMMAND_COOLDOWN_MS);

        Log.d("SpeechRecognition", "💻 Processing command: " + command);

        if (command.contains("next")) {
            resetStep(); // Stop current timer and reset display
            canProcessCommand = false;

            // Move to next step first
            currentStepIndex++;

            if (currentRecipe != null && currentStepIndex < currentRecipe.getSteps().size()) {
                playStep(currentRecipe.getSteps().get(currentStepIndex));
                Toast.makeText(this, "Next step", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No more steps", Toast.LENGTH_SHORT).show();
                // Reset to last valid step if we went too far
                currentStepIndex = Math.max(0, currentRecipe.getSteps().size() - 1);
            }
        } else if (command.contains("previous") || command.contains("back")) {
            canProcessCommand = false;
            resetStep(); // Stop current timer and reset display
            if (currentStepIndex > 0) {
                resetStep();
                currentStepIndex--;
                playStep(currentRecipe.getSteps().get(currentStepIndex));
                Toast.makeText(this, "Previous step", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Already at first step", Toast.LENGTH_SHORT).show();
            }
        } else if (command.contains("pause") || command.contains("stop")) {
            canProcessCommand = false;
            stopStep(); // This will stop and reset the timer
            Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show();
        } else if (command.contains("resume") || command.contains("continue")) {
            canProcessCommand = false;
            if (remainingTimeInMillis > 0 && currentStepTimerTextView != null) {
                startInlineCountdown(remainingTimeInMillis); // ▶️ Resume countdown
                Recipe.Step step = currentRecipe.getSteps().get(currentStepIndex);
                textToSpeech.speak(step.getDescription(), TextToSpeech.QUEUE_FLUSH, null, null); // Optional: resume speech
                Toast.makeText(this, "Resumed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Nothing to resume", Toast.LENGTH_SHORT).show();
            }
        } else if (command.contains("favorite")) {
            canProcessCommand = false;
            isActive = !isActive;
            toggleFavorite(isActive);
            Toast.makeText(this, isActive ? "Added to favorites" : "Removed from favorites", Toast.LENGTH_SHORT).show();
        } else if (command.contains("repeat") || command.contains("again")) {
            resetStep(); // Stop current timer first
            if (currentRecipe != null && currentStepIndex >= 0 && currentStepIndex < currentRecipe.getSteps().size()) {
                playStep(currentRecipe.getSteps().get(currentStepIndex));
                Toast.makeText(this, "Repeating current step", Toast.LENGTH_SHORT).show();
            }
        } else if(command.contains("play")) {
            canProcessCommand = false;

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
            canProcessCommand = false;
            Toast.makeText(this, "Restarting recipe", Toast.LENGTH_SHORT).show();

            currentStepIndex = 0;
            resetStep();
            playStep(currentRecipe.getSteps().get(currentStepIndex));
        }
          else {
            Log.d("SpeechRecognition", "Unknown command: " + command);
            // Don't show toast for unknown commands to avoid spam
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            isListening = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (speechRecognizer != null && !isListening) {
            handler.postDelayed(() -> restartSpeechRecognition(), 500);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }

        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.cancel();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        isListening = false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Voice commands enabled", Toast.LENGTH_SHORT).show();
                initializeSpeechRecognition();
            } else {
                Toast.makeText(this, "Microphone permission required for voice commands", Toast.LENGTH_LONG).show();
            }
        }
    }
}