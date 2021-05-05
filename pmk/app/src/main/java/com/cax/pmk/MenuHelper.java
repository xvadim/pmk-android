package com.cax.pmk;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.ContextThemeWrapper;

public class MenuHelper {

	static MainActivity mainActivity;
	
	private static String getString(int resId) { return mainActivity.getString(resId); }

    // calculator model menu option callback
    static void onChooseMkModel(int mkModel) {
	    ContextThemeWrapper cw = new ContextThemeWrapper(mainActivity, R.style.AlertDialogTheme );
		AlertDialog.Builder builder = new AlertDialog.Builder(cw);
	
		builder.setSingleChoiceItems(new String[] {getString(R.string.item_mk61), getString(R.string.item_mk54)}, mkModel, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		    	mainActivity.setMkModel(item, false);
				dialog.cancel();
		    }
		});

		AlertDialog alert = builder.create();
		alert.show();		
	}

    // on settings menu selected
    static void goSettingsScreen() {
    	Intent settingsScreen = new Intent(mainActivity.getApplicationContext(), PreferencesActivity.class);
    	mainActivity.startActivity(settingsScreen);
    }

}
