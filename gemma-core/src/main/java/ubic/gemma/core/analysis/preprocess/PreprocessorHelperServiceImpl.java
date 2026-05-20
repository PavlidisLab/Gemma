/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.analysis.preprocess;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.core.analysis.preprocess.svd.SVDException;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.security.audit.AuditedOnError;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedMeanVarianceUpdateEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedPCAAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedSampleCorrelationAnalysisEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;

/**
 * Implementation of {@link PreprocessorHelperService}: a thin co-bean that
 * exists so each {@code processFor*} method is invoked through a Spring proxy
 * and the {@link AuditedOnError} aspect can intercept its catch path.
 * <p>
 * The methods previously lived as {@code private} members of
 * {@code PreprocessorServiceImpl} and were self-invoked via {@code this.} --
 * Spring AOP cannot intercept either case, so their imperative
 * {@code auditTrailService.addUpdateEvent( ee, FailedXEvent.class, ..., e )}
 * blocks could not be migrated to {@link AuditedOnError} during bucket 2e.
 * Hoisting them onto this separately-injected bean lets the aspect see them.
 *
 * <p>{@link Propagation#NEVER} matches {@code PreprocessorServiceImpl} -- these
 * diagnostic steps each manage their own transactions internally.
 */
@Service
@Transactional(propagation = Propagation.NEVER)
public class PreprocessorHelperServiceImpl implements PreprocessorHelperService {

    @Autowired
    private MeanVarianceService meanVarianceService;
    @Autowired
    private SVDService svdService;
    @Autowired
    private SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService;

    /**
     * Create the scatter plot to evaluate heteroscedasticity.
     */
    @Override
    @AuditedOnError(FailedMeanVarianceUpdateEvent.class)
    public void processForMeanVarianceRelation( ExpressionExperiment ee ) throws PreprocessingException {
        try {
            meanVarianceService.create( ee, true );
        } catch ( Exception e ) {
            throw new PreprocessingException( ee, e );
        }
    }

    @Override
    @AuditedOnError(FailedPCAAnalysisEvent.class)
    public void processForPca( ExpressionExperiment ee ) throws SVDRelatedPreprocessingException {
        try {
            svdService.svd( ee );
        } catch ( SVDException e ) {
            throw new SVDRelatedPreprocessingException( ee, e );
        }
    }

    /**
     * Create the heatmaps used to judge similarity among samples.
     */
    @Override
    @AuditedOnError(FailedSampleCorrelationAnalysisEvent.class)
    public void processForSampleCorrelation( ExpressionExperiment ee ) throws SampleCoexpressionRelatedPreprocessingException {
        try {
            sampleCoexpressionAnalysisService.compute( ee, sampleCoexpressionAnalysisService.prepare( ee ) );
        } catch ( FilteringException e ) {
            throw new FilteringRelatedPreprocessingException( ee, e );
        } catch ( Exception e ) {
            throw new SampleCoexpressionRelatedPreprocessingException( ee, e );
        }
    }
}
