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
package ubic.gemma.datastructure.matrix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * Base class for ExpressionDataMatrix implementations.
 * <p>
 * Implementation note: The underlying DoubleMatrixNamed is indexed by Integers, which are in turn mapped to BioAssays
 * etc. held here. Thus the 'names' of the underlying matrix are just numbers.
 * 
 * @author pavlidis
 * @version $Id$
 */
abstract public class BaseExpressionDataMatrix implements ExpressionDataMatrix {

    private Log log = LogFactory.getLog( ExpressionDataDoubleMatrix.class );
    // protected LinkedHashSet<DesignElement> rowElements;

    protected Collection<BioAssayDimension> bioAssayDimensions;

    // maps for bioassays/biomaterials/columns
    protected Map<BioAssay, Integer> columnAssayMap;
    protected Map<BioMaterial, Integer> columnBioMaterialMap;
    protected Map<Integer, Collection<BioAssay>> columnBioAssayMapByInteger;
    protected Map<Integer, BioMaterial> columnBioMaterialMapByInteger;

    // maps for designelements/sequences/rows
    protected Map<DesignElement, Integer> rowElementMap;
    protected Map<BioSequence, Collection<Integer>> rowBioSequenceMap;
    protected Map<Integer, Collection<DesignElement>> rowDesignElementMapByInteger;
    protected Map<Integer, BioSequence> rowBioSequencemapByInteger;

    protected void init() {

        bioAssayDimensions = new HashSet<BioAssayDimension>();

        // rowElements = new LinkedHashSet<DesignElement>(); // defunct.
        rowElementMap = new LinkedHashMap<DesignElement, Integer>();
        rowBioSequenceMap = new LinkedHashMap<BioSequence, Collection<Integer>>();
        rowDesignElementMapByInteger = new LinkedHashMap<Integer, Collection<DesignElement>>();
        rowBioSequencemapByInteger = new LinkedHashMap<Integer, BioSequence>();

        columnAssayMap = new LinkedHashMap<BioAssay, Integer>();
        columnBioMaterialMap = new LinkedHashMap<BioMaterial, Integer>();
        columnBioMaterialMapByInteger = new LinkedHashMap<Integer, BioMaterial>();
        columnBioAssayMapByInteger = new LinkedHashMap<Integer, Collection<BioAssay>>();

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getBioAssayForColumn(int)
     */
    public Collection<BioAssay> getBioAssaysForColumn( int index ) {
        return this.columnBioAssayMapByInteger.get( index );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getBioMaterialForColumn(int)
     */
    public BioMaterial getBioMaterialForColumn( int index ) {
        return this.columnBioMaterialMapByInteger.get( index );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#columns(ubic.gemma.model.expression.designElement.DesignElement)
     */
    public int columns( DesignElement el ) {
        int j = 0;
        ArrayDesign ad = el.getArrayDesign();
        for ( int i = 0; i < columns(); i++ ) {
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

    List<ExpressionDataMatrixRowElement> rowElements = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRowElements()
     */
    public List<ExpressionDataMatrixRowElement> getRowElements() {
        if ( this.rowElements == null ) {
            rowElements = new ArrayList<ExpressionDataMatrixRowElement>();
            for ( int i = 0; i < rows(); i++ ) {
                rowElements.add( new ExpressionDataMatrixRowElement( this, i ) );
            }
        }
        return this.rowElements;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRowElement(int)
     */
    public ExpressionDataMatrixRowElement getRowElement( int index ) {
        return this.getRowElements().get( index );
    }

    public BioSequence getBioSequenceForRow( int index ) {
        return this.rowBioSequencemapByInteger.get( index );
    }

    public Collection<DesignElement> getDesignElementsForRow( int index ) {
        return this.rowDesignElementMapByInteger.get( index );
    }

    public int getColumnIndex( BioMaterial bioMaterial ) {
        return columnBioMaterialMap.get( bioMaterial );
    }

    public int getColumnIndex( BioAssay bioAssay ) {
        return columnAssayMap.get( bioAssay );
    }

    public int getRowIndex( DesignElement designElement ) {
        return rowElementMap.get( designElement );
    }

    /**
     * @param expressionExperiment
     * @param quantitationTypes
     * @return
     */
    protected Collection<DesignElementDataVector> selectVectors( ExpressionExperiment expressionExperiment,
            Collection<QuantitationType> quantitationTypes ) {
        Collection<DesignElementDataVector> selected = new HashSet<DesignElementDataVector>();
        Collection<DesignElementDataVector> vectors = expressionExperiment.getDesignElementDataVectors();
        for ( QuantitationType type : quantitationTypes ) {
            selected.addAll( this.selectVectors( type, vectors ) );
        }
        return selected;
    }

    /**
     * @param expressionExperiment
     * @param quantitationType
     * @return Collection<DesignElementDataVector>
     */
    protected Collection<DesignElementDataVector> selectVectors( ExpressionExperiment expressionExperiment,
            QuantitationType quantitationType ) {
        Collection<DesignElementDataVector> vectors = expressionExperiment.getDesignElementDataVectors();
        return selectVectors( quantitationType, vectors );
    }

    /**
     * @param quantitationType
     * @param vectors
     * @return Collection<DesignElementDataVector>
     */
    protected Collection<DesignElementDataVector> selectVectors( QuantitationType quantitationType,
            Collection<DesignElementDataVector> vectors ) {
        Collection<DesignElementDataVector> vectorsOfInterest = new LinkedHashSet<DesignElementDataVector>();
        int i = 0;
        for ( DesignElementDataVector vector : vectors ) {
            QuantitationType vectorQuantitationType = vector.getQuantitationType();
            if ( vectorQuantitationType.equals( quantitationType ) ) {
                vectorsOfInterest.add( vector );
                DesignElement designElement = vector.getDesignElement();
                this.bioAssayDimensions.add( vector.getBioAssayDimension() );
                boolean addedRow = addToRowMaps( i, designElement );
                if ( addedRow ) i++;
            }
        }
        return vectorsOfInterest;
    }

    /**
     * @param quantitationType
     * @param bioAssayDimension
     * @param vectors
     * @return
     */
    protected Collection<DesignElementDataVector> selectVectors( ExpressionExperiment expressionExperiment,
            QuantitationType quantitationType, BioAssayDimension bioAssayDimension ) {
        Collection<DesignElementDataVector> vectors = expressionExperiment.getDesignElementDataVectors();
        Collection<DesignElementDataVector> vectorsOfInterest = new LinkedHashSet<DesignElementDataVector>();
        int i = 0;
        for ( DesignElementDataVector vector : vectors ) {
            QuantitationType vectorQuantitationType = vector.getQuantitationType();
            BioAssayDimension cand = vector.getBioAssayDimension();
            if ( vectorQuantitationType.equals( quantitationType ) && cand.equals( bioAssayDimension ) ) {
                vectorsOfInterest.add( vector );
                DesignElement designElement = vector.getDesignElement();
                this.bioAssayDimensions.add( vector.getBioAssayDimension() );
                boolean addedRow = addToRowMaps( i, designElement );
                if ( addedRow ) i++;
            }
        }
        return vectorsOfInterest;
    }

    /**
     * @param expressionExperiment
     * @param quantitationTypes
     * @param soughtBioAssayDimensions in the same order as the quantitation types
     * @return
     */
    protected Collection<DesignElementDataVector> selectVectors( ExpressionExperiment expressionExperiment,
            List<QuantitationType> quantitationTypes, List<BioAssayDimension> soughtBioAssayDimensions ) {

        if ( quantitationTypes.size() != soughtBioAssayDimensions.size() )
            throw new IllegalArgumentException(
                    "Must have the same number of quantitation types and bioassay dimensions" );

        Collection<DesignElementDataVector> vectors = expressionExperiment.getDesignElementDataVectors();

        Collection<DesignElementDataVector> vectorsOfInterest = selectVectors( vectors, soughtBioAssayDimensions,
                quantitationTypes );

        return vectorsOfInterest;
    }

    /**
     * @param vectors
     * @param bioAssayDimensions
     * @param quantitationTypes
     * @return
     */
    protected Collection<DesignElementDataVector> selectVectors( Collection<DesignElementDataVector> vectors,
            List<BioAssayDimension> bioAssayDimensions, List<QuantitationType> quantitationTypes ) {
        Collection<DesignElementDataVector> vectorsOfInterest = new LinkedHashSet<DesignElementDataVector>();
        int j = 0;
        for ( int i = 0; i < quantitationTypes.size(); i++ ) {
            QuantitationType soughtType = quantitationTypes.get( i );
            BioAssayDimension soughtDim = bioAssayDimensions.get( i );
            assert soughtType != null && soughtDim != null;
            for ( DesignElementDataVector vector : vectors ) {
                QuantitationType vectorQuantitationType = vector.getQuantitationType();
                BioAssayDimension cand = vector.getBioAssayDimension();
                if ( vectorQuantitationType.equals( soughtType ) && cand.equals( soughtDim ) ) {
                    vectorsOfInterest.add( vector );
                    DesignElement designElement = vector.getDesignElement();
                    this.bioAssayDimensions.add( vector.getBioAssayDimension() );
                    boolean addedRow = addToRowMaps( j, designElement );
                    if ( addedRow ) j++;
                }
            }
        }
        return vectorsOfInterest;
    }

    /**
     * Information needed to track row and biosequence &lt;--&gt; design element relations in the matrix.
     * <p>
     * Cases:
     * <ol>
     * <li>Sequence has already appeared
     * <ol>
     * <li>This is a repeat occurrence of this sequence on the same microarray. Action: add a row to the matrix, UNLESS
     * there is a match on another microarray design, in which case use THAT row.
     * <li>This is a new occurence of this sequence on this microarray, but has appeared on another. Action: don't add
     * a new row to the matrix. Add the design Element to the map for the current row.
     * </ol>
     * <li>Sequence has not yet appeared. Action: add a row to the matrix. Create new map information for this sequence
     * and row.
     * </ol>
     * 
     * @param row The current row number to be used if a new row is added.
     * @param designElement
     * @return true if a new row element is added, false if we've encountered a biosequence that is already in the
     *         matrix.
     */
    protected boolean addToRowMaps( Integer row, DesignElement designElement ) {
        // assert !rowBioSequencemapByInteger.containsKey( row ) : "Already have row " + row;
        assert designElement instanceof CompositeSequence : "Got a " + designElement.getClass();

        CompositeSequence cs = ( CompositeSequence ) designElement;
        BioSequence biologicalCharacteristic = cs.getBiologicalCharacteristic();

        if ( biologicalCharacteristic == null ) {
            if ( log.isDebugEnabled() ) log.debug( "No sequence for " + designElement + ", using dummy" );
            biologicalCharacteristic = getDummySequence( cs ); // this is guaranteed to be unique for this desgin
            // element.
        }

        boolean isNew = false;

        Integer actualRow = row;

        if ( log.isDebugEnabled() ) log.debug( "Seeking row for " + designElement );

        if ( rowBioSequenceMap.containsKey( biologicalCharacteristic ) ) {

            if ( log.isDebugEnabled() )
                log.debug( "Already have a row(s) for " + biologicalCharacteristic + ": "
                        + rowBioSequenceMap.get( biologicalCharacteristic ) );

            /*
             * then add it to one of the existing rows for this sequence OR create a new row, if the existing row is for
             * the SAME array design.
             */

            Collection<Integer> existingRowsForSequence = rowBioSequenceMap.get( biologicalCharacteristic );
            boolean foundRowToAddTo = false;
            for ( Integer candidateRowToAddTo : existingRowsForSequence ) {
                if ( log.isDebugEnabled() ) log.debug( "Checking if we can add to row " + candidateRowToAddTo );
                Collection<DesignElement> des = rowDesignElementMapByInteger.get( candidateRowToAddTo );

                // First look for a row we can add it to.
                for ( DesignElement element : des ) {
                    if ( log.isDebugEnabled() )
                        log.debug( "Row " + candidateRowToAddTo + ": " + element + " uses same sequence, checking..." );
                    if ( !element.getArrayDesign().equals( designElement.getArrayDesign() ) ) {
                        // make sure there isn't already an entry in the CURRENT array design for that row.
                        for ( DesignElement checkOnSameArray : rowDesignElementMapByInteger.get( candidateRowToAddTo ) ) {
                            if ( !checkOnSameArray.getArrayDesign().equals( designElement.getArrayDesign() ) ) {
                                if ( log.isDebugEnabled() ) log.debug( "Can add to row " + candidateRowToAddTo );
                                actualRow = candidateRowToAddTo;
                                foundRowToAddTo = true;
                            } else {
                                if ( log.isDebugEnabled() )
                                    log.debug( "Can't add to row because it's on the same array" );
                            }
                        }

                    } else {
                        if ( log.isDebugEnabled() )
                            log.debug( "Element is on the same array as " + designElement + "("
                                    + designElement.getArrayDesign() + ")" );
                    }
                }
            }

            if ( !foundRowToAddTo ) {
                if ( log.isDebugEnabled() ) log.debug( "Couldn't add " + designElement + " to an existing row." );
                isNew = true;
            }

        } else {
            if ( log.isDebugEnabled() ) log.debug( "No row for " + biologicalCharacteristic + " yet" );
            rowBioSequenceMap.put( biologicalCharacteristic, new LinkedHashSet<Integer>() );
            isNew = true;
        }

        if ( log.isDebugEnabled() ) log.debug( "Adding " + designElement + " to row " + actualRow );
        if ( !rowBioSequencemapByInteger.containsKey( actualRow ) ) {
            rowDesignElementMapByInteger.put( actualRow, new HashSet<DesignElement>() );
        }

        rowDesignElementMapByInteger.get( actualRow ).add( designElement );
        rowBioSequenceMap.get( biologicalCharacteristic ).add( actualRow );
        rowBioSequencemapByInteger.put( actualRow, biologicalCharacteristic );
        rowElementMap.put( designElement, actualRow );

        return isNew;
    }

    /**
     * @param designElement
     * @return
     */
    protected BioSequence getDummySequence( CompositeSequence designElement ) {
        BioSequence biologicalCharacteristic;
        biologicalCharacteristic = BioSequence.Factory.newInstance();
        biologicalCharacteristic.setName( "Dummy biosequence for " + designElement );
        return biologicalCharacteristic;
    }

    /**
     * Select vectors from a SINGLE bioassay dimension.
     * 
     * @param vectors
     * @param bioAssayDimension
     * @param quantitationType
     * @return
     */
    protected Collection<DesignElementDataVector> selectVectors( Collection<DesignElementDataVector> vectors,
            BioAssayDimension bioAssayDimension, QuantitationType quantitationType ) {
        Collection<DesignElementDataVector> vectorsOfInterest = new LinkedHashSet<DesignElementDataVector>();
        int i = 0;

        for ( DesignElementDataVector vector : vectors ) {
            QuantitationType vectorQuantitationType = vector.getQuantitationType();
            BioAssayDimension cand = vector.getBioAssayDimension();
            if ( vectorQuantitationType.equals( quantitationType ) && cand.equals( bioAssayDimension ) ) {
                vectorsOfInterest.add( vector );
                DesignElement designElement = vector.getDesignElement();
                bioAssayDimensions.add( vector.getBioAssayDimension() );
                boolean addedRow = addToRowMaps( i, designElement );
                if ( addedRow ) i++;
            }

        }
        return vectorsOfInterest;
    }

    /**
     * Deals with the fact that the bioassay dimensions can vary in size, and don't even need to overlap in the
     * biomaterials used. In the case where there is a single bioassaydimension this reduces to simply associating each
     * column with a bioassay (though we are forced to use an integer under the hood).
     * <p>
     * For example, in the following diagram "-" indicates a biomaterial, while "*" indicates a bioassay. Each row of
     * "*" indicates samples run on a different microarray design (a different bio assay material). In the examples we
     * assume there is just a single biomaterial dimension.
     * 
     * <pre>
     *                                                    ----------------
     *                                                    ******              -- only a few samples run on this platform
     *                                                      **********        -- ditto
     *                                                                ****    -- these samples were not run on any of the other platforms (rare but possible).
     * </pre>
     * 
     * <p>
     * A simpler case:
     * </p>
     * 
     * <pre>
     *                                                    ----------------
     *                                                    ****************
     *                                                    ************
     *                                                    ********
     * </pre>
     * 
     * <p>
     * A more typical and easy case (one microarray design used):
     * </p>
     * 
     * <pre>
     *                                                    -----------------
     *                                                    *****************
     * </pre>
     * 
     * <p>
     * If every sample was run on two different array designs:
     * </p>
     * 
     * <pre>
     *                                                    -----------------
     *                                                    *****************
     *                                                    *****************
     * </pre>
     * 
     * <p>
     * Clearly the first case is the only challenge. Because there can be limited or no overlap between the bioassay
     * dimensions,we cannot assume the dimensions of the matrix will be defined by the longest bioassaydimension.
     * </p>
     * 
     * @return int
     */
    protected int setUpColumnElements() {
        log.debug( "Setting up column elements" );
        assert this.bioAssayDimensions != null && this.bioAssayDimensions.size() > 0 : "No bioAssayDimensions defined";

        /*
         * build a map of biomaterials to bioassays. Because there can be more than one biomaterial used per bioassay,
         * we group them together. Each bioMaterialGroup corresponds to a single column in the matrix.
         */
        Map<BioMaterial, Collection<BioAssay>> bioMaterialMap = new LinkedHashMap<BioMaterial, Collection<BioAssay>>();
        Collection<Collection<BioMaterial>> bioMaterialGroups = new LinkedHashSet<Collection<BioMaterial>>();
        for ( BioAssayDimension dimension : this.bioAssayDimensions ) {
            log.debug( "Processing: " + dimension );
            for ( BioAssay ba : dimension.getBioAssays() ) {
                log.debug( " Processing " + ba );
                Collection<BioMaterial> bioMaterials = ba.getSamplesUsed();

                log.debug( " .... " + bioMaterials );
                if ( !alreadySeenGroup( bioMaterialGroups, bioMaterials ) ) {
                    log.debug( "New group " + bioMaterials );
                    bioMaterialGroups.add( bioMaterials );
                }

                for ( BioMaterial material : bioMaterials ) {
                    log.debug( "  Processing " + material );
                    if ( !bioMaterialMap.containsKey( material ) ) {
                        bioMaterialMap.put( material, new HashSet<BioAssay>() );
                    }
                    bioMaterialMap.get( material ).add( ba );
                }
            }
        }

        int column = 0;
        for ( Collection<BioMaterial> bms : bioMaterialGroups ) {
            for ( BioMaterial bioMaterial : bms ) {
                for ( BioAssay assay : bioMaterialMap.get( bioMaterial ) ) {
                    if ( this.columnBioMaterialMap.containsKey( bioMaterial ) ) {
                        int columnIndex = columnBioMaterialMap.get( bioMaterial );
                        this.columnAssayMap.put( assay, columnIndex );
                        log.debug( assay + " --> column " + columnIndex );

                        if ( columnBioAssayMapByInteger.get( columnIndex ) == null ) {
                            columnBioAssayMapByInteger.put( columnIndex, new HashSet<BioAssay>() );
                        }
                        columnBioAssayMapByInteger.get( columnIndex ).add( assay );
                    } else {
                        log.debug( bioMaterial + " --> column " + column );
                        log.debug( assay + " --> column " + column );
                        this.columnBioMaterialMap.put( bioMaterial, column );
                        this.columnAssayMap.put( assay, column );
                        if ( columnBioAssayMapByInteger.get( column ) == null ) {
                            columnBioAssayMapByInteger.put( column, new HashSet<BioAssay>() );
                        }

                        // FIXME This should be a collection of biomaterials. See bug 629.
                        columnBioMaterialMapByInteger.put( column, bioMaterial );
                        columnBioAssayMapByInteger.get( column ).add( assay );
                    }
                }

            }
            column++;
        }

        assert bioMaterialGroups.size() == columnBioMaterialMapByInteger.keySet().size();
        return columnBioMaterialMapByInteger.keySet().size();
    }

    /**
     * Determine if the bioMaterial group has already been seen; this is necessary because it is possible to have
     * multiple bioassays use the same biomaterials.
     * 
     * @param bioMaterialGroups
     * @param candidateGroup
     * @return
     */
    private boolean alreadySeenGroup( Collection<Collection<BioMaterial>> bioMaterialGroups,
            Collection<BioMaterial> candidateGroup ) {
        assert candidateGroup.size() > 0;
        for ( Collection<BioMaterial> existingGroup : bioMaterialGroups ) {
            boolean alreadyIn = true;
            for ( BioMaterial candidateMember : candidateGroup ) {
                boolean contained = false;
                for ( BioMaterial existing : existingGroup ) {
                    if ( existing.equals( candidateMember ) ) {
                        contained = true;
                        break;
                    }
                }
                if ( !contained ) {
                    // if ( !existingGroup.contains( candidateMember ) ) { // for some reason this does not work.
                    // log.debug( existingGroup + " does not contain " + candidateMember );
                    alreadyIn = false; // try the next group.
                    break;
                }
            }
            if ( alreadyIn ) return true;
        }
        return false;
    }

    protected abstract void vectorsToMatrix( Collection<DesignElementDataVector> vectors );
}
