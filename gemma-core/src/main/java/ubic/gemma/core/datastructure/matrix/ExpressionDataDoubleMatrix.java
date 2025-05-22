/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.core.datastructure.matrix;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.util.*;

/**
 * A data structure that holds a reference to the data for a given expression experiment. The data can be queried by row
 * or column, returning data for a specific DesignElement or data for a specific BioAssay. This class is not database
 * aware so the vectors provided must already be 'thawed'.
 *
 * @author pavlidis
 * @author keshav
 */
public class ExpressionDataDoubleMatrix extends AbstractMultiAssayExpressionDataMatrix<Double> implements BulkExpressionDataPrimitiveDoubleMatrix {

    private static final Log log = LogFactory.getLog( ExpressionDataDoubleMatrix.class.getName() );

    private DoubleMatrix<CompositeSequence, BioMaterial> matrix;
    private Map<CompositeSequence, Double> ranks = new HashMap<>();

    /**
     * To comply with bean specifications. Not to be instantiated.
     */
    public ExpressionDataDoubleMatrix() {
    }

    public ExpressionDataDoubleMatrix( Collection<? extends BulkExpressionDataVector> vectors ) {
        this.init();

        for ( BulkExpressionDataVector dedv : vectors ) {
            if ( !dedv.getQuantitationType().getRepresentation().equals( PrimitiveType.DOUBLE ) ) {
                throw new IllegalStateException(
                        "Cannot convert non-double quantitation types into double matrix:" + dedv
                                .getQuantitationType() );
            }
        }
        this.selectVectors( vectors );
        this.vectorsToMatrix( vectors );
    }

    public ExpressionDataDoubleMatrix( ExpressionExperiment ee, Collection<? extends BulkExpressionDataVector> vectors ) {
        this.init();
        for ( BulkExpressionDataVector dedv : vectors ) {
            if ( !dedv.getQuantitationType().getRepresentation().equals( PrimitiveType.DOUBLE ) ) {
                throw new IllegalStateException(
                        "Cannot convert non-double quantitation types into double matrix:" + dedv
                                .getQuantitationType() );
            }
        }
        this.expressionExperiment = ee;
        this.selectVectors( vectors );
        this.vectorsToMatrix( vectors );
    }

    public ExpressionDataDoubleMatrix( Collection<? extends BulkExpressionDataVector> dataVectors,
            Collection<QuantitationType> quantitationTypes ) {
        this.init();
        for ( QuantitationType qt : quantitationTypes ) {
            if ( !qt.getRepresentation().equals( PrimitiveType.DOUBLE ) ) {
                throw new IllegalStateException(
                        "Cannot convert non-double quantitation types into double matrix: " + qt );
            }
        }
        Collection<BulkExpressionDataVector> selectedVectors = this.selectVectors( dataVectors, quantitationTypes );
        this.vectorsToMatrix( selectedVectors );
    }

    public ExpressionDataDoubleMatrix( Collection<? extends BulkExpressionDataVector> dataVectors,
            QuantitationType quantitationType ) {
        this.init();
        if ( !quantitationType.getRepresentation().equals( PrimitiveType.DOUBLE ) ) {
            throw new IllegalStateException(
                    "Cannot convert non-double quantitation types into double matrix: " + quantitationType );
        }
        Collection<BulkExpressionDataVector> selectedVectors = this.selectVectors( dataVectors, quantitationType );
        this.vectorsToMatrix( selectedVectors );
    }

    /**
     * Create a data matrix like sourceMatrix but use the values and quantitations from dataMatrix.
     * <p>
     * Note: The rows can be different from the original matrix, but the columns must be the same.
     *
     * @param sourceMatrix      source matrix from which most of the meta-data will be imported
     * @param dataMatrix        data matrix to use
     * @param quantitationTypes quantitation types used by the dataMatrix
     */
    public ExpressionDataDoubleMatrix( ExpressionDataDoubleMatrix sourceMatrix,
            DoubleMatrix<CompositeSequence, BioMaterial> dataMatrix, Collection<QuantitationType> quantitationTypes ) {
        this.init();
        this.expressionExperiment = sourceMatrix.expressionExperiment;
        this.bioAssayDimensions = sourceMatrix.bioAssayDimensions;
        this.columnAssayMap = sourceMatrix.columnAssayMap;
        this.columnBioAssayMapByInteger = sourceMatrix.columnBioAssayMapByInteger;
        this.columnBioMaterialMap = sourceMatrix.columnBioMaterialMap;
        this.columnBioMaterialMapByInteger = sourceMatrix.columnBioMaterialMapByInteger;
        this.quantitationTypes = quantitationTypes;
        this.matrix = dataMatrix;

        for ( int i = 0; i < dataMatrix.rows(); i++ ) {
            this.addToRowMaps( i, dataMatrix.getRowName( i ) );
        }

    }

    public ExpressionDataDoubleMatrix( ExpressionDataDoubleMatrix dmatrix, DoubleMatrix<CompositeSequence, BioMaterial> copiedMatrix ) {
        this( dmatrix, copiedMatrix, dmatrix.getQuantitationTypes() );
    }

    /**
     * Create a matrix based on another one's selected rows.
     *
     * @param rowsToUse rows
     * @param sourceMatrix matrix
     */
    public ExpressionDataDoubleMatrix( ExpressionDataDoubleMatrix sourceMatrix, List<CompositeSequence> rowsToUse ) {
        this.init();
        this.expressionExperiment = sourceMatrix.expressionExperiment;
        this.bioAssayDimensions = sourceMatrix.bioAssayDimensions;
        this.columnAssayMap = sourceMatrix.columnAssayMap;
        this.columnBioAssayMapByInteger = sourceMatrix.columnBioAssayMapByInteger;
        this.columnBioMaterialMap = sourceMatrix.columnBioMaterialMap;
        this.columnBioMaterialMapByInteger = sourceMatrix.columnBioMaterialMapByInteger;
        this.quantitationTypes = sourceMatrix.getQuantitationTypes();
        this.matrix = new DenseDoubleMatrix<>( rowsToUse.size(), sourceMatrix.columns() );
        this.matrix.setColumnNames( sourceMatrix.getMatrix().getColNames() );

        ExpressionDataDoubleMatrix.log
                .debug( "Creating a filtered matrix " + rowsToUse.size() + " x " + sourceMatrix.columns() );

        int i = 0;
        for ( CompositeSequence element : rowsToUse ) {
            super.addToRowMaps( i, element );
            double[] rowVals = sourceMatrix.getRowAsDoubles( element );
            assert rowVals != null : "Source matrix does not have row for " + element;

            this.matrix.addRowName( element );

            for ( int j = 0; j < rowVals.length; j++ ) {
                Double val = rowVals[j];
                this.set( i, j, val );
            }
            i++;
        }
    }

    /**
     * Create a matrix given a 'raw' matrix that uses the same samples as the experiment. Only simple situations are
     * supported (one platform, not subsetting the dataset).
     *
     * @param ee to be associated with this
     * @param qt to be associated with this
     * @param matrix with valid row and column elements, and the data
     */
    public ExpressionDataDoubleMatrix( ExpressionExperiment ee, QuantitationType qt,
            DoubleMatrix<CompositeSequence, BioMaterial> matrix ) {
        Assert.notNull( ee, "Experiment cannot be null" );

        if ( matrix.rows() == 0 || matrix.columns() == 0 || matrix.getRowNames().isEmpty() || matrix.getColNames()
                .isEmpty() ) {
            throw new IllegalArgumentException( "Matrix is invalid" );
        }

        this.init();
        this.expressionExperiment = ee;
        this.matrix = matrix;
        this.quantitationTypes.add( qt );

        List<BioAssay> bioassays = new ArrayList<>( matrix.columns() );
        for ( BioMaterial bm : matrix.getColNames() ) {
            Collection<BioAssay> bioAssaysUsedIn = bm.getBioAssaysUsedIn();
            if ( bioAssaysUsedIn.size() > 1 ) {
                throw new UnsupportedOperationException(
                        "Can't make new data from matrix that has multiple bioassays per biomaterial" );
            }
            BioAssay bioAssay = bioAssaysUsedIn.iterator().next();
            if ( !CollectionUtils.containsAny( ee.getBioAssays(), bioAssay.getSampleUsed().getAllBioAssaysUsedIn() ) ) {
                throw new IllegalArgumentException( "Bioassays in the matrix must match those in the experiment" );
            }
            bioassays.add( bioAssay );
        }

        BioAssayDimension dim = BioAssayDimension.Factory.newInstance( bioassays );

        assert !matrix.getRowNames().isEmpty();
        int i = 0;
        for ( CompositeSequence cs : matrix.getRowNames() ) {
            bioAssayDimensions.put( cs, dim );
            this.addToRowMaps( i, cs );
            i++;
        }

        assert !bioAssayDimensions.isEmpty();

        this.setUpColumnElements();

    }

    /**
     * Create a matrix based on another one's selected columns. The results will be somewhat butchered - only a single
     * BioAssayDimension and the ranks will be copied over (not recomputed based on the selected columns).
     *
     * @param columnsToUse columns
     * @param sourceMatrix matrix
     * @param reorderedDim the reordered bioAssayDimension.
     */
    public ExpressionDataDoubleMatrix( ExpressionDataDoubleMatrix sourceMatrix, List<BioMaterial> columnsToUse,
            BioAssayDimension reorderedDim ) {
        this.init();
        this.expressionExperiment = sourceMatrix.expressionExperiment;

        this.matrix = new DenseDoubleMatrix<>( sourceMatrix.rows(), columnsToUse.size() );
        this.matrix.setRowNames( sourceMatrix.getMatrix().getRowNames() );
        this.matrix.setColumnNames( columnsToUse );

        this.ranks = sourceMatrix.ranks; // not strictly correct if we are using subcolumns

        this.getQuantitationTypes().addAll( sourceMatrix.getQuantitationTypes() );

        List<Integer> originalBioMaterialIndices = new ArrayList<>();
        for ( BioMaterial bm : columnsToUse ) {
            originalBioMaterialIndices.add( sourceMatrix.getColumnIndex( bm ) );
        }

        this.bioAssayDimensions.clear();

        int i = 0;
        for ( ExpressionDataMatrixRowElement element : sourceMatrix.getRowElements() ) {
            CompositeSequence designElement = element.getDesignElement();
            super.addToRowMaps( i, designElement );

            double[] sourceRow = sourceMatrix.getRowAsDoubles( designElement );

            assert sourceRow != null : "Source matrix does not have row for " + designElement;
            bioAssayDimensions.put( designElement, reorderedDim );

            for ( int j = 0; j < originalBioMaterialIndices.size(); j++ ) {
                Double val = sourceRow[originalBioMaterialIndices.get( j )];
                this.set( i, j, val );
            }
            i++;
        }

        super.setUpColumnElements();
    }

    @Override
    public int columns() {
        return matrix.columns();
    }

    @Override
    public Double get( int row, int column ) {
        return matrix.get( row, column );
    }

    @Override
    public double getAsDouble( int row, int column ) {
        return matrix.get( row, column );
    }

    @Override
    public double[] getColumnAsDoubles( BioAssay bioAssay ) {
        int j = getColumnIndex( bioAssay );
        if ( j == -1 ) {
            return null;
        }
        return getColumnAsDoubles( j );
    }

    @Override
    public Double[] getColumn( int index ) {
        // FIXME: DoubleMatrix.getColObj is not efficient
        return ArrayUtils.toObject( matrix.getColumn( index ) );
    }

    @Override
    public double[] getColumnAsDoubles( int index ) {
        return this.matrix.getColumn( index );
    }

    @Override
    public Double[][] getRawMatrix() {
        Double[][] dMatrix = new Double[matrix.rows()][matrix.columns()];
        for ( int i = 0; i < matrix.rows(); i++ ) {
            // FIXME: getRowObj() is not efficient
            dMatrix[i] = ArrayUtils.toObject( matrix.getRow( i ) );
        }
        return dMatrix;
    }

    /**
     * Obtain the raw matrix without boxing.
     * @see #getRawMatrix()
     */
    public double[][] getRawMatrixAsDoubles() {
        return matrix.getRawMatrix();
    }

    @Override
    public double[] getRowAsDoubles( CompositeSequence designElement ) {
        int row = getRowIndex( designElement );
        if ( row == -1 ) {
            return null;
        }
        return this.getRowAsDoubles( row );
    }

    @Override
    public Double[] getRow( int index ) {
        // FIXME: DoubleMatrix.getRowObj is not efficient
        return ArrayUtils.toObject( matrix.getRow( index ) );
    }

    @Override
    public double[] getRowAsDoubles( int index ) {
        return matrix.getRow( index );
    }

    @Override
    public boolean hasMissingValues() {
        int rows = matrix.rows();
        int cols = matrix.columns();
        for ( int i = 0; i < rows; i++ ) {
            for ( int j = 0; j < cols; j++ ) {
                if ( Double.isNaN( matrix.get( i, j ) ) )
                    return true;
            }
        }
        return false;
    }

    @Override
    public int rows() {
        return matrix.rows();
    }

    /**
     * Modifying the matrix directly is not recommended, make a copy instead.
     */
    @Deprecated
    public void set( int row, int column, @Nullable Double value ) {
        if ( value == null ) {
            matrix.set( row, column, Double.NaN );
        } else {
            matrix.set( row, column, value );
        }
    }

    /**
     * @return Convert this to a collection of vectors.
     */
    public Collection<ProcessedExpressionDataVector> toProcessedDataVectors() {
        Collection<ProcessedExpressionDataVector> result = new HashSet<>();
        QuantitationType qt = this.getQuantitationTypes().iterator().next();
        if ( this.getQuantitationTypes().size() > 1 ) {
            throw new UnsupportedOperationException( "Cannot convert matrix that has more than one quantitation type" );
        }
        BioAssayDimension bad = this.getBioAssayDimension();
        for ( int i = 0; i < this.rows(); i++ ) {
            ProcessedExpressionDataVector v = ProcessedExpressionDataVector.Factory.newInstance();
            v.setBioAssayDimension( bad );
            v.setDesignElement( this.getRowNames().get( i ) );
            v.setQuantitationType( qt );
            v.setDataAsDoubles( getRowAsDoubles( i ) );
            v.setExpressionExperiment( this.expressionExperiment );
            // we don't fill in the ranks because we only have the mean value here.
            result.add( v );
        }
        return result;
    }

    /**
     * Same as toProcessedDataVectors but uses RawExpressionDataVector
     *
     * @return Convert this to a collection of vectors.
     */
    public Collection<RawExpressionDataVector> toRawDataVectors() {
        Collection<RawExpressionDataVector> result = new HashSet<>();
        QuantitationType qt = this.getQuantitationTypes().iterator().next();

        if ( this.getQuantitationTypes().size() > 1 ) {
            throw new UnsupportedOperationException( "Cannot convert matrix that has more than one quantitation type" );
        }

        BioAssayDimension bad = this.getBioAssayDimension();
        for ( int i = 0; i < this.rows(); i++ ) {
            RawExpressionDataVector v = RawExpressionDataVector.Factory.newInstance();
            v.setBioAssayDimension( bad );
            v.setDesignElement( this.getRowNames().get( i ) );
            v.setQuantitationType( qt );
            v.setDataAsDoubles( getRowAsDoubles( i ) );
            v.setExpressionExperiment( this.expressionExperiment );
            // we don't fill in the ranks because we only have the mean value here.
            result.add( v );
        }

        assert result.size() == this.rows();

        return result;
    }

    public DoubleMatrix<CompositeSequence, BioMaterial> getMatrix() {
        return matrix;
    }

    /**
     * @return The expression level ranks (based on mean signal intensity in the vectors); this will be empty if the
     *         vectors used to construct the matrix were not ProcessedExpressionDataVectors.
     */
    public Map<CompositeSequence, Double> getRanks() {
        return this.ranks;
    }

    public List<CompositeSequence> getRowNames() {
        return this.getMatrix().getRowNames();
    }

    public void set( CompositeSequence designElement, BioAssay bioAssay, Double value ) {
        int row = this.getRowIndex( designElement );
        int column = this.getColumnIndex( bioAssay );
        matrix.set( row, column, value );
    }

    @Override
    protected String format( int row, int column ) {
        return format( matrix.get( row, column ) );
    }

    /**
     * Populate this matrix from a given collection of {@link BulkExpressionDataVector}s.
     */
    @Override
    protected void vectorsToMatrix( Collection<? extends BulkExpressionDataVector> vectors ) {
        if ( vectors.isEmpty() ) {
            throw new IllegalArgumentException( "No vectors!" );
        }

        for ( BulkExpressionDataVector vector : vectors ) {
            if ( vector instanceof ProcessedExpressionDataVector ) {
                this.ranks
                        .put( vector.getDesignElement(), ( ( ProcessedExpressionDataVector ) vector ).getRankByMean() );
            }
        }

        int maxSize = this.setUpColumnElements();
        this.matrix = this.createMatrix( vectors, maxSize );

    }

    /**
     * Fill in the data
     *
     * @return DoubleMatrixNamed
     */
    private DoubleMatrix<CompositeSequence, BioMaterial> createMatrix(
            Collection<? extends BulkExpressionDataVector> vectors, int maxSize ) {

        int numRows = this.rowDesignElementMapByInteger.size();

        DoubleMatrix<CompositeSequence, BioMaterial> mat = new DenseDoubleMatrix<>( numRows, maxSize );

        for ( int j = 0; j < mat.columns(); j++ ) {
            mat.addColumnName( this.columnBioMaterialMapByInteger.get( j ) );
        }

        // initialize the matrix to -Infinity; this marks values that are not yet initialized.
        for ( int i = 0; i < mat.rows(); i++ ) {
            for ( int j = 0; j < mat.columns(); j++ ) {
                mat.set( i, j, Double.NEGATIVE_INFINITY );
            }
        }

        Map<Integer, CompositeSequence> rowNames = new TreeMap<>();
        for ( BulkExpressionDataVector vector : vectors ) {
            BioAssayDimension dimension = vector.getBioAssayDimension();

            CompositeSequence designElement = vector.getDesignElement();
            assert designElement != null : "No design element for " + vector;

            Integer rowIndex = this.rowElementMap.get( designElement );
            assert rowIndex != null;

            rowNames.put( rowIndex, designElement );

            double[] vals = vector.getDataAsDoubles();

            Collection<BioAssay> bioAssays = dimension.getBioAssays();
            if ( bioAssays.size() != vals.length )
                throw new IllegalStateException(
                        "Mismatch: " + vals.length + " values in vector ( " + vector.getData().length + " bytes) for "
                                + designElement + " got " + bioAssays.size() + " bioassays in the bioAssayDimension" );

            Iterator<BioAssay> it = bioAssays.iterator();

            this.setMatBioAssayValues( mat, rowIndex, ArrayUtils.toObject( vals ), bioAssays, it );
        }

        /*
         * Note: these row names aren't that important unless we use the bare matrix.
         */
        for ( int i = 0; i < mat.rows(); i++ ) {
            mat.addRowName( rowNames.get( i ) );
        }
        assert mat.getRowNames().size() == mat.rows();

        // fill in remaining missing values.
        for ( int i = 0; i < mat.rows(); i++ ) {
            for ( int j = 0; j < mat.columns(); j++ ) {
                if ( mat.get( i, j ) == Double.NEGATIVE_INFINITY ) {
                    // log.debug( "Missing value at " + i + " " + j );
                    mat.set( i, j, Double.NaN );
                }
            }
        }
        ExpressionDataDoubleMatrix.log.debug( "Created a " + mat.rows() + " x " + mat.columns() + " matrix" );
        return mat;
    }

}
