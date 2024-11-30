package com.example.assignment4;

public class PhotoItem {
    public byte[] image;
    public String date;
    public String tags;

    public PhotoItem(byte[] image, String date, String tags) {
        this.image = image;
        this.date = date;
        this.tags = tags;
    }
}
