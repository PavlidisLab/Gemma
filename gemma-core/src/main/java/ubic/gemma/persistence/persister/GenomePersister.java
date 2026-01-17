/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.persistence.persister;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.sequence.SequenceBinUtils;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.*;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.SequenceSimilaritySearchResult;
import ubic.gemma.persistence.service.genome.ChromosomeDao;
import ubic.gemma.persistence.service.genome.GeneDao;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceDao;
import ubic.gemma.persistence.service.genome.gene.GeneProductDao;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.AnnotationAssociationDao;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.BlatAssociationDao;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.BlatResultDao;
import ubic.gemma.persistence.service.genome.taxon.TaxonDao;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author pavlidis
 */
public abstract class GenomePersister extends CommonPersister {

    @Autowired
    private GeneDao geneDao;
    @Autowired
    private ChromosomeDao chromosomeDao;
    @Autowired
    private GeneProductDao geneProductDao;
    @Autowired
    private BioSequenceDao bioSequenceDao;
    @Autowired
    private TaxonDao taxonDao;
    @Autowired
    private BlatAssociationDao blatAssociationDao;
    @Autowired
    private BlatResultDao blatResultDao;
    @Autowired
    private AnnotationAssociationDao annotationAssociationDao;

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends Identifiable> T doPersist( T entity, Caches caches ) {
        if ( entity instanceof Gene ) {
            return ( T ) this.persistGene( ( Gene ) entity, caches );
        } else if ( entity instanceof GeneProduct ) {
            return ( T ) this.persistGeneProduct( ( GeneProduct ) entity, caches );
        } else if ( entity instanceof BioSequence ) {
            return ( T ) this.persistBioSequence( ( BioSequence ) entity, caches );
        } else if ( entity instanceof Taxon ) {
            return ( T ) this.persistTaxon( ( Taxon ) entity, caches );
        } else if ( entity instanceof BioSequence2GeneProduct ) {
            return ( T ) this.persistBioSequence2GeneProduct( ( BioSequence2GeneProduct ) entity, caches );
        } else if ( entity instanceof SequenceSimilaritySearchResult ) {
            return ( T ) this.persistSequenceSimilaritySearchResult( ( SequenceSimilaritySearchResult ) entity, caches );
        } else if ( entity instanceof Chromosome ) {
            return ( T ) this.persistChromosome( ( Chromosome ) entity, null, caches );
        } else {
            return super.doPersist( entity, caches );
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends Identifiable> T doPersistOrUpdate( T entity, Caches caches ) {
        if ( entity instanceof BioSequence ) {
            return ( T ) this.persistOrUpdateBioSequence( ( BioSequence ) entity, caches );
        } else if ( entity instanceof Gene ) {
            return ( T ) this.persistOrUpdateGene( ( Gene ) entity, caches );
        } else if ( entity instanceof GeneProduct ) {
            return ( T ) this.persistOrUpdateGeneProduct( ( GeneProduct ) entity, caches );
        } else {
            return super.doPersistOrUpdate( entity, caches );
        }
    }

    /**
     * Update a gene.
     *
     * @param newGeneInfo the non-persistent gene we are copying information from
     */
    private Gene updateGene( Gene existingGene, Gene newGeneInfo, Caches caches ) {

        // NCBI id can be null if gene has been loaded from a gene info file.
        Integer existingNcbiId = existingGene.getNcbiGeneId();
        if ( existingNcbiId != null && !existingNcbiId.equals( newGeneInfo.getNcbiGeneId() ) ) {
            AbstractPersister.log
                    .info( "NCBI ID Change for " + existingGene + ", new id =" + newGeneInfo.getNcbiGeneId() );

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
                this.fillInDatabaseEntry( de, caches );
                existingGene.getAccessions().add( de );
            }
        }

        existingGene.setName( newGeneInfo.getName() );
        existingGene.setDescription( newGeneInfo.getDescription() );
        existingGene.setOfficialName( newGeneInfo.getOfficialName() );
        existingGene.setOfficialSymbol( newGeneInfo.getOfficialSymbol() );
        existingGene.setPhysicalLocation( newGeneInfo.getPhysicalLocation() );

        this.fillChromosomeLocationAssociations( existingGene.getPhysicalLocation(), existingGene.getTaxon(), caches );

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
                this.updateGeneProduct( existingGeneProduct, newGeneProductInfo, caches );
            } else if ( updatedGpMap.containsKey( newGeneProductInfo.getNcbiGi() ) ) {
                AbstractPersister.log.debug( "Updating gene product based on GI: " + newGeneProductInfo );
                GeneProduct existingGeneProduct = updatedGpMap.get( newGeneProductInfo.getNcbiGi() );
                this.updateGeneProduct( existingGeneProduct, newGeneProductInfo, caches );
            } else {
                GeneProduct existingGeneProduct = geneProductDao.find( newGeneProductInfo );
                if ( existingGeneProduct == null ) {
                    // it is, in fact, new, so far as we can tell.
                    newGeneProductInfo.setGene( existingGene );
                    this.fillInGeneProductAssociations( newGeneProductInfo, caches );
                    AbstractPersister.log.debug( "New product for " + existingGene + ": " + newGeneProductInfo );
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
                            log.debug( "Switch: Removing " + existingGeneProduct + " from " + oldGeneForExistingGeneProduct + " GI="
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
                        AbstractPersister.log.debug( "Attaching orphaned gene product to " + existingGene + " : "
                                + existingGeneProduct );
                    }

                    existingGeneProduct.setGene( existingGene );
                    existingGene.getProducts().add( existingGeneProduct );
                    assert existingGeneProduct.getGene().equals( existingGene );

                    this.updateGeneProduct( existingGeneProduct, newGeneProductInfo, caches );

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

    protected BioSequence persistBioSequence( BioSequence bioSequence, Caches caches ) {
        BioSequence existingBioSequence = bioSequenceDao.find( bioSequence );

        // try to avoid making the instance 'dirty' if we don't have to, to avoid updates.
        if ( existingBioSequence != null ) {
            if ( AbstractPersister.log.isDebugEnabled() )
                AbstractPersister.log.debug( "Found existing: " + existingBioSequence );
            return existingBioSequence;
        }

        return this.persistNewBioSequence( bioSequence, caches );
    }

    protected Gene persistGene( Gene gene, Caches caches ) {
        return this.persistGene( gene, true, caches );
    }

    protected Taxon persistTaxon( Taxon taxon, Caches caches ) {
        Map<Object, Taxon> seenTaxa = caches.getTaxonCache();

        // Avoid trips to the database to get the taxon.
        String scientificName = taxon.getScientificName();
        String commonName = taxon.getCommonName();
        Integer ncbiId = taxon.getNcbiId();

        if ( ncbiId != null && seenTaxa.containsKey( ncbiId ) ) {
            return seenTaxa.get( ncbiId );
        } else if ( scientificName != null && seenTaxa.containsKey( scientificName.toLowerCase() ) ) {
            return seenTaxa.get( scientificName.toLowerCase() );
        } else if ( commonName != null && seenTaxa.containsKey( commonName.toLowerCase() ) ) {
            return seenTaxa.get( commonName.toLowerCase() );
        } else {
            Taxon fTaxon = taxonDao.findOrCreate( taxon );
            assert fTaxon != null;
            assert fTaxon.getId() != null;

            if ( AbstractPersister.log.isDebugEnabled() )
                AbstractPersister.log.debug( "Fetched or created taxon " + fTaxon );

            if ( fTaxon.getScientificName() != null ) {
                seenTaxa.put( fTaxon.getScientificName().toLowerCase(), fTaxon );
            }
            if ( fTaxon.getCommonName() != null ) {
                seenTaxa.put( fTaxon.getCommonName().toLowerCase(), fTaxon );
            }
            if ( fTaxon.getNcbiId() != null ) {
                seenTaxa.put( fTaxon.getNcbiId(), fTaxon );
            }

            return fTaxon;
        }
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

    private void fillInBioSequenceTaxon( BioSequence bioSequence, Caches caches ) {
        Taxon t = bioSequence.getTaxon();
        if ( t == null )
            throw new IllegalArgumentException( "BioSequence Taxon cannot be null" );
        if ( t.getId() == null ) {
            bioSequence.setTaxon( this.persistTaxon( t, caches ) );
        }
    }

    private BioSequence2GeneProduct persistBioSequence2GeneProduct( BioSequence2GeneProduct bioSequence2GeneProduct, Caches caches ) {
        if ( bioSequence2GeneProduct instanceof BlatAssociation ) {
            return this.persistBlatAssociation( ( BlatAssociation ) bioSequence2GeneProduct, caches );
        }
        throw new UnsupportedOperationException(
                "Don't know how to deal with " + bioSequence2GeneProduct.getClass().getName() );

    }

    private BioSequence2GeneProduct persistBlatAssociation( BlatAssociation association, Caches caches ) {
        BlatResult blatResult = association.getBlatResult();
        if ( blatResult.getId() == null ) {
            association.setBlatResult( blatResultDao.create( blatResult ) );
        }
        if ( AbstractPersister.log.isDebugEnabled() ) {
            AbstractPersister.log.debug( "Persisting " + association );
        }
        association.setGeneProduct( this.persistGeneProduct( association.getGeneProduct(), caches ) );
        association.setBioSequence( this.persistBioSequence( association.getBioSequence(), caches ) );
        return blatAssociationDao.create( association );
    }

    private Gene persistGene( Gene gene, boolean checkFirst, Caches caches ) {
        if ( checkFirst ) {
            Gene existingGene = geneDao.find( gene );

            if ( existingGene != null ) {
                if ( AbstractPersister.log.isDebugEnabled() )
                    AbstractPersister.log.debug( "Gene exists, will not update" );
                return existingGene;
            }
        }

        if ( !gene.getAccessions().isEmpty() ) {
            for ( DatabaseEntry de : gene.getAccessions() ) {
                this.fillInDatabaseEntry( de, caches );
            }
        }

        Collection<GeneProduct> tempGeneProduct = gene.getProducts();
        gene.setProducts( null );
        if ( gene.getTaxon() != null ) {
            gene.setTaxon( this.persistTaxon( gene.getTaxon(), caches ) );
        }
        if ( gene.getPhysicalLocation() != null ) {
            this.fillChromosomeLocationAssociations( gene.getPhysicalLocation(), gene.getTaxon(), caches );
        }

        if ( AbstractPersister.log.isDebugEnabled() )
            AbstractPersister.log.debug( "New gene: " + gene );
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
            this.fillInGeneProductAssociations( gp, caches );
        }

        try {
            // we do a separate create because the cascade doesn't trigger auditing correctly - otherwise the
            // products are not persistent until the session is flushed, later. There might be a better way around this,
            // but so far as I know this is the only place this happens.
            gene.setProducts( new HashSet<>( geneProductDao.create( gene.getProducts() ) ) );
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

    private GeneProduct persistGeneProduct( GeneProduct geneProduct, Caches caches ) {
        GeneProduct existing = geneProductDao.find( geneProduct );

        if ( existing != null ) {
            if ( AbstractPersister.log.isDebugEnabled() )
                AbstractPersister.log.debug( geneProduct + " exists, will not update" );
            return existing;
        }

        if ( AbstractPersister.log.isDebugEnabled() )
            AbstractPersister.log.debug( "*** New: " + geneProduct + " *** " );

        this.fillInGeneProductAssociations( geneProduct, caches );

        if ( geneProduct.getGene().getId() == null ) {
            // this results in the persistence of the gene products, but only if the gene is transient.
            geneProduct.setGene( this.persistGene( geneProduct.getGene(), caches ) );
        } else {
            geneProduct = geneProductDao.create( geneProduct );
        }

        if ( geneProduct.getId() == null ) {
            return geneProductDao.create( geneProduct );
        }

        return geneProduct;

    }

    private BioSequence persistOrUpdateBioSequence( BioSequence bioSequence, Caches caches ) {
        // Note that this method is only really used by the ArrayDesignSequencePersister: it's for filling in
        //information about probes on arrays.
        BioSequence existingBioSequence = bioSequenceDao.find( bioSequence );

        if ( existingBioSequence == null ) {
            if ( AbstractPersister.log.isDebugEnabled() )
                AbstractPersister.log.debug( "Creating new: " + bioSequence );
            return this.persistNewBioSequence( bioSequence, caches );
        }

        if ( AbstractPersister.log.isDebugEnabled() )
            AbstractPersister.log.debug( "Found existing: " + existingBioSequence );

        // the sequence is the main field we might update.
        if ( bioSequence.getSequence() != null && !bioSequence.getSequence()
                .equals( existingBioSequence.getSequence() ) ) {
            if ( AbstractPersister.log.isDebugEnabled() )
                log.debug( "Updating sequence:" + bioSequence.getName() + "\nFROM:" + existingBioSequence.getSequence()
                        + "\nTO:" + bioSequence.getSequence() + "\n" );
            existingBioSequence.setSequence( bioSequence.getSequence() );
        }

        /*
         * Can do for all fields that might not be the same: anything besides the name and taxon.
         */
        if ( bioSequence.getDescription() != null && !bioSequence.getDescription()
                .equals( existingBioSequence.getDescription() ) ) {
            existingBioSequence.setDescription( bioSequence.getDescription() );
        }

        if ( bioSequence.getType() != null && !bioSequence.getType().equals( existingBioSequence.getType() ) ) {
            existingBioSequence.setType( bioSequence.getType() );
        }

        if ( bioSequence.getFractionRepeats() != null && !bioSequence.getFractionRepeats()
                .equals( existingBioSequence.getFractionRepeats() ) ) {
            existingBioSequence.setFractionRepeats( bioSequence.getFractionRepeats() );
        }

        if ( bioSequence.getLength() != null && !bioSequence.getLength().equals( existingBioSequence.getLength() ) ) {
            existingBioSequence.setLength( bioSequence.getLength() );
        }

        if ( bioSequence.getIsCircular() != null && !bioSequence.getIsCircular()
                .equals( existingBioSequence.getIsCircular() ) ) {
            existingBioSequence.setIsCircular( bioSequence.getIsCircular() );
        }

        if ( bioSequence.getPolymerType() != null && !bioSequence.getPolymerType()
                .equals( existingBioSequence.getPolymerType() ) ) {
            existingBioSequence.setPolymerType( bioSequence.getPolymerType() );
        }

        if ( bioSequence.getSequenceDatabaseEntry() != null && !bioSequence.getSequenceDatabaseEntry()
                .equals( existingBioSequence.getSequenceDatabaseEntry() ) ) {
            existingBioSequence.setSequenceDatabaseEntry( this.doPersist( bioSequence.getSequenceDatabaseEntry(), caches ) );
        }

        // I don't fully understand what's going on here, but if we don't do this we fail to synchronize changes.
        this.getSessionFactory().getCurrentSession().evict( existingBioSequence );
        bioSequenceDao.update( existingBioSequence ); // also tried to merge, without the update, doesn't work.
        return existingBioSequence;
    }

    /**
     * @param gene transient instance that will be used to provide information to update persistent version.
     * @return new or updated gene instance.
     */
    private Gene persistOrUpdateGene( Gene gene, Caches caches ) {
        Gene existingGene;
        if ( gene.getId() != null ) {
            existingGene = geneDao.load( gene.getId() );
        } else {
            existingGene = geneDao.find( gene );
        }

        if ( existingGene == null ) {
            return this.persistGene( gene, false, caches );
        }

        if ( AbstractPersister.log.isDebugEnabled() )
            AbstractPersister.log.debug( "Updating " + existingGene );

        return this.updateGene( existingGene, gene, caches );
    }

    private GeneProduct persistOrUpdateGeneProduct( GeneProduct geneProduct, Caches caches ) {
        GeneProduct existing;
        if ( geneProduct.getId() != null ) {
            existing = geneProductDao.load( geneProduct.getId() );
        } else {
            existing = geneProductDao.find( geneProduct );
        }

        if ( existing == null ) {
            return this.persistGeneProduct( geneProduct, caches );
        }

        this.updateGeneProduct( existing, geneProduct, caches );

        return existing;
    }

    private void addAnyNewAccessions( GeneProduct existing, GeneProduct geneProduct, Caches caches ) {
        Map<String, DatabaseEntry> updatedGpMap = new HashMap<>();
        existing = geneProductDao.thaw( existing );
        for ( DatabaseEntry de : existing.getAccessions() ) {
            updatedGpMap.put( de.getAccession(), de );
        }
        for ( DatabaseEntry de : geneProduct.getAccessions() ) {
            if ( !updatedGpMap.containsKey( de.getAccession() ) ) {
                this.fillInDatabaseEntry( de, caches );
                existing.getAccessions().add( de );
            }
        }
    }

    private void fillChromosomeLocationAssociations( ChromosomeLocation chromosomeLocation, Taxon t, Caches caches ) {
        if ( chromosomeLocation.getChromosome() != null ) {
            chromosomeLocation.setChromosome( this.persistChromosome( chromosomeLocation.getChromosome(), t, caches ) );
        }
    }

    private void fillInGeneProductAssociations( GeneProduct geneProduct, Caches caches ) {
        if ( geneProduct.getPhysicalLocation() != null ) {
            geneProduct.getPhysicalLocation().setChromosome(
                    this.persistChromosome( geneProduct.getPhysicalLocation().getChromosome(),
                            geneProduct.getGene().getTaxon(), caches ) );
        }

        if ( geneProduct.getAccessions() != null ) {
            for ( DatabaseEntry de : geneProduct.getAccessions() ) {
                de.setExternalDatabase( this.persistExternalDatabase( de.getExternalDatabase(), caches ) );
            }
        }
    }

    private PhysicalLocation fillPhysicalLocationAssociations( PhysicalLocation physicalLocation, Caches caches ) {
        physicalLocation.setChromosome( this.persistChromosome( physicalLocation.getChromosome(), null, caches ) );

        if ( physicalLocation.getBin() == null && physicalLocation.getNucleotide() != null
                && physicalLocation.getNucleotideLength() != null ) {
            physicalLocation.setBin( SequenceBinUtils.binFromRange( physicalLocation.getNucleotide().intValue(),
                    physicalLocation.getNucleotide().intValue() + physicalLocation.getNucleotideLength() ) );
        }

        return physicalLocation;
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

    private void persistBioSequenceAssociations( BioSequence bioSequence, Caches caches ) {
        this.fillInBioSequenceTaxon( bioSequence, caches );

        if ( bioSequence.getSequenceDatabaseEntry() != null
                && bioSequence.getSequenceDatabaseEntry().getExternalDatabase().getId() == null ) {
            bioSequence.getSequenceDatabaseEntry().setExternalDatabase(
                    this.persistExternalDatabase( bioSequence.getSequenceDatabaseEntry().getExternalDatabase(), caches ) );
        }

        for ( BioSequence2GeneProduct bioSequence2GeneProduct : bioSequence.getBioSequence2GeneProduct() ) {
            this.persistBioSequence2GeneProduct( bioSequence2GeneProduct, caches );
        }
    }

    /**
     * NOTE this method is not a regular 'persist' method: It does not use findOrCreate! A new result is made every
     * time.
     */
    private BlatResult persistBlatResult( BlatResult blatResult, Caches caches ) {
        if ( blatResult.getQuerySequence() == null ) {
            throw new IllegalArgumentException( "Blat result with null query sequence" );
        }
        blatResult.setQuerySequence( this.persistBioSequence( blatResult.getQuerySequence(), caches ) );
        blatResult.setTargetChromosome( this.persistChromosome( blatResult.getTargetChromosome(), null, caches ) );
        if ( blatResult.getSearchedDatabase() != null ) {
            blatResult.setSearchedDatabase( this.persistExternalDatabase( blatResult.getSearchedDatabase(), caches ) );
        }
        if ( blatResult.getTargetAlignedRegion() != null )
            blatResult.setTargetAlignedRegion(
                    this.fillPhysicalLocationAssociations( blatResult.getTargetAlignedRegion(), caches ) );
        return blatResultDao.create( blatResult );
    }

    private Chromosome persistChromosome( Chromosome chromosome, @Nullable Taxon t, Caches caches ) {
        Taxon ct = t;
        if ( ct == null ) {
            ct = chromosome.getTaxon();
        }

        // note that we can't use the native hashcode method because we need to ignore the ID.
        int key = chromosome.getName().hashCode();
        if ( ct.getNcbiId() != null ) {
            key += ct.getNcbiId().hashCode();
        } else if ( ct.getCommonName() != null ) {
            key += ct.getCommonName().hashCode();
        } else if ( ct.getScientificName() != null ) {
            key += ct.getScientificName().hashCode();
        }

        Map<Integer, Chromosome> seenChromosomes = caches.getChromosomeCache();

        if ( seenChromosomes.containsKey( key ) ) {
            return seenChromosomes.get( key );
        }

        Collection<Chromosome> chromosomes = chromosomeDao.find( chromosome.getName(), ct );

        if ( chromosomes == null || chromosomes.isEmpty() ) {

            // no point in doing this if it already exists.
            try {
                FieldUtils.writeField( chromosome, "taxon", this.doPersist( ct, caches ), true );
                if ( chromosome.getSequence() != null ) {
                    // cascade should do?
                    FieldUtils.writeField( chromosome, "sequence", this.doPersist( chromosome.getSequence(), caches ), true );
                }
                if ( chromosome.getAssemblyDatabase() != null ) {
                    FieldUtils.writeField( chromosome, "assemblyDatabase",
                            this.doPersist( chromosome.getAssemblyDatabase(), caches ), true );
                }
            } catch ( IllegalAccessException e ) {
                throw new IllegalArgumentException( e );
            }
            chromosome = chromosomeDao.create( chromosome );
        } else if ( chromosomes.size() == 1 ) {
            chromosome = chromosomes.iterator().next();
        } else {
            throw new IllegalArgumentException( "Non-unique chromosome name  " + chromosome.getName() + " on " + ct );
        }

        seenChromosomes.put( key, chromosome );
        if ( chromosome == null || chromosome.getId() == null )
            throw new IllegalStateException( "Failed to get a persistent chromosome instance" );
        return chromosome;

    }

    private BioSequence persistNewBioSequence( BioSequence bioSequence, Caches caches ) {
        if ( AbstractPersister.log.isDebugEnabled() )
            AbstractPersister.log.debug( "Creating new: " + bioSequence );

        this.persistBioSequenceAssociations( bioSequence, caches );

        assert bioSequence.getTaxon().getId() != null;
        return bioSequenceDao.create( bioSequence );
    }

    private SequenceSimilaritySearchResult persistSequenceSimilaritySearchResult(
            SequenceSimilaritySearchResult result, Caches caches ) {
        if ( result instanceof BlatResult ) {
            return this.persistBlatResult( ( BlatResult ) result, caches );
        }
        throw new UnsupportedOperationException( "Don't know how to persist a " + result.getClass().getName() );

    }

    /**
     * @param updatedGeneProductInfo information from this is copied onto the 'existing' gene product.
     */
    private void updateGeneProduct( GeneProduct existingGeneProduct, GeneProduct updatedGeneProductInfo, Caches caches ) {
        Gene geneForExistingGeneProduct = existingGeneProduct.getGene();

        existingGeneProduct = geneProductDao.thaw( existingGeneProduct );

        // Update all the fields. Note that usually, some of these can't have changed, or we wouldn't have even
        // found the 'existing' one (name GI in particular); however, sometimes we are updating this information

        existingGeneProduct.setName( updatedGeneProductInfo.getName() );
        existingGeneProduct.setDescription( updatedGeneProductInfo.getDescription() );
        existingGeneProduct.setNcbiGi( updatedGeneProductInfo.getNcbiGi() );

        this.addAnyNewAccessions( existingGeneProduct, updatedGeneProductInfo, caches );

        existingGeneProduct.setPhysicalLocation( updatedGeneProductInfo.getPhysicalLocation() );
        if ( existingGeneProduct.getPhysicalLocation() != null ) {
            existingGeneProduct.getPhysicalLocation().setChromosome(
                    this.persistChromosome( existingGeneProduct.getPhysicalLocation().getChromosome(),
                            geneForExistingGeneProduct.getTaxon(), caches ) );
        }

    }
}
