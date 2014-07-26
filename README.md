#SendLog

[SendLog](http://l6n.org/android/sendlog.shtml)
was originally released as an app to
[Google Play](http://l6n.org/android/sendlog.shtml).  
Due to
[changes in READ_LOGS permission in Android 4.1](http://groups.google.com/d/msg/android-developers/6U4A5irWang/dEsqi0dyPkkJ),
the app became a bit useless.
TL;DR: an app can only read it's own logs.  
That is why I decided to open source it so that you can include it in your own app.

##Brief Instructions
 - Add a dependency to sendlog-library
 - Extend `SendLogActivityBase` and implement one method
 - Add your activity to the manifest with translucent theme
 - Start the activity at a suitable point in your app

##Detailed Instructions

###Example
There is a sample project `sendlog-userapp` which demonstrates all the steps
described below.

###Add a dependency to sendlog-library
Shortly I will add the project to
[Maven Central](http://search.maven.org/),
but for now you should just download the source and reference it.

###Extend `SendLogActivityBase` and implement one method
Create an activity called `SendLogActivity` in your project which extends
`org.l6n.sendlog.library.SendLogActivityBase`.

Implement the following method
`@Override protected String getDestinationAddress() { return "android@L6n.org"; }`.  
Obviously you should use your own email address instead of mine for users to send their logs to.

###Add your activity to the manifest with translucent theme
In your manifest, add the activity created above  
`<activity android:name=".SendLogActivity" android:theme="@android:style/Theme.Translucent.NoTitleBar"/>`

###Start the activity at a suitable point in your app
Somewhere in your app, for example in a Help or About menu, you'll want to allow users to send
the log.  
`startActivity(new Intent(this, SendLogActivity.class));`

That's it!