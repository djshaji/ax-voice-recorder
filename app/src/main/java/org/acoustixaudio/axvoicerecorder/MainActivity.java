package org.acoustixaudio.axvoicerecorder;

import static android.os.Environment.DIRECTORY_DOCUMENTS;
import static android.os.Environment.DIRECTORY_RECORDINGS;
import static android.os.Environment.getExternalStorageDirectory;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.shajikhan.ladspa.amprack.AudioEngine;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
    MediaPlayer mediaPlayer = null;
    String dir, filename, basename ;
    JSONObject allPlugins;
    ArrayList<String> presetsForAdapter ;
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    public static Context context;
    public static MainActivity mainActivity;
    TextView lastFilename;
    String [] factoryPresets;
    String presetsDir ;
    RecyclerView recyclerView;
    DataAdapter dataAdapter;
    JSONObject ampModels, availablePlugins, availablePluginsLV2;
    static String[] sharedLibraries;
    static String[] sharedLibrariesLV2;
    Spinner spinner ;
    ArrayAdapter<String> adapter ;
    static boolean prepared = false ;

    static {
        System.loadLibrary("amprack");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = this;
        context = this;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mediaPlayer = new MediaPlayer(this);
        }
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

        presetsDir = getExternalFilesDir(DIRECTORY_DOCUMENTS).getPath();
        File pD = getExternalFilesDir(DIRECTORY_DOCUMENTS);
        if (! pD.exists()) {
            if (!pD.mkdirs()) {
                Toast.makeText(context, "cannot create preset folder", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(TAG, String.format ("[preset dir]: %s", presetsDir));
        }

        Log.d(TAG, String.format ("[dir]: %s", dir));
        ToggleButton record = findViewById(R.id.record);
        lastFilename = findViewById(R.id.last_filename);
        ToggleButton lastPlayPause = findViewById(R.id.last_play);

        lastFilename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lastPlayPause.performClick();
            }
        });

        Button lastShare = findViewById(R.id.share_last);

        lastShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (filename != null)
                    shareFile(new File(filename));
            }
        });
        lastPlayPause.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (filename != null && ! mediaPlayer.isPlaying()) {
                        if (mediaPlayer != null)
                            mediaPlayer.start();
                        buttonView.setBackground(getResources().getDrawable(R.drawable.baseline_pause_24));
                    }
                } else {
                    if (mediaPlayer != null)
                        mediaPlayer.pause();
                    buttonView.setBackground(getResources().getDrawable(R.drawable.baseline_play_arrow_24));
                }
            }
        });

        MediaPlayer.OnCompletionListener completionListener = new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.i(TAG, "onCompletion: ");
                lastPlayPause.setChecked(false);
                    /*
                    try {
                        mediaPlayer.setDataSource(filename + ".mp3");
                        mediaPlayer.prepare();
                    } catch (IOException e) {
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "onCheckedChanged: ", e);
                        return;
                    }

                     */
            }
        } ;

        MediaPlayer.OnPreparedListener onPreparedListener = new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.d(TAG, String.format("media player: prepared"));
                prepared = true;
            }
        } ;

        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(completionListener);
            mediaPlayer.setOnPreparedListener(onPreparedListener);

        }
        
        LinearLayout lastRecordedBox = findViewById(R.id.last_recorded_box);

        record.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                buttonView.setCompoundDrawables(null, null, null, null);
                if (isChecked) {
                    if (mediaPlayer != null && mediaPlayer.isPlaying())
                        lastPlayPause.setChecked(false);

                    buttonView.setButtonDrawable(getResources().getDrawable(R.drawable.stop1));

                    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_HH.mm.ss");
                    Date date = new Date();
                    basename = formatter.format(date);
                    filename = new StringJoiner("/").add (dir).add (basename).toString();
                    AudioEngine.setFileName(filename);
                    Log.d(TAG, String.format ("[filename]: %s", filename));

                    startEffect();
                    AudioEngine.toggleRecording(true);
                    lastRecordedBox.setVisibility(View.GONE);
                } else {
                    buttonView.setButtonDrawable(getResources().getDrawable(R.drawable.record));
                    stopEffect();

                    lastFilename.setText(new File (filename).getName());
                    lastRecordedBox.setVisibility(View.VISIBLE);

                    if (mediaPlayer != null && prepared) {
                        if (mediaPlayer.isPlaying())
                            mediaPlayer.stop();
                        mediaPlayer.release();
                        prepared = false;
                    }

                    if (mediaPlayer != null) {
                        mediaPlayer = new MediaPlayer(context);
                        mediaPlayer.setOnCompletionListener(completionListener);
                        mediaPlayer.setOnPreparedListener(onPreparedListener);
                        try {
                            mediaPlayer.setDataSource(filename + ".mp3");
                            mediaPlayer.prepare();
                        } catch (IOException e) {
                            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "onCheckedChanged: ", e);
                        }
                    }
                }
            }
        });

        Button renameLast = findViewById(R.id.last_edit);
        renameLast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                renameFile(basename, -1);
            }
        });

        Button deletePreset = findViewById(R.id.delete_preset);
        deletePreset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Delete this preset?").setMessage(spinner.getSelectedItem().toString());
                builder.setPositiveButton("Delete preset", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        deleteUserPreset();
                    }
                });

                builder.setNegativeButton("Cancel", null);
                builder.show();
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

        Button recordings = findViewById(R.id.recordings);
        recordings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, Recordings.class);
                startActivity(intent);
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

        presetsForAdapter = new ArrayList<>(Arrays.asList(factoryPresets));
        File [] userPresets = getExternalFilesDir(DIRECTORY_DOCUMENTS).listFiles();
        Log.d(TAG, String.format ("[user presets]: %d", userPresets.length));
        for (File file: userPresets) {
            presetsForAdapter.add(file.getName());
        }

        spinner = findViewById(R.id.presets);
        adapter = new ArrayAdapter<String>(mainActivity,
                android.R.layout.simple_spinner_item, presetsForAdapter);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        Button addPreset = findViewById(R.id.add_preset);
        addPreset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserPreset();
            }
        });

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
                } else {
                    try {
                        String preset = Files.readAllLines (Paths.get (new StringJoiner ("/").add(getExternalFilesDir(DIRECTORY_DOCUMENTS).getPath()).add((CharSequence) spinner.getAdapter().getItem(selection)).toString())).get(0).toString();
                        Log.d(TAG, String.format ("[user preset] %d: %s", selection, preset));
                        dataAdapter.loadPreset(preset);
                    } catch (IOException e) {
                        Log.e(TAG, "onItemSelected: ", e);
                    }
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
        AudioEngine.pushToLockFreeBeforeOutputVolumeAaaaaargh(true);

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

    public void saveUserPreset () {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        LinearLayout linearLayout = (LinearLayout) inflater.inflate(R.layout.get_filename, null);
        TextView textView = linearLayout.findViewById(R.id.filename);
        builder.setView(linearLayout)
               .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int id) {
                       CharSequence filename = textView.getText() ;
                       if (filename.equals("") || filename == null)
                           return;

                       String preset = dataAdapter.getPreset();
                       String fullFilename = new StringJoiner ("/").add(presetsDir).add(filename).toString();
                       Log.d(TAG, String.format ("write preset: %s", fullFilename));
                       File file = new File(dir, String.valueOf(fullFilename));
                       FileWriter writer = null;
                       if (! Objects.requireNonNull(getExternalFilesDir(DIRECTORY_DOCUMENTS)).exists())
                           Log.e(TAG, "onClick: presets directory does not exist!" );
                       if (writeFile(fullFilename, preset)) {
                           Toast.makeText(MainActivity.this, "Saved preset to file: " + filename, Toast.LENGTH_SHORT).show();
                           addToSpinner(String.valueOf(filename));
                       }
                   }
               })
               .setNegativeButton("Cancel", null);

        builder.show();
    }

    public boolean writeFile (String filename, String preset) {
        File myFile = new File(filename);

        try {
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            OutputStreamWriter myOSW = new OutputStreamWriter(fOut);
            myOSW.append(preset);
            myOSW.close();
            fOut.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "writeFile: ", e);
            Toast.makeText(this, "cannot create preset " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public void addToSpinner (String name) {
        presetsForAdapter.add(name);
        adapter = new ArrayAdapter<String>(mainActivity,
                android.R.layout.simple_spinner_item, presetsForAdapter);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setSelection(adapter.getCount() - 1);
    }

    public void deleteUserPreset () {
        if (spinner.getSelectedItemPosition() < factoryPresets.length) {
            Toast.makeText(context, "Factory preset cannot be deleted", Toast.LENGTH_SHORT).show();
            return;
        }

        String preset = spinner.getSelectedItem().toString();
        File file = new File(new StringJoiner("/").add (getExternalFilesDir(DIRECTORY_DOCUMENTS).getPath()).add("/").add (preset).toString());
        Log.d(TAG, String.format ("[delete]: %s", file.getAbsolutePath()));
        if (!file.delete())
            Toast.makeText(context, "Cannot delete preset: " + preset, Toast.LENGTH_SHORT).show();
        else {
            Toast.makeText(context, "Preset deleted", Toast.LENGTH_SHORT).show();
            presetsForAdapter.remove(preset);
            adapter = new ArrayAdapter<String>(mainActivity,
                    android.R.layout.simple_spinner_item, presetsForAdapter);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            spinner.setSelection(0);
        }

    }

    public static void shareFile(File file) {
        Intent intentShareFile = new Intent(Intent.ACTION_SEND);
        Uri contentUri = null;
        try {
            contentUri = FileProvider.getUriForFile(context, "org.acoustixaudio.axvoicerecorder.fileprovider", file);
        } catch (IllegalArgumentException illegalArgumentException) {
            Log.e(TAG, "shareFile: ", illegalArgumentException);
            Toast.makeText(context, illegalArgumentException.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        intentShareFile.setType("audio/*");
        intentShareFile.putExtra(Intent.EXTRA_STREAM, contentUri);

        intentShareFile.putExtra(Intent.EXTRA_SUBJECT,
                "Sharing Audio File...");
        intentShareFile.putExtra(Intent.EXTRA_TEXT, context.getResources().getString(R.string.app_name) + " recorded audio ...");

        intentShareFile.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intentShareFile, "Share Audio File"));

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mediaPlayer != null)
            mediaPlayer.stop();
    }

    public void renameFile (String oldName, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = mainActivity.getLayoutInflater();
        LinearLayout linearLayout = (LinearLayout) inflater.inflate(R.layout.get_filename, null);
        EditText textView = linearLayout.findViewById(R.id.filename);
        TextView title = linearLayout.findViewById(R.id.preset_name);
        title.setText("Enter filename");
        textView.setText(oldName);

        builder.setView(linearLayout)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        CharSequence filename = textView.getText() ;
                        if (filename.equals("") || filename == null || filename.equals(oldName))
                            return;

                        File file = new File(new StringJoiner("/").add (mainActivity.getExternalFilesDir(Environment.DIRECTORY_RECORDINGS).getAbsolutePath()).add (oldName).toString() + ".mp3");
                        file.renameTo(new File(new StringJoiner("/").add (mainActivity.getExternalFilesDir(Environment.DIRECTORY_RECORDINGS).getAbsolutePath()).add (filename).toString() + ".mp3"))  ;
                        mainActivity.lastFilename.setText(filename);
                    }
                })
                .setNegativeButton("Cancel", null);

        builder.show();
    }

}