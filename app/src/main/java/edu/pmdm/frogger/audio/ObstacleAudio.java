package edu.pmdm.frogger.audio;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;

import edu.pmdm.frogger.R;

public class ObstacleAudio {

    public void carHonks(Context c){
        MediaPlayer mp = MediaPlayer.create(c, R.raw.car_horn);
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
