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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.ChromosomeLocation;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
            return this.create( gene, externalDbCache, chromosomeCache );
        }

        if ( log.isDebugEnabled() )
            log.debug( "Updating " + existingGene );

        return this.updateGene( existingGene, gene, externalDbCache, chromosomeCache );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public Gene create( Gene gene ) {
        return this.create( gene, new HashMap<String, ExternalDatabase>(), new HashMap<Integer, Chromosome>() );
    }

    private Gene create( Gene gene, Map<String, ExternalDatabase> externalDbCache, Map<Integer, Chromosome> chromosomeCache ) {
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
        return this.updateGene( existingGene, newGeneInfo, new HashMap<String, ExternalDatabase>(), new HashMap<Integer, Chromosome>() );
    }

    private Gene updateGene( Gene existingGene, Gene newGeneInfo, Map<String, ExternalDatabase> externalDbCache, Map<Integer, Chromosome> chromosomeCache ) {

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
             * merge them, so one of the genes has to be deprecated. Such 'relics' are deleted by the DAO, because it
             * results in more than one gene being found.
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
            toRemove = this.handleGeneProductChangedGIs( existingGene, usedGIs );
        }

        geneDao.update( existingGene );

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
                    log.warn( "Updating the GI for " + existingGp + " -> GI:" + ngp.getNcbiGi()
                            + " and deleting orphan GP with same GI: " + otherGpUsingThisGi );

                    existingGp.setNcbiGi( ngp.getNcbiGi() );
                    // remove the old one, which was an orphan already.
                    toRemove.add( otherGpUsingThisGi );
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
                toRemove.add( existingGp );
                existingGp.setGene( null ); // we are erasing this association as we assume it is no longer
                // valid.
                log.warn( "Removing gene product from system: " + existingGp
                        + ", it is no longer listed as a product of " + existingGene );
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

    // ---- helpers replicated from GenomePersister / CommonPersister ----
    // These are cache-free copies. Hibernate L1 covers within-transaction
    // identity. Once the cutover lands, GenomePersister's copies go away.

    private void fillInDatabaseEntry( DatabaseEntry databaseEntry ) {
        this.fillInDatabaseEntry( databaseEntry, new HashMap<String, ExternalDatabase>() );
    }

    private void fillInDatabaseEntry( DatabaseEntry databaseEntry, Map<String, ExternalDatabase> externalDbCache ) {
        ExternalDatabase tempExternalDb = databaseEntry.getExternalDatabase();
        databaseEntry.setExternalDatabase( null );
        ExternalDatabase persistedDb = this.persistExternalDatabase( tempExternalDb, externalDbCache );
        databaseEntry.setExternalDatabase( persistedDb );
        assert databaseEntry.getExternalDatabase().getId() != null;
    }

    private ExternalDatabase persistExternalDatabase( ExternalDatabase database ) {
        return this.persistExternalDatabase( database, new HashMap<String, ExternalDatabase>() );
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

    private void fillChromosomeLocationAssociations( ChromosomeLocation chromosomeLocation, Taxon t ) {
        this.fillChromosomeLocationAssociations( chromosomeLocation, t, new HashMap<Integer, Chromosome>() );
    }

    private void fillChromosomeLocationAssociations( ChromosomeLocation chromosomeLocation, Taxon t, Map<Integer, Chromosome> chromosomeCache ) {
        if ( chromosomeLocation == null ) return;
        if ( chromosomeLocation.getChromosome() != null ) {
            chromosomeLocation.setChromosome( this.persistChromosome( chromosomeLocation.getChromosome(), t, chromosomeCache ) );
        }
    }

    private void fillInGeneProductAssociations( GeneProduct geneProduct ) {
        this.fillInGeneProductAssociations( geneProduct, new HashMap<String, ExternalDatabase>(), new HashMap<Integer, Chromosome>() );
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

    private Chromosome persistChromosome( Chromosome chromosome, Taxon t ) {
        return this.persistChromosome( chromosome, t, new HashMap<Integer, Chromosome>() );
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

        Session session = sessionFactory.getCurrentSession();
        Chromosome existing = BusinessKey.find( session, chromosome );

        Chromosome resolved;
        if ( existing == null ) {
            // On miss we are about to insert; the chromosome's taxon FK is NOT NULL and
            // does not cascade-persist, so resolve any transient Taxon first. This matches
            // GenomePersister.persistChromosome's miss-branch (which delegated to
            // doPersist(Taxon, caches) -> persistTaxon).
            if ( ct != null ) {
                chromosome.setTaxon( this.persistTaxon( ct ) );
            }
            resolved = chromosomeDao.create( chromosome );
        } else {
            resolved = existing;
        }
        chromosomeCache.put( key, resolved );
        return resolved;
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
