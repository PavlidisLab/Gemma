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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.preprocess.PreprocessingException;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.security.audit.payload.SampleRemovalPayload;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Service for removing sample(s) from an expression experiment. This can be done in the interest of quality control, so
 * we treat this synonymous with "outlier removal".
 * This does not actually remove the samples. It just replaces the data in the processed data with "missing values".
 * This means the data are only recoverable by regenerating the processed data from the raw data
 *
 * @author pavlidis
 */
@Component
public class OutlierFlaggingServiceImpl
        implements OutlierFlaggingService {

    private static final Log log = LogFactory.getLog( OutlierFlaggingServiceImpl.class );

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private OutlierFlaggingAuditService outlierFlaggingAuditService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private PreprocessorService preprocessorService;

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public void markAsMissing( Collection<BioAssay> bioAssays ) {

        if ( bioAssays == null || bioAssays.size() == 0 )
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
        ExpressionExperiment expExp = expressionExperimentService.findByBioAssay( bioAssays.iterator().next() );
        if ( expExp == null ) {
            throw new IllegalStateException( "Could not find experiment for bioassay " + bioAssays.iterator().next() );
        }
        // Phase C bucket 2f: typed payload via the AuditedAspect (co-bean
        // proxy hop in OutlierFlaggingAuditService). The bioassay list moves
        // from the free-form DETAIL string to AUDIT_EVENT.PAYLOAD as a typed
        // SampleRemovalPayload.
        List<String> baLabels = new ArrayList<>( bioAssays.size() );
        for ( BioAssay ba : bioAssays ) {
            baLabels.add( ba.toString() );
        }
        outlierFlaggingAuditService.recordSampleRemoval( expExp,
                bioAssays.size() + " flagged as outliers",
                new SampleRemovalPayload( baLabels ) );

        try {
            expExp = expressionExperimentService.thaw( expExp );
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

        ExpressionExperiment expExp = expressionExperimentService.findByBioAssay( bioAssays.iterator().next() );
        if ( expExp == null ) {
            throw new IllegalStateException( "Could not find experiment for bioassay " + bioAssays.iterator().next() );
        }
        // Phase C bucket 2f: typed payload via the AuditedAspect (co-bean
        // proxy hop in OutlierFlaggingAuditService).
        List<String> baLabels = new ArrayList<>( bioAssays.size() );
        for ( BioAssay ba : bioAssays ) {
            baLabels.add( ba.toString() );
        }
        outlierFlaggingAuditService.recordSampleRemovalReversion( expExp,
                "Marked " + bioAssays.size() + " bioassays as non-missing",
                new SampleRemovalPayload( baLabels ) );

        // several transactions
        try {
            expExp = expressionExperimentService.thaw( expExp );
            preprocessorService.process( expExp );
        } catch ( PreprocessingException e ) {
            OutlierFlaggingServiceImpl.log
                    .error( "Error during postprocessing, make sure additional steps are completed", e );
        }
    }

}
