package com.cax.pmk;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.webkit.WebView;

/**
 * Created by vadimkhohlov on 1/9/18.
 */

public class InstructionActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.description);

        WebView instruction_text = (WebView)findViewById(R.id.description_content);
        instruction_text.setBackgroundColor(Color.TRANSPARENT);
        instruction_text.loadUrl("file:///android_asset/instruction.html");
    }
}
