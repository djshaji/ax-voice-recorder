package org.acoustixaudio.axvoicerecorder;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;

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


    }

    @Override
    public int getItemCount() {
        return plugins.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ArrayList <Slider> sliders ;
        public LinearLayout linearLayout ;
        public LinearLayout root ;
        public TextView pluginName ;
        public SwitchMaterial switchMaterial ;

        public ViewHolder(View view) {
            super(view);

            sliders = new ArrayList<>();
            root = (LinearLayout) itemView ;
            linearLayout = root.findViewById(R.id.controls_layout);
            pluginName = root.findViewById(R.id.name);
            switchMaterial = root.findViewById(R.id.toggle);
        }
    }

    void addItem(int pluginID, int index) {
        plugins.add(pluginID);
        notifyItemInserted(index);
    }

    @Override
    public long getItemId(int position) {
        return plugins.get(position);
    }

}
