package edu.pmdm.frogger.audio;

import android.content.Context;
import android.media.MediaPlayer;

import edu.pmdm.frogger.R;

public class MainMenuAudio {

    public void themeSong(Context c){
        MediaPlayer mp = MediaPlayer.create(c, R.raw.frog_song2);
        mp.start();
        mp.setLooping(true);
    }

}
