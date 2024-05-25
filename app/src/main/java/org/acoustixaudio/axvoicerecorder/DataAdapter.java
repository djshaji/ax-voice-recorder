package org.acoustixaudio.axvoicerecorder;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.shajikhan.ladspa.amprack.AudioEngine;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public class DataAdapter extends RecyclerView.Adapter <DataAdapter.ViewHolder> {
    MainActivity mainActivity;
    int totalItems = 0;
    String TAG = this.getClass().getSimpleName();
    Context context = null ;
    ArrayList <Integer> plugins = new ArrayList<>();
    ArrayList<ViewHolder> holders = new ArrayList<>();

    public DataAdapter(MainActivity _mainActivity) {
        context = mainActivity = _mainActivity ;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        totalItems++;
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.plugin, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holders.add(holder);
        LinearLayout linearLayout = holder.linearLayout;
        linearLayout.removeAllViews();
        holder.sliders = new ArrayList<>();
        if (linearLayout == null) {
            Log.wtf(TAG, "linear layout for plugin!") ;
            return ;
        }

        holder.switchMaterial.setUseMaterialThemeColors(true);
        holder.showControls.setChecked(false);
        holder.buttonBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.showControls.performClick();
            }
        });

        holder.showControls.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    linearLayout.setVisibility(View.VISIBLE);
                else
                    linearLayout.setVisibility(View.GONE);
            }
        });

        holder.switchMaterial.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AudioEngine.togglePlugin(holder.getAdapterPosition(), isChecked);
            }
        });
        int ID = plugins.get(position);
        JSONObject data = null;
        try {
            data = mainActivity.allPlugins.getJSONObject(String.valueOf(ID));
            Log.d(TAG, "onBindViewHolder: " + data);
        } catch (JSONException e) {
            Log.e(TAG, "onBindViewHolder: ", e);
            return;
        }

        String name;

        try {
             name = data.getString("name");
             holder.pluginName.setText(name);
             JSONObject _controls = data.getJSONObject("controls");
             Iterator<String> keys = _controls.keys();
            Log.d(TAG, String.format ("_controls: %s", _controls));
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject controls = _controls.getJSONObject(key);
                Log.d(TAG, String.format ("[control: %s] %s: %s", name, key, controls));
                double def = controls.getDouble("default");
                double min = controls.getDouble("minimum");
                double max = controls.getDouble("maximum");
                String cname = controls.getString("name");
                if (max < min) {
                    double t = max ;
                    max = min ;
                    min = t ;
                }

                Log.d(TAG, String.format ("[controls]: %f %f %f", def, min, max));
                ArrayList<Integer> index = new ArrayList<>();
                if (controls.get("index") instanceof Integer) {
                    index.add(controls.getInt("index"));
                } else {
                    JSONArray indexes = controls.getJSONArray("index");
                    for (int i = 0; i < indexes.length(); i++) {
                        index.add(indexes.getInt(i));
                    }
                }

                TextView textView = new TextView(mainActivity);
                textView.setText(cname);
                Log.d(TAG, String.format ("[%s]: [%s]", name,cname));
                linearLayout.addView(textView);

                Slider slider = new Slider(mainActivity);
                try {
                    slider.setValue((float) def);
                    slider.setValueTo((float) max);
                    slider.setValueFrom((float) min);
                } catch (IllegalStateException e) {
                    Log.e(TAG, String.format("onBindViewHolder: error setting slider value: %d, %d", min, max), e);
                }

                linearLayout.addView(slider);
                slider.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        for (int control = 0; control < index.size(); control++)
                            AudioEngine.setPluginControl(holder.getAdapterPosition(), index.get(control), slider.getValue());
                    }
                });
            }
        } catch (JSONException e) {
            Log.e(TAG, "onBindViewHolder: ", e);
        }

    }

    @Override
    public int getItemCount() {
        return plugins.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ArrayList <Slider> sliders ;
        public LinearLayout linearLayout, buttonBox ;
        public LinearLayout root ;
        public TextView pluginName ;
        public SwitchMaterial switchMaterial ;
        public ToggleButton showControls ;

        public ViewHolder(View view) {
            super(view);

            sliders = new ArrayList<>();
            root = (LinearLayout) itemView ;
            linearLayout = root.findViewById(R.id.controls_layout);
            buttonBox = root.findViewById(R.id.pl1);
            pluginName = root.findViewById(R.id.name);
            switchMaterial = root.findViewById(R.id.toggle);
            showControls = root.findViewById(R.id.show_controls);
        }
    }

    void addItem(int pluginID, int index) {
        Log.d(TAG, String.format ("adding plugin: %d", pluginID));
        plugins.add(pluginID);
        notifyItemInserted(index);
    }

    @Override
    public long getItemId(int position) {
        return plugins.get(position);
    }

}
