package com.example.assignment4;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class PhotoListAdapter extends ArrayAdapter<PhotoItem> {

    public PhotoListAdapter(Context context, int resource, ArrayList<PhotoItem> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.photo_list_item, parent, false);
        }

        PhotoItem currentItem = getItem(position);

        ImageView photoImage = convertView.findViewById(R.id.photoImage);
        TextView photoDate = convertView.findViewById(R.id.photoDate);
        TextView photoTags = convertView.findViewById(R.id.photoTags);

        // Convert byte array to Bitmap and set it to the ImageView
        Bitmap bitmap = BitmapFactory.decodeByteArray(currentItem.image, 0, currentItem.image.length);
        photoImage.setImageBitmap(bitmap);

        photoDate.setText(currentItem.date);
        photoTags.setText(currentItem.tags);

        return convertView;
    }
}
