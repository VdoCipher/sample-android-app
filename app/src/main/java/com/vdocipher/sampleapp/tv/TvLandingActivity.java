package com.vdocipher.sampleapp.tv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.vdocipher.sampleapp.R;

public class TvLandingActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tv_landing);
    }

    public void startPlayback(View v) {
        //Intent intent = new Intent(this, TvPlayerActivity.class);
        //startActivity(intent);
    }
}
