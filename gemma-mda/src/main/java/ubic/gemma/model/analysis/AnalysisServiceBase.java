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

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.analysis.AnalysisService</code>, provides access to all services
 * and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.analysis.AnalysisService
 */
public abstract class AnalysisServiceBase<T extends Analysis> implements ubic.gemma.model.analysis.AnalysisService<T> {

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#delete(ubic.gemma.model.analysis.Analysis)
     */
    @Override
    public void delete( T toDelete ) {
        try {
            this.handleDelete( toDelete );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.AnalysisServiceException(
                    "Error performing 'ubic.gemma.model.analysis.AnalysisService.delete(ubic.gemma.model.analysis.Analysis toDelete)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#findByInvestigation(ubic.gemma.model.analysis.Investigation)
     */
    @Override
    public java.util.Collection<T> findByInvestigation( final ubic.gemma.model.analysis.Investigation investigation ) {
        try {
            return this.handleFindByInvestigation( investigation );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.AnalysisServiceException(
                    "Error performing 'ubic.gemma.model.analysis.AnalysisService.findByInvestigation(ubic.gemma.model.analysis.Investigation investigation)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#findByInvestigations(java.util.Collection)
     */
    @Override
    public java.util.Map findByInvestigations( final java.util.Collection investigations ) {
        try {
            return this.handleFindByInvestigations( investigations );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.AnalysisServiceException(
                    "Error performing 'ubic.gemma.model.analysis.AnalysisService.findByInvestigations(java.util.Collection investigations)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#findByName(java.lang.String)
     */
    @Override
    public Collection<T> findByName( final java.lang.String name ) {
        try {
            return this.handleFindByName( name );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.AnalysisServiceException(
                    "Error performing 'ubic.gemma.model.analysis.AnalysisService.findByName(java.lang.String name)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#findByTaxon(ubic.gemma.model.genome.Taxon)
     */
    @Override
    public java.util.Collection<T> findByParentTaxon( final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            return this.handleFindByParentTaxon( taxon );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.AnalysisServiceException(
                    "Error performing 'ubic.gemma.model.analysis.AnalysisService.findByParentTaxon(ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#findByTaxon(ubic.gemma.model.genome.Taxon)
     */
    @Override
    public java.util.Collection<T> findByTaxon( final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            return this.handleFindByTaxon( taxon );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.AnalysisServiceException(
                    "Error performing 'ubic.gemma.model.analysis.AnalysisService.findByTaxon(ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#findByUniqueInvestigations(java.util.Collection)
     */
    @Override
    public T findByUniqueInvestigations( final java.util.Collection investigations ) {
        try {
            return this.handleFindByUniqueInvestigations( investigations );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.AnalysisServiceException(
                    "Error performing 'ubic.gemma.model.analysis.AnalysisService.findByUniqueInvestigations(java.util.Collection investigations)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#load(java.lang.Long)
     */
    @Override
    public T load( final java.lang.Long id ) {
        try {
            return this.handleLoad( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.AnalysisServiceException(
                    "Error performing 'ubic.gemma.model.analysis.AnalysisService.load(java.lang.Long id)' --> " + th,
                    th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#loadAll()
     */
    @Override
    public java.util.Collection<T> loadAll() {
        try {
            return this.handleLoadAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.AnalysisServiceException(
                    "Error performing 'ubic.gemma.model.analysis.AnalysisService.loadAll()' --> " + th, th );
        }
    }

    /**
     * Performs the core logic for {@link #delete(ubic.gemma.model.analysis.Analysis)}
     */
    protected abstract void handleDelete( T toDelete ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByInvestigation(ubic.gemma.model.analysis.Investigation)}
     */
    protected abstract java.util.Collection<T> handleFindByInvestigation(
            ubic.gemma.model.analysis.Investigation investigation ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByInvestigations(java.util.Collection)}
     */
    protected abstract java.util.Map handleFindByInvestigations( java.util.Collection investigations )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByName(java.lang.String)}
     */
    protected abstract Collection<T> handleFindByName( java.lang.String name ) throws java.lang.Exception;

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

    /**
     * Performs the core logic for {@link #findByUniqueInvestigations(java.util.Collection)}
     */
    protected abstract T handleFindByUniqueInvestigations( Collection<Investigation> investigations )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract T handleLoad( java.lang.Long id ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection<T> handleLoadAll() throws java.lang.Exception;

}