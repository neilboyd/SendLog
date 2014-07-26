package org.l6n.sendlog.library;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.os.Environment;
import android.util.Log;

public class LogcatThread implements Runnable {

    private static final String TAG = "LogcatThread";

    private static final int BUFFER_SIZE = 8000;

    private ISendLog mSendLog;

    public LogcatThread(final ISendLog pSendLog) {
        Log.d(TAG, "LogcatThread()");

        mSendLog = pSendLog;
    }

    @Override
    public void run() {
        // Log.v(TAG, "run");

        try {
            final File file;
            final String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                file = new File(Environment.getExternalStorageDirectory(), "SendLog.txt");
            } else {
                file = new File(mSendLog.getNonSdCardFolder(), "SendLog.txt");
            }

            final boolean hasReadLogsPermission = mSendLog.hasReadLogsPermission();

            final Process process;
            try {
                process = getProcess(hasReadLogsPermission ? "sh" : "su");
            }catch(IOException e) {
                // if we haven't got permission it means we're a non-rooted 4.1+ device
                // and we couldn't start an su process,
                // so show a better error message
               if (!hasReadLogsPermission) {
                   throw new IOException(mSendLog.getNoPermissionMessage());
               }
                throw e;
            }

            boolean success = runProcess(file, process);

            if (!success) {
                // if we haven't got permission it means we're a non-rooted 4.1+ device
                // and the su process was denied permission,
                // so show a better error message
                if (!hasReadLogsPermission) {
                    throw new IOException(mSendLog.getNoPermissionMessage());
                }
                throw new IOException(mSendLog.getBadExitCodeMessage());
            }

            mSendLog.finished(file);

            Log.d(TAG, "Finished");

        } catch (final Exception e) {
            mSendLog.error(e);
        }
    }

    private boolean runProcess(final File pFile, final Process pProcess) throws IOException, InterruptedException {
        final BufferedWriter fileStream = new BufferedWriter(new FileWriter(pFile), BUFFER_SIZE);

        Thread readerThread = new Thread() {
            @Override
            public void run() {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new InputStreamReader(pProcess.getInputStream()), BUFFER_SIZE);
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Log.v(TAG, "su: " + line);
                        fileStream.write(line);
                        fileStream.newLine();
                    }
                } catch (Exception e) {
                    // Log.v(TAG, "Error reading/writing", e);
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (final IOException ignore) {
                        }
                    }
                }
            }
        };
        readerThread.start();

        final BufferedWriter processWriteStream = new BufferedWriter(new OutputStreamWriter(pProcess.getOutputStream()));

        Thread writerThread = new Thread() {
            @Override
            public void run() {
                try {
                    for (final String command : mSendLog.getCommands()) {
                        processWriteStream.write(command);
                        processWriteStream.newLine();
                        processWriteStream.write("echo");
                        processWriteStream.newLine();
                        processWriteStream.write("echo");
                        processWriteStream.newLine();
                    }
                    processWriteStream.write("exit");
                    processWriteStream.newLine();
                    processWriteStream.close();
                } catch (final IOException e) {
                    Log.d(TAG, "Error writing to process: " + e.getMessage());
                }
            }
        };
        writerThread.start();

        pProcess.waitFor();
        readerThread.join();
        writerThread.join();
        pProcess.destroy();

        fileStream.write(mSendLog.getFooter());
        fileStream.newLine();
        fileStream.close();

        return pProcess.exitValue() == 0;
    }

    private Process getProcess(String pShell) throws IOException {
        return new ProcessBuilder()
                .command(pShell)
                .redirectErrorStream(true)
                .start();
    }

}
