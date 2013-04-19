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
package ubic.gemma.analysis.preprocess;

import java.util.Collection;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedProcessedVectorComputationEvent;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Compute the "processed" expression data vectors with the rank information filled in.
 * 
 * @author pavlidis
 * @author raymond
 * @version $Id$
 */
@Component
public class ProcessedExpressionDataVectorCreateServiceImpl implements ProcessedExpressionDataVectorCreateService {

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private Probe2ProbeCoexpressionService probe2ProbeCoexpressionService;

    @Autowired
    private ProcessedExpressionDataVectorCreateHelperService helperService;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService#computeProcessedExpressionData(ubic
     * .gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public Collection<ProcessedExpressionDataVector> computeProcessedExpressionData( ExpressionExperiment ee ) {
        // WARNING long transaction.
        try {
            // First transaction: Delete any existing links from previous link analyses before computing new vectors
            probe2ProbeCoexpressionService.deleteLinks( ee );

            // should also delete any differential expression analyses.

            // second transaction
            Collection<ProcessedExpressionDataVector> processedVectors = helperService
                    .createProcessedExpressionData( ee );

            // third transaction.
            return helperService.updateRanks( ee, processedVectors );

            /*
             * TODO Reorder & renormalize?
             */

        } catch ( Exception e ) {
            auditTrailService.addUpdateEvent( ee, FailedProcessedVectorComputationEvent.Factory.newInstance(),
                    ExceptionUtils.getFullStackTrace( e ) );
            throw new RuntimeException( e );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService#reorderByDesign(java.lang.Long)
     */
    @Override
    public void reorderByDesign( Long eeId ) {
        this.helperService.reorderByDesign( eeId );
    }

}
