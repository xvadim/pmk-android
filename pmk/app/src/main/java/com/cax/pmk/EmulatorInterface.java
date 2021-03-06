package com.cax.pmk;

import java.util.ArrayList;

public interface EmulatorInterface extends Runnable, java.io.Externalizable
{
	void setAngleMode(int mode);
    int  getAngleMode();
	void setSpeedMode(int mode);
    int  getSpeedMode();
	void setMkModel(int mkModel);
    int  getMkModel();
	void setSaveStateName(String name);
//	void saveCmd(int address, int cmdCode);
	void storeCmd(int address, int cmdCode);
	void setImportPrgSize(int prgSize);
    String getSaveStateName();
    String indicatorString();
    ArrayList<String> regsDumpBuffer();
    void keypad(int keycode);
    void initTransient(MainActivity mainActivity);
    void stopEmulator(boolean forced);
    void run();
	void start();
}
