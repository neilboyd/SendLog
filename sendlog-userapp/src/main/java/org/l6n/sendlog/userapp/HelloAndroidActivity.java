package org.l6n.sendlog.userapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class HelloAndroidActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("USERAPP", "onCreate");

        final View intent = findViewById(R.id.intent);
        intent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View pView) {
                Log.d("USERAPP", "onClick intent");
                final Intent intent = getPackageManager().getLaunchIntentForPackage("org.l6n.sendlog");
                intent.setType("5|com.google.android.gm.ComposeActivityGmail|android@l6n.org");
                startActivity(intent);
            }
        });

        final View embedded = findViewById(R.id.embedded);
        embedded.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View pView) {
                Log.d("USERAPP", "onClick embedded");
                final Intent intent = new Intent(HelloAndroidActivity.this, SendLogActivity.class);
                startActivity(intent);
            }
        });
    }

}

