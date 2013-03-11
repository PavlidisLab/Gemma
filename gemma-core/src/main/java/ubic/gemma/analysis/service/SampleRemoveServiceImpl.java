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

import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.analysis.preprocess.SampleCoexpressionMatrixService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.ProcessedVectorComputationEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.SampleRemovalEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.SampleRemovalReversionEvent;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Service for removing sample(s) from an expression experiment. This can be done in the interest of quality control, so
 * we treat this synonymous with "outler removal".
 * <p>
 * This does not actually remove the samples. It just replaces the data in the processed data with "missing values".
 * This means the data are only recoverable by regenerating the processed data from the raw data
 * 
 * @author pavlidis
 * @version $Id$
 */
@Service
public class SampleRemoveServiceImpl extends ExpressionExperimentVectorManipulatingService implements
        SampleRemoveService {

    private static Log log = LogFactory.getLog( SampleRemoveServiceImpl.class.getName() );

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private SampleCoexpressionMatrixService sampleSoexpressionMatrixService;

    @Autowired
    private ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.service.SampleRemoveService#markAsMissing(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    @Override
    public void markAsMissing( BioAssay bioAssay ) {
        Collection<BioAssay> bms = new HashSet<BioAssay>();
        bms.add( bioAssay );
        bioAssayService.thaw( bioAssay );
        ExpressionExperiment expExp = expressionExperimentService.findByBioMaterial( bioAssay.getSamplesUsed()
                .iterator().next() );
        assert expExp != null;
        this.markAsMissing( expExp, bms );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.service.SampleRemoveService#markAsMissing(ubic.gemma.model.expression.experiment.
     * ExpressionExperiment, ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    @Override
    public void markAsMissing( ExpressionExperiment expExp, BioAssay bioAssay ) {
        Collection<BioAssay> bms = new HashSet<BioAssay>();
        bms.add( bioAssay );
        this.markAsMissing( expExp, bms );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.service.SampleRemoveService#unmarkAsMissing(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    @Override
    public void unmarkAsMissing( BioAssay bioAssay ) {
        if ( bioAssay.getIsOutlier() != null && !bioAssay.getIsOutlier() ) {
            throw new IllegalArgumentException( "Sample is not already marked as an outlier, can't revert." );
        }

        // Rather long transaction.
        bioAssay.setIsOutlier( false );
        bioAssayService.update( bioAssay );

        ExpressionExperiment expExp = expressionExperimentService.findByBioMaterial( bioAssay.getSamplesUsed()
                .iterator().next() );
        assert expExp != null;

        auditTrailService.addUpdateEvent( bioAssay, SampleRemovalReversionEvent.Factory.newInstance(), "" );

        Collection<ProcessedExpressionDataVector> vecs = processedExpressionDataVectorCreateService
                .computeProcessedExpressionData( expExp );

        sampleSoexpressionMatrixService.create( expExp, vecs );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.service.SampleRemoveService#markAsMissing(ubic.gemma.model.expression.experiment.
     * ExpressionExperiment, java.util.Collection)
     */
    @Override
    public void markAsMissing( ExpressionExperiment expExp, Collection<BioAssay> assaysToRemove ) {

        if ( assaysToRemove == null || assaysToRemove.size() == 0 ) return;

        // thaw vectors for each QT
        expExp = expressionExperimentService.thawLite( expExp );

        Collection<ProcessedExpressionDataVector> oldVectors = processedExpressionDataVectorService
                .getProcessedDataVectors( expExp );

        if ( oldVectors.isEmpty() ) {
            // this should not happen.
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
        sampleSoexpressionMatrixService.create( expExp, oldVectors );

        for ( BioAssay ba : assaysToRemove ) {
            ba.setIsOutlier( true );
            bioAssayService.update( ba );
            audit( ba, "Sample " + ba.getName() + " marked as missing data." );
        }

        auditTrailService.addUpdateEvent( expExp, ProcessedVectorComputationEvent.Factory.newInstance(),
                "Updated as part of outlier handling." );

        auditTrailService.addUpdateEvent( expExp, SampleRemovalEvent.Factory.newInstance(), assaysToRemove.size()
                + " flagged as outliers: " + StringUtils.join( assaysToRemove, "," ) );
    }

    /**
     * @param arrayDesign
     */
    private void audit( BioAssay bioAssay, String note ) {
        AuditEventType eventType = SampleRemovalEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( bioAssay, eventType, note );
    }

}
