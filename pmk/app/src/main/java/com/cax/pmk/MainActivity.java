package com.cax.pmk;

import java.io.IOException;
import java.util.List;

//import com.cax.pmk.R;
import com.cax.pmk.widget.AutoScaleTextView;

import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity {

    private final static int BUTTON_SOUNDS_NUMBER = 5;
    private static final String SOUND_BUTTON_CLICK_TEMPLATE = "sounds/button_click%d.ogg";

    // slash is an empty comma placeholder in indicator font
    private static final String EMPTY_INDICATOR = "/ / / / / / / / / / / / //";


    private EmulatorInterface emulator = null;
    void setEmulator(EmulatorInterface emulator) { this.emulator = emulator; }

    private int angleMode	= 0;
    private int speedMode	= 0;
    private int mkModel		= 0; // 0 for MK-61, 1 for MK-54

    private boolean vibrate = true;
    private boolean vibrateWithMoreIntensity = false;
    private boolean buttonPressOnTouch = false;
    private boolean isLandscape = false;
    private boolean hideSwitches  = false;
    private boolean grayscale  = false;
    
    private int poweredOn = 0;
    private Vibrator vibrator = null;

    private boolean makeSounds = false;
    private int buttonSoundType = 0;
    private SoundPool soundPool = null;
    private int buttonSoundId[] = new int[BUTTON_SOUNDS_NUMBER];

    private SaveStateManager saveStateManager = null;

    // flags that regulate onPause/onResume behavior
    static boolean splashScreenMode = false;

    // ----- UI initialization - common for onCreate and onConfigurationChange -----
    private void initializeUI() {
        
        List<View> keyboardViews = SkinHelper.getAllChildrenBFS(findViewById(R.id.tableLayoutKeyboard));
        for (View view: keyboardViews) {
            if (view instanceof Button)
                ((Button)view).setOnTouchListener(onButtonTouchListener);
        }

        // let AutoScaleTextView do the work - set font size and fix layout
        TextView calculatorIndicator = (TextView) findViewById(R.id.textView_Indicator);
        calculatorIndicator.setText(EMPTY_INDICATOR);

        // preferences activation
        activateSettings();
        
        setIndicatorColor(-1);

        // set listeners for slider movement
        SeekBar angleModeSlider	= (SeekBar) findViewById(R.id.angleModeSlider);
        angleModeSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { if (fromUser) onAngleMode(progress); }
        });
        
        SeekBar powerOnOffSlider = (SeekBar) findViewById(R.id.powerOnOffSlider);
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

        findViewById(R.id.TextViewPowerOnOff).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                openOptionsMenu();
                return true;
            }
        });

        findViewById(R.id.TextViewTableCellCalculatorName).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (poweredOn == 0) {
                    setMkModel(1 - mkModel, false);
                }
                return true;
            }
        });
    }

    // ----------------------- Activity life cycle handlers --------------------------------
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // preferences initialization
        PreferenceManager.setDefaultValues(this, R.layout.activity_preferences, false);

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
        editor.commit();

        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (splashScreenMode) {
            splashScreenMode = false;
            return;
        }

        saveStateManager.saveStateStoppingEmulator(emulator, -1); // save persistence emulation state
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
            saveStateManager.loadState(emulator, -1); // load persistence emulation state
        }
    }
    
    // ----------------------- Menu hooks --------------------------------
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        MenuItem menu_save = menu.findItem(R.id.menu_save);
        MenuItem menu_export = menu.findItem(R.id.menu_export);
        MenuItem menu_swap = menu.findItem(R.id.menu_swap_model);      

        if(poweredOn == 1) 
        {           
            menu_swap.setVisible(false);
            menu_save.setVisible(true);
            menu_export.setVisible(true);
        }
        else
        {
            menu_swap.setVisible(true);
            menu_save.setVisible(false);
            menu_export.setVisible(false);
        }

        return true;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
         switch (item.getItemId()) {
            case R.id.menu_about:
                MenuHelper.aboutDialog();
                return true;
            case R.id.menu_settings:
                MenuHelper.goSettingsScreen();
                return true;
            case R.id.menu_swap_model:
                MenuHelper.onChooseMkModel(mkModel);
                return true;
            case R.id.menu_save:
                saveStateManager.chooseAndUseSaveSlot(emulator, true);
                return true;
            case R.id.menu_load:
                saveStateManager.chooseAndUseSaveSlot(emulator, false);
                return true;
             case R.id.menu_export:
                 saveStateManager.exportState(emulator);
                 return true;
             case R.id.menu_import:
                 saveStateManager.importState(emulator);
                 return true;
            default:
                return super.onOptionsItemSelected(item);
            }
    }
    
    // ----------------------- Setting controls state --------------------------------
    void setAngleModeControl(int mode) {
        angleMode = mode;
        SeekBar angleModeSlider	= (SeekBar) findViewById(R.id.angleModeSlider);
        angleModeSlider.setProgress(angleMode);

        ((RadioButton) findViewById(R.id.radioRadians)).setChecked(angleMode == 0);
        ((RadioButton) findViewById(R.id.radioGrads  )).setChecked(angleMode == 1);
        ((RadioButton) findViewById(R.id.radioDegrees)).setChecked(angleMode == 2);
    }
    
    void setPowerOnOffControl(int mode) {
        poweredOn = mode;
        SeekBar powerOnOffSlider 	= (SeekBar) findViewById(R.id.powerOnOffSlider);
        if (powerOnOffSlider   != null && powerOnOffSlider.getProgress() != mode) powerOnOffSlider.setProgress(mode);
        CheckBox powerOnOffCheckBox	= (CheckBox)findViewById(R.id.powerOnOffCheckBox);
        if (powerOnOffCheckBox != null && (powerOnOffCheckBox.isChecked() ? 1:0) != mode) powerOnOffCheckBox.setChecked(mode==1);
    }

    void toggleSwitchesVisibility() {
        hideSwitches = !hideSwitches;
        View switches = findViewById(R.id.tableLayoutSwitches);
        switches.setVisibility(hideSwitches ? View.GONE : View.VISIBLE);
    }

    // ----------------------- UI update calls, also from other thread --------------------------------
    // Show string on calculator's indicator
    public void displayIndicator(final String text) {
        runOnUiThread(new Runnable() {
           public void run() {
               TextView calculatorIndicator = (TextView) findViewById(R.id.textView_Indicator);
               if (calculatorIndicator != null)
                   calculatorIndicator.setText(text);
           }
        });
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
        poweredOn = progress;
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
    private OnTouchListener onButtonTouchListener = new OnTouchListener() {
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

        TextView calculatorIndicator = (TextView) findViewById(R.id.textView_Indicator);
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
        
        SeekBar powerOnOffSlider 	= (SeekBar) findViewById(R.id.powerOnOffSlider);
        if (powerOnOffSlider   != null) powerOnOffSlider  .setVisibility(sliderOnOff ? View.VISIBLE : View.GONE);
        CheckBox powerOnOffCheckBox	= (CheckBox)findViewById(R.id.powerOnOffCheckBox);
        if (powerOnOffCheckBox != null) powerOnOffCheckBox.setVisibility(sliderOnOff ? View.GONE    : View.VISIBLE);

        boolean sliderAngle = sharedPref.getBoolean(PreferencesActivity.PREFERENCE_SLIDER_ANGLE, 
                                                    PreferencesActivity.DEFAULT_DUMMY_BOOLEAN);
        
        SeekBar angleModeSlider	= (SeekBar) findViewById(R.id.angleModeSlider);
        angleModeSlider.setVisibility(sliderAngle ? View.VISIBLE : View.GONE);
        ((RadioButton) findViewById(R.id.radioRadians)).setVisibility(sliderAngle ? View.GONE : View.VISIBLE);
        ((RadioButton) findViewById(R.id.radioGrads  )).setVisibility(sliderAngle ? View.GONE : View.VISIBLE);
        ((RadioButton) findViewById(R.id.radioDegrees)).setVisibility(sliderAngle ? View.GONE : View.VISIBLE);
        
        // set background color, scale buttons text, set buttons borders, style labels above buttons, etc.
        // all the default values are set in preferences.xml, so second argument in getters is dummy
        SkinHelper.style(grayscale, emulator == null ? -1 : emulator.getSpeedMode(),
                Float.parseFloat(sharedPref.getString(PreferencesActivity.PREFERENCE_BUTTON_TEXT_SIZE,
                                                      PreferencesActivity.DEFAULT_DUMMY_STRING)),
                Float.parseFloat(sharedPref.getString(PreferencesActivity.PREFERENCE_LABEL_TEXT_SIZE,
                                                      PreferencesActivity.DEFAULT_DUMMY_STRING)),
                sharedPref.getBoolean(PreferencesActivity.PREFERENCE_BORDER_BLACK_BUTTONS,
                                      PreferencesActivity.DEFAULT_DUMMY_BOOLEAN),
                sharedPref.getBoolean(PreferencesActivity.PREFERENCE_BORDER_OTHER_BUTTONS,
                                      PreferencesActivity.DEFAULT_DUMMY_BOOLEAN)
        );
    }
        
    private void switchOnCalculator(boolean enable) {
        if (enable) {
            if (poweredOn == 1) {
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

            TextView calculatorIndicator = (TextView) findViewById(R.id.textView_Indicator);
            calculatorIndicator.setText(EMPTY_INDICATOR);
            
            // just in case...
            setPowerOnOffControl(0);
            
            //erase persistence file
            saveStateManager.deleteSlot(-1);

            setIndicatorColor(-1);
        }
    }

}
