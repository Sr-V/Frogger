package edu.pmdm.frogger.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;

import edu.pmdm.frogger.R;

public class GameAudioManager {

    MediaPlayer mainThemeMp;
    MediaPlayer levelOneThemeMp;
    MediaPlayer levelTwoThemeMp;
    MediaPlayer levelThreeThemeMp;
    public void playerMovement(Context c){

        MediaPlayer mp = MediaPlayer.create(c, R.raw.frog_jump);
        mp.start();
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mp.release();
            }
        });

        MediaPlayer mp2 = MediaPlayer.create(c, R.raw.frog_ribbit);
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
        mp.start();
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mp.release();
            }
        });
    }

    public void wallCollision(Context c){
        MediaPlayer mp = MediaPlayer.create(c, R.raw.frog_wall_collision);
        mp.start();
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mp.release();
            }
        });
    }

    public void idleCroak(Context c){
        MediaPlayer mp = MediaPlayer.create(c, R.raw.frog_croak);
        Runnable playSound = new Runnable() {
            @Override
            public void run() {
                if (mp != null) {
                    mp.setVolume(0.5f, 0.5f);
                    mp.start();
                    mp.setOnCompletionListener(mediaPlayer -> {
                        new Handler().postDelayed(this, 9000);
                    });
                }
            }
        };

        new Handler().postDelayed(playSound, 9000);

    }

    public void mainThemeSong(Context c){
        mainThemeMp = MediaPlayer.create(c, R.raw.frog_song2);
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
        levelOneThemeMp.start();
        levelOneThemeMp.setLooping(true);
    }

    public void stopLevelOneTheme(){
        if (levelOneThemeMp != null) {
            levelOneThemeMp.stop();
            levelOneThemeMp.release();
            levelOneThemeMp = null;
        }
    }

    public void levelTwoTheme(Context c){
        levelTwoThemeMp = MediaPlayer.create(c, R.raw.frog_song4);
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
        MediaPlayer mp = MediaPlayer.create(c, R.raw.car_horn_2);

        Runnable playSound = new Runnable() {
            @Override
            public void run() {
                if (mp != null) {
                    mp.start();
                    mp.setOnCompletionListener(mediaPlayer -> {
                        new Handler().postDelayed(this, 7000);
                    });
                }
            }
        };

        new Handler().postDelayed(playSound, 7000);
    }




}
