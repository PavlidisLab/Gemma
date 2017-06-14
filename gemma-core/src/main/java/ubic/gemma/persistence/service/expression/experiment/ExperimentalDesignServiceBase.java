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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.AbstractService;
import ubic.gemma.persistence.service.BaseDao;

/**
 * Spring Service base class for <code>ubic.gemma.model.expression.experiment.ExperimentalDesignService</code>, provides
 * access to all services and entities referenced by this service.
 * 
 * @see ExperimentalDesignService
 */
public abstract class ExperimentalDesignServiceBase extends AbstractService<ExperimentalDesign> implements ExperimentalDesignService {

    public ExperimentalDesignServiceBase( ExperimentalDesignDao mainDao ) {
        super( mainDao );
    }

    /**
     * @see ExperimentalDesignService#getExpressionExperiment(ubic.gemma.model.expression.experiment.ExperimentalDesign)
     */
    @Override
    @Transactional(readOnly = true)
    public ExpressionExperiment getExpressionExperiment( final ExperimentalDesign experimentalDesign ) {
        return this.handleGetExpressionExperiment( experimentalDesign );
    }

    /**
     * Performs the core logic for {@link #getExpressionExperiment(ExperimentalDesign)}
     */
    protected abstract ExpressionExperiment handleGetExpressionExperiment( ExperimentalDesign experimentalDesign );


}