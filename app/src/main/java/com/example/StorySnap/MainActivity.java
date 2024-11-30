package com.example.assignment4;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private void myVisionTester() {

    }
    public void startPhoto(View view) {
        Intent intent = new Intent(MainActivity.this, PhotoTaggerActivity.class);
        startActivity(intent);
    }

    public void startDraw(View view) {
        Intent intent = new Intent(MainActivity.this, SketchTaggerActivity.class);
        startActivity(intent);
    }

    public void startStory(View view) {
        Intent intent = new Intent(MainActivity.this, StoryActivity.class);
        startActivity(intent);
    }
}