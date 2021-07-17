package com.cax.pmk;

import java.util.Arrays;

class ParserException extends Exception {
    String cmd;

    ParserException(String cmd) {
        this.cmd = cmd;
    }
}

/**
 * Class for parsing commands.
 * Supported format:
 *
 * 00.В↑ - a command with address
 *
 * В↑ - just a command
 */
class CommandParser {

    //codes of commands
    private static final int[] mCodes = {
            0x15,   //F10ˣ
            0x54,   //KНОП
            0x1F,   //1F
            0x2F,   //2F
            0x56,   //K2
            0x16,   //Feˣ
            0x17,   //Flg
            0x18,   //Fln
            0x30,   //KЧ→М
            0x19,   //Fsinᐨ¹
            0x31,   //K|x|
            0x1A,   //Fcosᐨ¹
            0x32,   //Kзн
            0x1B,   //Ftgᐨ¹
            0x33,   //KГ→М
            0x1C,   //Fsin
            0x34,   //K[x]
            0x1D,   //Fcos
            0x35,   //K{x}
            0x1E,   //Ftg
            0x36,   //Kmax
            0x10,   //+
            0x11,   //-
            0x12,   //×
            0x13,   //÷
            0x20,   //Fπ
            0x26,   //KМ→Г
            0x21,   //F√
            0x22,   //Fx²
            0x23,   //F1/x
            0x14,   //<->
            0x0E,   //В↑
            0x3D,   //3D
            0x3C,   //3C
            0x3E,   //x←y
            0x3F,   //3F
            0x24,   //Fxʸ
            0x27,   //K-
            0x28,   //K×
            0x29,   //K÷
            0x2A,   //KМ→Ч
            0x2B,   //"2B"
            0x2C,   //"2C
            0x2D,   //"2D"
            0x2E,   //"2E"
            0x2F,   //"2F"
            0x0F,   //FВх
            0x3B,   //KCЧ
            0x0A,   //".
            0x0B,   // /-/
            0x0C,   //ВП
            0x0D,   //Сx
            0x25,   //Fѻ
            0x37,   //KɅ
            0x38,   //KV
            0x39,   //K(+)
            0x3A,   //Kинв
            0x52,   //В/О
            0x50,   //С/П
            0x59,   //Fx≥0
            0x57,   //Fx≠0
            0x51,   //"БП"
            0x53,   //"ПП"
            0x58,   //FL2
            0x5A,   //FL3
            0x5C,   //Fx<0
            0x5E,   //Fx=0
            0x5D,   //FL0
            0x5B,   //FL1
            0x5F,   //"5F"
            0x40,   //xП
            0x4F,   //"4F"
            0x60,   //Пx
            0x6F,   //"6F"
            0x70,   //Kx≠0
            0x7F,   //"7F"
            0x80,   //"KБП"
            0x8F,   //"8F
            0x90,   //Kx≥0
            0x9F,   //"9F"
            0xA0,   //"KПП"
            0xAF,   //"AF"
            0xB0,   //KxП
            0xBF,   //BF
            0xC0,   //Kx<0
            0xCF,   //"CF"
            0xD0,   //KПx
            0xDF,   //"DF"
            0xE0,   //Kx=0
            0xEF,   //"EF"
            0xF0,   //"F"
            0xFF,   //"FF"
    };

    //mnemonics of codes
    private static final String[][] mMnemonics = {
            {"F10ˣ", "10ˣ", "F10^X", "10X", "F10X", "10**X"},
            {"KНОП", "KНOП", "НOП", "K0"},  // O кроме первой на анлийском
            {"1F"},
            {"2F"},
            {"K2"},
            {"Feˣ", "FEˣ", "Eˣ", "FE^X", "E^X", "EX", "FEX", "FE**X", "E**X"},
            {"Flg", "FLG", "LG"},
            {"Fln", "FLN", "LN"},
            {"KЧ→М", "KЧ→M","KЧM", "ЧM", "K3"}, // М кроме первой на анлийском
            {"Fsinᐨ¹", "FSINᐨ¹", "SINᐨ¹", "FSIN-1", "FARCSIN", "ARCSIN", "SIN-1"},
            {"K|x|", "K|X|", "|X|", "K4"},
            {"Fcosᐨ¹", "FCOSᐨ¹", "COSᐨ¹", "FCOS-1", "FARCCOS", "ARCCOS", "COS-1"},
            {"Kзн", "KЗН", "ЗН", "K5"},
            {"Ftgᐨ¹", "FTGᐨ¹", "TGᐨ¹", "FTG-1", "FARCTG", "ARCTG", "TG-1"},
            {"KГ→М", "KГ→M", "Г→M", "KГM", "ГM", "K6"}, // М кроме первой на анлийском
            {"Fsin", "FSIN", "SIN"},
            {"K[x]", "K[X]", "[X]", "K7"},
            {"Fcos", "FCOS", "COS"},
            {"K{x}", "K{X}", "{X}", "(X)", "K(X)", "K8"},
            {"Ftg", "FTG", "TG"},
            {"Kmax", "KMAX", "MAX", "K9"},
            {"+"},
            {"-", "–", "–"}, // "Hyphen", "Dash", "En Dash"
            {"×", "⋅", "X", "*"},
            {"÷", "/", ":"},
            {"Fπ", "FΠ", "ПИ", "Π", "FПИ"},
            {"KМ→Г", "KM→Г", "M→Г", "KMГ", "MГ", "K+"}, // М кроме первой на анлийском
            {"F√", "√", "KBKOР", "KOРEНЬ", "KBKOРEНЬ", "FKBKOР", "FKOРEНЬ", "FKBKOРEНЬ"},   // K;O;E английские
            {"Fx²", "FX²", "X^2", "X2", "X²", "FX^2", "FX2"},
            {"F1/x", "F1/X", "1/X"},
            {"<->", "↔", "XY", "X↔Y", "⟷"},
            {"В↑", "B↑","^","B^","↑"}, // "B" кроме первой на анлийском
            {"3D"},
            {"3C"},
            {"x←y", "X←Y"},
            {"3F"},
            {"Fxʸ", "FXʸ", "Xʸ", "FX^Y", "X^Y", "XY", "FXY"},
            {"K-", "K–", "K–"}, // "Hyphen", "Dash", "En Dash"
            {"K×", "KX", "K*"},
            {"K÷", "K/", "K:"},
            {"KМ→Ч", "KM→Ч", "M→Ч", "КM→Ч"},    // М кроме первой на анлийском
            {"2B"},
            {"2C"},
            {"2D"},
            {"2E"},
            {"2F"},
            {"FВх", "FBX", "ВX"},   // "B" кроме первой на анлийском
            {"KCЧ", "KCЧ", "CЧ", "KE"},
            {".", ",", "•"},
            {"/-/", "+/-", "/–/", "/–/"},   // "Hyphen", "Dash", "En Dash"
            {"ВП", "BП"},   // Первая "В" русская
            {"Сx", "CX"},
            {"Fѻ", "FѺ", "Ѻ", "F↻", "->", "↻", "→", "F->", "F→"},
            {"KɅ", "КɅ", "K^", "K⋀", "K∧", "∧", "K/\\", "/\\", "⋀", "K.", "KA", "KΛ"},
            {"KV", "Kv", "K⋁", "K∨", "∨", "K\\/", "\\/", "⋁", "K/-/", "KB"},
            {"K(+)", "K⊕", "(+)", "⊕", "KBП", "KC"},
            {"Kинв", "KИНB", "ИНB", "KCX", "KD"},
            {"В/О", "B/0", "B/O"},  // "B" кроме первой на анлийском, во второй - цифра ноль
            {"С/П", "C/П"}, // Первая "С" русская
            {"Fx≥0", "FX≥0", "X>=0", "X≥0", "X⩾0",
                    "Fx≥O", "FX≥O", "X>=O", "X≥O", "X⩾O"},  // Тут вместо нуля буква O
            {"Fx≠0", "FX≠0", "X#0", "X!=0", "X<>0", "FX#0", "FX!=0", "FX<>0",
                    "FX≠O", "X#O", "X!=O", "X<>O", "FX#O", "FX!=O", "FX<>O"},   //Тут вместо нуля буква O
            {"БП"},
            {"ПП"},
            {"FL2", "L2"},
            {"FL3", "L3"},
            {"Fx<0", "FX<0", "X<0",
                    "FX<O", "X<O"}, //  Тут вместо нуля буква O
            {"Fx=0", "FX=0", "X=0",
                    "FX=O", "X=O"}, // Тут вместо нуля буква O
            {"FL0", "L0",
                    "FLO", "LO"},   // Тут вместо нуля буква O
            {"FL1", "L1"},
            {"5F"},
            {"xП", "XП", "X→П", "XП", "П"},
            {"4F"},
            {"Пx", "ПX", "П→X", "ИП"},
            {"6F"},
            {"Kx≠0", "KX≠0", "KX#0", "KX!=0", "KX<>0",
                    "KX≠O", "KX#O", "KX!=O", "KX<>O"},  // Тут вместо нуля буква O
            {"7F"},
            {"KБП"},
            {"8F"},
            {"Kx≥0", "KX≥0", "KX⩾0", "KX>=0",
                    "Kx≥O", "KX≥O", "KX⩾O", "KX>=O"},   // Тут вместо нуля буква O
            {"9F"},
            {"KПП"},
            {"AF"},
            {"KxП", "KXП", "KX→П", "KП", "КП"},
            {"BF"},
            {"Kx<0", "KX<0",
                    "KX<O"},    // Тут вместо нуля буква O
            {"CF"},
            {"KПx", "KПX", "KП→x", "KП→X", "KИП"},
            {"DF"},
            {"Kx=0", "KX=0",
                    "KX=O"},    // Тут вместо нуля буква O
            {"EF"},
            {"F"},
            {"FF"},
    };

    //types:
    // 0 - ordinary command
    // 1 - with a register
    // 2 - with an address
    private static final int[] mTypes = {
            0, //F10ˣ
            0, //KНОП
            0, //1F
            0, //2F
            0, //"K2"
            0, //Feˣ
            0, //Flg
            0, //Fln
            0, //KЧ→М
            0, //Fsinᐨ¹
            0, //K|x|
            0, //Fcosᐨ¹
            0, //Kзн
            0, //Ftgᐨ¹
            0, //KГ→М
            0, //Fsin
            0, //K[x]
            0, //Fcos
            0, //K{x}
            0, //Ftg
            0, //Kmax
            0, //+
            0, //-
            0, //×
            0, //÷
            0, //Fπ
            0, //KМ→Г
            0, //F√
            0, //Fx²
            0, //F1/x
            0, //<->
            0, //В↑
            0, //3D
            0, //3C
            0, //x←y
            0, //3F
            0, //Fxʸ
            0, //K-
            0, //K×
            0, //K÷
            0, //KМ→Ч
            0, //"2B"
            0, //"2C
            0, //"2D"
            0, //"2E"
            0, //"2F"
            0, //FВх
            0, //KCЧ
            0, //".
            0, // /-/
            0, //ВП
            0, //Сx
            0, //Fѻ
            0, //KɅ
            0, //KV
            0, //K(+)
            0, //Kинв
            0, //В/О
            0, //С/П
            2, //Fx≥0
            2, //Fx≠0
            2, //"БП"
            2, //"ПП"
            2, //FL2
            2, //FL3
            2, //Fx<0
            2, //Fx=0
            2, //FL0
            2, //FL1
            0, //"5F"
            1, //xП
            0, //"4F"
            1, //Пx
            0, //"6F"
            1, //Kx≠0
            0, //"7F"
            1, //"KБП"
            0, //"8F
            1, //Kx≥0
            0, //"9F"
            1, //"KПП"
            0, //"AF"
            1, //KxП
            0, //BF
            1, //Kx<0
            0, //"CF"
            1, //KПx
            0, //"DF"
            1, //Kx=0
            0, //"EF"
            1, //"F"
            0, //"FF"
    };



    // parsing vars
    private int mAddress;
    private int mCode;

    // coding vars
    private boolean mIsAddress; // the next command is address
    /**
     * Parses the given command. Sets command vars
     * @param cmd a command for parsing
     */
    void parseCommand(int address, String cmd) throws ParserException {
        if (cmd.length() == 0) {
            return;
        }
        mCode = 0;
        try {
            cmd = parseCmdAddress(address, cmd);
            parseCmd(cmd);
        } catch (NumberFormatException ex) {
            throw new ParserException(cmd.contains(".") ? cmd :
                    String.format("%d.%s", mAddress, cmd));
        }
    }

    /**
     * Returns the command address if it given
     * @return the command address or -1 if the current command without address
     */
    int cmdAddress() {
        return mAddress;
    }

    /**
     * Returns the code of the current command
     */
    int cmdCode() {
        return mCode;
    }

    void initExport() {
        mIsAddress = false;
    }

    String cmdMnemonic(int cmd) {
        String res = null;

        if (mIsAddress) {
            mIsAddress = false;
            res = cmd < 10 ? "0" : "";
            return res + Integer.toString(cmd, 16).toUpperCase();
        }

        int i;
        for(i = 0; i < mCodes.length; i++) {
            if (mTypes[i] == 1) {   // a command with a reg
                int regNum = cmd % 0x10;
                if (regNum < 0xF && (cmd / 0x10 == mCodes[i] / 0x10)) {
                    res = mMnemonics[i][0] + Integer.toString(regNum, 16).toUpperCase();
                    break;
                }
            } else {
                if (mCodes[i] == cmd) {
                    res = mMnemonics[i][0];
                    break;
                }
            }
        }
        if (i == mCodes.length) {
            res = Integer.toString(cmd, 16).toUpperCase();
        } else {
            mIsAddress = mTypes[i] == 2;
        }
        return res;
    }

    /**
     * Parses a command address:
     * 99.Cx, A4.В/О
     * @param address the default address
     * @param cmd a command
     * @return a new command string without address
     */
    private String parseCmdAddress(int address, String cmd) {
        mAddress = address;
        if (cmd.length() > 2 && cmd.charAt(2) == '.') {
            cmd = cmd.substring(3);
            //don't parse addresses at the moment
            /*
            char firstChar = cmd.charAt(0);
            if (firstChar == 'A' || firstChar == '-' || firstChar == '.') {
                //100+ address in forms 'A0', '-0', '.0'
                try {
                  mAddress = 100 + Integer.parseInt(cmd.substring(1, 2));
                  cmd = cmd.substring(3);
                } catch (NumberFormatException ignored) { }
            } else {
                try {
                    mAddress = Integer.parseInt(cmd.substring(0, 2));
                    cmd = cmd.substring(3);
                } catch (NumberFormatException ignored) { }
            }
             */
        }
        return cmd;
    }

    private int regNum(String regStr) {
        return Integer.parseInt(regStr, 16);
    }

    private void parseCmd(String cmd){
        //a command without last char, it can be a register
        String cmd2 = cmd.substring(0, cmd.length() - 1);
        for(int i = 0; i < mMnemonics.length; i++) {
            boolean isCmdWithReg = mTypes[i] == 1;
            if (Arrays.asList(mMnemonics[i]).contains(isCmdWithReg ? cmd2 : cmd)) {
                mCode = mCodes[i];
                if (isCmdWithReg) {
                    mCode += regNum(cmd.substring(cmd.length() - 1));
                }
                return;
            }
        }

        //the last attempt: it's just a code 0-9 or an address like 45 or A2
        mCode = Integer.parseInt(
                cmd.replace('-', 'A')
                        .replace('.', 'A'),
                16);
    }
}
