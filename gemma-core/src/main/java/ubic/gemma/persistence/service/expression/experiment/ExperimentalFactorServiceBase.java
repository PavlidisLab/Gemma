/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.persistence.service.VoEnabledService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisDao;

/**
 * <p>
 * Spring Service base class for <code>ExperimentalFactorService</code>, provides
 * access to all services and entities referenced by this service.
 * </p>
 *
 * @see ExperimentalFactorService
 */
public abstract class ExperimentalFactorServiceBase
        extends VoEnabledService<ExperimentalFactor, ExperimentalFactorValueObject>
        implements ExperimentalFactorService {

    final ExperimentalFactorDao experimentalFactorDao;
    final DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao;

    public ExperimentalFactorServiceBase( ExperimentalFactorDao experimentalFactorDao,
            DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao ) {
        super( experimentalFactorDao );
        this.experimentalFactorDao = experimentalFactorDao;
        this.differentialExpressionAnalysisDao = differentialExpressionAnalysisDao;
    }

    /**
     * @see ExperimentalFactorService#delete(ubic.gemma.model.expression.experiment.ExperimentalFactor)
     */
    @Override
    @Transactional
    public void delete( final ExperimentalFactor experimentalFactor ) {
        this.handleDelete( experimentalFactor );
    }

    /**
     * Performs the core logic for {@link #delete(ubic.gemma.model.expression.experiment.ExperimentalFactor)}
     */
    protected abstract void handleDelete( ExperimentalFactor experimentalFactor );

}