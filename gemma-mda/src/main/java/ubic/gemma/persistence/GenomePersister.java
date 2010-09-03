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
import java.util.Map;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.ChromosomeLocation;
import ubic.gemma.model.genome.ChromosomeService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.sequenceAnalysis.BlastAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlastAssociationService;
import ubic.gemma.model.genome.sequenceAnalysis.BlastResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlastResultService;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationService;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultService;
import ubic.gemma.model.genome.sequenceAnalysis.SequenceSimilaritySearchResult;
import ubic.gemma.util.SequenceBinUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
abstract public class GenomePersister extends CommonPersister {

    @Autowired
    protected GeneService geneService;

    @Autowired
    protected ChromosomeService chromosomeService;

    @Autowired
    protected GeneProductService geneProductService;

    @Autowired
    protected BioSequenceService bioSequenceService;

    @Autowired
    protected TaxonService taxonService;

    @Autowired
    protected BlatAssociationService blatAssociationService;

    @Autowired
    protected BlastAssociationService blastAssociationService;

    @Autowired
    protected BlatResultService blatResultService;

    @Autowired
    protected BlastResultService blastResultService;

    protected Map<Object, Taxon> seenTaxa = new HashMap<Object, Taxon>();

    protected Map<Object, Chromosome> seenChromosomes = new HashMap<Object, Chromosome>();

    protected boolean firstBioSequence = false;

    public GenomePersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

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
            return persistChromosome( ( Chromosome ) entity );
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
     * @param bioSequenceService The bioSequenceService to set.
     */
    public void setBioSequenceService( BioSequenceService bioSequenceService ) {
        this.bioSequenceService = bioSequenceService;
    }

    /**
     * @param blastAssociationService The blastAssociationService to set.
     */
    public void setBlastAssociationService( BlastAssociationService blastAssociationService ) {
        this.blastAssociationService = blastAssociationService;
    }

    /**
     * @param blastResultService The blastResultService to set.
     */
    public void setBlastResultService( BlastResultService blastResultService ) {
        this.blastResultService = blastResultService;
    }

    /**
     * @param blatAssociationService The blatAssociationService to set.
     */
    public void setBlatAssociationService( BlatAssociationService blatAssociationService ) {
        this.blatAssociationService = blatAssociationService;
    }

    /**
     * @param blatResultService The blatResultService to set.
     */
    public void setBlatResultService( BlatResultService blatResultService ) {
        this.blatResultService = blatResultService;
    }

    /**
     * @param chromosomeService the chromosomeService to set
     */
    public void setChromosomeService( ChromosomeService chromosomeService ) {
        this.chromosomeService = chromosomeService;
    }

    /**
     * @param geneProductService the geneProductService to set
     */
    public void setGeneProductService( GeneProductService geneProductService ) {
        this.geneProductService = geneProductService;
    }

    /**
     * @param geneService The geneService to set.
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * @param taxonService The taxonService to set.
     */
    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
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

        BioSequence existingBioSequence = bioSequenceService.find( bioSequence );

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
        } else if ( bioSequence2GeneProduct instanceof BlastAssociation ) {
            return persistBlastAssociation( ( BlastAssociation ) bioSequence2GeneProduct );
        } else {
            throw new UnsupportedOperationException( "Don't know how to deal with "
                    + bioSequence2GeneProduct.getClass().getName() );
        }

    }

    /**
     * @param association
     */
    protected BioSequence2GeneProduct persistBlastAssociation( BlastAssociation association ) {
        BlastResult blastResult = association.getBlastResult();
        persistBlastResult( blastResult );
        return blastAssociationService.create( association );
    }

    /**
     * @param association
     */
    protected BioSequence2GeneProduct persistBlatAssociation( BlatAssociation association ) {
        BlatResult blatResult = association.getBlatResult();
        if ( isTransient( blatResult ) ) {
            blatResult = blatResultService.create( blatResult );
        }
        if ( log.isDebugEnabled() ) {
            log.debug( "Persisting " + association );
        }
        association.setGeneProduct( persistGeneProduct( association.getGeneProduct() ) );
        association.setBioSequence( persistBioSequence( association.getBioSequence() ) );
        return blatAssociationService.create( association );
    }

    /**
     * @param gene
     */
    protected Gene persistGene( Gene gene ) {
        if ( gene == null ) return null;
        if ( !isTransient( gene ) ) return gene;

        Gene existingGene = geneService.find( gene );

        if ( existingGene != null ) {
            if ( log.isDebugEnabled() ) log.debug( "Gene exists, will not update" );
            return existingGene;
        }

        log.debug( "New gene: " + gene );

        if ( gene.getAccessions().size() > 0 ) {
            for ( DatabaseEntry de : gene.getAccessions() ) {
                fillInDatabaseEntry( de );
            }
        }

        Collection<GeneProduct> tempGeneProduct = gene.getProducts();
        gene.setProducts( null );

        gene.setTaxon( persistTaxon( gene.getTaxon() ) );

        fillChromosomeLocationAssociations( gene.getPhysicalLocation() );
        fillChromosomeLocationAssociations( gene.getCytogenicLocation() );
        fillChromosomeLocationAssociations( gene.getGeneticLocation() );

        Gene newGene = geneService.create( gene );

        for ( GeneProduct product : tempGeneProduct ) {
            product.setGene( newGene );
        }

        gene.setProducts( tempGeneProduct );
        for ( GeneProduct gp : gene.getProducts() ) {
            fillInGeneProductAssociations( gp );
        }

        // attach the products.
        geneService.update( gene );

        return newGene;
    }

    /**
     * @param geneProduct
     * @return
     */
    protected GeneProduct persistGeneProduct( GeneProduct geneProduct ) {
        if ( geneProduct == null ) return null;
        if ( !isTransient( geneProduct ) ) return geneProduct;

        GeneProduct existing = geneProductService.find( geneProduct );

        if ( existing != null ) {
            if ( log.isDebugEnabled() ) log.debug( geneProduct + " exists, will not update" );
            return existing;
        }

        if ( log.isDebugEnabled() ) log.debug( "*** New: " + geneProduct + " *** " );

        fillInGeneProductAssociations( geneProduct );

        if ( isTransient( geneProduct.getGene() ) ) {
            // this results in the persistenct of the geneproducts, but only if the gene is transient.
            geneProduct.setGene( persistGene( geneProduct.getGene() ) );
        } else {
            geneProduct = geneProductService.create( geneProduct );
        }

        if ( geneProduct.getId() == null ) {
            return geneProductService.create( geneProduct );
        }

        return geneProduct;
        // ;
    }

    /**
     * @param bioSequence
     * @return
     */
    protected BioSequence persistOrUpdateBioSequence( BioSequence bioSequence ) {
        if ( bioSequence == null ) return null;

        BioSequence existingBioSequence = bioSequenceService.find( bioSequence );

        if ( existingBioSequence == null ) {
            if ( log.isDebugEnabled() ) log.debug( "Creating new: " + bioSequence );
            return persistNewBioSequence( bioSequence );
        }

        if ( log.isDebugEnabled() ) log.debug( "Found existing: " + existingBioSequence );
        bioSequence.setId( existingBioSequence.getId() );
        persistBioSequenceAssociations( bioSequence );
        if ( this.getSession( true ).contains( existingBioSequence ) ) {
            this.getHibernateTemplate().evict( existingBioSequence );
        }
        bioSequenceService.update( bioSequence );

        return bioSequence;

    }

    /**
     * @param gene transient instance that will be used to provide information to update persistent version.
     * @return new or updated gene instance.
     */
    protected Gene persistOrUpdateGene( Gene gene ) {

        if ( gene == null ) return null;

        Gene existingGene = null;
        if ( gene.getId() != null ) {
            existingGene = geneService.load( gene.getId() );
        } else {
            existingGene = geneService.find( gene );
        }

        if ( existingGene == null ) {
            return persistGene( gene );
        }

        log.debug( "Updating " + existingGene );

        // updated gene products.
        existingGene = geneService.thaw( existingGene );
        // NCBI id can be null if gene has been loaded from a gene info file
        String existingNcbiId = existingGene.getNcbiId();
        if ( existingNcbiId != null && !existingNcbiId.equals( gene.getNcbiId() ) ) {
            log.info( "NCBI ID Change for " + existingGene + ", new id =" + gene.getNcbiId() );
            String previousId = gene.getPreviousNcbiId();
            if ( previousId != null ) {
                if ( !previousId.equals( existingGene.getNcbiId() ) ) {
                    throw new IllegalStateException( "The NCBI ID for " + gene
                            + " has changed and the previous NCBI id on record with NCBI (" + gene.getPreviousNcbiId()
                            + ") doesn't match either." );
                }
                existingGene.setPreviousNcbiId( existingGene.getNcbiId() );
                existingGene.setNcbiId( gene.getNcbiId() );

            }
        }

        // We assume the taxon hasn't changed.

        Map<String, DatabaseEntry> updatedacMap = new HashMap<String, DatabaseEntry>();
        for ( DatabaseEntry de : existingGene.getAccessions() ) {
            updatedacMap.put( de.getAccession(), de );
        }
        for ( DatabaseEntry de : gene.getAccessions() ) {
            if ( !updatedacMap.containsKey( de.getAccession() ) ) {
                fillInDatabaseEntry( de );
                existingGene.getAccessions().add( de );
            }
        }

        existingGene.setName( gene.getName() );
        existingGene.setDescription( gene.getDescription() );
        existingGene.setOfficialName( gene.getOfficialName() );
        existingGene.setOfficialSymbol( gene.getOfficialSymbol() );

        existingGene.setPhysicalLocation( gene.getPhysicalLocation() );
        existingGene.setCytogenicLocation( gene.getCytogenicLocation() );
        existingGene.setGeneticLocation( gene.getGeneticLocation() );

        fillChromosomeLocationAssociations( existingGene.getPhysicalLocation() );
        fillChromosomeLocationAssociations( existingGene.getCytogenicLocation() );
        fillChromosomeLocationAssociations( existingGene.getGeneticLocation() );

        existingGene.getAliases().clear();
        existingGene.getAliases().addAll( gene.getAliases() );

        /*
         * This is the only tricky part - the gene products. We update them if they are already there, and add them if
         * not. We do not delete 'old' ones that the new gene instance does not have, because they might be from
         * differenct sources. For example, Ensembl.
         */
        Map<String, GeneProduct> updatedGpMap = new HashMap<String, GeneProduct>();
        for ( GeneProduct gp : existingGene.getProducts() ) {
            updatedGpMap.put( gp.getName(), gp );
        }

        for ( GeneProduct possiblyNewProduct : gene.getProducts() ) {
            if ( updatedGpMap.containsKey( possiblyNewProduct.getName() ) ) {
                log.debug( "Updating gene product based on name: " + possiblyNewProduct );
                updateGeneProduct( updatedGpMap.get( possiblyNewProduct.getName() ), possiblyNewProduct );
            } else {
                // it is, in fact, new.
                possiblyNewProduct.setGene( existingGene );
                fillInGeneProductAssociations( possiblyNewProduct );
                log.debug( "New product for " + existingGene + ": " + possiblyNewProduct );
                existingGene.getProducts().add( possiblyNewProduct );
            }
        }

        geneService.update( existingGene );

        return existingGene;

    }

    /**
     * @param geneProduct
     * @return
     */
    protected GeneProduct persistOrUpdateGeneProduct( GeneProduct geneProduct ) {
        if ( geneProduct == null ) return null;

        GeneProduct existing = null;
        if ( geneProduct.getId() != null ) {
            existing = geneProductService.load( geneProduct.getId() );
        } else {
            existing = geneProductService.find( geneProduct );
        }

        if ( existing == null ) {
            return persistGeneProduct( geneProduct );
        }

        return updateGeneProduct( existing, geneProduct );
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
            Taxon fTaxon = taxonService.findOrCreate( taxon );
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
        existing = geneProductService.thaw( existing );
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
    private void fillChromosomeLocationAssociations( ChromosomeLocation chromosomeLocation ) {
        if ( chromosomeLocation == null ) return;
        chromosomeLocation.setChromosome( persistChromosome( chromosomeLocation.getChromosome() ) );
    }

    /**
     * @param geneProduct
     */
    private void fillInGeneProductAssociations( GeneProduct geneProduct ) {
        if ( geneProduct.getAccessions() != null ) {
            for ( DatabaseEntry de : geneProduct.getAccessions() ) {
                de.setExternalDatabase( persistExternalDatabase( de.getExternalDatabase() ) );
            }
        }

        if ( geneProduct.getCdsPhysicalLocation() != null ) {
            geneProduct.getCdsPhysicalLocation().setChromosome(
                    persistChromosome( geneProduct.getCdsPhysicalLocation().getChromosome() ) );
        }

        if ( geneProduct.getPhysicalLocation() != null ) {
            geneProduct.getPhysicalLocation().setChromosome(
                    persistChromosome( geneProduct.getPhysicalLocation().getChromosome() ) );

            // sanity check, as we've had this problem...somehow.
            // assert geneProduct.getPhysicalLocation().getChromosome().getTaxon().equals(
            // geneProduct.getGene().getTaxon() );
        }

        if ( geneProduct.getExons() != null ) {
            for ( PhysicalLocation exon : geneProduct.getExons() ) {
                exon.setChromosome( persistChromosome( exon.getChromosome() ) );
            }
        }
    }

    private PhysicalLocation fillPhysicalLocationAssociations( PhysicalLocation physicalLocation ) {
        physicalLocation.setChromosome( persistChromosome( physicalLocation.getChromosome() ) );

        if ( physicalLocation.getBin() == null && physicalLocation.getNucleotide() != null
                && physicalLocation.getNucleotideLength() != null ) {
            physicalLocation.setBin( SequenceBinUtils.binFromRange( physicalLocation.getNucleotide().intValue(),
                    physicalLocation.getNucleotide().intValue() + physicalLocation.getNucleotideLength().intValue() ) );
        }

        return physicalLocation;
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
        blastResult.setTargetChromosome( persistChromosome( blastResult.getTargetChromosome() ) );
        if ( blastResult.getTargetAlignedRegion() != null )
            blastResult.getTargetAlignedRegion().setChromosome( blastResult.getTargetChromosome() );
        return blastResultService.create( blastResult );
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
        blatResult.setTargetChromosome( persistChromosome( blatResult.getTargetChromosome() ) );
        blatResult.setSearchedDatabase( persistExternalDatabase( blatResult.getSearchedDatabase() ) );
        if ( blatResult.getTargetAlignedRegion() != null )
            blatResult.setTargetAlignedRegion( fillPhysicalLocationAssociations( blatResult.getTargetAlignedRegion() ) );
        return blatResultService.create( blatResult );
    }

    /**
     * @param chromosome
     * @return
     */
    private Chromosome persistChromosome( Chromosome chromosome ) {
        if ( chromosome == null ) return null;
        if ( !isTransient( chromosome ) ) return chromosome;

        // note that we can't use the native hashcode method because we need to ignore the ID.
        int key = chromosome.getName().hashCode();
        if ( chromosome.getTaxon().getNcbiId() != null )
            key += chromosome.getTaxon().getNcbiId().hashCode();
        else if ( chromosome.getTaxon().getCommonName() != null )
            key += chromosome.getTaxon().getCommonName().hashCode();
        else if ( chromosome.getTaxon().getScientificName() != null )
            key += chromosome.getTaxon().getScientificName().hashCode();

        if ( seenChromosomes.containsKey( key ) ) {
            return seenChromosomes.get( key );
        }

        // chromosome.setSequence( persistBioSequence( chromosome.getSequence() ) );
        chromosome.setTaxon( persistTaxon( chromosome.getTaxon() ) );

        chromosome = chromosomeService.findOrCreate( chromosome.getName(), chromosome.getTaxon() );

        seenChromosomes.put( key, chromosome );

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
        return bioSequenceService.create( bioSequence );
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
     * @param existing
     * @param geneProduct information from this is copied onto the 'existing' gene product.
     * @return
     */
    private GeneProduct updateGeneProduct( GeneProduct existing, GeneProduct geneProduct ) {
        assert !isTransient( existing.getGene() );

        existing = geneProductService.thaw( existing );

        existing.setName( geneProduct.getName() );
        existing.setDescription( geneProduct.getDescription() );
        existing.setNcbiId( geneProduct.getNcbiId() );

        addAnyNewAccessions( existing, geneProduct );

        existing.setCdsPhysicalLocation( geneProduct.getCdsPhysicalLocation() );
        if ( existing.getCdsPhysicalLocation() != null ) {
            existing.getCdsPhysicalLocation().setChromosome(
                    persistChromosome( existing.getCdsPhysicalLocation().getChromosome() ) );
        }

        existing.setPhysicalLocation( geneProduct.getPhysicalLocation() );
        if ( existing.getPhysicalLocation() != null ) {

            existing.getPhysicalLocation().setChromosome(
                    persistChromosome( existing.getPhysicalLocation().getChromosome() ) );

            /*
             * Rethaw.
             */
            existing = geneProductService.thaw( existing );

            // sanity check, as we've had this problem...somehow.
            if ( !existing.getPhysicalLocation().getChromosome().getTaxon().equals( existing.getGene().getTaxon() ) ) {
                throw new IllegalStateException( "Taxa don't match for gene product location and gene" );
            }
        }

        existing.setExons( geneProduct.getExons() );
        if ( existing.getExons() != null ) {

            for ( PhysicalLocation exon : existing.getExons() ) {
                exon.setChromosome( persistChromosome( exon.getChromosome() ) );
            }
        }

        // geneProductService.update( existing );

        return existing;
    }
}
