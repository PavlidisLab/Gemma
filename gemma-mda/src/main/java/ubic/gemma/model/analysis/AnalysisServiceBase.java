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
        this.handleDelete( toDelete );

    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#findByInvestigation(ubic.gemma.model.analysis.Investigation)
     */
    @Override
    public java.util.Collection<T> findByInvestigation( final ubic.gemma.model.analysis.Investigation investigation ) {
        return this.handleFindByInvestigation( investigation );

    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#findByInvestigations(java.util.Collection)
     */
    @Override
    public java.util.Map<Investigation, Collection<T>> findByInvestigations(
            final java.util.Collection<? extends Investigation> investigations ) {
        return this.handleFindByInvestigations( investigations );

    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#findByName(java.lang.String)
     */
    @Override
    public Collection<T> findByName( final java.lang.String name ) {
        return this.handleFindByName( name );

    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#findByTaxon(ubic.gemma.model.genome.Taxon)
     */
    @Override
    public java.util.Collection<T> findByParentTaxon( final ubic.gemma.model.genome.Taxon taxon ) {
        return this.handleFindByParentTaxon( taxon );

    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#findByTaxon(ubic.gemma.model.genome.Taxon)
     */
    @Override
    public java.util.Collection<T> findByTaxon( final ubic.gemma.model.genome.Taxon taxon ) {
        return this.handleFindByTaxon( taxon );

    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#findByUniqueInvestigations(java.util.Collection)
     */
    @Override
    public T findByUniqueInvestigations( final java.util.Collection<? extends Investigation> investigations ) {
        return this.handleFindByUniqueInvestigations( investigations );

    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#load(java.lang.Long)
     */
    @Override
    public T load( final java.lang.Long id ) {
        return this.handleLoad( id );

    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#loadAll()
     */
    @Override
    public java.util.Collection<T> loadAll() {
        return this.handleLoadAll();

    }

    /**
     * Performs the core logic for {@link #delete(ubic.gemma.model.analysis.Analysis)}
     */
    protected abstract void handleDelete( T toDelete );

    /**
     * Performs the core logic for {@link #findByInvestigation(ubic.gemma.model.analysis.Investigation)}
     */
    protected abstract java.util.Collection<T> handleFindByInvestigation(
            ubic.gemma.model.analysis.Investigation investigation );

    /**
     * Performs the core logic for {@link #findByInvestigations(java.util.Collection)}
     */
    protected abstract java.util.Map<Investigation, Collection<T>> handleFindByInvestigations(
            java.util.Collection<? extends Investigation> investigations );

    /**
     * Performs the core logic for {@link #findByName(java.lang.String)}
     */
    protected abstract Collection<T> handleFindByName( java.lang.String name );

    /**
     * Performs the core logic for {@link #findByParentTaxon(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract java.util.Collection<T> handleFindByParentTaxon( ubic.gemma.model.genome.Taxon taxon );

    /**
     * Performs the core logic for {@link #findByTaxon(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract java.util.Collection<T> handleFindByTaxon( ubic.gemma.model.genome.Taxon taxon );

    /**
     * Performs the core logic for {@link #findByUniqueInvestigations(java.util.Collection)}
     */
    protected abstract T handleFindByUniqueInvestigations( Collection<? extends Investigation> investigations );

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract T handleLoad( java.lang.Long id );

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection<T> handleLoadAll();

}