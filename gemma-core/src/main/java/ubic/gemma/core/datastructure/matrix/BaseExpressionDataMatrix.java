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
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;

/**
 * Base class for ExpressionDataMatrix implementations.
 * Implementation note: The underlying DoubleMatrixNamed is indexed by Integers, which are in turn mapped to BioAssays
 * etc. held here. Thus the 'names' of the underlying matrix are just numbers.
 *
 * @author pavlidis
 */
abstract public class BaseExpressionDataMatrix<T> implements ExpressionDataMatrix<T>, Serializable {

    private static final Log log = LogFactory.getLog( ExpressionDataDoubleMatrix.class );

    @Nullable
    ExpressionExperiment expressionExperiment;
    Collection<QuantitationType> quantitationTypes;

    Map<CompositeSequence, BioAssayDimension> bioAssayDimensions;
    // maps for bioassays/biomaterials/columns
    Map<BioAssay, Integer> columnAssayMap;
    Map<BioMaterial, Integer> columnBioMaterialMap;
    Map<Integer, Collection<BioAssay>> columnBioAssayMapByInteger;
    Map<Integer, BioMaterial> columnBioMaterialMapByInteger;
    // maps for designElements/sequences/rows
    Map<CompositeSequence, Integer> rowElementMap;
    Map<Integer, CompositeSequence> rowDesignElementMapByInteger;

    private List<ExpressionDataMatrixRowElement> rowElements = null;

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
    public BioAssayDimension getBestBioAssayDimension() {

        Collection<BioAssayDimension> dims = new HashSet<>( this.bioAssayDimensions.values() );

        BioAssayDimension b = dims.iterator().next();

        if ( dims.size() > 1 ) {
            /*
             * Special complication if there is more than one BioAssayDimension
             */

            int s = -1;
            Collection<BioMaterial> allBioMaterials = new HashSet<>();
            // find the largest BioAssayDimension
            for ( BioAssayDimension bioAssayDimension : dims ) {
                if ( bioAssayDimension.getBioAssays().size() > s ) {
                    s = bioAssayDimension.getBioAssays().size();
                    b = bioAssayDimension;

                }

                for ( BioAssay ba : b.getBioAssays() ) {
                    allBioMaterials.add( ba.getSampleUsed() );
                }
            }

            /*
             * Sanity check: make sure all the biomaterials are accounted for by the chosen BioAssayDimension.
             */

            for ( BioAssay ba : b.getBioAssays() ) {
                if ( !allBioMaterials.contains( ba.getSampleUsed() ) ) {
                    /*
                     * In rare cases none of the usual ones has all the samples.
                     *
                     * This can also happen if the data are not sample-matched or vector-merged
                     */
                    throw new IllegalStateException(
                            "Could not find an appropriate BioAssayDimension to represent the data matrix; data might need to be matched or merged" );

                }

            }

        }

        return b;

    }

    @Override
    public BioAssayDimension getBioAssayDimension( CompositeSequence designElement ) {

        return this.bioAssayDimensions.get( designElement );

    }

    @Override
    public Collection<BioAssay> getBioAssaysForColumn( int index ) {
        return this.columnBioAssayMapByInteger.get( index );
    }

    @Override
    public BioMaterial getBioMaterialForColumn( int index ) {
        return this.columnBioMaterialMapByInteger.get( index );
    }

    @Override
    public int getColumnIndex( BioMaterial bioMaterial ) {
        return columnBioMaterialMap.get( bioMaterial );
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
        return this.rowDesignElementMapByInteger.get( index );
    }

    /**
     * The associated {@link ExpressionExperiment}, if known.
     */
    @Override
    @Nullable
    public ExpressionExperiment getExpressionExperiment() {
        return this.expressionExperiment;
    }

    @Override
    public Collection<QuantitationType> getQuantitationTypes() {
        return quantitationTypes;
    }

    @Override
    public List<ExpressionDataMatrixRowElement> getRowElements() {
        if ( this.rowElements == null ) {
            rowElements = new ArrayList<>();
            for ( int i = 0; i < this.rows(); i++ ) {
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

    public ExpressionDataMatrixRowElement getRowElement( int index ) {
        return this.getRowElements().get( index );
    }

    @SuppressWarnings("unused") // useful interface
    protected abstract void vectorsToMatrix( Collection<? extends DesignElementDataVector> vectors );

    int getColumnIndex( BioAssay bioAssay ) {
        return columnAssayMap.get( bioAssay );
    }

    void init() {
        quantitationTypes = new HashSet<>();
        bioAssayDimensions = new HashMap<>();

        rowElementMap = new LinkedHashMap<>();
        rowDesignElementMapByInteger = new LinkedHashMap<>();

        columnAssayMap = new LinkedHashMap<>();
        columnBioMaterialMap = new LinkedHashMap<>();
        columnBioMaterialMapByInteger = new LinkedHashMap<>();
        columnBioAssayMapByInteger = new LinkedHashMap<>();

    }

    <R, C, V> void setMatBioAssayValues( AbstractMatrix<R, C, V> mat, Integer rowIndex, V[] vals,
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
    void addToRowMaps( Integer row, CompositeSequence designElement ) {
        rowDesignElementMapByInteger.put( row, designElement );
        rowElementMap.put( designElement, row );
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
    int setUpColumnElements() {
        BaseExpressionDataMatrix.log.debug( "Setting up column elements" );
        assert this.bioAssayDimensions != null && this.bioAssayDimensions.size() > 0 : "No bioAssayDimensions defined";

        Map<BioMaterial, Collection<BioAssay>> bioMaterialMap = new LinkedHashMap<>();
        for ( BioAssayDimension dimension : this.bioAssayDimensions.values() ) {
            List<BioAssay> bioAssays = dimension.getBioAssays();
            BaseExpressionDataMatrix.log.debug( "Processing: " + dimension + " with " + bioAssays.size() + " assays" );
            this.getBioMaterialGroupsForAssays( bioMaterialMap, bioAssays );
        }

        if ( BaseExpressionDataMatrix.log.isDebugEnabled() )
            BaseExpressionDataMatrix.log.debug( bioMaterialMap.size() + " biomaterialGroups (correspond to columns)" );
        int column = 0;

        for ( BioMaterial bioMaterial : bioMaterialMap.keySet() ) {
            if ( BaseExpressionDataMatrix.log.isDebugEnabled() )
                BaseExpressionDataMatrix.log.debug( "Column " + column + " **--->>>> " + bioMaterial );
            for ( BioAssay assay : bioMaterialMap.get( bioMaterial ) ) {
                if ( this.columnBioMaterialMap.containsKey( bioMaterial ) ) {
                    int existingColumn = columnBioMaterialMap.get( bioMaterial );
                    this.columnAssayMap.put( assay, existingColumn );
                    if ( BaseExpressionDataMatrix.log.isDebugEnabled() )
                        BaseExpressionDataMatrix.log.debug( assay + " --> column " + existingColumn );

                    if ( columnBioAssayMapByInteger.get( existingColumn ) == null ) {
                        columnBioAssayMapByInteger.put( existingColumn, new HashSet<BioAssay>() );
                    }
                    columnBioAssayMapByInteger.get( existingColumn ).add( assay );
                } else {
                    if ( BaseExpressionDataMatrix.log.isDebugEnabled() ) {
                        BaseExpressionDataMatrix.log.debug( bioMaterial + " --> column " + column );
                        BaseExpressionDataMatrix.log.debug( assay + " --> column " + column );
                    }
                    this.columnBioMaterialMap.put( bioMaterial, column );
                    this.columnAssayMap.put( assay, column );
                    if ( columnBioAssayMapByInteger.get( column ) == null ) {
                        columnBioAssayMapByInteger.put( column, new HashSet<BioAssay>() );
                    }

                    columnBioMaterialMapByInteger.put( column, bioMaterial );
                    columnBioAssayMapByInteger.get( column ).add( assay );
                }
            }

            column++;
        }

        if ( BaseExpressionDataMatrix.log.isDebugEnabled() ) {
            for ( BioAssay o : this.columnAssayMap.keySet() ) {
                BaseExpressionDataMatrix.log.debug( o + " " + this.columnAssayMap.get( o ) );
            }
        }

        assert bioMaterialMap.size() == columnBioMaterialMapByInteger.keySet().size();
        return columnBioMaterialMapByInteger.keySet().size();
    }

    /**
     * Selects all the vectors passed in (uses them to initialize the data)
     */
    void selectVectors( Collection<? extends DesignElementDataVector> vectors ) {
        QuantitationType quantitationType = null;
        int i = 0;
        List<DesignElementDataVector> sorted = this.sortVectorsByDesignElement( vectors );
        for ( DesignElementDataVector vector : sorted ) {
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

    Collection<DesignElementDataVector> selectVectors( Collection<? extends DesignElementDataVector> vectors,
            Collection<QuantitationType> qTypes ) {
        this.quantitationTypes.addAll( qTypes );

        Collection<DesignElementDataVector> vectorsOfInterest = new LinkedHashSet<>();
        int i = 0;

        for ( DesignElementDataVector vector : vectors ) {
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

    Collection<DesignElementDataVector> selectVectors( Collection<? extends DesignElementDataVector> vectors,
            List<QuantitationType> qTypes ) {
        this.quantitationTypes.addAll( qTypes );
        List<DesignElementDataVector> sorted = this.sortVectorsByDesignElement( vectors );
        Collection<DesignElementDataVector> vectorsOfInterest = new LinkedHashSet<>();
        int rowIndex = 0;
        for ( QuantitationType soughtType : qTypes ) {
            for ( DesignElementDataVector vector : sorted ) {
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
        BaseExpressionDataMatrix.log.debug( "Selected " + vectorsOfInterest.size() + " vectors" );
        return vectorsOfInterest;
    }

    Collection<DesignElementDataVector> selectVectors( Collection<? extends DesignElementDataVector> vectors,
            QuantitationType quantitationType ) {
        this.quantitationTypes.add( quantitationType );

        Collection<DesignElementDataVector> vectorsOfInterest = new LinkedHashSet<>();
        int i = 0;

        for ( DesignElementDataVector vector : vectors ) {
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

    Collection<DesignElementDataVector> selectVectors( ExpressionExperiment ee, QuantitationType quantitationType ) {
        Collection<RawExpressionDataVector> vectors = ee.getRawExpressionDataVectors();
        return this.selectVectors( quantitationType, vectors );
    }

    private Collection<DesignElementDataVector> selectVectors( QuantitationType quantitationType,
            Collection<? extends DesignElementDataVector> vectors ) {
        Collection<DesignElementDataVector> vectorsOfInterest = new LinkedHashSet<>();
        this.quantitationTypes.add( quantitationType );
        List<DesignElementDataVector> sorted = this.sortVectorsByDesignElement( vectors );
        int i = 0;
        for ( DesignElementDataVector vector : sorted ) {
            QuantitationType vectorQuantitationType = vector.getQuantitationType();
            if ( this.expressionExperiment == null )
                this.expressionExperiment = vector.getExpressionExperiment();
            if ( vectorQuantitationType.equals( quantitationType ) ) {
                vectorsOfInterest.add( vector );
                CompositeSequence designElement = vector.getDesignElement();
                this.getQuantitationTypes().add( vectorQuantitationType );
                this.bioAssayDimensions.put( designElement, vector.getBioAssayDimension() );
                this.addToRowMaps( i, designElement );
                i++;
            }
        }
        return vectorsOfInterest;
    }

    private void getBioMaterialGroupsForAssays( Map<BioMaterial, Collection<BioAssay>> bioMaterialMap,
            List<BioAssay> bioAssays ) {
        for ( BioAssay ba : bioAssays ) {
            if ( BaseExpressionDataMatrix.log.isDebugEnabled() )
                BaseExpressionDataMatrix.log.debug( "      " + ba );
            BioMaterial bm = ba.getSampleUsed();

            if ( !bioMaterialMap.containsKey( bm ) ) {
                bioMaterialMap.put( bm, new HashSet<BioAssay>() );
            }
            bioMaterialMap.get( bm ).add( ba );

        }
    }

    private List<DesignElementDataVector> sortVectorsByDesignElement(
            Collection<? extends DesignElementDataVector> vectors ) {
        List<DesignElementDataVector> vectorSort = new ArrayList<>( vectors );
        Comparator<DesignElementDataVector> cmp = Comparator
                .comparing( ( DesignElementDataVector vector ) -> vector.getDesignElement().getName(), Comparator.nullsLast( Comparator.naturalOrder() ) )
                .thenComparing( ( DesignElementDataVector vector ) -> vector.getDesignElement().getId() );
        vectorSort.sort( cmp );
        return vectorSort;
    }

}
