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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.basecode.dataStructure.matrix.AbstractMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.core.analysis.preprocess.convert.QuantitationTypeConversionUtils.mergeQuantitationTypes;

/**
 * Base class for ExpressionDataMatrix implementations that can deal with multiple BioAssays per BioMaterial.
 * <p>
 * Implementation note: The underlying DoubleMatrixNamed is indexed by Integers, which are in turn mapped to BioAssays
 * etc. held here. Thus the 'names' of the underlying matrix are just numbers.
 *
 * @author pavlidis
 */
abstract public class AbstractMultiAssayExpressionDataMatrix<T> extends AbstractExpressionDataMatrix<T> implements MultiAssayBulkExpressionDataMatrix<T>, ExpressionDataMatrix<T> {

    private static final Log log = LogFactory.getLog( ExpressionDataDoubleMatrix.class );

    private static final Comparator<CompositeSequence> DESIGN_ELEMENT_COMPARATOR = Comparator
            .comparing( CompositeSequence::getName, Comparator.nullsLast( Comparator.naturalOrder() ) )
            .thenComparing( CompositeSequence::getId, Comparator.nullsLast( Comparator.naturalOrder() ) );

    @Nullable
    protected ExpressionExperiment expressionExperiment;
    protected Collection<QuantitationType> quantitationTypes;

    protected Map<CompositeSequence, BioAssayDimension> bioAssayDimensions;
    // maps for bioassays/biomaterials/columns
    protected Map<BioAssay, Integer> columnAssayMap;
    protected Map<BioMaterial, Integer> columnBioMaterialMap;
    protected Map<Integer, Collection<BioAssay>> columnBioAssayMapByInteger;
    protected Map<Integer, BioMaterial> columnBioMaterialMapByInteger;
    // maps for designElements/sequences/rows
    protected Map<CompositeSequence, Integer> rowElementMap;
    protected Map<Integer, CompositeSequence> rowDesignElementMapByInteger;

    private List<ExpressionDataMatrixRowElement> rowElements = null;

    protected AbstractMultiAssayExpressionDataMatrix() {
        quantitationTypes = new HashSet<>();
        bioAssayDimensions = new HashMap<>();

        rowElementMap = new LinkedHashMap<>();
        rowDesignElementMapByInteger = new LinkedHashMap<>();

        columnAssayMap = new LinkedHashMap<>();
        columnBioMaterialMap = new LinkedHashMap<>();
        columnBioMaterialMapByInteger = new LinkedHashMap<>();
        columnBioAssayMapByInteger = new LinkedHashMap<>();

    }

    @Override
    public int columns( CompositeSequence el ) {
        int j = 0;
        ArrayDesign ad = el.getArrayDesign();
        for ( int i = 0; i < this.columns(); i++ ) {
            Collection<BioAssay> bioAssay = this.columnBioAssayMapByInteger.get( i );
            for ( BioAssay assay : bioAssay ) {
                if ( assay.getArrayDesignUsed().equals( ad ) ) {
                    j++;
                    break;
                }
            }
        }
        return j;
    }

    @Override
    public Collection<BioAssayDimension> getBioAssayDimensions() {
        return new HashSet<>( this.bioAssayDimensions.values() );
    }

    @Override
    public BioAssayDimension getBioAssayDimension() {
        return getBestBioAssayDimension()
                .orElseThrow( () -> new IllegalStateException( "Could not find an appropriate BioAssayDimension to represent the data matrix; data might need to be matched or merged" ) );
    }

    @Override
    public Optional<BioAssayDimension> getBestBioAssayDimension() {
        Collection<BioAssayDimension> dims = new HashSet<>( this.bioAssayDimensions.values() );
        if ( dims.isEmpty() ) {
            throw new IllegalStateException( "There are no dimensions defined for this matrix." );
        } else if ( dims.size() == 1 ) {
            return Optional.of( dims.iterator().next() );
        }

        // Special complication if there is more than one BioAssayDimension
        Collection<BioMaterial> allBioMaterials = dims.stream()
                .map( BioAssayDimension::getBioAssays )
                .flatMap( Collection::stream )
                .map( BioAssay::getSampleUsed )
                .collect( Collectors.toSet() );

        // find the largest BioAssayDimension that contain all the biomaterials
        return dims.stream()
                .filter( dim -> {
                    // too small to contain all BMs
                    if ( dim.getBioAssays().size() < allBioMaterials.size() ) {
                        return false;
                    }
                    Set<BioMaterial> bms = dim.getBioAssays().stream()
                            .map( BioAssay::getSampleUsed )
                            .collect( Collectors.toSet() );
                    return bms.containsAll( allBioMaterials );
                } )
                .max( Comparator.comparingInt( dim -> dim.getBioAssays().size() ) );
    }

    @Override
    public BioAssayDimension getBioAssayDimension( CompositeSequence designElement ) {
        return this.bioAssayDimensions.get( designElement );
    }

    @Override
    public Collection<BioAssay> getBioAssaysForColumn( int index ) {
        if ( index < 0 || index >= columns() ) {
            throw new IndexOutOfBoundsException();
        }
        return this.columnBioAssayMapByInteger.get( index );
    }

    @Override
    public BioAssay getBioAssayForColumn( int index ) {
        Collection<BioAssay> assays = getBioAssaysForColumn( index );
        if ( assays.size() > 1 ) {
            throw new IllegalStateException( "More than one assay for column " + index + ", use getBioAssaysForColumn() instead." );
        }
        return assays.iterator().next();
    }

    @Override
    public BioMaterial getBioMaterialForColumn( int index ) {
        if ( index < 0 || index >= columns() ) {
            throw new IndexOutOfBoundsException();
        }
        return this.columnBioMaterialMapByInteger.get( index );
    }

    @Override
    public T[] getColumn( BioAssay bioAssay ) {
        int index = getColumnIndex( bioAssay );
        if ( index == -1 ) {
            return null;
        }
        return this.getColumn( index );
    }

    @Override
    public int getColumnIndex( BioMaterial bioMaterial ) {
        Integer j = columnBioMaterialMap.get( bioMaterial );
        return j != null ? j : -1;
    }

    @Override
    public T[] getRow( CompositeSequence designElement ) {
        int index = getRowIndex( designElement );
        if ( index == -1 ) {
            return null;
        }
        return getRow( index );
    }

    @Override
    public List<CompositeSequence> getDesignElements() {
        Vector<CompositeSequence> compositeSequences = new Vector<>();
        compositeSequences.setSize( rows() );
        for ( int i = 0; i < rows(); i++ ) {
            compositeSequences.set( i, this.rowDesignElementMapByInteger.get( i ) );
        }
        return compositeSequences;
    }

    @Override
    public CompositeSequence getDesignElementForRow( int index ) {
        if ( index < 0 || index >= rows() ) {
            throw new IndexOutOfBoundsException();
        }
        return this.rowDesignElementMapByInteger.get( index );
    }

    @Override
    public T get( CompositeSequence designElement, BioAssay bioAssay ) {
        Integer index = this.rowElementMap.get( designElement );
        if ( index == null ) {
            return null;
        }
        Integer j = this.columnAssayMap.get( bioAssay );
        if ( j == null ) {
            return null;
        }
        return get( index, j );
    }

    @Override
    public ExpressionExperiment getExpressionExperiment() {
        return this.expressionExperiment;
    }

    @Override
    public Collection<QuantitationType> getQuantitationTypes() {
        return quantitationTypes;
    }

    @Override
    public QuantitationType getQuantitationType() {
        if ( quantitationTypes.size() > 1 ) {
            return mergeQuantitationTypes( quantitationTypes );
        } else {
            return quantitationTypes.iterator().next();
        }
    }

    @Override
    public List<ExpressionDataMatrixRowElement> getRowElements() {
        if ( this.rowElements == null ) {
            int rows = rows();
            rowElements = new ArrayList<>( rows );
            for ( int i = 0; i < rows; i++ ) {
                rowElements.add( new ExpressionDataMatrixRowElement( this, i ) );
            }
        }
        return this.rowElements;
    }

    @Override
    public int getRowIndex( CompositeSequence designElement ) {
        Integer index = rowElementMap.get( designElement );
        if ( index == null )
            return -1;
        return index;
    }

    @Override
    public int[] getRowIndices( CompositeSequence designElement ) {
        Integer index = rowElementMap.get( designElement );
        if ( index == null ) {
            return null;
        }
        return new int[] { index };
    }

    @Override
    public ExpressionDataMatrixRowElement getRowElement( int index ) {
        if ( rowElements != null ) {
            return rowElements.get( index );
        } else if ( index >= 0 && index < rows() ) {
            return new ExpressionDataMatrixRowElement( this, index );
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * Obtain the column index of a given assay.
     *
     * @return the index, or -1 if not found
     */
    @Override
    public int getColumnIndex( BioAssay bioAssay ) {
        Integer j = columnAssayMap.get( bioAssay );
        return j != null ? j : -1;
    }

    /**
     * Format the value at the provided indices of the matrix.
     */
    protected abstract String format( int row, int column );

    protected abstract void vectorsToMatrix( Collection<? extends BulkExpressionDataVector> vectors );

    protected <R, C, V> void setMatBioAssayValues( AbstractMatrix<R, C, V> mat, Integer rowIndex, V[] vals,
            Collection<BioAssay> bioAssays, Iterator<BioAssay> it ) {
        for ( int j = 0; j < bioAssays.size(); j++ ) {
            BioAssay bioAssay = it.next();
            Integer column = this.columnAssayMap.get( bioAssay );
            assert column != null;
            mat.set( rowIndex, column, vals[j] );
        }
    }

    /**
     * Each row is a unique DesignElement.
     *
     * @param row The row number to be used by this design element.
     */
    protected void addToRowMaps( int row, CompositeSequence designElement ) {
        CompositeSequence prevValue;
        if ( ( prevValue = rowDesignElementMapByInteger.put( row, designElement ) ) != null ) {
            throw new IllegalStateException( "Row index: " + row + " is already associated to another design element: " + prevValue + "." );
        }
        Integer prevIndex;
        if ( ( prevIndex = rowElementMap.put( designElement, row ) ) != null ) {
            throw new IllegalStateException( designElement + " is already associated with a row " + prevIndex + ", it cannot be assigned to row " + row + "." );
        }
    }

    /**
     * <p>
     * Note: In the current versions of Gemma, we require that there can be only a single BioAssayDimension. Thus this
     * code is overly complex. If an experiment has multiple BioAssayDimensions (due to multiple arrays), we merge the
     * vectors (e.g., needed in the last case shown below). However, the issue of having multiple "BioMaterials" per
     * "BioAssay" still exists.
     * <p>
     * Deals with the fact that the bioassay dimensions can vary in size, and don't even need to overlap in the
     * biomaterials used. In the case where there is a single BioAssayDimension this reduces to simply associating each
     * column with a bioassay (though we are forced to use an integer under the hood).
     * <p>
     * For example, in the following diagram "-" indicates a biomaterial, while "*" indicates a bioassay. Each row of
     * "*" indicates samples run on a different microarray design (a different bio assay material). In the examples we
     * assume there is just a single biomaterial dimension.
     *
     * <pre>
     * ---------------
     * *****              -- only a few samples run on this platform
     *  **********        -- ditto
     *            ****    -- these samples were not run on any of the other platforms .
     * </pre>
     * <p>
     * A simpler case:
     * </p>
     *
     * <pre>
     * ---------------
     * ***************
     * ***********
     * *******
     * </pre>
     * <p>
     * A more typical and easy case (one microarray design used):
     * </p>
     *
     * <pre>
     * ----------------
     * ****************
     * </pre>
     * <p>
     * If every sample was run on two different array designs:
     * </p>
     *
     * <pre>
     * ----------------
     * ****************
     * ****************
     * </pre>
     * <p>
     * Every sample was run on a different array design:
     *
     * <pre>
     * -----------------------
     * ******
     *       *********
     *                ********
     * </pre>
     * <p>
     * Because there can be limited or no overlap between the bioassay dimensions, we cannot assume the dimensions of
     * the matrix will be defined by the longest BioAssayDimension. Note that later in processing, this possible lack of
     * overlap is fixed by sample matching or vector merging; this class has to deal with the general case.
     * </p>
     */
    protected int setUpColumnElements() {
        AbstractMultiAssayExpressionDataMatrix.log.debug( "Setting up column elements" );
        assert this.bioAssayDimensions != null && !this.bioAssayDimensions.isEmpty() : "No bioAssayDimensions defined";

        Map<BioMaterial, Collection<BioAssay>> bioMaterialMap = new LinkedHashMap<>();
        for ( BioAssayDimension dimension : this.bioAssayDimensions.values() ) {
            List<BioAssay> bioAssays = dimension.getBioAssays();
            AbstractMultiAssayExpressionDataMatrix.log.debug( "Processing: " + dimension + " with " + bioAssays.size() + " assays" );
            this.getBioMaterialGroupsForAssays( bioMaterialMap, bioAssays );
        }

        if ( AbstractMultiAssayExpressionDataMatrix.log.isDebugEnabled() )
            AbstractMultiAssayExpressionDataMatrix.log.debug( bioMaterialMap.size() + " biomaterialGroups (correspond to columns)" );
        int column = 0;

        for ( BioMaterial bioMaterial : bioMaterialMap.keySet() ) {
            if ( AbstractMultiAssayExpressionDataMatrix.log.isDebugEnabled() )
                AbstractMultiAssayExpressionDataMatrix.log.debug( "Column " + column + " **--->>>> " + bioMaterial );
            for ( BioAssay assay : bioMaterialMap.get( bioMaterial ) ) {
                if ( this.columnBioMaterialMap.containsKey( bioMaterial ) ) {
                    int existingColumn = columnBioMaterialMap.get( bioMaterial );
                    this.columnAssayMap.put( assay, existingColumn );
                    if ( AbstractMultiAssayExpressionDataMatrix.log.isDebugEnabled() )
                        AbstractMultiAssayExpressionDataMatrix.log.debug( assay + " --> column " + existingColumn );

                    columnBioAssayMapByInteger.computeIfAbsent( existingColumn, k -> new HashSet<>() );
                    columnBioAssayMapByInteger.get( existingColumn ).add( assay );
                } else {
                    if ( AbstractMultiAssayExpressionDataMatrix.log.isDebugEnabled() ) {
                        AbstractMultiAssayExpressionDataMatrix.log.debug( bioMaterial + " --> column " + column );
                        AbstractMultiAssayExpressionDataMatrix.log.debug( assay + " --> column " + column );
                    }
                    this.columnBioMaterialMap.put( bioMaterial, column );
                    this.columnAssayMap.put( assay, column );
                    columnBioAssayMapByInteger.computeIfAbsent( column, k -> new HashSet<>() );

                    columnBioMaterialMapByInteger.put( column, bioMaterial );
                    columnBioAssayMapByInteger.get( column ).add( assay );
                }
            }

            column++;
        }

        if ( AbstractMultiAssayExpressionDataMatrix.log.isDebugEnabled() ) {
            for ( BioAssay o : this.columnAssayMap.keySet() ) {
                AbstractMultiAssayExpressionDataMatrix.log.debug( o + " " + this.columnAssayMap.get( o ) );
            }
        }

        assert bioMaterialMap.size() == columnBioMaterialMapByInteger.size();
        return columnBioMaterialMapByInteger.size();
    }

    /**
     * Selects all the vectors passed in (uses them to initialize the data)
     */
    protected void selectVectors( Collection<? extends BulkExpressionDataVector> vectors ) {
        QuantitationType quantitationType = null;
        int i = 0;
        List<BulkExpressionDataVector> sorted = this.sortVectorsByDesignElement( vectors );
        for ( BulkExpressionDataVector vector : sorted ) {
            if ( this.expressionExperiment == null )
                this.expressionExperiment = vector.getExpressionExperiment();
            QuantitationType vectorQuantitationType = vector.getQuantitationType();
            CompositeSequence designElement = vector.getDesignElement();
            this.bioAssayDimensions.put( designElement, vector.getBioAssayDimension() );
            if ( quantitationType == null ) {
                quantitationType = vectorQuantitationType;

                this.getQuantitationTypes().add( vectorQuantitationType );
            } else {
                if ( quantitationType != vectorQuantitationType ) {
                    throw new IllegalArgumentException( "Cannot pass vectors from more than one quantitation type: " +
                            vectorQuantitationType + " vs "
                            + quantitationType );
                }

            }

            this.addToRowMaps( i, designElement );
            i++;
        }

    }

    protected Collection<BulkExpressionDataVector> selectVectors( Collection<? extends BulkExpressionDataVector> vectors,
            Collection<QuantitationType> qTypes ) {
        this.quantitationTypes.addAll( qTypes );

        Collection<BulkExpressionDataVector> vectorsOfInterest = new LinkedHashSet<>();
        int i = 0;

        for ( BulkExpressionDataVector vector : vectors ) {
            QuantitationType vectorQuantitationType = vector.getQuantitationType();
            if ( qTypes.contains( vectorQuantitationType ) ) {
                if ( this.expressionExperiment == null )
                    this.expressionExperiment = vector.getExpressionExperiment();
                vectorsOfInterest.add( vector );
                CompositeSequence designElement = vector.getDesignElement();
                bioAssayDimensions.put( designElement, vector.getBioAssayDimension() );
                this.addToRowMaps( i, designElement );
                this.getQuantitationTypes().add( vectorQuantitationType );
                i++;
            }

        }
        return vectorsOfInterest;
    }

    protected Collection<BulkExpressionDataVector> selectVectors( Collection<? extends BulkExpressionDataVector> vectors,
            List<QuantitationType> qTypes ) {
        this.quantitationTypes.addAll( qTypes );
        List<BulkExpressionDataVector> sorted = this.sortVectorsByDesignElement( vectors );
        Collection<BulkExpressionDataVector> vectorsOfInterest = new LinkedHashSet<>();
        int rowIndex = 0;
        for ( QuantitationType soughtType : qTypes ) {
            for ( BulkExpressionDataVector vector : sorted ) {
                QuantitationType vectorQuantitationType = vector.getQuantitationType();
                if ( vectorQuantitationType.equals( soughtType ) ) {
                    if ( this.expressionExperiment == null )
                        this.expressionExperiment = vector.getExpressionExperiment();
                    vectorsOfInterest.add( vector );
                    this.getQuantitationTypes().add( vectorQuantitationType );
                    CompositeSequence designElement = vector.getDesignElement();
                    this.bioAssayDimensions.put( designElement, vector.getBioAssayDimension() );
                    this.addToRowMaps( rowIndex, designElement );
                    rowIndex++;
                }
            }
        }
        AbstractMultiAssayExpressionDataMatrix.log.debug( "Selected " + vectorsOfInterest.size() + " vectors" );
        return vectorsOfInterest;
    }

    Collection<BulkExpressionDataVector> selectVectors( Collection<? extends BulkExpressionDataVector> vectors,
            QuantitationType quantitationType ) {
        this.quantitationTypes.add( quantitationType );

        Collection<BulkExpressionDataVector> vectorsOfInterest = new LinkedHashSet<>();
        int i = 0;

        for ( BulkExpressionDataVector vector : vectors ) {
            QuantitationType vectorQuantitationType = vector.getQuantitationType();
            if ( vectorQuantitationType.equals( quantitationType ) ) {
                if ( this.expressionExperiment == null )
                    this.expressionExperiment = vector.getExpressionExperiment();
                vectorsOfInterest.add( vector );
                this.getQuantitationTypes().add( vectorQuantitationType );
                CompositeSequence designElement = vector.getDesignElement();
                bioAssayDimensions.put( designElement, vector.getBioAssayDimension() );
                this.addToRowMaps( i, designElement );
                i++;
            }

        }
        return vectorsOfInterest;
    }

    @Override
    protected String formatRepresentation() {
        // we don't want an IllegalStateException if there are multiple, incompatible QTs
        return quantitationTypes.iterator().next().getRepresentation().name().toLowerCase();
    }

    @Override
    protected String getRowLabel( int i ) {
        return this.rowDesignElementMapByInteger.get( i ).getName();
    }

    @Override
    protected String getColumnLabel( int j ) {
        return this.getBioMaterialForColumn( j ).getName() + ":" +
                this.getBioAssaysForColumn( j ).stream().map( BioAssay::getName ).collect( Collectors.joining( "," ) );
    }

    private void getBioMaterialGroupsForAssays( Map<BioMaterial, Collection<BioAssay>> bioMaterialMap,
            List<BioAssay> bioAssays ) {
        for ( BioAssay ba : bioAssays ) {
            if ( AbstractMultiAssayExpressionDataMatrix.log.isDebugEnabled() )
                AbstractMultiAssayExpressionDataMatrix.log.debug( "      " + ba );
            BioMaterial bm = ba.getSampleUsed();
            bioMaterialMap
                    .computeIfAbsent( bm, k -> new HashSet<>() )
                    .add( ba );
        }
    }

    private List<BulkExpressionDataVector> sortVectorsByDesignElement(
            Collection<? extends BulkExpressionDataVector> vectors ) {
        return vectors.stream()
                .sorted( Comparator.comparing( BulkExpressionDataVector::getDesignElement, DESIGN_ELEMENT_COMPARATOR ) )
                .collect( Collectors.toList() );
    }
}
