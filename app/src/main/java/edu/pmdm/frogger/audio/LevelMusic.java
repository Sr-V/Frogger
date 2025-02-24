package edu.pmdm.frogger.audio;

import android.content.Context;
import android.media.MediaPlayer;

import edu.pmdm.frogger.R;

public class LevelMusic {

    public void levelOneTheme(Context c){
        MediaPlayer mp = MediaPlayer.create(c, R.raw.frog_song3);
        mp.start();
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mp.release();
            }
        });
    }

    public void levelTwoTheme(Context c){
        MediaPlayer mp = MediaPlayer.create(c, R.raw.frog_song4);
        mp.start();
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mp.release();
            }
        });
    }

    public void levelThreeTheme(Context c){
        MediaPlayer mp = MediaPlayer.create(c, R.raw.frog_song5);
        mp.start();
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mp.release();
            }
        });
    }

}
