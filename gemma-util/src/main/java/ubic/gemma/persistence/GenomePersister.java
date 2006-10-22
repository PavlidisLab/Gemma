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

import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.ChromosomeService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.gene.GeneAlias;
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

/**
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="bioSequenceService" ref="bioSequenceService"
 * @spring.property name="taxonService" ref="taxonService"
 * @spring.property name="blatAssociationService" ref="blatAssociationService"
 * @spring.property name="blastAssociationService" ref="blastAssociationService"
 * @spring.property name="blatResultService" ref="blatResultService"
 * @spring.property name="blastResultService" ref="blastResultService"
 * @spring.property name="geneProductService" ref="geneProductService"
 * @spring.property name="chromosomeService" ref="chromosomeService"
 * @author pavlidis
 * @version $Id$
 */
abstract public class GenomePersister extends CommonPersister {

    protected GeneService geneService;

    protected ChromosomeService chromosomeService;

    protected GeneProductService geneProductService;

    protected BioSequenceService bioSequenceService;

    protected TaxonService taxonService;

    protected BlatAssociationService blatAssociationService;

    protected BlastAssociationService blastAssociationService;

    protected BlatResultService blatResultService;

    protected BlastResultService blastResultService;

    protected Map<Object, Taxon> seenTaxa = new HashMap<Object, Taxon>();

    protected Map<Object, Chromosome> seenChromosomes = new HashMap<Object, Chromosome>();

    protected boolean firstBioSequence = false;

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
        }

        return super.persistOrUpdate( entity );
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

    /**
     * @param gene
     */
    @SuppressWarnings("unchecked")
    protected Gene persistGene( Gene gene ) {
        if ( gene == null ) return null;
        if ( !isTransient( gene ) ) return gene;

        if ( gene.getAccessions().size() > 0 ) {
            gene.setAccessions( ( Collection<DatabaseEntry> ) persist( gene.getAccessions() ) );
        }

        Collection<GeneProduct> tempGeneProduct = gene.getProducts();
        gene.setProducts( null );

        gene.setTaxon( persistTaxon( gene.getTaxon() ) );

        gene = geneService.findOrCreate( gene );

        for ( GeneProduct product : tempGeneProduct ) {
            product.setGene( gene );
            for ( DatabaseEntry databaseEntry : product.getAccessions() ) {
                databaseEntry.setExternalDatabase( persistExternalDatabase( databaseEntry.getExternalDatabase() ) );
            }
        }
        gene.setProducts( tempGeneProduct );

        for ( GeneAlias alias : gene.getAliases() ) {
            alias.setGene( gene );
        }
        geneService.update( gene );
        return gene;
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
        bioSequenceService.update( bioSequence );

        return bioSequence;

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
     * @param bioSequence2GeneProduct
     * @return
     */
    protected BioSequence2GeneProduct persistBioSequence2GeneProduct( BioSequence2GeneProduct bioSequence2GeneProduct ) {
        if ( bioSequence2GeneProduct == null ) return null;
        if ( !isTransient( bioSequence2GeneProduct ) ) return bioSequence2GeneProduct;

        bioSequence2GeneProduct.setGeneProduct( persistGeneProduct( bioSequence2GeneProduct.getGeneProduct() ) );

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
     * @param blastResult
     */
    private BlastResult persistBlastResult( BlastResult blastResult ) {
        blastResult.setQuerySequence( persistBioSequence( blastResult.getQuerySequence() ) );
        blastResult.setTargetChromosome( persistChromosome( blastResult.getTargetChromosome() ) );
        return blastResultService.create( blastResult );
    }

    /**
     * @param blatResult
     */
    private BlatResult persistBlatResult( BlatResult blatResult ) {
        blatResult.setQuerySequence( persistBioSequence( blatResult.getQuerySequence() ) );
        blatResult.setTargetChromosome( persistChromosome( blatResult.getTargetChromosome() ) );
        return blatResultService.create( blatResult );
    }

    /**
     * @param chromosome
     * @return
     */
    private Chromosome persistChromosome( Chromosome chromosome ) {
        if ( chromosome == null ) return null;
        if ( !isTransient( chromosome ) ) return chromosome;

        String key = chromosome.getTaxon().getCommonName() + " " + chromosome.getName();

        if ( seenChromosomes.containsKey( key ) ) {
            return seenChromosomes.get( key );
        }

        chromosome.setSequence( persistBioSequence( chromosome.getSequence() ) );
        chromosome.setTaxon( persistTaxon( chromosome.getTaxon() ) );
        chromosome = chromosomeService.findOrCreate( chromosome );

        seenChromosomes.put( key, chromosome );

        return chromosome;

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
            throw new UnsupportedOperationException( "Don't know how to deal with " + result.getClass().getName() );
        }
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
     * @param geneProduct
     * @return
     */
    protected GeneProduct persistGeneProduct( GeneProduct geneProduct ) {
        if ( geneProduct == null ) return null;
        if ( !isTransient( geneProduct ) ) return geneProduct;

        if ( log.isDebugEnabled() ) {
            log.debug( "Persisting " + geneProduct );
        }

        if ( geneProduct.getCdsPhysicalLocation() != null ) {
            geneProduct.getCdsPhysicalLocation().setChromosome(
                    persistChromosome( geneProduct.getCdsPhysicalLocation().getChromosome() ) );
        }

        if ( geneProduct.getPhysicalLocation() != null ) {
            geneProduct.getPhysicalLocation().setChromosome(
                    persistChromosome( geneProduct.getPhysicalLocation().getChromosome() ) );
        }

        if ( geneProduct.getExons() != null ) {
            for ( PhysicalLocation exon : geneProduct.getExons() ) {
                exon.setChromosome( persistChromosome( exon.getChromosome() ) );
            }
        }

        // careful, circular trap?
        if ( isTransient( geneProduct.getGene() ) ) {
            geneProduct.setGene( persistGene( geneProduct.getGene() ) );
        }

        // going via gene will persist it.
        if ( isTransient( geneProduct ) ) {
            geneProduct = geneProductService.findOrCreate( geneProduct );
        }

        return geneProduct;
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
     * @param bioSequenceService The bioSequenceService to set.
     */
    public void setBioSequenceService( BioSequenceService bioSequenceService ) {
        this.bioSequenceService = bioSequenceService;
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
     * @param blatAssociationService The blatAssociationService to set.
     */
    public void setBlatAssociationService( BlatAssociationService blatAssociationService ) {
        this.blatAssociationService = blatAssociationService;
    }

    /**
     * @param blastAssociationService The blastAssociationService to set.
     */
    public void setBlastAssociationService( BlastAssociationService blastAssociationService ) {
        this.blastAssociationService = blastAssociationService;
    }

    /**
     * @param blatResultService The blatResultService to set.
     */
    public void setBlatResultService( BlatResultService blatResultService ) {
        this.blatResultService = blatResultService;
    }

    /**
     * @param blastResultService The blastResultService to set.
     */
    public void setBlastResultService( BlastResultService blastResultService ) {
        this.blastResultService = blastResultService;
    }

    /**
     * @param geneProductService the geneProductService to set
     */
    public void setGeneProductService( GeneProductService geneProductService ) {
        this.geneProductService = geneProductService;
    }

    /**
     * @param chromosomeService the chromosomeService to set
     */
    public void setChromosomeService( ChromosomeService chromosomeService ) {
        this.chromosomeService = chromosomeService;
    }
}
