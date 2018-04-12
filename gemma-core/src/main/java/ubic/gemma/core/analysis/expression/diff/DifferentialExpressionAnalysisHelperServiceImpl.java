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
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetDao;

import java.util.Collection;

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
    private ExpressionAnalysisResultSetDao expressionAnalysisResultSetDao;

    @Autowired
    private Persister persisterHelper = null;

    @Override
    @Transactional
    public void addResults( DifferentialExpressionAnalysis entity,
            Collection<ExpressionAnalysisResultSet> resultSets ) {
        entity.getResultSets().addAll( resultSets );
        differentialExpressionAnalysisService.update( entity ); // could be sped up.
    }

    @Override
    @Transactional
    public ExpressionAnalysisResultSet create( ExpressionAnalysisResultSet rs ) {
        return this.expressionAnalysisResultSetDao.create( rs );
    }

    @Override
    @Transactional
    public DifferentialExpressionAnalysis persistStub( DifferentialExpressionAnalysis entity ) {
        entity.setProtocol( ( Protocol ) persisterHelper.persist( entity.getProtocol() ) );

        // Sometimes we have made a new EESubSet as part of the analysis.
        if ( ExpressionExperimentSubSet.class.isAssignableFrom( entity.getExperimentAnalyzed().getClass() )
                && entity.getId() == null ) {
            entity.setExperimentAnalyzed( ( BioAssaySet ) persisterHelper.persist( entity.getExperimentAnalyzed() ) );
        }

        entity = differentialExpressionAnalysisService.create( entity );

        return entity;
    }

}
