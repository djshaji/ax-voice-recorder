package org.acoustixaudio.axvoicerecorder;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.acoustixaudio.axvoicerecorder.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    public String TAG = getClass().getSimpleName();
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    public static Context context ;
    public static MainActivity mainActivity ;
    RecyclerView recyclerView ;
    DataAdapter dataAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = this ;
        context = this;

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ToggleButton record = findViewById(R.id.record);
        record.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                buttonView.setCompoundDrawables(null,null,null,null);
                if (isChecked) {
                    buttonView.setButtonDrawable(getResources().getDrawable(R.drawable.stop1));
                } else {
                    buttonView.setButtonDrawable(getResources().getDrawable(R.drawable.record));
                }
            }
        });

        ToggleButton preview = findViewById(R.id.preview);
        preview.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                buttonView.setCompoundDrawables(null,null,null,null);
                if (isChecked) {
                    buttonView.setButtonDrawable(getResources().getDrawable(R.drawable.mute));
                } else {
                    buttonView.setButtonDrawable(getResources().getDrawable(R.drawable.preview));
                }
            }
        });

        ToggleButton pause = findViewById(R.id.pause);
        pause.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                buttonView.setCompoundDrawables(null,null,null,null);
                if (isChecked) {
                    buttonView.setButtonDrawable(getResources().getDrawable(R.drawable.play));
                } else {
                    buttonView.setButtonDrawable(getResources().getDrawable(R.drawable.pause));
                }

            }
        });

        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.show();

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.fab)
                        .setAction("Action", null).show();
            }
        });

        recyclerView = findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        dataAdapter = new DataAdapter(mainActivity);
        recyclerView.setAdapter(dataAdapter);

        for (int i = 0 ; i < 6 ; i++)
            dataAdapter.addItem(i,i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static void toast (String text) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }
}