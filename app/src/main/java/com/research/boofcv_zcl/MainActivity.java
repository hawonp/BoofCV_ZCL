package com.research.boofcv_zcl;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    // UI element imports
    Button btn_calibrate;
    Button btn_camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI element initialization
        btn_calibrate = findViewById(R.id.btn_calibrate);
        btn_camera = findViewById(R.id.btn_camera);
    }
}