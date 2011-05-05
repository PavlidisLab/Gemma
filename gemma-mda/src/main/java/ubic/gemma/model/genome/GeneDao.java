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
package ubic.gemma.model.genome;

import java.util.Collection;
import java.util.Map;

import ubic.gemma.model.analysis.expression.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;

/**
 * @see ubic.gemma.model.genome.Gene
 */
public interface GeneDao extends ubic.gemma.model.genome.ChromosomeFeatureDao<Gene> {
    /**
     * This constant is used as a transformation flag; entities can be converted automatically into value objects or
     * other types, different methods in a class implementing this interface support this feature: look for an
     * <code>int</code> parameter called <code>transform</code>.
     * <p/>
     * This specific flag denotes entities must be transformed into objects of type
     * {@link ubic.gemma.model.genome.gene.GeneValueObject}.
     */
    public final static int TRANSFORM_GENEVALUEOBJECT = 1;

    /**
     * 
     */
    public java.lang.Integer countAll();

    /**
     * <p>
     * Does the same thing as {@link #find(boolean, ubic.gemma.model.genome.Gene)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #find(int, ubic.gemma.model.genome.Gene gene)}.
     * </p>
     */
    public Gene find( int transform, String queryString, ubic.gemma.model.genome.Gene gene );

    /**
     * <p>
     * Does the same thing as {@link #find(ubic.gemma.model.genome.Gene)} with an additional flag called
     * <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Gene find( int transform, ubic.gemma.model.genome.Gene gene );

    /**
     * Find all genes at a physical location. All overlapping genes are returned. The location can be a point or a
     * region. If strand is non-null, only genes on the same strand are returned.
     * 
     * @param physicalLocation
     * @return
     */
    public Collection<Gene> find( PhysicalLocation physicalLocation );

    /**
     * <p>
     * Does the same thing as {@link #find(ubic.gemma.model.genome.Gene)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #find(ubic.gemma.model.genome.Gene)}.
     * </p>
     */
    public ubic.gemma.model.genome.Gene find( String queryString, ubic.gemma.model.genome.Gene gene );

    /**
     * 
     */
    public ubic.gemma.model.genome.Gene find( ubic.gemma.model.genome.Gene gene );

    /**
     * 
     */
    public ubic.gemma.model.genome.Gene findByAccession( java.lang.String accession,
            ubic.gemma.model.common.description.ExternalDatabase source );

    /**
     * <p>
     * Locate genes that match the given alias string
     * </p>
     */
    public java.util.Collection<Gene> findByAlias( java.lang.String search );

    /**
     * <p>
     * Does the same thing as {@link #findByOfficalSymbol(java.lang.String)} with an additional flag called
     * <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public java.util.Collection<Gene> findByOfficalSymbol( int transform, java.lang.String officialSymbol );

    /**
     * <p>
     * Does the same thing as {@link #findByOfficalSymbol(boolean, java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findByOfficalSymbol(int, java.lang.String officialSymbol)}.
     * </p>
     */
    public java.util.Collection<Gene> findByOfficalSymbol( int transform, String queryString,
            java.lang.String officialSymbol );

    /**
     * <p>
     * Finder based on the official name.
     * </p>
     */
    public java.util.Collection<Gene> findByOfficalSymbol( java.lang.String officialSymbol );

    /**
     * <p>
     * Does the same thing as {@link #findByOfficalSymbol(java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findByOfficalSymbol(java.lang.String)}.
     * </p>
     */
    public java.util.Collection<Gene> findByOfficalSymbol( String queryString, java.lang.String officialSymbol );

    /**
     * <p>
     * Does the same thing as {@link #findByOfficialName(java.lang.String)} with an additional flag called
     * <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public java.util.Collection<Gene> findByOfficialName( int transform, java.lang.String officialName );

    /**
     * <p>
     * Does the same thing as {@link #findByOfficialName(boolean, java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findByOfficialName(int, java.lang.String officialName)}.
     * </p>
     */
    public java.util.Collection<Gene> findByOfficialName( int transform, String queryString,
            java.lang.String officialName );

    /**
     * 
     */
    public java.util.Collection<Gene> findByOfficialName( java.lang.String officialName );

    /**
     * <p>
     * Does the same thing as {@link #findByOfficialName(java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findByOfficialName(java.lang.String)}.
     * </p>
     */
    public java.util.Collection<Gene> findByOfficialName( String queryString, java.lang.String officialName );

    /**
     * 
     */
    public ubic.gemma.model.genome.Gene findByOfficialSymbol( java.lang.String symbol,
            ubic.gemma.model.genome.Taxon taxon );

    /**
     * <p>
     * Does the same thing as {@link #findByOfficialSymbolInexact(java.lang.String)} with an additional flag called
     * <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public java.util.Collection<Gene> findByOfficialSymbolInexact( int transform, java.lang.String officialSymbol );

    /**
     * <p>
     * Does the same thing as {@link #findByOfficialSymbolInexact(boolean, java.lang.String)} with an additional
     * argument called <code>queryString</code>. This <code>queryString</code> argument allows you to override the query
     * string defined in {@link #findByOfficialSymbolInexact(int, java.lang.String officialSymbol)}.
     * </p>
     */
    public java.util.Collection<Gene> findByOfficialSymbolInexact( int transform, String queryString,
            java.lang.String officialSymbol );

    /**
     * 
     */
    public java.util.Collection<Gene> findByOfficialSymbolInexact( java.lang.String officialSymbol );

    /**
     * <p>
     * Does the same thing as {@link #findByOfficialSymbolInexact(java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findByOfficialSymbolInexact(java.lang.String)}.
     * </p>
     */
    public java.util.Collection<Gene> findByOfficialSymbolInexact( String queryString, java.lang.String officialSymbol );

    /**
     * @param officialName
     * @return
     */
    public Collection<Gene> findByOfficialNameInexact( String officialName );

    /**
     * Find the Genes closest to the given location. If the location is in a gene(s), they will be returned. Otherwise a
     * single gene closest to the location will be returned, except in the case of ties in which more than one will be
     * returned.
     * 
     * @param physicalLocation
     * @param useStrand if true, the nearest Gene on the same strand will be found. Otherwise the nearest gene on either
     *        strand will be returned.
     * @return
     */
    public RelativeLocationData findNearest( PhysicalLocation physicalLocation, boolean useStrand );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(boolean, ubic.gemma.model.genome.Gene)} with an additional argument
     * called <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string
     * defined in {@link #findOrCreate(int, ubic.gemma.model.genome.Gene gene)}.
     * </p>
     */
    public Object findOrCreate( int transform, String queryString, ubic.gemma.model.genome.Gene gene );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(ubic.gemma.model.genome.Gene)} with an additional flag called
     * <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object findOrCreate( int transform, ubic.gemma.model.genome.Gene gene );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(ubic.gemma.model.genome.Gene)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findOrCreate(ubic.gemma.model.genome.Gene)}.
     * </p>
     */
    public ubic.gemma.model.genome.Gene findOrCreate( String queryString, ubic.gemma.model.genome.Gene gene );

    /**
     * 
     */
    public ubic.gemma.model.genome.Gene findOrCreate( ubic.gemma.model.genome.Gene gene );

    /**
     * Converts an instance of type {@link ubic.gemma.model.genome.gene.GeneValueObject} to this DAO's entity.
     */
    public ubic.gemma.model.genome.Gene geneValueObjectToEntity(
            ubic.gemma.model.genome.gene.GeneValueObject geneValueObject );

    /**
     * Copies the fields of {@link ubic.gemma.model.genome.gene.GeneValueObject} to the specified entity.
     * 
     * @param copyIfNull If FALSE, the value object's field will not be copied to the entity if the value is NULL. If
     *        TRUE, it will be copied regardless of its value.
     */
    public void geneValueObjectToEntity( ubic.gemma.model.genome.gene.GeneValueObject sourceVO,
            ubic.gemma.model.genome.Gene targetEntity, boolean copyIfNull );

    /**
     * Converts a Collection of instances of type {@link ubic.gemma.model.genome.gene.GeneValueObject} to this DAO's
     * entity.
     */
    public void geneValueObjectToEntityCollection( java.util.Collection<Gene> instances );

    /**
     * Function to get coexpressed genes given a set of genes and a collection of expressionExperiments. The return
     * value is a Map of CoexpressionCollectionValueObjects.
     * 
     * @param genes
     * @param ees
     * @param stringency
     * @param knownGenesOnly
     * @param interGeneOnly if true, only links among the query genes will be returned. This is ingored if only a single
     *        gene is entered
     * @return
     */
    public Map<Gene, CoexpressionCollectionValueObject> getCoexpressedGenes(
            Collection<ubic.gemma.model.genome.Gene> genes, java.util.Collection<? extends BioAssaySet> ees,
            java.lang.Integer stringency, boolean knownGenesOnly, boolean interGeneOnly );

    /**
     * <p>
     * Function to get coexpressed genes given a gene and a collection of expressionExperiments. The return value is a
     * CoexpressionCollectionValueObject.
     * </p>
     */
    public CoexpressionCollectionValueObject getCoexpressedGenes( ubic.gemma.model.genome.Gene gene,
            java.util.Collection<? extends BioAssaySet> ees, java.lang.Integer stringency, boolean knownGenesOnly );

    /**
     * 
     */
    public long getCompositeSequenceCountById( long id );

    /**
     * 
     */
    public java.util.Collection<CompositeSequence> getCompositeSequences( ubic.gemma.model.genome.Gene gene,
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * 
     */
    public java.util.Collection<CompositeSequence> getCompositeSequencesById( long id );

    /**
     * <p>
     * returns a collections of genes that match the given taxon
     * </p>
     */
    public java.util.Collection<Gene> getGenesByTaxon( ubic.gemma.model.genome.Taxon taxon );

    /**
     * <p>
     * Returns a collection of genes that are actually MicroRNA for a given taxon
     * </p>
     */
    public java.util.Collection<Gene> getMicroRnaByTaxon( ubic.gemma.model.genome.Taxon taxon );

    /**
     * 
     */
    public java.util.Collection<Gene> load( java.util.Collection<Long> ids );

    /**
     * <p>
     * Returns a collection of genes for the specified taxon (not all genes, ie not probe aligned regions and predicted
     * genes)
     * </p>
     */
    public java.util.Collection<Gene> loadKnownGenes( ubic.gemma.model.genome.Taxon taxon );

    /**
     * Returns a collection of predicted genes for the specified taxon.
     */
    public java.util.Collection<PredictedGene> loadPredictedGenes( ubic.gemma.model.genome.Taxon taxon );

    /**
     * Returns a collection of probe aligned regions for the specified taxon
     */
    public java.util.Collection<ProbeAlignedRegion> loadProbeAlignedRegions( ubic.gemma.model.genome.Taxon taxon );

    /**
     * 
     */
    public Gene thaw( Gene gene );

    /**
     * @param gene
     * @return
     */
    public Gene thawLite( Gene gene );

    /**
     * @param genes
     * @return
     * @see loadThawed, which you should use instead of this method if you know you want to load thawed objects.
     */
    public Collection<Gene> thawLite( java.util.Collection<Gene> genes );

    /**
     * @param ids
     * @return
     */
    public Collection<Gene> loadThawed( Collection<Long> ids );

}
