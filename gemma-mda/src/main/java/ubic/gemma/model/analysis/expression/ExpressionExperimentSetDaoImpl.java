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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.analysis.expression;

import java.util.Collection;

import ubic.gemma.model.expression.experiment.BioAssaySet;

/**
 * @see ubic.gemma.model.analysis.ExpressionExperimentSet
 * @version $Id$
 * @author paul
 */
public class ExpressionExperimentSetDaoImpl extends ubic.gemma.model.analysis.expression.ExpressionExperimentSetDaoBase {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDaoBase#handleGetAnalyses(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Collection<ExpressionAnalysis> handleGetAnalyses( ExpressionExperimentSet expressionExperimentSet )
            throws Exception {
        return this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select a from ExpressionAnalysisImpl a inner join a.expressionExperimentSetAnalyzed ees where ees = :eeset ",
                        "eeset", expressionExperimentSet );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDaoBase#handleFindByName(java.lang.String)
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Collection<ExpressionExperimentSet> handleFindByName( String name ) throws Exception {
        return this.getHibernateTemplate().findByNamedParam( "from ExpressionExperimentSetImpl where name=:query",
                "query", name );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#find(ubic.gemma.model.expression.experiment.BioAssaySet)
     */
    @SuppressWarnings("unchecked")
    public Collection<ExpressionExperimentSet> find( BioAssaySet bioAssaySet ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select ees from ExpressionExperimentSetImpl ees inner join ees.experiments e where e = :ee", "ee",
                bioAssaySet );
    }

}