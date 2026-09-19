/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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
package ubic.gemma.persistence.service.genome.gene;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.ChromosomeLocation;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseDao;
import ubic.gemma.persistence.service.genome.ChromosomeDao;
import ubic.gemma.persistence.service.genome.GeneDao;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceDao;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.AnnotationAssociationDao;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.BlatAssociationDao;
import ubic.gemma.persistence.service.genome.taxon.TaxonDao;
import ubic.gemma.persistence.util.BusinessKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * Implementation of {@link GeneWriteService}. Method bodies are copied
 * verbatim from {@code GenomePersister} per the Chunk 5.3 PREP plan; the
 * persister still owns production traffic until a future cutover session
 * rewires {@code NcbiGeneLoader} and {@code ExternalFileGeneLoaderServiceImpl}
 * to call this service directly.
 * <p>
 * The {@code Caches} parameter from the persister chain is dropped: Hibernate
 * L1 covers the within-transaction caching that the per-call cache was
 * protecting, and the {@code Taxon}/{@code Chromosome} caches were already
 * removed in Chunks 5.1 / 5.2.
 *
 * @see ubic.gemma.persistence.persister.GenomePersister
 */
@Service
public class GeneWriteServiceImpl implements GeneWriteService {

    private static final Log log = LogFactory.getLog( GeneWriteServiceImpl.class );

    @Autowired
    private GeneDao geneDao;
    @Autowired
    private GeneProductDao geneProductDao;
    @Autowired
    private ChromosomeDao chromosomeDao;
    @Autowired
    private BioSequenceDao bioSequenceDao;
    @Autowired
    private BlatAssociationDao blatAssociationDao;
    @Autowired
    private AnnotationAssociationDao annotationAssociationDao;
    @Autowired
    private ExternalDatabaseDao externalDatabaseDao;
    @Autowired
    private TaxonDao taxonDao;
    @Autowired
    private SessionFactory sessionFactory;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Gene upsert( Gene gene ) {
        return this.upsert( gene, true, null );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Gene upsert( Gene gene, boolean removeProducts, @Nullable Consumer<GeneProductChange> changes ) {
        ChangeLog changeLog = changes != null ? new ChangeLog( changes ) : null;
        Gene existingGene;
        if ( gene.getId() != null ) {
            existingGene = geneDao.load( gene.getId() );
        } else {
            existingGene = geneDao.find( gene );
        }

        // Per-upsert caches. Without these, every external-database and chromosome
        // business-key lookup goes to the DB, and those finds trigger Hibernate
        // auto-flush in the middle of a dirty entity manipulation (e.g. after a new
        // PhysicalLocation is wired in but before it has been cascaded). The legacy
        // GenomePersister threaded a Caches object for exactly this reason. We retain
        // the two caches that the gene-load path exercises hard (ExternalDatabase per
        // accession, Chromosome per location); the Taxon cache is intentionally
        // omitted because Hibernate L1 covers within-tx repeats and the gene loader
        // does not churn taxa.
        Map<String, ExternalDatabase> externalDbCache = new HashMap<>();
        Map<Integer, Chromosome> chromosomeCache = new HashMap<>();

        if ( existingGene == null ) {
            Gene created = this.create( gene, externalDbCache, chromosomeCache, changeLog );
            if ( changeLog != null ) {
                this.report( changeLog );
            }
            return created;
        }

        if ( log.isDebugEnabled() )
            log.debug( "Updating " + existingGene );

        return this.updateGene( existingGene, gene, externalDbCache, chromosomeCache, removeProducts, changeLog );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Gene create( Gene gene ) {
        return this.create( gene, new HashMap<String, ExternalDatabase>(), new HashMap<Integer, Chromosome>(), null );
    }

    private Gene create( Gene gene, Map<String, ExternalDatabase> externalDbCache, Map<Integer, Chromosome> chromosomeCache, @Nullable ChangeLog changeLog ) {
        if ( !gene.getAccessions().isEmpty() ) {
            for ( DatabaseEntry de : gene.getAccessions() ) {
                this.fillInDatabaseEntry( de, externalDbCache );
            }
        }

        Collection<GeneProduct> tempGeneProduct = gene.getProducts();
        gene.setProducts( null );
        // Resolve the gene's taxon up-front (matches GenomePersister.persistGene); without this
        // a transient Taxon attached to the gene blows up on flush with a
        // TransientPropertyValueException, and downstream chromosome BK lookups need an
        // ID-bearing taxon too.
        if ( gene.getTaxon() != null ) {
            gene.setTaxon( this.persistTaxon( gene.getTaxon() ) );
        }
        if ( gene.getPhysicalLocation() != null ) {
            this.fillChromosomeLocationAssociations( gene.getPhysicalLocation(), gene.getTaxon(), chromosomeCache );
        }

        // Pre-resolve every ExternalDatabase used by any product accession BEFORE
        // geneDao.create(). The find() inside persistExternalDatabase triggers an
        // auto-flush, which after gene creation would try to flush the gene's
        // cascade-pending DatabaseEntries and blow up with HHH000099. Doing it now
        // keeps the call cache warm so the post-create fillInGeneProductAssociations
        // never re-queries.
        for ( GeneProduct gp : tempGeneProduct ) {
            if ( gp.getAccessions() != null ) {
                for ( DatabaseEntry de : gp.getAccessions() ) {
                    de.setExternalDatabase( this.persistExternalDatabase( de.getExternalDatabase(), externalDbCache ) );
                }
            }
        }

        if ( log.isDebugEnabled() )
            log.debug( "New gene: " + gene );
        gene = geneDao.create( gene );

        Set<GeneProduct> geneProductsForNewGene = new HashSet<>();
        for ( GeneProduct product : tempGeneProduct ) {
            GeneProduct existingProduct = geneProductDao.find( product );
            if ( existingProduct != null ) {
                /*
                 * A geneProduct is being moved to a gene that didn't exist in the system already
                 */
                Gene previousGeneForProduct = existingProduct.getGene();
                if ( changeLog != null && previousGeneForProduct != null ) {
                    changeLog.moved( existingProduct, previousGeneForProduct, gene );
                }
                previousGeneForProduct.getProducts().remove( existingProduct );
                product.setGene( null ); // we aren't going to make it, this isn't really necessary.
                existingProduct.setGene( gene );
                geneProductsForNewGene.add( existingProduct );

                log.warn( "While creating new gene: Gene product: [New=" + product
                        + "] is already associated with a gene [Old=" + existingProduct
                        + "], will move to associate with new gene: " + gene );
            } else {
                product.setGene( gene );
                geneProductsForNewGene.add( product );
            }
        }

        // attach the products.
        gene.setProducts( geneProductsForNewGene );
        for ( GeneProduct gp : gene.getProducts() ) {
            this.fillInGeneProductAssociations( gp, externalDbCache, chromosomeCache );
        }

        try {
            // we do a separate create because the cascade doesn't trigger auditing correctly - otherwise the
            // products are not persistent until the session is flushed, later. There might be a better way around this,
            // but so far as I know this is the only place this happens.
            gene.setProducts( new HashSet<>( geneProductDao.create( gene.getProducts() ) ) );
            geneDao.update( gene );
            return gene;
        } catch ( Exception e ) {
            log.error( "**** Error while creating gene: " + gene + "; products:" );
            for ( GeneProduct gp : gene.getProducts() ) {
                System.err.println( gp );
            }
            throw new RuntimeException( e );
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Gene updateGene( Gene existingGene, Gene newGeneInfo ) {
        return this.updateGene( existingGene, newGeneInfo, new HashMap<String, ExternalDatabase>(), new HashMap<Integer, Chromosome>(), true, null );
    }

    private Gene updateGene( Gene existingGene, Gene newGeneInfo, Map<String, ExternalDatabase> externalDbCache, Map<Integer, Chromosome> chromosomeCache,
            boolean removeProducts, @Nullable ChangeLog changeLog ) {

        // NCBI id can be null if gene has been loaded from a gene info file.
        Integer existingNcbiId = existingGene.getNcbiGeneId();
        if ( existingNcbiId != null && !existingNcbiId.equals( newGeneInfo.getNcbiGeneId() ) ) {
            log.info( "NCBI ID Change for " + existingGene + ", new id =" + newGeneInfo.getNcbiGeneId() );

            String previousIdString = newGeneInfo.getPreviousNcbiGeneId();
            if ( StringUtils.isNotBlank( previousIdString ) ) {
                /*
                 * Unfortunately, we need to check multiple 'previous' genes. The example I have run across is MTUS2-AS1
                 * (human) which was created by merging two previous genes, LOC728437 and LOC731614; only the former was
                 * in Gemma with its gene product GI:22268051. It also has a product we don't have, GI:14676690. This
                 * comma-delimited set thing is a hack.
                 */
                String[] previousIds = StringUtils.split( previousIdString, "," );
                boolean found = false;
                for ( String previousId : previousIds ) {
                    if ( previousId.equals( existingGene.getNcbiGeneId().toString() ) ) {
                        found = true;
                        break;
                    }
                }

                if ( !found ) {
                    throw new IllegalStateException( "The NCBI ID for " + newGeneInfo
                            + " has changed and the previous NCBI id on record with NCBI (" + newGeneInfo
                            .getPreviousNcbiGeneId()
                            + ") doesn't match." );
                }
            }

            // swap
            existingGene.setPreviousNcbiGeneId( existingGene.getNcbiGeneId().toString() );
            existingGene.setNcbiGeneId( newGeneInfo.getNcbiGeneId() );

            /*
             * Note: On occasion, we have two genes with the same symbol but different NCBI ids. This happens when NCBI
             * screws up somehow (?) and has two records for the same gene with different IDs, and we end up with them
             * both at the time they were considered separate genes. At some later date NCBI decides to (in effect)
             * merge them, so one of the genes has to be deprecated. Such 'relics' are left in place: GeneDaoImpl.find()
             * no longer deletes them (HQL_SQL_AUDIT C4), it logs them and returns the gene with the matching NCBI id.
             */

        }

        /*
         * We might want to change this behaviour to clear the value if the updated one has none. For now, I just want to
         * avoid wiping data.
         */
        if ( StringUtils.isNotBlank( newGeneInfo.getEnsemblId() ) ) {
            existingGene.setEnsemblId( newGeneInfo.getEnsemblId() );
        }

        // We assume the taxon hasn't changed.

        Map<String, DatabaseEntry> updatedAcMap = new HashMap<>();
        for ( DatabaseEntry de : existingGene.getAccessions() ) {
            updatedAcMap.put( de.getAccession(), de );
        }
        for ( DatabaseEntry de : newGeneInfo.getAccessions() ) {
            if ( !updatedAcMap.containsKey( de.getAccession() ) ) {
                this.fillInDatabaseEntry( de, externalDbCache );
                existingGene.getAccessions().add( de );
            }
        }

        existingGene.setName( newGeneInfo.getName() );
        existingGene.setDescription( newGeneInfo.getDescription() );
        existingGene.setOfficialName( newGeneInfo.getOfficialName() );
        existingGene.setOfficialSymbol( newGeneInfo.getOfficialSymbol() );

        // Resolve the new PhysicalLocation's chromosome BEFORE attaching it to the
        // managed existingGene. Otherwise the persistChromosome call (BK find)
        // auto-flushes the dirty existingGene whose new transient PhysicalLocation
        // still references a transient Chromosome — failing with TransientObjectException
        // ("save the transient instance before flushing: PhysicalLocation"). cascade=all
        // on Gene->PhysicalLocation does not extend to PL->Chromosome.
        if ( newGeneInfo.getPhysicalLocation() != null ) {
            this.fillChromosomeLocationAssociations( newGeneInfo.getPhysicalLocation(), existingGene.getTaxon(), chromosomeCache );
        }
        existingGene.setPhysicalLocation( newGeneInfo.getPhysicalLocation() );

        existingGene.getAliases().clear();
        existingGene.getAliases().addAll( newGeneInfo.getAliases() );

        /*
         * This is the only tricky part - the gene products. We update them if they are already there, and add them if
         * not. We do not normally remove 'old' ones that the new gene instance does not have, because they might be
         * from different sources. For example, Ensembl or GoldenPath. -- UNLESS the product has an NCBI GI because we
         * know those come from NCBI.
         */
        Map<String, GeneProduct> updatedGpMap = new HashMap<>();

        for ( GeneProduct existingGp : existingGene.getProducts() ) {
            updatedGpMap.put( existingGp.getName(), existingGp );
            updatedGpMap.put( existingGp.getNcbiGi(), existingGp );
        }

        Map<String, GeneProduct> usedGIs = new HashMap<>();
        for ( GeneProduct newGeneProductInfo : newGeneInfo.getProducts() ) {
            if ( updatedGpMap.containsKey( newGeneProductInfo.getName() ) ) {
                log.debug( "Updating gene product based on name: " + newGeneProductInfo );
                GeneProduct existingGeneProduct = updatedGpMap.get( newGeneProductInfo.getName() );
                this.updateGeneProduct( existingGeneProduct, newGeneProductInfo, externalDbCache, chromosomeCache );
            } else if ( updatedGpMap.containsKey( newGeneProductInfo.getNcbiGi() ) ) {
                log.debug( "Updating gene product based on GI: " + newGeneProductInfo );
                GeneProduct existingGeneProduct = updatedGpMap.get( newGeneProductInfo.getNcbiGi() );
                this.updateGeneProduct( existingGeneProduct, newGeneProductInfo, externalDbCache, chromosomeCache );
            } else {
                GeneProduct existingGeneProduct = geneProductDao.find( newGeneProductInfo );
                if ( existingGeneProduct == null ) {
                    // it is, in fact, new, so far as we can tell.
                    newGeneProductInfo.setGene( existingGene );
                    this.fillInGeneProductAssociations( newGeneProductInfo, externalDbCache, chromosomeCache );
                    log.debug( "New product for " + existingGene + ": " + newGeneProductInfo );
                    existingGene.getProducts().add( newGeneProductInfo );
                } else {
                    /*
                     * This can only happen if this gene product is associated with a different gene. This generally
                     * happens when a transcript is associated with two genes in NCBI, so the switching is actually not
                     * useful to us, but we do it anyway to be consistent (and in case it really does matter). It is
                     * rare. Causes can be 1) bicistronic genes such as human LUZP6 and MTPN; 2) genome-duplicated
                     * genes; or 3) an error in the data source. The problem for us is at this point in processing, we
                     * don't know if the gene is going to get 'reattached' to its original gene.
                     */
                    existingGeneProduct = geneProductDao.thaw( existingGeneProduct );
                    Gene oldGeneForExistingGeneProduct = existingGeneProduct.getGene();
                    if ( oldGeneForExistingGeneProduct != null ) {
                        Gene geneInfo = newGeneProductInfo.getGene(); // transient.
                        if ( !oldGeneForExistingGeneProduct.equals( geneInfo ) ) {

                            log.warn( "Switching gene product from one gene to another: " + existingGeneProduct
                                    + " switching to " + geneInfo
                                    + " (this can also happen if an mRNA is associated with two genes, which we don't allow, so we switch it arbitrarily)" );

                            // Here we just remove its old association.
                            oldGeneForExistingGeneProduct = geneDao.thaw( oldGeneForExistingGeneProduct );
                            if ( changeLog != null ) {
                                changeLog.moved( existingGeneProduct, oldGeneForExistingGeneProduct, existingGene );
                            }
                            oldGeneForExistingGeneProduct.getProducts().remove( existingGeneProduct );
                            log.debug( "Switch: Removing " + existingGeneProduct + " from " + oldGeneForExistingGeneProduct + " GI="
                                    + existingGeneProduct.getNcbiGi() );
                            geneDao.update( oldGeneForExistingGeneProduct );

                            if ( oldGeneForExistingGeneProduct.getProducts().isEmpty() ) {
                                log.warn( "Gene has no products left after removing that gene product (but it might change later): "
                                        + oldGeneForExistingGeneProduct );

                                /*
                                 * On occasion, we run into problems with sequences that have two diffent NCBI GI
                                 * IDs (due to an update) and which is also associated with two genes - almost
                                 * always in Drosophila. A recent example was GenBank: BT099970, which had the GI
                                 * 289666832 but after an update was GI 1108657489 associated with both Lcp65Ab1 and
                                 * Lcp65Ab2 in gene2accession. It's proven hard to track down exactly how to fix this as
                                 * the failure happens at the transaction flush - but using --restart seems to fix it.
                                 */

                            }
                        }

                        assert !oldGeneForExistingGeneProduct.getProducts().contains( existingGeneProduct );
                    } else {
                        log.debug( "Attaching orphaned gene product to " + existingGene + " : "
                                + existingGeneProduct );
                    }

                    existingGeneProduct.setGene( existingGene );
                    existingGene.getProducts().add( existingGeneProduct );
                    assert existingGeneProduct.getGene().equals( existingGene );

                    this.updateGeneProduct( existingGeneProduct, newGeneProductInfo, externalDbCache, chromosomeCache );

                }
            }

            if ( newGeneProductInfo.getNcbiGi() != null )
                usedGIs.put( newGeneProductInfo.getNcbiGi(), newGeneProductInfo );
        }

        Collection<GeneProduct> toRemove = new HashSet<>();

        if ( !usedGIs.isEmpty() ) {
            toRemove = this.handleGeneProductChangedGIs( existingGene, usedGIs, removeProducts, changeLog );
        }

        geneDao.update( existingGene );

        if ( changeLog != null ) {
            // before the removal, which deletes the associations this counts
            this.report( changeLog );
        }

        if ( !toRemove.isEmpty() ) {
            this.removeGeneProducts( toRemove );
        }

        if ( existingGene.getProducts().isEmpty() ) {
            log.debug( "No products left for: " + existingGene );
        }

        return existingGene;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Collection<GeneProduct> handleGeneProductChangedGIs( Gene existingGene, Map<String, GeneProduct> usedGIs ) {
        return this.handleGeneProductChangedGIs( existingGene, usedGIs, true, null );
    }

    /**
     * @param removeProducts if false, return nothing to remove and leave every product that would be removed attached
     *                       to its gene
     */
    private Collection<GeneProduct> handleGeneProductChangedGIs( Gene existingGene, Map<String, GeneProduct> usedGIs,
            boolean removeProducts, @Nullable ChangeLog changeLog ) {
        Collection<String> switchedGis = new HashSet<>();
        Collection<GeneProduct> toRemove = new HashSet<>();
        for ( GeneProduct existingGp : existingGene.getProducts() ) {

            if ( StringUtils.isBlank( existingGp.getNcbiGi() ) || usedGIs.containsKey( existingGp.getNcbiGi() ) ) {
                continue;
            }

            /*
             * Check to make sure this isn't an updated GI situation (actually common, whenever a sequence is updated).
             * That is, this gene product (already in the system) is actually a match for one of the imports: it's just
             * that the GI of our version is no longer valid. There are two situations. In the simplest case, we just
             * have to update the GI on our record. However, it might be that we _also_ have the one with the correct
             * GI. If that happens there are three situations. First, if the other one is already associated with this
             * gene, we should proceed with deleting the outdated copy and just keep the other one. Second, if the other
             * one is not associated with any gene, we should remove that one and update the outdated record. Third, the
             * other one might be associated with a _different_ gene, in which case we remove _that gp_ and update the
             * outdated record attached to _this_ gene.
             */
            boolean deleteIt = true;
            for ( GeneProduct ngp : usedGIs.values() ) {
                if ( !existingGp.getName().equals( ngp.getName() ) ) {
                    // this is the only way we can tell it is the same. Since GenBank accessions are good
                    // identifiers when you don't have a GI, this is reasonable.
                    continue;
                }

                /*
                 * Check if this GI is already associated with some other gene.
                 */
                GeneProduct otherGpUsingThisGi = geneProductDao.findByNcbiId( ngp.getNcbiGi() );

                /*
                 * In-place GI rotation: if the only row carrying the new GI is THIS same row (same id),
                 * then updateGeneProduct() already mutated the GI in place on the managed instance. The
                 * `existingGp` we are iterating may be a detached reflection still showing the OLD GI
                 * (so the early-skip via usedGIs.containsKey at the top of the outer loop missed it),
                 * but otherGpUsingThisGi resolves back to the same persistent row. Treat as a no-op:
                 * the rotation is done, do not remove it. Without this guard, the existing-copy branch
                 * below mistakenly deletes the row as a duplicate of itself.
                 */
                if ( otherGpUsingThisGi != null
                        && existingGp.getId() != null
                        && existingGp.getId().equals( otherGpUsingThisGi.getId() ) ) {
                    // Sync the GI on the in-iteration reflection so the returned Gene's
                    // products set surfaces the rotated value (the managed copy in the
                    // session already carries it via updateGeneProduct).
                    existingGp.setNcbiGi( ngp.getNcbiGi() );
                    deleteIt = false;
                    switchedGis.add( ngp.getNcbiGi() );
                    continue;
                }

                if ( otherGpUsingThisGi == null ) {
                    // this is routine; it happens whenever a sequence is updated by NCBI.

                    /*
                     * HOWEVER, if we ALREADY applied the same GI to some other product of the same gene, we have to
                     * remove the duplicate. This is due to cruft, we shouldn't have such duplicates.
                     */
                    if ( switchedGis.contains( ngp.getNcbiGi() ) ) {
                        log.warn( "Another gene product with the same intended GI will be deleted: "
                                + existingGp );
                        deleteIt = true;
                        continue;
                    }

                    // ok
                    log.warn( "Updating the GI for " + existingGp + " -> GI:" + ngp.getNcbiGi() );
                    existingGp.setNcbiGi( ngp.getNcbiGi() );
                    deleteIt = false;
                    switchedGis.add( ngp.getNcbiGi() );
                    continue;

                }

                // handle less common cases, largely due to database cruft.
                otherGpUsingThisGi = geneProductDao.thaw( otherGpUsingThisGi );

                Gene oldGeneForExistingGeneProduct = otherGpUsingThisGi.getGene();
                if ( oldGeneForExistingGeneProduct == null ) {
                    if ( changeLog != null ) {
                        changeLog.removed( otherGpUsingThisGi, null, existingGene.getTaxon(), removeProducts );
                    }
                    if ( removeProducts ) {
                        log.warn( "Updating the GI for " + existingGp + " -> GI:" + ngp.getNcbiGi()
                                + " and deleting orphan GP with same GI: " + otherGpUsingThisGi );

                        existingGp.setNcbiGi( ngp.getNcbiGi() );
                        // remove the old one, which was an orphan already.
                        toRemove.add( otherGpUsingThisGi );
                    } else {
                        // The GI stays as it is: with the orphan kept, two products would hold the new one, and
                        // findByNcbiId above expects at most one.
                        log.warn( "Not updating the GI for " + existingGp + " -> GI:" + ngp.getNcbiGi()
                                + ", because removing gene products is off and orphan GP " + otherGpUsingThisGi
                                + " holds that GI" );
                    }
                    deleteIt = false;
                } else if ( oldGeneForExistingGeneProduct.equals( existingGene ) ) {
                    // this is the common case, for crufted database.
                    log.warn( "Removing outdated gp for which there is already an existing copy: " + existingGp
                            + " (already have " + otherGpUsingThisGi + ")" );
                    deleteIt = true;
                } else {
                    /*
                     * That GI is associated with another gene's products. In effect, switch it to this gene. This
                     * should not generally happen.
                     */
                    log.warn( "Removing gene product: " + otherGpUsingThisGi + " and effectively switching to "
                            + existingGene + " -- detected during GI update checks " );

                    // Here we just remove its old association.
                    oldGeneForExistingGeneProduct = geneDao.thaw( oldGeneForExistingGeneProduct );
                    oldGeneForExistingGeneProduct.getProducts().remove( otherGpUsingThisGi );
                    geneDao.update( oldGeneForExistingGeneProduct );

                    // but we keep the one we have here.
                    existingGp.setNcbiGi( ngp.getNcbiGi() );
                    deleteIt = false;
                }

            }

            if ( deleteIt ) {
                if ( changeLog != null ) {
                    changeLog.removed( existingGp, existingGene, existingGene.getTaxon(), removeProducts );
                }
                if ( removeProducts ) {
                    toRemove.add( existingGp );
                    existingGp.setGene( null ); // we are erasing this association as we assume it is no longer
                    // valid.
                    log.warn( "Removing gene product from system: " + existingGp
                            + ", it is no longer listed as a product of " + existingGene );
                } else {
                    // Not detached either: a product without a gene drops out of GENE2CS and the annotation files
                    // just as a deleted one does.
                    log.warn( "Not removing gene product " + existingGp + ", which is no longer listed as a product of "
                            + existingGene + ", because removing gene products is off" );
                }
            }
        } // over this gene's gene products.

        // finalize any deletions.
        if ( !toRemove.isEmpty() ) {
            existingGene.getProducts().removeAll( toRemove );
        }

        return toRemove;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void updateGeneProduct( GeneProduct existingGeneProduct, GeneProduct updatedGeneProductInfo ) {
        this.updateGeneProduct( existingGeneProduct, updatedGeneProductInfo,
                new HashMap<String, ExternalDatabase>(), new HashMap<Integer, Chromosome>() );
    }

    private void updateGeneProduct( GeneProduct existingGeneProduct, GeneProduct updatedGeneProductInfo,
            Map<String, ExternalDatabase> externalDbCache, Map<Integer, Chromosome> chromosomeCache ) {
        Gene geneForExistingGeneProduct = existingGeneProduct.getGene();

        existingGeneProduct = geneProductDao.thaw( existingGeneProduct );

        // Update all the fields. Note that usually, some of these can't have changed, or we wouldn't have even
        // found the 'existing' one (name GI in particular); however, sometimes we are updating this information

        existingGeneProduct.setName( updatedGeneProductInfo.getName() );
        existingGeneProduct.setDescription( updatedGeneProductInfo.getDescription() );
        existingGeneProduct.setNcbiGi( updatedGeneProductInfo.getNcbiGi() );

        this.addAnyNewAccessions( existingGeneProduct, updatedGeneProductInfo, externalDbCache );

        // Resolve the new PhysicalLocation's chromosome BEFORE attaching it to the
        // managed existingGeneProduct, for the same reason as in updateGene above:
        // PL->Chromosome has no cascade, so the auto-flush triggered by the chromosome
        // BK lookup would blow up on the transient PL otherwise.
        if ( updatedGeneProductInfo.getPhysicalLocation() != null
                && updatedGeneProductInfo.getPhysicalLocation().getChromosome() != null ) {
            updatedGeneProductInfo.getPhysicalLocation().setChromosome(
                    this.persistChromosome( updatedGeneProductInfo.getPhysicalLocation().getChromosome(),
                            geneForExistingGeneProduct.getTaxon(), chromosomeCache ) );
        }
        existingGeneProduct.setPhysicalLocation( updatedGeneProductInfo.getPhysicalLocation() );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void addAnyNewAccessions( GeneProduct existing, GeneProduct geneProduct ) {
        this.addAnyNewAccessions( existing, geneProduct, new HashMap<String, ExternalDatabase>() );
    }

    private void addAnyNewAccessions( GeneProduct existing, GeneProduct geneProduct, Map<String, ExternalDatabase> externalDbCache ) {
        Map<String, DatabaseEntry> updatedGpMap = new HashMap<>();
        existing = geneProductDao.thaw( existing );
        for ( DatabaseEntry de : existing.getAccessions() ) {
            updatedGpMap.put( de.getAccession(), de );
        }
        for ( DatabaseEntry de : geneProduct.getAccessions() ) {
            if ( !updatedGpMap.containsKey( de.getAccession() ) ) {
                this.fillInDatabaseEntry( de, externalDbCache );
                existing.getAccessions().add( de );
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void removeGeneProducts( Collection<GeneProduct> toRemove ) {
        Collection<BlatAssociation> associations = this.blatAssociationDao.find( toRemove );
        if ( !associations.isEmpty() ) {
            log.info( "Removing " + associations.size() + " blat associations involving up to " + toRemove.size()
                    + " products." );
            this.blatAssociationDao.remove( associations );
        }

        Collection<AnnotationAssociation> annotationAssociations = this.annotationAssociationDao.find( toRemove );
        if ( !annotationAssociations.isEmpty() ) {
            log.info( "Removing " + annotationAssociations.size() + " annotationAssociations involving up to "
                    + toRemove.size() + " products." );
            this.annotationAssociationDao.remove( annotationAssociations );
        }

        // might need to add referenceAssociations also.
        // remove associations to database entries that are still associated with sequences.
        for ( GeneProduct gp : toRemove ) {
            /*
            This thaw was not thought to be necessary but during NcbiGeneLoader processing, we sometimes hit products that
            are somehow not associated with the current session, so we need to initialize gp.accessions in particular.
             */
            GeneProduct gpt = geneProductDao.thaw( gp );
            Collection<DatabaseEntry> accessions = gpt.getAccessions();
            Collection<DatabaseEntry> toRelease = new HashSet<>();
            for ( DatabaseEntry de : accessions ) {
                if ( this.bioSequenceDao.findByAccession( de ) != null ) {
                    toRelease.add( de );
                }
            }
            gpt.getAccessions().removeAll( toRelease );
            this.geneProductDao.remove( gpt );
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public GeneProductRemovalOutcome replayGeneProductRemoval( GeneProductChange removal, @Nullable Collection<ArrayDesign> platforms, boolean dryRun ) {
        Assert.isTrue( removal.kind() == GeneProductChange.Kind.REMOVE && !removal.applied(),
                "Only a gene product removal that was not applied can be replayed." );
        Assert.isTrue( platforms == null || !platforms.isEmpty(), "The platforms to limit the removal to must not be empty; pass null for no limit." );

        GeneProduct gp = removal.productId() != null ? geneProductDao.load( removal.productId() ) : null;
        if ( gp == null ) {
            return GeneProductRemovalOutcome.skipped( GeneProductRemovalOutcome.SkipReason.PRODUCT_NOT_FOUND,
                    "there is no gene product with ID " + removal.productId() );
        }
        if ( gp.isDummy() ) {
            return GeneProductRemovalOutcome.skipped( GeneProductRemovalOutcome.SkipReason.DUMMY_PRODUCT,
                    gp + " is a dummy gene product" );
        }
        Gene gene = gp.getGene();
        if ( removal.geneNcbiId() == null ? gene != null : gene == null || !removal.geneNcbiId().equals( gene.getNcbiGeneId() ) ) {
            return GeneProductRemovalOutcome.skipped( GeneProductRemovalOutcome.SkipReason.GENE_CHANGED,
                    gp + " now belongs to " + ( gene != null ? gene.getOfficialSymbol() + " [NCBI " + gene.getNcbiGeneId() + "]" : "no gene" )
                            + ", not " + ( removal.geneNcbiId() != null ? removal.geneSymbol() + " [NCBI " + removal.geneNcbiId() + "]" : "no gene" ) );
        }
        if ( !Objects.equals( StringUtils.stripToNull( removal.productGi() ), StringUtils.stripToNull( gp.getNcbiGi() ) ) ) {
            return GeneProductRemovalOutcome.skipped( GeneProductRemovalOutcome.SkipReason.GI_CHANGED,
                    gp + " now has GI " + gp.getNcbiGi() + ", not " + removal.productGi() );
        }

        Collection<BlatAssociation> blatAssociations;
        Collection<AnnotationAssociation> annotationAssociations;
        if ( platforms == null ) {
            blatAssociations = blatAssociationDao.find( Collections.singleton( gp ) );
            annotationAssociations = annotationAssociationDao.find( Collections.singleton( gp ) );
        } else {
            blatAssociations = this.findAssociationsOnPlatforms( BlatAssociation.class, gp, platforms );
            annotationAssociations = this.findAssociationsOnPlatforms( AnnotationAssociation.class, gp, platforms );
        }
        boolean deleteProduct = platforms == null
                || this.countAssociations( gp ) == blatAssociations.size() + annotationAssociations.size();

        Set<BioSequence> sequences = new HashSet<>();
        blatAssociations.forEach( a -> sequences.add( a.getBioSequence() ) );
        annotationAssociations.forEach( a -> sequences.add( a.getBioSequence() ) );
        List<String> affectedPlatforms = this.findPlatformShortNames( sequences );

        if ( !dryRun ) {
            if ( deleteProduct ) {
                if ( gene != null ) {
                    gene.getProducts().remove( gp );
                    gp.setGene( null );
                }
                // deletes the associations too; with a platform limit, those found above are all there are
                this.removeGeneProducts( Collections.singleton( gp ) );
            } else {
                blatAssociationDao.remove( blatAssociations );
                annotationAssociationDao.remove( annotationAssociations );
            }
        }

        String detail = ( dryRun ? "would delete " : "deleted " ) + blatAssociations.size() + " BLAT and "
                + annotationAssociations.size() + " annotation associations"
                + ( affectedPlatforms.isEmpty() ? "" : " (on " + String.join( ", ", affectedPlatforms ) + ")" )
                + ( deleteProduct ? " and the gene product" : "; the gene product " + ( dryRun ? "would be" : "is" )
                + " kept for its associations on other platforms" );
        return new GeneProductRemovalOutcome( null, detail, deleteProduct, blatAssociations.size(),
                annotationAssociations.size(), affectedPlatforms );
    }

    private <T extends BioSequence2GeneProduct> List<T> findAssociationsOnPlatforms( Class<T> type, GeneProduct gp, Collection<ArrayDesign> platforms ) {
        return sessionFactory.getCurrentSession()
                .createQuery( "select distinct a from " + type.getSimpleName() + " a, CompositeSequence cs "
                        + "where a.geneProduct = :gp and cs.biologicalCharacteristic = a.bioSequence "
                        + "and cs.arrayDesign in (:platforms)", type )
                .setParameter( "gp", gp )
                .setParameterList( "platforms", platforms )
                .list();
    }

    /**
     * Every association to the product, of any kind: the product cannot be deleted while one is left.
     */
    private long countAssociations( GeneProduct gp ) {
        return sessionFactory.getCurrentSession()
                .createQuery( "select count(a) from BioSequence2GeneProduct a where a.geneProduct = :gp", Long.class )
                .setParameter( "gp", gp )
                .uniqueResult();
    }

    private List<String> findPlatformShortNames( Collection<BioSequence> sequences ) {
        if ( sequences.isEmpty() ) {
            return Collections.emptyList();
        }
        return sessionFactory.getCurrentSession()
                .createQuery( "select distinct ad.shortName from CompositeSequence cs join cs.arrayDesign ad "
                        + "where cs.biologicalCharacteristic in (:sequences) and ad.shortName is not null "
                        + "order by ad.shortName", String.class )
                .setParameterList( "sequences", sequences )
                .list();
    }

    /**
     * Gene product removals and moves seen during one upsert, reported together so that the associations of all the
     * products are counted in one set of queries.
     */
    private static final class ChangeLog {

        private final Consumer<GeneProductChange> sink;
        private final List<PendingChange> pending = new ArrayList<>();

        private ChangeLog( Consumer<GeneProductChange> sink ) {
            this.sink = sink;
        }

        private void removed( GeneProduct product, @Nullable Gene gene, @Nullable Taxon taxon, boolean applied ) {
            pending.add( new PendingChange( GeneProductChange.Kind.REMOVE, applied, product, gene, null, taxon ) );
        }

        private void moved( GeneProduct product, Gene from, Gene to ) {
            pending.add( new PendingChange( GeneProductChange.Kind.SWITCH, true, product, from, to, from.getTaxon() ) );
        }
    }

    /**
     * The genes are held rather than their NCBI ids and symbols, which may still change during the upsert.
     */
    private record PendingChange( GeneProductChange.Kind kind, boolean applied, GeneProduct product,
                                  @Nullable Gene gene, @Nullable Gene toGene, @Nullable Taxon taxon ) {
    }

    /**
     * Count the associations of each pending product, send the changes to the sink and clear them. Must run before
     * any of the products is deleted.
     */
    private void report( ChangeLog changeLog ) {
        if ( changeLog.pending.isEmpty() ) {
            return;
        }
        Set<Long> productIds = new HashSet<>();
        for ( PendingChange change : changeLog.pending ) {
            productIds.add( change.product().getId() );
        }
        productIds.remove( null );
        Map<Long, Long> blatCounts = this.countAssociationsByProduct( BlatAssociation.class, productIds );
        Map<Long, Long> annotationCounts = this.countAssociationsByProduct( AnnotationAssociation.class, productIds );
        Map<Long, SortedSet<String>> platforms = new HashMap<>();
        this.addPlatformsByProduct( BlatAssociation.class, productIds, platforms );
        this.addPlatformsByProduct( AnnotationAssociation.class, productIds, platforms );
        for ( PendingChange change : changeLog.pending ) {
            GeneProduct product = change.product();
            Long id = product.getId();
            Gene gene = change.gene();
            Gene toGene = change.toGene();
            changeLog.sink.accept( new GeneProductChange(
                    change.kind(),
                    change.applied(),
                    change.taxon() != null ? change.taxon().getCommonName() : null,
                    gene != null ? gene.getNcbiGeneId() : null,
                    gene != null ? gene.getOfficialSymbol() : null,
                    toGene != null ? toGene.getNcbiGeneId() : null,
                    toGene != null ? toGene.getOfficialSymbol() : null,
                    id,
                    product.getName(),
                    product.getNcbiGi(),
                    id != null ? blatCounts.getOrDefault( id, 0L ) : 0L,
                    id != null ? annotationCounts.getOrDefault( id, 0L ) : 0L,
                    id != null ? new ArrayList<>( platforms.getOrDefault( id, Collections.emptySortedSet() ) ) : Collections.emptyList() ) );
        }
        changeLog.pending.clear();
    }

    private Map<Long, Long> countAssociationsByProduct( Class<? extends BioSequence2GeneProduct> type, Collection<Long> productIds ) {
        Map<Long, Long> counts = new HashMap<>();
        if ( productIds.isEmpty() ) {
            return counts;
        }
        List<Object[]> rows = sessionFactory.getCurrentSession()
                .createQuery( "select a.geneProduct.id, count(a) from " + type.getSimpleName() + " a "
                        + "where a.geneProduct.id in (:ids) group by a.geneProduct.id", Object[].class )
                .setParameterList( "ids", productIds )
                .list();
        for ( Object[] row : rows ) {
            counts.put( ( Long ) row[0], ( Long ) row[1] );
        }
        return counts;
    }

    private void addPlatformsByProduct( Class<? extends BioSequence2GeneProduct> type, Collection<Long> productIds, Map<Long, SortedSet<String>> platforms ) {
        if ( productIds.isEmpty() ) {
            return;
        }
        List<Object[]> rows = sessionFactory.getCurrentSession()
                .createQuery( "select distinct a.geneProduct.id, ad.shortName from " + type.getSimpleName() + " a, "
                        + "CompositeSequence cs join cs.arrayDesign ad "
                        + "where cs.biologicalCharacteristic = a.bioSequence and a.geneProduct.id in (:ids) "
                        + "and ad.shortName is not null", Object[].class )
                .setParameterList( "ids", productIds )
                .list();
        for ( Object[] row : rows ) {
            platforms.computeIfAbsent( ( Long ) row[0], k -> new TreeSet<>() ).add( ( String ) row[1] );
        }
    }

    // ---- helpers replicated from GenomePersister / CommonPersister ----
    // These are cache-free copies. Hibernate L1 covers within-transaction
    // identity. Once the cutover lands, GenomePersister's copies go away.

    private void fillInDatabaseEntry( DatabaseEntry databaseEntry, Map<String, ExternalDatabase> externalDbCache ) {
        ExternalDatabase tempExternalDb = databaseEntry.getExternalDatabase();
        databaseEntry.setExternalDatabase( null );
        ExternalDatabase persistedDb = this.persistExternalDatabase( tempExternalDb, externalDbCache );
        databaseEntry.setExternalDatabase( persistedDb );
        assert databaseEntry.getExternalDatabase().getId() != null;
    }

    private ExternalDatabase persistExternalDatabase( ExternalDatabase database, Map<String, ExternalDatabase> externalDbCache ) {
        String name = database.getName();
        if ( name != null && externalDbCache.containsKey( name ) ) {
            return externalDbCache.get( name );
        }
        // ExternalDatabase has no static BusinessKey.find; DAO-level find()
        // resolves by name (single-property business key).
        ExternalDatabase existingDatabase = externalDatabaseDao.find( database );
        ExternalDatabase resolved = existingDatabase != null ? existingDatabase : externalDatabaseDao.create( database );
        if ( name != null ) {
            externalDbCache.put( name, resolved );
        }
        return resolved;
    }

    private void fillChromosomeLocationAssociations( ChromosomeLocation chromosomeLocation, Taxon t, Map<Integer, Chromosome> chromosomeCache ) {
        if ( chromosomeLocation == null ) return;
        if ( chromosomeLocation.getChromosome() != null ) {
            chromosomeLocation.setChromosome( this.persistChromosome( chromosomeLocation.getChromosome(), t, chromosomeCache ) );
        }
    }

    private void fillInGeneProductAssociations( GeneProduct geneProduct, Map<String, ExternalDatabase> externalDbCache, Map<Integer, Chromosome> chromosomeCache ) {
        if ( geneProduct.getPhysicalLocation() != null && geneProduct.getPhysicalLocation().getChromosome() != null ) {
            geneProduct.getPhysicalLocation().setChromosome(
                    this.persistChromosome( geneProduct.getPhysicalLocation().getChromosome(),
                            geneProduct.getGene().getTaxon(), chromosomeCache ) );
        }

        if ( geneProduct.getAccessions() != null ) {
            for ( DatabaseEntry de : geneProduct.getAccessions() ) {
                de.setExternalDatabase( this.persistExternalDatabase( de.getExternalDatabase(), externalDbCache ) );
            }
        }
    }

    private Chromosome persistChromosome( Chromosome chromosome, Taxon t, Map<Integer, Chromosome> chromosomeCache ) {
        if ( chromosome == null ) return null;
        Taxon ct = t;
        if ( ct == null ) {
            ct = chromosome.getTaxon();
        }
        chromosome.setTaxon( ct );

        // Build a cache key the same way GenomePersister did: chromosome name +
        // taxon identifier hash (NCBI id first, then common/scientific name). This
        // avoids hitting BusinessKey.find for every repeat (and the auto-flush it
        // triggers) when the same chromosome is referenced by many products / a
        // batch of genes on the same chromosome.
        int key = chromosome.getName() != null ? chromosome.getName().hashCode() : 0;
        if ( ct != null ) {
            if ( ct.getNcbiId() != null ) {
                key += ct.getNcbiId().hashCode();
            } else if ( ct.getCommonName() != null ) {
                key += ct.getCommonName().hashCode();
            } else if ( ct.getScientificName() != null ) {
                key += ct.getScientificName().hashCode();
            }
        }
        if ( chromosomeCache.containsKey( key ) ) {
            return chromosomeCache.get( key );
        }

        // By name and taxon only, as Gemma 1.x did. The full business key also matches the sequence, and gene2accession
        // places a gene on several genomic accessions, so each one created another chromosome of the same name.
        Session session = sessionFactory.getCurrentSession();
        Chromosome existing = BusinessKey.find( session, Chromosome.Factory.newInstance( chromosome.getName(), ct ) );

        Chromosome resolved;
        if ( existing == null ) {
            // On miss we are about to insert; the chromosome's taxon FK is NOT NULL and
            // does not cascade-persist, so resolve any transient Taxon first. This matches
            // GenomePersister.persistChromosome's miss-branch (which delegated to
            // doPersist(Taxon, caches) -> persistTaxon).
            if ( ct != null ) {
                chromosome.setTaxon( this.persistTaxon( ct ) );
            }
            // Chromosome.sequence is also a NOT-NULL-free many-to-one without cascade-persist;
            // NcbiGeneConverter.getChromosomeDetails attaches a fresh transient BioSequence here
            // (with a transient DatabaseEntry that has a transient ExternalDatabase). Without
            // pre-persisting, the chromosomeDao.create flush throws
            // TransientObjectException on BioSequence. Mirrors GenomePersister.persistChromosome
            // miss-branch.
            if ( chromosome.getSequence() != null ) {
                chromosome.setSequence( this.persistBioSequence( chromosome.getSequence() ) );
            }
            resolved = chromosomeDao.create( chromosome );
        } else {
            resolved = existing;
        }
        chromosomeCache.put( key, resolved );
        return resolved;
    }

    /**
     * Find-or-create for a {@link BioSequence} by business key. Used by
     * {@link #persistChromosome} to resolve the transient BioSequence that
     * {@code NcbiGeneConverter.getChromosomeDetails} attaches to a fresh
     * chromosome. Mirrors {@code GenomePersister.persistBioSequence} +
     * {@code persistBioSequenceAssociations} minus the per-call caches (the
     * gene-loader path hits at most one BioSequence per chromosome, so the
     * cache benefit is negligible compared with GenomePersister's broader
     * use of this method via the ArrayDesignSequencePersister path).
     */
    private BioSequence persistBioSequence( BioSequence bioSequence ) {
        if ( bioSequence == null ) return null;
        if ( bioSequence.getId() != null ) return bioSequence;

        BioSequence existing = bioSequenceDao.find( bioSequence );
        if ( existing != null ) return existing;

        // Resolve associations on miss. Taxon FK is NOT NULL; ExternalDatabase on the
        // sequenceDatabaseEntry has no cascade-persist.
        if ( bioSequence.getTaxon() != null ) {
            bioSequence.setTaxon( this.persistTaxon( bioSequence.getTaxon() ) );
        }
        DatabaseEntry sde = bioSequence.getSequenceDatabaseEntry();
        if ( sde != null && sde.getExternalDatabase() != null && sde.getExternalDatabase().getId() == null ) {
            sde.setExternalDatabase( this.persistExternalDatabase( sde.getExternalDatabase(), new HashMap<String, ExternalDatabase>() ) );
        }
        return bioSequenceDao.create( bioSequence );
    }

    /**
     * Find-or-create for a {@link Taxon} by business key. Mirrors
     * {@link ubic.gemma.persistence.persister.GenomePersister#persistTaxon} minus the
     * per-call cache (Hibernate L1 covers within-transaction identity, and the gene
     * import paths that drive this service don't churn taxa hot enough to need the
     * cache that GenomePersister maintained).
     */
    private Taxon persistTaxon( Taxon taxon ) {
        if ( taxon == null ) return null;
        if ( taxon.getId() != null ) return taxon;
        Session session = sessionFactory.getCurrentSession();
        Taxon existing = BusinessKey.find( session, taxon );
        if ( existing == null ) {
            return taxonDao.create( taxon );
        }
        return existing;
    }
}
