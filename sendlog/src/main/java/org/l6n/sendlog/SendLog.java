package org.l6n.sendlog;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import org.l6n.sendlog.library.SendLogActivityBase;

public class SendLog extends SendLogActivityBase {

    private static final String TAG = "SendLog";

    static final String FORMAT_SEPARATOR = "|";

    /**
     * Format entered in shortcut.
     */
    private String mFormatString;

    /**
     * Destination email entered in shortcut.
     */
    private String mDestination;

    /**
     * Sender entered in shortcut.
     */
    private String mSender;

    /**
     * Extra entered in extras.
     */
    private String mExtra;

    /**
     * Subject entered in extras.
     */
    private String mSubject;

    @Override
    public String getSubject() {
        return mSubject;
    }

    @Override
    protected String getLogFormat() {
        return mFormatString;
    }

    @Override
    protected String getSenderApp() {
        return mSender;
    }

    @Override
    public String[] getCommands() {
        final StringBuilder sb = new StringBuilder();
        sb.append("logcat -v ");
        sb.append(mFormatString);
        sb.append(" -d ");

        if (mExtra != null) {
            sb.append(mExtra);
        }

        final String logcatCommand = sb.toString();

        return new String[]{"top -n 1", logcatCommand};
    }


    @Override
    protected String getDestinationAddress() {
        return mDestination;
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "onResume");

        // type comes from the shortcut
        String type = getIntent().getType();
        int i = type == null ? -1 : type.indexOf(FORMAT_SEPARATOR);
        if (i == -1) {
            mDestination = type;
        } else {
            try {
                final int format = Integer.parseInt(type.substring(0, i));
                mFormatString = getResources().getStringArray(R.array.format_list)[format];
            } catch (final Exception e) {
                Log.w(TAG, "Error parsing format: ", e);
            }
            type = type.substring(i + 1);
            i = type.indexOf(FORMAT_SEPARATOR);
            if (i == -1) {
                mDestination = type;
            } else {
                mSender = type.substring(0, i);
                mDestination = type.substring(i + 1);
            }
        }

        Log.d(TAG, "mFormat=" + mFormatString + " mSender=" + mSender + " mDestination=" + mDestination);

        // extras come when SendLog is invoked via an intent
        mExtra = getIntent().getStringExtra("filter");
        mSubject = getIntent().getStringExtra(Intent.EXTRA_SUBJECT);
        Log.d(TAG, "mExtra=" + mExtra + " mSubject=" + mSubject);

        super.onResume();
    }

    @Override
    public boolean hasReadLogsPermission() {
        final boolean hasPermission = getPackageManager()
                .checkPermission(android.Manifest.permission.READ_LOGS, getPackageName()) == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, "Has READ_LOGS permission: " + hasPermission);
        return hasPermission;
    }

}
