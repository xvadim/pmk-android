package com.cax.pmk.emulator;

import java.io.*;

public class MCU implements Externalizable 
{
	public MCU() {
		R  = new int[ARRAY_SIZE];
		M  = new int[ARRAY_SIZE];
		ST = new int[ARRAY_SIZE];

		ind_comma = new boolean[14];

		for (int ix = 0; ix < ARRAY_SIZE; ix++) {
			R[ix] = 0; M[ix] = 0; ST[ix] = 0;
		}

		S = 0; S1 = 0;
		L = 0; T = 0; P = 0;
		microtick = 0; mcmd = 0;
		keyb_x = 0; keyb_y = 0; comma = 0;
		in = 0; out = 0;
		AMK = 0; ASP = 0; AK = 0; MOD = 0;
		redraw_indic = false;

		for(int ix = 0; ix < IND_COMMA_SIZE; ix++) {
			ind_comma[ix] = false;
		}
	}

	public void tick() {
		int tick_0123 = microtick & 3;
		int chetv_1248 = 1 << tick_0123;
		int signal_I = microtick >>> 2;
		int signal_D = microtick / 12 | 0;
		//int signal_E = (microtick >>> 2) % 3;
		boolean keyb_processed = false;
		
		if (microtick == 0) {
			AK = R[36] + 16 * R[39];
			if ((cmd_rom[AK] & 0xfc0000) == 0) T = 0;
		}
		
		if (chetv_1248 == 1) {
			int k = microtick / 36 | 0;
			if (k < 3) ASP = 0xff & cmd_rom[AK];
			else if (k == 3) ASP = 0xff & cmd_rom[AK] >>> 8;
			else if (k == 4) {
				ASP = 0xff & cmd_rom[AK] >>> 16;
				if (ASP > 0x1f) {
					if (microtick == 144) {
						R[37] = ASP & 0xf;
						R[40] = ASP >>> 4;
					}
					ASP = 0x5f;
				}
			}
			MOD = 0xff & cmd_rom[AK] >>> 24;
			AMK = synchro_rom[ASP * 9 + J[microtick >>> 2]];
			AMK = AMK & 0x3f;
			if (AMK > 59) {
				AMK = (AMK - 60) * 2;
				if (L == 0) AMK++;
				AMK += 60;
			}
			mcmd = ucmd_rom[AMK];
		}
		
		int alpha = 0, beta = 0, gamma = 0;
		
		switch (mcmd >>> 24 & 3) {
			case 2:
			case 3:
				if ((microtick / 12 | 0) != keyb_x - 1)
					if (keyb_y > 0) {
						if (chetv_1248 == 1) S1 |= keyb_y;
						keyb_processed = true;
					}
				break;
		}
		
		if ((mcmd & 1) > 0) alpha |= R[signal_I];
		if ((mcmd & 2) > 0) alpha |= M[signal_I];
		if ((mcmd & 4) > 0) alpha |= ST[signal_I];
		if ((mcmd & 8) > 0) alpha |= ~R[signal_I] & 0xf;
		if ((mcmd & 16) > 0) if (L == 0) alpha |= 0xa;
		if ((mcmd & 32) > 0) alpha |= S;
		if ((mcmd & 64) > 0) alpha |= 4;
		if ((mcmd >>> 7 & 16) > 0) beta |= 1;
		if ((mcmd >>> 7 & 8) > 0) beta |= 6;
		if ((mcmd >>> 7 & 4) > 0) beta |= S1;
		if ((mcmd >>> 7 & 2) > 0) beta |= ~S & 0xf;
		if ((mcmd >>> 7 & 1) > 0) beta |= S;
		if ((cmd_rom[AK] & 0xfc0000) > 0) {
			if (keyb_y == 0) T = 0;
		}
		else 
		{
			redraw_indic = true;
			if ((microtick / 12 | 0) == keyb_x - 1)
				if (keyb_y > 0) {
					S1 = keyb_y;
					T = 1;
					keyb_processed = true;
				}
			if (tick_0123 == 0)
				if (signal_D >= 0 && signal_D < 12)
					if (L > 0) comma = signal_D;
			ind_comma[signal_D] = L > 0;
		}
		if ((mcmd >>> 12 & 4) > 0) gamma = ~T & 1;
		if ((mcmd >>> 12 & 2) > 0) gamma |= ~L & 1;
		if ((mcmd >>> 12 & 1) > 0) gamma |= L & 1;
		
		int sum = alpha + beta + gamma;
		int sigma = sum & 0xf;
		P = sum >>> 4;
		
		if (MOD == 0 || (microtick >>> 2) >= 36) {
			switch (mcmd >>> 15 & 7) {
				case 1: R[signal_I] = R[(signal_I + 3) % ARRAY_SIZE]; break;
				case 2: R[signal_I] = sigma; break;
				case 3: R[signal_I] = S; break;
				case 4: R[signal_I] = R[signal_I] | S | sigma; break;
				case 5: R[signal_I] = S | sigma; break;
				case 6: R[signal_I] = R[signal_I] | S; break;
				case 7: R[signal_I] = R[signal_I] | sigma; break;
			}
			if ((mcmd >>> 18 & 1) > 0) R[(signal_I + 41) % ARRAY_SIZE] = sigma;
			if ((mcmd >>> 19 & 1) > 0) R[(signal_I + 40) % ARRAY_SIZE] = sigma;
		}
		if ((mcmd >>> 21 & 1) > 0) L = 1 & P;
		if ((mcmd >>> 20 & 1) > 0) M[signal_I] = S;
		
		switch (mcmd >>> 22 & 3) {
			case 1: S = S1; break;
			case 2: S = sigma; break;
			case 3: S = S1 | sigma; break;
		}
		
		switch (mcmd >>> 24 & 3) {
			case 1: S1 = sigma; break;
			//case 2: S1 = S1; break;
			case 3: S1 = S1 | sigma; break;
		}

        int x, y, z;
		switch (mcmd >>> 26 & 3) {
			case 1:	ST[(signal_I + 2) % ARRAY_SIZE] = ST[(signal_I + 1) % ARRAY_SIZE];
				ST[(signal_I + 1) % ARRAY_SIZE] = ST[signal_I];
				ST[signal_I] = sigma;
				break;
			case 2:	x = ST[signal_I];
				ST[signal_I] = ST[(signal_I + 1) % ARRAY_SIZE];
				ST[(signal_I + 1) % ARRAY_SIZE] = ST[(signal_I + 2) % ARRAY_SIZE];
				ST[(signal_I + 2) % ARRAY_SIZE] = x;
				break;
			case 3:	x = ST[signal_I];
				y = ST[(signal_I + 1) % ARRAY_SIZE];
				z = ST[(signal_I + 2) % ARRAY_SIZE];
				ST[(signal_I + 0) % ARRAY_SIZE] = sigma | y;
				ST[(signal_I + 1) % ARRAY_SIZE] = x | z;
				ST[(signal_I + 2) % ARRAY_SIZE] = y | x;
				break;
		}
		
		out = 0xf & M[signal_I];
		M[signal_I] = in;
		microtick += 4;
		if (microtick > 167) microtick = 0;
		
		if (keyb_processed && ik130x != 3) keyb_x = keyb_y = 0;
	}

	transient int[] ucmd_rom;
	transient int[] cmd_rom;
	transient int[] synchro_rom;
	transient int ik130x;
	
	int[] R;
	int[] M;
	int[] ST;
	int S, S1, L, T, P, microtick, mcmd, keyb_x, keyb_y, comma, in, out, AMK, ASP, AK, MOD;
	boolean[] ind_comma;
	boolean redraw_indic;
	
	private static final int[] J = {
		0, 1, 2, 3, 4, 5,
		3, 4, 5, 3, 4, 5,
		3, 4, 5, 3, 4, 5,
		3, 4, 5, 3, 4, 5,
		6, 7, 8, 0, 1, 2,
		3, 4, 5, 6, 7, 8,
		0, 1, 2, 3, 4, 5
	};

	private static final int ARRAY_SIZE = 42;
	private static final int IND_COMMA_SIZE = 14;

	private static final long serialVersionUID = 1;

	@Override
	public void readExternal(ObjectInput objIn) throws IOException, ClassNotFoundException {
        for (int i = 0; i < ARRAY_SIZE; i++) R[i]  = objIn.readInt();
        for (int i = 0; i < ARRAY_SIZE; i++) M[i]  = objIn.readInt();
        for (int i = 0; i < ARRAY_SIZE; i++) ST[i] = objIn.readInt();
        for (int i = 0; i < IND_COMMA_SIZE; i++) ind_comma[i] = objIn.readBoolean();
        redraw_indic = objIn.readBoolean();
        S 			= objIn.readInt();
        S1 			= objIn.readInt();
        L 			= objIn.readInt();
        T 			= objIn.readInt();
        P 			= objIn.readInt();
        microtick 	= objIn.readInt();
        mcmd 		= objIn.readInt();
        keyb_x 		= objIn.readInt();
        keyb_y 		= objIn.readInt();
        comma 		= objIn.readInt();
        in 			= objIn.readInt();
        out 		= objIn.readInt();
        AMK 		= objIn.readInt();
        ASP 		= objIn.readInt();
        AK 			= objIn.readInt();
        MOD 		= objIn.readInt();
	}

	@Override
	public void writeExternal(ObjectOutput objOut) throws IOException {
        for (int i = 0; i < ARRAY_SIZE; i++) objOut.writeInt(R[i]);
        for (int i = 0; i < ARRAY_SIZE; i++) objOut.writeInt(M[i]);
        for (int i = 0; i < ARRAY_SIZE; i++) objOut.writeInt(ST[i]);
        for (int i = 0; i < IND_COMMA_SIZE; i++) objOut.writeBoolean(ind_comma[i]);
        objOut.writeBoolean(redraw_indic);
		objOut.writeInt(S);
		objOut.writeInt(S1);
		objOut.writeInt(L);
		objOut.writeInt(T);
		objOut.writeInt(P);
		objOut.writeInt(microtick);
		objOut.writeInt(mcmd);
		objOut.writeInt(keyb_x);
		objOut.writeInt(keyb_y);
		objOut.writeInt(comma);
		objOut.writeInt(in);
		objOut.writeInt(out);
		objOut.writeInt(AMK);
		objOut.writeInt(ASP);
		objOut.writeInt(AK);
		objOut.writeInt(MOD);
	}
}
