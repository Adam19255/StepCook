package com.adams_maxims_evyatarc.stepcook;

import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manager class to handle Recipe data operations with Firebase
 * Follows the singleton pattern for app-wide consistency
 */
public class RecipeManager {
    private static final String TAG = "RecipeManager";
    private static final String RECIPES_COLLECTION = "Recipes";
    private static final String RECIPE_IMAGES_PATH = "recipe_images";

    private static RecipeManager instance;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    // Interface for handling recipe operations
    public interface RecipeOperationCallback {
        void onSuccess(String recipeId);
        void onError(Exception e);
    }

    // Interface for retrieving recipes
    public interface RecipesRetrievalCallback {
        void onRecipesLoaded(List<Recipe> recipes);
        void onError(Exception e);
    }

    // Private constructor to enforce singleton pattern
    private RecipeManager() {
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }

    // Get the singleton instance
    public static synchronized RecipeManager getInstance() {
        if (instance == null) {
            instance = new RecipeManager();
        }
        return instance;
    }

    /**
     * Save a new recipe to Firestore
     *
     * @param title Recipe title
     * @param difficulty Recipe difficulty level
     * @param totalCookTime Total cooking time in minutes
     * @param stepsContainer LinearLayout containing all step views
     * @param imageUri Optional Uri of the recipe image (null if no image)
     * @param callback Callback to receive success or error
     */
    public void saveRecipe(String title, String difficulty, int totalCookTime,
                           LinearLayout stepsContainer, Uri imageUri,
                           final RecipeOperationCallback callback) {

        // Get current user ID and verify authentication
        String userId = UserManager.getInstance().getCurrentUserId();
        if (userId == null) {
            callback.onError(new Exception("No user is logged in. Please sign in first."));
            return;
        }

        // Extract steps from the container
        List<Map<String, Object>> steps = extractSteps(stepsContainer);

        // Create a new recipe document
        Map<String, Object> recipeData = new HashMap<>();
        recipeData.put("title", title);
        recipeData.put("difficulty", difficulty);
        recipeData.put("authorId", userId);
        recipeData.put("totalCookTimeMinutes", totalCookTime);
        recipeData.put("createdDate", new Timestamp(new Date()));
        recipeData.put("steps", steps);

        // Add user display name if available
        User currentUser = UserManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getUserName() != null) {
            recipeData.put("authorName", currentUser.getUserName());
        }

        // If no image, just save the recipe data
        if (imageUri == null) {
            saveRecipeData(recipeData, callback);
            return;
        }

        // Otherwise, upload the image first
        String imageFileName = UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = storageRef.child(RECIPE_IMAGES_PATH + "/" + imageFileName);

        imageRef.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get the image download URL
                        imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri downloadUri) {
                                // Add the image URL to recipe data
                                recipeData.put("imageUrl", downloadUri.toString());

                                // Now save the recipe with image URL
                                saveRecipeData(recipeData, callback);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "Failed to get download URL", e);
                                callback.onError(e);
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Image upload failed", e);
                        callback.onError(e);
                    }
                });
    }

    /**
     * Save recipe data to Firestore
     */
    private void saveRecipeData(Map<String, Object> recipeData, final RecipeOperationCallback callback) {
        db.collection(RECIPES_COLLECTION)
                .add(recipeData)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "Recipe saved with ID: " + documentReference.getId());
                        callback.onSuccess(documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error saving recipe", e);
                        callback.onError(e);
                    }
                });
    }

    /**
     * Extract step data from the step views in the container
     */
    private List<Map<String, Object>> extractSteps(LinearLayout stepsContainer) {
        List<Map<String, Object>> stepsList = new ArrayList<>();

        for (int i = 0; i < stepsContainer.getChildCount(); i++) {
            View stepView = stepsContainer.getChildAt(i);

            // Skip if not a step view
            if (stepView.getTag() == null || !stepView.getTag().toString().startsWith("step_")) {
                continue;
            }

            EditText stepDetail = stepView.findViewById(R.id.stepDetail);
            String content = stepDetail.getText().toString().trim();

            // Skip empty steps
            if (content.isEmpty()) {
                continue;
            }

            Map<String, Object> stepData = new HashMap<>();
            stepData.put("description", content);
            stepData.put("order", i + 1);

            // Check if this step has a timer
            ImageView timerIcon = stepView.findViewById(R.id.timerIcon);
            if (timerIcon != null && timerIcon.getTag() != null) {
                int minutes = (int) timerIcon.getTag();
                stepData.put("timerMinutes", minutes);
            }

            stepsList.add(stepData);
        }

        return stepsList;
    }

    /**
     * Get all recipes from Firestore
     */
    public void getAllRecipes(final RecipesRetrievalCallback callback) {
        db.collection(RECIPES_COLLECTION)
                .orderBy("createdDate", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Recipe> recipes = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Recipe recipe = document.toObject(Recipe.class);
                                recipe.setId(document.getId());
                                recipes.add(recipe);
                            }
                            callback.onRecipesLoaded(recipes);
                        } else {
                            Log.e(TAG, "Error getting recipes", task.getException());
                            callback.onError(task.getException());
                        }
                    }
                });
    }
}