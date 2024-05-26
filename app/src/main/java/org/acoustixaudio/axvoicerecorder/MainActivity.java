package org.acoustixaudio.axvoicerecorder;

import static android.os.Environment.DIRECTORY_RECORDINGS;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.shajikhan.ladspa.amprack.AudioEngine;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.acoustixaudio.axvoicerecorder.databinding.ActivityMainBinding;
import org.json.JSONException;
import org.json.JSONObject;

import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Objects;
import java.util.StringJoiner;

import android.Manifest;
import android.widget.Toolbar;

public class MainActivity extends AppCompatActivity {
    public static String TAG = "MainActivity";
    private static final int AUDIO_EFFECT_REQUEST = 0;
    boolean running = false;
    String dir, filename ;
    JSONObject allPlugins;
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    public static Context context;
    public static MainActivity mainActivity;
    String [] factoryPresets;
    RecyclerView recyclerView;
    DataAdapter dataAdapter;
    JSONObject ampModels, availablePlugins, availablePluginsLV2;
    static String[] sharedLibraries;
    static String[] sharedLibrariesLV2;
    Spinner spinner ;

    static {
        System.loadLibrary("amprack");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = this;
        context = this;

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AudioEngine.setExportFormat(2);
        dir = getExternalFilesDir("Recordings").getPath();
        File folder = getExternalFilesDir(DIRECTORY_RECORDINGS);
        if (! folder.exists()) {
            if (!folder.mkdirs()) {
                Toast.makeText(context, "Unable to create recording files directory", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, String.format("created folder: %s", folder.getAbsolutePath()));
            }
        } else {
            Log.d(TAG, String.format ("folder exists: %s", folder.getAbsolutePath()));
        }

        Log.d(TAG, String.format ("[dir]: %s", dir));
        ToggleButton record = findViewById(R.id.record);
        record.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                buttonView.setCompoundDrawables(null, null, null, null);
                if (isChecked) {
                    buttonView.setButtonDrawable(getResources().getDrawable(R.drawable.stop1));

                    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_HH.mm.ss");
                    Date date = new Date();
                    filename = new StringJoiner("/").add (dir).add (formatter.format(date)).toString() + ".mp3";
                    AudioEngine.setFileName(filename);
                    Log.d(TAG, String.format ("[filename]: %s", filename));

                    startEffect();
                    AudioEngine.toggleRecording(true);
                } else {
                    buttonView.setButtonDrawable(getResources().getDrawable(R.drawable.record));
                    stopEffect();
                }
            }
        });

        ToggleButton preview = findViewById(R.id.preview);
        preview.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                buttonView.setCompoundDrawables(null, null, null, null);
                if (isChecked) {
                    buttonView.setButtonDrawable(getResources().getDrawable(R.drawable.mute));
                    AudioEngine.setOutputVolume(1f);
                } else {
                    buttonView.setButtonDrawable(getResources().getDrawable(R.drawable.preview));
                    AudioEngine.setOutputVolume(0f);
                }
            }
        });

        ToggleButton pause = findViewById(R.id.pause);
        pause.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                buttonView.setCompoundDrawables(null, null, null, null);
                if (isChecked) {
                    buttonView.setButtonDrawable(getResources().getDrawable(R.drawable.play));
                    AudioEngine.bypass(true);
                    AudioEngine.setInputVolume(0f);
                } else {
                    buttonView.setButtonDrawable(getResources().getDrawable(R.drawable.pause));
                    AudioEngine.bypass(false);
                    AudioEngine.setInputVolume(1f);
                }

            }
        });

        setSupportActionBar(binding.toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        Button test = findViewById(R.id.test);
        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, String.format ("[get preset]: %s", dataAdapter.getPreset()));
            }
        });
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

        factoryPresets = dataAdapter.getFactoryPresets();
        Log.d(TAG, String.format ("factory presets: %s", factoryPresets));
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(mainActivity,
                android.R.layout.simple_spinner_item, factoryPresets);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner = findViewById(R.id.presets);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selection = spinner.getSelectedItemPosition();
                if (selection < factoryPresets.length) {
                    JSONObject preset = loadJSONFromAssetFile(context, new StringBuffer("presets/").append(factoryPresets [selection]).toString());
                    Log.d(TAG, String.format("%d: %s", selection, preset));
                    recyclerView.scrollToPosition(8);
                    if (preset != null)
                        dataAdapter.loadPreset(preset);
                    recyclerView.scrollToPosition(0);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        AudioEngine.create();
        AudioEngine.popFunction(); // this disables the meter output
        AudioEngine.setLibraryPath(getApplicationInfo().nativeLibraryDir);
        AudioEngine.setLazyLoad(true);

        AudioEngine.setOutputVolume(0f);

        availablePlugins = loadJSONFromAssetFile(this, "all_plugins.json");
        availablePluginsLV2 = loadJSONFromAssetFile(this, "lv2_plugins.json");
        ampModels = loadJSONFromAssetFile(this, "amps.json");

        sharedLibraries = context.getResources().getStringArray(R.array.ladspa_plugins);
        sharedLibrariesLV2 = context.getResources().getStringArray(R.array.lv2_plugins);

        AudioEngine.setMainActivityClassName("org/acoustixaudio/axvoicerecorder/MainActivity");
        loadPlugins();
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

    public static void toast(String text) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    private void startEffect() {
        Log.d(TAG, "Attempting to start");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (!isRecordPermissionGranted()) {
            requestRecordPermission();
            return;
        }

        running = AudioEngine.setEffectOn(true);
    }

    private void stopEffect() {
        if (!running) return;

        Log.d(TAG, "Playing, attempting to stop, state: " + running);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        running = AudioEngine.setEffectOn(false);
    }

    private boolean isRecordPermissionGranted() {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED);
    }

    private void requestRecordPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                AUDIO_EFFECT_REQUEST);
    }

    static public JSONObject loadJSONFromAssetFile(Context context, String filename) {
        String json = null;
        try {
            InputStream is = context.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            Log.e(TAG, "loadJSONFromAsset: unable to parse json " + filename, ex);
            return null;
        }

        JSONObject jsonObject = null ;
        try {
            jsonObject = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "loadJSONFromAsset: cannot parse json " + filename, e);
        }

        return jsonObject;
    }

    public void addPluginToRack(int pluginID) {
        int library = pluginID / 100;
        int plug = pluginID - (library * 100);
        Log.d(TAG, "addPluginToRack: loading from " + sharedLibraries.length + " LADSPA and " + sharedLibrariesLV2.length + " LV2 plugins");
        Log.d(TAG, "Adding plugin: " + library + ": " + plug);
        int ret = -1;

        if (library > 149 /* because we have 149 lADSPA libraries */) {
            ret = AudioEngine.addPluginLazyLV2(sharedLibrariesLV2[library - sharedLibraries.length], plug);
        } else
            ret = AudioEngine.addPluginLazy(sharedLibraries[library], plug);

        dataAdapter.addItem(pluginID, ret);
    }

    void printPlugins () {
        JSONObject plugins = mainActivity.availablePluginsLV2 ;
        Iterator<String> keys = plugins.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            try {

                if (plugins.get(key) instanceof JSONObject) {
                    JSONObject object = plugins.getJSONObject(key);
                    String name = object.getString("name");
                    String id = object.getString("id");
                    Log.d(TAG, String.format ("[LV2 plugin] %s: %s", id, name));
//                    mainActivity.pluginDialogAdapter.addItem(Integer.parseInt(key), name, Integer.parseInt(id));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        plugins = mainActivity.availablePlugins ;
        keys = plugins.keys();

        while (keys.hasNext()) {
            String key = keys.next();
            try {
                if (plugins.get(key) instanceof JSONObject) {
                    JSONObject object = plugins.getJSONObject(key);
                    String name = object.getString("name");
                    String id = object.getString("id");
                    Log.d(TAG, "[LADSPA plugin]: " + name + ": " + id);
//                    mainActivity.pluginDialogAdapter.addItem(Integer.parseInt(key), name, Integer.parseInt(id));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getLV2Info (String libraryName, String plugin) {
        String pluginName ;
        if (plugin.indexOf("#") != -1)
            pluginName = plugin.split("#")[1];
        else {
            String [] p = plugin.split("/");
            pluginName = p [p.length -1];
        }

        Log.d(TAG, "getLV2Info: lv2/" + libraryName + "/" + pluginName + ".json");
        JSONObject jsonObject = loadJSONFromAssetFile(context, "lv2/" + libraryName + "/" + pluginName + ".json");
        return jsonObject.toString();
    }

    public void loadPlugins () {
        allPlugins = loadJSONFromAssetFile(context, "voice.json");
        Iterator<String> keys = allPlugins.keys();

        int index = 0;
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                if (allPlugins.get(key) instanceof JSONObject) {
                    JSONObject object = allPlugins.getJSONObject(key);
                    String name = object.getString("name");
                    addPluginToRack (Integer.parseInt(key));

                    AudioEngine.togglePlugin(index, false);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            index ++ ;
        }
    }
}