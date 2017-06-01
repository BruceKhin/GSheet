package com.brucekhin.homebuilder;

/**
 * Created by brucekhin_home on 5/2/17.
 */
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                moveNextActivity();
            }
        }, 3000);

    }

    private void moveNextActivity() {
        Intent i = new Intent(getApplicationContext(), SheetLayout1.class);
        startActivity(i);
        setContentView(R.layout.sheet_layout);
    }


}