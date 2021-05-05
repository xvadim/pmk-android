package com.cax.pmk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.RelativeLayout.LayoutParams;

import com.cax.pmk.widget.AutoScaleTextView;

public class SkinHelper {
    static MainActivity mainActivity;
    private static int yellowLabelLeftPadding = 0;
    private static float buttonTextSize = 0;
    private static float labelTextSize = 0;

    private static final String FONT_INDICATOR_DIGITS	= "fonts/digital-7-mod.ttf";
    private static final String FONT_MISSING_SYMBOLS	= "fonts/missing-symbols.ttf";

    private static final int [] blackButtons = {
        R.id.buttonStepBack,	R.id.buttonStepForward, R.id.buttonReturn,	R.id.buttonStopStart,
        R.id.buttonXToRegister,	R.id.buttonRegisterToX, R.id.buttonGoto,	R.id.buttonSubroutine
    };

    private static final int[] blueLabels = {
        R.id.labelFloor,	R.id.labelFrac,	R.id.labelMax,
        R.id.labelAbs,		R.id.labelSign,	R.id.labelFromHM,	R.id.labelToHM,
                                            R.id.labelFromHMS,	R.id.labelToHMS,R.id.labelRandom,
        R.id.labelNOP,		R.id.labelAnd,	R.id.labelOr,		R.id.labelXor,	R.id.labelInv
    };

    // blue labels text storage for switching between mk61 and mk54 
    private static final CharSequence[] blueLabelsText = new CharSequence[blueLabels.length];
    
    // yellow labels above buttons that have only this label
    private static final int[] singleYellowLabels = {
        R.id.labelLessThanZero,	R.id.labelIsZero,	R.id.labelGreaterOrEqualsZero,	R.id.labelIsNotZero,
        R.id.labelL0,			R.id.labelL1,		R.id.labelL2,					R.id.labelL3,
                                                                                    R.id.labelXpower2,
                                                    R.id.labelSquare,				R.id.label1divX,
        R.id.labelEpowerX,		R.id.labelLg
    };

    // yellow labels above buttons that have both yellow and blue labels
    private static final int[] pairedYellowLabels = {
        R.id.labelSin,		R.id.labelCos,		R.id.labelTg,
        R.id.labelArcSin,	R.id.labelArcCos,	R.id.labelArcTg,	R.id.labelPi,
                                                R.id.labelLn,		R.id.labelXpowerY,	R.id.labelBx,
        R.id.label10powerX,	R.id.labelDot,		R.id.labelAVT,		R.id.labelPRG,		R.id.labelCF
    };

    static void init() {
        // remember blue labels text for switching between mk61/54
        for (int i=0; i < blueLabels.length; i++) {
            blueLabelsText[i] = ((TextView) mainActivity.findViewById(blueLabels[i])).getText();
        }

        // remember labels padding for later use
        yellowLabelLeftPadding = mainActivity.findViewById(R.id.label10powerX).getPaddingLeft();
        
        // remember button and label text size for later use
        buttonTextSize = ((Button)  mainActivity.findViewById(R.id.buttonF    )).getTextSize();
        labelTextSize  = ((TextView)mainActivity.findViewById(R.id.labelSquare)).getTextSize();

        // style indicator
        TextView calculatorIndicator = mainActivity.findViewById(R.id.textView_Indicator);
        Typeface tf = Typeface.createFromAsset(mainActivity.getAssets(), FONT_INDICATOR_DIGITS);
        calculatorIndicator.setTypeface(tf);
        calculatorIndicator = mainActivity.findViewById(R.id.textView_IndicatorY);
        calculatorIndicator.setTypeface(tf);

        TextView indicator = mainActivity.findViewById(R.id.indicatorF);
        indicator.setTypeface(tf);
        indicator = mainActivity.findViewById(R.id.indicatorTurbo);
        indicator.setTypeface(tf);
        indicator = mainActivity.findViewById(R.id.indicatorK);
        indicator.setTypeface(tf);

        // use manually created symbols for some labels
        tf = Typeface.createFromAsset(mainActivity.getAssets(), FONT_MISSING_SYMBOLS);
        for (int viewId : new int[] { R.id.labelSquare, R.id.labelEpowerX, R.id.label10powerX, R.id.labelXpowerY, R.id.labelDot }) {
            ((TextView)mainActivity.findViewById(viewId)).setTypeface(tf);
        }
        
        // use manually created symbols for some buttons
        for (int viewId : new int[] { R.id.buttonUpStack, 		R.id.buttonStepBack, 	R.id.buttonStepForward, 
                                      R.id.buttonRegisterToX, 	R.id.buttonXToRegister, R.id.buttonExchangeXY}) {
            if (mainActivity.findViewById(viewId) != null)
                ((Button)mainActivity.findViewById(viewId)).setTypeface(tf, Typeface.NORMAL);
        }

    }

    @SuppressLint("SetTextI18n")
    static void setMkModelName(int mkModel) {
        View v = mainActivity.findViewById(R.id.buttonReturn);
        int x = v.getWidth() + v.getPaddingLeft();
        AutoScaleTextView textView = mainActivity.findViewById(R.id.TextViewTableCellCalculatorName);
        if (textView != null) {
            textView.setMaxWidth(x * 3);
            textView.setText(mainActivity.getString(R.string.electronica) + "  MK" + (mkModel == 1 ? "-54" : " 61"));
        }
    }

    @SuppressLint("SetTextI18n")
    static void setMkModelSkin(int mkModel) {

        if (mkModel == 1) { // 1 for MK-54

            for (int i=0; i < blueLabels.length; i++) {

                // remove blue labels
                TextView blueLabel = mainActivity.findViewById(blueLabels[i]);
                blueLabel.setText("");

                // center yellow labels
                View modView = mainActivity.findViewById(pairedYellowLabels[i]);
                modView.setPadding(0, 0, 0, 0);

                LayoutParams params = (LayoutParams) modView.getLayoutParams();
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
                params.addRule(RelativeLayout.CENTER_IN_PARENT);

            }
            // remove "e", add white "НОП", replace "CX" with "Cx"
            ((TextView) mainActivity.findViewById(R.id.labelE)).setText("");
            ((TextView) mainActivity.findViewById(R.id.labelNop54)).setText("НОП");

            Button clearButton = mainActivity.findViewById(R.id.buttonClear);
            clearButton.setText("Cx");
            Typeface tf = clearButton.getTypeface();

            setMemoryButtonsSkin(true);

            Button b = mainActivity.findViewById(R.id.buttonExchangeXY);
            b.setText("XY");
            b.setTypeface(tf);
        } else { // 0 for MK-61

            for (int i=0; i < blueLabels.length; i++) {
                // align yellow labels on the left side
                View modView = mainActivity.findViewById(pairedYellowLabels[i]);
                modView.setPadding(yellowLabelLeftPadding, 0, 0, 0);

                LayoutParams params = (LayoutParams) modView.getLayoutParams();
                params.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

                // add back blue labels
                TextView blueLabel = mainActivity.findViewById(blueLabels[i]);
                blueLabel.setText(blueLabelsText[i]);
            }

            // add "e", remove white "НОП", replace "Cx" with "CX"
            ((TextView) mainActivity.findViewById(R.id.labelE)).setText("e");
            ((TextView) mainActivity.findViewById(R.id.labelNop54)).setText("");
            ((Button)mainActivity.findViewById(R.id.buttonClear)).setText("CX");

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainActivity);
            boolean isMemButtons34 = prefs.getBoolean(PreferencesActivity.PREFERENCE_MEM_BUTTONS_54,
                    false);

            // use manually created symbols for some buttons
            Typeface tf = Typeface.createFromAsset(mainActivity.getAssets(), FONT_MISSING_SYMBOLS);
            setMemoryButtonsSkin(isMemButtons34);

            Button b = mainActivity.findViewById(R.id.buttonExchangeXY);
            b.setText(mainActivity.getString(R.string.buttonExchangeXY));
            b.setTypeface(tf, Typeface.NORMAL);
        }
    }

    static private void setMemoryButtonsSkin(boolean isMK54) {
         if (isMK54) {
             Button clearButton = mainActivity.findViewById(R.id.buttonClear);
             Typeface tf = clearButton.getTypeface();
             Button b = mainActivity.findViewById(R.id.buttonRegisterToX);
             b.setText("ИП");
             b.setTypeface(tf);

             b = mainActivity.findViewById(R.id.buttonXToRegister);
             b.setText("П");
             b.setTypeface(tf);
         } else {
             Typeface tf = Typeface.createFromAsset(mainActivity.getAssets(), FONT_MISSING_SYMBOLS);
             // use manually created symbols for some buttons
             Button b = mainActivity.findViewById(R.id.buttonRegisterToX);
             b.setText(mainActivity.getString(R.string.buttonRegisterToX));
             b.setTypeface(tf, Typeface.NORMAL);

             b = mainActivity.findViewById(R.id.buttonXToRegister);
             b.setText(mainActivity.getString(R.string.buttonXToRegister));
             b.setTypeface(tf, Typeface.NORMAL);
         }
    }

    static List<View> getAllChildrenBFS(View v) {
        List<View> visited = new ArrayList<>();
        List<View> unvisited = new ArrayList<>();
        unvisited.add(v);

        while (!unvisited.isEmpty()) {
            View child = unvisited.remove(0);
            visited.add(child);
            if (!(child instanceof ViewGroup)) continue;
            ViewGroup group = (ViewGroup) child;
            final int childCount = group.getChildCount();
            for (int i=0; i<childCount; i++) unvisited.add(group.getChildAt(i));
        }

        return visited;
    }

    static void style(boolean grayscale, int indicatorMode,
                float prefButtonTextSize, float prefLabelTextSize,
                boolean borderBlackButtons, boolean borderOtherButtons,
                      boolean isMK54MemoryButtons) {
        // set background
        styleScreen(grayscale);

        // style indicator
        styleIndicator(grayscale, indicatorMode);

        // scale buttons text and set borders
        SkinHelper.styleButtons(grayscale,
                prefButtonTextSize, prefLabelTextSize,
                borderBlackButtons, borderOtherButtons);
        
        // style labels above buttons
        SkinHelper.styleLabels(grayscale);

        SkinHelper.setMemoryButtonsSkin(isMK54MemoryButtons);
    }

    /**
     * Sets indicator colors
     * @param grayscale true if grayscale mode
     * @param mode -1 - off, 0 - fast mode, 1 - normal mode
     */
    static void styleIndicator(boolean grayscale, int mode) {
        TextView calculatorIndicator = mainActivity.findViewById(R.id.textView_Indicator);

        int indicatorTextColor = mainActivity.getResources().getColor(grayscale
                ? R.color.indicatorDigitsGrayscale
                : R.color.indicatorDigits);

        // set indicator digits color
        calculatorIndicator.setTextColor(indicatorTextColor);
        TextView indicator = mainActivity.findViewById(R.id.indicatorTurbo);
        indicator.setTextColor(indicatorTextColor);
        indicator = mainActivity.findViewById(R.id.indicatorF);
        indicator.setTextColor(indicatorTextColor);
        indicator = mainActivity.findViewById(R.id.indicatorK);
        indicator.setTextColor(indicatorTextColor);

        // set indicator background color
        int color;
        if (mode < 0) {
            color = R.color.indicatorOff;
        } else {
            color = (mode == 0)
                    ? (grayscale
                            ? R.color.indicatorFastSpeedModeGrayscale
                            : R.color.indicatorFastSpeedMode
                      )
                    : (grayscale
                            ? R.color.indicatorSlowSpeedModeGrayscale
                            : R.color.indicatorSlowSpeedMode
                      );
        }

        int bgColor = mainActivity.getResources().getColor(color);
        calculatorIndicator.setBackgroundColor(bgColor);

        calculatorIndicator = mainActivity.findViewById(R.id.textView_IndicatorY);
        calculatorIndicator.setTextColor(indicatorTextColor);
        calculatorIndicator.setBackgroundColor(bgColor);

        int indicatorVisibility = mode == 0 ? View.VISIBLE : View.INVISIBLE;

        mainActivity.findViewById(R.id.indicatorTurbo).setVisibility(indicatorVisibility);
    }

    static private void styleButtonF(boolean grayscale, boolean borderOtherButtons) {
        View buttonF = mainActivity.findViewById(R.id.buttonF);

        int bgResourceId;
        if (grayscale) {
            bgResourceId = borderOtherButtons ? R.drawable.button_yellow_border_grayscale :
                                                R.drawable.button_yellow_grayscale;
        } else {
            bgResourceId = borderOtherButtons ? R.drawable.button_yellow_border :
                    R.drawable.button_yellow;
        }
        buttonF.setBackgroundResource(bgResourceId);
    }

    static private void styleButtonK(boolean grayscale, boolean borderOtherButtons) {
        View buttonK = mainActivity.findViewById(R.id.buttonK);

        int bgResourceId;
        if (grayscale) {
            bgResourceId = borderOtherButtons ? R.drawable.button_blue_border_grayscale :
                    R.drawable.button_blue_grayscale;
        } else {
            bgResourceId = borderOtherButtons ? R.drawable.button_blue_border :
                    R.drawable.button_blue;
        }
        buttonK.setBackgroundResource(bgResourceId);
    }
    
    private static void styleScreen(boolean grayscale) {
        mainActivity.findViewById(R.id.mainLayout).setBackgroundColor(
                mainActivity.getResources().getColor(grayscale
                            ? R.color.commonBackgroundGrayscale
                            : R.color.commonBackground));
    }

    private static void styleLabels(boolean grayscale) {

        int color = mainActivity.getResources().getColor(grayscale
                ? R.color.aboveButtonTextYellowGrayscale
                : R.color.aboveButtonTextYellow);
        for (int singleYellowLabel : singleYellowLabels) {
            ((TextView) mainActivity.findViewById(singleYellowLabel)).setTextColor(color);
        }
        for (int pairedYellowLabel : pairedYellowLabels) {
            ((TextView) mainActivity.findViewById(pairedYellowLabel)).setTextColor(color);
        }

        color = mainActivity.getResources().getColor(grayscale
                ? R.color.aboveButtonTextBlueGrayscale
                : R.color.aboveButtonTextBlue);
        for (int blueLabel : blueLabels) {
            ((TextView) mainActivity.findViewById(blueLabel)).setTextColor(color);
        }
    }
    
    private static void styleButtons(boolean grayscale, 
            float prefButtonTextSize, float prefLabelTextSize,
            boolean borderBlackButtons, boolean borderOtherButtons) {

        float chosenButtonTextSize = buttonTextSize * prefButtonTextSize;
        float chosenLabelTextSize  = labelTextSize  * prefLabelTextSize;

        int bgResource = borderOtherButtons ? R.drawable.button_other_border : R.drawable.button_other;

        HashSet<View> set = new HashSet<>(getAllChildrenBFS(mainActivity.findViewById(R.id.tableLayoutKeyboard)));
        for (View view: set) {
            if (view instanceof Button) {
                Button b = (Button)view;
                b.setTextSize(TypedValue.COMPLEX_UNIT_PX, chosenButtonTextSize);
                boolean isBlack = false;
                int butId = b.getId();
                for (int j=0; j < blackButtons.length; j++) {
                    if (butId == blackButtons[j]) {
                        isBlack = true;
                        break;
                    }
                }

                if (isBlack) {
                    b.setBackgroundResource(borderBlackButtons
                            ? R.drawable.button_black_border
                            : R.drawable.button_black
                    );

                } else if (butId == R.id.buttonF) {
                    styleButtonF(grayscale, borderOtherButtons);

                } else if (butId == R.id.buttonK) {
                    styleButtonK(grayscale, borderOtherButtons);

                } else if (butId == R.id.buttonClear) {
                        b.setBackgroundResource(borderOtherButtons
                                ?	(grayscale
                                        ? R.drawable.button_red_border_grayscale
                                        : R.drawable.button_red_border
                                    )
                                :	(grayscale
                                        ? R.drawable.button_red_grayscale
                                        : R.drawable.button_red
                                    )
                        );

                } else {
                        b.setBackgroundResource(bgResource);
                }

            } else if (view instanceof TextView) {
                ((TextView)view).setTextSize(TypedValue.COMPLEX_UNIT_PX, chosenLabelTextSize);
            }
        }

        /*
        float smallerButtonTextSize = (float) (chosenButtonTextSize * 0.8);
        ((Button)mainActivity.findViewById(R.id.buttonReturn   )).setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerButtonTextSize);
        ((Button)mainActivity.findViewById(R.id.buttonStopStart)).setTextSize(TypedValue.COMPLEX_UNIT_PX, smallerButtonTextSize);
        */
    }
    
}
