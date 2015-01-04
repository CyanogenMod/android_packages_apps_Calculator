package com.android.calculator2.view;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.android.calculator2.R;
import com.android.calculator2.view.display.AdvancedDisplay;
import com.android.calculator2.view.display.AdvancedDisplayControls;
import com.android.calculator2.view.display.DisplayComponent;
import com.android.calculator2.view.display.EventListener;
import com.xlythe.math.Constants;
import com.xlythe.math.Solver;

import org.ejml.simple.SimpleMatrix;
import org.javia.arity.SyntaxException;

import java.text.DecimalFormatSymbols;
import java.util.regex.Pattern;

public class DivisionView extends LinearLayout implements AdvancedDisplayControls {
    private EditText mTop;
    private EditText mBottom;
    private EventListener mListener;
    private Solver mSolver;

    public DivisionView(Context context) {
        super(context);
        setup();
    }

    private void setup() {
        setBackgroundResource(R.drawable.division);
        setFocusable(true);
    }

    @Override
    public String toString() {
        return mTop.getText().toString() + Constants.DIV + mBottom.getText();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mTop.setEnabled(enabled);
        mBottom.setEnabled(enabled);
    }

    @Override
    public boolean hasNext() {
        return mTop.isFocused() && mBottom.getText().toString().isEmpty();
    }

    private static String grabFirstNumber(String text) {
        StringBuilder number = new StringBuilder();
        if(text.startsWith("(")) {
            int paren = 0;
            do {
                if(text.startsWith("(")) {
                    paren++;
                }
                else if(text.startsWith(")")) {
                    paren--;
                }
                number.append(text.substring(0, 1));
                text = text.substring(1);
            } while(!text.isEmpty() && paren > 0);

            return number.toString();
        }
        else {
            while(!text.isEmpty() && text.substring(0, 1).matches(Constants.REGEX_NUMBER)) {
                number.append(text.substring(0, 1));
                text = text.substring(1);
            }
            return number.toString();
        }
    }

    private static boolean verify(String text) {
        // Strip off the first number and see if a division sign is next
        text = text.substring(grabFirstNumber(text).length());
        if(text.startsWith(String.valueOf(Constants.DIV)) || text.startsWith("/")) {
            return true;
        }

        return false;
    }

    public static class DVDisplayComponent implements DisplayComponent {
        @Override
        public View getView(Context context, Solver solver, String equation, EventListener listener) {
            DivisionView dv = new DivisionView(context);
            dv.mSolver = solver;
            dv.mListener = listener;

            AdvancedDisplay.LayoutParams params = new AdvancedDisplay.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.CENTER_VERTICAL;
            dv.setLayoutParams(params);

            return dv;
        }

        @Override
        public String parse(String equation) {
            if(DivisionView.verify(equation)) {
                StringBuilder buffer = new StringBuilder();

                // Grab the first number
                buffer.append(DivisionView.grabFirstNumber(equation));
                equation = equation.substring(buffer.length());

                // Grab the sign
                buffer.append(equation.charAt(0));
                equation = equation.substring(1);

                // Grab the second number
                buffer.append(DivisionView.grabFirstNumber(equation));

                return buffer.toString();
            }
            else {
                return null;
            }
        }
    }
}
