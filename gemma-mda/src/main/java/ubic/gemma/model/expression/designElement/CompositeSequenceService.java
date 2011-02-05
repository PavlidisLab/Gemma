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
package ubic.gemma.model.expression.designElement;

import java.util.Collection;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Gene;

/**
 * @author paul
 * @version $Id$
 */
public interface CompositeSequenceService {

    /**
     * 
     */
    public java.lang.Integer countAll();

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public java.util.Collection<CompositeSequence> create( java.util.Collection<CompositeSequence> compositeSequences );

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public ubic.gemma.model.expression.designElement.CompositeSequence create(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_ARRAYDESIGN_COLLECTION_READ" })
    public ubic.gemma.model.expression.designElement.CompositeSequence find(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_ARRAYDESIGN_COLLECTION_READ" })
    public java.util.Collection<CompositeSequence> findByBioSequence(
            ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_ARRAYDESIGN_COLLECTION_READ" })
    public java.util.Collection<CompositeSequence> findByBioSequenceName( java.lang.String name );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_ARRAYDESIGN_COLLECTION_READ" })
    public java.util.Collection<CompositeSequence> findByGene( ubic.gemma.model.genome.Gene gene );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_ARRAYDESIGN_COLLECTION_READ" })
    public java.util.Collection<CompositeSequence> findByGene( ubic.gemma.model.genome.Gene gene,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_ARRAYDESIGN_COLLECTION_READ" })
    public java.util.Collection<CompositeSequence> findByName( java.lang.String name );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_ARRAYDESIGN_COLLECTION_READ" })
    public ubic.gemma.model.expression.designElement.CompositeSequence findByName(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign, java.lang.String name );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_ARRAYDESIGN_COLLECTION_READ" })
    public java.util.Collection<CompositeSequence> findByNamesInArrayDesigns(
            java.util.Collection<String> compositeSequenceNames, java.util.Collection<ArrayDesign> arrayDesigns );

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public ubic.gemma.model.expression.designElement.CompositeSequence findOrCreate(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence );

    /**
     * <p>
     * Given a Collection of composite sequences returns of map of a compositesequence to a collection of genes
     * </p>
     */
    public java.util.Map<CompositeSequence, Collection<Gene>> getGenes(
            java.util.Collection<CompositeSequence> sequences );

    /**
     * 
     */
    public java.util.Collection<Gene> getGenes(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence );

    /**
     * <p>
     * Returns a map of CompositeSequences to PhysicalLocation to BlatAssociations at each location.
     * </p>
     */
    public java.util.Map<CompositeSequence, Collection<BioSequence2GeneProduct>> getGenesWithSpecificity(
            java.util.Collection<CompositeSequence> compositeSequences );

    /**
     * 
     */
    public java.util.Collection<Object[]> getRawSummary( java.util.Collection<CompositeSequence> compositeSequences,
            java.lang.Integer numResults );

    /**
     * 
     */
    public java.util.Collection<Object[]> getRawSummary(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign, java.lang.Integer numResults );

    /**
     * @deprecated Not used?
     */
    @Deprecated
    public java.util.Collection<Object[]> getRawSummary(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence, java.lang.Integer numResults );

    /**
     * 
     */
    public ubic.gemma.model.expression.designElement.CompositeSequence load( java.lang.Long id );

    /**
     * <p>
     * Load all compositeSequences specified by the given ids.
     * </p>
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_ARRAYDESIGN_COLLECTION_READ" })
    public java.util.Collection<CompositeSequence> loadMultiple( java.util.Collection<Long> ids );

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public void remove( java.util.Collection<CompositeSequence> sequencesToDelete );

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public void remove( ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence );

    /**
     * 
     */
    public void thaw( java.util.Collection<CompositeSequence> compositeSequences );

    /**
     * @param compositeSequence
     * @return
     */
    public CompositeSequence thaw( CompositeSequence compositeSequence );

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public void update( ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence );

}
