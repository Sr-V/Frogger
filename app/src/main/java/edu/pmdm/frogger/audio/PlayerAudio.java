package edu.pmdm.frogger.audio;

import android.content.Context;
import android.media.MediaPlayer;

import edu.pmdm.frogger.R;

public class PlayerAudio {

    public void playerMovement(Context c){

        MediaPlayer mp = MediaPlayer.create(c, R.raw.frog_jump);
        mp.start();
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mp.release();
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

}

