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
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

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

    protected final ExpressionExperiment expressionExperiment;
    protected final Set<QuantitationType> quantitationTypes;
    protected final Map<CompositeSequence, BioAssayDimension> bioAssayDimensions;

    // maps for bioassays/biomaterials/columns
    protected final Map<BioAssay, Integer> columnAssayMap;
    protected final Map<BioMaterial, Integer> columnBioMaterialMap;
    private final Map<Integer, Collection<BioAssay>> columnBioAssayMapByInteger;
    private final Map<Integer, BioMaterial> columnBioMaterialMapByInteger;

    // maps for designElements/sequences/rows
    protected final Map<CompositeSequence, Integer> rowElementMap;
    protected final Map<Integer, CompositeSequence> rowDesignElementMapByInteger;
    private List<ExpressionDataMatrixRowElement> rowElements = null;

    protected BaseExpressionDataMatrix( ExpressionExperiment ee ) {
        expressionExperiment = ee;
        quantitationTypes = new HashSet<>();
        bioAssayDimensions = new HashMap<>();
        rowElementMap = new LinkedHashMap<>();
        rowDesignElementMapByInteger = new LinkedHashMap<>();
        columnAssayMap = new LinkedHashMap<>();
        columnBioMaterialMap = new LinkedHashMap<>();
        columnBioMaterialMapByInteger = new LinkedHashMap<>();
        columnBioAssayMapByInteger = new LinkedHashMap<>();
    }

    protected BaseExpressionDataMatrix( ExpressionExperiment ee, Collection<? extends DesignElementDataVector> vectors ) {
        this( ee );
        int i = 0;
        List<DesignElementDataVector> sortedVectors = new ArrayList<>( vectors );
        Comparator<DesignElementDataVector> cmp = Comparator
                .comparing( ( DesignElementDataVector v ) -> v.getDesignElement().getName(), Comparator.nullsLast( Comparator.naturalOrder() ) )
                .thenComparing( ( DesignElementDataVector v ) -> v.getDesignElement().getId() );
        sortedVectors.sort( cmp );
        for ( DesignElementDataVector vector : sortedVectors ) {
            QuantitationType vectorQuantitationType = vector.getQuantitationType();
            CompositeSequence designElement = vector.getDesignElement();
            this.bioAssayDimensions.put( designElement, vector.getBioAssayDimension() );
            this.quantitationTypes.add( vectorQuantitationType );
            this.addToRowMaps( i++, designElement );
        }
    }

    public BaseExpressionDataMatrix( ExpressionExperiment ee, Collection<? extends DesignElementDataVector> dataVectors, Collection<QuantitationType> quantitationTypes ) {
        this( ee, selectVectors( dataVectors, quantitationTypes ) );
        this.quantitationTypes.addAll( quantitationTypes );
    }

    public BaseExpressionDataMatrix( ExpressionExperiment ee, Collection<? extends DesignElementDataVector> dataVectors, QuantitationType quantitationType ) {
        this( ee, selectVectors( dataVectors, quantitationType ) );
        this.quantitationTypes.add( quantitationType );
    }

    protected BaseExpressionDataMatrix( BaseExpressionDataMatrix<T> sourceMatrix ) {
        this( sourceMatrix, sourceMatrix.quantitationTypes );
    }

    protected BaseExpressionDataMatrix( BaseExpressionDataMatrix<T> sourceMatrix, Set<QuantitationType> quantitationTypes ) {
        this.expressionExperiment = sourceMatrix.expressionExperiment;
        this.bioAssayDimensions = sourceMatrix.bioAssayDimensions;
        this.columnAssayMap = sourceMatrix.columnAssayMap;
        this.columnBioAssayMapByInteger = sourceMatrix.columnBioAssayMapByInteger;
        this.columnBioMaterialMap = sourceMatrix.columnBioMaterialMap;
        this.columnBioMaterialMapByInteger = sourceMatrix.columnBioMaterialMapByInteger;
        this.quantitationTypes = quantitationTypes;
        this.rowElementMap = sourceMatrix.rowElementMap;
        this.rowDesignElementMapByInteger = sourceMatrix.rowDesignElementMapByInteger;
        this.rowElements = sourceMatrix.rowElements;
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
    public ExpressionExperiment getExpressionExperiment() {
        return this.expressionExperiment;
    }

    @Override
    public Set<QuantitationType> getQuantitationTypes() {
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

    protected int getColumnIndex( BioAssay bioAssay ) {
        return columnAssayMap.get( bioAssay );
    }

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
    protected void addToRowMaps( Integer row, CompositeSequence designElement ) {
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
    protected int setUpColumnElements() {
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

                    columnBioAssayMapByInteger.computeIfAbsent( existingColumn, k -> new HashSet<>() );
                    columnBioAssayMapByInteger.get( existingColumn ).add( assay );
                } else {
                    if ( BaseExpressionDataMatrix.log.isDebugEnabled() ) {
                        BaseExpressionDataMatrix.log.debug( bioMaterial + " --> column " + column );
                        BaseExpressionDataMatrix.log.debug( assay + " --> column " + column );
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

        if ( BaseExpressionDataMatrix.log.isDebugEnabled() ) {
            for ( BioAssay o : this.columnAssayMap.keySet() ) {
                BaseExpressionDataMatrix.log.debug( o + " " + this.columnAssayMap.get( o ) );
            }
        }

        assert bioMaterialMap.size() == columnBioMaterialMapByInteger.keySet().size();
        return columnBioMaterialMapByInteger.keySet().size();
    }

    protected static Collection<DesignElementDataVector> selectVectors(
            Collection<? extends DesignElementDataVector> vectors,
            Collection<QuantitationType> qTypes ) {
        Collection<DesignElementDataVector> vectorsOfInterest = new LinkedHashSet<>();
        for ( DesignElementDataVector vector : vectors ) {
            QuantitationType vectorQuantitationType = vector.getQuantitationType();
            if ( qTypes.contains( vectorQuantitationType ) ) {
                vectorsOfInterest.add( vector );
            }
        }
        return vectorsOfInterest;
    }

    protected static Collection<DesignElementDataVector> selectVectors(
            Collection<? extends DesignElementDataVector> vectors,
            QuantitationType quantitationType ) {
        return selectVectors( vectors, Collections.singleton( quantitationType ) );
    }

    private void getBioMaterialGroupsForAssays( Map<BioMaterial, Collection<BioAssay>> bioMaterialMap,
            List<BioAssay> bioAssays ) {
        for ( BioAssay ba : bioAssays ) {
            if ( BaseExpressionDataMatrix.log.isDebugEnabled() )
                BaseExpressionDataMatrix.log.debug( "      " + ba );
            BioMaterial bm = ba.getSampleUsed();

            if ( !bioMaterialMap.containsKey( bm ) ) {
                bioMaterialMap.put( bm, new HashSet<>() );
            }
            bioMaterialMap.get( bm ).add( ba );
        }
    }
}
