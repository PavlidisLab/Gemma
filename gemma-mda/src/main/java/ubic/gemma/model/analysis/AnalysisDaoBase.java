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
import java.util.Map;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

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
    @Override
    public Collection<T> findByInvestigation( final Investigation investigation ) {
        return this.handleFindByInvestigation( investigation );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisDao#findByInvestigations(java.util.Collection)
     */
    @Override
    public Map<Investigation, Collection<T>> findByInvestigations( final Collection<Investigation> investigations ) {
        return this.handleFindByInvestigations( investigations );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisDao#findByParentTaxon(ubic.gemma.model.genome.Taxon)
     */
    @Override
    public Collection<T> findByParentTaxon( final Taxon taxon ) {
        return this.handleFindByParentTaxon( taxon );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisDao#findByTaxon(ubic.gemma.model.genome.Taxon)
     */
    @Override
    public Collection<T> findByTaxon( final Taxon taxon ) {
        return this.handleFindByTaxon( taxon );
    }

    /**
     * Performs the core logic for {@link #findByInvestigation(ubic.gemma.model.analysis.Investigation)}
     */
    protected abstract Collection<T> handleFindByInvestigation( Investigation investigation );

    /**
     * Performs the core logic for {@link #findByInvestigations(java.util.Collection)}
     */
    protected abstract Map<Investigation, Collection<T>> handleFindByInvestigations(
            Collection<Investigation> investigatons );

    /**
     * Performs the core logic for {@link #findByParentTaxon(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract Collection<T> handleFindByParentTaxon( Taxon taxon );

    /**
     * Performs the core logic for {@link #findByTaxon(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract Collection<T> handleFindByTaxon( Taxon taxon );

}