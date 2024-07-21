package com.cax.pmk.emulator;

import java.io.*;

public class Memory implements Externalizable
{
	public Memory()
	{
		for (int i = 0; i < MEM_SIZE; i++) M[i] = 0;
	}

	public final void tick() {
		if (microtick == MEM_SIZE) microtick = 0;
		out = M[microtick];
		M[(microtick + MEM_SIZE) % MEM_SIZE] = in;
		microtick++;
	}

	public int in=0;
	public int out=0;
	public int microtick=0;

	public int[] M = new int[MEM_SIZE];

	private static final int MEM_SIZE = 252;

	private static final long serialVersionUID = 1;

	@Override
	public void readExternal(ObjectInput objIn) throws IOException,	ClassNotFoundException {
		  in        = objIn.readInt();
          out       = objIn.readInt();
          microtick = objIn.readInt();
          for (int i = 0; i < MEM_SIZE; i++) M[i] = objIn.readInt();
	}

	@Override
	public void writeExternal(ObjectOutput objOut) throws IOException {
		  objOut.writeInt(in);
		  objOut.writeInt(out);
		  objOut.writeInt(microtick);
          for (int i = 0; i < MEM_SIZE; i++) objOut.writeInt(M[i]);
	}

}


