package com.cax.pmk;

public interface EmulatorInterface extends Runnable, java.io.Externalizable 
{
	public void setAngleMode(int mode);
    public int  getAngleMode();
	public void setSpeedMode(int mode);
    public int  getSpeedMode();
	public void setMkModel(int mkModel);
    public int  getMkModel();
	public void setSaveStateName(String name);
    public String getSaveStateName();
    public void keypad(int keycode);
    public void initTransient(MainActivity mainActivity);
    public void stopEmulator(boolean forced);
    public void run();
	public void start();
}
