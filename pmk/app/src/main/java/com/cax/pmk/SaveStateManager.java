package com.cax.pmk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
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
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.view.Gravity;
import android.widget.Toast;

public class SaveStateManager {
	
	private static final String PERSISTENCE_STATE_FILENAME 	= "persist.pmk";
    private static final String PERSISTENCE_DESCR_FILENAME 	= "persist_descr.html";

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
            try { if (fileOut != null) fileOut.close(); } catch(IOException ignored) {}
        }

        if (mProgramDescription == null) {
            deleteDescriptionFile();
        } else {
            savePersistentDescription();
        }
    }

    private void savePersistentDescription() {
        FileOutputStream fileOut = null;
        try {
            fileOut = mainActivity.openFileOutput(PERSISTENCE_DESCR_FILENAME, Context.MODE_PRIVATE);
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOut));
            bufferedWriter.write(mProgramDescription);
            bufferedWriter.close();
        } catch (IOException ignored) {
        } finally {
            try {
                if (fileOut != null) fileOut.close();
            } catch (IOException ignored) {
            }
        }
    }

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

    	if (! getFileStreamPath(PERSISTENCE_STATE_FILENAME).exists()) {
            return;
        }
    	
		FileInputStream fileIn = null;
        try {
            fileIn = mainActivity.openFileInput(PERSISTENCE_STATE_FILENAME);
            loadStateFromFile(emulator, fileIn);
        } catch(Exception ignored) {
        } finally {
            try { if (fileIn != null) fileIn.close(); } catch(IOException ignored) {}
        }

        loadPersistentDescription();
    }

    private void loadPersistentDescription() {
        if (! getFileStreamPath(PERSISTENCE_DESCR_FILENAME).exists()) {
            return;
        }

        FileInputStream fileIn = null;
        try {
            fileIn = mainActivity.openFileInput(PERSISTENCE_DESCR_FILENAME);
            BufferedReader buf = new BufferedReader(new InputStreamReader(fileIn));
            final StringBuilder stringBuilder = new StringBuilder();

            String line = buf.readLine();
            while(line != null){
                stringBuilder.append(line);
                line = buf.readLine();
            }
            buf.close();
            mProgramDescription = stringBuilder.toString();
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

    void exportStateTxt(final EmulatorInterface emulator, final Uri uri) {
        emulator.requestExportTxt(cmds -> exportCmdsTxt(cmds, uri));
    }

    void exportCmdsTxt(ArrayList<Integer> cmds, Uri uri) {
        CommandParser parser = new CommandParser();
        parser.initExport();

        final StringBuilder stringBuilder = new StringBuilder();

        int cmdNum = 0;
        try{
            for(Integer cmd: cmds) {
                String cmdMnemonic = parser.cmdMnemonic(cmd);
                if (cmdNum < 10) {
                    stringBuilder.append("0");
                }
                if (cmdNum < 100) {
                    stringBuilder.append(cmdNum);
                } else {
                    stringBuilder.append("A0");
                }
                stringBuilder.append(".");
                stringBuilder.append(cmdMnemonic);
                stringBuilder.append('\t');
                cmdNum++;
                if (cmdNum % 10 == 0) {
                    stringBuilder.append('\n');
                }
            }

            final ContentResolver cr = mainActivity.getContentResolver();
            OutputStream os;
            os = cr.openOutputStream(uri);
            if (os == null) {
                showErrorMessage(R.string.import_common_error);
                return;
            }

            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(os));
            bufferedWriter.write(stringBuilder.toString());
            bufferedWriter.close();
            os.close();
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

    private void importTxtProgram(final EmulatorInterface emulator, InputStream fileIn)
            throws IOException, ParserException {

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

    public void deleteAllPersistentFiles() {
        deletePersistentFile();
        deleteDescriptionFile();
    }

    public void deletePersistentFile() {
        File file = getFileStreamPath(PERSISTENCE_STATE_FILENAME);
        if (file.exists())
            //noinspection ResultOfMethodCallIgnored
            file.delete();
    }

    private void deleteDescriptionFile() {
	    mProgramDescription = null;
        File file = getFileStreamPath(PERSISTENCE_DESCR_FILENAME);
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    public void showErrorMessage(int errorTextID) {
        showErrorMessage(mainActivity.getString(errorTextID));
    }

    private void showErrorMessage(String message) {
        Toast toast = Toast.makeText(mainActivity, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();
    }

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
