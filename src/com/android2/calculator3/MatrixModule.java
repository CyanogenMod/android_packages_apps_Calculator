package com.android2.calculator3;

import org.ejml.simple.SimpleMatrix;
import org.ejml.simple.SimpleSVD;
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
            boolean power = false;
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
                    else if(text.startsWith(String.valueOf(Logic.POW))) power = true;
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
                        	matrix = matrix.scale(scal);
                        	multiply = false;
                        }
                        else if(divide) {
                        	matrix = matrix.scale(1.0/scal);
                        	divide = false;
                        }
                        else if(power) {
                        	int m = matrix.numRows();
                        	int n = matrix.numCols();
                        	if(m != n) throw new SyntaxException();
                        	else if(scal == 0.0) matrix = SimpleMatrix.identity(m);
                        	else if(scal == 1.0) continue;
                        	else {
	                        	//Two methods
	                        	//If power is real, use SVD with questionable accuracy
	                        	if(scal > Math.floor(scal)) {
		                        	SimpleSVD decomp = new SimpleSVD(matrix.getMatrix(), false);
		                        	SimpleMatrix S = decomp.getW();
		                        	for(int i1 = 0; i1 < m; i1++) {
		                        		for(int j = 0; j < n; j++) {
		                        			double arg = S.get(i1, j);
		                        			S.set(i1, j, Math.pow(arg, scal));
		                        		}
		                        	}
		                        	matrix = decomp.getU().mult(S);
		                        	matrix = matrix.mult(decomp.getV().transpose());
		                        }
	                        	else { //Integer power: multiply by itself
	                        		long equiv = Math.round(scal);
	                        		for(long e = 1; e < equiv; e++)
	                        			matrix = matrix.mult(matrix);
		                        }
                        	}//close else (113)
                        }//close else if (107)
                        else throw new SyntaxException();
                    }//close if (88)
                    else if((text.length() > 1) && !Character.isDigit(text.charAt(1))) throw new SyntaxException();
                }
            }
            return logic.mBaseModule.updateTextToNewMode(MatrixView.matrixToString(matrix, logic), Mode.DECIMAL, logic.mBaseModule.getMode());
        }
        catch(Exception e) {
            throw new SyntaxException();
        }
    }
}
