/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.analysis.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

/**
 * Service for removing sample(s) from an expression experiment. This can be done in the interest of quality control.
 * 
 * @spring.bean id="sampleRemoveService"
 * @spring.property ref="expressionExperimentService" name="expressionExperimentService"
 * @spring.property name="bioAssayService" ref = "bioAssayService"
 * @spring.property name="bioMaterialService" ref ="bioMaterialService"
 * @spring.property name="bioAssayDimensionService" ref="bioAssayDimensionService"
 * @author pavlidis
 * @version $Id$
 */
public class SampleRemoveService extends ExpressionExperimentVectorManipulatingService {

    private static Log log = LogFactory.getLog( SampleRemoveService.class.getName() );

    // ByteArrayConverter is stateless.
    ByteArrayConverter converter = new ByteArrayConverter();

    BioAssayDimensionService bioAssayDimensionService;
    BioAssayService bioAssayService;
    DesignElementDataVectorService designElementDataVectorService;
    BioMaterialService bioMaterialService;
    ExpressionExperimentService expressionExperimentService;

    public void setBioAssayService( BioAssayService bioAssayService ) {
        this.bioAssayService = bioAssayService;
    }

    public void setBioMaterialService( BioMaterialService bioMaterialService ) {
        this.bioMaterialService = bioMaterialService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setBioAssayDimensionService( BioAssayDimensionService bioAssayDimensionService ) {
        this.bioAssayDimensionService = bioAssayDimensionService;
    }

    /**
     * @param expExp
     * @param bm
     */
    public void remove( ExpressionExperiment expExp, BioAssay bm ) {
        Collection<BioAssay> bms = new HashSet<BioAssay>();
        bms.add( bm );
        this.remove( expExp, bms );
    }

    /**
     * @param expExp
     * @param assaysToRemove
     */
    @SuppressWarnings("unchecked")
    public void remove( ExpressionExperiment expExp, Collection<BioAssay> assaysToRemove ) {

        if ( assaysToRemove == null || assaysToRemove.size() == 0 ) return;

        // thaw vectors for each QT
        Collection<QuantitationType> qts = expressionExperimentService.getQuantitationTypes( expExp );

        Collection<ArrayDesign> arrayDesigns = expressionExperimentService.getArrayDesignsUsed( expExp );

        if ( arrayDesigns.size() > 1 ) {
            throw new IllegalArgumentException( "Cannot cope with more than one platform: merge vectors first!" );
        }

        Collection<BioAssayDimension> dims = new HashSet<BioAssayDimension>();
        for ( QuantitationType type : qts ) {
            log.info( "Removing samples for " + type );
            Collection<DesignElementDataVector> oldVectors = getVectorsForOneQuantitationType( type );
            PrimitiveType representation = type.getRepresentation();
            for ( DesignElementDataVector vector : oldVectors ) {
                BioAssayDimension bad = vector.getBioAssayDimension();
                dims.add( bad );
                List<BioAssay> vectorAssays = ( List<BioAssay> ) bad.getBioAssays();
                if ( !CollectionUtils.containsAny( vectorAssays, assaysToRemove ) ) continue;
                LinkedList<Object> data = new LinkedList<Object>();
                convertFromBytes( data, representation, vector );

                // now remove the data for the samples of interest
                int i = 0;
                for ( BioAssay vecAs : vectorAssays ) {
                    if ( assaysToRemove.contains( vecAs ) ) {
                        data.remove( i );
                    }
                    i++;
                }

                if ( data.size() == 0 ) {
                    // FIXME we removed everything!
                }

                // convert it back.
                byte[] newDataAr = converter.toBytes( data.toArray() );
                vector.setData( newDataAr );

            }

            log.info( "Updating " + oldVectors.size() + " vectors" );
            designElementDataVectorService.update( oldVectors );

        }

        for ( BioAssayDimension dim : dims ) {
            LinkedList<BioAssay> bioAssays = new LinkedList<BioAssay>( dim.getBioAssays() );
            bioAssays.removeAll( assaysToRemove );
            Collection<BioAssay> cleanedAssays = bioAssays.subList( 0, bioAssays.size() - 1 );
            dim.setBioAssays( cleanedAssays );
            log.info( "Updating " + dim );
            bioAssayDimensionService.update( dim );
        }

        for ( BioAssay ba : assaysToRemove ) {
            Collection<BioMaterial> samplesUsed = ba.getSamplesUsed();

            log.info( "Removing " + ba );
            bioAssayService.remove( ba );
            for ( BioMaterial material : samplesUsed ) {
                if ( material.getBioAssaysUsedIn().size() == 1 ) {
                    log.info( "Removing " + material );
                    bioMaterialService.remove( material );
                }
            }
        }

    }
}
