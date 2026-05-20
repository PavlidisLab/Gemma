/*
 * The baseCode project
 *
 * Copyright (c) 2007-2019 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.util.matrix;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.Format;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;

/**
 * Class for writing matrices to disk
 *
 * @author Raymond Lim
 * @author paul
 */
public class MatrixWriter<R, C> {

    public static final String DEFAULT_SEP = "\t";
    public static final String DEFAULT_TOP_LEFT = "ID";
    protected Map<C, String> colNameMap = new HashMap<C, String>();
    protected Format formatter;
    protected Writer out;

    protected Map<R, String> rowNameMap = new HashMap<R, String>();

    protected String sep;
    protected Map<?, String> sliceNameMap;
    protected String topLeft;

    public MatrixWriter(OutputStream out) {
        this(out, null, DEFAULT_SEP);
    }

    public MatrixWriter(OutputStream out, Format formatter) {
        this(out, formatter, DEFAULT_SEP);
    }

    public MatrixWriter(OutputStream out, Format formatter, String sep) {
        this(new BufferedWriter(new OutputStreamWriter(out)), formatter, sep);
    }

    public MatrixWriter(String fileName, Format formatter) throws IOException {
        this(fileName, formatter, DEFAULT_SEP);
    }

    @SuppressWarnings("resource")
    public MatrixWriter(String fileName, Format formatter, String sep) throws IOException {
        this(new BufferedWriter(new FileWriter(fileName)), formatter, sep);
    }

    public MatrixWriter(Writer out) {
        this(out, null, DEFAULT_SEP);
    }

    public MatrixWriter(Writer out, Format formatter) {
        this(out, formatter, DEFAULT_SEP);
    }

    public MatrixWriter(Writer out, Format formatter, String sep) {
        this.out = out;
        this.formatter = formatter;
        this.sep = sep;
        this.topLeft = DEFAULT_TOP_LEFT;
    }

    public MatrixWriter(Writer out, String sep) {
        this(out, null, sep);
    }

    /**
     * Use to customize the labels instead of relying on the toString method of the column object
     *
     * @param colNameMap
     */
    public void setColNameMap(Map<C, String> colNameMap) {
        this.colNameMap = colNameMap;
    }

    /**
     * Use to customize the labels instead of relying on the toString method of the row object
     *
     * @param rowNameMap
     */
    public void setRowNameMap(Map<R, String> rowNameMap) {
        this.rowNameMap = rowNameMap;
    }

    public void setSep(String sep) {
        this.sep = sep;
    }

    public void setSliceNameMap(Map<?, String> sliceNameMap) {
        this.sliceNameMap = sliceNameMap;
    }

    public void setTopLeft(String topLeft) {
        this.topLeft = topLeft;
    }

    /**
     * @param <V>
     * @param matrix
     * @param printNames Should the row and column names be included; FIXME this fails if names aren't provided
     * @throws IOException
     */
    public <V> void writeMatrix(Matrix2D<R, C, V> matrix, boolean printNames) throws IOException {
        // write headers
        StringBuffer buf = new StringBuffer(topLeft);
        if (printNames) {
            for (Iterator<C> it = matrix.getColNames().iterator(); it.hasNext(); ) {
                Object colName = it.next();
                buf.append(sep);
                if (this.colNameMap.containsKey(colName)) {
                    buf.append(colNameMap.get(colName));
                } else {
                    buf.append(colName);
                }
            }
            buf.append("\n");
            out.write(buf.toString());
        }

        for (Iterator<R> rowIt = matrix.getRowNames().iterator(); rowIt.hasNext(); ) {
            R rowName = rowIt.next();
            int rowIndex = matrix.getRowIndexByName(rowName);
            buf = new StringBuffer();
            if (printNames) {
                if (this.rowNameMap.containsKey(rowName)) {
                    buf.append(rowNameMap.get(rowName) + sep);
                } else {
                    buf.append(rowName + sep);
                }
            }
            for (Iterator<C> colIt = matrix.getColNames().iterator(); colIt.hasNext(); ) {
                C colName = colIt.next();
                int colIndex = matrix.getColIndexByName(colName);
                Object val = MatrixUtil.getObject(matrix, rowIndex, colIndex);

                if (val != null) {
                    String s = val.toString();
                    if (formatter != null) s = formatter.format(val);
                    buf.append(s);
                } else {
                    buf.append(""); // just to make explicit ...
                }
                if (colIt.hasNext()) buf.append(sep);
            }
            buf.append("\n");
            out.write(buf.toString());
        }
        out.flush();
        out.close();
    }

    /**
     * Write a bare matrix without row names or columns
     * @param coltMatrix
     * @throws IOException
     */
    public void writeMatrix(DoubleMatrix2D coltMatrix) throws IOException {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < coltMatrix.rows(); i++) {
            for (int j = 0; j < coltMatrix.columns(); j++) {
                if (j > 0) buf.append("\t");
                buf.append(coltMatrix.get(i, j));
            }
            buf.append("\n");
        }
        out.write(buf.toString());
        out.close();
    }


    /**
     * Write a bare 1D matrix (one value per line)
     * @param coltMatrix
     * @throws IOException
     */
    public void writeMatrix(DoubleMatrix1D coltMatrix) throws IOException {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < coltMatrix.size(); i++) {
                buf.append(coltMatrix.get(i));
                buf.append("\n");
        }
        out.write(buf.toString());
        out.close();
    }

    // NOTE: writeMatrix(Matrix3D) overload dropped during port; no Gemma consumer uses Matrix3D.

}
