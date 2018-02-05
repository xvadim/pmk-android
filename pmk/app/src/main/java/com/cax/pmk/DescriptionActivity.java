package com.cax.pmk;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

/**
 * Created by vadimkhohlov on 1/23/17.
 */

public class DescriptionActivity extends Activity {

    public static final String KEY_DESCRIPTION = "keyDescription";

    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.description);

        Bundle args = getIntent().getExtras();

        String description = null;
        if (args != null) {
            description = args.getString(KEY_DESCRIPTION);
        }

        mWebView = (WebView)findViewById(R.id.description_content);
        mWebView.loadData(description != null ? description : getString(R.string.default_description),
                "text/html; charset=utf-8", "UTF-8");

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
