package com.cax.pmk;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.view.Gravity;
import android.widget.Toast;

import com.cax.pmk.emulator.Emulator;

public class SaveStateManager {
	
	private static final String PERSISTENCE_STATE_FILENAME 	= "persist.pmk";
	MainActivity mainActivity;
    String mProgramDescription = null;
	
	private File getFileStreamPath(String file) { return mainActivity.getFileStreamPath(file); }
	
	SaveStateManager(MainActivity mainActivity) {
		this.mainActivity = mainActivity;  
	}
	
	void setMainActivity(MainActivity mainActivity) {
		this.mainActivity = mainActivity;  
	}


    void saveStateStoppingEmulator(EmulatorInterface emulator) {
    	if (emulator == null)
    		return ;
    	
    	FileOutputStream fileOut = null;
        try {
            fileOut = mainActivity.openFileOutput(PERSISTENCE_STATE_FILENAME, Context.MODE_PRIVATE);
            saveStateStoppingEmulatorToFile(emulator, fileOut);
        } catch(IOException ignored) {
        } finally {
            mainActivity.setEmulator(null);
            try { if (fileOut != null) fileOut.close(); } catch(IOException i) {}
        }
    }

    /*
    void exportState(final EmulatorInterface emulator) {
        if (emulator == null) // disable saving when calculator is switched off
            return;

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            showErrorMessage(R.string.export_no_sd_card_error);
            return;
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
                            String resSaving = saveStateStoppingEmulatorToFile(emulator, fileOut);
                            if (resSaving != null) {
                                showErrorMessage(mainActivity.getString(R.string.export_common_error) +
                                        ":" + resSaving);
                            }

                            //keep going
                            mainActivity.setEmulator(null);
                            FileInputStream fileIn = new FileInputStream(file);
                            resSaving = loadStateFromFile(null, fileIn);
                            if (resSaving != null) {
//                                showErrorMessage(R.string.export_common_error);
                                showErrorMessage(mainActivity.getString(R.string.export_common_error) +
                                        ":" + resSaving);
                            }
                        } catch (IOException e) {
//                            showErrorMessage(R.string.export_common_error);
                            showErrorMessage(mainActivity.getString(R.string.export_common_error) +
                                    ":" + e.getMessage());
                        } finally {
                            try { if (fileOut != null) fileOut.close(); } catch(IOException ignored) {}
                        }
                    }
                });

        setupDataStorage(FileOpenDialog);
    }
     */

    private String saveStateStoppingEmulatorToFile(EmulatorInterface emulator, OutputStream fileOut) {
        ObjectOutputStream out = null;

        emulator.stopEmulator(false);

        try {
            out = new ObjectOutputStream(fileOut);
            out.writeObject(emulator);
            return null;
        } catch(IOException i) {
            return i.getMessage();
        } finally {
            mainActivity.setEmulator(null);
            try {
                if (out != null)
                    out.close();
            } catch(IOException ignored) {}
        }
    }

    void loadState(EmulatorInterface emulator) {

    	if (! getFileStreamPath(PERSISTENCE_STATE_FILENAME).exists())
    		return;
    	
		FileInputStream fileIn = null;
        try {
            fileIn = mainActivity.openFileInput(PERSISTENCE_STATE_FILENAME);
            loadStateFromFile(emulator, fileIn);
        } catch(Exception ignored) {
        } finally {
            try { if (fileIn != null) fileIn.close(); } catch(IOException ignored) {}
        }
    }

    void exportState(final EmulatorInterface emulator, Uri uri) {
        if (emulator == null) // disable saving when calculator is switched off
            return;

        final ContentResolver cr = mainActivity.getContentResolver();
        OutputStream os;
        InputStream is;
        try {
            os = cr.openOutputStream(uri);
            String resSaving = saveStateStoppingEmulatorToFile(emulator, os);
            if (resSaving != null) {
                showErrorMessage(mainActivity.getString(R.string.export_common_error) + ":" + resSaving);
                return;
            }
            //keep going
            mainActivity.setEmulator(null);
            is = cr.openInputStream(uri);
            resSaving = loadStateFromFile(null, is);
            if (resSaving != null) {
                showErrorMessage(mainActivity.getString(R.string.export_common_error) + ":" + resSaving);
            }
        } catch (RuntimeException rethrown) {
            throw rethrown;
        } catch (Exception ignored) {
            showErrorMessage(mainActivity.getString(R.string.export_common_error));
        }
    }

    void importStatePmk(final EmulatorInterface emulator, Uri uri) {
        final ContentResolver cr = mainActivity.getContentResolver();
        InputStream is;
        try {
            is = cr.openInputStream(uri);
            if (is == null) {
                showErrorMessage(R.string.import_common_error);
                return;
            }
            if (loadStateFromFile(emulator, is) != null) {
                showErrorMessage(R.string.import_common_error);
            } else {
                showErrorMessage(R.string.import_successfull);
            }
        } catch (RuntimeException rethrown) {
            throw rethrown;
        } catch (Exception ignored) {
            showErrorMessage(R.string.import_common_error);
        }
    }

    void importStateTxt(final EmulatorInterface emulator, Uri uri) {
        final ContentResolver cr = mainActivity.getContentResolver();
        InputStream is;
        try {
            is = cr.openInputStream(uri);
            if (is == null) {
                showErrorMessage(R.string.import_common_error);
                return;
            }
            importTxtProgram(emulator, is);
        } catch (ParserException parseEx) {
            showErrorMessage(mainActivity.getString(R.string.import_parse_error, parseEx.cmd));
        } catch (RuntimeException rethrown) {
            throw rethrown;
        } catch (Exception ignored) {
            showErrorMessage(R.string.import_common_error);
        }
    }

    void importProgDescr(Uri uri) {
        mProgramDescription = null;
        final ContentResolver cr = mainActivity.getContentResolver();
        InputStream is = null;
        BufferedReader buf = null;
        try {
            is = cr.openInputStream(uri);
            if (is == null) {
                return;
            }
            buf = new BufferedReader(new InputStreamReader(is));
            final StringBuilder stringBuilder = new StringBuilder();

            String line = buf.readLine();
            while(line != null){
                stringBuilder.append(line);
                stringBuilder.append('\n');
                line = buf.readLine();
            }
            buf.close();
            is.close();
            mProgramDescription = stringBuilder.toString();
        } catch (RuntimeException rethrown) {
            throw rethrown;
        } catch (Exception ignored) {
            showErrorMessage(R.string.import_common_error);
        } finally {
            closeQuietly(buf);
            closeQuietly(is);
        }
    }

    /*
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
                                if (loadStateFromFile(emulator, fileIn) != null) {
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
     */

    /*
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
     */

    private String loadStateFromFile(EmulatorInterface emulator, InputStream fileIn) {
        ObjectInputStream in = null;
        EmulatorInterface loadedEmulator;
        try {
            in = new ObjectInputStream(fileIn);
            loadedEmulator = (com.cax.pmk.emulator.Emulator) in.readObject();
            in.close();
            fileIn.close();

        } catch(Exception i) {
            return i.getMessage();
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

        return null;
    }

    /*
    private void loadProgramDescription(String pFileName) {
        mProgramDescription = null;
        try {
            int pointIndex = pFileName.lastIndexOf('.');
            if (pointIndex != -1) {
                pointIndex++;
                StringBuilder descrFileName = new StringBuilder(pFileName);
                descrFileName.replace(pointIndex, pFileName.length(), "html");
                if (pFileName.startsWith(PROVIDER_PREFIX)) {
                    descrFileName.replace(0, PROVIDER_PREFIX_LEN, "");
                }
                mProgramDescription = descrFileName.toString();
            }
        } catch (Exception ignored) {}
    }
     */

    private void importTxtProgram(final EmulatorInterface emulator, InputStream fileIn)
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
        fileIn.close();

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

    public void deletePersistentFile() {
        File file = getFileStreamPath(PERSISTENCE_STATE_FILENAME);
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

    /*
    private void saveProgramsDir(File pFile) {
	    String dir = pFile.getParent();
	    if (dir != null) {
            SharedPreferences.Editor sharedPrefEditor = PreferenceManager.getDefaultSharedPreferences(mainActivity).edit();
            sharedPrefEditor.putString(PreferencesActivity.PROGRAMS_DIR_KEY, dir);
            sharedPrefEditor.apply();
        }
    }
     */

    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }
}
