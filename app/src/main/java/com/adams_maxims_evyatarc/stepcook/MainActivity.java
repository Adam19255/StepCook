package com.adams_maxims_evyatarc.stepcook;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener, RecipeAdapter.OnRecipeClickListener  {

    private ImageView popupMenuButton;
    private EditText searchInput;
    private Button difficultyFilter;
    private Button favoriteFilter;
    private Button cookTimeFilter;
    private Button myRecipesFilter;
    private ImageView addRecipeButton;
    private RecyclerView recipeRecyclerView;
    private FrameLayout loadingLayout;

    // Adapter for recipes
    private RecipeAdapter recipeAdapter;

    // Filter managers
    private FilterManager difficultyFilterManager;
    private FilterManager cookTimeFilterManager;
    private FavoriteFilterManager favoriteFilterManager;
    private MyRecipesFilterManager myRecipesFilterManager;

    // UI utils
    private DialogManager dialogManager;
    private UIHelper uiHelper;

    // Recipe manager
    private RecipeManager recipeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Force Light Mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeManagers();
        setupRecyclerView();
        setupClickListeners();
        loadRecipes();
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

        searchInput.clearFocus();
    }

    private void initializeManagers() {
        uiHelper = new UIHelper(this);
        dialogManager = new DialogManager(this);
        recipeManager = RecipeManager.getInstance();

        difficultyFilterManager = new DifficultyFilterManager(this, difficultyFilter);
        cookTimeFilterManager = new CookTimeFilterManager(this, cookTimeFilter);
        favoriteFilterManager = new FavoriteFilterManager(this, favoriteFilter);
        myRecipesFilterManager = new MyRecipesFilterManager(this, myRecipesFilter);
    }

    private void setupRecyclerView() {
        recipeAdapter = new RecipeAdapter(this);
        recipeAdapter.setOnRecipeClickListener(this);
        recipeRecyclerView.setAdapter(recipeAdapter);
        recipeRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupClickListeners() {
        popupMenuButton.setOnClickListener(v -> showPopupMenu(v));

        addRecipeButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddRecipeActivity.class)));

        difficultyFilter.setOnClickListener(v -> difficultyFilterManager.showFilterDialog());

        favoriteFilter.setOnClickListener(v -> favoriteFilterManager.toggleFilter());

        cookTimeFilter.setOnClickListener(v -> cookTimeFilterManager.showFilterDialog());

        myRecipesFilter.setOnClickListener(v -> myRecipesFilterManager.toggleFilter());
    }

    private void showLoading() {
        loadingLayout.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        loadingLayout.setVisibility(View.GONE);
    }

    /**
     * Load recipes from the database
     */
    private void loadRecipes() {
        showLoading();

        // Fetch recipes from RecipeManager
        recipeManager.getAllRecipes(new RecipeManager.RecipesRetrievalCallback() {
            @Override
            public void onRecipesLoaded(List<Recipe> recipes) {
                // Update the adapter with the loaded recipes
                recipeAdapter.setRecipes(recipes);
                hideLoading();
            }

            @Override
            public void onError(Exception e) {
                hideLoading();

                // Show error message
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

    @Override
    public void onRecipeClick(Recipe recipe) {
        // Handle recipe click event
        Intent intent = new Intent(MainActivity.this, RecipeDetailActivity.class);
        intent.putExtra("RECIPE_ID", recipe.getId());
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(Recipe recipe, int position) {
        // Handle favorite button click
        // This would implement favorite functionality in a future update
        Toast.makeText(this, "Favorite button clicked", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload recipes when returning to this activity
        loadRecipes();
    }
}