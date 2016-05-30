package com.cax.pmk;

import java.text.MessageFormat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.view.ContextThemeWrapper;
import android.widget.TextView;
import com.cax.pmk.R;

public class MenuHelper {

	static MainActivity mainActivity;
	
	private static String getString(int resId) { return mainActivity.getString(resId); }

	static void aboutDialog() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mainActivity);
 
		// set title
		alertDialogBuilder.setTitle(getString(R.string.menu_about) + " " + getString(R.string.app_name));
 
		String versionName = "";
		try {
			versionName = mainActivity.getPackageManager().getPackageInfo(mainActivity.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
		    e.printStackTrace();
		}
		    
		// set dialog message
		alertDialogBuilder.setMessage(MessageFormat.format(getString(R.string.msg_about), versionName));
		
		alertDialogBuilder.setNegativeButton(getString(R.string.label_close), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
			}
		});

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();
 		 
		// show it
		alertDialog.show();
 
		// change font size
		TextView textView = (TextView) alertDialog.findViewById(android.R.id.message);
		textView.setTextSize(14);

	}

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
