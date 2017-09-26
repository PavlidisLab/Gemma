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
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;

import java.util.Collection;

/**
 * Spring Service base class for <code>ExpressionExperimentSubSetService</code>, provides access to all services and
 * entities referenced by this service.
 *
 * @see ExpressionExperimentSubSetService
 */
public abstract class ExpressionExperimentSubSetServiceBase implements ExpressionExperimentSubSetService {

    @Autowired
    private ExpressionExperimentSubSetDao expressionExperimentSubSetDao;


    @Override
    @Transactional
    public ExpressionExperimentSubSet create( final ExpressionExperimentSubSet expressionExperimentSubSet ) {
        return this.handleCreate( expressionExperimentSubSet );

    }


    @Override
    @Transactional(readOnly = true)
    public ExpressionExperimentSubSet load( final Long id ) {
        return this.handleLoad( id );

    }


    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSubSet> loadAll() {
        return this.handleLoadAll();

    }


    protected ExpressionExperimentSubSetDao getExpressionExperimentSubSetDao() {
        return this.expressionExperimentSubSetDao;
    }


    public void setExpressionExperimentSubSetDao( ExpressionExperimentSubSetDao expressionExperimentSubSetDao ) {
        this.expressionExperimentSubSetDao = expressionExperimentSubSetDao;
    }


    protected abstract ExpressionExperimentSubSet handleCreate( ExpressionExperimentSubSet expressionExperimentSubSet );


    protected abstract ExpressionExperimentSubSet handleLoad( Long id );


    protected abstract java.util.Collection<ExpressionExperimentSubSet> handleLoadAll();

}