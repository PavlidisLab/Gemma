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
package ubic.gemma.core.analysis.service;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.preprocess.PreprocessingException;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.model.common.auditAndSecurity.eventType.SampleRemovalEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.SampleRemovalReversionEvent;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Collection;

import static java.util.Objects.requireNonNull;

/**
 * Service for removing sample(s) from an expression experiment. This can be done in the interest of quality control, so
 * we treat this synonymous with "outlier removal".
 * This does not actually remove the samples. It just replaces the data in the processed data with "missing values".
 * This means the data are only recoverable by regenerating the processed data from the raw data
 *
 * @author pavlidis
 */
@Component
public class OutlierFlaggingServiceImpl extends ExpressionExperimentVectorManipulatingService
        implements OutlierFlaggingService {

    private static final Log log = LogFactory.getLog( OutlierFlaggingServiceImpl.class );

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private PreprocessorService preprocessorService;

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void markAsMissing( Collection<BioAssay> bioAssays ) {

        if ( bioAssays == null || bioAssays.isEmpty() )
            return;

        boolean hasNewOutliers = false;

        /*
         * FIXME: if there are two (or more) platforms, make sure we flag all bioassays that use the same biomaterial.
         * However, we are intending to turn all multiplatform datasets into single platform ones
         */
        for ( BioAssay ba : bioAssays ) {
            if ( ba.getIsOutlier() ) {
                continue;
            }
            hasNewOutliers = true;
            ba.setIsOutlier( true );
            bioAssayService.update( ba );
        }

        if ( !hasNewOutliers ) {
            //   log.info( "No new outliers." );
            return;
        }
        ExpressionExperiment expExp = requireNonNull( expressionExperimentService.findByBioAssay( bioAssays.iterator().next() ) );
        auditTrailService.addUpdateEvent( expExp, SampleRemovalEvent.class,
                bioAssays.size() + " flagged as outliers", StringUtils.join( bioAssays, "," ) );

        try {
            preprocessorService.process( expExp );
        } catch ( PreprocessingException e ) {
            OutlierFlaggingServiceImpl.log
                    .error( "Error during postprocessing, make sure additional steps are completed", e );
        }
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void unmarkAsMissing( Collection<BioAssay> bioAssays ) {
        if ( bioAssays.isEmpty() )
            return;

        boolean hasReversions = false;
        for ( BioAssay bioAssay : bioAssays ) {

            if ( !bioAssay.getIsOutlier() ) {
                continue;
            }

            // Rather long transaction.
            hasReversions = true;
            bioAssay.setIsOutlier( false );
            bioAssayService.update( bioAssay );
        }

        if ( !hasReversions ) {
            return;
        }

        ExpressionExperiment expExp = requireNonNull( expressionExperimentService.findByBioAssay( bioAssays.iterator().next() ) );
        auditTrailService.addUpdateEvent( expExp, SampleRemovalReversionEvent.class,
                "Marked " + bioAssays.size() + " bioassays as non-missing", StringUtils.join( bioAssays, "" ) );

        // several transactions
        try {
            preprocessorService.process( expExp );
        } catch ( PreprocessingException e ) {
            OutlierFlaggingServiceImpl.log
                    .error( "Error during postprocessing, make sure additional steps are completed", e );
        }
    }

}
