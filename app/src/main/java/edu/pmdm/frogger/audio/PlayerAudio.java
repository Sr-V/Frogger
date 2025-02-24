package edu.pmdm.frogger.audio;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;


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
        try {
            mp.wait(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        mp.start();
        mp.setLooping(true);
        new Handler().postDelayed(mp::start, 5000);

        new Handler().postDelayed(() ->{
            if(mp.isPlaying()){
                mp.stop();
                mp.release();
            }
        }, 20000);
    }

}

