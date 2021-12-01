package com.cax.pmk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.cax.pmk.emulator.Emulator;
import com.cax.pmk.emulator.IndicatorInterface;
import com.cax.pmk.widget.AutoScaleTextView;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity
                          implements PopupMenu.OnMenuItemClickListener,
        IndicatorInterface
{

    public static int REGISTER_X = 0;
    public static int REGISTER_Y = 1;

    private final static int BUTTON_SOUNDS_NUMBER = 5;
    private static final String SOUND_BUTTON_CLICK_TEMPLATE = "sounds/button_click%d.ogg";

    // slash is an empty comma placeholder in indicator font
    private static final String EMPTY_INDICATOR = "/ / / / / / / / / / / / //";

    private EmulatorInterface emulator = null;
    void setEmulator(EmulatorInterface emulator) {
        this.emulator = emulator;
    }

    private int angleMode	= 0;
    private int speedMode	= 0;
    private int mkModel		= 0; // 0 for MK-61, 1 for MK-54

    private boolean vibrate = true;
    private boolean vibrateWithMoreIntensity = false;
    private boolean buttonPressOnTouch = false;
    private boolean isLandscape = false;
    private boolean hideSwitches  = false;
    private boolean grayscale  = false;
    private boolean borderOtherButtons = true;
    private boolean buttonFPressed = false;
    private TextView buttonFIndicator;
    private boolean buttonKPressed = false;
    private TextView buttonKIndicator;
    private LinearLayout mYIndicator;
    public boolean mIsYIndicatorVisible = false;

    private static final int sPowerOFF = 0;
    private static final int sPowerON = 1;
    private int poweredOn = sPowerOFF;
    private Vibrator vibrator = null;

    private boolean makeSounds = false;
    private int buttonSoundType = 0;
    private SoundPool soundPool = null;
    private final int[] buttonSoundId = new int[BUTTON_SOUNDS_NUMBER];

    private SaveStateManager saveStateManager = null;

    private static final int RC_IMPORT = 12345;
    private static final int RC_EXPORT = 12346;
    private static final int RC_IMPORT_TXT = 12347;
    private static final int RC_IMPORT_DESCR = 12348;
    private static final int RC_EXPORT_TXT = 12349;

    enum ExtUriType {
        NOT_SET,
        IMPORT_PMK,
        IMPORT_TXT,
        IMPORT_DESCR,
        EXPORT,
        EXPORT_TXT,
    }

    private Uri externalUri = null;
    private ExtUriType externalUriType = ExtUriType.NOT_SET;
    private final ExtUriType[] rcToUriType = {
            ExtUriType.IMPORT_PMK,
            ExtUriType.EXPORT,
            ExtUriType.IMPORT_TXT,
            ExtUriType.IMPORT_DESCR,
            ExtUriType.EXPORT_TXT};

    // flags that regulate onPause/onResume behavior
    static boolean splashScreenMode = false;

    // ----- UI initialization - common for onCreate and onConfigurationChange -----
    private void initializeUI() {
        
        List<View> keyboardViews = SkinHelper.getAllChildrenBFS(findViewById(R.id.tableLayoutKeyboard));
        for (View view: keyboardViews) {
            if (view instanceof Button) {
                view.setOnTouchListener(onButtonTouchListener);
            }
        }

        // let AutoScaleTextView do the work - set font size and fix layout
        TextView calculatorIndicator = findViewById(R.id.textView_Indicator);
        calculatorIndicator.setText(EMPTY_INDICATOR);
        calculatorIndicator = findViewById(R.id.textView_IndicatorY);
        calculatorIndicator.setText(EMPTY_INDICATOR);

        mYIndicator = findViewById(R.id.linearLayout_IndicatorY);

        // preferences activation
        activateSettings();
        
        setIndicatorColor(-1);

        // set listeners for slider movement
        SeekBar angleModeSlider	= findViewById(R.id.angleModeSlider);
        angleModeSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { if (fromUser) onAngleMode(progress); }
        });
        
        SeekBar powerOnOffSlider = findViewById(R.id.powerOnOffSlider);
        if (powerOnOffSlider != null) powerOnOffSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {} 
            @Override public void onProgressChanged(final SeekBar seekBar, int progress, boolean fromUser) { if (fromUser) onPower(progress); }   	
        });
                
        View switches = findViewById(R.id.tableLayoutSwitches);
        switches.setVisibility(hideSwitches ? View.GONE : View.VISIBLE);
        
        findViewById(R.id.textView_Indicator).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                toggleSwitchesVisibility();
                return true;
            }
        });

        findViewById(R.id.TextViewPowerOnOff).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(MainActivity.this, v);
                popup.getMenuInflater().inflate(R.menu.main, popup.getMenu());
                onPrepareOptionsMenu(popup.getMenu());
                popup.setOnMenuItemClickListener(MainActivity.this);
                popup.show();
            }
        });

        findViewById(R.id.TextViewTableCellCalculatorName).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                setMkModel(1 - mkModel, false);
                return true;
            }
        });

        findViewById(R.id.butInfo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openInfoActivity();
            }
        });

        buttonFIndicator = findViewById(R.id.indicatorF);
        buttonKIndicator = findViewById(R.id.indicatorK);

        setPowerOn(sPowerOFF);
    }

    // ----------------------- Activity life cycle handlers --------------------------------
    @SuppressLint("ResourceType")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // preferences initialization
        PreferenceManager.setDefaultValues(this,
                R.layout.activity_preferences,
                false);

        // sound initialization
        soundPool = new SoundPool(BUTTON_SOUNDS_NUMBER, AudioManager.STREAM_MUSIC, 0);
        for (int i=0; i<BUTTON_SOUNDS_NUMBER; i++) {
            try {
                buttonSoundId[i] = soundPool.load(
                        getAssets().openFd(String.format(SOUND_BUTTON_CLICK_TEMPLATE, i+1)), 0);
            } catch (IOException ignore) {}
        }

        // UI initialization
        setContentView(R.layout.activity_main);

        saveStateManager = new SaveStateManager(this);
        MenuHelper.mainActivity = this;
        SkinHelper.mainActivity = this;

        SkinHelper.init();

        // remember vibrator service
        vibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
        
        isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        hideSwitches = sharedPref.getBoolean(PreferencesActivity.HIDE_SWITCHES_PREFERENCE_KEY,  PreferencesActivity.DEFAULT_HIDE_SWITCHES);

        initializeUI();

        // recover model, speed and angle modes from preferences even if calculator was switched off before destroying
        speedMode = sharedPref.getInt(PreferencesActivity.SPEED_MODE_PREFERENCE_KEY, PreferencesActivity.DEFAULT_SPEED_MODE);
        setAngleModeControl(sharedPref.getInt(PreferencesActivity.ANGLE_MODE_PREFERENCE_KEY, PreferencesActivity.DEFAULT_ANGLE_MODE));
        setMkModel(sharedPref.getInt(PreferencesActivity.MK_MODEL_PREFERENCE_KEY, PreferencesActivity.DEFAULT_MK_MODEL), false);
    }
        
    @Override
    public void onDestroy() {
        soundPool.release();

        saveStateManager.setMainActivity(null);
        MenuHelper.mainActivity = null;

        // remember speed mode, angle mode, mk model, etc. even if calculator was switched off before destroying
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(PreferencesActivity.SPEED_MODE_PREFERENCE_KEY, speedMode);
        editor.putInt(PreferencesActivity.ANGLE_MODE_PREFERENCE_KEY, angleMode);
        editor.putInt(PreferencesActivity.MK_MODEL_PREFERENCE_KEY,   mkModel);
        editor.putBoolean(PreferencesActivity.HIDE_SWITCHES_PREFERENCE_KEY, hideSwitches);
        editor.apply();

        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (splashScreenMode) {
            splashScreenMode = false;
            return;
        }

        saveStateManager.saveStateStoppingEmulator(emulator); // save persistence emulation state
        emulator = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (splashScreenMode) {
            return;
        }

        activateSettings();

        if (emulator == null) {
            saveStateManager.loadState(emulator); // load persistence emulation state
        }

        if (externalUri != null) {
            processExternalUri();
        }

    }

    // ----------------------- Menu hooks --------------------------------
    public boolean onPrepareOptionsMenu(Menu menu)  {
        MenuItem menu_export = menu.findItem(R.id.menu_export);
        MenuItem menu_swap = menu.findItem(R.id.menu_swap_model);
        MenuItem menu_copy_x = menu.findItem(R.id.menu_copy_x);
        MenuItem menu_import_txt = menu.findItem(R.id.menu_import_txt);
        MenuItem menu_export_txt = menu.findItem(R.id.menu_export_txt);

        if(poweredOn == sPowerON)
        {
            menu_swap.setVisible(false);
            menu_export.setVisible(true);
            menu_copy_x.setVisible(true);
            final boolean isSupportedModel = emulator != null &&
                    emulator.getMkModel() == Emulator.modelMK61;
            menu_import_txt.setVisible(isSupportedModel);
            menu_export_txt.setVisible(isSupportedModel);
        }
        else
        {
            menu_swap.setVisible(true);
            menu_export.setVisible(false);
            menu_copy_x.setVisible(false);
            menu_import_txt.setVisible(false);
            menu_export_txt.setVisible(false);
        }

        MenuItem menu_donate = menu.findItem(R.id.menu_donate);
        menu_donate.setVisible(BuildConfig.IS_DONATION_INFO_ENABLED);

        return true;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
         switch (item.getItemId()) {
             case R.id.menu_about:
                openProgsRepository();
                return true;
             case R.id.menu_settings:
                MenuHelper.goSettingsScreen();
                return true;
             case R.id.menu_swap_model:
                MenuHelper.onChooseMkModel(mkModel);
                return true;
             case R.id.menu_export:
                 exportState(ExtUriType.EXPORT);
                 return true;
             case R.id.menu_export_txt:
                 //just for robust as this item available when the emulator is turned onn and mk-61
                 if (emulator != null && emulator.getMkModel() == Emulator.modelMK61) {
                     exportState(ExtUriType.EXPORT_TXT);
                 }
                 return true;
             case R.id.menu_import:
                 importState(ExtUriType.IMPORT_PMK);
                 return true;
             case R.id.menu_import_txt:
                 //just for robust as this item available when the emulator is turned onn and mk-61
                 if (emulator != null && emulator.getMkModel() == Emulator.modelMK61) {
                     importState(ExtUriType.IMPORT_TXT);
                 }
                 return true;
             case R.id.menu_import_descr:
                 importState(ExtUriType.IMPORT_DESCR);
                 return true;
             case R.id.menu_instruction:
                 openInfoActivity();
                 return true;
             case R.id.menu_copy_x:
                 copyToClipboard();
                 return true;
             case R.id.menu_donate:
                 openDonatePage();
                return true;
             default:
                 return super.onOptionsItemSelected(item);
            }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        onOptionsItemSelected(item);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null && requestCode >= RC_IMPORT && requestCode <= RC_EXPORT_TXT) {
            if (externalUriType == ExtUriType.NOT_SET) {
                externalUriType = rcToUriType[requestCode - RC_IMPORT];
            }
            externalUri = data.getData();
        }
    }
    
    // ----------------------- Setting controls state --------------------------------
    void setAngleModeControl(int mode) {
        angleMode = mode;
        SeekBar angleModeSlider	= findViewById(R.id.angleModeSlider);
        angleModeSlider.setProgress(angleMode);

        ((RadioButton) findViewById(R.id.radioRadians)).setChecked(angleMode == 0);
        ((RadioButton) findViewById(R.id.radioGrads  )).setChecked(angleMode == 1);
        ((RadioButton) findViewById(R.id.radioDegrees)).setChecked(angleMode == 2);
    }
    
    void setPowerOnOffControl(int mode) {
        setPowerOn(mode);
        SeekBar powerOnOffSlider 	= findViewById(R.id.powerOnOffSlider);
        if (powerOnOffSlider   != null && powerOnOffSlider.getProgress() != mode) powerOnOffSlider.setProgress(mode);
        CheckBox powerOnOffCheckBox	= findViewById(R.id.powerOnOffCheckBox);
        if (powerOnOffCheckBox != null && (powerOnOffCheckBox.isChecked() ? 1:0) != mode) powerOnOffCheckBox.setChecked(mode==1);
    }

    void toggleSwitchesVisibility() {
        hideSwitches = !hideSwitches;
        View switches = findViewById(R.id.tableLayoutSwitches);
        switches.setVisibility(hideSwitches ? View.GONE : View.VISIBLE);
    }


    // ----------------------- UI update calls, also from other thread --------------------------------
    // Show string on calculator's indicator
    @Override
    public void displayIndicator(final int registerNum, final String text) {
        runOnUiThread(new Runnable() {
           public void run() {
               TextView calculatorIndicator = findViewById(registerNum == REGISTER_X ?
                       R.id.textView_Indicator : R.id.textView_IndicatorY);
               calculatorIndicator.setText(text);
           }
        });
    }

    @Override
    public boolean isYIndicatorVisible() {
        return mIsYIndicatorVisible;
    }

    @Override
    public int registerXIndex() {
        return REGISTER_X;
    }

    @Override
    public int registerYIndex() {
        return REGISTER_Y;
    }

    // ----------------------- UI call backs --------------------------------
    // calculator indicator touch callback
    public void onIndicatorTouched(View view) {
        if (emulator != null) {
            emulator.setSpeedMode(1 - emulator.getSpeedMode());
            setIndicatorColor(emulator.getSpeedMode());
        }
    }

    // calculator power switch callback
    public void onPowerCheckBoxTouched(View view) {
        onPower(((CheckBox)view).isChecked() ? 1 : 0);
    }
    
    // common code for both power slider callback and power check box callback
    private void onPower(int progress) {
        if (poweredOn == progress)
            return;
        setPowerOn(progress);
        if (vibrate) vibrator.vibrate(PreferencesActivity.VIBRATE_ON_OFF_SWITCH);
        switchOnCalculator(poweredOn == 1);
    }
    
    // calculator angle mode switch callback
    public void onAngleModeRadioButtonTouched(View view) {
        onAngleMode(Integer.parseInt((String)view.getTag()));
    }
    
    // common code for both angle slider callback and angle radio boxes callback
    private void onAngleMode(int progress) {
        angleMode = progress;
        if (emulator != null) {
            emulator.setAngleMode(angleMode);
            if (vibrate) vibrator.vibrate(PreferencesActivity.VIBRATE_ANGLE_SWITCH);
        }
    }

    // calculator button touch callback
    private final OnTouchListener onButtonTouchListener = new OnTouchListener() {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            if (buttonPressOnTouch && event.getAction() == MotionEvent.ACTION_DOWN ) {
                onKeypadButtonTouched(view);
                return true;
            } else {
                return false;
            }
        }
    };
    
    // calculator button release callback (not just touched, but released !)
    public void onKeypadButtonTouched(View view) {
        if (emulator == null || view == null || view.getTag() == null)
            return;

        // buttonSoundType, when selected in Preferences, is 1-based
        if (makeSounds && buttonSoundType > 0)
            soundPool.play(buttonSoundId[buttonSoundType-1], 1, 1, 0, 0, 1);

        if (vibrate)
            vibrator.vibrate(vibrateWithMoreIntensity
                ? PreferencesActivity.VIBRATE_KEYPAD_MORE
                : PreferencesActivity.VIBRATE_KEYPAD);

        int keycode = Integer.parseInt((String)view.getTag());
        if (keycode == 39) {    //button F
            buttonFPressed = true;
            buttonFIndicator.setVisibility(View.VISIBLE);
        } else {
            if (buttonFPressed) {
                buttonFPressed = false;
                buttonFIndicator.setVisibility(View.INVISIBLE);
            }
        }

        if (keycode == 38) {    //button K
            buttonKPressed = true;
            buttonKIndicator.setVisibility(View.VISIBLE);
        } else {
            if (buttonKPressed) {
                buttonKPressed = false;
                buttonKIndicator.setVisibility(View.INVISIBLE);
            }
        }

        emulator.keypad(keycode);
    }

    // ----------------------- Other --------------------------------
    void setIndicatorColor(int mode) {
        if (mode >= 0) speedMode = mode;
        SkinHelper.styleIndicator(grayscale, mode);
    }
    
    @SuppressLint("NewApi")
    private Point getScreenSize(Activity a) {
        Point size = new Point();
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);        
        size.x = metrics.widthPixels;
        size.y = metrics.heightPixels;
        return size;
    }
    
    void resizeIndicator() {
        AutoScaleTextView indicator = (AutoScaleTextView)findViewById(R.id.textView_Indicator);
        indicator.setWidth(getScreenSize(this).x);
        indicator.refitNow();
    }
    
    
    void setMkModel(int mkModel, boolean force) {
        boolean doNothing = false;
        if (mkModel == this.mkModel && !force)
            doNothing = true;

        SkinHelper.setMkModelName(mkModel);

        if (doNothing) return;
        
        SkinHelper.setMkModelSkin(mkModel);

        this.mkModel = mkModel;
    }

    private void activateSettings() {
        // all the default values are set in preferences.xml, so second argument in getters is dummy

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        vibrate = sharedPref.getBoolean(PreferencesActivity.PREFERENCE_VIBRATE,
                                        PreferencesActivity.DEFAULT_DUMMY_BOOLEAN);
        vibrateWithMoreIntensity = sharedPref.getBoolean(PreferencesActivity.PREFERENCE_VIBRATE_KEYPAD_MORE,
                                                         PreferencesActivity.DEFAULT_DUMMY_BOOLEAN);
        
        makeSounds = sharedPref.getBoolean(PreferencesActivity.PREFERENCE_SOUND,
                                           PreferencesActivity.DEFAULT_DUMMY_BOOLEAN);

        String buttonSoundTypeString = sharedPref.getString(PreferencesActivity.PREFERENCE_BUTTON_SOUND, 
                                                            PreferencesActivity.DEFAULT_DUMMY_STRING);
        buttonSoundType = Integer.parseInt(buttonSoundTypeString == null ? "0" : buttonSoundTypeString);
        
        buttonPressOnTouch = sharedPref.getBoolean(PreferencesActivity.PREFERENCE_ON_BUTTON_TOUCH,
                                                   PreferencesActivity.DEFAULT_DUMMY_BOOLEAN);

        grayscale = sharedPref.getBoolean(PreferencesActivity.PREFERENCE_GRAYSCALE,
                                          PreferencesActivity.DEFAULT_DUMMY_BOOLEAN);
        borderOtherButtons = sharedPref.getBoolean(PreferencesActivity.PREFERENCE_BORDER_OTHER_BUTTONS,
                                          PreferencesActivity.DEFAULT_DUMMY_BOOLEAN);

        TextView calculatorIndicator = findViewById(R.id.textView_Indicator);
        calculatorIndicator.setKeepScreenOn(
                sharedPref.getBoolean(PreferencesActivity.PREFERENCE_SCREEN_ALWAYS_ON,
                                      PreferencesActivity.DEFAULT_DUMMY_BOOLEAN));

        if (sharedPref.getBoolean(PreferencesActivity.PREFERENCE_FULL_SCREEN,
                                  PreferencesActivity.DEFAULT_DUMMY_BOOLEAN))
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        boolean sliderOnOff = sharedPref.getBoolean(PreferencesActivity.PREFERENCE_SLIDER_ON_OFF, 
                                                    PreferencesActivity.DEFAULT_DUMMY_BOOLEAN);
        
        SeekBar powerOnOffSlider 	= findViewById(R.id.powerOnOffSlider);
        if (powerOnOffSlider   != null) powerOnOffSlider  .setVisibility(sliderOnOff ? View.VISIBLE : View.GONE);
        CheckBox powerOnOffCheckBox	= findViewById(R.id.powerOnOffCheckBox);
        if (powerOnOffCheckBox != null) powerOnOffCheckBox.setVisibility(sliderOnOff ? View.GONE    : View.VISIBLE);

        boolean sliderAngle = sharedPref.getBoolean(PreferencesActivity.PREFERENCE_SLIDER_ANGLE, 
                                                    PreferencesActivity.DEFAULT_DUMMY_BOOLEAN);
        
        SeekBar angleModeSlider	= findViewById(R.id.angleModeSlider);
        angleModeSlider.setVisibility(sliderAngle ? View.VISIBLE : View.GONE);
        int visibility = sliderAngle ? View.GONE : View.VISIBLE;
        findViewById(R.id.radioRadians).setVisibility(visibility);
        findViewById(R.id.radioGrads  ).setVisibility(visibility);
        findViewById(R.id.radioDegrees).setVisibility(visibility);
        
        // set background color, scale buttons text, set buttons borders, style labels above buttons, etc.
        // all the default values are set in preferences.xml, so second argument in getters is dummy
        SkinHelper.style(grayscale, emulator == null ? -1 : emulator.getSpeedMode(),
                Float.parseFloat(sharedPref.getString(PreferencesActivity.PREFERENCE_BUTTON_TEXT_SIZE,
                                                      PreferencesActivity.DEFAULT_DUMMY_STRING)),
                Float.parseFloat(sharedPref.getString(PreferencesActivity.PREFERENCE_LABEL_TEXT_SIZE,
                                                      PreferencesActivity.DEFAULT_DUMMY_STRING)),
                sharedPref.getBoolean(PreferencesActivity.PREFERENCE_BORDER_BLACK_BUTTONS,
                                      PreferencesActivity.DEFAULT_DUMMY_BOOLEAN),
                borderOtherButtons,
                        sharedPref.getBoolean(PreferencesActivity.PREFERENCE_MEM_BUTTONS_54, false));

        mIsYIndicatorVisible = sharedPref.getBoolean(PreferencesActivity.PREFERENCE_Y_INDICATOR,
                PreferencesActivity.DEFAULT_DUMMY_BOOLEAN);
        mYIndicator.setVisibility(mIsYIndicatorVisible ? View.VISIBLE : View.GONE);

        findViewById(R.id.butInfo).setVisibility(sharedPref.getBoolean("pref_show_i_button",
                true) ? View.VISIBLE : View.INVISIBLE);
    }
        
    private void switchOnCalculator(boolean enable) {
        if (enable) {
            if (poweredOn == sPowerON) {
                emulator = new com.cax.pmk.emulator.Emulator();
                emulator.setAngleMode(angleMode);
                emulator.setSpeedMode(speedMode);
                emulator.setMkModel(mkModel);
                emulator.initTransient(this);
                setIndicatorColor(speedMode);
                emulator.start();
            }
        } else {
            if (emulator != null) {
                emulator.stopEmulator(true);
                emulator = null;
            }

            TextView calculatorIndicator = findViewById(R.id.textView_Indicator);
            calculatorIndicator.setText(EMPTY_INDICATOR);
            calculatorIndicator = findViewById(R.id.textView_IndicatorY);
            calculatorIndicator.setText(EMPTY_INDICATOR);
            
            // just in case...
            setPowerOnOffControl(0);
            
            //erase persistence file
            saveStateManager.deleteAllPersistentFiles();

            setIndicatorColor(-1);
        }

        findViewById(R.id.indicatorF).setVisibility(View.INVISIBLE);
        findViewById(R.id.indicatorK).setVisibility(View.INVISIBLE);
    }

    private void setPowerOn(int mode) {
        poweredOn = mode;
        findViewById(R.id.TextViewTableCellCalculatorName).setLongClickable(poweredOn == sPowerOFF);
    }

    private void openProgsRepository() {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://xvadim.github.io/xbasoft/pmk/pmk.html"));
            startActivity(browserIntent);
        } catch (Exception ignored) {
        }
    }

    private void openDonatePage() {
        Intent infoIntent = new Intent(this, InfoActivity.class);
        infoIntent.putExtra(InfoActivity.KEY_DONATE_MODE, true);
        startActivity(infoIntent);

        overridePendingTransition(R.anim.slide_left_in, R.anim.slide_left_out);
    }

    private void openInfoActivity() {

        Intent infoIntent = new Intent(this, InfoActivity.class);
        if (poweredOn == sPowerON && emulator != null) {
            ArrayList<String> regs = emulator.regsDumpBuffer();

            infoIntent.putExtra(InfoActivity.KEY_DESCRIPTION_FILE, saveStateManager.mProgramDescription);
            infoIntent.putExtra(InfoActivity.KEY_REGS_DUMP, regs);

        }
        startActivity(infoIntent);

        overridePendingTransition(R.anim.slide_left_in, R.anim.slide_left_out);
    }

    private void exportState(ExtUriType uriType) {
        externalUriType = uriType;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        if (uriType == ExtUriType.EXPORT) {
            intent.setType("*/pmk");
            startActivityForResult(intent, RC_EXPORT);
        } else {
            intent.setType("text/txt");
            startActivityForResult(intent, RC_EXPORT_TXT);
        }
    }


    private void importState(ExtUriType uriType) {
        externalUriType = uriType;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        if (uriType == ExtUriType.IMPORT_DESCR) {
            intent.setType("text/html");
            startActivityForResult(intent, RC_IMPORT_DESCR);
        } else {
            intent.setType("*/*");
            startActivityForResult(intent, (uriType == ExtUriType.IMPORT_PMK ? RC_IMPORT : RC_IMPORT_TXT));
        }
    }

    private void processExternalUri() {
        final Uri tempUri = externalUri;
        externalUri = null;
        switch (externalUriType) {
            case IMPORT_PMK:
                saveStateManager.importStatePmk(emulator, tempUri);
                break;
            case IMPORT_TXT:
                saveStateManager.importStateTxt(emulator, tempUri);
                break;
            case IMPORT_DESCR:
                saveStateManager.importProgDescr(tempUri);
                break;
            case EXPORT:
                saveStateManager.exportState(emulator, tempUri);
                break;
            case EXPORT_TXT:
                if (emulator == null || emulator.getMkModel() != Emulator.modelMK61) {
                    saveStateManager.showErrorMessage(R.string.import_unsupported_error);
                } else {
                    saveStateManager.exportStateTxt(emulator, tempUri);
                }
                break;
        }
    }

    private void copyToClipboard() {
        String indStr = emulator.indicatorString();
        String valueX = indStr.substring(0, 10).trim();
        int lastIdx = valueX.length() - 1;
        if (valueX.charAt(lastIdx) == '.') {
            //remove trailing coma
            valueX = valueX.substring(0, lastIdx);
        }
        if (indStr.charAt(11) != ' ') {
            //add exponent if it needed
            valueX = valueX + "e" + indStr.substring(10);
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("MK-54/61 Emulator", valueX);
        clipboard.setPrimaryClip(clip);
    }
}
