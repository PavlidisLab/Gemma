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
import java.util.Map;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.genome.Gene;

/**
 * @author kelsey
 * @version $Id$
 */
public interface BioSequenceService {

    /**
     * 
     */
    public java.lang.Integer countAll();

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public java.util.Collection<BioSequence> create( java.util.Collection<BioSequence> bioSequences );

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public BioSequence create( BioSequence bioSequence );

    /**
     * 
     */
    public ubic.gemma.model.genome.biosequence.BioSequence find(
            ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

    /**
     * 
     */
    public ubic.gemma.model.genome.biosequence.BioSequence findByAccession(
            ubic.gemma.model.common.description.DatabaseEntry accession );

    /**
     * <p>
     * Returns matching biosequences for the given genes in a Map (gene to a collection of biosequences). Genes which
     * had no associated sequences are not included in the result.
     * </p>
     */
    public Map<Gene, Collection<BioSequence>> findByGenes( java.util.Collection<Gene> genes );

    /**
     * <p>
     * Retrieve all biosequences with names matching the given string. This matches only the name field, not the
     * accession.
     * </p>
     */
    public java.util.Collection<BioSequence> findByName( java.lang.String name );

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public java.util.Collection<BioSequence> findOrCreate( java.util.Collection<BioSequence> bioSequences );

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public ubic.gemma.model.genome.biosequence.BioSequence findOrCreate(
            ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

    /**
     * 
     */
    public java.util.Collection<Gene> getGenesByAccession( java.lang.String search );

    /**
     * 
     */
    public java.util.Collection<Gene> getGenesByName( java.lang.String search );

    /**
     * 
     */
    public ubic.gemma.model.genome.biosequence.BioSequence load( long id );

    /**
     * <p>
     * loads all biosequences specified by the provided ids.
     * </p>
     */
    public java.util.Collection<BioSequence> loadMultiple( java.util.Collection<Long> ids );

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public void remove( ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

    /**
     * 
     */
    public Collection<BioSequence> thaw( java.util.Collection<BioSequence> bioSequences );

    /**
     * 
     */
    public BioSequence thaw( ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public void update( java.util.Collection<BioSequence> bioSequences );

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public void update( ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

}
