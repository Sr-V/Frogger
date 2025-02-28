package edu.pmdm.frogger.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.SeekBar;
import androidx.appcompat.app.AppCompatActivity;

import edu.pmdm.frogger.R;
import edu.pmdm.frogger.utils.GameAudioManager;

public class SettingsActivity extends AppCompatActivity {

    private SeekBar seekBarMusic;
    private SeekBar seekBarSound;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        seekBarMusic = findViewById(R.id.seekBarMusic);
        seekBarSound = findViewById(R.id.seekBarSound);

        sharedPreferences = getSharedPreferences("AudioSettings", Context.MODE_PRIVATE);

        float musicVolume = sharedPreferences.getFloat("MUSIC_AUDIO_LEVEL", 1.0f);
        float soundVolume = sharedPreferences.getFloat("AUDIO_LEVEL", 1.0f);

        seekBarMusic.setProgress((int) (musicVolume * 100));
        seekBarSound.setProgress((int) (soundVolume * 100));

        seekBarMusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float volume = progress / 100.0f;
                GameAudioManager.getInstance(SettingsActivity.this).MUSIC_VOLUME = volume;
                saveVolumeSettings("MUSIC_AUDIO_LEVEL", volume);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarSound.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float volume = progress / 100.0f;
                GameAudioManager.getInstance(SettingsActivity.this).AUDIO_VOLUME = volume;
                saveVolumeSettings("AUDIO_LEVEL", volume);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        findViewById(R.id.btnBackToMain).setOnClickListener(v ->{
            finish();
        });

    }

    private void saveVolumeSettings(String key, float value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(key, value);
        editor.apply();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}