package com.cax.pmk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputFilter;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.Toast;

import com.cax.pmk.emulator.Emulator;

public class SaveStateManager {
	
	private static final String PERSISTENCE_STATE_FILENAME 	= "persist";
    private static final String PERSISTENCE_STATE_EXTENSION = ".pmk";
	
	private static final int    EDIT_TEXT_ENTRY_ID = 12345; // it has to be some number, so let it be 12345
	private static final int    SAVE_SLOTS_NUMBER = 99;
	private static final int    SAVE_NAME_MAX_LEN = 25;

	private static int selectedSaveSlot = 0;
    private static int tempSaveSlot = 0;

	MainActivity mainActivity;
    String mProgramDescription = null;
	
	private String getString(int resId) { return mainActivity.getString(resId); }
	private File getFileStreamPath(String file) { return mainActivity.getFileStreamPath(file); }
	
	SaveStateManager(MainActivity mainActivity) {
		this.mainActivity = mainActivity;  
	}
	
	void setMainActivity(MainActivity mainActivity) {
		this.mainActivity = mainActivity;  
	}
	// ----------------------- Dialogs --------------------------------
    void chooseAndUseSaveSlot(final EmulatorInterface emulator, final boolean save) {
		if (save && emulator == null) // disable saving when calculator is switched off
			return;
		
	    final CharSequence[] items = new CharSequence[SAVE_SLOTS_NUMBER];
	    for (int i=0; i < SAVE_SLOTS_NUMBER; i++) {
    		items[i] = getSlotDisplayName(i);
    	}
	
	    ContextThemeWrapper cw = new ContextThemeWrapper(mainActivity, R.style.AlertDialogTheme);
		AlertDialog.Builder builder = new AlertDialog.Builder(cw);
		builder.setTitle(getString(R.string.msg_choose_slot) + " " + (save ? getString(R.string.msg_save) : getString(R.string.msg_load)));
	
		builder.setSingleChoiceItems(items, selectedSaveSlot, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		    	tempSaveSlot = item;
		    }
		});
	
		builder.setPositiveButton(save ? getString(R.string.label_save) : getString(R.string.label_load), 
				new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int id) {
		    	if (save) {
		    		chooseNameAndSaveState(emulator);
		    	} else {
			    	if (loadState(emulator, tempSaveSlot))
				    	selectedSaveSlot = tempSaveSlot;
		    	}
		    }
		});
	
		builder.setNegativeButton(getString(R.string.label_cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
			}
		});

		AlertDialog alert = builder.create();
		alert.show();		
	}

	// ----------------------- Save/Load emulator state --------------------------------
    private void chooseNameAndSaveState(final EmulatorInterface emulator) {
    	String chosenName = emulator.getSaveStateName();
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle(getString(R.string.msg_choose_name));
        EditText input = new EditText(mainActivity);
        input.setId(EDIT_TEXT_ENTRY_ID);
        input.append(chosenName);
        input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(SAVE_NAME_MAX_LEN) });
        builder.setView(input);
        builder.setPositiveButton(getString(R.string.label_save), new DialogInterface.OnClickListener() {
            //@Override
            public void onClick(DialogInterface dialog, int which) {
                EditText input = (EditText) ((Dialog)dialog).findViewById(EDIT_TEXT_ENTRY_ID);
                Editable value = input.getText();
                emulator.setSaveStateName(value.toString());
	    		saveStateStoppingEmulator(emulator, tempSaveSlot); // stop and save
	    		mainActivity.setEmulator(null);
	    		loadState(null, tempSaveSlot); // keep going !
		    	selectedSaveSlot = tempSaveSlot;
            }
        });
        builder.show();

    }
    
    private String getSlotDisplayName(int i) {
    	String filename = getSlotFilename(i);
    	String i_str = "0" + (i+1);
    	i_str = i_str.substring(i_str.length()-2);
    	String slotName = "";
    	if (!getFileStreamPath(getSlotFilename(i)).exists()) {
    		slotName = getString(R.string.savestate_name_slot) + " " + i_str + " " + getString(R.string.savestate_name_empty);
    	} else {
			FileInputStream fileIn = null;
			ObjectInputStream in = null;
	
			try {
				fileIn = mainActivity.openFileInput(filename);
				in = new ObjectInputStream(fileIn);
				com.cax.pmk.emulator.Emulator.readStateNamesMode = true;
				EmulatorInterface emulatorObjForStateNameOnly = (com.cax.pmk.emulator.Emulator) in.readObject();
				if (emulatorObjForStateNameOnly.getSaveStateName() != null && emulatorObjForStateNameOnly.getSaveStateName().length() > 0)
					slotName = emulatorObjForStateNameOnly.getSaveStateName();
				in.close();
				fileIn.close();
		    } catch(Exception e) {
		    	e.printStackTrace();
			} finally {
				com.cax.pmk.emulator.Emulator.readStateNamesMode = false;
				try { if (in != null) in.close(); } catch(IOException ignored) {}
				try { if (fileIn != null) fileIn.close(); } catch(IOException ignored) {}
			}
    	}
    	return "[" + i_str + "] " + ("".equals(slotName) ? getString(R.string.savestate_name_noname) : slotName);
    }
	
	private String getSlotFilename(int slotNumber) {
    	String filename;
    	if (slotNumber < 0)
    		filename = PERSISTENCE_STATE_FILENAME;
    	else
    		filename = "slot" + slotNumber;
    	filename += PERSISTENCE_STATE_EXTENSION;
    	return filename;
    }
    
    boolean saveStateStoppingEmulator(EmulatorInterface emulator, int slotNumber) {
    	if (emulator == null)
    		return false;
    	
    	String filename = getSlotFilename(slotNumber);
    	
    	FileOutputStream fileOut = null;
        try {
            fileOut = mainActivity.openFileOutput(filename, Context.MODE_PRIVATE);
            return saveStateStoppingEmulatorToFile(emulator, fileOut);
        } catch(IOException i) {
            return false;
        } finally {
            mainActivity.setEmulator(null);
            try { if (fileOut != null) fileOut.close(); } catch(IOException i) {}
        }
    }

    boolean exportState(final EmulatorInterface emulator) {
        if (emulator == null) // disable saving when calculator is switched off
            return false;

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            showErrorMessage(R.string.export_no_sd_card_error);
            return false;
        }

        SimpleFileDialog FileOpenDialog =  new SimpleFileDialog(mainActivity, "FileSave",
                new SimpleFileDialog.SimpleFileDialogListener()
                {
                    @Override
                    public void onChosenDir(String chosenFileName)
                    {
                        File file = new File(chosenFileName);
                        saveProgramsDir(file);

                        FileOutputStream fileOut = null;
                        try {
                            emulator.setSaveStateName(chosenFileName);
                            fileOut = new FileOutputStream(file);
                            if (!saveStateStoppingEmulatorToFile(emulator, fileOut)) {
                                showErrorMessage(R.string.export_common_error);
                            }

                            //keep going
                            mainActivity.setEmulator(null);
                            FileInputStream fileIn = new FileInputStream(file);
                            if (!loadStateFromFile(null, fileIn)) {
                                showErrorMessage(R.string.export_common_error);
                            }
                        } catch (IOException e) {
                            showErrorMessage(R.string.export_common_error);
                        } finally {
                            try { if (fileOut != null) fileOut.close(); } catch(IOException ignored) {}
                        }
                    }
                });

        setupDataStorage(FileOpenDialog);

        return true;
    }

    private boolean saveStateStoppingEmulatorToFile(EmulatorInterface emulator, FileOutputStream fileOut) {
        ObjectOutputStream out = null;

        emulator.stopEmulator(false);

        try {
            out = new ObjectOutputStream(fileOut);
            out.writeObject(emulator);
            return true;
        } catch(IOException i) {
            return false;
        } finally {
            mainActivity.setEmulator(null);
            try { if (out != null)         out.close(); } catch(IOException ignored) {}
        }
    }

    boolean loadState(EmulatorInterface emulator, int slotNumber) {
    	String filename = getSlotFilename(slotNumber);
    	
    	if (! getFileStreamPath(filename).exists())
    		return false;
    	
		FileInputStream fileIn = null;
        boolean isLoaded = false;
        try {
            fileIn = mainActivity.openFileInput(filename);
            isLoaded = loadStateFromFile(emulator, fileIn);
        } catch(Exception i) {
            return false;
        } finally {
            try { if (fileIn != null) fileIn.close(); } catch(IOException ignored) {}
        }
        return isLoaded;
    }


    boolean importState(final EmulatorInterface emulator) {

        SimpleFileDialog FileOpenDialog =  new SimpleFileDialog(mainActivity, "FileOpen",
                new SimpleFileDialog.SimpleFileDialogListener()  {
                    @Override
                    public void onChosenDir(String chosenFileName)  {
                        File file = new File(chosenFileName);
                        saveProgramsDir(file);

                        FileInputStream fileIn = null;
                        mProgramDescription = null;
                        try {
                            fileIn = new FileInputStream(file);
                            if (chosenFileName.endsWith(".pmk")) {  //binary emulator's dump
                                if (!loadStateFromFile(emulator, fileIn)) {
                                    showErrorMessage(R.string.import_common_error);
                                } else {
                                    loadProgramDescription(chosenFileName);
                                }
                            } else {
                                //import from a text file
                                importTxtProgram(emulator, fileIn);
                            }

                        } catch (ParserException parseEx) {
                            showErrorMessage(mainActivity.getString(R.string.import_parse_error, parseEx.cmd));
                        } catch (Exception e) {
                            showErrorMessage(R.string.import_common_error);
                        } finally {
                            try { if (fileIn != null) fileIn.close(); } catch(IOException ignored) {}
                        }
                    }
                });

        setupDataStorage(FileOpenDialog);

        return true;
    }

    private void setupDataStorage(SimpleFileDialog FileOpenDialog) {
        String programsDir = null;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainActivity);
        String dataStorage = prefs.getString(PreferencesActivity.PREFERENCE_DATA_STORAGE,
                PreferencesActivity.PREFERENCE_DATA_STORAGE_DEF_VALUE);

        if (dataStorage.equals(PreferencesActivity.PREFERENCE_DATA_STORAGE_DEF_VALUE)) {
            programsDir = prefs.getString(PreferencesActivity.PROGRAMS_DIR_KEY,
                    PreferencesActivity.DEFAULT_DUMMY_STRING);
        }
        FileOpenDialog.Default_File_Name = "dump.pmk";
        if (programsDir == null) {
            FileOpenDialog.chooseFile_or_Dir();
        } else {
            FileOpenDialog.chooseFile_or_Dir(programsDir);
        }
    }

    private boolean loadStateFromFile(EmulatorInterface emulator, FileInputStream fileIn) {
        ObjectInputStream in = null;
        EmulatorInterface loadedEmulator;
        try {
            in = new ObjectInputStream(fileIn);
            loadedEmulator = (com.cax.pmk.emulator.Emulator) in.readObject();
            in.close();
            fileIn.close();

        } catch(Exception i) {
            return false;
        } finally {
            try {
                if (in != null)         in.close();
            } catch(IOException ignored) {}
        }

        if (emulator != null) {
            emulator.stopEmulator(false);
        }

        mainActivity.setEmulator(loadedEmulator);
        emulator = loadedEmulator;
        emulator.initTransient(mainActivity);

        mainActivity.setMkModel(emulator.getMkModel(), false);

        mainActivity.setIndicatorColor(emulator.getSpeedMode());
        mainActivity.setAngleModeControl(emulator.getAngleMode());
        mainActivity.setPowerOnOffControl(1);

        emulator.start();

        return true;
    }

    private void loadProgramDescription(String pFileName) {
        mProgramDescription = null;
        try {
            int pointIndex = pFileName.lastIndexOf('.');
            if (pointIndex != -1) {
                pointIndex++;
                StringBuilder descrFileName = new StringBuilder(pFileName);
                descrFileName.replace(pointIndex, pFileName.length(), "html");
                mProgramDescription = descrFileName.toString();
            }
        } catch (Exception ex) {}
    }

    private void importTxtProgram(final EmulatorInterface emulator, FileInputStream fileIn)
            throws IOException, ParserException {

        if (emulator == null) {
            showErrorMessage(R.string.import_turned_off_error);
            return;
        }

        if (emulator.getMkModel() != Emulator.modelMK61) {
            showErrorMessage(R.string.import_unsupported_error);
            return;
        }

        BufferedReader buf = new BufferedReader(new InputStreamReader(fileIn));
        final StringBuilder stringBuilder = new StringBuilder();

        String line = buf.readLine();
        while(line != null){
            line = line.trim();
            //skip lines like '#	|	00	01	02	03	04	05	06	07	08	09'
            if (!line.startsWith("#")) {
                stringBuilder.append(line);
                stringBuilder.append(' ');
            }
            line = buf.readLine();
        }
        buf.close();

        //replace parts '00	|	В/О' to '00.В/О' and Russian letters to English ones
        String prgString = stringBuilder.toString().toUpperCase()
                .replaceAll("\\s+\\|\\s+", ".")
                .replaceAll("А", "A")
                .replaceAll("В", "B")
                .replaceAll("С", "C")
                .replaceAll("Д", "D")
                .replaceAll("Е", "E")
                .replaceAll("К", "K")
                .replaceAll("М", "M")
                .replaceAll("О", "O")
                .replaceAll("Х", "X")
                ;
        String[] prgCommands = prgString.split("\\s+");
        CommandParser cmdParser = new CommandParser();
        int addr = 0;
        for (String cmd : prgCommands) {
            cmdParser.parseCommand(addr, cmd);
            emulator.storeCmd(cmdParser.cmdAddress(), cmdParser.cmdCode());
            addr++;
        }
        //inform the emulator that the program is ready to import
        emulator.setImportPrgSize(addr);

        showErrorMessage(R.string.import_successfull);
    }

    public void deleteSlot(int slot) {
        File file = getFileStreamPath(getSlotFilename(slot));
        if (file.exists())
        	file.delete();
    }

    private void showErrorMessage(int errorTextID) {
        showErrorMessage(mainActivity.getString(errorTextID));
    }

    private void showErrorMessage(String message) {
        Toast toast = Toast.makeText(mainActivity, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();
    }

    private void saveProgramsDir(File pFile) {
	    String dir = pFile.getParent();
	    if (dir != null) {
            SharedPreferences.Editor sharedPrefEditor = PreferenceManager.getDefaultSharedPreferences(mainActivity).edit();
            sharedPrefEditor.putString(PreferencesActivity.PROGRAMS_DIR_KEY, dir);
            sharedPrefEditor.apply();
        }
    }
}
