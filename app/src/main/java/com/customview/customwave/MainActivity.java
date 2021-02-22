package com.customview.customwave;


import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    private WaveProgressView waveProgressView;

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.customwave);
        waveProgressView = (WaveProgressView) findViewById(R.id.wave_progress);
        waveProgressView.setProgressNum(80,3000);
        waveProgressView.setOnAnimationListener(new WaveProgressView.OnAnimationListener() {
            //省略部分代码...
            @Override
            public float howToChangeWaveHeight(float percent, float waveHeight) {
                return (1-percent)*waveHeight;
            }
        });
        waveProgressView.setDrawSecondWave(true);
    }
}
