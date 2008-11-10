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
package ubic.gemma.model.expression.arrayDesign;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.BaseDao;

/**
 * @see ubic.gemma.model.expression.arrayDesign.ArrayDesign
 */
public interface ArrayDesignDao extends BaseDao<ArrayDesign> {
    /**
     * This constant is used as a transformation flag; entities can be converted automatically into value objects or
     * other types, different methods in a class implementing this interface support this feature: look for an
     * <code>int</code> parameter called <code>transform</code>.
     * <p/>
     * This specific flag denotes entities must be transformed into objects of type
     * {@link ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject}.
     */
    public final static int TRANSFORM_ARRAYDESIGNVALUEOBJECT = 1;

    /**
     * Converts an instance of type {@link ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject} to this DAO's
     * entity.
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesignValueObjectToEntity(
            ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject arrayDesignValueObject );

    /**
     * Copies the fields of {@link ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject} to the specified
     * entity.
     * 
     * @param copyIfNull If FALSE, the value object's field will not be copied to the entity if the value is NULL. If
     *        TRUE, it will be copied regardless of its value.
     */
    public void arrayDesignValueObjectToEntity(
            ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject sourceVO,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign targetEntity, boolean copyIfNull );

    /**
     * Converts a Collection of instances of type {@link ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject}
     * to this DAO's entity.
     */
    public void arrayDesignValueObjectToEntityCollection( java.util.Collection<ArrayDesignValueObject> instances );

    /**
     * <p>
     * returns all compositeSequences for the given arrayDesign that do not have bioSequence associations.
     * </p>
     */
    public java.util.Collection<CompositeSequence> compositeSequenceWithoutBioSequences(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * returns all compositeSequences for the given arrayDesign that do not have BLAT results.
     * </p>
     */
    public java.util.Collection<CompositeSequence> compositeSequenceWithoutBlatResults(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * returns all compositeSequences for the given arrayDesign without gene associations.
     * </p>
     */
    public java.util.Collection<CompositeSequence> compositeSequenceWithoutGenes(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * 
     */
    public java.lang.Integer countAll();

    /**
     * 
     */
    public void deleteAlignmentData( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * deletes the gene product associations on the specified array design
     * </p>
     */
    public void deleteGeneProductAssociations( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * Does the same thing as {@link #find(boolean, ubic.gemma.model.expression.arrayDesign.ArrayDesign)} with an
     * additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in {@link #find(int, ubic.gemma.model.expression.arrayDesign.ArrayDesign
     * arrayDesign)}.
     * </p>
     */
    public Object find( int transform, String queryString,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * Does the same thing as {@link #find(ubic.gemma.model.expression.arrayDesign.ArrayDesign)} with an additional flag
     * called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object find( int transform, ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * Does the same thing as {@link #find(ubic.gemma.model.expression.arrayDesign.ArrayDesign)} with an additional
     * argument called <code>queryString</code>. This <code>queryString</code> argument allows you to override the query
     * string defined in {@link #find(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}.
     * </p>
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign find( String queryString,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * 
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign find(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * 
     */
    public java.util.Collection<ArrayDesign> findByAlternateName( java.lang.String queryString );

    /**
     * <p>
     * Does the same thing as {@link #findByName(java.lang.String)} with an additional flag called
     * <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object findByName( int transform, java.lang.String name );

    /**
     * <p>
     * Does the same thing as {@link #findByName(boolean, java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findByName(int, java.lang.String name)}.
     * </p>
     */
    public Object findByName( int transform, String queryString, java.lang.String name );

    /**
     * 
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign findByName( java.lang.String name );

    /**
     * <p>
     * Does the same thing as {@link #findByName(java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findByName(java.lang.String)}.
     * </p>
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign findByName( String queryString, java.lang.String name );

    /**
     * <p>
     * Does the same thing as {@link #findByShortName(java.lang.String)} with an additional flag called
     * <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object findByShortName( int transform, java.lang.String shortName );

    /**
     * <p>
     * Does the same thing as {@link #findByShortName(boolean, java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findByShortName(int, java.lang.String shortName)}.
     * </p>
     */
    public Object findByShortName( int transform, String queryString, java.lang.String shortName );

    /**
     * 
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign findByShortName( java.lang.String shortName );

    /**
     * <p>
     * Does the same thing as {@link #findByShortName(java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findByShortName(java.lang.String)}.
     * </p>
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign findByShortName( String queryString,
            java.lang.String shortName );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(boolean, ubic.gemma.model.expression.arrayDesign.ArrayDesign)} with
     * an additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in {@link #findOrCreate(int,
     * ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)}.
     * </p>
     */
    public Object findOrCreate( int transform, String queryString,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(ubic.gemma.model.expression.arrayDesign.ArrayDesign)} with an
     * additional flag called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder
     * results will <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants
     * defined here then finder results <strong>WILL BE</strong> passed through an operation which can optionally
     * transform the entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object findOrCreate( int transform, ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(ubic.gemma.model.expression.arrayDesign.ArrayDesign)} with an
     * additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in {@link #findOrCreate(ubic.gemma.model.expression.arrayDesign.ArrayDesign)}.
     * </p>
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign findOrCreate( String queryString,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * 
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign findOrCreate(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * 
     */
    public java.util.Collection<BioAssay> getAllAssociatedBioAssays( java.lang.Long id );

    /**
     * <p>
     * Get all audit events associated with the specified arrayDesign ids.
     * </p>
     */
    public java.util.Map getAuditEvents( java.util.Collection<Long> ids );

    /**
     * 
     */
    public java.util.Collection getExpressionExperiments(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * 
     */
    public ubic.gemma.model.genome.Taxon getTaxon( java.lang.Long id );

    /**
     * 
     */
    public java.util.Map<Long, Boolean> isMerged( java.util.Collection<Long> ids );

    /**
     * 
     */
    public java.util.Map<Long, Boolean> isMergee( java.util.Collection<Long> ids );

    /**
     * 
     */
    public java.util.Map<Long, Boolean> isSubsumed( java.util.Collection<Long> ids );

    /**
     * 
     */
    public java.util.Map<Long, Boolean> isSubsumer( java.util.Collection<Long> ids );

    /**
     * <p>
     * loads all Array designs as value objects.
     * </p>
     */
    public java.util.Collection<ArrayDesignValueObject> loadAllValueObjects();

    /**
     * <p>
     * Needed because we want to lazy-load composite sequences
     * </p>
     */
    public java.util.Collection<CompositeSequence> loadCompositeSequences( java.lang.Long id );

    /**
     * <p>
     * does a 'thaw' of the given arrayDesign id, and returns the thawed arrayDesign.
     * </p>
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign loadFully( java.lang.Long id );

    /**
     * <p>
     * Given a list of AD ids (longs) returns a collection of ArrayDesign Objects
     * </p>
     */
    public java.util.Collection<ArrayDesign> load( java.util.Collection<Long> ids );

    /**
     * <p>
     * loads the Value Objects for the Array Designs specified by the input ids.
     * </p>
     */
    public java.util.Collection<ArrayDesignValueObject> loadValueObjects( java.util.Collection<Long> ids );

    /**
     * <p>
     * Function to count all composite sequences with bioSequences.
     * </p>
     */
    public long numAllCompositeSequenceWithBioSequences();

    /**
     * <p>
     * Function to return the count of all composite sequences with biosequences, given a list of array design Ids
     * </p>
     */
    public long numAllCompositeSequenceWithBioSequences( java.util.Collection<Long> ids );

    /**
     * <p>
     * Function to count all compositeSequences with blat results.
     * </p>
     */
    public long numAllCompositeSequenceWithBlatResults();

    /**
     * <p>
     * Function to return the count of all composite sequences with blat results, given a list of array design Ids
     * </p>
     */
    public long numAllCompositeSequenceWithBlatResults( java.util.Collection<Long> ids );

    /**
     * <p>
     * Function to count all compositeSequences with associated genes.
     * </p>
     */
    public long numAllCompositeSequenceWithGenes();

    /**
     * <p>
     * Function to return the count of all composite sequences with genes, given a list of array design Ids
     * </p>
     */
    public long numAllCompositeSequenceWithGenes( java.util.Collection<Long> ids );

    /**
     * <p>
     * Returns a count of the genes associated with all composite Sequences
     * </p>
     */
    public long numAllGenes();

    /**
     * <p>
     * returns a count of the unique genes associated witht the given arrayDesigns
     * </p>
     */
    public long numAllGenes( java.util.Collection<Long> ids );

    /**
     * <p>
     * returns the number of bioSequences associated with this ArrayDesign
     * </p>
     */
    public long numBioSequences( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * returns the number of BlatResults (BioSequence2GeneProduct) entries associated with this ArrayDesign id.
     * </p>
     */
    public long numBlatResults( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * 
     */
    public java.lang.Integer numCompositeSequences( java.lang.Long id );

    /**
     * <p>
     * Given an array design, returns the number of unique composite sequences from that array design that have
     * bioSequences associated with them. The bioSequences matched have a non-null sequence.
     * </p>
     */
    public long numCompositeSequenceWithBioSequences( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * Given an array design, returns the number of unique composite sequences from that array design that have blat
     * results associated with them.
     * </p>
     */
    public long numCompositeSequenceWithBlatResults( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * Given an array design, returns the number of unique composite sequences from that array design that have genes
     * associated with them.
     * </p>
     */
    public long numCompositeSequenceWithGenes( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * function to get the number of composite sequences that are aligned to a predicted gene.
     * </p>
     */
    public long numCompositeSequenceWithPredictedGene( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * function to get the number of composite sequences that are aligned to a probe-mapped region.
     * </p>
     */
    public long numCompositeSequenceWithProbeAlignedRegion(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * Returns the number of Genes associated with this ArrayDesign
     * </p>
     */
    public long numGenes( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * 
     */
    public java.lang.Integer numReporters( java.lang.Long id );

    /**
     * <p>
     * Remove all associations that this array design has with BioSequences. This is needed for cases where the original
     * import has associated the probes with the wrong sequences. A common case is for GEO data sets where the actual
     * oligonucleotide is not given. Instead the submitter provides Genbank accessions, which are misleading. This
     * method can be used to clear those until the "right" sequences can be identified and filled in. Note that this
     * does not delete the BioSequences, it just nulls the BiologicalCharacteristics of the CompositeSequences.
     * </p>
     */
    public void removeBiologicalCharacteristics( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * Unlazify associations of this object.
     * </p>
     */
    public void thaw( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * <p>
     * Perform a less intensive thaw of an array design.
     * </p>
     */
    public void thawLite( ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Converts this DAO's entity to an object of type
     * {@link ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject}.
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject toArrayDesignValueObject(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign entity );

    /**
     * Copies the fields of the specified entity to the target value object. This method is similar to
     * toArrayDesignValueObject(), but it does not handle any attributes in the target value object that are "read-only"
     * (as those do not have setter methods exposed).
     */
    public void toArrayDesignValueObject( ubic.gemma.model.expression.arrayDesign.ArrayDesign sourceEntity,
            ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject targetVO );

    /**
     * Converts this DAO's entity to a Collection of instances of type
     * {@link ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject}.
     */
    public void toArrayDesignValueObjectCollection( java.util.Collection<ArrayDesign> entities );

    /**
     * <p>
     * Test whether the candidateSubsumer subsumes the candidateSubsumee. If so, the array designs are updated to
     * reflect this fact. The boolean value returned indicates whether there was indeed a subsuming relationship found.
     * </p>
     */
    public java.lang.Boolean updateSubsumingStatus(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign candidateSubsumer,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign candidateSubsumee );

}
