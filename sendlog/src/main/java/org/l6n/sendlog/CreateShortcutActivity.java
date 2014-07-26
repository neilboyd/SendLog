package org.l6n.sendlog;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

public class CreateShortcutActivity extends Activity implements TextWatcher, OnClickListener {

    // private static final String TAG = "CreateShortcutActivity";

    private EditText mNameView;
    private EditText mDestView;
    private Spinner mFormatSpinner;
    private Spinner mSenderSpinner;
    private SenderAdapter mSenderAdapter;

    @Override
    public void onCreate(final Bundle pSavedInstanceState)
    {
        // Log.v(TAG, "onCreate");
        super.onCreate(pSavedInstanceState);
        setContentView(R.layout.shortcut);

        mNameView = (EditText) findViewById(R.id.shortcut_name);
        mNameView.setError(getText(R.string.shortcut_no_text_error));
        mNameView.addTextChangedListener(this);
        // XXX could lookup contacts, but that will require permission

        findViewById(R.id.shortcut_ok).setOnClickListener(this);
        findViewById(R.id.shortcut_cancel).setOnClickListener(this);

        mDestView = (EditText) findViewById(R.id.shortcut_destination);
        mDestView.setError(getText(R.string.shortcut_no_text_error));
        mDestView.addTextChangedListener(this);

        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        final int format = sp.getInt(SendLog.FORMAT_PREFERENCE, SendLog.FORMAT_DEFAULT);
        mFormatSpinner = (Spinner) findViewById(R.id.format_spinner);
        mFormatSpinner.setSelection(format);

        mSenderAdapter = new SenderAdapter();
        mSenderSpinner = (Spinner) findViewById(R.id.sender_spinner);
        mSenderSpinner.setAdapter(mSenderAdapter.getInstance(this));
    }

    @Override
    public void afterTextChanged(final Editable pS) {

        // the same handler for both text fields - the message is generic

        if (mNameView.length() == 0) {
            mNameView.setError(getText(R.string.shortcut_no_text_error));
        } else {
            mNameView.setError(null);
        }

        if (mDestView.length() == 0) {
            mDestView.setError(getText(R.string.shortcut_no_text_error));
        } else {
            mDestView.setError(null);
        }
    }

    @Override
    public void beforeTextChanged(final CharSequence pS, final int pStart, final int pCount, final int pAfter) {
    }

    @Override
    public void onTextChanged(final CharSequence pS, final int pStart, final int pBefore, final int pCount) {
    }

    @Override
    public void onClick(final View pView) {
        // Log.v(TAG, "onClick");

        switch (pView.getId()) {
        case R.id.shortcut_ok: {
            final Intent intent = createShortcutIntent();
            if (intent != null) {
                setResult(RESULT_OK, intent);
                finish();
            }
            break;
        }

        case R.id.shortcut_cancel: {
            setResult(RESULT_CANCELED);
            finish();
            break;
        }
        }
    }

    private Intent createShortcutIntent() {

        // if name and destination are set then handle it
        // otherwise ignore it
        if (mNameView.length() > 0 && mDestView.length() > 0) {

            // create the destination intent
            final Intent intent = new Intent(this, SendLog.class);

            final CharSequence dest = mDestView.getText();
            final int format = mFormatSpinner.getSelectedItemPosition();
            final int senderPos = mSenderSpinner.getSelectedItemPosition();
            final ResolveInfo ri = mSenderAdapter.ri.get(senderPos);

            intent.setType(format + SendLog.FORMAT_SEPARATOR + ri.activityInfo.name + SendLog.FORMAT_SEPARATOR + dest.toString());

            // create the shortcut
            final Intent shortcut = new Intent();
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, ShortcutIconResource.fromContext(this, R.drawable.icon));
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, mNameView.getText().toString());

            return shortcut;
        } else {
            return null;
        }
    }

    private class SenderAdapter {
        final PackageManager pm = getPackageManager();
        final Intent sendIntent = new Intent(Intent.ACTION_SEND).setType("message/rfc822");
        final List<ResolveInfo> ri = pm.queryIntentActivities(sendIntent, PackageManager.MATCH_DEFAULT_ONLY);
        final ArrayList<String> names = new ArrayList<String>();
        final ArrayList<Drawable> icons = new ArrayList<Drawable>();

        public SenderAdapter() {
            for(final ResolveInfo info : ri) {
                final ApplicationInfo ai = info.activityInfo.applicationInfo;
                names.add(pm.getApplicationLabel(ai).toString());
                icons.add(pm.getApplicationIcon(ai));
            }
        }

        public ArrayAdapter<String> getInstance(final Context pContext) {
            return new ArrayAdapter<String>(pContext, android.R.layout.simple_spinner_item, names) {

                @Override
                public View getDropDownView(final int pPosition, final View pConvertView, final ViewGroup pParent) {
                    final View view;
                    if (pConvertView == null) {
                        // TODO http://www.doubleencore.com/2013/05/layout-inflation-as-intended/
                        view = View.inflate(getApplicationContext(), R.layout.sender_list_item, null);
                    } else {
                        view = pConvertView;
                    }

                    final ImageView iconView = (ImageView) view.findViewById(R.id.sender_icon);
                    iconView.setImageDrawable(icons.get(pPosition));

                    final CheckedTextView tv = (CheckedTextView) view.findViewById(android.R.id.text1);
                    tv.setText(names.get(pPosition));
                    tv.setChecked(pPosition == mSenderSpinner.getSelectedItemPosition());

                    return view;
                }
            };
        }
    }
}
