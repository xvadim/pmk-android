package com.cax.pmk;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;

import static java.text.MessageFormat.format;

public class InfoActivity extends Activity {

    public static final String KEY_DESCRIPTION_FILE = "keyDescriptionFile";
    public static final String KEY_REGS_DUMP = "keyRegsDump";

    private static final String sKeyInfoMode = "keyInfoMode";

    private WebView mWebView;

    private String mDescription = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.info_layout);

        mWebView = findViewById(R.id.info_view);
        mWebView.setBackgroundColor(Color.parseColor("#BBBBBB"));

        Bundle args = getIntent().getExtras();
        String descrFile;
        if (args != null) {
            descrFile = args.getString(KEY_DESCRIPTION_FILE);
            if (descrFile != null) {
               mDescription = "file://" + descrFile;
            }
        }

        //TODO: switch to the previous mode
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        int mode = sharedPref.getInt(sKeyInfoMode, 0);

        if (mode == 0) {
            mWebView.loadUrl(getString(R.string.info_panel_description_file));
        } else {
            switchMode(mode);
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    public void onClick(View v) {
        int butId = v.getId();
        if (butId == R.id.butExit) {
            finish();
            return;
        }

        switchMode(butId);
    }

    private void switchMode(int modeId) {
        SharedPreferences.Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        prefEditor.putInt(sKeyInfoMode, modeId).apply();

        switch (modeId) {
            case R.id.butDescription:
                if (mDescription == null) {
                    mWebView.loadData(getString(R.string.default_description),
                            "text/html; charset=utf-8", "UTF-8");
                } else {
                    mWebView.loadUrl(mDescription);
                }
                break;
            case R.id.butInstruction:
                mWebView.loadUrl("file:///android_asset/instruction.html");
                break;
            case R.id.butDonate:
                mWebView.loadData(getString(R.string.msg_donate), "text/html; charset=utf-8", "UTF-8");
                break;
            case R.id.butAbout:
                openAbout();
                break;
        }
    }

    private void openAbout() {
        String versionName = "1.0";
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        mWebView.loadData(format(getString(R.string.msg_about), versionName),
                "text/html; charset=utf-8", "UTF-8");
    }
}
