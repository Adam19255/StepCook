package com.adams_maxims_evyatarc.stepcook;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class RecipeVoiceService extends Service {

    private static final String TAG = "VoiceService";
    public static final String ACTION_VOICE_COMMAND = "com.adams_maxims_evyatarc.stepcook.VOICE_COMMAND";
    public static final String EXTRA_COMMAND = "command";

    private SpeechRecognizer speechRecognizer;
    private Handler handler = new Handler();
    private boolean isListening = false;
    private boolean shouldKeepListening = true;

    private long speechStartTime = 0;
    private final RecognitionListener recognitionListener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "Ready for speech");
            isListening = true;
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "Beginning of speech");
        }

        @Override
        public void onRmsChanged(float rmsdB) {}

        @Override
        public void onBufferReceived(byte[] buffer) {}

        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "End of speech");
            isListening = false;
        }

        @Override
        public void onError(int error) {
            isListening = false;
            Log.e(TAG, "Speech error: " + error);

            // Handle the common first-time ERROR_NO_MATCH issue
            long duration = System.currentTimeMillis() - speechStartTime;
            if (error == SpeechRecognizer.ERROR_NO_MATCH && duration < 1000) {
                Log.w(TAG, "Ignoring quick ERROR_NO_MATCH (likely initialization issue)");
                if (shouldKeepListening) {
                    handler.postDelayed(() -> {
                        if (shouldKeepListening) {
                            restartSpeechRecognition();
                        }
                    }, 500); // Shorter delay for quick restart
                }
                return;
            }

            if (shouldKeepListening && isRecoverableError(error)) {
                handler.postDelayed(() -> {
                    if (shouldKeepListening) {
                        restartSpeechRecognition();
                    }
                }, 1500);
            }
        }

        @Override
        public void onResults(Bundle results) {
            isListening = false;
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String command = matches.get(0).toLowerCase().trim();
                Log.d(TAG, "🎤 RECOGNIZED: " + command);

                // Send broadcast to activity
                sendVoiceCommandBroadcast(command);
            }

            if (shouldKeepListening) {
                handler.postDelayed(() -> {
                    if (shouldKeepListening) {
                        restartSpeechRecognition();
                    }
                }, 800);
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String partial = matches.get(0).toLowerCase().trim();
                Log.d(TAG, "🟡 PARTIAL: " + partial);

                // Send partial results too for immediate response
//                sendVoiceCommandBroadcast(partial);
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) {}
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Voice service created");
        initializeSpeechRecognition();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Voice service started");
        shouldKeepListening = true;

        if (!isListening) {
            startSpeechRecognition();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Voice service destroyed");
        shouldKeepListening = false;

        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.cancel();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        isListening = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendVoiceCommandBroadcast(String command) {
        if (command == null || command.trim().isEmpty()) {
            Log.d(TAG, "Ignoring empty command");
            return;
        }

        Intent intent = new Intent(ACTION_VOICE_COMMAND);
        intent.putExtra(EXTRA_COMMAND, command);
        sendBroadcast(intent);
        Log.d(TAG, "Sent broadcast for command: " + command);
    }

    private void initializeSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech recognition not available");
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Audio permission not granted");
            return;
        }

        try {
            if (speechRecognizer != null) {
                speechRecognizer.destroy();
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            if (speechRecognizer == null) {
                Log.e(TAG, "Failed to create SpeechRecognizer");
                return;
            }

            speechRecognizer.setRecognitionListener(recognitionListener);
            Log.d(TAG, "Speech recognition initialized");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing speech recognition", e);
        }
    }

    private void startSpeechRecognition() {
        if (speechRecognizer == null || isListening) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            speechStartTime = System.currentTimeMillis(); // Track start time

            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

            speechRecognizer.startListening(intent);
            Log.d(TAG, "Started listening");

        } catch (Exception e) {
            Log.e(TAG, "Error starting speech recognition", e);
            isListening = false;
        }
    }

    private void restartSpeechRecognition() {
        if (speechRecognizer != null && !isListening && shouldKeepListening) {
            startSpeechRecognition();
        }
    }

    private boolean isRecoverableError(int error) {
        return error == SpeechRecognizer.ERROR_NO_MATCH ||
                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                error == SpeechRecognizer.ERROR_AUDIO;
    }
}
