package com.android2.calculator3;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ejml.simple.SimpleEVD;
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
    	//I never realized negative numbers could be so difficult.
    	input = input.replaceAll("(\\d+(?:\\.\\d+)?|\\[.+\\])\u2212(\\d+(?:\\.\\d+)?)", "$1-$2");
    	//All remaining instances of U+2212 will be on negative numbers.
    	//They will be counted as whole tokens.
    	
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
    	
    	//Handle functions.
    	Matcher match = Pattern.compile("(\u221a|log|ln|sin\\^-1|cos\\^-1|tan\\^-1|sin|cos|tan)(\u2212?\\d+(?:\\.\\d+)?|\\[\\[.+\\]\\])")
    			.matcher(input);
    	while(match.find())
    	{
    		String res = applyFunc(match.group(1), match.group(2));
    		input = input.replace(match.group(), res);
    	}
    	
    	//Substitute e
    	input = input.replaceAll("(?<!\\d)e", "2.7182818284590452353");
    	//Sub pi
    	input = input.replace("\u03c0", "3.141592653589");

    	//Split into seperate arrays of operators and operands.
    	//Operator 0 applies to operands 0 and 1, and so on
    	String[] parts = input.split("\u00d7|\\+|(?<=\\d|\\])-|\u00f7|\\^");
    	char[] ops = opSplit(input);
    	
    	//Fill in the pieces.
    	//Store everything a Object, and cast out later
    	Object[] pieces = new Object[parts.length];
    	for(int i = 0; i < parts.length; i++)
    	{
    		if(parts[i].startsWith("[["))
    			pieces[i] = (Object) parseMatrix(parts[i]);
    		else
    			pieces[i] = (Object) Double.parseDouble(parts[i].replace('\u2212', '-'));
    	}

    	//If there are no ops, there's nothing to do
    	if(ops.length == 0) return input;

    	//Work on the operators in order of their precedence.
    	for(int i = 0; i < ops.length; i++)
    	{
    		int[] landr  = null;
    		if(ops[i] == '^')
    		{
    			landr = lookAfield(pieces, i);
    			int l = landr[0]; 
    			int r = landr[1];
    			Object res = applyPow(pieces[l], pieces[r]);

    			pieces[l] = res;//This is arbitrary (I think)
    			pieces[r] = null;//I could also have put the result in right
    			//and null in left
    		}
    	}

    	//Yes, I'm doing a complete loop over all operators several times.
    	//Realistically, there will only be a few of them. 
    	//For the purposes of this app, it's no big deal.
    	for(int i = 0; i < ops.length; i++)
    	{
    		int[] landr  = null;
    		if(ops[i] == '\u00d7')
    		{
    			landr = lookAfield(pieces, i);
    			int l = landr[0];
    			int r = landr[1];
    			Object res = applyMult(pieces[l], pieces[r]);

    			pieces[i] = res;
    			pieces[i+1] = null;
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
    			Object res = applyDiv(pieces[l], pieces[r]);

    			pieces[l] = res;
    			pieces[r] = null;
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
    			Object res = applyPlus(pieces[l], pieces[r]);

    			pieces[l] = res;
    			pieces[r] = null;
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
    			Object res = applySub(pieces[l], pieces[r]);

    			pieces[l] = res;
    			pieces[r] = null;
    		}
    	}

    	for(Object piece: pieces)
    		if(piece != null) {
    			if(piece instanceof Double)
    				return String.format("%f", (Double)piece);
    			else if(piece instanceof SimpleMatrix)
    				return MatrixView.matrixToString((SimpleMatrix) piece, logic);
    			else throw new SyntaxException(); //Neither matrix nor double should never happen
    		}
    	throw new RuntimeException(); //Getting here should be impossible
    }//end main

    String evaluateMatrices(AdvancedDisplay display) throws SyntaxException {
    	String text = display.getText();
    	String result = "";
    	//try{
    		result = calculate(text).replace('-', '\u2212');//Back to fancy minus
    	//}catch(Exception e){
    		//result = "Error";
    	//}
    	
    	return logic.mBaseModule.updateTextToNewMode(result, Mode.DECIMAL, logic.mBaseModule.getMode());
    }
    
    private static char[] opSplit(String str)
	{
		StringBuilder buffer = new StringBuilder();
		for(int i = 0; i < str.length(); i++)
		{
			char c = str.charAt(i);
			if(c == '^' || c == '\u00d7' || c == '\u00f7' || c == '+')
				buffer.append(c);
			else if(c == '-' && (Character.isDigit(str.charAt(i-1)) || str.charAt(i-1) == ']'))
				buffer.append(c);
		}
		
		return buffer.toString().toCharArray();
	}
	
	//Look for the nearest valid operand
    private static int[] lookAfield(Object[] field, int orig)
	{
		int left, right;
		
		//Start with the original position (of the operator)
		//Left operand is at the same index as its operator
		//But if it's null, look farther left
		int pos = orig;
		while(field[pos]==null) //
			pos--;
		left = pos;
		//Right operand is one greater than the operator index
		pos = orig+1;
		while(field[pos] == null)//Look to the right if null
			pos++;
		right = pos;//Found it
		
		return new int[] {left, right};//Return the indices to allow later sub-in of null
	}
	
    //The following have a lot of repeated boilerplate code.
    //Condensing it down would require language features/properties
    //that Java does not have.
    //In short, Java is not F#. 
    
	private String applyFunc(String func, String arg) throws SyntaxException
	{
		arg = arg.replace('\u2212', '-');
		if(func.equals("\u221a"))//sqrt
		{
			if(arg.startsWith("[["))
			{
				SimpleMatrix matrix = parseMatrix(arg);
				int m = matrix.numRows();
				int n = matrix.numCols();
				if(m != n) throw new SyntaxException();
				SimpleEVD decomp = new SimpleEVD(matrix.getMatrix());
				double[] evals = new double[m];
				for(int i1 = 0; i1 < m; i1++) {
					evals[i1] = Math.sqrt(decomp.getEigenvalue(i1).getMagnitude());
				}
				SimpleMatrix D = SimpleMatrix.diag(evals);
				SimpleMatrix V = new SimpleMatrix(m,n);
				for(int k = 0; k < m; k++) {
					SimpleMatrix col = decomp.getEigenVector(k);
					for(int l = 0; l < n; l++) {
						V.set(k, l, col.get(l,0));
					}
				}
				SimpleMatrix temp = V.mult(D);
				temp = temp.mult(V.invert());
				return MatrixView.matrixToString(temp, logic);
			}
			else return String.format("%f", Math.sqrt(Double.parseDouble(arg)));
		}
		if(func.equals("sin"))
		{
			if(arg.startsWith("[["))
			{
				SimpleMatrix m = parseMatrix(arg);
				for(int i = 0; i < m.numRows(); i++)
					for(int j = 0; j < m.numCols(); j++)
						m.set(i, j, Math.sin(m.get(i,j)));
				return MatrixView.matrixToString(m, logic);
			}
			else return String.format("%f", Math.sin(Double.parseDouble(arg)));
		}
		else if(func.equals("cos"))
		{
			if(arg.startsWith("[["))
			{
				SimpleMatrix m = parseMatrix(arg);
				for(int i = 0; i < m.numRows(); i++)
					for(int j = 0; j < m.numCols(); j++)
						m.set(i, j, Math.cos(m.get(i,j)));
				return MatrixView.matrixToString(m, logic);
			}
			else return String.format("%f", Math.cos(Double.parseDouble(arg)));
		}
		else if(func.equals("tan"))
		{
			if(arg.startsWith("[["))
			{
				SimpleMatrix m = parseMatrix(arg);
				for(int i = 0; i < m.numRows(); i++)
					for(int j = 0; j < m.numCols(); j++)
						m.set(i, j, Math.tan(m.get(i,j)));
				return MatrixView.matrixToString(m, logic);
			}
			else return String.format("%f", Math.tan(Double.parseDouble(arg)));
		}
		else if(func.equals("log"))
		{
			if(arg.startsWith("[["))
			{
				SimpleMatrix m = parseMatrix(arg);
				for(int i = 0; i < m.numRows(); i++)
					for(int j = 0; j < m.numCols(); j++)
						m.set(i, j, Math.log10(m.get(i,j)));
				return MatrixView.matrixToString(m, logic);
			}
			else return String.format("%f", Math.log10(Double.parseDouble(arg)));
		}
		else if(func.equals("ln"))
		{
			if(arg.startsWith("[["))
			{
				SimpleMatrix m = parseMatrix(arg);
				for(int i = 0; i < m.numRows(); i++)
					for(int j = 0; j < m.numCols(); j++)
						m.set(i, j, Math.log(m.get(i,j)));
				return MatrixView.matrixToString(m, logic);
			}
			else return String.format("%f", Math.log(Double.parseDouble(arg)));
		}
		else if(func.equals("sin^-1"))
		{
			if(arg.startsWith("[["))
			{
				SimpleMatrix m = parseMatrix(arg);
				for(int i = 0; i < m.numRows(); i++)
					for(int j = 0; j < m.numCols(); j++)
						m.set(i, j, Math.asin(m.get(i,j)));
				return MatrixView.matrixToString(m, logic);
			}
			else return String.format("%f", Math.asin(Double.parseDouble(arg)));
		}
		else if(func.equals("cos^-1"))
		{
			if(arg.startsWith("[["))
			{
				SimpleMatrix m = parseMatrix(arg);
				for(int i = 0; i < m.numRows(); i++)
					for(int j = 0; j < m.numCols(); j++)
						m.set(i, j, Math.acos(m.get(i,j)));
				return MatrixView.matrixToString(m, logic);
			}
			else return String.format("%f", Math.acos(Double.parseDouble(arg)));
		}
		else if(func.equals("tan^-1"))
		{
			if(arg.startsWith("[["))
			{
				SimpleMatrix m = parseMatrix(arg);
				for(int i = 0; i < m.numRows(); i++)
					for(int j = 0; j < m.numCols(); j++)
						m.set(i, j, Math.atan(m.get(i,j)));
				return MatrixView.matrixToString(m, logic);
			}
			else return String.format("%f", Math.atan(Double.parseDouble(arg)));
		}
		else throw new SyntaxException();
	}
	
	private  Object applyPow(Object l, Object r) throws SyntaxException
	{
		if(l instanceof SimpleMatrix && r instanceof SimpleMatrix) throw new SyntaxException();
        else if(l instanceof SimpleMatrix)
        {
            SimpleMatrix a = (SimpleMatrix)l;
            int m = a.numRows();
            int n = a.numCols();
            if(m != n) throw new SyntaxException();
            double b = (Double)r;
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
                return matrix;
            }
            else
            {
            	long equiv = Math.round(b);
                for(long e = 1; e < equiv; e++)
                    a = a.mult(a);
                
                return a;
            }
        }
        else if(r instanceof SimpleMatrix)
        {
        	SimpleMatrix a = (SimpleMatrix)r;
            int m = a.numRows();
            int n = a.numCols();
            if(m != n) throw new SyntaxException();
            double b = (Double)l;
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
                return (Object)matrix;
            }
            else
            {
            	long equiv = Math.round(b);
                for(long e = 1; e < equiv; e++)
                    a = a.mult(a);
                
                return a;
            }
        }
        else
        {
            double a = (Double)l;
            double b = (Double)r;
            return Math.pow(a, b);
        }
	}

	private Object applyMult(Object l, Object r) throws SyntaxException
	{
		if(l instanceof SimpleMatrix && r instanceof SimpleMatrix)
		{
			SimpleMatrix a = (SimpleMatrix)l;
			SimpleMatrix b = (SimpleMatrix)r;
			return a.mult(b);
		}
		else if(l instanceof SimpleMatrix)
		{
			SimpleMatrix a = (SimpleMatrix)l;
			double b = (Double)r;
			return a.scale(b);
		}
		else if(r instanceof SimpleMatrix)
		{
			SimpleMatrix a = (SimpleMatrix)r;
			double b = (Double)l;
			return a.scale(b);
		}
		else
		{
			double a = (Double)l;
			double b = (Double)r;
			return a*b;
		}
	}
	
	private Object applyDiv(Object l, Object r) throws SyntaxException 
	{
		if(l instanceof SimpleMatrix && r instanceof SimpleMatrix)
        {
            SimpleMatrix a = (SimpleMatrix)l;
            SimpleMatrix b = (SimpleMatrix)r;
            return a.mult(b.pseudoInverse());
        }
        else if(l instanceof SimpleMatrix)
        {
            SimpleMatrix a = (SimpleMatrix)l;
            double b = (Double)r;
            return a.scale(1.0/b);
        }
        else if(r instanceof SimpleMatrix)
        {
            SimpleMatrix a = (SimpleMatrix)r;
            double b = (Double)l;
            return a.scale(1.0/b);
        }
        else
        {
            double a = (Double)l;
            double b = (Double)r;
            return a/b;
        }
	}
	
	private Object applyPlus(Object l, Object r) throws SyntaxException 
	{
		if(l instanceof SimpleMatrix && r instanceof SimpleMatrix)
        {
            SimpleMatrix a = (SimpleMatrix)l;
            SimpleMatrix b = (SimpleMatrix)r;
            return a.plus(b);
        }
        else if(l instanceof SimpleMatrix)
        {
            SimpleMatrix a = (SimpleMatrix)l;
            double b = (Double)r;
            return addScalar(a,b);
        }
        else if(r instanceof SimpleMatrix)
        {
            SimpleMatrix a = (SimpleMatrix)r;
            double b = (Double)l;
            return addScalar(a,b);
        }
        else
        {
            double a = (Double)l;
            double b = (Double)r;
            return a+b;
        }
	}

	private  Object applySub(Object l, Object r) throws SyntaxException
	{
		if(l instanceof SimpleMatrix && r instanceof SimpleMatrix)
        {
            SimpleMatrix a = (SimpleMatrix)l;
            SimpleMatrix b = (SimpleMatrix)r;
            return a.minus(b);
        }
        else if(l instanceof SimpleMatrix)
        {
            SimpleMatrix a = (SimpleMatrix)l;
            double b = (Double)r;
            return addScalar(a,-b);
        }
        else if(r instanceof SimpleMatrix)
        {
            SimpleMatrix a = (SimpleMatrix)r;
            double b = (Double)l;
            return addScalar(a,-b);
        }
        else
        {
            double a = (Double)l;
            double b = (Double)r;
            return a-b;
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
				temp.set(i, j, Double.parseDouble(cols[j].replace('\u2212', '-')));
		}
		
		return temp;
	}
}
