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
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Collection;
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

    protected AnalysisDaoBase( Class<T> elementClass, SessionFactory sessionFactory ) {
        super( elementClass, sessionFactory );
    }

    /**
     * @see AnalysisDao#findByInvestigation(ubic.gemma.model.analysis.Investigation)
     */
    @Override
    public abstract Collection<T> findByInvestigation( final Investigation investigation );

    /**
     * @see AnalysisDao#findByInvestigations(java.util.Collection)
     */
    @Override
    public abstract Map<Investigation, Collection<T>> findByInvestigations(
            final Collection<Investigation> investigations );

    /**
     * @see AnalysisDao#findByTaxon(ubic.gemma.model.genome.Taxon)
     */
    @Override
    public abstract Collection<T> findByTaxon( final Taxon taxon );

}