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
package ubic.gemma.model.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.genome.Taxon;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.analysis.Analysis</code>.
 * </p>
 * 
 * @see ubic.gemma.model.analysis.Analysis
 */
public abstract class AnalysisDaoBase<T extends Analysis> extends HibernateDaoSupport implements AnalysisDao<T> {

    /**
     * @see ubic.gemma.model.analysis.AnalysisDao#findByInvestigation(ubic.gemma.model.analysis.Investigation)
     */
    public Collection<T> findByInvestigation( final Investigation investigation ) {
        try {
            return this.handleFindByInvestigation( investigation );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.analysis.AnalysisDao.findByInvestigation(Investigation investigation)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisDao#findByInvestigations(java.util.Collection)
     */
    public Map findByInvestigations( final Collection investigators ) {
        try {
            return this.handleFindByInvestigations( investigators );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.analysis.AnalysisDao.findByInvestigations(java.util.Collection investigators)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisDao#findByName(int, java.lang.String)
     */
    public Collection<T> findByName( final int transform, final String name ) {
        return this.findByName( transform, "select a from AnalysisImpl as a where a.name like :name", name );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisDao#findByName(int, java.lang.String, java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public Collection<T> findByName( final int transform, final String queryString,
            final String name ) {
        List<String> argNames = new ArrayList<String>();
        List<Object> args = new ArrayList<Object>();
        args.add( name );
        argNames.add( "name" );
        java.util.List<?> results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return ( Collection<T> ) results;
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisDao#findByName(java.lang.String)
     */
    public Collection<T> findByName( String name ) {
        return this.findByName( TRANSFORM_NONE, name );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisDao#findByName(java.lang.String, java.lang.String)
     */
    public Collection<T> findByName( final String queryString, final String name ) {
        return this.findByName( TRANSFORM_NONE, queryString, name );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisDao#findByParentTaxon(ubic.gemma.model.genome.Taxon)
     */
    public Collection<T> findByParentTaxon( final Taxon taxon ) {
        try {
            return this.handleFindByParentTaxon( taxon );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ubic.gemma.model.analysis.AnalysisDao.findByParentTaxon(ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisDao#findByTaxon(ubic.gemma.model.genome.Taxon)
     */
    public Collection<T> findByTaxon( final Taxon taxon ) {
        try {
            return this.handleFindByTaxon( taxon );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ubic.gemma.model.analysis.AnalysisDao.findByTaxon(ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * Performs the core logic for {@link #findByInvestigation(ubic.gemma.model.analysis.Investigation)}
     */
    protected abstract Collection<T> handleFindByInvestigation(
            Investigation investigation ) throws Exception;

    /**
     * Performs the core logic for {@link #findByInvestigations(java.util.Collection)}
     */
    protected abstract Map<Investigation, Collection<DifferentialExpressionAnalysis>> handleFindByInvestigations(
            Collection<Investigation> investigatons ) throws Exception;

    /**
     * Performs the core logic for {@link #findByParentTaxon(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract Collection<T> handleFindByParentTaxon( Taxon taxon )
            throws Exception;

    /**
     * Performs the core logic for {@link #findByTaxon(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract Collection<T> handleFindByTaxon( Taxon taxon )
            throws Exception;
 

}