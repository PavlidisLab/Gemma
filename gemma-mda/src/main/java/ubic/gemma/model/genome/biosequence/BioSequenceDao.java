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
package ubic.gemma.model.genome.biosequence;

import java.util.Collection;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.BaseDao;

/**
 * @see ubic.gemma.model.genome.biosequence.BioSequence
 */
public interface BioSequenceDao extends BaseDao<BioSequence> {
    /**
     * 
     */
    public java.lang.Integer countAll();

    /**
     * <p>
     * Does the same thing as {@link #find(boolean, BioSequence)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #find(int, BioSequence bioSequence)}.
     * </p>
     */
    public BioSequence find( int transform, String queryString, BioSequence bioSequence );

    /**
     * <p>
     * Does the same thing as {@link #find(BioSequence)} with an additional flag called <code>transform</code>. If this
     * flag is set to <code>TRANSFORM_NONE</code> then finder results will <strong>NOT</strong> be transformed during
     * retrieval. If this flag is any of the other constants defined here then finder results <strong>WILL BE</strong>
     * passed through an operation which can optionally transform the entities (into value objects for example). By
     * default, transformation does not occur.
     * </p>
     */
    public BioSequence find( int transform, BioSequence bioSequence );

    /**
     * <p>
     * Does the same thing as {@link #find(BioSequence)} with an additional argument called <code>queryString</code>.
     * This <code>queryString</code> argument allows you to override the query string defined in
     * {@link #find(BioSequence)} .
     * </p>
     */
    public BioSequence find( String queryString, BioSequence bioSequence );

    /**
     * 
     */
    public BioSequence find( BioSequence bioSequence );

    /**
     * <p>
     * Does the same thing as {@link #findByAccession(boolean, ubic.gemma.model.common.description.DatabaseEntry)} with
     * an additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in {@link #findByAccession(int,
     * ubic.gemma.model.common.description.DatabaseEntry accession)}.
     * </p>
     */
    public BioSequence findByAccession( int transform, String queryString,
            ubic.gemma.model.common.description.DatabaseEntry accession );

    /**
     * <p>
     * Does the same thing as {@link #findByAccession(ubic.gemma.model.common.description.DatabaseEntry)} with an
     * additional flag called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder
     * results will <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants
     * defined here then finder results <strong>WILL BE</strong> passed through an operation which can optionally
     * transform the entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public BioSequence findByAccession( int transform, ubic.gemma.model.common.description.DatabaseEntry accession );

    /**
     * <p>
     * Does the same thing as {@link #findByAccession(ubic.gemma.model.common.description.DatabaseEntry)} with an
     * additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in {@link #findByAccession(ubic.gemma.model.common.description.DatabaseEntry)}
     * .
     * </p>
     */
    public BioSequence findByAccession( String queryString, ubic.gemma.model.common.description.DatabaseEntry accession );

    /**
     * 
     */
    public BioSequence findByAccession( ubic.gemma.model.common.description.DatabaseEntry accession );

    /**
     * <p>
     * Returns matching biosequences for the given genes in a Map (gene to biosequences). Genes which had no associated
     * sequences are not included in the result.
     * </p>
     */
    public java.util.Map<Gene, Collection<BioSequence>> findByGenes( java.util.Collection<Gene> genes );

    /**
     * 
     */
    public java.util.Collection<BioSequence> findByName( java.lang.String name );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(boolean, BioSequence)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findOrCreate(int, BioSequence bioSequence)}.
     * </p>
     */
    public BioSequence findOrCreate( int transform, String queryString, BioSequence bioSequence );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(BioSequence)} with an additional flag called <code>transform</code>.
     * If this flag is set to <code>TRANSFORM_NONE</code> then finder results will <strong>NOT</strong> be transformed
     * during retrieval. If this flag is any of the other constants defined here then finder results <strong>WILL
     * BE</strong> passed through an operation which can optionally transform the entities (into value objects for
     * example). By default, transformation does not occur.
     * </p>
     */
    public BioSequence findOrCreate( int transform, BioSequence bioSequence );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(BioSequence)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findOrCreate(BioSequence)}.
     * </p>
     */
    public BioSequence findOrCreate( String queryString, BioSequence bioSequence );

    /**
     * 
     */
    public BioSequence findOrCreate( BioSequence bioSequence );

    /**
     * 
     */
    public java.util.Collection<Gene> getGenesByAccession( java.lang.String search );

    /**
     * For a biosequence name, get the genes
     */
    public java.util.Collection<Gene> getGenesByName( java.lang.String search );

    public Collection<BioSequence> load( Collection<Long> ids );

    /**
     * 
     */
    public Collection<BioSequence> thaw( java.util.Collection<BioSequence> bioSequences );

    /**
     * 
     */
    public BioSequence thaw( BioSequence bioSequence );

}
