package com.camera.camera;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.camera.camera.ui.sharedpreferences.ISharedPreferences;

public class SettingsActivity extends AppCompatActivity {

    Switch gridlines;
    Switch sound;
    Switch sdcard;
    Switch qrcode;

    Spinner spinner;

    ISharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferences = new ISharedPreferences(this);

        gridlines = findViewById(R.id.gridlines);
        sound = findViewById(R.id.shutterSound);
        sdcard = findViewById(R.id.sdcard);
        qrcode = findViewById(R.id.qrcode);
        spinner = (Spinner) findViewById(R.id.spinner);

        gridlines.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) { preferences.setGridlinesShow(isChecked); }
        });
        sound.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) { preferences.setShutterSound(isChecked); }
        });
        sdcard.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) { preferences.setSaveToSD(isChecked); }
        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
               preferences.setShutterTimeOut(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // can leave this empty
            }
        });


        //Modes
        qrcode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) { preferences.setQrCodeMode(isChecked); }
        });

        renderButtonsState();
    }

    private void renderButtonsState () {
        gridlines.setChecked(preferences.getGridlinesShow());
        sound.setChecked(preferences.getShutterSound());
        sdcard.setChecked(preferences.getSaveToSD());
        qrcode.setChecked(preferences.getQrCodeMode());
        spinner.setSelection(Integer.valueOf(preferences.getShutterTimeOut()));
    }

    public void goBack(View view){
        Intent intent = new Intent(this, MainActivity.class);
        view.getContext().startActivity(intent);
    }
}
