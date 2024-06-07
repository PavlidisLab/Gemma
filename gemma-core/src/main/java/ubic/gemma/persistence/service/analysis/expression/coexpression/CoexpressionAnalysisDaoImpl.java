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
package ubic.gemma.persistence.service.analysis.expression.coexpression;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.analysis.expression.coexpression.CoexpCorrelationDistribution;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.SingleExperimentAnalysisDaoBase;

import java.util.Collection;

import static ubic.gemma.persistence.util.QueryUtils.optimizeParameterList;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis</code>.
 * </p>
 *
 * @see ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis
 */
@Repository
public class CoexpressionAnalysisDaoImpl extends SingleExperimentAnalysisDaoBase<CoexpressionAnalysis>
        implements CoexpressionAnalysisDao {

    @Autowired
    public CoexpressionAnalysisDaoImpl( SessionFactory sessionFactory ) {
        super( CoexpressionAnalysis.class, sessionFactory );
    }

    @Override
    public CoexpCorrelationDistribution getCoexpCorrelationDistribution( ExpressionExperiment expressionExperiment ) {
        String q = "select ccd from CoexpressionAnalysis pca "
                + "join pca.coexpCorrelationDistribution ccd where pca.experimentAnalyzed = :ee";
        return ( CoexpCorrelationDistribution ) this.getSessionFactory().getCurrentSession().createQuery( q )
                .setParameter( "ee", expressionExperiment ).uniqueResult();

    }

    @Override
    public Collection<Long> getExperimentsWithAnalysis( Collection<Long> idsToFilter ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select experimentAnalyzed.id from CoexpressionAnalysis where experimentAnalyzed.id in (:ids)" )
                .setParameterList( "ids", optimizeParameterList( idsToFilter ) ).list();
    }

    @Override
    public Boolean hasCoexpCorrelationDistribution( ExpressionExperiment ee ) {
        String q = "select ccd from CoexpressionAnalysis pca "
                + "join pca.coexpCorrelationDistribution ccd where pca.experimentAnalyzed = :ee";
        return this.getSessionFactory().getCurrentSession().createQuery( q ).setParameter( "ee", ee ).uniqueResult()
                != null;
    }

}