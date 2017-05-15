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
package ubic.gemma.core.analysis.preprocess;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedProcessedVectorComputationEvent;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;

/**
 * Compute the "processed" expression data vectors with the rank information filled in.
 *
 * @author pavlidis
 * @author raymond
 */
@Component

public class ProcessedExpressionDataVectorCreateServiceImpl implements ProcessedExpressionDataVectorCreateService {

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private ProcessedExpressionDataVectorCreateHelperService helperService;

    @Override
    public void computeProcessedExpressionData( ExpressionExperiment ee ) {
        try {

            // transaction
            ee = helperService.createProcessedExpressionData( ee );

            assert ee.getNumberOfDataVectors() != null;

            // transaction. We load the vectors again because otherwise we have a long dirty check? See bug 3597
            helperService.updateRanks( ee );

            assert ee.getNumberOfDataVectors() != null;

        } catch ( Exception e ) {
            auditTrailService.addUpdateEvent( ee, FailedProcessedVectorComputationEvent.Factory.newInstance(),
                    ExceptionUtils.getStackTrace( e ) );
            throw new RuntimeException( e );
        }

    }

    @Override
    public void createProcessedDataVectors( ExpressionExperiment ee, Collection<ProcessedExpressionDataVector> vecs ) {
        try {

            // transaction
            ee = helperService.createProcessedDataVectors( ee, vecs );

            assert ee.getNumberOfDataVectors() != null;

            // transaction. We load the vectors again because otherwise we have a long dirty check? See bug 3597
            helperService.updateRanks( ee );

            assert ee.getNumberOfDataVectors() != null;

        } catch ( Exception e ) {
            auditTrailService.addUpdateEvent( ee, FailedProcessedVectorComputationEvent.Factory.newInstance(),
                    ExceptionUtils.getStackTrace( e ) );
            throw new RuntimeException( e );
        }

    }

    @Override
    public void reorderByDesign( Long eeId ) {
        this.helperService.reorderByDesign( eeId );
    }

}
