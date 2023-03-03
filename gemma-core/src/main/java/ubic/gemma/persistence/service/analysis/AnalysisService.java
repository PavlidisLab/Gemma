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

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Taxon;

import java.util.Collection;
import java.util.Map;

/**
 * Provides basic services for dealing with analyses
 *
 * @author Gemma
 */
@SuppressWarnings("unused") // Possible external use
public interface AnalysisService<T extends Analysis> {

    /**
     * @param toDelete deletes the given analysis from the system
     */
    @Secured({ "GROUP_USER", "ACL_ANALYSIS_EDIT" })
    void remove( T toDelete );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<T> findByName( String name );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<T> findByTaxon( Taxon taxon );

    /**
     * Not secured: for internal use only
     *
     * @param idsToFilter starting list of bioassayset ids.
     * @return the ones which have a coexpression analysis.
     */
    Collection<Long> getExperimentsWithAnalysis( Collection<Long> idsToFilter );

    /**
     * Not secured: for internal use only
     *
     * @param taxon taxon
     * @return ids of bioassaysets from the given taxon that have a coexpression analysis
     */
    Collection<Long> getExperimentsWithAnalysis( Taxon taxon );

    /**
     * @param id id
     * @return the analysis with the specified ID
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    T load( Long id );

    /**
     * @return all of the analysis objects
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<T> loadAll();

}
