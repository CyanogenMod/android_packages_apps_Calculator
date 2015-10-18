package com.xlythe.math;

/**
 * A super class for BaseModule, GraphModule, MatrixModule
 */
public class Module {
    // Used whenever math is necessary
    private final Solver mSolver;

    // Used for formatting Dec, Bin, and Hex.
    // Dec looks like 1,234,567. Bin is 1010 1010. Hex is 0F 1F 2F.
    private final int mDecSeparatorDistance = 3;
    private final int mBinSeparatorDistance = 4;
    private final int mHexSeparatorDistance = 2;

    Module(Solver solver) {
        mSolver = solver;
    }

    public int getDecSeparatorDistance() {
        return mDecSeparatorDistance;
    }

    public int getBinSeparatorDistance() {
        return mBinSeparatorDistance;
    }

    public int getHexSeparatorDistance() {
        return mHexSeparatorDistance;
    }

    public char getDecimalPoint() {
        return Constants.DECIMAL_POINT;
    }

    public char getDecSeparator() {
        return Constants.DECIMAL_SEPARATOR;
    }

    public char getBinSeparator() {
        return Constants.BINARY_SEPARATOR;
    }

    public char getHexSeparator() {
        return Constants.HEXADECIMAL_SEPARATOR;
    }

    public char getMatrixSeparator() {
        return Constants.MATRIX_SEPARATOR;
    }

    public Solver getSolver() {
        return mSolver;
    }
}
