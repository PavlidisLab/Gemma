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

package ubic.gemma.core.analysis.expression.diff;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.common.protocol.ProtocolDao;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSubSetService;

/**
 * Transactional methods for dealing with differential expression analyses.
 *
 * @author Paul
 */
@Service

public class DifferentialExpressionAnalysisHelperServiceImpl implements DifferentialExpressionAnalysisHelperService {

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;

    @Autowired
    private ProtocolDao protocolDao;

    @Autowired
    private ExpressionExperimentSubSetService expressionExperimentSubSetService;

    @Override
    @Transactional
    public DifferentialExpressionAnalysis persistStub( DifferentialExpressionAnalysis entity ) {
        if ( entity.getProtocol() != null ) {
            // Protocols are not shared across analyses (per PP2017 comment); always create.
            // Inlined from CommonPersister.persistProtocol during the persister sweep.
            entity.setProtocol( protocolDao.create( entity.getProtocol() ) );
        }

        // Sometimes we have made a new EESubSet as part of the analysis.
        // Persister-shrink S1: replaces persisterHelper.persist(subset) with a direct
        // call to ExpressionExperimentSubSetService.findOrCreate (matches the
        // findOrCreate semantic of the former EeWriteServiceImpl.persistExpressionExperimentSubSet).
        if ( entity.getExperimentAnalyzed() instanceof ExpressionExperimentSubSet
                && entity.getId() == null ) {
            ExpressionExperimentSubSet subset = ( ExpressionExperimentSubSet ) entity.getExperimentAnalyzed();
            entity.setExperimentAnalyzed( expressionExperimentSubSetService.findOrCreate( subset ) );
        }

        entity = differentialExpressionAnalysisService.create( entity );

        return entity;
    }

}
