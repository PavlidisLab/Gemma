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
package ubic.gemma.persistence.service.analysis;

import org.hibernate.SessionFactory;
import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.analysis.Analysis</code>.
 * </p>
 *
 * @see ubic.gemma.model.analysis.Analysis
 */
public abstract class AnalysisDaoBase<T extends Analysis> extends AbstractDao<T> implements AnalysisDao<T> {

    private Class<T> analysisClass;

    protected AnalysisDaoBase( Class<T> elementClass, SessionFactory sessionFactory ) {
        super( elementClass, sessionFactory );
        this.analysisClass = elementClass;
    }

    @Override
    public Collection<T> findByInvestigation( final Investigation investigation ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                "select an  from " + this.analysisClass.getSimpleName() + " an where an.experimentAnalyzed = :ee" )
                .setParameter( "ee", investigation ).list();
    }

    @Override
    public Map<Investigation, Collection<T>> findByInvestigations( final Collection<Investigation> investigations ) {
        Map<Investigation, Collection<T>> results = new HashMap<>();

        for ( Investigation ee : investigations ) {
            results.put( ee, this.findByInvestigation( ee ) );
        }

        return results;
    }

    @Override
    public Collection<T> findByName( String name ) {
        return this.findByProperty( "name", name );
    }

    @Override
    public Collection<T> findByTaxon( final Taxon taxon ) {
        //language=HQL
        final String queryString = "select distinct an from " + this.analysisClass.getSimpleName()
                + " an inner join an.experimentAnalyzed ee " + "inner join ee.bioAssays ba "
                + "inner join ba.sampleUsed sample where sample.sourceTaxon = :taxon ";
        //noinspection unchecked
        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    @Override
    public void removeForExperiment( ExpressionExperiment ee ) {
        this.remove( this.findByInvestigation( ee ) );
    }

}