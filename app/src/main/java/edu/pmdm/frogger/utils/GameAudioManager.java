package edu.pmdm.frogger.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageButton;

import edu.pmdm.frogger.R;

/**
 * {@code GameAudioManager} gestiona todos los recursos de audio del juego Frogger.
 * Permite reproducir música de fondo, efectos de sonido y ajustar volúmenes,
 * utilizando una instancia singleton para evitar cargas redundantes.
 */
public class GameAudioManager {

    // Instancia singleton de GameAudioManager
    private static GameAudioManager instance;
    // MediaPlayers para diferentes pistas y efectos
    public MediaPlayer mainThemeMp;
    public MediaPlayer levelOneThemeMp;
    public MediaPlayer levelTwoThemeMp;
    public MediaPlayer levelThreeThemeMp;
    public MediaPlayer idleCroakMp;
    public MediaPlayer carHonksMp;

    // Niveles de volumen para música y efectos
    public float MUSIC_VOLUME = 1.0f;
    public float AUDIO_VOLUME = 1.0f;

    // Constructor privado para evitar instanciación externa
    private GameAudioManager() {}

    /**
     * Obtiene la instancia única de GameAudioManager.
     *
     * @param context Contexto de la aplicación, necesario para acceder a recursos y SharedPreferences.
     * @return Instancia de {@code GameAudioManager}.
     */
    public static GameAudioManager getInstance(Context context) {
        if (instance == null) {
            instance = new GameAudioManager();
            instance.loadVolumeSettings(context);
        }
        return instance;
    }

    /**
     * Carga los ajustes de volumen almacenados en SharedPreferences.
     *
     * @param context Contexto de la aplicación.
     */
    private void loadVolumeSettings(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("AudioSettings", Context.MODE_PRIVATE);
        MUSIC_VOLUME = sharedPreferences.getFloat("MUSIC_AUDIO_LEVEL", 1.0f);
        AUDIO_VOLUME = sharedPreferences.getFloat("AUDIO_LEVEL", 1.0f);
    }

    /**
     * Reproduce efectos de sonido asociados al movimiento del jugador.
     *
     * @param c Contexto de la aplicación.
     */
    public void playerMovement(Context c) {
        // Reproducir sonido de salto de rana
        MediaPlayer mp = MediaPlayer.create(c, R.raw.frog_jump);
        mp.setVolume(AUDIO_VOLUME, AUDIO_VOLUME);
        mp.start();
        mp.setOnCompletionListener(mediaPlayer -> mp.release());

        // Reproducir sonido de croar de la rana
        MediaPlayer mp2 = MediaPlayer.create(c, R.raw.frog_ribbit);
        mp2.setVolume(AUDIO_VOLUME, AUDIO_VOLUME);
        mp2.start();
        mp2.setOnCompletionListener(mediaPlayer -> mp2.release());
    }

    /**
     * Reproduce el efecto de sonido de la muerte de la rana.
     *
     * @param c Contexto de la aplicación.
     */
    public void playerDeath(Context c) {
        MediaPlayer mp = MediaPlayer.create(c, R.raw.frog_death);
        mp.setVolume(AUDIO_VOLUME, AUDIO_VOLUME);
        mp.start();
        mp.setOnCompletionListener(mediaPlayer -> mp.release());
    }

    /**
     * Realiza una prueba de sonido para efectos, deshabilitando temporalmente el botón.
     *
     * @param c      Contexto de la aplicación.
     * @param button Botón que invoca la prueba, se deshabilita para evitar múltiples clics.
     */
    public void soundTest(Context c, ImageButton button) {
        // Deshabilitar el botón para evitar múltiples clics
        button.setEnabled(false);

        // Crear y reproducir el MediaPlayer con el sonido de croar
        MediaPlayer mp = MediaPlayer.create(c, R.raw.frog_croak);
        mp.setVolume(AUDIO_VOLUME, AUDIO_VOLUME);
        mp.start();

        // Al terminar, liberar recursos y volver a habilitar el botón
        mp.setOnCompletionListener(mediaPlayer -> {
            mp.release();
            button.setEnabled(true);
        });
    }

    /**
     * Reproduce el sonido de croar en bucle (con intervalos) cuando la rana está inactiva.
     *
     * @param c Contexto de la aplicación.
     */
    public void idleCroak(Context c) {
        idleCroakMp = MediaPlayer.create(c, R.raw.frog_croak);
        Runnable playSound = new Runnable() {
            @Override
            public void run() {
                if (idleCroakMp != null) {
                    idleCroakMp.setVolume(AUDIO_VOLUME / 2, AUDIO_VOLUME / 2);
                    idleCroakMp.start();
                    idleCroakMp.setOnCompletionListener(mediaPlayer -> new Handler().postDelayed(this, 9000));
                }
            }
        };

        new Handler().postDelayed(playSound, 9000);
    }

    /**
     * Reproduce la música principal del juego en bucle.
     *
     * @param c Contexto de la aplicación.
     */
    public void mainThemeSong(Context c) {
        mainThemeMp = MediaPlayer.create(c, R.raw.frog_song2);
        mainThemeMp.setVolume(MUSIC_VOLUME, MUSIC_VOLUME);
        mainThemeMp.start();
        mainThemeMp.setLooping(true);
    }

    /**
     * Detiene y libera la música principal.
     */
    public void stopMainThemeSong() {
        if (mainThemeMp != null) {
            mainThemeMp.stop();
            mainThemeMp.release();
            mainThemeMp = null;
        }
    }

    /**
     * Reproduce la música del nivel 1 en bucle.
     *
     * @param c Contexto de la aplicación.
     */
    public void levelOneTheme(Context c) {
        levelOneThemeMp = MediaPlayer.create(c, R.raw.frog_song3);
        levelOneThemeMp.setVolume(MUSIC_VOLUME, MUSIC_VOLUME);
        levelOneThemeMp.start();
        levelOneThemeMp.setLooping(true);
    }

    /**
     * Detiene y libera la música del nivel 1.
     */
    public void stopLevelOneTheme() {
        if (levelOneThemeMp != null) {
            try {
                if (levelOneThemeMp.isPlaying()) {
                    levelOneThemeMp.stop();
                    Log.d("Audio", "Level One Theme stopped");
                }
                levelOneThemeMp.release();
                levelOneThemeMp = null;
                Log.d("Audio", "Level One Theme released");
            } catch (IllegalStateException e) {
                Log.e("Audio", "Error stopping Level One Theme", e);
            }
        } else {
            Log.d("Audio", "Level One Theme is null");
        }
    }

    /**
     * Reproduce la música del nivel 2 en bucle.
     *
     * @param c Contexto de la aplicación.
     */
    public void levelTwoTheme(Context c) {
        levelTwoThemeMp = MediaPlayer.create(c, R.raw.frog_song4);
        levelTwoThemeMp.setVolume(MUSIC_VOLUME, MUSIC_VOLUME);
        levelTwoThemeMp.start();
        levelTwoThemeMp.setLooping(true);
    }

    /**
     * Detiene y libera la música del nivel 2.
     */
    public void stopLevelTwoTheme() {
        if (levelTwoThemeMp != null) {
            levelTwoThemeMp.stop();
            levelTwoThemeMp.release();
            levelTwoThemeMp = null;
        }
    }

    /**
     * Reproduce la música del nivel 3 en bucle.
     *
     * @param c Contexto de la aplicación.
     */
    public void levelThreeTheme(Context c) {
        levelThreeThemeMp = MediaPlayer.create(c, R.raw.frog_song5);
        levelThreeThemeMp.setVolume(MUSIC_VOLUME, MUSIC_VOLUME);
        levelThreeThemeMp.start();
        levelThreeThemeMp.setLooping(true);
    }

    /**
     * Detiene y libera la música del nivel 3.
     */
    public void stopLevelThreeTheme() {
        if (levelThreeThemeMp != null) {
            levelThreeThemeMp.stop();
            levelThreeThemeMp.release();
            levelThreeThemeMp = null;
        }
    }

    /**
     * Reproduce el sonido de bocina de automóvil en intervalos periódicos.
     *
     * @param c Contexto de la aplicación.
     */
    public void carHonks(Context c) {
        carHonksMp = MediaPlayer.create(c, R.raw.car_horn_2);

        Runnable playSound = new Runnable() {
            @Override
            public void run() {
                if (carHonksMp != null) {
                    carHonksMp.setVolume(AUDIO_VOLUME, AUDIO_VOLUME);
                    carHonksMp.start();
                    carHonksMp.setOnCompletionListener(mediaPlayer -> new Handler().postDelayed(this, 7000));
                }
            }
        };

        new Handler().postDelayed(playSound, 7000);
    }

    /**
     * Detiene y libera los sonidos de idle croak y bocinas.
     */
    public void stopIdleSound() {
        if (idleCroakMp != null) {
            idleCroakMp.stop();
            idleCroakMp.release();
            idleCroakMp = null;
        }
        if (carHonksMp != null) {
            carHonksMp.stop();
            carHonksMp.release();
            carHonksMp = null;
        }
    }

    /**
     * Reproduce el efecto de sonido al recoger la llave.
     *
     * @param c Contexto de la aplicación.
     */
    public void keyCollected(Context c) {
        MediaPlayer mp = MediaPlayer.create(c, R.raw.key_found);
        mp.setVolume(AUDIO_VOLUME, AUDIO_VOLUME);
        mp.start();
        mp.setOnCompletionListener(mediaPlayer -> mp.release());
    }

    /**
     * Reproduce el sonido de ahogamiento cuando la rana se queda sin vidas en el agua.
     *
     * @param c Contexto de la aplicación.
     */
    public void playerDrowned(Context c) {
        MediaPlayer mp = MediaPlayer.create(c, R.raw.drowning);
        mp.setVolume(AUDIO_VOLUME, AUDIO_VOLUME);
        mp.start();
        mp.setOnCompletionListener(mediaPlayer -> mp.release());
    }

    /**
     * Reproduce el sonido al caer en arena.
     *
     * @param c Contexto de la aplicación.
     */
    public void playerSand(Context c) {
        MediaPlayer mp = MediaPlayer.create(c, R.raw.sand_fall);
        mp.setVolume(AUDIO_VOLUME, AUDIO_VOLUME);
        mp.start();
        mp.setOnCompletionListener(mediaPlayer -> mp.release());
    }

    /**
     * Reproduce el sonido al caer (por ejemplo, en el espacio o precipicio).
     *
     * @param c Contexto de la aplicación.
     */
    public void playerFell(Context c) {
        MediaPlayer mp = MediaPlayer.create(c, R.raw.fall);
        mp.setVolume(AUDIO_VOLUME, AUDIO_VOLUME);
        mp.start();
        mp.setOnCompletionListener(mediaPlayer -> mp.release());
    }
}