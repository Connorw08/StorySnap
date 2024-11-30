package com.example.assignment4;

public class PhotoCheckItem {
    public byte[] image;
    public String date;
    public String tags;
    private boolean selected;

    public PhotoCheckItem(byte[] image, String date, String tags) {
        this.image = image;
        this.date = date;
        this.tags = tags;
        this.selected = false; // Default value
    }

    public boolean isSelected() {
        return selected;
    }

    public String getTags() {
        return tags;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
