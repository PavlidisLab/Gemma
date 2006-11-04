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

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Base class for ExpressionDataMatrix implementations.
 * 
 * @author pavlidis
 * @version $Id$
 */
abstract public class BaseExpressionDataMatrix implements ExpressionDataMatrix {

    private Log log = LogFactory.getLog( ExpressionDataDoubleMatrix.class );
    protected Collection<DesignElement> rowElements;
    protected Collection<BioAssayDimension> bioAssayDimensions;
    protected Map<BioAssay, Integer> columnAssayMap;
    protected Map<BioMaterial, Integer> columnBioMaterialMap;

    protected void init() {
        rowElements = new LinkedHashSet<DesignElement>();
        bioAssayDimensions = new HashSet<BioAssayDimension>();
        columnAssayMap = new LinkedHashMap<BioAssay, Integer>();
        columnBioMaterialMap = new LinkedHashMap<BioMaterial, Integer>();
    }

    /**
     * Returns the column map.
     * 
     * @return Map<BioAssay,Integer>
     */
    public Collection<Integer> getColumnMap() {
        return columnAssayMap.values();
    }

    /**
     * 
     */
    public Collection<DesignElement> getRowMap() {
        return this.rowElements;
    }

    /**
     * @param designElements
     * @param quantitationType
     * @return
     */
    protected Collection<DesignElementDataVector> selectVectors( Collection<DesignElement> designElements,
            QuantitationType quantitationType ) {
        Collection<DesignElementDataVector> vectorsOfInterest = new HashSet<DesignElementDataVector>();
        for ( DesignElement designElement : designElements ) {
            DesignElementDataVector vectorOfInterest = null;
            Collection<DesignElementDataVector> vectors = designElement.getDesignElementDataVectors();
            for ( DesignElementDataVector vector : vectors ) {
                QuantitationType vectorQuantitationType = vector.getQuantitationType();
                if ( vectorQuantitationType.equals( quantitationType ) ) {
                    vectorOfInterest = vector;
                    vectorsOfInterest.add( vectorOfInterest );
                    rowElements.add( designElement );
                    bioAssayDimensions.add( vector.getBioAssayDimension() );
                    break;
                }
            }
            if ( vectorOfInterest == null ) {
                log.warn( "Vector not found for quantitation type " + quantitationType.getType() + ".  Skipping ..." );
                continue;
            }
        }
        return vectorsOfInterest;
    }

    /**
     * @param expressionExperiment
     * @param quantitationType
     * @return
     */
    protected Collection<DesignElementDataVector> selectVectors( ExpressionExperiment expressionExperiment,
            QuantitationType quantitationType ) {
        Collection<DesignElementDataVector> vectors = expressionExperiment.getDesignElementDataVectors();
        return selectVectors( quantitationType, vectors );
    }

    /**
     * @param quantitationType
     * @param vectors
     * @return
     */
    protected Collection<DesignElementDataVector> selectVectors( QuantitationType quantitationType,
            Collection<DesignElementDataVector> vectors ) {
        Collection<DesignElementDataVector> vectorsOfInterest = new HashSet<DesignElementDataVector>();
        for ( DesignElementDataVector vector : vectors ) {
            QuantitationType vectorQuantitationType = vector.getQuantitationType();
            if ( vectorQuantitationType.equals( quantitationType ) ) {
                vectorsOfInterest.add( vector );
                rowElements.add( vector.getDesignElement() );
                bioAssayDimensions.add( vector.getBioAssayDimension() );
            }
        }
        return vectorsOfInterest;
    }

    /**
     * Deals with the fact that the bioassay dimensions can vary in size. In the case where there is a single
     * bioassaydimension this reduces to simply associating each column with a bioassay (though we are forced to use an
     * integer under the hood).
     */
    protected int setUpColumnElements() {
        log.debug( "Setting up column elements" );
        assert this.bioAssayDimensions != null && this.bioAssayDimensions.size() > 0;
        int maxSize = 0;
        for ( BioAssayDimension dimension : this.bioAssayDimensions ) {
            Collection<BioAssay> bioAssays = dimension.getBioAssays();
            assert bioAssays.size() > 0 : "Empty BioAssayDimension for the vectors";
            if ( bioAssays.size() > maxSize ) {
                maxSize = bioAssays.size();
            }

            /*
             * We "line up" the data so all the data for a given biomaterial shows up in the same column.
             */
            int i = 0;
            for ( BioAssay assay : dimension.getBioAssays() ) {
                for ( BioMaterial bioMaterial : assay.getSamplesUsed() ) {
                    if ( this.columnBioMaterialMap.containsKey( bioMaterial ) ) {
                        int columnIndex = columnBioMaterialMap.get( bioMaterial );
                        this.columnAssayMap.put( assay, columnIndex );
                        log.debug( assay + " --> column " + columnIndex );
                    } else {
                        log.debug( bioMaterial + " --> column " + i );
                        log.debug( assay + " --> column " + i );
                        this.columnBioMaterialMap.put( bioMaterial, i );
                        this.columnAssayMap.put( assay, i );
                    }
                }

                i++;
            }
        }
        // assert this.columnAssayMap.values().size() == maxSize : "Expected " + maxSize + " got "
        // + this.columnAssayMap.values().size();
        // assert this.columnBioMaterialMap.values().size() == maxSize;

        return maxSize;
    }

    protected abstract void vectorsToMatrix( Collection<DesignElementDataVector> vectors );
}
