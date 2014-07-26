package org.l6n.sendlog.userapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;

public class HelloAndroidActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("USERAPP", "onCreate");

        final View hello = findViewById(R.id.hello);
        hello.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View pView) {
                Log.d("USERAPP", "onClick");
                final Intent intent = getPackageManager().getLaunchIntentForPackage("org.l6n.sendlog");
                intent.setType("5|com.google.android.gm.ComposeActivityGmail|android@l6n.org");
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

}

