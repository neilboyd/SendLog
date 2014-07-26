package org.l6n.sendlog.userapp;

import android.util.Log;

import org.l6n.sendlog.library.SendLogActivityBase;

/**
 * An example implementation of SendLogActivity.
 */
public class SendLogActivity extends SendLogActivityBase {

    /**
     * Write a log message otherwise the log will be empty.
     * This is just for demonstration purposes. Of course you wouldn't really do this.
     */
    @Override
    protected void onResume() {
        Log.d("SendLogActivity", "Something that will appear in the logs");
        super.onResume();
    }

    /**
     * This is the only method you have to override.
     * There others that you may optionally want to override.
     */
    @Override
    protected String getDestinationAddress() {
        return "android@L6n.org";
    }

}