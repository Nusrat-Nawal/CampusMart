package com.campusmart.app.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.campusmart.app.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // This activity will be the main hub of the app
        // Once the user is logged in.
    }
}