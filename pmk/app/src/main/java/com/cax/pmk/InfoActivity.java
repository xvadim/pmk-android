package com.cax.pmk;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.ArrayList;
import java.util.Locale;

import static java.text.MessageFormat.format;

public class InfoActivity extends Activity {

    public static final String KEY_DESCRIPTION_FILE = "keyDescriptionFile";
    public static final String KEY_REGS_DUMP = "keyRegsDump";

    private static final String sKeyInfoMode = "keyInfoMode";

    private WebView mWebView;

    private String mDescription = null;
    private String mRegsDump = null;

    private static final String[] sRegNames = {
            "X1", "X", "Y", "Z", "T",
            "0", "1", "2", "3", "4", "5", "6", "7",
            "8", "9", "a", "b", "c", "d", "e"
    };
    private static final String sRegValueTemplate = "<tr><td align='right'>%s:</td><td><span id=code>%s</span></td></tr>\n";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.info_layout);

        mWebView = findViewById(R.id.info_view);
        mWebView.setBackgroundColor(Color.parseColor("#BBBBBB"));
        mWebView.setWebViewClient(new WebViewClient());

        Bundle args = getIntent().getExtras();
        String descrFile;
        if (args == null) {
            mRegsDump = getString(R.string.info_panel_emulator_turned_off);
        } else {
            descrFile = args.getString(KEY_DESCRIPTION_FILE);
            if (descrFile != null) {
               mDescription = "file://" + descrFile;
            }

            buildRegsDumpString(args);
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

        mWebView.getSettings().setTextZoom(100);
        switch (modeId) {
            case R.id.butRegs:
                mWebView.getSettings().setTextZoom(120);
                mWebView.loadDataWithBaseURL("file:///android_asset/",
                        mRegsDump, "text/html; charset=utf-8", "UTF-8", null);
                break;
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

    private void buildRegsDumpString(Bundle args) {
        ArrayList<String> regs = args.getStringArrayList(KEY_REGS_DUMP);
        if (regs == null) {
            return;
        }

        StringBuilder regsBuffer = new StringBuilder();
        int regsCount = regs.size() - 1;    //the e reg may be absent
        int i;
        Locale nullLocale = null;
        for(i = 0; i < 5; i++) {
            regsBuffer.append(String.format(nullLocale, sRegValueTemplate, sRegNames[i],
                    String.format(nullLocale,"%-13s", regs.get(i))));
        }

        regsBuffer.append("<tr><td colspan=2><hr></td></tr>\n");

        for( ; i < regsCount; i++) {
            regsBuffer.append(String.format(nullLocale, sRegValueTemplate, sRegNames[i],
                    String.format(nullLocale,"%-13s", regs.get(i))));
        }

        String regE = regs.get(i);
        if (regE.length() > 0) {
            regsBuffer.append(String.format(nullLocale, sRegValueTemplate, sRegNames[i],
                    String.format(nullLocale,"%-13s", regs.get(i))));
        }

        mRegsDump = String.format(nullLocale, getString(R.string.info_regs_dump_header),
                regsBuffer.toString());


    }
}
