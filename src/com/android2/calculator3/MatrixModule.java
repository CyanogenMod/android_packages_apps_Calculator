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
    Logic mLogic;

    MatrixModule(Logic logic) {
        this.mLogic = logic;
    }

    SimpleMatrix addScalar(SimpleMatrix mat, double scalar) {
        SimpleMatrix temp = mat.copy();
        int M = mat.numRows();
        int N = mat.numCols();
        for(int i = 0; i < M; i++) {
            for(int j = 0; j < N; j++)
                temp.set(i, j, mat.get(i, j) + scalar);
        }
        return temp;
    }

    double gatherScalar(String text) throws SyntaxException {
        if(!Character.isDigit(text.charAt(1))) throw new SyntaxException();
        return Double.parseDouble(text.substring(1));
    }

    String evaluateMatrices(AdvancedDisplay display) throws SyntaxException {
        try {
            SimpleMatrix matrix = null;
            boolean add = false;
            boolean multiply = false;
            boolean subtract = false;
            boolean divide = false;
            for(int i = 0; i < display.getChildCount(); i++) {
                View child = display.getChildAt(i);
                if(child instanceof MatrixView) {
                    if(!add && !multiply && !divide && !subtract) {
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
                    else if(subtract) {
                        subtract = false;
                        if(matrix == null) throw new SyntaxException();
                        matrix = matrix.minus(((MatrixView) child).getSimpleMatrix());
                    }
                    else if(divide) {
                        divide = false;
                        if(matrix == null) throw new SyntaxException();
                        matrix = matrix.mult(((MatrixView) child).getSimpleMatrix().pseudoInverse());
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
                    if(text.length() == 0) continue;
                    if(text.startsWith(String.valueOf(Logic.MUL))) multiply = true;
                    else if(text.startsWith(String.valueOf(Logic.PLUS))) add = true;
                    else if(text.startsWith(String.valueOf(Logic.MINUS))) subtract = true;
                    else if(text.startsWith(String.valueOf(Logic.DIV))) divide = true;
                    if((text.length() > 1) && Character.isDigit(text.charAt(1))) {
                        double scal = gatherScalar(text);
                        if(add) {
                            matrix = addScalar(matrix, scal);
                            add = false;
                        }
                        else if(subtract) {
                            matrix = addScalar(matrix, -1.0 * scal);
                            subtract = false;
                        }
                        else if(multiply) {
                            matrix = (new SimpleMatrix(matrix.numRows(), matrix.numCols())).plus(scal, matrix);
                            multiply = false;
                        }
                        else if(divide) {
                            matrix = (new SimpleMatrix(matrix.numRows(), matrix.numCols())).plus(1.0 / scal, matrix);
                            divide = false;
                        }
                        else throw new SyntaxException();
                    }
                    else if((text.length() > 1) && !Character.isDigit(text.charAt(1))) throw new SyntaxException();
                }
            }
            return mLogic.mBaseModule.updateTextToNewMode(MatrixView.matrixToString(display.getContext(), matrix, mLogic), Mode.DECIMAL,
                    mLogic.mBaseModule.getMode());
        }
        catch(Exception e) {
            throw new SyntaxException();
        }
    }
}
