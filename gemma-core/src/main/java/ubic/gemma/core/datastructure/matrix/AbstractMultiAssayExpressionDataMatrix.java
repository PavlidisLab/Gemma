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
import org.springframework.util.Assert;
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

import static ubic.gemma.model.common.quantitationtype.QuantitationTypeUtils.mergeQuantitationTypes;

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
    private final ExpressionExperiment expressionExperiment;
    private final Set<QuantitationType> quantitationTypes = new HashSet<>();
    private final Set<BioAssayDimension> bioAssayDimensions = new HashSet<>();

    // maps for designElements/sequences/rows
    private final List<CompositeSequence> rowDesignElements = new ArrayList<>();
    private final Map<CompositeSequence, Integer> rowElementMap = new HashMap<>();
    private final Map<CompositeSequence, QuantitationType> rowElementQuantitationTypeMap = new HashMap<>();
    private final Map<CompositeSequence, BioAssayDimension> rowElementBioAssayDimensionMap = new HashMap<>();

    // maps for bioassays/biomaterials/columns
    // populated by setUpColumnElements() or by copy, which must be done by the end of the constructor
    // TODO: add specialized constructors and make these fields final
    private List<BioMaterial> columnBioMaterials;
    private Map<BioMaterial, Integer> columnBioMaterialMap;
    private List<Set<BioAssay>> columnBioAssays;
    private Map<BioAssay, Integer> columnAssayMap;

    @Nullable
    private List<ExpressionDataMatrixRowElement> rowElements = null;

    /**
     * Create a simple multi-assay matrix.
     * <p>
     * Add rows with {@link #addToRowMaps(CompositeSequence, QuantitationType, BioAssayDimension)} and call
     * {@link #setUpColumnElements()} once you are done during the constructor of the subclass.
     */
    protected AbstractMultiAssayExpressionDataMatrix( @Nullable ExpressionExperiment ee ) {
        this.expressionExperiment = ee;
    }

    protected AbstractMultiAssayExpressionDataMatrix( @Nullable ExpressionExperiment ee, Collection<BioAssayDimension> dimension ) {
        this.expressionExperiment = ee;
        this.bioAssayDimensions.addAll( dimension );
        setUpColumnElements();
    }

    /**
     * Copy constructor.
     */
    protected AbstractMultiAssayExpressionDataMatrix( AbstractMultiAssayExpressionDataMatrix<T> sourceMatrix ) {
        this.expressionExperiment = sourceMatrix.expressionExperiment;
        this.quantitationTypes.addAll( sourceMatrix.quantitationTypes );
        this.bioAssayDimensions.addAll( sourceMatrix.bioAssayDimensions );
        this.rowElementMap.putAll( sourceMatrix.rowElementMap );
        this.rowDesignElements.addAll( sourceMatrix.rowDesignElements );
        this.rowElementQuantitationTypeMap.putAll( sourceMatrix.rowElementQuantitationTypeMap );
        this.rowElementBioAssayDimensionMap.putAll( sourceMatrix.rowElementBioAssayDimensionMap );
        this.columnAssayMap = new HashMap<>( sourceMatrix.columnAssayMap );
        this.columnBioAssays = new ArrayList<>( sourceMatrix.columnBioAssays );
        this.columnBioMaterialMap = new HashMap<>( sourceMatrix.columnBioMaterialMap );
        this.columnBioMaterials = new ArrayList<>( sourceMatrix.columnBioMaterials );
        this.rowElements = sourceMatrix.rowElements != null ? new ArrayList<>( sourceMatrix.rowElements ) : null;
    }

    @Override
    public Collection<BioAssayDimension> getBioAssayDimensions() {
        return this.bioAssayDimensions;
    }

    @Override
    public BioAssayDimension getBioAssayDimension() {
        return getBestBioAssayDimension()
                .orElseThrow( () -> new IllegalStateException( "Could not find an appropriate BioAssayDimension to represent the data matrix; data might need to be matched or merged" ) );
    }

    @Override
    public Optional<BioAssayDimension> getBestBioAssayDimension() {
        Collection<BioAssayDimension> dims = this.bioAssayDimensions;
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
        return this.rowElementBioAssayDimensionMap.get( designElement );
    }

    @Override
    public List<BioMaterial> getBioMaterials() {
        return columnBioMaterials;
    }

    @Override
    public int columns() {
        return columnBioMaterials.size();
    }

    @Override
    public int columns( CompositeSequence el ) {
        BioAssayDimension dimension = rowElementBioAssayDimensionMap.get( el );
        return ( int ) dimension.getBioAssays().stream()
                .map( BioAssay::getSampleUsed )
                .distinct() // in case a BioMaterial is used for more than one BioAssay in this dimension
                .filter( columnBioMaterialMap::containsKey )
                .count();
    }

    @Override
    public Collection<BioAssay> getBioAssaysForColumn( int index ) {
        if ( index < 0 || index >= columns() ) {
            throw new IndexOutOfBoundsException();
        }
        return this.columnBioAssays.get( index );
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
        return this.columnBioMaterials.get( index );
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
    public int rows() {
        return rowDesignElements.size();
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
        return rowDesignElements;
    }

    @Override
    public CompositeSequence getDesignElementForRow( int index ) {
        if ( index < 0 || index >= rows() ) {
            throw new IndexOutOfBoundsException();
        }
        return this.rowDesignElements.get( index );
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

    @Nullable
    public QuantitationType getQuantitationType( CompositeSequence designElement ) {
        return this.rowElementQuantitationTypeMap.get( designElement );
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

    protected void addToRowMaps( BulkExpressionDataVector vector ) {
        addToRowMaps( vector.getDesignElement(), vector.getQuantitationType(), vector.getBioAssayDimension() );
    }

    /**
     * Add a design element to the row maps.
     *
     * @param qt  The quantitation type for this design element.
     * @param dim The dimension for this design element.
     * @throws IllegalStateException if the row or design element is already mapped.
     */
    protected void addToRowMaps( CompositeSequence designElement, QuantitationType qt, BioAssayDimension dim ) {
        Assert.state( columnBioMaterials == null && columnBioAssays == null, "Column elements were already set up." );
        int row = rowDesignElements.size();
        Integer prevIndex;
        if ( ( prevIndex = rowElementMap.put( designElement, row ) ) != null ) {
            throw new IllegalStateException( designElement + " is already associated with a row " + prevIndex + ", it cannot be assigned to row " + row + "." );
        }
        rowDesignElements.add( designElement );
        quantitationTypes.add( qt );
        rowElementQuantitationTypeMap.put( designElement, qt );
        bioAssayDimensions.add( dim );
        rowElementBioAssayDimensionMap.put( designElement, dim );
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
    protected void setUpColumnElements() {
        Assert.state( !bioAssayDimensions.isEmpty(), "No dimensions to setup columns from." );
        LinkedHashMap<BioMaterial, Set<BioAssay>> bioMaterialMap = new LinkedHashMap<>();
        for ( BioAssayDimension dimension : this.bioAssayDimensions ) {
            List<BioAssay> bioAssays = dimension.getBioAssays();
            AbstractMultiAssayExpressionDataMatrix.log.debug( "Processing: " + dimension + " with " + bioAssays.size() + " assays" );
            for ( BioAssay ba : bioAssays ) {
                if ( AbstractMultiAssayExpressionDataMatrix.log.isDebugEnabled() )
                    AbstractMultiAssayExpressionDataMatrix.log.debug( "      " + ba );
                BioMaterial bm = ba.getSampleUsed();
                bioMaterialMap
                        .computeIfAbsent( bm, k -> new HashSet<>() )
                        .add( ba );
            }
        }
        setUpColumnElements( bioMaterialMap );
    }

    protected void setUpColumnElements( LinkedHashMap<BioMaterial, Set<BioAssay>> bioMaterialMap ) {
        Assert.state( columnBioMaterials == null && columnBioAssays == null, "Column elements were already setup." );
        AbstractMultiAssayExpressionDataMatrix.log.debug( "Setting up column elements" );

        // populated from BADs
        columnBioMaterials = new ArrayList<>( bioMaterialMap.size() );
        columnBioMaterialMap = new HashMap<>( bioMaterialMap.size() );
        columnBioAssays = new ArrayList<>( bioMaterialMap.size() );
        columnAssayMap = new HashMap<>( bioMaterialMap.size() );

        if ( AbstractMultiAssayExpressionDataMatrix.log.isDebugEnabled() )
            AbstractMultiAssayExpressionDataMatrix.log.debug( bioMaterialMap.size() + " biomaterialGroups (correspond to columns)" );

        for ( Map.Entry<BioMaterial, Set<BioAssay>> e : bioMaterialMap.entrySet() ) {
            BioMaterial bioMaterial = e.getKey();
            int column;
            if ( this.columnBioMaterialMap.containsKey( bioMaterial ) ) {
                column = columnBioMaterialMap.get( bioMaterial );
            } else {
                column = columnBioMaterials.size();
                if ( AbstractMultiAssayExpressionDataMatrix.log.isDebugEnabled() )
                    AbstractMultiAssayExpressionDataMatrix.log.debug( "Column " + column + " **--->>>> " + bioMaterial );
                this.columnBioMaterialMap.put( bioMaterial, column );
                columnBioMaterials.add( bioMaterial );
                columnBioAssays.add( new HashSet<>() );
            }
            for ( BioAssay assay : e.getValue() ) {
                if ( AbstractMultiAssayExpressionDataMatrix.log.isDebugEnabled() ) {
                    AbstractMultiAssayExpressionDataMatrix.log.debug( bioMaterial + " --> column " + column );
                    AbstractMultiAssayExpressionDataMatrix.log.debug( assay + " --> column " + column );
                }
                this.columnAssayMap.put( assay, column );
                columnBioAssays.get( column ).add( assay );
            }
        }

        if ( AbstractMultiAssayExpressionDataMatrix.log.isDebugEnabled() ) {
            for ( BioAssay o : this.columnAssayMap.keySet() ) {
                AbstractMultiAssayExpressionDataMatrix.log.debug( o + " " + this.columnAssayMap.get( o ) );
            }
        }

        assert bioMaterialMap.size() == columnBioMaterials.size();
    }

    /**
     * Selects all the vectors passed in (uses them to initialize the data)
     */
    protected List<BulkExpressionDataVector> selectVectors( Collection<? extends BulkExpressionDataVector> vectors ) {
        List<BulkExpressionDataVector> sorted = this.sortVectorsByDesignElement( vectors );
        sorted.forEach( this::addToRowMaps );
        setUpColumnElements();
        return sorted;
    }

    protected List<BulkExpressionDataVector> selectVectors( Collection<? extends BulkExpressionDataVector> vectors,
            Collection<QuantitationType> qTypes ) {
        List<BulkExpressionDataVector> sorted = this.sortVectorsByDesignElement( vectors );
        List<BulkExpressionDataVector> vectorsOfInterest = new ArrayList<>( sorted.size() );
        for ( BulkExpressionDataVector vector : vectors ) {
            QuantitationType vectorQuantitationType = vector.getQuantitationType();
            if ( qTypes.contains( vectorQuantitationType ) ) {
                vectorsOfInterest.add( vector );
                this.addToRowMaps( vector );
            }
        }
        setUpColumnElements();
        return vectorsOfInterest;
    }

    protected List<BulkExpressionDataVector> selectVectors( Collection<? extends BulkExpressionDataVector> vectors,
            List<QuantitationType> qTypes ) {
        List<BulkExpressionDataVector> sorted = this.sortVectorsByDesignElement( vectors );
        List<BulkExpressionDataVector> vectorsOfInterest = new ArrayList<>( vectors.size() );
        for ( QuantitationType soughtType : qTypes ) {
            for ( BulkExpressionDataVector vector : sorted ) {
                QuantitationType vectorQuantitationType = vector.getQuantitationType();
                if ( vectorQuantitationType.equals( soughtType ) ) {
                    vectorsOfInterest.add( vector );
                    this.addToRowMaps( vector );
                }
            }
        }
        AbstractMultiAssayExpressionDataMatrix.log.debug( "Selected " + vectorsOfInterest.size() + " vectors" );
        setUpColumnElements();
        return vectorsOfInterest;
    }

    protected List<BulkExpressionDataVector> selectVectors( Collection<? extends BulkExpressionDataVector> vectors,
            QuantitationType quantitationType ) {
        List<BulkExpressionDataVector> vectorsOfInterest = new ArrayList<>();
        for ( BulkExpressionDataVector vector : vectors ) {
            QuantitationType vectorQuantitationType = vector.getQuantitationType();
            if ( vectorQuantitationType.equals( quantitationType ) ) {
                vectorsOfInterest.add( vector );
                this.addToRowMaps( vector );
            }
        }
        setUpColumnElements();
        return vectorsOfInterest;
    }

    @Override
    protected String formatRepresentation() {
        // we don't want an IllegalStateException if there are multiple, incompatible QTs
        return quantitationTypes.iterator().next().getRepresentation().name().toLowerCase();
    }

    @Override
    protected String getRowLabel( int i ) {
        return this.rowDesignElements.get( i ).getName();
    }

    @Override
    protected String getColumnLabel( int j ) {
        return this.getBioMaterialForColumn( j ).getName() + ":" +
                this.getBioAssaysForColumn( j ).stream().map( BioAssay::getName ).collect( Collectors.joining( "," ) );
    }

    private List<BulkExpressionDataVector> sortVectorsByDesignElement(
            Collection<? extends BulkExpressionDataVector> vectors ) {
        return vectors.stream()
                .sorted( Comparator.comparing( BulkExpressionDataVector::getDesignElement, DESIGN_ELEMENT_COMPARATOR ) )
                .collect( Collectors.toList() );
    }
}
