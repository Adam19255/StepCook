package com.adams_maxims_evyatarc.stepcook;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Model class representing a recipe in the application.
 * Stores all recipe data retrieved from Firestore.
 */
public class Recipe {
    @DocumentId
    private String id;
    private String title;
    private String difficulty;
    private String authorId;
    private String authorName;
    private int totalCookTimeMinutes;
    private Timestamp createdDate;
    private String imageUrl;
    private List<Step> steps;

    // Default constructor required for Firestore
    public Recipe() {
        this.steps = new ArrayList<>();
    }

    // Constructor with all fields
    public Recipe(String id, String title, String difficulty, String authorId, String authorName,
                  int totalCookTimeMinutes, Timestamp createdDate, String imageUrl, List<Step> steps) {
        this.id = id;
        this.title = title;
        this.difficulty = difficulty;
        this.authorId = authorId;
        this.authorName = authorName;
        this.totalCookTimeMinutes = totalCookTimeMinutes;
        this.createdDate = createdDate;
        this.imageUrl = imageUrl;
        this.steps = steps != null ? steps : new ArrayList<>();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public int getTotalCookTimeMinutes() {
        return totalCookTimeMinutes;
    }

    public void setTotalCookTimeMinutes(int totalCookTimeMinutes) {
        this.totalCookTimeMinutes = totalCookTimeMinutes;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

    /**
     * Formats the total cook time into a readable string
     */
    @Exclude
    public String getFormattedCookTime() {
        int minutes = totalCookTimeMinutes;

        // Calculate days
        int days = minutes / (24 * 60);

        // Calculate remaining hours after removing days
        int remainingMinutes = minutes % (24 * 60);
        int hours = remainingMinutes / 60;

        // Calculate remaining minutes
        minutes = remainingMinutes % 60;

        StringBuilder sb = new StringBuilder();

        if (days > 0) {
            sb.append(days).append("d ");
        }

        if (hours > 0 || days > 0) {
            sb.append(hours).append("h ");
        }

        if (minutes > 0 || (hours == 0 && days == 0)) {
            // Show minutes if there are any or if both hours and days are 0
            sb.append(minutes).append("m");
        }

        return sb.toString().trim();
    }

    /**
     * Get formatted creation date
     */
    @Exclude
    public String getFormattedDate() {
        if (createdDate == null) return "";
        Date date = createdDate.toDate();
        android.text.format.DateFormat df = new android.text.format.DateFormat();
        return df.format("MMM dd, yyyy", date).toString();
    }

    /**
     * Step class to represent a single step in a recipe
     */
    public static class Step {
        private String description;
        private int order;
        private Integer timerMinutes;

        // Default constructor required for Firestore
        public Step() {
        }

        public Step(String description, int order, Integer timerMinutes) {
            this.description = description;
            this.order = order;
            this.timerMinutes = timerMinutes;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }

        public Integer getTimerMinutes() {
            return timerMinutes;
        }

        public void setTimerMinutes(Integer timerMinutes) {
            this.timerMinutes = timerMinutes;
        }

        /**
         * Formats the timer minutes into a readable string if they exist
         */
        @Exclude
        public String getFormattedTime() {
            if (timerMinutes == null || timerMinutes <= 0) {
                return "";
            }

            int minutes = timerMinutes;
            int hours = minutes / 60;
            minutes = minutes % 60;

            if (hours > 0) {
                return hours + "h " + (minutes > 0 ? minutes + "m" : "");
            } else {
                return minutes + "m";
            }
        }
    }
}