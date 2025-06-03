package com.adams_maxims_evyatarc.stepcook;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.ViewHolder> {

    private final Context context;
    private final List<Recipe> fullRecipeList = new ArrayList<>();
    private final List<Recipe> filteredList = new ArrayList<>();
    private OnRecipeClickListener onRecipeClickListener;

    public RecipeAdapter(Context context) {
        this.context = context;
    }

    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
        void onFavoriteClick(Recipe recipe, int position);
    }

    public void setOnRecipeClickListener(OnRecipeClickListener listener) {
        this.onRecipeClickListener = listener;
    }

    public void setRecipes(List<Recipe> recipes) {
        fullRecipeList.clear();
        fullRecipeList.addAll(recipes);
        filteredList.clear();
        filteredList.addAll(recipes);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(fullRecipeList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Recipe recipe : fullRecipeList) {
                if (recipe.getTitle() != null && recipe.getTitle().toLowerCase().contains(lowerQuery)) {
                    filteredList.add(recipe);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recipe_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeAdapter.ViewHolder holder, int position) {
        Recipe recipe = filteredList.get(position);

        holder.recipeTitle.setText(recipe.getTitle());
        holder.recipeCookTime.setText(recipe.getFormattedCookTime());
        holder.recipeDifficulty.setText(recipe.getDifficulty());

        if (recipe.getImageUrl() != null && !recipe.getImageUrl().isEmpty()) {
            Glide.with(context).load(recipe.getImageUrl()).into(holder.recipeImage);
        } else {
            holder.recipeImage.setImageResource(R.drawable.image_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (onRecipeClickListener != null) {
                onRecipeClickListener.onRecipeClick(recipe);
            }
        });

        holder.favoriteRecipe.setOnClickListener(v -> {
            if (onRecipeClickListener != null) {
                onRecipeClickListener.onFavoriteClick(recipe, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView recipeTitle, recipeCookTime, recipeDifficulty;
        ImageView recipeImage, favoriteRecipe;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            recipeTitle = itemView.findViewById(R.id.recipeTitle);
            recipeCookTime = itemView.findViewById(R.id.recipeCookTime);
            recipeDifficulty = itemView.findViewById(R.id.recipeDifficulty);
            recipeImage = itemView.findViewById(R.id.recipeImage);
            favoriteRecipe = itemView.findViewById(R.id.favoriteRecipe);
        }
    }
}
