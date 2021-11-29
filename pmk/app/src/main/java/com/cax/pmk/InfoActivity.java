package com.cax.pmk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Locale;

import static java.text.MessageFormat.format;

public class InfoActivity extends Activity {

    public static final String KEY_DESCRIPTION_FILE = "keyDescriptionFile";
    public static final String KEY_REGS_DUMP = "keyRegsDump";
    public static final String KEY_DONATE_MODE = "keyDonateMode";

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

        if (android.os.Build.VERSION.SDK_INT >= 24) {
            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest webResourceRequest) {
                    if (webResourceRequest.getUrl().getScheme().equals("file")) {
                        webView.loadUrl(webResourceRequest.getUrl().toString());
                    } else {
                        // If the URI is not pointing to a local file, open with an ACTION_VIEW Intent
                        webView.getContext().startActivity(new Intent(Intent.ACTION_VIEW, webResourceRequest.getUrl()));
                    }
                    return true; // in both cases we handle the link manually
                }
            });
        } else {
            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView webView, String url) {
                    if (Uri.parse(url).getScheme().equals("file")) {
                        webView.loadUrl(url);
                    } else {
                        // If the URI is not pointing to a local file, open with an ACTION_VIEW Intent
                        webView.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    }
                    return true; // in both cases we handle the link manually
                }
            });
        }

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        int mode = sharedPref.getInt(sKeyInfoMode, 0);

        Bundle args = getIntent().getExtras();
        String descrFile;
        if (args == null) {
            mRegsDump = getString(R.string.info_panel_emulator_turned_off);
        } else {
            descrFile = args.getString(KEY_DESCRIPTION_FILE);
            if (descrFile != null) {
                if (descrFile.startsWith("/")) {
                    mDescription = "file://" + descrFile;
                } else {
                    mDescription = descrFile;
                }
            }
            if (BuildConfig.IS_DONATION_INFO_ENABLED &&
                    args.getBoolean(KEY_DONATE_MODE, false)) {
                mode = R.id.butDonate;
            }

            buildRegsDumpString(args);
        }

        if (mode == 0) {
            mWebView.loadUrl(getString(R.string.info_panel_description_file));
        } else {
            switchMode(mode);
        }

        if (BuildConfig.IS_DONATION_INFO_ENABLED) {
            final Button donateBut = findViewById(R.id.butDonate);
            donateBut.setVisibility(View.VISIBLE);
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

    @SuppressLint("NonConstantResourceId")
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
                    if (mDescription.startsWith("/")) {
                        mWebView.loadUrl(mDescription);
                    } else {
                        mWebView.loadDataWithBaseURL(null, mDescription,"text/html",
                                "UTF-8", null);
                    }
                }
                break;
            case R.id.butInstruction:
                mWebView.loadUrl("file:///android_asset/instruction.html");
                break;
            case R.id.butDonate:
                if (BuildConfig.IS_DONATION_INFO_ENABLED) {
                    mWebView.loadData(getString(R.string.msg_donate), "text/html; charset=utf-8", "UTF-8");
                    break;
                }
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
        for(i = 4; i >= 0; i--) {
            regsBuffer.append(String.format(nullLocale, sRegValueTemplate, sRegNames[i],
                    String.format(nullLocale,"%-13s", regs.get(i))));
        }

        regsBuffer.append("<tr><td colspan=2><hr></td></tr>\n");

        for(i = 5 ; i < regsCount; i++) {
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
