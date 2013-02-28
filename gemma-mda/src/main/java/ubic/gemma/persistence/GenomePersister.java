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
package ubic.gemma.persistence;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.FlushMode;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.ChromosomeDao;
import ubic.gemma.model.genome.ChromosomeLocation;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneDao;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonDao;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceDao;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductDao;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociationDao;
import ubic.gemma.model.genome.sequenceAnalysis.BlastResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlastResultDao;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationDao;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao;
import ubic.gemma.model.genome.sequenceAnalysis.SequenceSimilaritySearchResult;
import ubic.gemma.util.SequenceBinUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
abstract public class GenomePersister extends CommonPersister {

    @Autowired
    protected GeneDao geneDao;

    @Autowired
    protected ChromosomeDao chromosomeDao;

    @Autowired
    protected GeneProductDao geneProductDao;

    @Autowired
    protected BioSequenceDao bioSequenceDao;

    @Autowired
    protected TaxonDao taxonDao;

    @Autowired
    protected BlatAssociationDao blatAssociationDao;

    @Autowired
    protected BlatResultDao blatResultDao;

    @Autowired
    protected BlastResultDao blastResultDao;

    @Autowired
    protected AnnotationAssociationDao annotationAssociationDao;

    protected Map<Object, Taxon> seenTaxa = new HashMap<Object, Taxon>();

    protected Map<Object, Chromosome> seenChromosomes = new HashMap<Object, Chromosome>();

    protected boolean firstBioSequence = false;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.persister.Persister#persist(java.lang.Object)
     */
    @Override
    public Object persist( Object entity ) {
        if ( entity instanceof Gene ) {
            return persistGene( ( Gene ) entity );
        } else if ( entity instanceof GeneProduct ) {
            return persistGeneProduct( ( GeneProduct ) entity );
        } else if ( entity instanceof BioSequence ) {
            return persistBioSequence( ( BioSequence ) entity );
        } else if ( entity instanceof Taxon ) {
            return persistTaxon( ( Taxon ) entity );
        } else if ( entity instanceof BioSequence2GeneProduct ) {
            return persistBioSequence2GeneProduct( ( BioSequence2GeneProduct ) entity );
        } else if ( entity instanceof SequenceSimilaritySearchResult ) {
            return persistSequenceSimilaritySearchResult( ( SequenceSimilaritySearchResult ) entity );
        } else if ( entity instanceof Chromosome ) {
            return persistChromosome( ( Chromosome ) entity, null );
        }
        return super.persist( entity );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.CommonPersister#persistOrUpdate(java.lang.Object)
     */
    @Override
    public Object persistOrUpdate( Object entity ) {
        if ( entity == null ) return null;

        if ( entity instanceof BioSequence ) {
            return this.persistOrUpdateBioSequence( ( BioSequence ) entity );
        } else if ( entity instanceof Gene ) {
            return this.persistOrUpdateGene( ( Gene ) entity );
        } else if ( entity instanceof GeneProduct ) {
            return this.persistOrUpdateGeneProduct( ( GeneProduct ) entity );
        }

        return super.persistOrUpdate( entity );
    }

    /**
     * Update a gene.
     * 
     * @param existingGene
     * @param newGeneInfo the non-persistent gene we are copying information from
     * @return
     */
    public Gene updateGene( Gene existingGene, Gene newGeneInfo ) {

        // NCBI id can be null if gene has been loaded from a gene info file.
        Integer existingNcbiId = existingGene.getNcbiGeneId();
        if ( existingNcbiId != null && !existingNcbiId.equals( newGeneInfo.getNcbiGeneId() ) ) {
            log.info( "NCBI ID Change for " + existingGene + ", new id =" + newGeneInfo.getNcbiGeneId() );

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
                            + " has changed and the previous NCBI id on record with NCBI ("
                            + newGeneInfo.getPreviousNcbiId() + ") doesn't match." );
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

        Map<String, DatabaseEntry> updatedacMap = new HashMap<String, DatabaseEntry>();
        for ( DatabaseEntry de : existingGene.getAccessions() ) {
            updatedacMap.put( de.getAccession(), de );
        }
        for ( DatabaseEntry de : newGeneInfo.getAccessions() ) {
            if ( !updatedacMap.containsKey( de.getAccession() ) ) {
                fillInDatabaseEntry( de );
                existingGene.getAccessions().add( de );
            }
        }

        existingGene.setName( newGeneInfo.getName() );
        existingGene.setDescription( newGeneInfo.getDescription() );
        existingGene.setOfficialName( newGeneInfo.getOfficialName() );
        existingGene.setOfficialSymbol( newGeneInfo.getOfficialSymbol() );
        existingGene.setPhysicalLocation( newGeneInfo.getPhysicalLocation() );
        existingGene.setCytogenicLocation( newGeneInfo.getCytogenicLocation() );

        fillChromosomeLocationAssociations( existingGene.getPhysicalLocation(), existingGene.getTaxon() );
        fillChromosomeLocationAssociations( existingGene.getCytogenicLocation(), existingGene.getTaxon() );

        existingGene.getAliases().clear();
        existingGene.getAliases().addAll( newGeneInfo.getAliases() );

        /*
         * This is the only tricky part - the gene products. We update them if they are already there, and add them if
         * not. We do not normally delete 'old' ones that the new gene instance does not have, because they might be
         * from different sources. For example, Ensembl or GoldenPath. -- UNLESS the product has an NCBI GI because we
         * know those come from NCBI.
         */
        Map<String, GeneProduct> updatedGpMap = new HashMap<String, GeneProduct>();

        for ( GeneProduct existingGp : existingGene.getProducts() ) {
            updatedGpMap.put( existingGp.getName(), existingGp );
            updatedGpMap.put( existingGp.getNcbiGi(), existingGp );
        }

        Map<String, GeneProduct> usedGIs = new HashMap<String, GeneProduct>();
        for ( GeneProduct newGeneProductInfo : newGeneInfo.getProducts() ) {
            if ( updatedGpMap.containsKey( newGeneProductInfo.getName() ) ) {
                log.debug( "Updating gene product based on name: " + newGeneProductInfo );
                GeneProduct existingGeneProduct = updatedGpMap.get( newGeneProductInfo.getName() );
                updateGeneProduct( existingGeneProduct, newGeneProductInfo );
            } else if ( updatedGpMap.containsKey( newGeneProductInfo.getNcbiGi() ) ) {
                log.debug( "Updating gene product based on GI: " + newGeneProductInfo );
                GeneProduct existingGeneProduct = updatedGpMap.get( newGeneProductInfo.getNcbiGi() );
                updateGeneProduct( existingGeneProduct, newGeneProductInfo );
            } else {
                GeneProduct existingGeneProduct = geneProductDao.find( newGeneProductInfo );
                if ( existingGeneProduct == null ) {
                    // it is, in fact, new, so far as we can tell.
                    newGeneProductInfo.setGene( existingGene );
                    fillInGeneProductAssociations( newGeneProductInfo );
                    log.info( "New product for " + existingGene + ": " + newGeneProductInfo );
                    existingGene.getProducts().add( newGeneProductInfo );
                } else {
                    /*
                     * This can only happen if this gene product is associated with a different gene. This happens when
                     * a transcript is associated with two genes in NCBI, so the switching is actually not useful to us,
                     * but we do it anyway to be consistent (and in case it really does matter). It is rare. Causes can
                     * be 1) bicistronic genes such as human LUZP6 and MTPN; 2) genome-duplicated genes; or 3) an error
                     * in the data source. The problem for us is at this point in processing, we don't know if the gene
                     * is going to get 'reattached' to its original gene.
                     */
                    assert existingGeneProduct != null;
                    existingGeneProduct = geneProductDao.thaw( existingGeneProduct );
                    Gene oldGeneForExistingGeneProduct = existingGeneProduct.getGene();
                    if ( oldGeneForExistingGeneProduct != null ) {
                        Gene geneInfo = newGeneProductInfo.getGene(); // transient.
                        if ( !oldGeneForExistingGeneProduct.equals( geneInfo ) ) {
                            log.warn( "Switching gene product from one gene to another: "
                                    + existingGeneProduct
                                    + " switching to "
                                    + geneInfo
                                    + " (often this means an mRNA is associated with two genes, which we don't allow, so we switch it arbitrarily)" );

                            // / Here we just remove its old association.
                            oldGeneForExistingGeneProduct = geneDao.thaw( oldGeneForExistingGeneProduct );
                            oldGeneForExistingGeneProduct.getProducts().remove( existingGeneProduct );
                            geneDao.update( oldGeneForExistingGeneProduct );

                            if ( oldGeneForExistingGeneProduct.getProducts().isEmpty() ) {
                                log.warn( "Gene has no products left after removing that gene product (but it might change later): "
                                        + oldGeneForExistingGeneProduct );
                            }
                        }

                        assert !oldGeneForExistingGeneProduct.getProducts().contains( existingGeneProduct );
                    } else {
                        log.info( "Attaching orphaned gene product to " + existingGene + " : " + existingGeneProduct );
                    }

                    existingGeneProduct.setGene( existingGene );
                    existingGene.getProducts().add( existingGeneProduct );
                    assert existingGeneProduct.getGene().equals( existingGene );

                    updateGeneProduct( existingGeneProduct, newGeneProductInfo );

                }
            }

            if ( newGeneProductInfo.getNcbiGi() != null )
                usedGIs.put( newGeneProductInfo.getNcbiGi(), newGeneProductInfo );
        }

        Collection<GeneProduct> toRemove = new HashSet<GeneProduct>();

        if ( !usedGIs.isEmpty() ) {
            toRemove = handleGeneProductChangedGIs( existingGene, usedGIs );
        }

        geneDao.update( existingGene );

        if ( !toRemove.isEmpty() ) {
            removeGeneProducts( toRemove );
        }

        if ( existingGene.getProducts().isEmpty() ) {
            log.debug( "No products left for: " + existingGene );
        }

        return existingGene;
    }

    /**
     * FIXME this duplicates some code from GeneProductService, but we're using the DAOs here.
     * 
     * @param toRemove
     */
    protected void removeGeneProducts( Collection<GeneProduct> toRemove ) {
        Collection<? extends BlatAssociation> associations = this.blatAssociationDao.find( toRemove );
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
            Collection<DatabaseEntry> accessions = gp.getAccessions();
            Collection<DatabaseEntry> toRelease = new HashSet<DatabaseEntry>();
            for ( DatabaseEntry de : accessions ) {
                if ( this.bioSequenceDao.findByAccession( de ) != null ) {
                    toRelease.add( de );
                }
            }
            gp.getAccessions().removeAll( toRelease );
            this.geneProductDao.remove( gp );

        }
        // geneProductDao.remove( toRemove );
    }

    /**
     * @param bioSequence
     */
    protected void fillInBioSequenceTaxon( BioSequence bioSequence ) {
        Taxon t = bioSequence.getTaxon();
        if ( t == null ) throw new IllegalArgumentException( "BioSequence Taxon cannot be null" );
        if ( !isTransient( t ) ) return;

        bioSequence.setTaxon( persistTaxon( t ) );

    }

    /**
     * @param bioSequence
     */
    protected BioSequence persistBioSequence( BioSequence bioSequence ) {
        if ( bioSequence == null || !isTransient( bioSequence ) ) return bioSequence;

        BioSequence existingBioSequence = bioSequenceDao.find( bioSequence );

        // try to avoid making the instance 'dirty' if we don't have to, to avoid updates.
        if ( existingBioSequence != null ) {
            if ( log.isDebugEnabled() ) log.debug( "Found existing: " + existingBioSequence );
            return existingBioSequence;
        }

        return persistNewBioSequence( bioSequence );
    }

    /**
     * @param bioSequence2GeneProduct
     * @return
     */
    protected BioSequence2GeneProduct persistBioSequence2GeneProduct( BioSequence2GeneProduct bioSequence2GeneProduct ) {
        if ( bioSequence2GeneProduct == null ) return null;
        if ( !isTransient( bioSequence2GeneProduct ) ) return bioSequence2GeneProduct;

        if ( bioSequence2GeneProduct instanceof BlatAssociation ) {
            return persistBlatAssociation( ( BlatAssociation ) bioSequence2GeneProduct );
        }
        throw new UnsupportedOperationException( "Don't know how to deal with "
                + bioSequence2GeneProduct.getClass().getName() );

    }

    /**
     * @param association
     */
    protected BioSequence2GeneProduct persistBlatAssociation( BlatAssociation association ) {
        BlatResult blatResult = association.getBlatResult();
        if ( isTransient( blatResult ) ) {
            blatResult = blatResultDao.create( blatResult );
        }
        if ( log.isDebugEnabled() ) {
            log.debug( "Persisting " + association );
        }
        association.setGeneProduct( persistGeneProduct( association.getGeneProduct() ) );
        association.setBioSequence( persistBioSequence( association.getBioSequence() ) );
        return blatAssociationDao.create( association );
    }

    /**
     * @param gene
     * @return
     */
    protected Gene persistGene( Gene gene ) {
        return persistGene( gene, true );
    }

    /**
     * @param gene
     * @param checkFirst check if it exists already.
     */
    protected Gene persistGene( Gene gene, boolean checkFirst ) {
        if ( gene == null ) return null;
        if ( !isTransient( gene ) ) return gene;

        if ( checkFirst ) {
            Gene existingGene = geneDao.find( gene );

            if ( existingGene != null ) {
                if ( log.isDebugEnabled() ) log.debug( "Gene exists, will not update" );
                return existingGene;
            }
        }

        if ( gene.getAccessions().size() > 0 ) {
            for ( DatabaseEntry de : gene.getAccessions() ) {
                fillInDatabaseEntry( de );
            }
        }

        Collection<GeneProduct> tempGeneProduct = gene.getProducts();
        gene.setProducts( null );
        gene.setTaxon( persistTaxon( gene.getTaxon() ) );
        fillChromosomeLocationAssociations( gene.getPhysicalLocation(), gene.getTaxon() );
        fillChromosomeLocationAssociations( gene.getCytogenicLocation(), gene.getTaxon() );

        log.info( "New gene: " + gene );
        gene = geneDao.create( gene );

        Collection<GeneProduct> geneProductsForNewGene = new HashSet<GeneProduct>();
        for ( GeneProduct product : tempGeneProduct ) {
            GeneProduct existingProduct = geneProductDao.find( product );
            if ( existingProduct != null ) {
                /*
                 * A geneproduct is being moved to a gene that didn't exist in the system already
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
            fillInGeneProductAssociations( gp );
        }

        try {
            // // we do a separate create because the cascade doesn't trigger auditing correctly - otherwise the
            // products are not persistent until the session is flushed, later. There might be a better way around this,
            // but so far as I know this is the only place this happens.
            gene.setProducts( ( Collection<GeneProduct> ) geneProductDao.create( gene.getProducts() ) );
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

    /**
     * @param geneProduct
     * @return
     */
    protected GeneProduct persistGeneProduct( GeneProduct geneProduct ) {
        if ( geneProduct == null ) return null;
        if ( !isTransient( geneProduct ) ) return geneProduct;

        GeneProduct existing = geneProductDao.find( geneProduct );

        if ( existing != null ) {
            if ( log.isDebugEnabled() ) log.debug( geneProduct + " exists, will not update" );
            return existing;
        }

        if ( log.isDebugEnabled() ) log.debug( "*** New: " + geneProduct + " *** " );

        fillInGeneProductAssociations( geneProduct );

        if ( isTransient( geneProduct.getGene() ) ) {
            // this results in the persistence of the gene products, but only if the gene is transient.
            geneProduct.setGene( persistGene( geneProduct.getGene() ) );
        } else {
            geneProduct = geneProductDao.create( geneProduct );
        }

        if ( geneProduct.getId() == null ) {
            return geneProductDao.create( geneProduct );
        }

        return geneProduct;

    }

    /**
     * @param bioSequence
     * @return
     */
    protected BioSequence persistOrUpdateBioSequence( BioSequence bioSequence ) {
        if ( bioSequence == null ) return null;

        /*
         * Note that this method is only really used by the ArrayDesignSequencePersister: it's for filling in
         * information about probes on arrays.
         */

        BioSequence existingBioSequence = bioSequenceDao.find( bioSequence );

        if ( existingBioSequence == null ) {
            if ( log.isDebugEnabled() ) log.debug( "Creating new: " + bioSequence );
            return persistNewBioSequence( bioSequence );
        }

        if ( log.isDebugEnabled() ) log.debug( "Found existing: " + existingBioSequence );

        // the sequence is the main field we might update.
        if ( bioSequence.getSequence() != null && !bioSequence.getSequence().equals( existingBioSequence.getSequence() ) ) {
            existingBioSequence.setSequence( bioSequence.getSequence() );
        }

        /*
         * Can do for all fields that might not be the same: anything besides the name and taxon.
         */
        if ( bioSequence.getDescription() != null
                && !bioSequence.getDescription().equals( existingBioSequence.getDescription() ) ) {
            existingBioSequence.setDescription( bioSequence.getDescription() );
        }

        if ( bioSequence.getType() != null && !bioSequence.getType().equals( existingBioSequence.getType() ) ) {
            existingBioSequence.setType( bioSequence.getType() );
        }

        if ( bioSequence.getFractionRepeats() != null
                && !bioSequence.getFractionRepeats().equals( existingBioSequence.getFractionRepeats() ) ) {
            existingBioSequence.setFractionRepeats( bioSequence.getFractionRepeats() );
        }

        if ( bioSequence.getLength() != null && !bioSequence.getLength().equals( existingBioSequence.getLength() ) ) {
            existingBioSequence.setLength( bioSequence.getLength() );
        }

        if ( bioSequence.getIsCircular() != null
                && !bioSequence.getIsCircular().equals( existingBioSequence.getIsCircular() ) ) {
            existingBioSequence.setIsCircular( bioSequence.getIsCircular() );
        }

        if ( bioSequence.getPolymerType() != null
                && !bioSequence.getPolymerType().equals( existingBioSequence.getPolymerType() ) ) {
            existingBioSequence.setPolymerType( bioSequence.getPolymerType() );
        }

        if ( bioSequence.getSequenceDatabaseEntry() != null
                && !bioSequence.getSequenceDatabaseEntry().equals( existingBioSequence.getSequenceDatabaseEntry() ) ) {
            existingBioSequence.setSequenceDatabaseEntry( ( DatabaseEntry ) persist( bioSequence
                    .getSequenceDatabaseEntry() ) );
        }

        // biosequence2geneproduct?

        bioSequenceDao.update( existingBioSequence );

        return existingBioSequence;

    }

    /**
     * @param gene transient instance that will be used to provide information to update persistent version.
     * @return new or updated gene instance.
     */
    protected Gene persistOrUpdateGene( Gene gene ) {

        if ( gene == null ) return null;

        Gene existingGene = null;
        if ( gene.getId() != null ) {
            existingGene = geneDao.load( gene.getId() );
        } else {
            existingGene = geneDao.find( gene );
        }

        if ( existingGene == null ) {
            return persistGene( gene, false );
        }

        if ( log.isDebugEnabled() ) log.debug( "Updating " + existingGene );

        /*
         * This allows stale data to exist in this Session, but flushing prematurely causes constraint violations.
         * Probably we should fix this some other way.
         */
        getSession().setFlushMode( FlushMode.COMMIT );

        return updateGene( existingGene, gene );

    }

    /**
     * @param geneProduct
     * @return
     */
    protected GeneProduct persistOrUpdateGeneProduct( GeneProduct geneProduct ) {
        if ( geneProduct == null ) return null;

        GeneProduct existing = null;
        if ( geneProduct.getId() != null ) {
            existing = geneProductDao.load( geneProduct.getId() );
        } else {
            existing = geneProductDao.find( geneProduct );
        }

        if ( existing == null ) {
            return persistGeneProduct( geneProduct );
        }

        updateGeneProduct( existing, geneProduct );

        return existing;
    }

    /**
     * @param taxon
     */
    protected Taxon persistTaxon( Taxon taxon ) {
        if ( taxon == null ) return null;
        if ( !isTransient( taxon ) ) return taxon;

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

            if ( log.isDebugEnabled() ) log.debug( "Fetched or created taxon " + fTaxon );

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

    /**
     * @param existing
     * @param geneProduct
     */
    private void addAnyNewAccessions( GeneProduct existing, GeneProduct geneProduct ) {
        Map<String, DatabaseEntry> updatedGpMap = new HashMap<String, DatabaseEntry>();
        existing = geneProductDao.thaw( existing );
        for ( DatabaseEntry de : existing.getAccessions() ) {
            updatedGpMap.put( de.getAccession(), de );
        }
        for ( DatabaseEntry de : geneProduct.getAccessions() ) {
            if ( !updatedGpMap.containsKey( de.getAccession() ) ) {
                fillInDatabaseEntry( de );
                existing.getAccessions().add( de );
            }
        }
    }

    /**
     * @param chromosomeLocation
     * @return
     */
    private void fillChromosomeLocationAssociations( ChromosomeLocation chromosomeLocation, Taxon t ) {
        if ( chromosomeLocation == null ) return;
        Chromosome chromosome = persistChromosome( chromosomeLocation.getChromosome(), t );
        chromosomeLocation.setChromosome( chromosome );
    }

    /**
     * @param geneProduct
     */
    private void fillInGeneProductAssociations( GeneProduct geneProduct ) {

        if ( geneProduct.getPhysicalLocation() != null ) {
            geneProduct.getPhysicalLocation().setChromosome(
                    persistChromosome( geneProduct.getPhysicalLocation().getChromosome(), geneProduct.getGene()
                            .getTaxon() ) );
        }

        if ( geneProduct.getExons() != null ) {
            for ( PhysicalLocation exon : geneProduct.getExons() ) {
                exon.setChromosome( persistChromosome( exon.getChromosome(), geneProduct.getGene().getTaxon() ) );
            }
        }

        if ( geneProduct.getAccessions() != null ) {
            for ( DatabaseEntry de : geneProduct.getAccessions() ) {
                de.setExternalDatabase( persistExternalDatabase( de.getExternalDatabase() ) );
            }
        }
    }

    /**
     * @param physicalLocation
     * @param t
     * @return
     */
    private PhysicalLocation fillPhysicalLocationAssociations( PhysicalLocation physicalLocation, Taxon t ) {
        physicalLocation.setChromosome( persistChromosome( physicalLocation.getChromosome(), t ) );

        if ( physicalLocation.getBin() == null && physicalLocation.getNucleotide() != null
                && physicalLocation.getNucleotideLength() != null ) {
            physicalLocation.setBin( SequenceBinUtils.binFromRange( physicalLocation.getNucleotide().intValue(),
                    physicalLocation.getNucleotide().intValue() + physicalLocation.getNucleotideLength().intValue() ) );
        }

        return physicalLocation;
    }

    /**
     * Check for deletions or changed GIs. If we have a GI that is not in the collection, then we might delete it from
     * the system.
     * 
     * @param existingGene
     * @param usedGIs return toRemove
     */
    private Collection<GeneProduct> handleGeneProductChangedGIs( Gene existingGene, Map<String, GeneProduct> usedGIs ) {
        Collection<String> switchedGis = new HashSet<String>();
        Collection<GeneProduct> toRemove = new HashSet<GeneProduct>();
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
             * one is not associated with any gene, we should delete that one and update the outdated record. Third, the
             * other one might be associated with a _different_ gene, in which case we delete _that gp_ and update the
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
                     * delete the duplicate. This is due to cruft, we shouldn't have such duplicates.
                     */
                    if ( switchedGis.contains( ngp.getNcbiGi() ) ) {
                        log.warn( "Another gene product with the same intended GI will be deleted: " + existingGp );
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

    /**
     * @param bioSequence
     */
    private void persistBioSequenceAssociations( BioSequence bioSequence ) {
        fillInBioSequenceTaxon( bioSequence );

        if ( bioSequence.getSequenceDatabaseEntry() != null
                && bioSequence.getSequenceDatabaseEntry().getExternalDatabase().getId() == null ) {
            bioSequence.getSequenceDatabaseEntry().setExternalDatabase(
                    persistExternalDatabase( bioSequence.getSequenceDatabaseEntry().getExternalDatabase() ) );
        }

        for ( BioSequence2GeneProduct bioSequence2GeneProduct : bioSequence.getBioSequence2GeneProduct() ) {
            bioSequence2GeneProduct = persistBioSequence2GeneProduct( bioSequence2GeneProduct );
        }
    }

    /**
     * NOTE this method is not a regular 'persist' method: It does not use findOrCreate! A new result is made every
     * time.
     * 
     * @param blastResult
     * @return
     */
    private BlastResult persistBlastResult( BlastResult blastResult ) {
        if ( !isTransient( blastResult ) ) return blastResult;
        blastResult.setQuerySequence( persistBioSequence( blastResult.getQuerySequence() ) );
        blastResult.setTargetChromosome( persistChromosome( blastResult.getTargetChromosome(), null ) );
        if ( blastResult.getTargetAlignedRegion() != null )
            blastResult.getTargetAlignedRegion().setChromosome( blastResult.getTargetChromosome() );
        return blastResultDao.create( blastResult );
    }

    /**
     * NOTE this method is not a regular 'persist' method: It does not use findOrCreate! A new result is made every
     * time.
     * 
     * @param blatResult
     * @return
     */
    private BlatResult persistBlatResult( BlatResult blatResult ) {
        if ( !isTransient( blatResult ) ) return blatResult;
        if ( blatResult.getQuerySequence() == null ) {
            throw new IllegalArgumentException( "Blat result with null query sequence" );
        }
        blatResult.setQuerySequence( persistBioSequence( blatResult.getQuerySequence() ) );
        blatResult.setTargetChromosome( persistChromosome( blatResult.getTargetChromosome(), null ) );
        blatResult.setSearchedDatabase( persistExternalDatabase( blatResult.getSearchedDatabase() ) );
        if ( blatResult.getTargetAlignedRegion() != null )
            blatResult.setTargetAlignedRegion( fillPhysicalLocationAssociations( blatResult.getTargetAlignedRegion(),
                    null ) );
        return blatResultDao.create( blatResult );
    }

    /**
     * @param chromosome
     * @return
     */
    private Chromosome persistChromosome( Chromosome chromosome, Taxon t ) {
        if ( chromosome == null ) return null;
        if ( !isTransient( chromosome ) ) return chromosome;

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

        if ( seenChromosomes.containsKey( key ) ) {
            return seenChromosomes.get( key );
        }

        chromosome.setTaxon( persistTaxon( ct ) );

        chromosome = chromosomeDao.findOrCreate( chromosome.getName(), ct );

        seenChromosomes.put( key, chromosome );
        if ( chromosome == null || chromosome.getId() == null )
            throw new IllegalStateException( "Failed to get a persistent chromosome instance" );
        return chromosome;

    }

    /**
     * @param bioSequence
     * @return
     */
    private BioSequence persistNewBioSequence( BioSequence bioSequence ) {
        if ( log.isDebugEnabled() ) log.debug( "Creating new: " + bioSequence );

        persistBioSequenceAssociations( bioSequence );

        assert bioSequence.getTaxon().getId() != null;
        return bioSequenceDao.create( bioSequence );
    }

    /**
     * @param result
     * @return
     */
    private SequenceSimilaritySearchResult persistSequenceSimilaritySearchResult( SequenceSimilaritySearchResult result ) {
        if ( result instanceof BlatResult ) {
            return persistBlatResult( ( BlatResult ) result );
        } else if ( result instanceof BlastResult ) {
            return persistBlastResult( ( BlastResult ) result );
        } else {
            throw new UnsupportedOperationException( "Don't know how to persist a " + result.getClass().getName() );
        }
    }

    /**
     * @param existingGeneProduct
     * @param updatedGeneProductInfo information from this is copied onto the 'existing' gene product.
     * @param object
     */
    private void updateGeneProduct( GeneProduct existingGeneProduct, GeneProduct updatedGeneProductInfo ) {
        Gene geneForExistingGeneProduct = existingGeneProduct.getGene();
        assert !isTransient( geneForExistingGeneProduct );

        existingGeneProduct = geneProductDao.thaw( existingGeneProduct );

        // Update all the fields. Note that usually, some of these can't have changed or we wouldn't have even
        // found the 'existing' one (name GI in particular); however, sometimes we are updating this information

        existingGeneProduct.setName( updatedGeneProductInfo.getName() );
        existingGeneProduct.setDescription( updatedGeneProductInfo.getDescription() );
        existingGeneProduct.setNcbiGi( updatedGeneProductInfo.getNcbiGi() );

        addAnyNewAccessions( existingGeneProduct, updatedGeneProductInfo );

        existingGeneProduct.setPhysicalLocation( updatedGeneProductInfo.getPhysicalLocation() );
        if ( existingGeneProduct.getPhysicalLocation() != null ) {
            existingGeneProduct.getPhysicalLocation().setChromosome(
                    persistChromosome( existingGeneProduct.getPhysicalLocation().getChromosome(),
                            geneForExistingGeneProduct.getTaxon() ) );
        }

        existingGeneProduct.setExons( updatedGeneProductInfo.getExons() );
        if ( existingGeneProduct.getExons() != null ) {
            for ( PhysicalLocation exon : existingGeneProduct.getExons() ) {
                exon.setChromosome( persistChromosome( exon.getChromosome(), geneForExistingGeneProduct.getTaxon() ) );
            }
        }

    }
}
