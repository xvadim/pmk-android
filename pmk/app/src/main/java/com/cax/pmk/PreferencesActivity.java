package com.cax.pmk;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import com.cax.pmk.R;

public class PreferencesActivity extends PreferenceActivity {

	// ui visual settings
	static final String PREFERENCE_SLIDER_ON_OFF		= "pref_slider_on_off";
	static final String PREFERENCE_SLIDER_ANGLE			= "pref_slider_angle";
	static final String PREFERENCE_BORDER_BLACK_BUTTONS = "pref_border_black_buttons";
	static final String PREFERENCE_BORDER_OTHER_BUTTONS = "pref_border_other_buttons";
	static final String PREFERENCE_BUTTON_TEXT_SIZE		= "pref_button_text_size";
	static final String PREFERENCE_LABEL_TEXT_SIZE		= "pref_label_text_size";

	// ui behavior settings
	static final String PREFERENCE_SCREEN_ALWAYS_ON 	= "pref_screen_always_on";
	static final String PREFERENCE_FULL_SCREEN			= "pref_full_screen";
	static final String PREFERENCE_GRAYSCALE			= "pref_grayscale";
	static final String PREFERENCE_VIBRATE          	= "pref_vibrate";
	static final String PREFERENCE_VIBRATE_KEYPAD_MORE	= "pref_vibrate_keypad_more";
	static final String PREFERENCE_SOUND				= "pref_sound";
	static final String PREFERENCE_BUTTON_SOUND			= "pref_button_sound";
	static final String PREFERENCE_ON_BUTTON_TOUCH		= "pref_on_button_touch";

	static final String PREFERENCE_DATA_STORAGE         = "pref_data_storage";
	//by default use the last dir
    static final String PREFERENCE_DATA_STORAGE_DEF_VALUE = "2";

	static final String PREFERENCE_INDICATOR_DEL_LAST   = "pref_del_last_digit";
	
	// internal settings
	static final String ANGLE_MODE_PREFERENCE_KEY		= "angleMode";
	static final String SPEED_MODE_PREFERENCE_KEY		= "speedMode";
	static final String MK_MODEL_PREFERENCE_KEY			= "mkModel";
	static final String HIDE_SWITCHES_PREFERENCE_KEY	= "hideSwitches";
	static final String PROGRAMS_DIR_KEY                = "programsDir";

	static final String PREFERENCE_MEM_BUTTONS_54 = "pref_mem_buttons_54";
	static final String PREFERENCE_Y_INDICATOR = "pref_y_indicator";
	
	// defaults
	public static final boolean DEFAULT_DUMMY_BOOLEAN		 = false;
	public static final String  DEFAULT_DUMMY_STRING		 = null;
	
	static final int     DEFAULT_ANGLE_MODE  		= 0; // radians
	static final int     DEFAULT_SPEED_MODE  		= 1; // slow mode
	static final int     DEFAULT_MK_MODEL    		= 0; // MK 61
	static final boolean DEFAULT_HIDE_SWITCHES		= false;
	static final boolean DEFAULT_VDM    			= true; // VDM on
	
	static final int    VIBRATE_ON_OFF_SWITCH = 50;
	static final int    VIBRATE_ANGLE_SWITCH  = 20;
	static final int    VIBRATE_KEYPAD 		  = 15;
	static final int    VIBRATE_KEYPAD_MORE	  = 25;
	
	@SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.activity_preferences);
	}
	
}