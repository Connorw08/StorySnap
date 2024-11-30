package com.example.assignment4;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StoryActivity extends AppCompatActivity {

    private SQLiteDatabase mydb;
    private final String URL = "https://api.textcortex.com/v1/texts/social-media-posts/";
    private final String API_KEY = "KEY HIDDEN";
    private ArrayList<PhotoCheckItem> photoList = new ArrayList<>();
    private PhotoCheckAdapter adapter;
    private TextView selectedTagsTextView;
    private TextToSpeech tts = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story);
        CheckBox checkBox = findViewById(R.id.simpleCheckBox);
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> find(null));

        // Initialize ListView and Adapter
        ListView listView = findViewById(R.id.checkPhotoList);
        selectedTagsTextView = findViewById(R.id.selectedTags);

        adapter = new PhotoCheckAdapter(this, R.layout.check_photo_list_item, photoList, selectedTagsTextView);
        listView.setAdapter(adapter);

        // Initialize database (replace with your database helper)
        mydb = openOrCreateDatabase("mydb", MODE_PRIVATE, null);

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.US);
                }
            }
        });


    }

    //    {
//        "context": "string",
//            "formality": "default",
//            "keywords": [
//        "string"
//  ],
//        "max_tokens": 2048,
//            "mode": "twitter",
//            "model": "claude-3-haiku",
//            "n": 1,
//            "source_lang": "en",
//            "target_lang": "en",
//            "temperature": null
//    }

    public void generateStory(View view) {
        // Collect selected tags
        List<String> selectedTags = photoList.stream()
                .filter(PhotoCheckItem::isSelected)
                .map(PhotoCheckItem::getTags)
                .flatMap(tags -> Arrays.stream(tags.split("\\s*,\\s*")))
                .distinct()
                .limit(3)
                .collect(Collectors.toList());

        if (selectedTags.isEmpty()) {
            selectedTagsTextView.setText("No items selected to generate a story");
            return;
        }

        JSONObject data = new JSONObject();
        try {
            data.put("context", "Generate a short story (around 50-100 words) based on these tags: " + String.join(", ", selectedTags));
            data.put("keywords", new JSONArray(selectedTags));
            data.put("max_tokens", "100");
            data.put("mode", "twitter");
            data.put("model", "claude-3-haiku");

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, URL, data, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                TextView textView = findViewById(R.id.result);
                String textResponse = null;
                try {
                    textResponse = response.getJSONObject("data").getJSONArray("outputs").getJSONObject(0).getString("text");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                textView.setText(textResponse);
                Log.d("success", response.toString());
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("error", new String(error.networkResponse.data));

            }
        }){

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Authorization", "Bearer " + API_KEY);
                    return headers;
                }
            };

            Log.d("GenerateStory", "Selected Tags: " + selectedTags);
            Log.d("GenerateStory", "Payload: " + data.toString());
            RequestQueue requestQueue = Volley.newRequestQueue(this);
            requestQueue.add(request);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        TextView myText =  findViewById(R.id.result);
        String text = myText.getText().toString();
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    public void find(View view) {
        photoList.clear(); // Clear previous search results
        EditText searchInput = findViewById(R.id.search_tags);
        String tags = searchInput.getText().toString();
        String[] tagsArray = tags.split("\\s*,\\s*");

        boolean includeSketches = ((CheckBox) findViewById(R.id.simpleCheckBox)).isChecked();

        String queryPhotos = "SELECT IMAGE, DATE, TAGS FROM Photos";
        String querySketches = "SELECT IMAGE, DATE, TAGS FROM Drawings";

        if (!tags.isEmpty()) {
            String whereClause = " WHERE ";
            for (int i = 0; i < tagsArray.length; i++) {
                whereClause += "TAGS LIKE '%" + tagsArray[i] + "%'";
                if (i < tagsArray.length - 1) whereClause += " OR ";
            }
            queryPhotos += whereClause;
            querySketches += whereClause;
        }

        queryPhotos += " ORDER BY ID DESC";
        querySketches += " ORDER BY ID DESC";

        Cursor photoCursor = mydb.rawQuery(queryPhotos, null);
        fetchResults(photoCursor);

        if (includeSketches) {
            Cursor sketchCursor = mydb.rawQuery(querySketches, null);
            fetchResults(sketchCursor);
        }

        adapter.notifyDataSetChanged(); // Refresh the adapter

        // Update the selection message
        updateSelectionMessage();
    }

    private void fetchResults(Cursor cursor) {
        if (cursor.moveToFirst()) {
            do {
                byte[] imageBytes = cursor.getBlob(0);
                String date = cursor.getString(1);
                String tags = cursor.getString(2);

                photoList.add(new PhotoCheckItem(imageBytes, date, tags)); // Add item to list
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    void updateSelectionMessage() {
        List<String> selectedTags = photoList.stream()
                .filter(PhotoCheckItem::isSelected)  // Filter selected items
                .map(PhotoCheckItem::getTags)
                .flatMap(tags -> Arrays.stream(tags.split("\\s*,\\s*")))  // Split tags into individual tags
                .distinct()
                .collect(Collectors.toList());

        if (selectedTags.isEmpty()) {
            selectedTagsTextView.setText("No items selected");
        } else {
            selectedTagsTextView.setText("You selected: " + String.join(", ", selectedTags));
        }
    }

    public void goBack(View view) {
        Intent intent = new Intent(StoryActivity.this, MainActivity.class);
        startActivity(intent);
    }
}