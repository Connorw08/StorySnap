package com.example.assignment4;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SketchTaggerActivity extends AppCompatActivity {

    private final String API_KEY = "KEY HIDDEN";
    private SQLiteDatabase mydb;
    private int id = 0;
    private String currentDateTime;
    private PhotoListAdapter adapter;
    private ArrayList<PhotoItem> photoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sketch_tagger);
        databaseWork();

        photoList = new ArrayList<>();
        adapter = new PhotoListAdapter(this, R.layout.photo_list_item, photoList);

        ListView listView = findViewById(R.id.photoListView);
        listView.setAdapter(adapter);

    }

    void databaseWork() {
        mydb = this.openOrCreateDatabase("mydb", Context.MODE_PRIVATE, null);

        mydb.execSQL("CREATE TABLE IF NOT EXISTS Drawings (ID INTEGER PRIMARY KEY AUTOINCREMENT, IMAGE BLOB, DATE DATETIME, TAGS TEXT)");
    }

    private String myVisionTester(Bitmap bitmap) throws IOException {
        // Encode image
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bout);
        Image myImage = new Image();
        myImage.encodeContent(bout.toByteArray());

        // Prepare AnnotateImageRequest
        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
        annotateImageRequest.setImage(myImage);
        Feature f = new Feature();
        f.setType("LABEL_DETECTION");
        f.setMaxResults(5);
        annotateImageRequest.setFeatures(List.of(f));

        // Build the Vision API client
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        Vision vision = new Vision.Builder(httpTransport, jsonFactory, null)
                .setVisionRequestInitializer(new VisionRequestInitializer(API_KEY))
                .build();

        // Execute the request
        BatchAnnotateImagesRequest batchRequest = new BatchAnnotateImagesRequest();
        batchRequest.setRequests(List.of(annotateImageRequest));
        BatchAnnotateImagesResponse response = vision.images().annotate(batchRequest).execute();

        List<String> tags = new ArrayList<>();
        if (response.getResponses() != null && !response.getResponses().isEmpty()) {
            List<String> allTags = new ArrayList<>();

            response.getResponses().get(0).getLabelAnnotations().forEach(label -> {
                if (label.getScore() != null) {
                    allTags.add(label.getDescription());
                    if (label.getScore() > 0.85) {
                        tags.add(label.getDescription());
                    }
                }
            });

            // If no tags were added with > 0.85 confidence, add only the first tag
            if (tags.isEmpty() && !allTags.isEmpty()) {
                tags.add(allTags.get(0));
            }
        }
        return String.join(", ", tags); // Convert tags to a comma-separated string
    }

    public void save(View view) {
        DrawView mcas = findViewById(R.id.draw);
        Bitmap b = mcas.getBitmap(); // Retrieve the drawing as a bitmap
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] ba = stream.toByteArray();

        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy - h a");
        currentDateTime = sdf.format(new Date());

        // Retrieve user-defined tags from EditText
        EditText tagText = findViewById(R.id.tags);
        String userTags = tagText.getText().toString();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Retrieve tags from Vision API
                    String autoTags = myVisionTester(b);

                    // Combine user-defined tags with auto-generated tags
                    String combinedTags;
                    if (userTags.isEmpty()) {
                        combinedTags = autoTags;
                    } else {
                        combinedTags = userTags + ", " + autoTags;
                    }

                    // Update UI on the main thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tagText.setText(combinedTags);

                            // Save to database
                            ContentValues cv = new ContentValues();
                            cv.put("IMAGE", ba);
                            cv.put("DATE", currentDateTime);
                            cv.put("TAGS", combinedTags);
                            mydb.insert("Drawings", null, cv);
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void resetView(View view) {
        DrawView DrawView = findViewById(R.id.draw);
        DrawView.clear();
        EditText myEditText = findViewById(R.id.tags);
        myEditText.setText("");
    }

    public void find(View view) {
        photoList.clear(); // Clear previous search results
        EditText myEditText = findViewById(R.id.search_tags);
        String tags = myEditText.getText().toString();
        String[] tagsArray = tags.split("\\s*,\\s*");

        String query = "SELECT * FROM Drawings WHERE ";
        for (int i = 0; i < tagsArray.length; i++) {
            query += "TAGS LIKE '%" + tagsArray[i] + "%'";
            if (i < tagsArray.length - 1) {
                query += " OR ";
            }
        }
        query += " ORDER BY ID DESC;";

        Cursor cursor = mydb.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                byte[] imageBytes = cursor.getBlob(1);
                String date = cursor.getString(2);
                String tagsFound = cursor.getString(3);

                // Add the photo item to the list
                photoList.add(new PhotoItem(imageBytes, date, tagsFound));
            } while (cursor.moveToNext());
        }
        cursor.close();

        adapter.notifyDataSetChanged(); // Refresh the adapter to display new search results
    }

    public void goBack(View view) {
        Intent intent = new Intent(SketchTaggerActivity.this, MainActivity.class);
        startActivity(intent);
    }
}

