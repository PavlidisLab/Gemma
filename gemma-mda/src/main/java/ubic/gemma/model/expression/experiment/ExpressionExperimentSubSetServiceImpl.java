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
package ubic.gemma.model.expression.experiment;

import org.springframework.stereotype.Service;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService
 */
@Service
public class ExpressionExperimentSubSetServiceImpl extends
        ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetServiceBase {

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService#saveExpressionExperimentSubSet(ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet)
     */
    @Override
    protected ExpressionExperimentSubSet handleCreate(
            ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet expressionExperimentSubSet )
            throws java.lang.Exception {
        return this.getExpressionExperimentSubSetDao().create( expressionExperimentSubSet );
    }

    /**
     * Loads one subset, given an id
     * 
     * @return ExpressionExperimentSubSet
     */
    @Override
    protected ExpressionExperimentSubSet handleLoad( Long id ) throws java.lang.Exception {
        return this.getExpressionExperimentSubSetDao().load( id );
    }

    /**
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSetService#getAllExpressionExperimentSubSets()
     */
    @Override
    protected java.util.Collection handleLoadAll() throws java.lang.Exception {
        return this.getExpressionExperimentSubSetDao().loadAll();
    }

    @Override
    public ExpressionExperimentSubSet findOrCreate( ExpressionExperimentSubSet entity ) {
        return this.getExpressionExperimentSubSetDao().findOrCreate( entity );
    }

    @Override
    public ExpressionExperimentSubSet find( ExpressionExperimentSubSet entity ) {
        return this.getExpressionExperimentSubSetDao().find( entity );
    }

}