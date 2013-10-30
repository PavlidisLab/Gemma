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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Base class for ExpressionDataMatrix implementations.
 * <p>
 * Implementation note: The underlying DoubleMatrixNamed is indexed by Integers, which are in turn mapped to BioAssays
 * etc. held here. Thus the 'names' of the underlying matrix are just numbers.
 * 
 * @author pavlidis
 * @version $Id$
 */
@SuppressWarnings("serial")
abstract public class BaseExpressionDataMatrix<T> implements ExpressionDataMatrix<T>, Serializable {

    private Log log = LogFactory.getLog( ExpressionDataDoubleMatrix.class );

    // protected LinkedHashSet<DesignElement> rowElements;

    protected Map<CompositeSequence, BioAssayDimension> bioAssayDimensions;

    protected ExpressionExperiment expressionExperiment;

    // maps for bioassays/biomaterials/columns
    protected Map<BioAssay, Integer> columnAssayMap;
    protected Map<BioMaterial, Integer> columnBioMaterialMap;
    protected Map<Integer, Collection<BioAssay>> columnBioAssayMapByInteger;
    protected Map<Integer, BioMaterial> columnBioMaterialMapByInteger;

    // maps for designelements/sequences/rows
    protected Map<CompositeSequence, Integer> rowElementMap;
    protected Map<Integer, CompositeSequence> rowDesignElementMapByInteger;

    protected Collection<QuantitationType> quantitationTypes;

    List<ExpressionDataMatrixRowElement> rowElements = null;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.datastructure.matrix.ExpressionDataMatrix#columns(ubic.gemma.model.expression.designElement.DesignElement
     * )
     */
    @Override
    public int columns( CompositeSequence el ) {
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getBestBioAssayDimension()
     */
    @Override
    public BioAssayDimension getBestBioAssayDimension() {

        Collection<BioAssayDimension> dims = new HashSet<BioAssayDimension>( this.bioAssayDimensions.values() );

        BioAssayDimension b = dims.iterator().next();
        if ( dims.size() > 1 ) {
            /* see bug 2139 */
            int s = -1;
            Collection<BioMaterial> bms = new HashSet<BioMaterial>();
            for ( BioAssayDimension bioAssayDimension : dims ) {
                if ( bioAssayDimension.getBioAssays().size() > s ) {
                    s = bioAssayDimension.getBioAssays().size();
                    b = bioAssayDimension;
                }

                for ( BioAssay ba : bioAssayDimension.getBioAssays() ) {
                    bms.add( ba.getSampleUsed() );
                }
            }

            /*
             * Sanity check: make sure all the biomaterials are accounted for by the chosen bioassaydimension.
             */
            for ( BioAssayDimension bioAssayDimension : dims ) {
                for ( BioAssay ba : bioAssayDimension.getBioAssays() ) {
                    if ( !bms.contains( ba.getSampleUsed() ) ) {
                        throw new IllegalStateException(
                                "This data set seems to require further preprocessing before it can be used for SVD; Vector merge or sample match?" );
                    }

                }
            }

        }
        return b;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getBioAssayDimension()
     */
    @Override
    public BioAssayDimension getBioAssayDimension( CompositeSequence designElement ) {

        return this.bioAssayDimensions.get( designElement );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getBioAssayForColumn(int)
     */
    @Override
    public Collection<BioAssay> getBioAssaysForColumn( int index ) {
        return this.columnBioAssayMapByInteger.get( index );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getBioMaterialForColumn(int)
     */
    @Override
    public BioMaterial getBioMaterialForColumn( int index ) {
        return this.columnBioMaterialMapByInteger.get( index );
    }

    /**
     * @param bioAssay
     * @return
     */
    public int getColumnIndex( BioAssay bioAssay ) {
        return columnAssayMap.get( bioAssay );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.datastructure.matrix.ExpressionDataMatrix#getColumnIndex(ubic.gemma.model.expression.biomaterial.
     * BioMaterial)
     */
    @Override
    public int getColumnIndex( BioMaterial bioMaterial ) {
        return columnBioMaterialMap.get( bioMaterial );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getDesignElementForRow(int)
     */
    @Override
    public CompositeSequence getDesignElementForRow( int index ) {
        return this.rowDesignElementMapByInteger.get( index );
    }

    @Override
    public ExpressionExperiment getExpressionExperiment() {
        return this.expressionExperiment;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getQuantitationTypes()
     */
    @Override
    public Collection<QuantitationType> getQuantitationTypes() {
        return quantitationTypes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRowElement(int)
     */
    public ExpressionDataMatrixRowElement getRowElement( int index ) {
        return this.getRowElements().get( index );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRowElements()
     */
    @Override
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
     * @seeubic.gemma.datastructure.matrix.ExpressionDataMatrix#getRowIndex(ubic.gemma.model.expression.designElement.
     * DesignElement)
     */
    @Override
    public int getRowIndex( CompositeSequence designElement ) {
        Integer index = rowElementMap.get( designElement );
        if ( index == null ) return -1;
        return index;
    }

    /**
     * Each row is a unique DesignElement.
     * 
     * @param rwo The row number to be used by this design element.
     * @param designElement
     */
    protected void addToRowMaps( Integer row, CompositeSequence designElement ) {
        rowDesignElementMapByInteger.put( row, designElement );
        rowElementMap.put( designElement, row );
    }

    protected void init() {
        quantitationTypes = new HashSet<QuantitationType>();
        bioAssayDimensions = new HashMap<CompositeSequence, BioAssayDimension>();

        rowElementMap = new LinkedHashMap<CompositeSequence, Integer>();
        rowDesignElementMapByInteger = new LinkedHashMap<Integer, CompositeSequence>();

        columnAssayMap = new LinkedHashMap<BioAssay, Integer>();
        columnBioMaterialMap = new LinkedHashMap<BioMaterial, Integer>();
        columnBioMaterialMapByInteger = new LinkedHashMap<Integer, BioMaterial>();
        columnBioAssayMapByInteger = new LinkedHashMap<Integer, Collection<BioAssay>>();

    }

    /**
     * Selects all the vectors passed in (uses them to initialize the data)
     * 
     * @param vectors
     */
    protected void selectVectors( Collection<? extends DesignElementDataVector> vectors ) {
        QuantitationType quantitationType = null;
        int i = 0;
        List<DesignElementDataVector> sorted = sortVectorsByDesignElement( vectors );
        for ( DesignElementDataVector vector : sorted ) {
            if ( this.expressionExperiment == null ) this.expressionExperiment = vector.getExpressionExperiment();
            QuantitationType vectorQuantitationType = vector.getQuantitationType();
            CompositeSequence designElement = vector.getDesignElement();
            this.bioAssayDimensions.put( designElement, vector.getBioAssayDimension() );
            if ( quantitationType == null ) {
                quantitationType = vectorQuantitationType;

                this.getQuantitationTypes().add( vectorQuantitationType );
            } else {
                if ( quantitationType != vectorQuantitationType ) {
                    throw new IllegalArgumentException( "Cannot pass vectors from more than one quantitation type" );
                }

            }

            addToRowMaps( i, designElement );
            i++;
        }

    }

    protected Collection<DesignElementDataVector> selectVectors( Collection<? extends DesignElementDataVector> vectors,
            Collection<QuantitationType> qTypes ) {
        this.quantitationTypes.addAll( qTypes );

        Collection<DesignElementDataVector> vectorsOfInterest = new LinkedHashSet<DesignElementDataVector>();
        int i = 0;

        for ( DesignElementDataVector vector : vectors ) {
            QuantitationType vectorQuantitationType = vector.getQuantitationType();
            if ( qTypes.contains( vectorQuantitationType ) ) {
                if ( this.expressionExperiment == null ) this.expressionExperiment = vector.getExpressionExperiment();
                vectorsOfInterest.add( vector );
                CompositeSequence designElement = vector.getDesignElement();
                bioAssayDimensions.put( designElement, vector.getBioAssayDimension() );
                addToRowMaps( i, designElement );
                this.getQuantitationTypes().add( vectorQuantitationType );
                i++;
            }

        }
        return vectorsOfInterest;
    }

    /**
     * @param vectors
     * @param bioAssayDimensions
     * @param qTypes
     * @return
     */
    protected Collection<DesignElementDataVector> selectVectors( Collection<? extends DesignElementDataVector> vectors,
            List<QuantitationType> qTypes ) {
        this.quantitationTypes.addAll( qTypes );
        List<DesignElementDataVector> sorted = sortVectorsByDesignElement( vectors );
        Collection<DesignElementDataVector> vectorsOfInterest = new LinkedHashSet<DesignElementDataVector>();
        int rowIndex = 0;
        for ( int qTypeIndex = 0; qTypeIndex < qTypes.size(); qTypeIndex++ ) {
            QuantitationType soughtType = qTypes.get( qTypeIndex );
            for ( DesignElementDataVector vector : sorted ) {
                QuantitationType vectorQuantitationType = vector.getQuantitationType();
                if ( vectorQuantitationType.equals( soughtType ) ) {
                    if ( this.expressionExperiment == null )
                        this.expressionExperiment = vector.getExpressionExperiment();
                    vectorsOfInterest.add( vector );
                    this.getQuantitationTypes().add( vectorQuantitationType );
                    CompositeSequence designElement = vector.getDesignElement();
                    this.bioAssayDimensions.put( designElement, vector.getBioAssayDimension() );
                    addToRowMaps( rowIndex, designElement );
                    rowIndex++;
                }
            }
        }
        log.debug( "Selected " + vectorsOfInterest.size() + " vectors" );
        return vectorsOfInterest;
    }

    /**
     * @param vectors
     * @param bioAssayDimension
     * @param quantitationType
     * @return
     */
    protected Collection<DesignElementDataVector> selectVectors( Collection<? extends DesignElementDataVector> vectors,
            QuantitationType quantitationType ) {
        this.quantitationTypes.add( quantitationType );

        Collection<DesignElementDataVector> vectorsOfInterest = new LinkedHashSet<DesignElementDataVector>();
        int i = 0;

        for ( DesignElementDataVector vector : vectors ) {
            QuantitationType vectorQuantitationType = vector.getQuantitationType();
            if ( vectorQuantitationType.equals( quantitationType ) ) {
                if ( this.expressionExperiment == null ) this.expressionExperiment = vector.getExpressionExperiment();
                vectorsOfInterest.add( vector );
                this.getQuantitationTypes().add( vectorQuantitationType );
                CompositeSequence designElement = vector.getDesignElement();
                bioAssayDimensions.put( designElement, vector.getBioAssayDimension() );
                addToRowMaps( i, designElement );
                i++;
            }

        }
        return vectorsOfInterest;
    }

    /**
     * @param ee
     * @param qTypes
     * @return
     */
    protected Collection<DesignElementDataVector> selectVectors( ExpressionExperiment ee,
            Collection<QuantitationType> qTypes ) {
        Collection<DesignElementDataVector> selected = new HashSet<DesignElementDataVector>();
        Collection<RawExpressionDataVector> vectors = ee.getRawExpressionDataVectors();
        this.quantitationTypes.addAll( qTypes );
        for ( QuantitationType type : qTypes ) {
            selected.addAll( this.selectVectors( type, vectors ) );
        }
        return selected;
    }

    protected Collection<DesignElementDataVector> selectVectors( ExpressionExperiment ee, List<QuantitationType> qTypes ) {

        Collection<RawExpressionDataVector> vectors = ee.getRawExpressionDataVectors();
        this.quantitationTypes.addAll( qTypes );
        Collection<DesignElementDataVector> vectorsOfInterest = selectVectors( vectors, qTypes );

        return vectorsOfInterest;
    }

    /**
     * @param ee
     * @param quantitationType
     * @return Collection<DesignElementDataVector>
     */
    protected Collection<DesignElementDataVector> selectVectors( ExpressionExperiment ee,
            QuantitationType quantitationType ) {
        Collection<RawExpressionDataVector> vectors = ee.getRawExpressionDataVectors();
        return selectVectors( quantitationType, vectors );
    }

    /**
     * @param quantitationType
     * @param vectors
     * @return Collection<DesignElementDataVector>
     */
    protected Collection<DesignElementDataVector> selectVectors( QuantitationType quantitationType,
            Collection<? extends DesignElementDataVector> vectors ) {
        Collection<DesignElementDataVector> vectorsOfInterest = new LinkedHashSet<DesignElementDataVector>();
        this.quantitationTypes.add( quantitationType );
        List<DesignElementDataVector> sorted = sortVectorsByDesignElement( vectors );
        int i = 0;
        for ( DesignElementDataVector vector : sorted ) {
            QuantitationType vectorQuantitationType = vector.getQuantitationType();
            if ( this.expressionExperiment == null ) this.expressionExperiment = vector.getExpressionExperiment();
            if ( vectorQuantitationType.equals( quantitationType ) ) {
                vectorsOfInterest.add( vector );
                CompositeSequence designElement = vector.getDesignElement();
                this.getQuantitationTypes().add( vectorQuantitationType );
                this.bioAssayDimensions.put( designElement, vector.getBioAssayDimension() );
                addToRowMaps( i, designElement );
                i++;
            }
        }
        return vectorsOfInterest;
    }

    /**
     * <p>
     * Note: In the current versions of Gemma, we require that there can be only a single bioassaydimension. Thus this
     * code is overly complex. If an experiment has multiple bioassaydimensions (due to multiple arrays), we merge the
     * vectors (e.g., needed in the last case shown below). However, the issue of having multiple "BioMaterials" per
     * "BioAssay" still exists.
     * <p>
     * Deals with the fact that the bioassay dimensions can vary in size, and don't even need to overlap in the
     * biomaterials used. In the case where there is a single bioassaydimension this reduces to simply associating each
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
     * the matrix will be defined by the longest bioassaydimension. Note that later in processing, this possible lack of
     * overlap is fixed by sample matching or vector merging; this class has to deal with the general case.
     * </p>
     * 
     * @return int
     */
    protected int setUpColumnElements() {
        log.debug( "Setting up column elements" );
        assert this.bioAssayDimensions != null && this.bioAssayDimensions.size() > 0 : "No bioAssayDimensions defined";

        Map<BioMaterial, Collection<BioAssay>> bioMaterialMap = new LinkedHashMap<>();
        for ( BioAssayDimension dimension : this.bioAssayDimensions.values() ) {
            Collection<BioAssay> bioAssays = dimension.getBioAssays(); // this should in fact be a list.
            log.debug( "Processing: " + dimension + " with " + bioAssays.size() + " assays" );
            getBioMaterialGroupsForAssays( bioMaterialMap, bioAssays );
        }

        if ( log.isDebugEnabled() ) log.debug( bioMaterialMap.size() + " biomaterialGroups (correspond to columns)" );
        int column = 0;

        for ( BioMaterial bioMaterial : bioMaterialMap.keySet() ) {
            if ( log.isDebugEnabled() ) log.debug( "Column " + column + " **--->>>> " + bioMaterial );
            for ( BioAssay assay : bioMaterialMap.get( bioMaterial ) ) {
                if ( this.columnBioMaterialMap.containsKey( bioMaterial ) ) {
                    int existingColumn = columnBioMaterialMap.get( bioMaterial );
                    this.columnAssayMap.put( assay, existingColumn );
                    if ( log.isDebugEnabled() ) log.debug( assay + " --> column " + existingColumn );

                    if ( columnBioAssayMapByInteger.get( existingColumn ) == null ) {
                        columnBioAssayMapByInteger.put( existingColumn, new HashSet<BioAssay>() );
                    }
                    columnBioAssayMapByInteger.get( existingColumn ).add( assay );
                } else {
                    if ( log.isDebugEnabled() ) {
                        log.debug( bioMaterial + " --> column " + column );
                        log.debug( assay + " --> column " + column );
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

        if ( log.isDebugEnabled() ) {
            for ( Object o : this.columnAssayMap.keySet() ) {
                log.debug( o + " " + this.columnAssayMap.get( o ) );
            }
        }

        assert bioMaterialMap.size() == columnBioMaterialMapByInteger.keySet().size();
        return columnBioMaterialMapByInteger.keySet().size();
    }

    protected abstract void vectorsToMatrix( Collection<? extends DesignElementDataVector> vectors );

    /**
     * @param bioMaterialMap
     * @param bioMaterials
     * @param bioAssays
     */
    private void getBioMaterialGroupsForAssays( Map<BioMaterial, Collection<BioAssay>> bioMaterialMap,
            Collection<BioAssay> bioAssays ) {
        for ( BioAssay ba : bioAssays ) {
            if ( log.isDebugEnabled() ) log.debug( "      " + ba );
            BioMaterial bm = ba.getSampleUsed();

            if ( !bioMaterialMap.containsKey( bm ) ) {
                bioMaterialMap.put( bm, new HashSet<BioAssay>() );
            }
            bioMaterialMap.get( bm ).add( ba );

        }
    }

    /**
     * @param vectors
     * @return
     */
    private List<DesignElementDataVector> sortVectorsByDesignElement(
            Collection<? extends DesignElementDataVector> vectors ) {
        List<DesignElementDataVector> vectorSort = new ArrayList<DesignElementDataVector>( vectors );
        Collections.sort( vectorSort, new Comparator<DesignElementDataVector>() {
            @Override
            public int compare( DesignElementDataVector o1, DesignElementDataVector o2 ) {
                return o1.getDesignElement().getName().compareTo( o2.getDesignElement().getName() );
            }
        } );
        return vectorSort;
    }

}
