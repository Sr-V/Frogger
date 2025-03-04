package edu.pmdm.frogger.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import androidx.appcompat.app.AppCompatActivity;
import edu.pmdm.frogger.R;
import edu.pmdm.frogger.utils.GameAudioManager;

/**
 * {@code SettingsActivity} permite al usuario ajustar los niveles de audio de la música y los efectos de sonido.
 * La actividad utiliza SeekBars para modificar los volúmenes, guarda los ajustes en SharedPreferences y
 * permite al usuario regresar a la pantalla principal.
 */
public class SettingsActivity extends AppCompatActivity {

    // Controles para ajustar el volumen de la música y los efectos de sonido
    private SeekBar seekBarMusic;
    private SeekBar seekBarSound;
    // SharedPreferences para almacenar la configuración de audio
    private SharedPreferences sharedPreferences;

    /**
     * Método del ciclo de vida que se invoca al crear la actividad.
     * Inicializa la interfaz, configura los SeekBars con los volúmenes almacenados y asigna listeners.
     *
     * @param savedInstanceState Estado previo de la actividad.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Configurar la actividad para mostrarse en pantalla completa y modo inmersivo
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN      // Ocultar la barra de estado
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION  // Ocultar la barra de navegación
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY   // Modo inmersivo sticky
        );

        // Referenciar los SeekBars del layout
        seekBarMusic = findViewById(R.id.seekBarMusic);
        seekBarSound = findViewById(R.id.seekBarSound);

        // Inicializar SharedPreferences para almacenar la configuración de audio
        sharedPreferences = getSharedPreferences("AudioSettings", Context.MODE_PRIVATE);

        // Recuperar los niveles de audio guardados, valor predeterminado 1.0f (máximo)
        float musicVolume = sharedPreferences.getFloat("MUSIC_AUDIO_LEVEL", 1.0f);
        float soundVolume = sharedPreferences.getFloat("AUDIO_LEVEL", 1.0f);

        // Establecer el progreso de los SeekBars basado en los volúmenes (escala de 0 a 100)
        seekBarMusic.setProgress((int) (musicVolume * 100));
        seekBarSound.setProgress((int) (soundVolume * 100));

        // Configurar el listener para el SeekBar de la música
        seekBarMusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            /**
             * Se invoca cuando el progreso del SeekBar cambia.
             *
             * @param seekBar El SeekBar que ha cambiado.
             * @param progress El nuevo valor de progreso.
             * @param fromUser Indica si el cambio fue iniciado por el usuario.
             */
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Calcular el volumen en base al progreso (escala 0.0 a 1.0)
                float volume = progress / 100.0f;
                // Actualizar el volumen de la música en GameAudioManager
                GameAudioManager.getInstance(SettingsActivity.this).MUSIC_VOLUME = volume;
                // Guardar el ajuste de volumen en SharedPreferences
                saveVolumeSettings("MUSIC_AUDIO_LEVEL", volume);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Método requerido por la interfaz, sin implementación necesaria aquí.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Método requerido por la interfaz, sin implementación necesaria aquí.
            }
        });

        // Configurar el listener para el SeekBar de los efectos de sonido
        seekBarSound.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            /**
             * Se invoca cuando el progreso del SeekBar cambia.
             *
             * @param seekBar El SeekBar que ha cambiado.
             * @param progress El nuevo valor de progreso.
             * @param fromUser Indica si el cambio fue iniciado por el usuario.
             */
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Calcular el volumen en base al progreso (escala de 0.0 a 1.0)
                float volume = progress / 100.0f;
                // Actualizar el volumen de los efectos de sonido en GameAudioManager
                GameAudioManager.getInstance(SettingsActivity.this).AUDIO_VOLUME = volume;
                // Guardar el ajuste de volumen en SharedPreferences
                saveVolumeSettings("AUDIO_LEVEL", volume);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Método requerido por la interfaz, sin implementación necesaria aquí.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Método requerido por la interfaz, sin implementación necesaria aquí.
            }
        });

        // Configurar el botón para regresar a la pantalla principal (MainActivity)
        findViewById(R.id.btnBackToMain).setOnClickListener(v -> {
            finish(); // Finaliza la actividad actual
        });

        // Configurar el botón para realizar una prueba de sonido
        findViewById(R.id.imageButton).setOnClickListener(v -> {
            // Obtener la instancia de GameAudioManager y reproducir un sonido de prueba
            GameAudioManager gam = GameAudioManager.getInstance(this);
            gam.soundTest(this, findViewById(R.id.imageButton));
        });
    }

    /**
     * Guarda el valor del volumen en SharedPreferences.
     *
     * @param key   La clave bajo la cual se almacenará el valor.
     * @param value El valor de volumen a almacenar.
     */
    private void saveVolumeSettings(String key, float value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat(key, value); // Guardar el valor de volumen
        editor.apply(); // Aplicar los cambios de manera asíncrona
    }

    /**
     * Maneja la pulsación del botón "Atrás".
     * Finaliza la actividad y regresa a la pantalla anterior.
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}