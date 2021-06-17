package ubic.gemma.persistence.persister.gene;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.ChromosomeLocation;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.persistence.persister.AbstractPersister;
import ubic.gemma.persistence.persister.ChromosomePersister;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.genome.GeneDao;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceDao;
import ubic.gemma.persistence.service.genome.gene.GeneProductDao;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.AnnotationAssociationDao;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.BlatAssociationDao;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Service
public class GenePersister extends AbstractPersister<Gene> {

    @Autowired
    private GeneDao geneDao;

    @Autowired
    private GeneProductDao geneProductDao;

    @Autowired
    private AnnotationAssociationDao annotationAssociationDao;

    @Autowired
    private BlatAssociationDao blatAssociationDao;

    @Autowired
    private BioSequenceDao bioSequenceDao;

    @Autowired
    private Persister<DatabaseEntry> databaseEntryPersister;

    @Autowired
    private Persister<Taxon> taxonPersister;

    @Autowired
    private ChromosomePersister chromosomePersister;

    @Autowired
    private GeneProductPersister geneProductPersister;

    @Autowired
    public GenePersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    public Gene persist( Gene gene ) {
        return this.persistGene( gene, true );
    }

    private Gene persistGene( Gene gene, boolean checkFirst ) {
        if ( gene == null )
            return null;
        if ( !this.isTransient( gene ) )
            return gene;

        if ( checkFirst ) {
            Gene existingGene = geneDao.find( gene );

            if ( existingGene != null ) {
                if ( AbstractPersister.log.isDebugEnabled() )
                    AbstractPersister.log.debug( "Gene exists, will not update" );
                return existingGene;
            }
        }

        if ( gene.getAccessions().size() > 0 ) {
            for ( DatabaseEntry de : gene.getAccessions() ) {
                databaseEntryPersister.persist( de );
            }
        }

        Collection<GeneProduct> tempGeneProduct = gene.getProducts();
        gene.setProducts( null );
        gene.setTaxon( taxonPersister.persist( gene.getTaxon() ) );
        this.fillChromosomeLocationAssociations( gene.getPhysicalLocation(), gene.getTaxon() );

        if ( AbstractPersister.log.isInfoEnabled() )
            AbstractPersister.log.info( "New gene: " + gene );
        gene = geneDao.create( gene );

        Collection<GeneProduct> geneProductsForNewGene = new HashSet<>();
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

                AbstractPersister.log.warn( "While creating new gene: Gene product: [New=" + product
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
            this.geneProductPersister.fillInGeneProductAssociations( gp );
        }

        try {
            // we do a separate create because the cascade doesn't trigger auditing correctly - otherwise the
            // products are not persistent until the session is flushed, later. There might be a better way around this,
            // but so far as I know this is the only place this happens.
            //noinspection unchecked
            gene.setProducts( geneProductDao.create( gene.getProducts() ) );
            geneDao.update( gene );
            return gene;
        } catch ( Exception e ) {
            AbstractPersister.log.error( "**** Error while creating gene: " + gene + "; products:" );
            for ( GeneProduct gp : gene.getProducts() ) {
                System.err.println( gp );
            }
            throw new RuntimeException( e );
        }

    }

    /**
     * @param gene transient instance that will be used to provide information to update persistent version.
     * @return new or updated gene instance.
     */
    @Override
    @Transactional
    public Gene persistOrUpdate( Gene gene ) {

        if ( gene == null )
            return null;

        Gene existingGene;
        if ( gene.getId() != null ) {
            existingGene = geneDao.load( gene.getId() );
        } else {
            existingGene = geneDao.find( gene );
        }

        if ( existingGene == null ) {
            return this.persistGene( gene, false );
        }

        if ( AbstractPersister.log.isDebugEnabled() )
            AbstractPersister.log.debug( "Updating " + existingGene );

        /*
         * This allows stale data to exist in this Session, but flushing prematurely causes constraint violations.
         * Probably we should fix this some other way.
         */
        this.getSession().setFlushMode( FlushMode.COMMIT );

        return this.updateGene( existingGene, gene );

    }

    private void fillChromosomeLocationAssociations( ChromosomeLocation chromosomeLocation, Taxon t ) {
        if ( chromosomeLocation == null )
            return;
        Chromosome chromosome = chromosomePersister.persistChromosome( chromosomeLocation.getChromosome(), t );
        chromosomeLocation.setChromosome( chromosome );
    }

    /**
     * Update a gene.
     *
     * @param newGeneInfo the non-persistent gene we are copying information from
     */
    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public Gene updateGene( Gene existingGene, Gene newGeneInfo ) {

        // NCBI id can be null if gene has been loaded from a gene info file.
        Integer existingNcbiId = existingGene.getNcbiGeneId();
        if ( existingNcbiId != null && !existingNcbiId.equals( newGeneInfo.getNcbiGeneId() ) ) {
            AbstractPersister.log
                    .info( "NCBI ID Change for " + existingGene + ", new id =" + newGeneInfo.getNcbiGeneId() );

            String previousIdString = newGeneInfo.getPreviousNcbiId();
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
                    }
                }

                if ( !found ) {
                    throw new IllegalStateException( "The NCBI ID for " + newGeneInfo
                            + " has changed and the previous NCBI id on record with NCBI (" + newGeneInfo
                            .getPreviousNcbiId()
                            + ") doesn't match." );
                }
            }

            // swap
            existingGene.setPreviousNcbiId( existingGene.getNcbiGeneId().toString() );
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
         * We might want to change this behaviour to clear the value if the updated one has none. For now I just want to
         * avoid wiping data.
         */
        if ( StringUtils.isNotBlank( newGeneInfo.getEnsemblId() ) ) {
            existingGene.setEnsemblId( newGeneInfo.getEnsemblId() );
        }

        // We assume the taxon hasn't changed.

        Map<String, DatabaseEntry> updatedacMap = new HashMap<>();
        for ( DatabaseEntry de : existingGene.getAccessions() ) {
            updatedacMap.put( de.getAccession(), de );
        }
        for ( DatabaseEntry de : newGeneInfo.getAccessions() ) {
            if ( !updatedacMap.containsKey( de.getAccession() ) ) {
                databaseEntryPersister.persist( de );
                existingGene.getAccessions().add( de );
            }
        }

        existingGene.setName( newGeneInfo.getName() );
        existingGene.setDescription( newGeneInfo.getDescription() );
        existingGene.setOfficialName( newGeneInfo.getOfficialName() );
        existingGene.setOfficialSymbol( newGeneInfo.getOfficialSymbol() );
        existingGene.setPhysicalLocation( newGeneInfo.getPhysicalLocation() );

        this.fillChromosomeLocationAssociations( existingGene.getPhysicalLocation(), existingGene.getTaxon() );

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
                AbstractPersister.log.debug( "Updating gene product based on name: " + newGeneProductInfo );
                GeneProduct existingGeneProduct = updatedGpMap.get( newGeneProductInfo.getName() );
                this.geneProductPersister.updateGeneProduct( existingGeneProduct, newGeneProductInfo );
            } else if ( updatedGpMap.containsKey( newGeneProductInfo.getNcbiGi() ) ) {
                AbstractPersister.log.debug( "Updating gene product based on GI: " + newGeneProductInfo );
                GeneProduct existingGeneProduct = updatedGpMap.get( newGeneProductInfo.getNcbiGi() );
                this.geneProductPersister.updateGeneProduct( existingGeneProduct, newGeneProductInfo );
            } else {
                GeneProduct existingGeneProduct = geneProductDao.find( newGeneProductInfo );
                if ( existingGeneProduct == null ) {
                    // it is, in fact, new, so far as we can tell.
                    newGeneProductInfo.setGene( existingGene );
                    this.geneProductPersister.fillInGeneProductAssociations( newGeneProductInfo );
                    AbstractPersister.log.info( "New product for " + existingGene + ": " + newGeneProductInfo );
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

                            AbstractPersister.log
                                    .warn( "Switching gene product from one gene to another: " + existingGeneProduct
                                            + " switching to " + geneInfo
                                            + " (this can also happen if an mRNA is associated with two genes, which we don't allow, so we switch it arbitrarily)" );

                            // Here we just remove its old association.
                            oldGeneForExistingGeneProduct = geneDao.thaw( oldGeneForExistingGeneProduct );
                            oldGeneForExistingGeneProduct.getProducts().remove( existingGeneProduct );
                            log.info( "Switch: Removing " + existingGeneProduct + " from " + oldGeneForExistingGeneProduct + " GI="
                                    + existingGeneProduct.getNcbiGi() );
                            geneDao.update( oldGeneForExistingGeneProduct );

                            if ( oldGeneForExistingGeneProduct.getProducts().isEmpty() ) {
                                AbstractPersister.log
                                        .warn( "Gene has no products left after removing that gene product (but it might change later): "
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
                        AbstractPersister.log.info( "Attaching orphaned gene product to " + existingGene + " : "
                                + existingGeneProduct );
                    }

                    existingGeneProduct.setGene( existingGene );
                    existingGene.getProducts().add( existingGeneProduct );
                    assert existingGeneProduct.getGene().equals( existingGene );

                    this.geneProductPersister.updateGeneProduct( existingGeneProduct, newGeneProductInfo );

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
            AbstractPersister.log.debug( "No products left for: " + existingGene );
        }

        return existingGene;
    }

    private void removeGeneProducts( Collection<GeneProduct> toRemove ) {
        Collection<BlatAssociation> associations = this.blatAssociationDao.find( toRemove );
        if ( !associations.isEmpty() ) {
            AbstractPersister.log
                    .info( "Removing " + associations.size() + " blat associations involving up to " + toRemove.size()
                            + " products." );
            this.blatAssociationDao.remove( associations );
        }

        Collection<AnnotationAssociation> annotationAssociations = this.annotationAssociationDao.find( toRemove );
        if ( !annotationAssociations.isEmpty() ) {
            AbstractPersister.log
                    .info( "Removing " + annotationAssociations.size() + " annotationAssociations involving up to "
                            + toRemove.size() + " products." );
            this.annotationAssociationDao.remove( annotationAssociations );
        }

        // might need to add referenceAssociations also.
        // remove associations to database entries that are still associated with sequences.
        for ( GeneProduct gp : toRemove ) {
            Collection<DatabaseEntry> accessions = gp.getAccessions();
            Collection<DatabaseEntry> toRelease = new HashSet<>();
            for ( DatabaseEntry de : accessions ) {
                if ( this.bioSequenceDao.findByAccession( de ) != null ) {
                    toRelease.add( de );
                }
            }
            gp.getAccessions().removeAll( toRelease );
            this.geneProductDao.remove( gp );

        }
    }


    /**
     * Check for deletions or changed GIs. If we have a GI that is not in the collection, then we might remove it from
     * the system.
     *
     * @param usedGIs return toRemove
     */
    private Collection<GeneProduct> handleGeneProductChangedGIs( Gene existingGene, Map<String, GeneProduct> usedGIs ) {
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
                    // this is the only way we can tell it is the same. Since Genbank Accessions are good
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
                        AbstractPersister.log.warn( "Another gene product with the same intended GI will be deleted: "
                                + existingGp );
                        deleteIt = true;
                        continue;
                    }

                    // ok
                    AbstractPersister.log.warn( "Updating the GI for " + existingGp + " -> GI:" + ngp.getNcbiGi() );
                    existingGp.setNcbiGi( ngp.getNcbiGi() );
                    deleteIt = false;
                    switchedGis.add( ngp.getNcbiGi() );
                    continue;

                }

                // handle less common cases, largely due to database cruft.
                otherGpUsingThisGi = geneProductDao.thaw( otherGpUsingThisGi );

                Gene oldGeneForExistingGeneProduct = otherGpUsingThisGi.getGene();
                if ( oldGeneForExistingGeneProduct == null ) {
                    AbstractPersister.log.warn( "Updating the GI for " + existingGp + " -> GI:" + ngp.getNcbiGi()
                            + " and deleting orphan GP with same GI: " + otherGpUsingThisGi );

                    existingGp.setNcbiGi( ngp.getNcbiGi() );
                    // remove the old one, which was an orphan already.
                    toRemove.add( otherGpUsingThisGi );
                    deleteIt = false;
                } else if ( oldGeneForExistingGeneProduct.equals( existingGene ) ) {
                    // this is the common case, for crufted database.
                    AbstractPersister.log
                            .warn( "Removing outdated gp for which there is already an existing copy: " + existingGp
                                    + " (already have " + otherGpUsingThisGi + ")" );
                    deleteIt = true;
                } else {
                    /*
                     * That GI is associated with another gene's products. In effect, switch it to this gene. This
                     * should not generally happen.
                     */
                    AbstractPersister.log
                            .warn( "Removing gene product: " + otherGpUsingThisGi + " and effectively switching to "
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
                AbstractPersister.log.warn( "Removing gene product from system: " + existingGp
                        + ", it is no longer listed as a product of " + existingGene );
            }
        } // over this gene's gene products.

        // finalize any deletions.
        if ( !toRemove.isEmpty() ) {
            existingGene.getProducts().removeAll( toRemove );
        }

        return toRemove;
    }

}
