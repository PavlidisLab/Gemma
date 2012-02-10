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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.analysis.preprocess.SampleCoexpressionMatrixService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.ProcessedVectorComputationEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.SampleRemovalEvent;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Service for removing sample(s) from an expression experiment. This can be done in the interest of quality control.
 * <p>
 * NOTE currently this does not actually remove the samples. It just replaces the data in the processed data with
 * "missing values". This means the data are only recoverable by regenerating the processed data from the raw data (note
 * that in previous version, the raw data was modified as well).
 * <p>
 * The reason we don't simply mark it as missing in the "absent-present" data (and leave the regular data alone) is that
 * for many data sets we either 1) don't already have an absent-present data type or 2) the absent-present data is not
 * used in analysis. We will probably change this behavior to preserve the data in question.
 * <p>
 * In the meantime, this should be used very judiciously!
 * 
 * @author pavlidis
 * @version $Id$
 */
@Service
public class SampleRemoveService extends ExpressionExperimentVectorManipulatingService {

    private static Log log = LogFactory.getLog( SampleRemoveService.class.getName() );

    @Autowired
    BioAssayService bioAssayService;

    @Autowired
    AuditTrailService auditTrailService;

    @Autowired
    ExpressionExperimentService expressionExperimentService;

    @Autowired
    ExpressionDataMatrixService expressionDataMatrixService;

    public void setBioAssayService( BioAssayService bioAssayService ) {
        this.bioAssayService = bioAssayService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * This does not actually remove the sample; rather, it sets all values to "missing".
     * 
     * @param expExp
     * @param bioAssay
     */
    public void markAsMissing( ExpressionExperiment expExp, BioAssay bioAssay ) {
        Collection<BioAssay> bms = new HashSet<BioAssay>();
        bms.add( bioAssay );
        this.markAsMissing( expExp, bms );
    }

    /**
     * This does not actually remove the sample; rather, it sets all values to "missing".
     * 
     * @param expExp
     * @param bioAssay
     */
    public void markAsMissing( BioAssay bioAssay ) {
        Collection<BioAssay> bms = new HashSet<BioAssay>();
        bms.add( bioAssay );
        bioAssayService.thaw( bioAssay );
        ExpressionExperiment expExp = expressionExperimentService.findByBioMaterial( bioAssay.getSamplesUsed()
                .iterator().next() );
        assert expExp != null;
        this.markAsMissing( expExp, bms );
    }

    /**
     * This does not actually remove the sample; rather, it sets all values to "missing" in the processed data.
     * 
     * @param expExp
     * @param assaysToRemove
     */
    public void markAsMissing( ExpressionExperiment expExp, Collection<BioAssay> assaysToRemove ) {

        if ( assaysToRemove == null || assaysToRemove.size() == 0 ) return;

        // thaw vectors for each QT
        expExp = expressionExperimentService.thawLite( expExp );

        Collection<ProcessedExpressionDataVector> oldVectors = processedExpressionDataVectorService
                .getProcessedDataVectors( expExp );

        if ( oldVectors.isEmpty() ) {
            throw new IllegalArgumentException(
                    "The data set must have processed data first before outliers can be marked." );
        }

        Collection<BioAssayDimension> dims = new HashSet<BioAssayDimension>();

        int count = 0;
        for ( DesignElementDataVector vector : oldVectors ) {

            BioAssayDimension bad = vector.getBioAssayDimension();

            assert vector.getQuantitationType().getRepresentation().equals( PrimitiveType.DOUBLE );

            dims.add( bad );
            List<BioAssay> vectorAssays = ( List<BioAssay> ) bad.getBioAssays();

            if ( !CollectionUtils.containsAny( vectorAssays, assaysToRemove ) ) continue;

            LinkedList<Object> data = new LinkedList<Object>();
            convertFromBytes( data, PrimitiveType.DOUBLE, vector );

            // now set data as missing.
            int i = 0;
            for ( BioAssay vecAs : vectorAssays ) {
                if ( assaysToRemove.contains( vecAs ) ) {
                    data.set( i, Double.NaN );
                }
                i++;
            }

            // convert it back.
            byte[] newDataAr = converter.toBytes( data.toArray() );
            vector.setData( newDataAr );
            if ( ++count % 5000 == 0 ) {
                log.info( "Edited " + count + " vectors ... " );
            }
        }

        log.info( "Committing changes to " + oldVectors.size() + " vectors" );

        designElementDataVectorService.update( oldVectors );

        /*
         * Update the correlation heatmaps.
         */
        sampleSoexpressionMatrixService.findOrCreate( expExp );

        for ( BioAssay ba : assaysToRemove ) {
            audit( ba, "Sample " + ba.getName() + " marked as missing data." );
        }

        auditTrailService.addUpdateEvent( expExp, ProcessedVectorComputationEvent.Factory.newInstance(),
                "Updated as part of outlier handling." );

        auditTrailService.addUpdateEvent( expExp, SampleRemovalEvent.Factory.newInstance(), assaysToRemove.size()
                + " flagged as outliers: " + StringUtils.join( assaysToRemove, "," ) );
    }

    public void setAuditTrailService( AuditTrailService auditTrailService ) {
        this.auditTrailService = auditTrailService;
    }

    @Autowired
    private SampleCoexpressionMatrixService sampleSoexpressionMatrixService;

    /**
     * @param arrayDesign
     */
    private void audit( BioAssay bioAssay, String note ) {
        AuditEventType eventType = SampleRemovalEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( bioAssay, eventType, note );
    }

}
