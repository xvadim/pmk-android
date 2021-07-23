package com.cax.pmk;

import com.cax.pmk.emulator.IndicatorInterface;

import java.util.ArrayList;

public interface EmulatorInterface extends Runnable, java.io.Externalizable
{
    interface ExportTxtListener {
        void exportedCmds(ArrayList<Integer> cmds);
    }

	void setAngleMode(int mode);
    int  getAngleMode();
	void setSpeedMode(int mode);
    int  getSpeedMode();
	void setMkModel(int mkModel);
    int  getMkModel();
	void storeCmd(int address, int cmdCode);
	void setImportPrgSize(int prgSize);
    void requestExportTxt(ExportTxtListener exportTxtListener);
    String indicatorString();
    ArrayList<String> regsDumpBuffer();
    void keypad(int keycode);
    void initTransient(IndicatorInterface mainActivity);
    void stopEmulator(boolean forced);
    void run();
	void start();
}
