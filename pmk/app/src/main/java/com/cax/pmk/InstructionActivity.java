package com.cax.pmk;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebView;

/**
 * Created by vadimkhohlov on 1/9/18.
 */

public class InstructionActivity extends Activity {

    public static final String KEY_INSTRUCTION_FILE = "keyInstructionFile";
    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.description);

        Bundle args = getIntent().getExtras();

        String instructionFile = null;
        if (args != null) {
            instructionFile = args.getString(KEY_INSTRUCTION_FILE);
        }
        if (instructionFile == null) {
            instructionFile = "/android_asset/instruction.html";
        }
        mWebView = findViewById(R.id.description_content);
        mWebView.setBackgroundColor(Color.TRANSPARENT);
        mWebView.loadUrl("file://" + instructionFile);
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
