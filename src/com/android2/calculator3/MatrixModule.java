package com.android2.calculator3;

import org.ejml.simple.SimpleMatrix;
import org.javia.arity.SyntaxException;

import android.view.View;

import com.android2.calculator3.BaseModule.Mode;
import com.android2.calculator3.view.AdvancedDisplay;
import com.android2.calculator3.view.MatrixInverseView;
import com.android2.calculator3.view.MatrixTransposeView;
import com.android2.calculator3.view.MatrixView;

public class MatrixModule {
    Logic logic;

    MatrixModule(Logic logic) {
        this.logic = logic;
    }

    String evaluateMatrices(AdvancedDisplay display) throws SyntaxException {
        try {
            SimpleMatrix matrix = null;
            boolean add = false;
            boolean multiply = false;
            for(int i = 0; i < display.getChildCount(); i++) {
                View child = display.getChildAt(i);
                if(child instanceof MatrixView) {
                    if(!add && !multiply) {
                        matrix = ((MatrixView) child).getSimpleMatrix();
                    }
                    else if(add) {
                        add = false;
                        if(matrix == null) throw new SyntaxException();
                        matrix = matrix.plus(((MatrixView) child).getSimpleMatrix());
                    }
                    else if(multiply) {
                        multiply = false;
                        if(matrix == null) throw new SyntaxException();
                        matrix = matrix.mult(((MatrixView) child).getSimpleMatrix());
                    }
                }
                else if(child instanceof MatrixTransposeView) {
                    if(matrix == null) throw new SyntaxException();
                    matrix = matrix.transpose();
                }
                else if(child instanceof MatrixInverseView) {
                    if(matrix == null) throw new SyntaxException();
                    matrix = matrix.invert();
                }
                else {
                    String text = child.toString();
                    if(text.length() > 1) throw new SyntaxException();
                    else if(text.length() == 0) continue;
                    if(text.startsWith(String.valueOf(Logic.MUL))) multiply = true;
                    else if(text.startsWith(String.valueOf(Logic.PLUS))) add = true;
                    else throw new SyntaxException();
                }
            }
            return logic.mBaseModule.updateTextToNewMode(MatrixView.matrixToString(matrix, logic), Mode.DECIMAL, logic.mBaseModule.getMode());
        }
        catch(Exception e) {
            throw new SyntaxException();
        }
    }
}
