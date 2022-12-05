/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.eventType.MissingValueAnalysisEvent;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Collection;

/**
 * @author paul
 */
@Service
public class TwoChannelMissingValueHelperServiceImpl implements TwoChannelMissingValueHelperService {

    private static final Log log = LogFactory.getLog( TwoChannelMissingValueHelperServiceImpl.class );

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Override
    @Transactional
    public Collection<RawExpressionDataVector> persist( ExpressionExperiment source,
            Collection<RawExpressionDataVector> results ) {
        expressionExperimentService.update( source );
        TwoChannelMissingValueHelperServiceImpl.log.info( "Persisting " + results.size() + " vectors ... " );
        source.getRawExpressionDataVectors().addAll( results );
        expressionExperimentService.update( source ); // this is needed to get the QT filled in properly.
        auditTrailService.addUpdateEvent( source, MissingValueAnalysisEvent.class,
                "Computed missing value data" );
        return results;

    }

}
