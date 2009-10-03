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

import ubic.gemma.persistence.BaseDao;

/**
 * @see ubic.gemma.model.analysis.Investigation
 */
public interface InvestigationDao<T extends Investigation> extends BaseDao<T> {
    /**
     * <p>
     * Does the same thing as {@link #findByInvestigator(boolean, ubic.gemma.model.common.auditAndSecurity.Contact)}
     * with an additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in {@link #findByInvestigator(int,
     * ubic.gemma.model.common.auditAndSecurity.Contact investigator)}.
     * </p>
     */
    public java.util.Collection<T> findByInvestigator( int transform, String queryString,
            ubic.gemma.model.common.auditAndSecurity.Contact investigator );

    /**
     * <p>
     * Does the same thing as {@link #findByInvestigator(ubic.gemma.model.common.auditAndSecurity.Contact)} with an
     * additional flag called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder
     * results will <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants
     * defined here then finder results <strong>WILL BE</strong> passed through an operation which can optionally
     * transform the entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public java.util.Collection<T> findByInvestigator( int transform,
            ubic.gemma.model.common.auditAndSecurity.Contact investigator );

    /**
     * <p>
     * Does the same thing as {@link #findByInvestigator(ubic.gemma.model.common.auditAndSecurity.Contact)} with an
     * additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in
     * {@link #findByInvestigator(ubic.gemma.model.common.auditAndSecurity.Contact)}.
     * </p>
     */
    public java.util.Collection<T> findByInvestigator( String queryString,
            ubic.gemma.model.common.auditAndSecurity.Contact investigator );

    /**
     * 
     */
    public java.util.Collection<T> findByInvestigator( ubic.gemma.model.common.auditAndSecurity.Contact investigator );

}
