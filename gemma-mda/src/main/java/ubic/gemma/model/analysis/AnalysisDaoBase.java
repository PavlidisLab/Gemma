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

import java.util.Collection;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.analysis.Analysis</code>.
 * </p>
 * 
 * @see ubic.gemma.model.analysis.Analysis
 */
public abstract class AnalysisDaoBase<T extends Analysis> extends HibernateDaoSupport implements
        ubic.gemma.model.analysis.AnalysisDao<T> {

    /**
     * @see ubic.gemma.model.analysis.AnalysisDao#findByInvestigation(ubic.gemma.model.analysis.Investigation)
     */
    public java.util.Collection<T> findByInvestigation( final ubic.gemma.model.analysis.Investigation investigation ) {
        try {
            return this.handleFindByInvestigation( investigation );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.analysis.AnalysisDao.findByInvestigation(ubic.gemma.model.analysis.Investigation investigation)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisDao#findByInvestigations(java.util.Collection)
     */
    public java.util.Map findByInvestigations( final java.util.Collection investigators ) {
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

    public java.util.Collection<T> findByName( final int transform, final java.lang.String name ) {
        return this.findByName( transform, "select a from AnalysisImpl as a where a.name like :name", name );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisDao#findByName(int, java.lang.String, java.lang.String)
     */

    @SuppressWarnings("unchecked")
    public java.util.Collection<T> findByName( final int transform, final java.lang.String queryString,
            final java.lang.String name ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( name );
        argNames.add( "name" );
        java.util.List<?> results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return ( Collection<T> ) results;
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisDao#findByName(java.lang.String)
     */
    public java.util.Collection<T> findByName( java.lang.String name ) {
        return this.findByName( TRANSFORM_NONE, name );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisDao#findByName(java.lang.String, java.lang.String)
     */
    public java.util.Collection<T> findByName( final java.lang.String queryString, final java.lang.String name ) {
        return this.findByName( TRANSFORM_NONE, queryString, name );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisDao#findByParentTaxon(ubic.gemma.model.genome.Taxon)
     */
    public java.util.Collection<T> findByParentTaxon( final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            return this.handleFindByParentTaxon( taxon );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.analysis.AnalysisDao.findByParentTaxon(ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisDao#findByTaxon(ubic.gemma.model.genome.Taxon)
     */
    public java.util.Collection<T> findByTaxon( final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            return this.handleFindByTaxon( taxon );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.analysis.AnalysisDao.findByTaxon(ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * Performs the core logic for {@link #findByInvestigation(ubic.gemma.model.analysis.Investigation)}
     */
    protected abstract java.util.Collection<T> handleFindByInvestigation(
            ubic.gemma.model.analysis.Investigation investigation ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByInvestigations(java.util.Collection)}
     */
    protected abstract java.util.Map<Investigation, Collection<DifferentialExpressionAnalysis>> handleFindByInvestigations(
            java.util.Collection<Investigation> investigatons ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByParentTaxon(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract java.util.Collection<T> handleFindByParentTaxon( ubic.gemma.model.genome.Taxon taxon )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByTaxon(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract java.util.Collection<T> handleFindByTaxon( ubic.gemma.model.genome.Taxon taxon )
            throws java.lang.Exception;
 

}