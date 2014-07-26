package org.l6n.sendlog.library;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class SendLogActivityBase extends Activity implements ISendLog {

    private static final String TAG = "SendLogActivityBase";

    private static final int BUFFER_SIZE = 8000;

    // persist the format selection
    public static final String FORMAT_PREFERENCE = "FORMAT";
    public static final int FORMAT_DEFAULT = 5; // time
    private int mFormat = FORMAT_DEFAULT;

    private ProgressDialog mProgressDialog;

    /**
     * If we're sending the message finish when the logcat thread finishes,
     * otherwise finish immediately.
     */
    private boolean mFinishLater = false;

    /**
     * The app to use to send the log,
     * for example com.google.android.gm.ComposeActivityGmail
     * Return null to prompt the user to choose an app.
     */
    protected String getSenderApp() {
        return null;
    }

    /**
     * The email address to send the log to.
     */
    protected abstract String getDestinationAddress();

    /**
     * The subject of the email. Override to use a different subject.
     */
    protected String getSubject() {
        return getString(R.string.app_name);
    }

    /**
     * The log format to use. Default is "time". Override to use a different format.
     * Return null to prompt for format.
     */
    protected String getLogFormat() {
        final String[] formats = getResources().getStringArray(R.array.format_list);
        if (mFormat >= 0 && mFormat < formats.length) {
            return formats[mFormat];
        }
        return formats[FORMAT_DEFAULT];
    }

    /**
     * The text of the email. Override to use different text.
     * @param pZipFile the file containing the zipped log
     * @param pPackageManager the package manager
     * @return the message text
     */
    protected String getMessageText(final File pZipFile, final PackageManager pPackageManager) {
        String version = "";
        try {
            final PackageInfo pi = pPackageManager.getPackageInfo(getPackageName(), 0);
            version = " " + pi.versionName;
        } catch (final PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Version name not found", e);
        }
        return getString(R.string.email_text, version,
                Build.MODEL, Build.DEVICE, Build.VERSION.RELEASE, Build.ID,
                pZipFile.getName(), (pZipFile.length() + 512) / 1024);
    }

    /**
     * The command to send to generate the logcat.
     * May be overridden to execute different commands, but use with caution.
     */
    @Override
    public String[] getCommands() {
        final String logcatCommand = "logcat -v " + getLogFormat() + " -d ";
        return new String[]{"top -n 1", logcatCommand};
    }

    /**
     * The footer of the email. Override to use a different footer.
     */
    @Override
    public String getFooter() {
        return getString(R.string.footer);
    }

    @Override
    public boolean hasReadLogsPermission() {
        return true;
    }

    @Override
    public String getNoPermissionMessage() {
        return getString(R.string.no_permission);
    }

    @Override
    public String getBadExitCodeMessage() {
        return getString(R.string.bad_exit_code);
    }

    @Override
    public File getNonSdCardFolder() {
        return getFilesDir();
    }

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

        final String format = getLogFormat();
        if (TextUtils.isEmpty(format)) {
            final AlertDialog ad = new AlertDialog.Builder(this)
                    .setTitle(R.string.format_dialog_title)
                    .setSingleChoiceItems(R.array.format_list, mFormat, mFormatDialogClickListener)
                    .setPositiveButton(R.string.format_dialog_send, mFormatDialogClickListener)
                    .setNegativeButton(R.string.format_dialog_cancel, mFormatDialogClickListener)
                    .create();

            ad.setOnDismissListener(mFormatDialogDismissListener);
            ad.show();
        } else {
            Log.d(TAG, "Send the email with format " + format);
            sendLog();
            mFinishLater = true;
        }
    }

    @Override
    protected void onDestroy() {
        mFinishLater = false;
        super.onDestroy();
    }

    @Override
    public void finished(final File pFile) {
        Log.d(TAG, "finished(" + pFile + ")");

        final File zipFile = createZipFile(pFile);

        final PackageManager pm = getPackageManager();

        final String emailText = getMessageText(zipFile, pm);

        mProgressDialog.dismiss();

        // send the email
        final Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, getSubject());
        sendIntent.setType("message/rfc822");
        sendIntent.putExtra(Intent.EXTRA_TEXT, emailText);
        sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(zipFile));

        boolean chooser = true;
        final String sender = getSenderApp();
        if (sender != null) {

            final List<ResolveInfo> ri = pm.queryIntentActivities(sendIntent, PackageManager.MATCH_DEFAULT_ONLY);

            for (final ResolveInfo info : ri) {
                if (sender.equals(info.activityInfo.name)) {
                    sendIntent.setComponent(new ComponentName(
                            info.activityInfo.applicationInfo.packageName,
                            info.activityInfo.name));
                    chooser = false;
                    break;
                }
            }
        }

        final String destination = getDestinationAddress();
        if (!TextUtils.isEmpty(destination)) {
            sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { destination });
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
                new AlertDialog.Builder(SendLogActivityBase.this)
                        .setMessage(e.getMessage())
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialog, final int which) {
                                finish();
                            }
                        })
                        .show();
            }
        });
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
        } catch (final IOException e) {
            Log.e(TAG, "Error creating zip file", e);
            return pFile;
        }
        // TODO of course the close's should be in a finally block
    }

    private void sendLog() {
        final String format = getLogFormat();
        Log.d(TAG, "sendLog(" + format + ")");

        mProgressDialog = ProgressDialog.show(this, "", getString(R.string.progress_dialog_text));

        final LogcatThread lt = new LogcatThread(this);
        final Thread t = new Thread(lt);
        t.start();
    }

    private final DialogInterface.OnClickListener mFormatDialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(final DialogInterface dialog, final int which) {
            // Log.v(TAG, "onClick(" + dialog + "," + which + ")");

            final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(SendLogActivityBase.this);

            if (which == DialogInterface.BUTTON_POSITIVE) {
                mFormat = sp.getInt(FORMAT_PREFERENCE, FORMAT_DEFAULT);
                Log.d(TAG, "Send the email with format " + mFormat);
                sendLog();
                mFinishLater = true;
            }

            if (which >= 0) {
                // selected one of the formats
                sp.edit().putInt(FORMAT_PREFERENCE, which).commit();
            }
        }
    };

    private final DialogInterface.OnDismissListener mFormatDialogDismissListener = new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(final DialogInterface dialog) {
            Log.d(TAG, "onDismiss mFinishLater=" + mFinishLater);
            if (!mFinishLater) {
                Log.d(TAG, "call finish()");
                finish();
            }
        }
    };
}