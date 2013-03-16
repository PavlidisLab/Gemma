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

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.userdetails.User;

import ubic.gemma.model.genome.Taxon;

/**
 * Provides basic services for dealing with analysis
 * 
 * @author Gemma
 * @version $Id$
 */
public interface AnalysisService<T extends Analysis> {

    /**
     * <p>
     * deletes the given analysis from the system
     * </p>
     */
    @Secured({ "GROUP_USER", "ACL_ANALYSIS_EDIT" })
    public void delete( T toDelete );

    /**
     * <p>
     * find all the analyses that involved the given investigation
     * </p>
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<T> findByInvestigation( Investigation investigation );

    /**
     * <p>
     * Given a collection of investigations returns a Map of Analysis --> collection of Investigations
     * </p>
     * <p>
     * The collection of investigations returned by the map will include all the investigations for the analysis key iff
     * one of the investigations for that analysis was in the given collection started with
     * </p>
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_MAP_READ" })
    public java.util.Map<Investigation, Collection<T>> findByInvestigations(
            java.util.Collection<? extends Investigation> investigations );

    /**
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<T> findByName( java.lang.String name );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<T> findByParentTaxon( Taxon taxon );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<T> findByTaxon( Taxon taxon );

    /**
     * <p>
     * An analysis is uniquely determined by its set of investigations. Only returns an analyis if the collection of
     * investigations given exacly matches other wise returns null
     * </p>
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public T findByUniqueInvestigations( Collection<? extends Investigation> investigations );

    /**
     * <p>
     * Returns the analysis with the specified ID
     * </p>
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public T load( java.lang.Long id );

    /**
     * <p>
     * Returns all of the analysis objects
     * </p>
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<T> loadAll();

    /**
     * Returns the {@link Analyses}s for the currently logged in {@link User} - i.e, ones for which the current user has
     * specific write permissions on (as opposed to analyses which are public) and which are "Enabled". Important: This
     * method will return all analyses if security is not enabled.
     * <p>
     * Implementation note: Via a methodInvocationFilter. See AclAfterFilterCollectionForMyData for
     * processConfigAttribute. (in Gemma-core)
     * 
     * @return
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_FILTER_MY_DATA" })
    public Collection<T> loadMyAnalyses();

    /**
     * Returns the {@link Analyses}s for the currently logged in {@link User} - i.e, ones for which the current user has
     * specific read permissions on (as opposed to analyses which are public) and which are "Enabled". Important: This
     * method will return all analyses if security is not enabled.
     * <p>
     * Implementation note: Via a methodInvocationFilter. See AclAfterFilterCollectionForMyPrivateData for
     * processConfigAttribute. (in Gemma-core)
     * 
     * @return
     */
    @Secured({ "GROUP_USER", "AFTER_ACL_FILTER_MY_SHARED_DATA" })
    public Collection<T> loadMySharedAnalyses();
}
