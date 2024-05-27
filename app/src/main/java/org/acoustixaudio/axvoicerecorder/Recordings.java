package org.acoustixaudio.axvoicerecorder;

import android.os.Bundle;
import android.os.Environment;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class Recordings extends AppCompatActivity {
    Tracks tracks ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recordings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tracks = new Tracks();
        tracks.onCreateView(getLayoutInflater(), null, null);
        ConstraintLayout parent = findViewById(R.id.main);
        parent.addView(tracks.constraintLayout);

        tracks.context = this ;
        tracks.onViewCreated(tracks.constraintLayout, null);
//        populateRecordings();
        tracks.load(Objects.requireNonNull(getExternalFilesDir(Environment.DIRECTORY_RECORDINGS)));
    }

    void populateRecordings () {
        for (File f: Objects.requireNonNull(getExternalFilesDirs("Recordings")[0].listFiles())) {
            tracks.tracksAdapter.add(f.getName());
        }
    }
}