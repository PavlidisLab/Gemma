/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
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
package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.persistence.service.AbstractVoEnabledService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;

import java.util.Collection;

/**
 * @author pavlidis
 * @see ExperimentalFactorService
 */
@Service
public class ExperimentalFactorServiceImpl
        extends AbstractVoEnabledService<ExperimentalFactor, ExperimentalFactorValueObject>
        implements ExperimentalFactorService {

    private final ExperimentalFactorDao experimentalFactorDao;
    private final DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    public ExperimentalFactorServiceImpl( ExperimentalFactorDao experimentalFactorDao,
            DifferentialExpressionAnalysisService differentialExpressionAnalysisService ) {
        super( experimentalFactorDao );
        this.experimentalFactorDao = experimentalFactorDao;
        this.differentialExpressionAnalysisService = differentialExpressionAnalysisService;
    }

    @Override
    public void delete( ExperimentalFactor experimentalFactor ) {
        /*
         * First, check to see if there are any diff results that use this factor.
         */
        Collection<DifferentialExpressionAnalysis> analyses = differentialExpressionAnalysisService
                .findByFactor( experimentalFactor );
        for ( DifferentialExpressionAnalysis a : analyses ) {
            differentialExpressionAnalysisService.remove( a );
        }
        this.experimentalFactorDao.remove( experimentalFactor );

    }

}