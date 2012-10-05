/*
 * jChemistry
 * Copyright (C) 2011 Philippe T. Pinard
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.android.calculator2;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * Utilities for the Apache common math library.
 * 
 * @author Philippe T. Pinard
 */
public final class CommonMathUtils {

    /**
     * Converts a <code>Vector3D</code> to a <code>RealVector</code>.
     * 
     * @param v
     *            a vector 3D
     * @return a real vector
     */
    public static RealVector toRealVector(Vector3D v) {
        return new ArrayRealVector(
                new double[] { v.getX(), v.getY(), v.getZ() });
    }



    /**
     * Converts a <code>RealVector</code> to a <code>Vector3D</code>.
     * 
     * @param v
     *            a real vector
     * @return a vector 3D
     */
    public static Vector3D toVector3D(RealVector v) throws IllegalArgumentException{
        if (v.getDimension() != 3)
            throw new IllegalArgumentException("The vector length ("
                    + v.getDimension() + ") must be equal to 3.");

        return new Vector3D(v.getEntry(0), v.getEntry(1), v.getEntry(2));
    }
    
    /**
     * Converts a <code>RealVector</code> to a <code>RealMatrix</code>.
     * 
     * @param v
     *            a real vector
     * @return a real matrix
     */
    public static RealMatrix toRealMatrix(RealVector v) {
        double[] values = new double[v.getDimension()];
        for(int i=0;i<v.getDimension();i++) {
            values[i] = v.getEntry(i);
        }
        return new Array2DRowRealMatrix(values);
    }
    
    /**
     * Converts a <code>Vector3D</code> to a <code>RealMatrix</code>.
     * 
     * @param v
     *            a Vector3D
     * @return a RealMatrix
     */
    public static RealMatrix toRealMatrix(Vector3D v) {
        double[] values = { v.getX(), v.getY(), v.getZ() };
        return new Array2DRowRealMatrix(values);
    }



    /**
     * Multiplies the specified matrix by the specified COLUMN vector.
     * <p/>
     * The <code>RealMatrix</code> interface only allows multiplication of
     * matrix by ROW vector.
     * 
     * @param m
     *            matrix
     * @param v
     *            vector
     * @return resultant COLUMN vector
     */
    public static RealVector postMultiply(RealMatrix m, RealVector v) {
        return m.transpose().preMultiply(v);
    }



    /**
     * Constructor to prevent the class to be instantiated.
     */
    private CommonMathUtils() {
    }
}
