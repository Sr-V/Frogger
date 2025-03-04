package edu.pmdm.frogger.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageButton;

import edu.pmdm.frogger.R;

public class GameAudioManager {

    private static GameAudioManager instance;
    public MediaPlayer mainThemeMp;
    public MediaPlayer levelOneThemeMp;
    public MediaPlayer levelTwoThemeMp;
    public MediaPlayer levelThreeThemeMp;
    public MediaPlayer idleCroakMp;
    public MediaPlayer carHonksMp;

    public float MUSIC_VOLUME = 1.0f;
    public float AUDIO_VOLUME = 1.0f;
    private GameAudioManager() {}

    // Método estático para obtener la instancia única
    public static GameAudioManager getInstance(Context context) {
        if (instance == null) {
            instance = new GameAudioManager();
            instance.loadVolumeSettings(context);
        }
        return instance;
    }

    private void loadVolumeSettings(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("AudioSettings", Context.MODE_PRIVATE);
        MUSIC_VOLUME = sharedPreferences.getFloat("MUSIC_AUDIO_LEVEL", 1.0f);
        AUDIO_VOLUME = sharedPreferences.getFloat("AUDIO_LEVEL", 1.0f);
    }

    public void playerMovement(Context c){

        MediaPlayer mp = MediaPlayer.create(c, R.raw.frog_jump);
        mp.setVolume(AUDIO_VOLUME, AUDIO_VOLUME);
        mp.start();
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mp.release();
            }
        });

        MediaPlayer mp2 = MediaPlayer.create(c, R.raw.frog_ribbit);
        mp2.setVolume(AUDIO_VOLUME, AUDIO_VOLUME);
        mp2.start();
        mp2.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mp2.release();
            }
        });

    }

    public void playerDeath(Context c){
        MediaPlayer mp = MediaPlayer.create(c, R.raw.frog_death);
        mp.setVolume(AUDIO_VOLUME, AUDIO_VOLUME);
        mp.start();
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mp.release();
            }
        });
    }

    public void soundTest(Context c, ImageButton button) {
        // Deshabilitar el botón para evitar múltiples clics
        button.setEnabled(false);

        // Crear y configurar el MediaPlayer
        MediaPlayer mp = MediaPlayer.create(c, R.raw.frog_croak);
        mp.setVolume(AUDIO_VOLUME, AUDIO_VOLUME);
        mp.start();

        // Configurar el listener para cuando el audio termine
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                // Liberar el MediaPlayer
                mp.release();

                // Habilitar el botón nuevamente
                button.setEnabled(true);
            }
        });
    }

    public void idleCroak(Context c){
        idleCroakMp = MediaPlayer.create(c, R.raw.frog_croak);
        Runnable playSound = new Runnable() {
            @Override
            public void run() {
                if (idleCroakMp != null) {
                    idleCroakMp.setVolume(AUDIO_VOLUME /2, AUDIO_VOLUME /2);
                    idleCroakMp.start();
                    idleCroakMp.setOnCompletionListener(mediaPlayer -> {
                        new Handler().postDelayed(this, 9000);
                    });
                }
            }
        };

        new Handler().postDelayed(playSound, 9000);

    }

    public void mainThemeSong(Context c){
        mainThemeMp = MediaPlayer.create(c, R.raw.frog_song2);
        mainThemeMp.setVolume(MUSIC_VOLUME, MUSIC_VOLUME);
        mainThemeMp.start();
        mainThemeMp.setLooping(true);
    }

    public void stopMainThemeSong(){
        if (mainThemeMp != null) {
            mainThemeMp.stop();
            mainThemeMp.release();
            mainThemeMp = null;
        }
    }

    public void levelOneTheme(Context c){
        levelOneThemeMp = MediaPlayer.create(c, R.raw.frog_song3);
        levelOneThemeMp.setVolume(MUSIC_VOLUME, MUSIC_VOLUME);
        levelOneThemeMp.start();
        levelOneThemeMp.setLooping(true);
    }

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

    public void levelTwoTheme(Context c){
        levelTwoThemeMp = MediaPlayer.create(c, R.raw.frog_song4);
        levelTwoThemeMp.setVolume(MUSIC_VOLUME, MUSIC_VOLUME);
        levelTwoThemeMp.start();
        levelTwoThemeMp.setLooping(true);
    }

    public void stopLevelTwoTheme(){
        if (levelTwoThemeMp != null) {
            levelTwoThemeMp.stop();
            levelTwoThemeMp.release();
            levelTwoThemeMp = null;
        }
    }

    public void levelThreeTheme(Context c){
        levelThreeThemeMp = MediaPlayer.create(c, R.raw.frog_song5);
        levelThreeThemeMp.setVolume(MUSIC_VOLUME, MUSIC_VOLUME);
        levelThreeThemeMp.start();
        levelThreeThemeMp.setLooping(true);
    }

    public void stopLevelThreeTheme(){
        if (levelThreeThemeMp != null) {
            levelThreeThemeMp.stop();
            levelThreeThemeMp.release();
            levelThreeThemeMp = null;
        }
    }

    public void carHonks(Context c){
        carHonksMp = MediaPlayer.create(c, R.raw.car_horn_2);

        Runnable playSound = new Runnable() {
            @Override
            public void run() {
                if (carHonksMp != null) {
                    carHonksMp.setVolume(AUDIO_VOLUME, AUDIO_VOLUME);
                    carHonksMp.start();
                    carHonksMp.setOnCompletionListener(mediaPlayer -> {
                        new Handler().postDelayed(this, 7000);
                    });
                }
            }
        };

        new Handler().postDelayed(playSound, 7000);
    }


    public void stopIdleSound(){
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

    public void keyCollected(Context c){
        MediaPlayer mp = MediaPlayer.create(c, R.raw.key_found);
        mp.setVolume(AUDIO_VOLUME, AUDIO_VOLUME);
        mp.start();
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mp.release();
            }
        });

    }

    public void playerDrowned(Context c){
        MediaPlayer mp = MediaPlayer.create(c, R.raw.drowning);
        mp.setVolume(AUDIO_VOLUME, AUDIO_VOLUME);
        mp.start();
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mp.release();
            }
        });
    }

    public void playerSand(Context c){
        MediaPlayer mp = MediaPlayer.create(c, R.raw.sand_fall);
        mp.setVolume(AUDIO_VOLUME, AUDIO_VOLUME);
        mp.start();
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mp.release();
            }
        });
    }

    public void playerFell(Context c){
        MediaPlayer mp = MediaPlayer.create(c, R.raw.fall);
        mp.setVolume(AUDIO_VOLUME, AUDIO_VOLUME);
        mp.start();
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mp.release();
            }
        });
    }

}
