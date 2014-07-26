package org.l6n.sendlog;

import java.io.File;

public interface ISendLog {

    String[] getCommands();

    String getFooter();

    File getNonSdCardFolder();

    void finished(File file);

    void error(Exception e);

    boolean hasReadLogsPermission();

    String getNoPermissionMessage();

    String getBadExitCodeMessage();
}
