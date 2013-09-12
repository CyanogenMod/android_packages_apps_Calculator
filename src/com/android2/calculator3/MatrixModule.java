package com.android2.calculator3;

import org.ejml.simple.SimpleMatrix;
import org.ejml.simple.SimpleSVD;
import org.javia.arity.SyntaxException;

import com.android2.calculator3.BaseModule.Mode;
import com.android2.calculator3.view.AdvancedDisplay;
import com.android2.calculator3.view.MatrixView;

public class MatrixModule {
    Logic logic;

    MatrixModule(Logic logic) {
        this.logic = logic;
    }

    static SimpleMatrix addScalar(SimpleMatrix mat, double scalar) {
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
    
    private String calculate(String input) throws SyntaxException
    {
    	//How to handle parens? Recursion.
    	int open = 0;
    	int close = 0;
    	int lastOpen = 0;
    	for(int i = 0; i < input.length(); i++)
    	{
    		if(input.charAt(i) == '(')
    		{
    			//if(close != 0) throw new Exception();
    			open++;
    			if(lastOpen == 0) lastOpen = i;
    		}
    		else if(input.charAt(i) == ')')
    		{
    			if(open == 0) throw new SyntaxException();
    			close++;
    			if((open == close) && open != 0)
    			{
    				//Balanced means one whole set
    				StringBuffer temp = new StringBuffer(input);
    				String res = calculate(temp.substring(lastOpen+1, i).toString());
    				temp.replace(lastOpen, i+1, res);
    				input = temp.toString();
    			}
    		}
    	}

    	//String TEST_INPUT = "8.7^2.3+3.4*5.7";

    	String[] pieces = input.split("\u00d7|\\+|-|\u00f7|\\^");
    	char[] ops = opSplit(input);

    	//If there's no ops, there's nothing to do
    	if(ops.length == 0) return input;

    	for(int i = 0; i < ops.length; i++)
    	{
    		int[] landr  = null;
    		if(ops[i] == '^')
    		{
    			landr = lookAfield(pieces, i);
    			int l = landr[0];
    			int r = landr[1];
    			String res = applyPow(pieces[l], pieces[r]);

    			pieces[l] = res;
    			pieces[r] = "NAN";
    		}
    	}

    	for(int i = 0; i < ops.length; i++)
    	{
    		int[] landr  = null;
    		if(ops[i] == '\u00d7')
    		{
    			landr = lookAfield(pieces, i);
    			int l = landr[0];
    			int r = landr[1];
    			String res = applyMult(pieces[l], pieces[r]);

    			pieces[i] = res;
    			pieces[i+1] = "NAN";
    		}
    	}

    	for(int i = 0; i < ops.length; i++)
    	{
    		int[] landr  = null;
    		if(ops[i] == '\u00f7')
    		{
    			landr = lookAfield(pieces, i);
    			int l = landr[0];
    			int r = landr[1];
    			String res = applyDiv(pieces[l], pieces[r]);

    			pieces[l] = res;
    			pieces[r] = "NAN";
    		}
    	}

    	for(int i = 0; i < ops.length; i++)
    	{
    		int[] landr  = null;
    		if(ops[i] == '+')
    		{
    			landr = lookAfield(pieces, i);
    			int l = landr[0];
    			int r = landr[1];
    			String res = applyPlus(pieces[l], pieces[r]);

    			pieces[l] = res;
    			pieces[r] = "NAN";
    		}
    	}

    	for(int i = 0; i < ops.length; i++)
    	{
    		int[] landr  = null;
    		if(ops[i] == '-')
    		{
    			landr = lookAfield(pieces, i);
    			int l = landr[0];
    			int r = landr[1];
    			String res = applySub(pieces[l], pieces[r]);

    			pieces[l] = res;
    			pieces[r] = "NAN";
    		}
    	}

    	for(String piece: pieces)
    		if(piece.compareTo("NAN") != 0) return piece;
    	return "BROKEN";
    }//end main

    String evaluateMatrices(AdvancedDisplay display) throws SyntaxException {
    	String text = display.getText();
    	String result = calculate(text);
    	
    	return logic.mBaseModule.updateTextToNewMode(result, Mode.DECIMAL, logic.mBaseModule.getMode());
    }
    
    private static char[] opSplit(String str)
	{
		StringBuilder buffer = new StringBuilder();
		for(char c: str.toCharArray())
			if(c == '^' || c == '\u00d7' || c == '\u00f7' || c == '+' || c=='-')
				buffer.append(c);
		
		return buffer.toString().toCharArray();
	}
	
	private static int[] lookAfield(String[] field, int orig)
	{
		int left, right;
		
		int pos = orig;
		while(field[pos]=="NAN")
			pos--;
		left = pos;
		//Look right
		pos = orig+1;
		while(field[pos] == "NAN")
			pos++;
		right = pos;
		
		return new int[] {left, right};
	}
	
	private  String applyPow(String l, String r) throws SyntaxException
	{
		if(l.startsWith("[[") && r.startsWith("[[")) throw new SyntaxException();
        else if(l.startsWith("[["))
        {
            SimpleMatrix a = parseMatrix(l);
            int m = a.numRows();
            int n = a.numCols();
            if(m != n) throw new SyntaxException();
            double b = Double.parseDouble(r);
            if(b > Math.floor(b))
            {
            	SimpleSVD decomp = new SimpleSVD(a.getMatrix(), false);
                SimpleMatrix S = decomp.getW();
                for(int i1 = 0; i1 < m; i1++) {
                    for(int j = 0; j < n; j++) {
                        double arg = S.get(i1, j);
                        S.set(i1, j, Math.pow(arg, b));
                    }
                }
                SimpleMatrix matrix = decomp.getU().mult(S);
                matrix = matrix.mult(decomp.getV().transpose());
                return MatrixView.matrixToString(matrix, logic);
            }
            else
            {
            	long equiv = Math.round(b);
                for(long e = 1; e < equiv; e++)
                    a = a.mult(a);
                
                return MatrixView.matrixToString(a, logic);
            }
        }
        else if(r.startsWith("[["))
        {
        	SimpleMatrix a = parseMatrix(l);
            int m = a.numRows();
            int n = a.numCols();
            if(m != n) throw new SyntaxException();
            double b = Double.parseDouble(r);
            if(b > Math.floor(b))
            {
            	SimpleSVD decomp = new SimpleSVD(a.getMatrix(), false);
                SimpleMatrix S = decomp.getW();
                for(int i1 = 0; i1 < m; i1++) {
                    for(int j = 0; j < n; j++) {
                        double arg = S.get(i1, j);
                        S.set(i1, j, Math.pow(arg, b));
                    }
                }
                SimpleMatrix matrix = decomp.getU().mult(S);
                matrix = matrix.mult(decomp.getV().transpose());
                return MatrixView.matrixToString(matrix, logic);
            }
            else
            {
            	long equiv = Math.round(b);
                for(long e = 1; e < equiv; e++)
                    a = a.mult(a);
                
                return MatrixView.matrixToString(a, logic);
            }
        }
        else
        {
            double a = Double.parseDouble(l);
            double b = Double.parseDouble(r);
            return String.format("%f", Math.pow(a, b));
        }
	}

	private String applyMult(String l, String r) throws SyntaxException
	{
		if(l.startsWith("[[") && r.startsWith("[["))
		{
			SimpleMatrix a = parseMatrix(l);
			SimpleMatrix b = parseMatrix(r);
			return MatrixView.matrixToString(a.mult(b), logic);
		}
		else if(l.startsWith("[["))
		{
			SimpleMatrix a = parseMatrix(l);
			double b = Double.parseDouble(r);
			return MatrixView.matrixToString(a.scale(b), logic);
		}
		else if(r.startsWith("[["))
		{
			SimpleMatrix a = parseMatrix(r);
			double b = Double.parseDouble(l);
			return MatrixView.matrixToString(a.scale(b), logic);
		}
		else
		{
			double a = Double.parseDouble(l);
			double b = Double.parseDouble(r);
			return String.format("%f", a*b);
		}
	}
	
	private String applyDiv(String l, String r) throws SyntaxException 
	{
		if(l.startsWith("[[") && r.startsWith("[["))
        {
            SimpleMatrix a = parseMatrix(l);
            SimpleMatrix b = parseMatrix(r);
            return MatrixView.matrixToString(a.mult(b.pseudoInverse()), logic);
        }
        else if(l.startsWith("[["))
        {
            SimpleMatrix a = parseMatrix(l);
            double b = Double.parseDouble(r);
            return MatrixView.matrixToString(a.scale(1.0/b), logic);
        }
        else if(r.startsWith("[["))
        {
            SimpleMatrix a = parseMatrix(r);
            double b = Double.parseDouble(l);
            return MatrixView.matrixToString(a.scale(1.0/b), logic);
        }
        else
        {
            double a = Double.parseDouble(l);
            double b = Double.parseDouble(r);
            return String.format("%f", a/b);
        }
	}
	
	private String applyPlus(String l, String r) throws SyntaxException 
	{
		if(l.startsWith("[[") && r.startsWith("[["))
        {
            SimpleMatrix a = parseMatrix(l);
            SimpleMatrix b = parseMatrix(r);
            return MatrixView.matrixToString(a.plus(b), logic);
        }
        else if(l.startsWith("[["))
        {
            SimpleMatrix a = parseMatrix(l);
            double b = Double.parseDouble(r);
            return MatrixView.matrixToString(addScalar(a,b), logic);
        }
        else if(r.startsWith("[["))
        {
            SimpleMatrix a = parseMatrix(r);
            double b = Double.parseDouble(l);
            return MatrixView.matrixToString(addScalar(a,b), logic);
        }
        else
        {
            double a = Double.parseDouble(l);
            double b = Double.parseDouble(r);
            return String.format("%f", a+b);
        }
	}

	private  String applySub(String l, String r) throws SyntaxException
	{
		if(l.startsWith("[[") && r.startsWith("[["))
        {
            SimpleMatrix a = parseMatrix(l);
            SimpleMatrix b = parseMatrix(r);
            return MatrixView.matrixToString(a.minus(b), logic);
        }
        else if(l.startsWith("[["))
        {
            SimpleMatrix a = parseMatrix(l);
            double b = Double.parseDouble(r);
            return MatrixView.matrixToString(addScalar(a, -1.0*b), logic);
        }
        else if(r.startsWith("[["))
        {
            SimpleMatrix a = parseMatrix(r);
            double b = Double.parseDouble(l);
            return MatrixView.matrixToString(addScalar(a, -1.0*b), logic);
        }
        else
        {
            double a = Double.parseDouble(l);
            double b = Double.parseDouble(r);
            return String.format("%f", a-b);
        }
	}
	
	private static SimpleMatrix parseMatrix(String text)
	{
		//Count rows & cols
		String interior = text.substring(2, text.length()-2);
		String[] rows = interior.split("\\]\\[");
		
		SimpleMatrix temp = new SimpleMatrix(rows.length, rows[0].split(",").length);
		
		for(int i = 0; i < rows.length; i++)
		{
			String[] cols = rows[i].split(",");
			for(int j = 0; j < cols.length; j++)
				temp.set(i, j, Double.parseDouble(cols[j]));
		}
		
		return temp;
	}
}
