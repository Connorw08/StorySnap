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
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ListView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PhotoTaggerActivity extends AppCompatActivity {
    private final String API_KEY = "KEY HIDDEN";
    private SQLiteDatabase mydb;
    private int id = 0;
    private String currentDateTime;
    private PhotoListAdapter adapter;
    private ArrayList<PhotoItem> photoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_tagger);
        databaseWork();

        photoList = new ArrayList<>();
        adapter = new PhotoListAdapter(this, R.layout.photo_list_item, photoList);

        ListView listView = findViewById(R.id.photoListView);
        listView.setAdapter(adapter);

    }

    void databaseWork() {
        mydb = this.openOrCreateDatabase("mydb", Context.MODE_PRIVATE, null);

        mydb.execSQL("CREATE TABLE IF NOT EXISTS Photos (ID INTEGER PRIMARY KEY AUTOINCREMENT, IMAGE BLOB, DATE DATETIME, TAGS TEXT)");
    }
    public void takePicture(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, 212);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Bitmap b = (Bitmap) data.getExtras().get("data");
        ImageView im = findViewById(R.id.curimg);

        im.setImageBitmap(b);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Retrieve tags using Vision API
                    String autoTags = myVisionTester(b);

                    // Update UI on the main thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            EditText tagText = findViewById(R.id.tags);
                            tagText.setText(autoTags);
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
                Log.d("Skecth Tagger", allTags.toString());
            });

            // If no tags were added with > 0.85 confidence, add only the first tag
            if (tags.isEmpty() && !allTags.isEmpty()) {
                tags.add(allTags.get(0));
            }
        }
        return String.join(", ", tags); // Convert tags to a comma-separated string
    }

    public void save(View view) {
        ImageView mcas = findViewById(R.id.curimg);
        Bitmap b = ((BitmapDrawable) mcas.getDrawable()).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] ba = stream.toByteArray();

        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy - h a");
        currentDateTime = sdf.format(new Date());

        EditText myEditText =  (EditText) findViewById(R.id.tags);
        String tags = myEditText.getText().toString();
        Log.d("212", tags);

        ContentValues cv = new ContentValues();
        cv.put("IMAGE", ba);
        cv.put("DATE", currentDateTime);
        cv.put("TAGS", tags);
        mydb.insert("Photos", null, cv);
        mcas.setImageResource(R.drawable.border_drawable);
        myEditText.setText("");
    }

    public void find(View view) {
        photoList.clear(); // Clear previous search results
        EditText myEditText = findViewById(R.id.search_tags);
        String tags = myEditText.getText().toString();
        String[] tagsArray = tags.split("\\s*,\\s*");

        String query = "SELECT * FROM Photos WHERE ";
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
        Intent intent = new Intent(PhotoTaggerActivity.this, MainActivity.class);
        startActivity(intent);
    }
}

