package com.example.assignment4;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PhotoCheckAdapter extends ArrayAdapter<PhotoCheckItem> {
    private int maxSelections = 3;
    private TextView selectedTagsTextView;

    public PhotoCheckAdapter(Context context, int resource, ArrayList<PhotoCheckItem> objects, TextView selectedTagsTextView) {
        super(context, resource, objects);
        this.selectedTagsTextView = selectedTagsTextView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.check_photo_list_item, parent, false);
        }

        PhotoCheckItem currentItem = getItem(position);

        ImageView photoImage = convertView.findViewById(R.id.photoImage);
        TextView photoDate = convertView.findViewById(R.id.photoDate);
        TextView photoTags = convertView.findViewById(R.id.photoTags);
        CheckBox checkBox = convertView.findViewById(R.id.photoCheckBox);

        // Convert byte array to Bitmap and set it to the ImageView
        Bitmap bitmap = BitmapFactory.decodeByteArray(currentItem.image, 0, currentItem.image.length);
        photoImage.setImageBitmap(bitmap);

        photoDate.setText(currentItem.date);
        photoTags.setText(currentItem.tags);

        checkBox.setOnCheckedChangeListener(null); // Reset listener to avoid duplicate triggers
        checkBox.setChecked(currentItem.isSelected());

        // Handle CheckBox state
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (getSelectedCount() < maxSelections) {
                    currentItem.setSelected(true);
                    updateSelectedTags();
                } else {
                    checkBox.setChecked(false); // Prevent selecting more than max
                }
            } else {
                currentItem.setSelected(false);
                updateSelectedTags();
            }
        });

        return convertView;
    }

    private int getSelectedCount() {
        // Count the selected items in the adapter
        return (int) getItems().stream().filter(PhotoCheckItem::isSelected).count();
    }

    private void updateSelectedTags() {
        // Collect the tags of selected items and update the TextView
        Set<String> selectedTags = getItems().stream()
                .filter(PhotoCheckItem::isSelected)
                .map(PhotoCheckItem::getTags)
                .collect(Collectors.toSet());
        selectedTagsTextView.setText("You selected: " + String.join(", ", selectedTags));
    }

    private List<PhotoCheckItem> getItems() {
        // Helper method to access all items
        List<PhotoCheckItem> items = new ArrayList<>();
        for (int i = 0; i < getCount(); i++) {
            items.add(getItem(i));
        }
        return items;
    }
}
