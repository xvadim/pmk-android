package com.cax.pmk.emulator;

import java.io.*;
import com.cax.pmk.*;

public class Emulator extends Thread implements EmulatorInterface
{
	enum RunningState {
		RUNNING, STOPPED, STOPPING_NORMAL, STOPPING_FORCED
	};
	public Emulator() {}

	public void initTransient(MainActivity mainActivity) {
    	this.mainActivity = mainActivity;

    	indicator     = new int[12];
    	indicator_old = new int[12];
    	ind_comma     = new boolean[12];
    	ind_comma_old = new boolean[12];
        displayString = new StringBuffer(24);
    	
        IK1302.ik130x = 2;
        IK1303.ik130x = 3;
        IK1306.ik130x = 6;
        
		IK1302.ucmd_rom = UCommands.ik1302_urom;
		IK1303.ucmd_rom = UCommands.ik1303_urom;
		IK1306.ucmd_rom = UCommands.ik1306_urom;
	
		IK1302.synchro_rom = Synchro.ik1302_srom;
		IK1303.synchro_rom = Synchro.ik1303_srom;
		IK1306.synchro_rom = Synchro.ik1306_srom;
	
		IK1302.cmd_rom = MCommands.ik1302_mrom;
		IK1303.cmd_rom = MCommands.ik1303_mrom;
		IK1306.cmd_rom = MCommands.ik1306_mrom;
	}

	public void run() {
		runningState = RunningState.RUNNING;
		while(runningState == RunningState.RUNNING) step();
		
		if (runningState != RunningState.STOPPING_FORCED) {
			while(! (IR2_1.microtick == 84 && syncCounter == 0))
				tick42();
	}

		runningState = RunningState.STOPPED;
        mainActivity = null;
	}

	public void stopEmulator(boolean force) {
		if (force) {
			runningState = RunningState.STOPPING_FORCED;
		} else {
			runningState = RunningState.STOPPING_NORMAL;
		}

		while (runningState == RunningState.STOPPING_NORMAL || runningState == RunningState.STOPPING_FORCED)
        	try { sleep(10); } catch (Exception e) {}
	}

	public void setAngleMode(int mode) {
		mode = (mode == 0) ? 0 : 3 - mode; // convert to Rad=0, Grad=2, Deg=1 as emulator engine expects
		angle_mode = mode + 10;
	}

	public int getAngleMode() {
		int mode = angle_mode - 10;
		return (mode == 0) ? 0 : 3 - mode;  // convert back to Rad=0, Grad=1, Deg=2 for UI convenience
	}

	public void setSpeedMode(int mode) {
		speed_mode = mode;
	}

	public int getSpeedMode() {
		return speed_mode;
	}

	public void setMkModel(int model) {
		this.mk_model = model;
	}

	public int getMkModel() {
		return mk_model;
	}

	public void setSaveStateName(String name) {
		saveStateName = name;
	}

	public String getSaveStateName() {
		return saveStateName;
	}

	public String indicatorString() {
		StringBuffer indBuf = new StringBuffer(24);
		indBuf.setLength(0);
		for (int ix = 0; ix < 12; ix++) {
			indBuf.append(show_symbols[indicator[ix]]);
			if (ind_comma[ix]) {
				indBuf.append(".");
			}
		}
		return indBuf.toString();
	}

	public void keypad(int keycode) {
		IK1302.keyb_x = (keycode % 10) + 2;
		keycode /= 10;
		IK1302.keyb_y = keycode == 2 ? 8 : (keycode == 3 ? 9 : 1);

    	/*
    	11,9	7,9		9,9		4,9		2,9		<-   39 37 35 32 30
    	10,9	8,9		6,9		3,9		5,9     <-   38 36 34 31 33
    	9,1		10,1	11,1	3,8		5,8     <-   17 18 19 21 23
    	6,1		7,1		8,1		2,8		4,8     <-   14 15 16 20 22
    	3,1		4,1		5,1		6,8		11,8    <-   11 12 13 24 29
    	2,1		7,8		8,8		9,8		10,8    <-   10 25 26 27 28
    	*/
	}
	
	void show_indicator() {
		displayString.setLength(0);
		for (int ix = 0; ix < 12; ix++) {
			displayString.append(show_symbols[indicator[ix]]);
			displayString.append(ind_comma[ix] ? "." : "/");
		}
		mainActivity.displayIndicator(displayString.toString());
	}

	void tick() {
		IK1302.in = IR2_2.out;		IK1302.tick();
		IK1303.in = IK1302.out;		IK1303.tick();
		
		if (mk_model == 1) 
		{ // MK-54
			IR2_1.in  = IK1303.out;		IR2_1.tick();
		}
		else 
		{ // MK-61
			IK1306.in = IK1303.out;		IK1306.tick();
			IR2_1.in  = IK1306.out;		IR2_1.tick();
		}
		
		IR2_2.in  = IR2_1.out;		IR2_2.tick();
		IK1302.M[((IK1302.microtick >>> 2) + 41) % 42] = IR2_2.out;
	}
	
	boolean tick42() {
		for (int j = 0; j < 42; j++) { 
			tick();
		}
		
		if (IR2_1.microtick == 84) {
			syncCounter = (syncCounter + 1) % (mk_model == 0 ? 5 : 7); 
			if (IK1302.redraw_indic && syncCounter == (mk_model == 0 ? 4 : 6)) {
				return true;
			}
		}
		return false;
	}
	
	void step() {
		int i,idx;
		boolean renew = false;
		IK1303.keyb_y = 1;
		IK1303.keyb_x = angle_mode;
		for (int ix = 0; ix < 560; ix++) {
			if (runningState == RunningState.STOPPING_FORCED) break;
			if (speed_mode>0) try { sleep(1); } catch (InterruptedException e) {}
			tick42();
			
			if (IK1302.redraw_indic) {
				for (i = 0; i <= 8; i++) indicator[i] 	  = IK1302.R[(8 - i) * 3];
				for (i = 0; i <= 2; i++) indicator[i + 9] = IK1302.R[(11 - i) * 3];
				for (i = 0; i <= 8; i++) ind_comma[i]     = IK1302.ind_comma[9 - i];
				for (i = 0; i <= 2; i++) ind_comma[i + 9] = IK1302.ind_comma[12 - i];
				IK1302.redraw_indic = false;
			}
			else
			{
				for (i = 0; i < 12; i++) {
					indicator[i] = 15; ind_comma[i] = false;
					IK1302.redraw_indic = false;
				}
			}

			renew = false;
			for (idx = 0; idx < 12; idx++) {
				if (indicator_old[idx] != indicator[idx]) renew = true;
				indicator_old[idx] = indicator[idx];
				if (ind_comma_old[idx] != ind_comma[idx]) renew = true;
				ind_comma_old[idx] = ind_comma[idx];
			}
			if (renew) show_indicator();
		}
	}

	MCU IK1302 = new MCU(); 
	MCU IK1303 = new MCU();
	MCU IK1306 = new MCU();
	
	Memory IR2_1 = new Memory();
	Memory IR2_2 = new Memory();
	
	private String saveStateName="";

	private int angle_mode = 10; // R=10, GRD=11, G=12
	private int speed_mode = 0;  // 0=fast, 1=real speed
	private int mk_model   = 0;  // 0=MK-61, 1=MK-54
	
	private transient int syncCounter = 0;
	private transient int[] indicator;
	private transient int[] indicator_old;
	private transient boolean[] ind_comma;
	private transient boolean[] ind_comma_old;
    private transient StringBuffer displayString = new StringBuffer(24);
   	private transient RunningState runningState;
   	private transient MainActivity mainActivity;

	private static final char[] show_symbols = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', 'L', 'C', 'D', 'E', ' '};
	private static final int externalizeVersion = 1;
	private static final long serialVersionUID = 1;

	public static boolean readStateNamesMode = false;

	@Override
	public void readExternal(ObjectInput objIn) throws IOException, ClassNotFoundException {
   		int extVersion = objIn.readInt();
   		saveStateName = objIn.readUTF();
   		if (readStateNamesMode)
   			return;
   		
   		if (extVersion == Emulator.externalizeVersion) { // save version check
   	   		IK1302 = (MCU) objIn.readObject();
   	   		IK1303 = (MCU) objIn.readObject();
   	   		IK1306 = (MCU) objIn.readObject();
   	   		IR2_1 = (Memory) objIn.readObject();
   	   		IR2_2 = (Memory) objIn.readObject();
   	   		angle_mode = objIn.readInt();
   	   		int speed_mode_AND_mk_model = objIn.readInt();
   	   		speed_mode = speed_mode_AND_mk_model & 255;
   	   		mk_model = speed_mode_AND_mk_model >> 8;
   		} else {
   			throw new ClassNotFoundException();
   		}
   	}

	@Override
	public void writeExternal(ObjectOutput objOut) throws IOException {
   		objOut.writeInt(externalizeVersion);
   		objOut.writeUTF(saveStateName);
   		objOut.writeObject(IK1302);
   		objOut.writeObject(IK1303);
   		objOut.writeObject(IK1306);
   		objOut.writeObject(IR2_1);
   		objOut.writeObject(IR2_2);
   		objOut.writeInt(angle_mode);
   		objOut.writeInt(speed_mode | (mk_model << 8));
	}
}
