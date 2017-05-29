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
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.persistence.service.VoEnabledService;
import ubic.gemma.persistence.service.analysis.expression.ExpressionExperimentSetDao;

import java.util.Collection;

/**
 * Spring Service base class for <code>ubic.gemma.model.analysis.expression.ExpressionExperimentSetService</code>,
 * provides access to all services and entities referenced by this service.
 *
 * @see ExpressionExperimentSetService
 */
public abstract class ExpressionExperimentSetServiceBase
        extends VoEnabledService<ExpressionExperimentSet, ExpressionExperimentSetValueObject>
        implements ExpressionExperimentSetService {

    public ExpressionExperimentSetServiceBase( ExpressionExperimentSetDao mainDao ) {
        super( mainDao );
    }

    /**
     * @see ExpressionExperimentSetService#findByName(String)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSet> findByName( final String name ) {
        return this.handleFindByName( name );
    }

    /**
     * @see ExpressionExperimentSetService#loadUserSets(User)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSet> loadUserSets( final User user ) {
        return this.handleLoadUserSets( user );
    }

    /**
     * Performs the core logic for {@link #findByName(String)}
     */
    protected abstract Collection<ExpressionExperimentSet> handleFindByName( String name );

    /**
     * Performs the core logic for {@link #loadUserSets(User)}
     */
    protected abstract Collection<ExpressionExperimentSet> handleLoadUserSets( User user );
}