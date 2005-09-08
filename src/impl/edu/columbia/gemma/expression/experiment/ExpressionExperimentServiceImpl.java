/*
 * The Gemma project.
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.expression.experiment;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.expression.experiment.ExpressionExperimentService
 */
public class ExpressionExperimentServiceImpl extends
        edu.columbia.gemma.expression.experiment.ExpressionExperimentServiceBase {

    /**
     * @see edu.columbia.gemma.expression.experiment.ExpressionExperimentService#saveExpressionExperiment(edu.columbia.gemma.expression.experiment.ExpressionExperiment)
     */
    protected void handleSaveExpressionExperiment(
            edu.columbia.gemma.expression.experiment.ExpressionExperiment expressionExperiment )
            throws java.lang.Exception {
        this.getExpressionExperimentDao().update( expressionExperiment );
    }

    /**
     * @see edu.columbia.gemma.expression.experiment.ExpressionExperimentService#getAllExpressionExperiments()
     */
    protected java.util.Collection handleGetAllExpressionExperiments() throws java.lang.Exception {
        return this.getExpressionExperimentDao().findAll();
    }

    /**
     * @see edu.columbia.gemma.expression.experiment.ExpressionExperimentService#findByInvestigator()
     */
    protected java.util.Collection handleFindByInvestigator( long id ) throws java.lang.Exception {
        return this.getExpressionExperimentDao().findByInvestigator( id );
    }

    /**
     * @see edu.columbia.gemma.expression.experiment.ExpressionExperimentService#findById()
     */
    protected ExpressionExperiment handleFindById( long id ) throws java.lang.Exception {
        return this.getExpressionExperimentDao().findById( id );
    }

    /**
     * @see edu.columbia.gemma.expression.experiment.ExpressionExperimentService#removeExpressionExperiment(edu.columbia.gemma.expression.experiment.ExpressionExperiment)
     */
    protected void handleRemoveExpressionExperiment( ExpressionExperiment expressionExperiment )
            throws java.lang.Exception {
        this.getExpressionExperimentDao().remove( expressionExperiment );
    }

    /**
     * @see edu.columbia.gemma.expression.experiment.ExpressionExperimentService#createExpressionExperiment(edu.columbia.gemma.expression.experiment.ExpressionExperiment)
     */
    protected ExpressionExperiment handleCreateExpressionExperiment( ExpressionExperiment expressionExperiment )
            throws java.lang.Exception {
        return ( ExpressionExperiment ) this.getExpressionExperimentDao().create( expressionExperiment );
    }

    /**
     * @see edu.columbia.gemma.expression.experiment.ExpressionExperimentService#updateExpressionExperiment(edu.columbia.gemma.expression.experiment.ExpressionExperiment)
     */
    protected void handleUpdateExpressionExperiment( ExpressionExperiment expressionExperiment )
            throws java.lang.Exception {
        this.getExpressionExperimentDao().update( expressionExperiment );
    }
}