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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import ubic.basecode.dataStructure.matrix.AbstractMatrix;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.Rank;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

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

    private final DoubleMatrix<CompositeSequence, BioMaterial> matrix;

    /**
     * Indicate if {@link #matrix} contains missing values.
     */
    @Nullable
    private Boolean hasMissingValues = null;

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
    private Map<CompositeSequence, Double> ranks = null;

    public ExpressionDataDoubleMatrix( @Nullable ExpressionExperiment ee, Collection<? extends BulkExpressionDataVector> vectors ) {
        super( ee );
        for ( BulkExpressionDataVector dedv : vectors ) {
            if ( !dedv.getQuantitationType().getRepresentation().equals( PrimitiveType.DOUBLE ) ) {
                throw new IllegalStateException(
                        "Cannot convert non-double quantitation types into double matrix:" + dedv
                                .getQuantitationType() );
            }
        }
        List<BulkExpressionDataVector> selectedVectors = selectVectors( vectors );
        if ( selectedVectors.isEmpty() ) {
            throw new IllegalArgumentException( "No vectors!" );
        }
        this.matrix = this.createMatrix( selectedVectors );
        this.numberOfCells = getNumberOfCellsFromVectors( selectedVectors );
    }

    public ExpressionDataDoubleMatrix( ExpressionExperiment ee, Collection<? extends BulkExpressionDataVector> dataVectors,
            Collection<QuantitationType> quantitationTypes ) {
        super( ee );
        for ( QuantitationType qt : quantitationTypes ) {
            if ( !qt.getRepresentation().equals( PrimitiveType.DOUBLE ) ) {
                throw new IllegalStateException(
                        "Cannot convert non-double quantitation types into double matrix: " + qt );
            }
        }
        List<BulkExpressionDataVector> selectedVectors = selectVectors( dataVectors, quantitationTypes );
        if ( selectedVectors.isEmpty() ) {
            throw new IllegalArgumentException( "No vectors!" );
        }
        this.matrix = this.createMatrix( selectedVectors );
        this.numberOfCells = getNumberOfCellsFromVectors( selectedVectors );
    }

    public ExpressionDataDoubleMatrix( ExpressionExperiment ee, Collection<? extends BulkExpressionDataVector> dataVectors,
            QuantitationType quantitationType ) {
        super( ee );
        if ( !quantitationType.getRepresentation().equals( PrimitiveType.DOUBLE ) ) {
            throw new IllegalStateException(
                    "Cannot convert non-double quantitation types into double matrix: " + quantitationType );
        }
        List<BulkExpressionDataVector> selectedVectors = selectVectors( dataVectors, quantitationType );
        if ( selectedVectors.isEmpty() ) {
            throw new IllegalArgumentException( "No vectors!" );
        }
        this.matrix = this.createMatrix( selectedVectors );
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

        this.matrix = dataMatrix;
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
    private ExpressionDataDoubleMatrix( ExpressionDataDoubleMatrix sourceMatrix,
            DoubleMatrix<CompositeSequence, BioMaterial> dataMatrix ) {
        super( sourceMatrix );
        Assert.isTrue( dataMatrix.getColNames().equals( sourceMatrix.getBioMaterials() )
                        && dataMatrix.getRowNames().equals( sourceMatrix.getDesignElements() ),
                "The rows and columns of the new matrix correspond to the original matrix." );
        this.matrix = dataMatrix;
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
            DoubleMatrix<CompositeSequence, BioMaterial> dataMatrix, Map<QuantitationType, QuantitationType> quantitationTypes ) {
        super( sourceMatrix.getExpressionExperiment() );
        Assert.isTrue( dataMatrix.getColNames().equals( sourceMatrix.getBioMaterials() )
                        && dataMatrix.getRowNames().equals( sourceMatrix.getDesignElements() ),
                "The rows and columns of the new matrix correspond to the original matrix." );
        for ( int i = 0; i < dataMatrix.rows(); i++ ) {
            CompositeSequence element = dataMatrix.getRowName( i );
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
        this.matrix = new DenseDoubleMatrix<>( rowsToUse.size(), sourceMatrix.columns() );
        this.matrix.setColumnNames( sourceMatrix.matrix.getColNames() );
        int[][] numberOfCells = null;
        for ( int i = 0; i < rowsToUse.size(); i++ ) {
            CompositeSequence element = rowsToUse.get( i );
            addToRowMaps( element, requireNonNull( sourceMatrix.getQuantitationType( element ) ),
                    requireNonNull( sourceMatrix.getBioAssayDimension( element ) ) );
            double[] rowVals = sourceMatrix.getRowAsDoubles( element );
            if ( rowVals == null ) {
                throw new IllegalArgumentException( "Source matrix does not have a row for " + element + "." );
            }

            this.matrix.addRowName( element );

            for ( int j = 0; j < rowVals.length; j++ ) {
                Double val = rowVals[j];
                this.matrix.set( i, j, val );
            }
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

        this.numberOfCells = numberOfCells;
    }

    private ExpressionDataDoubleMatrix( ExpressionDataDoubleMatrix sourceMatrix, List<BioMaterial> columnsToUse, boolean dummy ) {
        super( sourceMatrix.getExpressionExperiment() );

        this.matrix = new DenseDoubleMatrix<>( sourceMatrix.rows(), columnsToUse.size() );
        this.matrix.setRowNames( sourceMatrix.matrix.getRowNames() );
        this.matrix.setColumnNames( columnsToUse );

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
                Double val = sourceRow[originalBioMaterialIndices[j]];
                this.matrix.set( i, j, val );
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

        this.matrix = new DenseDoubleMatrix<>( sourceMatrix.rows(), columnsToUse.size() );
        this.matrix.setRowNames( sourceMatrix.matrix.getRowNames() );
        this.matrix.setColumnNames( columnsToUse );

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
                Double val = sourceRow[originalBioMaterialIndices[j]];
                this.matrix.set( i, j, val );
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

        this.numberOfCells = numberOfCells;
    }

    public ExpressionDataDoubleMatrix( ExpressionDataDoubleMatrix sourceMatrix, @Nullable int[][] numberOfCells ) {
        super( sourceMatrix );
        Assert.isTrue( numberOfCells == null || ( numberOfCells.length == sourceMatrix.rows() && Arrays.stream( numberOfCells ).allMatch( row -> row.length == sourceMatrix.columns() ) ) );
        this.matrix = sourceMatrix.matrix;
        this.ranks = sourceMatrix.ranks;
        this.numberOfCells = numberOfCells;
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
    public ExpressionDataDoubleMatrix sliceColumns( List<BioMaterial> bioMaterials ) {
        return new ExpressionDataDoubleMatrix( this, bioMaterials, true );
    }

    @Override
    public ExpressionDataDoubleMatrix sliceColumns( List<BioMaterial> bioMaterials, BioAssayDimension reorderedDim ) {
        return new ExpressionDataDoubleMatrix( this, bioMaterials, reorderedDim );
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
     *
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
    public ExpressionDataDoubleMatrix sliceRows( List<CompositeSequence> designElements ) {
        ExpressionDataDoubleMatrix.log.debug( "Creating a filtered matrix " + designElements.size() + " x " + columns() );
        return new ExpressionDataDoubleMatrix( this, designElements );
    }

    @Override
    public double[] getRowAsDoubles( int index ) {
        return matrix.getRow( index );
    }

    @Override
    public boolean hasMissingValues() {
        if ( hasMissingValues == null ) {
            hasMissingValues = computeHasMissingValues( matrix );
        }
        return hasMissingValues;
    }

    private boolean computeHasMissingValues( DoubleMatrix<CompositeSequence, BioMaterial> matrix ) {
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

    /**
     * @deprecated modifying the matrix directly is not recommended, make a copy instead.
     */
    @Deprecated
    public void set( int row, int column, @Nullable Double value ) {
        if ( value == null || value.isNaN() ) {
            matrix.set( row, column, Double.NaN );
            hasMissingValues = true;
        } else {
            matrix.set( row, column, value );
            // invalidate missing values cache
            hasMissingValues = null;
        }
        // invalidate ranks
        this.ranks = null;
    }

    public DoubleMatrix<CompositeSequence, BioMaterial> getMatrix() {
        return matrix;
    }

    /**
     * Create a copy of this matrix with the given data matrix.
     */
    public ExpressionDataDoubleMatrix withMatrix( DoubleMatrix<CompositeSequence, BioMaterial> matrix ) {
        return new ExpressionDataDoubleMatrix( this, matrix );
    }

    /**
     * Create a copy of this matrix with the given data matrix and quantitation types.
     * <p>
     * Use this if the matrix and quantitation types are both changing (i.e. normalizing resulting in the
     * {@link QuantitationType#setIsNormalized(boolean) flag to be altered}
     */
    public ExpressionDataDoubleMatrix withMatrix( DoubleMatrix<CompositeSequence, BioMaterial> matrix, Map<QuantitationType, QuantitationType> quantitationTypes ) {
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
    public Map<CompositeSequence, Double> getRanksByMean() {
        if ( this.ranks == null ) {
            this.ranks = computeRanksByMean( this.matrix );
        }
        return this.ranks;
    }

    private Map<CompositeSequence, Double> computeRanksByMean( DoubleMatrix<CompositeSequence, BioMaterial> matrix ) {
        DoubleArrayList means = new DoubleArrayList( matrix.rows() );
        for ( int i = 0; i < matrix.rows(); i++ ) {
            means.add( DescriptiveWithMissing.mean( matrix.getRowArrayList( i ) ) );
        }
        DoubleArrayList ranks = Rank.rankTransform( means );
        Map<CompositeSequence, Double> rankMap = new HashMap<>( matrix.rows() );
        for ( int i = 0; i < matrix.rows(); i++ ) {
            rankMap.put( matrix.getRowName( i ), ranks.get( i ) / matrix.rows() );
        }
        return rankMap;
    }

    public List<CompositeSequence> getRowNames() {
        return matrix.getRowNames();
    }

    @Deprecated
    public void set( CompositeSequence designElement, BioAssay bioAssay, Double value ) {
        int row = this.getRowIndex( designElement );
        int column = this.getColumnIndex( bioAssay );
        set( row, column, value );
    }

    @Override
    protected String format( int row, int column ) {
        return format( matrix.get( row, column ) );
    }

    /**
     * Fill in the data
     *
     * @return DoubleMatrixNamed
     */
    private DoubleMatrix<CompositeSequence, BioMaterial> createMatrix(
            Collection<? extends BulkExpressionDataVector> vectors ) {

        int numRows = rows();
        int numColumns = columns();

        DoubleMatrix<CompositeSequence, BioMaterial> mat = new DenseDoubleMatrix<>( numRows, numColumns );

        for ( int j = 0; j < mat.columns(); j++ ) {
            mat.addColumnName( this.getBioMaterialForColumn( j ) );
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

            int rowIndex = getRowIndex( designElement );
            assert rowIndex != -1;

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

    private <R, C, V> void setMatBioAssayValues( AbstractMatrix<R, C, V> mat, Integer rowIndex, V[] vals,
            Collection<BioAssay> bioAssays, Iterator<BioAssay> it ) {
        for ( int j = 0; j < bioAssays.size(); j++ ) {
            BioAssay bioAssay = it.next();
            int column = getColumnIndex( bioAssay );
            assert column != -1;
            mat.set( rowIndex, column, vals[j] );
        }
    }
}
