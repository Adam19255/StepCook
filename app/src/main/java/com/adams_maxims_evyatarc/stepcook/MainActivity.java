package com.adams_maxims_evyatarc.stepcook;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener,
        RecipeAdapter.OnRecipeClickListener, CookingInterruptionCallback {

    private static final int ADD_RECIPE_REQUEST_CODE = 1001;
    private ImageView popupMenuButton;
    private EditText searchInput;
    private Button difficultyFilter;
    private Button favoriteFilter;
    private Button cookTimeFilter;
    private Button myRecipesFilter;
    private ImageView addRecipeButton;
    private RecyclerView recipeRecyclerView;
    private FrameLayout loadingLayout;
    private TextView noResultsText;

    private RecipeAdapter recipeAdapter;
    private List<Recipe> allRecipes;

    private DifficultyFilterManager difficultyFilterManager;

    private CookTimeFilterManager cookTimeFilterManager;
    private FavoriteFilterManager favoriteFilterManager;
    private MyRecipesFilterManager myRecipesFilterManager;

    private DialogManager dialogManager;
    private UIHelper uiHelper;
    private RecipeManager recipeManager;
    private User currentUser;
    private BroadcastManager broadcastManager;


    private ActivityResultLauncher<Intent> addRecipeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Recipe was added successfully, reload the list
                    loadRecipes();
                }
            }
    );

    private ActivityResultLauncher<Intent> recipeDetailLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Recipe was deleted successfully, reload the list
                    loadRecipes();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeManagers();
        setupRecyclerView();
        setupClickListeners();

        // Initialize broadcast manager
        broadcastManager = BroadcastManager.getInstance(this);
        broadcastManager.setCookingInterruptionCallback(this);

        // Load favorites first, then recipes
        loadFavoriteRecipesAndThenLoadRecipes();
    }


    private void initializeViews() {
        popupMenuButton = findViewById(R.id.popupMenuButton);
        searchInput = findViewById(R.id.searchInput);
        difficultyFilter = findViewById(R.id.difficultyFilter);
        favoriteFilter = findViewById(R.id.favoriteFilter);
        cookTimeFilter = findViewById(R.id.cookTimeFilter);
        myRecipesFilter = findViewById(R.id.myRecipesFilter);
        addRecipeButton = findViewById(R.id.addRecipeButton);
        recipeRecyclerView = findViewById(R.id.recipeRecyclerView);
        loadingLayout = findViewById(R.id.loadingLayout);
        noResultsText = findViewById(R.id.noResultsText);

        searchInput.clearFocus();

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (recipeAdapter != null) {
                    recipeAdapter.filter(s.toString());
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void initializeManagers() {
        uiHelper = new UIHelper(this);
        dialogManager = new DialogManager(this);
        recipeManager = RecipeManager.getInstance();

        difficultyFilterManager = new DifficultyFilterManager(this, difficultyFilter, uiHelper);

        cookTimeFilterManager = new CookTimeFilterManager(this, cookTimeFilter, uiHelper);
        favoriteFilterManager = new FavoriteFilterManager(this, favoriteFilter, uiHelper);
        myRecipesFilterManager = new MyRecipesFilterManager(this, myRecipesFilter, uiHelper);
    }

    private void setupRecyclerView() {
        recipeAdapter = new RecipeAdapter(this);
        recipeAdapter.setOnRecipeClickListener(this);
        recipeRecyclerView.setAdapter(recipeAdapter);
        recipeRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupClickListeners() {
        popupMenuButton.setOnClickListener(this::showPopupMenu);
        addRecipeButton.setOnClickListener(v ->{
                    Intent intent = new Intent(MainActivity.this, AddRecipeActivity.class);
                    addRecipeLauncher.launch(intent);
                });
        difficultyFilter.setOnClickListener(v -> difficultyFilterManager.showFilterDialog());
        favoriteFilter.setOnClickListener(v -> favoriteFilterManager.toggleFilter());
        cookTimeFilter.setOnClickListener(v -> cookTimeFilterManager.showFilterDialog());
        myRecipesFilter.setOnClickListener(v -> {
            boolean newValue = !myRecipesFilterManager.isFilterActive(); // or however your manager tracks toggle
            onMyRecipesFilterToggled(newValue);
            myRecipesFilterManager.setFilterActive(newValue); // update its state
        });

    }
    String getCurrentUserId() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }


    private void showLoading() {
        loadingLayout.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        loadingLayout.setVisibility(View.GONE);
    }

    private void loadRecipes() {
        showLoading();
        recipeManager.getAllRecipes(new RecipeManager.RecipesRetrievalCallback() {
            @Override
            public void onRecipesLoaded(List<Recipe> recipes) {
                allRecipes = recipes;
                recipeAdapter.setRecipes(allRecipes);
                hideLoading();

                // Reapply filters after loading recipes
                applyAllFilters();
            }

            @Override
            public void onError(Exception e) {
                hideLoading();
                Toast.makeText(MainActivity.this, "Error loading recipes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.popup_menu);
        popupMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        } else if (id == R.id.menu_about) {
            dialogManager.showAboutDialog();
            return true;
        } else if (id == R.id.menu_exit) {
            dialogManager.showExitConfirmationDialog();
            return true;
        }
        return false;
    }

    public void applyAllFilters() {
        // Check if recipes are loaded first
        if (allRecipes == null || allRecipes.isEmpty()) {
            return;
        }

        String currentUserId = getCurrentUserId();
        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Recipe> filtered = new ArrayList<>(allRecipes);

                    // FAVORITE FILTER
                    if (favoriteFilterManager.isFilterActive()) {
                        List<String> favorites = (List<String>) snapshot.get("favorites");
                        if (favorites != null) {
                            filtered.removeIf(recipe -> !favorites.contains(recipe.getId()));
                        }
                    }

                    // OTHER FILTERS
                    filtered = difficultyFilterManager.applyFilter(filtered);
                    filtered = cookTimeFilterManager.applyFilter(filtered);
                    filtered = myRecipesFilterManager.applyFilter(filtered);

                    // Show/hide "No results" message
                    updateNoResultsVisibility(filtered.isEmpty());

                    // UPDATE LIST
                    recipeAdapter.updateList(filtered);
                });
    }

    // Method to show/hide "No results" message
    private void updateNoResultsVisibility(boolean showNoResults) {
        if (noResultsText != null) {
            noResultsText.setVisibility(showNoResults ? View.VISIBLE : View.GONE);
        }
        recipeRecyclerView.setVisibility(showNoResults ? View.GONE : View.VISIBLE);
    }

    public void onFavoriteFilterToggled(boolean onlyFavorites) {
        String currentUserId = getCurrentUserId(); // You already use this method

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        List<String> favorites = (List<String>) snapshot.get("favorites");
                        if (favorites == null) favorites = new ArrayList<>();

                        if (onlyFavorites) {
                            // Filter to only show recipes in favorites
                            List<Recipe> filtered = new ArrayList<>();
                            for (Recipe recipe : allRecipes) {
                                if (favorites.contains(recipe.getId())) {
                                    filtered.add(recipe);
                                }
                            }
                            // Show/hide "No results" message
                            updateNoResultsVisibility(filtered.isEmpty());
                            recipeAdapter.updateList(filtered);
                        } else {
                            // Show/hide "No results" message
                            updateNoResultsVisibility(allRecipes.isEmpty());
                            recipeAdapter.updateList(allRecipes);
                        }
                    }
                });
    }


    private void loadFavoriteRecipesAndThenLoadRecipes() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore.getInstance()
                    .collection("Users")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        List<String> favorites = (List<String>) documentSnapshot.get("favorites");
                        if (favorites != null) {
                            recipeAdapter.setFavoriteRecipeIds(new ArrayList<>(favorites));
                        } else {
                            recipeAdapter.setFavoriteRecipeIds(new ArrayList<>());
                        }

                        // Now that favorites are set, load recipes
                        loadRecipes();
                    })
                    .addOnFailureListener(e -> {
                        recipeAdapter.setFavoriteRecipeIds(new ArrayList<>());
                        loadRecipes(); // Still try loading recipes
                    });
        } else {
            loadRecipes(); // Not logged in? Still load recipes
        }
    }


    public void onMyRecipesFilterToggled(boolean onlyMine) {
        // You can get user ID from wherever it's stored (e.g., shared preferences or user session)
        String userId = getCurrentUserId();  // define this method if needed
        recipeAdapter.setMyRecipesFilter(userId, onlyMine);
    }


    @Override
    public void onRecipeClick(Recipe recipe) {
        Intent intent = new Intent(MainActivity.this, RecipeDetailActivity.class);
        intent.putExtra("RECIPE_ID", recipe.getId());
        recipeDetailLauncher.launch(intent);
    }

    @Override
    public void onFavoriteClick(Recipe recipe, int position) {
        String currentUserId = getCurrentUserId(); // your existing method

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        List<String> favorites = (List<String>) snapshot.get("favorites");
                        if (favorites == null) favorites = new ArrayList<>();

                        String recipeId = recipe.getId();
                        boolean isFavorite = favorites.contains(recipeId);

                        if (isFavorite) {
                            favorites.remove(recipeId);
                        } else {
                            favorites.add(recipeId);
                        }

                        List<String> updatedFavorites = new ArrayList<>(favorites); // final copy

                        snapshot.getReference().update("favorites", updatedFavorites)
                                .addOnSuccessListener(unused -> {
                                    // Update adapter with latest list
                                    recipeAdapter.setFavoriteRecipeIds(updatedFavorites);

                                    // Reapply filters after favorite change
                                    applyAllFilters();
                                });
                    }
                });
    }

    // Implement CookingInterruptionCallback methods
    @Override
    public void onCookingInterrupted(InterruptionType type, String message) {
        // No implementation needed
    }

    @Override
    public void onCallEnded() {
        // No implementation needed
    }

    @Override
    public void onScreenStateChanged(boolean isScreenOn) {
        // No implementation needed
    }

    @Override
    public void onHeadphonesStateChanged(boolean areConnected) {
        // No implementation needed
    }

    @Override
    public void onInternetStateChanged(boolean isConnected) {
        // Handle internet state changes
        Log.d("MainActivity", "Internet state changed: " + isConnected);

        // Refresh recipes when internet comes back
        if (isConnected && (allRecipes == null || allRecipes.isEmpty())) {
            loadRecipes();
        }
    }

    @Override
    public void onPowerStateChanged(boolean isPowerConnected) {
        // No implementation needed
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start listening for broadcast events
        broadcastManager.startListening();
        // Only reload if we don't have recipes yet, otherwise just reapply filters
        if (allRecipes == null || allRecipes.isEmpty()) {
            loadRecipes();
        } else {
            applyAllFilters();
        }
    }
}
