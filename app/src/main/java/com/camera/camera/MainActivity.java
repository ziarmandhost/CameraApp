package com.camera.camera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraMetadata;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.camera.camera.ui.base.BaseFragment;
import com.camera.camera.ui.sharedpreferences.ISharedPreferences;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private ConstraintLayout fragmentContainer;
    private ConstraintLayout filterBar;

    private ImageView captureBtn;
    private ImageView changeCamera;
    private ImageView flash;
    private ImageView hdr;
    private ImageView filter;

    private BaseFragment photo;
    private BaseFragment video;
    private BaseFragment qrCode;

    private BaseFragment item;

    private ImageView none;
    private ImageView aqua;
    private ImageView solarize;
    private ImageView blackboard;
    private ImageView whiteboard;
    private ImageView mono;
    private ImageView negative;
    private ImageView posterize;
    private ImageView sepia;

    private TextView timeOutText;

    private ISharedPreferences preferences;
    private int timeOut;
    int time = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        hideSystemUI();
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (!checkCameraPermission()) {
            requestCameraPermission();
        }

        preferences = new ISharedPreferences(this);

        captureBtn = findViewById(R.id.capture);
        changeCamera = findViewById(R.id.changeCamera);

        timeOutText = findViewById(R.id.timeOutText);

        flash = findViewById(R.id.flash);

        hdr = findViewById(R.id.hdr);
        filter = findViewById(R.id.filter);
        filterBar = findViewById(R.id.filterBar);

        none = findViewById(R.id.none);
        aqua = findViewById(R.id.aqua);
        solarize = findViewById(R.id.solarize);
        blackboard = findViewById(R.id.blackboard);
        whiteboard = findViewById(R.id.whiteboard);
        mono = findViewById(R.id.mono);
        negative = findViewById(R.id.negative);
        posterize = findViewById(R.id.posterize);
        sepia = findViewById(R.id.sepia);

        initTabs();

        none.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { item.setFilter(CameraMetadata.CONTROL_EFFECT_MODE_OFF);
            }
        });
        aqua.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { item.setFilter(CameraMetadata.CONTROL_EFFECT_MODE_AQUA); }
        });
        solarize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { item.setFilter(CameraMetadata.CONTROL_EFFECT_MODE_SOLARIZE); }
        });
        blackboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { item.setFilter(CameraMetadata.CONTROL_EFFECT_MODE_BLACKBOARD); }
        });
        whiteboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { item.setFilter(CameraMetadata.CONTROL_EFFECT_MODE_WHITEBOARD); }
        });
        mono.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { item.setFilter(CameraMetadata.CONTROL_EFFECT_MODE_MONO); }
        });
        negative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { item.setFilter(CameraMetadata.CONTROL_EFFECT_MODE_NEGATIVE); }
        });
        posterize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { item.setFilter(CameraMetadata.CONTROL_EFFECT_MODE_POSTERIZE); }
        });
        sepia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { item.setFilter(CameraMetadata.CONTROL_EFFECT_MODE_SEPIA); }
        });

        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timeOut = preferences.getShutterTimeOut();

                String shortcuts[] = getResources().getStringArray(R.array.timeout);
                time = Integer.valueOf(shortcuts[timeOut]);

                if (item.getName().equals("Видео") || item.getName().equals("QRCODE")) {
                    item.onShutter();
                }
                else {
                    if (time == 0) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (preferences.getShutterSound() && !item.getName().equals("Видео")) {
                                    MediaPlayer.create(getApplicationContext(), R.raw.shutter).start();
                                }
                                item.onShutter();
                            }
                        }, 500);
                    }
                    else {
                        timeOutText.setVisibility(View.VISIBLE);
                        new CountDownTimer(Integer.valueOf(shortcuts[timeOut])*1000+1000, 1000) {

                            public void onTick(long millisUntilFinished) {
                                timeOutText.setText(time+"");
                                time--;
                            }

                            public void onFinish() {
                                timeOutText.setVisibility(View.GONE);
                                if (preferences.getShutterSound()) {
                                    MediaPlayer.create(getApplicationContext(), R.raw.shutter).start();
                                }
                                item.onShutter();
                            }

                        }.start();
                    }
                }
            }
        });
        changeCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                item.changeCamera();
            }
        });
        filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (item.getName().equals("Видео") ||  item.getName().equals("QRCODE")) {return;}

                if (filterBar.getVisibility() == View.VISIBLE) {
                    filterBar.setVisibility(View.GONE);
                }
                else {
                    filterBar.setVisibility(View.VISIBLE);
                }
            }
        });

        renderIcons();
    }

    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {return false;}
        return true;
    }
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
                },
                REQUEST_CAMERA_PERMISSION);
    }
    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(), "You need some permissions to use this app!", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void initTabs () {
        fragmentContainer = findViewById(R.id.forTextureView);

        View photoFrg = findViewById(R.id.photoFrg);
        View videoFrg = findViewById(R.id.videoFrg);
        View qrCodeFrg = findViewById(R.id.qrcodeFrg);

        photo = new Photo();
        video = new Video();

        if (preferences.getQrCodeMode()) {
            qrCode = new Qrcode();
            qrCodeFrg.setVisibility(View.VISIBLE);

            qrCodeFrg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    item = qrCode;
                    setFragment(qrCode);
                }
            });
        }

        photoFrg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                item = photo;
                setFragment(photo);
            }
        });
        videoFrg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                item = video;
                setFragment(video);
            }
        });

        item = photo;

        setFragment(photo);
    }

    private void setFragment (BaseFragment fragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.forTextureView, fragment);
        ft.commit();
    }
    private void hideSystemUI () {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    public void openSettings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        view.getContext().startActivity(intent);
    }

    public void renderIcons () {
        if (preferences.getFlashOn()) {
            flash.setImageResource(R.drawable.ic_flash);
        }
        else {
            flash.setImageResource(R.drawable.ic_noflash);
        }

        if (preferences.getHDROn()) {
            hdr.setImageResource(R.drawable.ic_hdr);
        }
        else {
            hdr.setImageResource(R.drawable.ic_nohdr);
        }
    }


    public void changeFlashIcon(View view){
        if (preferences.getFlashOn()) {
            flash.setImageResource(R.drawable.ic_noflash);
            preferences.setFlashOn(false);
            item.turnOffFlashLight();
        }
        else {
            flash.setImageResource(R.drawable.ic_flash);
            preferences.setFlashOn(true);
            item.turnOnFlashLight();
        }
    }
    public void changeHDRIcon(View view){
        if (preferences.getHDROn()) {
            hdr.setImageResource(R.drawable.ic_nohdr);
            preferences.setHDROn(false);
            item.turnHdrOff();
        }
        else {
            hdr.setImageResource(R.drawable.ic_hdr);
            preferences.setHDROn(true);
            item.turnHdrOn();
        }
    }
}