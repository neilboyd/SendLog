package org.l6n.sendlog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

public class SendLog extends Activity implements ISendLog, OnClickListener, OnDismissListener {

    private static final String TAG = "SendLog";

    public static final int BUFFER_SIZE = 8000;

    // persist the format selection
    static final String FORMAT_PREFERENCE = "FORMAT";
    static final int FORMAT_DEFAULT = 5; // time
    static final String FORMAT_SEPARATOR = "|";

    /**
     * If we're sending the message finish when the logcat thread finishes,
     * otherwise finish immediately.
     */
    private boolean mFinishLater = false;

    /**
     * Format entered in shortcut.
     */
    private int mFormat = -1;
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

    private ProgressDialog mProgressDialog;

    @Override
    protected void onResume() {
        Log.v(TAG, "onResume mFinishLater=" + mFinishLater);
        super.onResume();

        // if we're already running, exit
        // this happens if prompted for superuser permission
        if (mFinishLater) {
            return;
        }

        mFinishLater = false;

        // type comes from the shortcut
        String type = getIntent().getType();
        int i = type == null ? -1 : type.indexOf(FORMAT_SEPARATOR);
        if (i == -1) {
            mDestination = type;
        } else {
            try {
                mFormat = Integer.parseInt(type.substring(0, i));
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

        Log.d(TAG, "mFormat=" + mFormat + " mSender=" + mSender + " mDestination=" + mDestination);

        // extras come when SendLog is invoked via an intent
        mExtra = getIntent().getStringExtra("filter");
        mSubject = getIntent().getStringExtra(Intent.EXTRA_SUBJECT);
        Log.d(TAG, "mExtra=" + mExtra + " mSubject=" + mSubject);

        // if we were started with the format in the shortcut then
        // run now, otherwise prompt for format
        if (mFormat == -1) {
            final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
            mFormat = sp.getInt(FORMAT_PREFERENCE, FORMAT_DEFAULT);

            final AlertDialog ad = new AlertDialog.Builder(this)
            .setTitle(R.string.format_dialog_title)
            .setSingleChoiceItems(R.array.format_list, mFormat, this)
            .setPositiveButton(R.string.format_dialog_send, this)
            .setNegativeButton(R.string.format_dialog_cancel, this)
            .create();

            ad.setOnDismissListener(this);
            ad.show();
        } else {
            Log.d(TAG, "Send the email with format " + mFormat);
            sendLog();
            mFinishLater = true;
        }
    }

    @Override
    public void onClick(final DialogInterface dialog, final int which) {
        // Log.v(TAG, "onClick(" + dialog + "," + which + ")");

        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        if (which == DialogInterface.BUTTON_POSITIVE) {
            mFormat = sp.getInt(FORMAT_PREFERENCE, FORMAT_DEFAULT);
            Log.d(TAG, "Send the email with format " + mFormat);
            sendLog();
            mFinishLater = true;
        }

        if (which >= 0) {
            // selected one of the formats
            final SharedPreferences.Editor ed = sp.edit();
            ed.putInt(FORMAT_PREFERENCE, which);
            ed.commit();
        }
    }

    @Override
    public void onDismiss(final DialogInterface dialog) {
        Log.d(TAG, "onDismiss mFinishLater=" + mFinishLater);
        if (!mFinishLater) {
            Log.d(TAG, "call finish()");
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        mFinishLater = false;
        super.onDestroy();
    }

    private void sendLog() {
        Log.d(TAG, "sendLog(" + mFormat + ")");
        final String[] formats = getResources().getStringArray(R.array.format_list);
        mFormatString = formats[mFormat];
        Log.d(TAG, "format=" + mFormatString);

        mProgressDialog = ProgressDialog.show(this, "", getString(R.string.progress_dialog_text));

        final LogcatThread lt = new LogcatThread(this);
        final Thread t = new Thread(lt);
        t.start();
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
    public String getFooter() {
        return getString(R.string.footer);
    }

    @Override
    public File getNonSdCardFolder() {
        return getFilesDir();
    }

    @Override
    public void finished(final File pFile) {
        Log.d(TAG, "finished(" + pFile + ")");

        final File zipFile = createZipFile(pFile);

        mProgressDialog.dismiss();

        final PackageManager pm = getPackageManager();

        String version = "";
        try {
            final PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
            version = " " + pi.versionName;
        } catch (final NameNotFoundException e) {
            Log.e(TAG, "Version name not found", e);
        }
        final String emailText = getString(R.string.email_text, version,
                Build.MODEL, Build.DEVICE, VERSION.RELEASE, Build.ID,
                zipFile.getName(), (zipFile.length() + 512) / 1024);

        // send the email
        final Intent sendIntent = new Intent(Intent.ACTION_SEND);
        if (!TextUtils.isEmpty(mSubject)) {
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, mSubject);
        } else {
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        }
        sendIntent.setType("message/rfc822");
        sendIntent.putExtra(Intent.EXTRA_TEXT, emailText);
        sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(zipFile));

        boolean chooser = true;
        if (mSender != null) {

            final List<ResolveInfo> ri = pm.queryIntentActivities(sendIntent, PackageManager.MATCH_DEFAULT_ONLY);

            for (final ResolveInfo info : ri) {
                if (mSender.equals(info.activityInfo.name)) {
                    sendIntent.setComponent(new ComponentName(
                            info.activityInfo.applicationInfo.packageName,
                            info.activityInfo.name));
                    chooser = false;
                    break;
                }
            }
        }

        if (!TextUtils.isEmpty(mDestination)) {
            sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { mDestination });
        }

        if (chooser) {
            startActivity(Intent.createChooser(sendIntent, getString(R.string.app_name)));
        } else {
            startActivity(sendIntent);
        }

        // now finish the main activity
        Log.d(TAG, "call finish()");
        finish();
    }

    @Override
    public void error(final Exception e) {
        Log.e(TAG, "Error", e);
        mProgressDialog.dismiss();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(SendLog.this)
                        .setMessage(e.getMessage())
                        .setPositiveButton(android.R.string.ok, new OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                finish();
                            }
                        })
                        .show();
            }
        });
    }

    @Override
    public boolean hasReadLogsPermission() {
        final boolean hasPermission = getPackageManager()
                .checkPermission(Manifest.permission.READ_LOGS, getPackageName()) == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, "Has READ_LOGS permission: " + hasPermission);
        return hasPermission;
    }

    private File createZipFile(final File pFile) {
        try {
            final File file = new File(pFile.getAbsolutePath().replace(".txt", ".zip"));
            final FileOutputStream fileOutputStream = new FileOutputStream(file);
            final ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
            final ZipEntry entry = new ZipEntry(pFile.getName());
            zipOutputStream.putNextEntry(entry);
            final FileInputStream fileInputStream = new FileInputStream(pFile);
            final byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while((length = fileInputStream.read(buffer)) > 0) {
                zipOutputStream.write(buffer, 0, length);
            }
            zipOutputStream.close();
            fileInputStream.close();
            return file;
        } catch (IOException e) {
            Log.e(TAG, "Error creating zip file", e);
            return pFile;
        }
        // TODO of course the close's should be in a finally block
    }

    @Override
    public String getNoPermissionMessage() {
        return getString(R.string.no_permission);
    }

    @Override
    public String getBadExitCodeMessage() {
        return getString(R.string.bad_exit_code);
    }
}
