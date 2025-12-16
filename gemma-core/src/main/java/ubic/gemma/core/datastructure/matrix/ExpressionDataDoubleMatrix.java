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

import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.Rank;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * A data structure that holds a reference to the data for a given expression experiment. The data can be queried by row
 * or column, returning data for a specific DesignElement or data for a specific BioAssay. This class is not database
 * aware so the vectors provided must already be 'thawed'.
 *
 * @author pavlidis
 * @author keshav
 */
public class ExpressionDataDoubleMatrix extends AbstractMultiAssayExpressionDataMatrix<Double> implements BulkExpressionDataPrimitiveDoubleMatrix, SingleCellDerivedBulkExpressionDataMatrix<Double> {

    private static final Log log = LogFactory.getLog( ExpressionDataDoubleMatrix.class.getName() );

    private final double[][] matrix;

    /**
     * Indicate if {@link #matrix} contains missing values.
     */
    private final boolean hasMissingValues;

    /**
     * Indicate how many cells were used to compute each value in the matrix.
     * <p>
     * May be null if this information is not available.
     */
    @Nullable
    private final int[][] numberOfCells;

    /**
     * Indicate the rank (by mean signal intensity) of each row in the matrix.
     */
    @Nullable
    private double[] ranks = null;

    public ExpressionDataDoubleMatrix( @Nullable ExpressionExperiment ee, Collection<? extends BulkExpressionDataVector> vectors ) {
        super( ee );
        List<BulkExpressionDataVector> selectedVectors = selectVectors( vectors );
        if ( selectedVectors.isEmpty() ) {
            throw new IllegalArgumentException( "No vectors!" );
        }
        this.matrix = this.createMatrix( selectedVectors );
        this.hasMissingValues = computeHasMissingValues( this.matrix );
        this.numberOfCells = getNumberOfCellsFromVectors( selectedVectors );
    }

    public ExpressionDataDoubleMatrix( ExpressionExperiment ee, Collection<? extends BulkExpressionDataVector> dataVectors,
            Collection<QuantitationType> quantitationTypes ) {
        super( ee );
        List<BulkExpressionDataVector> selectedVectors = selectVectors( dataVectors, quantitationTypes );
        if ( selectedVectors.isEmpty() ) {
            throw new IllegalArgumentException( "No vectors!" );
        }
        this.matrix = this.createMatrix( selectedVectors );
        this.hasMissingValues = computeHasMissingValues( this.matrix );
        this.numberOfCells = getNumberOfCellsFromVectors( selectedVectors );
    }

    public ExpressionDataDoubleMatrix( ExpressionExperiment ee, Collection<? extends BulkExpressionDataVector> dataVectors,
            QuantitationType quantitationType ) {
        super( ee );
        List<BulkExpressionDataVector> selectedVectors = selectVectors( dataVectors, quantitationType );
        if ( selectedVectors.isEmpty() ) {
            throw new IllegalArgumentException( "No vectors!" );
        }
        this.matrix = this.createMatrix( selectedVectors );
        this.hasMissingValues = computeHasMissingValues( this.matrix );
        this.numberOfCells = getNumberOfCellsFromVectors( selectedVectors );
    }

    /**
     * Create a matrix given a 'raw' matrix that uses the same samples as the experiment. Only simple situations are
     * supported (one platform, not subsetting the dataset).
     *
     * @param ee         to be associated with this
     * @param dataMatrix with valid row and column elements, and the data
     * @param qt         to be associated with this
     */
    public ExpressionDataDoubleMatrix( ExpressionExperiment ee, DoubleMatrix<CompositeSequence, BioMaterial> dataMatrix, QuantitationType qt ) {
        super( ee );
        Assert.notNull( ee, "Experiment cannot be null" );

        if ( dataMatrix.rows() == 0 || dataMatrix.columns() == 0 || dataMatrix.getRowNames().isEmpty() || dataMatrix.getColNames()
                .isEmpty() ) {
            throw new IllegalArgumentException( "Matrix is invalid" );
        }

        List<BioAssay> bioassays = new ArrayList<>( dataMatrix.columns() );
        for ( BioMaterial bm : dataMatrix.getColNames() ) {
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

        assert !dataMatrix.getRowNames().isEmpty();
        for ( int i = 0; i < dataMatrix.rows(); i++ ) {
            CompositeSequence cs = dataMatrix.getRowName( i );
            addToRowMaps( cs, qt, dim );
        }

        setUpColumnElements();

        this.matrix = dataMatrix.getRawMatrix();
        this.hasMissingValues = dataMatrix.hasMissingValues();
        this.numberOfCells = null;
    }

    /**
     * Create a data matrix like sourceMatrix but use the values and quantitations from dataMatrix.
     * <p>
     * Note: The rows can be different from the original matrix, but the columns must be the same.
     *
     * @param sourceMatrix source matrix from which most of the meta-data will be imported
     * @param dataMatrix   data matrix to use, it must be compatible with sourceMatrix in terms of rows and columns
     */
    private ExpressionDataDoubleMatrix( ExpressionDataDoubleMatrix sourceMatrix, double[][] dataMatrix ) {
        super( sourceMatrix );
        Assert.isTrue( dataMatrix.length == rows() && dataMatrix[0].length == columns(),
                "The new matrix has the same dimensions as the original matrix." );
        this.matrix = dataMatrix;
        this.hasMissingValues = computeHasMissingValues( dataMatrix );
        this.numberOfCells = sourceMatrix.numberOfCells;
    }

    /**
     * Create a data matrix like sourceMatrix but use the values and quantitations from dataMatrix.
     * <p>
     * Note: The rows can be different from the original matrix, but the columns must be the same.
     *
     * @param sourceMatrix      source matrix from which most of the meta-data will be imported
     * @param dataMatrix        data matrix to use, it must be compatible with sourceMatrix in terms of rows and columns
     * @param quantitationTypes quantitation type(s) used by dataMatrix
     */
    private ExpressionDataDoubleMatrix( ExpressionDataDoubleMatrix sourceMatrix,
            double[][] dataMatrix, Map<QuantitationType, QuantitationType> quantitationTypes ) {
        super( sourceMatrix.getExpressionExperiment() );
        Assert.isTrue( dataMatrix.length == sourceMatrix.rows() && dataMatrix[0].length == sourceMatrix.columns(),
                "The rows and columns of the new matrix correspond to the original matrix." );
        for ( int i = 0; i < sourceMatrix.rows(); i++ ) {
            CompositeSequence element = sourceMatrix.getDesignElementForRow( i );
            QuantitationType qt = sourceMatrix.getQuantitationType( element );
            if ( qt == null ) {
                throw new IllegalArgumentException( "The source matrix does not have a quantitation type for " + element + "." );
            }
            QuantitationType newQt = quantitationTypes.get( qt );
            if ( newQt == null ) {
                throw new IllegalArgumentException( "The provided quantitation types do not include a mapping for " + qt + "." );
            }
            BioAssayDimension dim = sourceMatrix.getBioAssayDimension( element );
            if ( dim == null ) {
                throw new IllegalArgumentException( "The source matrix does not have a dimension for " + element + "." );
            }
            addToRowMaps( element, newQt, dim );
        }

        setUpColumnElements();

        this.matrix = dataMatrix;
        this.hasMissingValues = computeHasMissingValues( dataMatrix );
        this.numberOfCells = sourceMatrix.numberOfCells;
    }

    /**
     * Create a matrix based on another one's selected rows.
     *
     * @param rowsToUse    rows
     * @param sourceMatrix matrix
     */
    private ExpressionDataDoubleMatrix( ExpressionDataDoubleMatrix sourceMatrix, List<CompositeSequence> rowsToUse ) {
        super( sourceMatrix.getExpressionExperiment() );
        this.matrix = new double[rowsToUse.size()][sourceMatrix.columns()];
        int[][] numberOfCells = null;
        for ( int i = 0; i < rowsToUse.size(); i++ ) {
            CompositeSequence element = rowsToUse.get( i );
            addToRowMaps( element, requireNonNull( sourceMatrix.getQuantitationType( element ) ),
                    requireNonNull( sourceMatrix.getBioAssayDimension( element ) ) );
            double[] rowVals = sourceMatrix.getRowAsDoubles( element );
            if ( rowVals == null ) {
                throw new IllegalArgumentException( "Source matrix does not have a row for " + element + "." );
            }

            System.arraycopy( rowVals, 0, this.matrix[i], 0, rowVals.length );
            int[] noc = sourceMatrix.getNumberOfCellsForRow( i );
            if ( noc != null ) {
                if ( numberOfCells == null ) {
                    numberOfCells = new int[rowsToUse.size()][];
                }
                numberOfCells[i] = noc;
            }
        }

        if ( rowsToUse.isEmpty() ) {
            // there is no dimensions to set up assays from
            LinkedHashMap<BioMaterial, Set<BioAssay>> bmm = new LinkedHashMap<>();
            for ( BioMaterial bm : sourceMatrix.getBioMaterials() ) {
                bmm.put( bm, new HashSet<>() );
            }
            setUpColumnElements( bmm );
        } else {
            setUpColumnElements();
        }

        this.hasMissingValues = sourceMatrix.hasMissingValues && computeHasMissingValues( this.matrix );
        this.numberOfCells = numberOfCells;
    }

    private ExpressionDataDoubleMatrix( ExpressionDataDoubleMatrix sourceMatrix, List<BioMaterial> columnsToUse, boolean dummy ) {
        super( sourceMatrix.getExpressionExperiment() );

        this.matrix = new double[sourceMatrix.rows()][columnsToUse.size()];
        for ( double[] row : matrix ) {
            Arrays.fill( row, Double.NaN );
        }

        int k = 0;
        int[] originalBioMaterialIndices = new int[columnsToUse.size()];
        for ( BioMaterial bm : columnsToUse ) {
            originalBioMaterialIndices[k++] = sourceMatrix.getColumnIndex( bm );
        }

        Map<BioAssayDimension, BioAssayDimension> dimMap = new HashMap<>();

        int[][] numberOfCells = null;
        for ( int i = 0; i < sourceMatrix.rows(); i++ ) {
            CompositeSequence designElement = sourceMatrix.getDesignElementForRow( i );
            double[] sourceRow = sourceMatrix.getRowAsDoubles( designElement );
            assert sourceRow != null : "Source matrix does not have row for " + designElement;
            int[] sourceNumberOfCells = sourceMatrix.getNumberOfCellsForRow( i );
            QuantitationType qt = sourceMatrix.getQuantitationType( designElement );
            if ( qt == null ) {
                throw new IllegalArgumentException( "Source matrix does not have a quantitation type for " + designElement + "." );
            }
            BioAssayDimension dim = sourceMatrix.getBioAssayDimension( designElement );
            if ( dim == null ) {
                throw new IllegalArgumentException( "Source matrix does not have a dimension for " + designElement + "." );
            }
            BioAssayDimension slicedDim = dimMap.computeIfAbsent( dim, ignored -> {
                List<BioAssay> selectedAssays = dim.getBioAssays().stream()
                        .filter( ba -> columnsToUse.contains( ba.getSampleUsed() ) )
                        .collect( Collectors.toList() );
                return BioAssayDimension.Factory.newInstance( selectedAssays );
            } );
            addToRowMaps( designElement, qt, slicedDim );
            for ( int j = 0; j < originalBioMaterialIndices.length; j++ ) {
                this.matrix[i][j] = sourceRow[originalBioMaterialIndices[j]];
                if ( sourceNumberOfCells != null ) {
                    if ( numberOfCells == null ) {
                        numberOfCells = new int[sourceMatrix.rows()][];
                    }
                    if ( numberOfCells[i] == null ) {
                        numberOfCells[i] = new int[columnsToUse.size()];
                    }
                    numberOfCells[i][j] = sourceNumberOfCells[originalBioMaterialIndices[j]];
                }
            }
        }

        setUpColumnElements();

        this.hasMissingValues = sourceMatrix.hasMissingValues && computeHasMissingValues( this.matrix );
        this.numberOfCells = numberOfCells;
    }

    /**
     * Create a matrix based on another one's selected columns. The results will be somewhat butchered - only a single
     * BioAssayDimension and the ranks will be copied over (not recomputed based on the selected columns).
     *
     * @param columnsToUse columns
     * @param sourceMatrix matrix
     * @param reorderedDim the reordered bioAssayDimension.
     */
    private ExpressionDataDoubleMatrix( ExpressionDataDoubleMatrix sourceMatrix, List<BioMaterial> columnsToUse,
            BioAssayDimension reorderedDim ) {
        super( sourceMatrix.getExpressionExperiment() );

        this.matrix = new double[sourceMatrix.rows()][columnsToUse.size()];

        int k = 0;
        int[] originalBioMaterialIndices = new int[columnsToUse.size()];
        for ( BioMaterial bm : columnsToUse ) {
            originalBioMaterialIndices[k++] = sourceMatrix.getColumnIndex( bm );
        }

        int[][] numberOfCells = null;
        for ( int i = 0; i < sourceMatrix.rows(); i++ ) {
            CompositeSequence designElement = sourceMatrix.getDesignElementForRow( i );
            double[] sourceRow = sourceMatrix.getRowAsDoubles( designElement );
            assert sourceRow != null : "Source matrix does not have row for " + designElement;
            int[] sourceNumberOfCells = sourceMatrix.getNumberOfCellsForRow( i );
            addToRowMaps( designElement, requireNonNull( sourceMatrix.getQuantitationType( designElement ) ), reorderedDim );
            for ( int j = 0; j < originalBioMaterialIndices.length; j++ ) {
                this.matrix[i][j] = sourceRow[originalBioMaterialIndices[j]];
                if ( sourceNumberOfCells != null ) {
                    if ( numberOfCells == null ) {
                        numberOfCells = new int[sourceMatrix.rows()][];
                    }
                    if ( numberOfCells[i] == null ) {
                        numberOfCells[i] = new int[columnsToUse.size()];
                    }
                    numberOfCells[i][j] = sourceNumberOfCells[originalBioMaterialIndices[j]];
                }
            }
        }

        setUpColumnElements();

        this.hasMissingValues = sourceMatrix.hasMissingValues && computeHasMissingValues( this.matrix );
        this.numberOfCells = numberOfCells;
    }

    public ExpressionDataDoubleMatrix( ExpressionDataDoubleMatrix sourceMatrix, @Nullable int[][] numberOfCells ) {
        super( sourceMatrix );
        Assert.isTrue( numberOfCells == null || ( numberOfCells.length == sourceMatrix.rows() && Arrays.stream( numberOfCells ).allMatch( row -> row.length == sourceMatrix.columns() ) ) );
        this.matrix = sourceMatrix.matrix;
        this.hasMissingValues = sourceMatrix.hasMissingValues;
        this.ranks = sourceMatrix.ranks;
        this.numberOfCells = numberOfCells;
    }

    @Nonnull
    @Override
    public Double get( int row, int column ) {
        return matrix[row][column];
    }

    @Override
    public double getAsDouble( int row, int column ) {
        return matrix[row][column];
    }

    public double getAsDouble( CompositeSequence designElement, BioMaterial bioMaterial ) {
        int row = getRowIndex( designElement );
        int column = getColumnIndex( bioMaterial );
        if ( row == -1 || column == -1 ) {
            return Double.NaN;
        }
        return matrix[row][column];
    }

    public double getAsDouble( CompositeSequence designElement, BioAssay bioAssay ) {
        int row = getRowIndex( designElement );
        int column = getColumnIndex( bioAssay );
        if ( row == -1 || column == -1 ) {
            return Double.NaN;
        }
        return matrix[row][column];
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
    public Double[] getColumn( int column ) {
        if ( column < 0 || column >= columns() ) {
            throw new IndexOutOfBoundsException( "Column index out of bounds: " + column );
        }
        Double[] col = new Double[rows()];
        for ( int i = 0; i < col.length; i++ ) {
            col[i] = matrix[i][column];
        }
        return col;
    }

    @Override
    public double[] getColumnAsDoubles( int column ) {
        if ( column < 0 || column >= columns() ) {
            throw new IndexOutOfBoundsException( "Column index out of bounds: " + column );
        }
        double[] col = new double[rows()];
        for ( int i = 0; i < col.length; i++ ) {
            col[i] = matrix[i][column];
        }
        return col;
    }

    @Override
    public ExpressionDataDoubleMatrix sliceColumns( List<BioMaterial> bioMaterials ) {
        return new ExpressionDataDoubleMatrix( this, bioMaterials, true );
    }

    @Override
    public ExpressionDataDoubleMatrix sliceColumns( List<BioMaterial> bioMaterials, BioAssayDimension reorderedDim ) {
        return new ExpressionDataDoubleMatrix( this, bioMaterials, reorderedDim );
    }

    @Override
    public Double[][] getMatrix() {
        Double[][] dMatrix = new Double[matrix.length][];
        for ( int i = 0; i < matrix.length; i++ ) {
            dMatrix[i] = ArrayUtils.toObject( matrix[i] );
        }
        return dMatrix;
    }

    /**
     * Obtain the raw matrix without boxing.
     *
     * @see #getMatrix()
     */
    public double[][] getMatrixAsDoubles() {
        return matrix;
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
        return ArrayUtils.toObject( matrix[index] );
    }

    @Override
    public ExpressionDataDoubleMatrix sliceRows( List<CompositeSequence> designElements ) {
        ExpressionDataDoubleMatrix.log.debug( "Creating a filtered matrix " + designElements.size() + " x " + columns() );
        return new ExpressionDataDoubleMatrix( this, designElements );
    }

    @Override
    public double[] getRowAsDoubles( int index ) {
        return matrix[index];
    }

    @Override
    public boolean hasMissingValues() {
        return hasMissingValues;
    }

    /**
     * Convert this matrix to a {@link DoubleMatrix} object.
     * <p>
     * This always returns a copy.
     */
    public DoubleMatrix<CompositeSequence, BioMaterial> asDoubleMatrix() {
        DenseDoubleMatrix<CompositeSequence, BioMaterial> mat = new DenseDoubleMatrix<>( matrix );
        mat.setRowNames( getDesignElements() );
        mat.setColumnNames( getBioMaterials() );
        return mat;
    }

    /**
     * Convert this matrix to a Colt {@link DoubleMatrix2D} object.
     * <p>
     * This always returns a copy.
     */
    public DoubleMatrix2D asDoubleMatrix2D() {
        return new DenseDoubleMatrix2D( matrix );
    }

    /**
     * Create a copy of this matrix with the given data matrix.
     */
    public ExpressionDataDoubleMatrix withMatrix( DoubleMatrix<CompositeSequence, BioMaterial> matrix ) {
        Assert.isTrue( getDesignElements().equals( matrix.getRowNames() ) && getBioMaterials().equals( matrix.getColNames() ),
                "New matrix must have matching design elements and biomaterials." );
        return new ExpressionDataDoubleMatrix( this, matrix.getRawMatrix() );
    }

    public ExpressionDataDoubleMatrix withMatrix( DoubleMatrix2D matrix ) {
        return new ExpressionDataDoubleMatrix( this, matrix.toArray() );
    }

    /**
     * Create a copy of this matrix with the given data matrix and quantitation types.
     * <p>
     * Use this if the matrix and quantitation types are both changing (i.e. normalizing resulting in the
     * {@link QuantitationType#setIsNormalized(boolean) flag to be altered}
     */
    public ExpressionDataDoubleMatrix withMatrix( DoubleMatrix<CompositeSequence, BioMaterial> matrix, Map<QuantitationType, QuantitationType> quantitationTypes ) {
        Assert.isTrue( getDesignElements().equals( matrix.getRowNames() ) && getBioMaterials().equals( matrix.getColNames() ),
                "New matrix must have matching design elements and biomaterials." );
        return withMatrix( matrix.getRawMatrix(), quantitationTypes );
    }

    public ExpressionDataDoubleMatrix withMatrix( DoubleMatrix2D matrix, Map<QuantitationType, QuantitationType> quantitationTypes ) {
        return new ExpressionDataDoubleMatrix( this, matrix.toArray(), quantitationTypes );
    }

    public ExpressionDataDoubleMatrix withMatrix( double[][] matrix, Map<QuantitationType, QuantitationType> quantitationTypes ) {
        return new ExpressionDataDoubleMatrix( this, matrix, quantitationTypes );
    }

    @Nullable
    @Override
    public int[][] getNumberOfCells() {
        return numberOfCells;
    }

    @Nullable
    @Override
    public Integer getNumberOfCells( int row, int column ) {
        return numberOfCells != null ? numberOfCells[row][column] : null;
    }

    @Nullable
    @Override
    public int[] getNumberOfCellsForColumn( int column ) {
        if ( numberOfCells != null ) {
            int[] noc = new int[rows()];
            for ( int i = 0; i < noc.length; i++ ) {
                noc[i] = numberOfCells[i][column];
            }
            return noc;
        }
        return null;
    }

    @Nullable
    @Override
    public int[] getNumberOfCellsForColumn( BioMaterial bioMaterial ) {
        if ( numberOfCells != null ) {
            int column = getColumnIndex( bioMaterial );
            return column != -1 ? numberOfCells[column] : null;
        } else {
            return null;
        }
    }

    @Nullable
    public int[] getNumberOfCellsForRow( int row ) {
        if ( numberOfCells != null ) {
            return numberOfCells[row];
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public int[] getNumberOfCellsForRow( CompositeSequence designElement ) {
        if ( numberOfCells != null ) {
            int row = getRowIndex( designElement );
            return row != -1 ? numberOfCells[row] : null;
        } else {
            return null;
        }
    }

    public ExpressionDataDoubleMatrix withNumberOfCells( @Nullable int[][] numberOfCells ) {
        return new ExpressionDataDoubleMatrix( this, numberOfCells );
    }

    /**
     * @return The expression level ranks (based on mean signal intensity in the vectors)
     */
    public double[] getRanksByMean() {
        if ( this.ranks == null ) {
            this.ranks = computeRanksByMean( this.matrix );
        }
        return this.ranks;
    }

    private double[] computeRanksByMean( double[][] matrix ) {
        int numRows = rows();
        DoubleArrayList means = new DoubleArrayList( numRows );
        for ( int i = 0; i < numRows; i++ ) {
            means.add( DescriptiveWithMissing.mean( new DoubleArrayList( matrix[i] ) ) );
        }
        DoubleArrayList ranks = Rank.rankTransform( means );
        double[] ranksAsDoubles = ranks.elements();
        // convert to [0, 1] range
        for ( int i = 0; i < ranksAsDoubles.length; i++ ) {
            ranksAsDoubles[i] = ranksAsDoubles[i] / numRows;
        }
        return ranksAsDoubles;
    }

    @Override
    protected String format( int row, int column ) {
        return format( matrix[row][column] );
    }

    /**
     * Fill in the data
     *
     * @return DoubleMatrixNamed
     */
    private double[][] createMatrix( List<? extends BulkExpressionDataVector> vectors ) {
        double[][] mat = new double[rows()][columns()];
        for ( double[] doubles : mat ) {
            Arrays.fill( doubles, Double.NaN );
        }
        for ( int i = 0; i < vectors.size(); i++ ) {
            BulkExpressionDataVector vector = vectors.get( i );
            BioAssayDimension dimension = vector.getBioAssayDimension();
            double[] vals = vector.getDataAsDoubles();
            List<BioAssay> bioAssays = dimension.getBioAssays();
            for ( int j = 0; j < bioAssays.size(); j++ ) {
                int column = getColumnIndex( bioAssays.get( j ) );
                mat[i][column] = vals[j];
            }
        }
        ExpressionDataDoubleMatrix.log.debug( "Created a " + rows() + " x " + columns() + " double matrix." );
        return mat;
    }

    @Nullable
    private static int[][] getNumberOfCellsFromVectors( List<? extends BulkExpressionDataVector> vectors ) {
        int[][] result = new int[vectors.size()][];
        for ( int i = 0; i < vectors.size(); i++ ) {
            BulkExpressionDataVector vector = vectors.get( i );
            if ( vector.getNumberOfCells() != null ) {
                result[i] = vector.getNumberOfCells();
            } else {
                log.debug( "At least one vector is missing number of cells information; returning null for the entire matrix." );
                return null;
            }
        }
        return result;
    }

    private static boolean computeHasMissingValues( double[][] matrix ) {
        for ( double[] doubles : matrix ) {
            for ( double d : doubles ) {
                if ( Double.isNaN( d ) )
                    return true;
            }
        }
        return false;
    }
}
