package com.example.myapplicationtt.models;

public class ModelImageSlider {

    String id;
    String imageUrl;

    public ModelImageSlider() {

    }

    public ModelImageSlider(String imageUrl, String id) {
        this.imageUrl = imageUrl;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
