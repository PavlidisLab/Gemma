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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.analysis.preprocess.PreprocessingException;
import ubic.gemma.analysis.preprocess.PreprocessorService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.SampleRemovalEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.SampleRemovalReversionEvent;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Service for removing sample(s) from an expression experiment. This can be done in the interest of quality control, so
 * we treat this synonymous with "outlier removal".
 * <p>
 * This does not actually remove the samples. It just replaces the data in the processed data with "missing values".
 * This means the data are only recoverable by regenerating the processed data from the raw data
 * 
 * @author pavlidis
 * @version $Id$
 */
@Component
public class SampleRemoveServiceImpl extends ExpressionExperimentVectorManipulatingService implements
        SampleRemoveService {

    private static Log log = LogFactory.getLog( SampleRemoveServiceImpl.class );

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private PreprocessorService preprocessorService;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.service.SampleRemoveService#unmarkAsMissing(ubic.gemma.model.expression.bioAssay.BioAssay)
     */
    @Override
    public void unmarkAsMissing( Collection<BioAssay> bioAssays ) {
        if ( bioAssays.isEmpty() ) return;

        for ( BioAssay bioAssay : bioAssays ) {

            if ( bioAssay.getIsOutlier() != null && !bioAssay.getIsOutlier() ) {
                throw new IllegalArgumentException( "Sample is not already marked as an outlier, can't revert." );
            }

            // Rather long transaction.
            bioAssay.setIsOutlier( false );
            bioAssayService.update( bioAssay );
        }

        ExpressionExperiment expExp = expressionExperimentService.findByBioAssay( bioAssays.iterator().next() );
        auditTrailService.addUpdateEvent( expExp, SampleRemovalReversionEvent.Factory.newInstance(), "Marked "
                + bioAssays.size() + " bioassays as non-missing", StringUtils.join( bioAssays, "" ) );

        assert expExp != null;

        // several transactions
        try {
            preprocessorService.process( expExp );
        } catch ( PreprocessingException e ) {
            log.error( "Error during postprocessing, make sure additional steps are completed", e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.service.SampleRemoveService#markAsMissing(ubic.gemma.model.expression.experiment.
     * ExpressionExperiment, java.util.Collection)
     */
    @Override
    public void markAsMissing( Collection<BioAssay> bioAssays ) {

        if ( bioAssays == null || bioAssays.size() == 0 ) return;

        for ( BioAssay ba : bioAssays ) {
            ba.setIsOutlier( true );
            bioAssayService.update( ba );
            audit( ba, "Sample " + ba.getName() + " marked as missing data." );
        }
        ExpressionExperiment expExp = expressionExperimentService.findByBioAssay( bioAssays.iterator().next() );
        auditTrailService.addUpdateEvent( expExp, SampleRemovalEvent.Factory.newInstance(), bioAssays.size()
                + " flagged as outliers", StringUtils.join( bioAssays, "," ) );

        try {
            preprocessorService.process( expExp );
        } catch ( PreprocessingException e ) {
            log.error( "Error during postprocessing, make sure additional steps are completed", e );
        }
    }

    /**
     * @param arrayDesign
     */
    private void audit( BioAssay bioAssay, String note ) {
        AuditEventType eventType = SampleRemovalEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( bioAssay, eventType, note );
    }

}
