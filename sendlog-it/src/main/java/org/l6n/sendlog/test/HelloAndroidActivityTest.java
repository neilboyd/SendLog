package org.l6n.sendlog.test;

import org.l6n.sendlog.SendLog;

import android.test.ActivityInstrumentationTestCase2;

public class HelloAndroidActivityTest extends ActivityInstrumentationTestCase2<SendLog> {

    public HelloAndroidActivityTest() {
        super("org.l6n.sendlog", SendLog.class);
    }

    public void testActivity() {
        SendLog activity = getActivity();
        assertNotNull(activity);
    }
}

