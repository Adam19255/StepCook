package com.adams_maxims_evyatarc.stepcook;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener, RecipeAdapter.OnRecipeClickListener {

    private ImageView popupMenuButton;
    private EditText searchInput;
    private Button difficultyFilter;
    private Button favoriteFilter;
    private Button cookTimeFilter;
    private Button myRecipesFilter;
    private ImageView addRecipeButton;
    private RecyclerView recipeRecyclerView;
    private FrameLayout loadingLayout;

    private RecipeAdapter recipeAdapter;
    private List<Recipe> allRecipes;

    private FilterManager difficultyFilterManager;
    private FilterManager cookTimeFilterManager;
    private FavoriteFilterManager favoriteFilterManager;
    private MyRecipesFilterManager myRecipesFilterManager;

    private DialogManager dialogManager;
    private UIHelper uiHelper;
    private RecipeManager recipeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        popupMenuButton.setOnClickListener(this::showPopupMenu);
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

    private void loadRecipes() {
        showLoading();
        recipeManager.getAllRecipes(new RecipeManager.RecipesRetrievalCallback() {
            @Override
            public void onRecipesLoaded(List<Recipe> recipes) {
                allRecipes = recipes;
                recipeAdapter.setRecipes(allRecipes);
                hideLoading();
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

    @Override
    public void onRecipeClick(Recipe recipe) {
        Intent intent = new Intent(MainActivity.this, RecipeDetailActivity.class);
        intent.putExtra("RECIPE_ID", recipe.getId());
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(Recipe recipe, int position) {
        Toast.makeText(this, "Favorite button clicked", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecipes();
    }
}
