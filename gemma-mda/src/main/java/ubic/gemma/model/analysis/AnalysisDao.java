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

import ubic.gemma.persistence.BaseDao;

/**
 * @see ubic.gemma.model.analysis.Analysis
 * @version $Id$
 */
public interface AnalysisDao<T extends Analysis> extends BaseDao<T> {
    /**
     * 
     */
    public java.util.Collection<T> findByInvestigation( ubic.gemma.model.analysis.Investigation investigation );

    /**
     * <p>
     * Given a collection of investigations returns a Map of Analysis --> collection of Investigations
     * </p>
     * <p>
     * The collection of investigations returned by the map will include all the investigations for the analysis key iff
     * one of the investigations for that analysis was in the given collection started with
     * </p>
     */
    public java.util.Map<Investigation, Collection<T>> findByInvestigations(
            java.util.Collection<Investigation> investigations );

    /**
     * <p>
     * Does the same thing as {@link #findByName(java.lang.String)} with an additional flag called
     * <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public java.util.Collection<T> findByName( int transform, java.lang.String name );

    /**
     * <p>
     * Does the same thing as {@link #findByName(boolean, java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findByName(int, java.lang.String name)}.
     * </p>
     */
    public java.util.Collection<T> findByName( int transform, String queryString, java.lang.String name );

    /**
     * <p>
     * Returns a collection of anaylsis that have a name that starts with the given name
     * </p>
     */
    public java.util.Collection<T> findByName( java.lang.String name );

    /**
     * <p>
     * Does the same thing as {@link #findByName(java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findByName(java.lang.String)}.
     * </p>
     */
    public java.util.Collection<T> findByName( String queryString, java.lang.String name );

    /**
     * 
     */
    public java.util.Collection<T> findByParentTaxon( ubic.gemma.model.genome.Taxon taxon );

    /**
     * 
     */
    public java.util.Collection<T> findByTaxon( ubic.gemma.model.genome.Taxon taxon );

}
