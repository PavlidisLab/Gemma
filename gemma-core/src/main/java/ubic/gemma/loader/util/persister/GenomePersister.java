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
package ubic.gemma.loader.util.persister;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.gene.GeneService;

/**
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="bioSequenceService" ref="bioSequenceService"
 * @spring.property name="taxonService" ref="taxonService"
 * @author pavlidis
 * @version $Id$
 */
abstract public class GenomePersister extends CommonPersister {

    protected GeneService geneService;

    protected BioSequenceService bioSequenceService;

    protected TaxonService taxonService;

    protected Map<Object, Taxon> seenTaxa = new HashMap<Object, Taxon>();

    protected boolean firstBioSequence = false;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.persister.Persister#persist(java.lang.Object)
     */
    public Object persist( Object entity ) {
        if ( entity instanceof Gene ) {
            return persistGene( ( Gene ) entity );
        } else if ( entity instanceof BioSequence ) {
            if ( firstBioSequence )
                log.warn( "*** Attempt to directly persist a BioSequence "
                        + "*** BioSequence are only persisted by association to other objects." );
            firstBioSequence = false;
            return null;
            // deal with in cascade from array design? Do nothing, probably.
        } else if ( entity instanceof Taxon ) { // AS
            return persistTaxon( ( Taxon ) entity );
        }
        return super.persist( entity );
    }

    /**
     * @param gene
     */
    @SuppressWarnings("unchecked")
    protected Object persistGene( Gene gene ) {
        if ( gene == null ) return null;
        if ( !isTransient( gene ) ) return gene;

        gene.setAccessions( ( Collection<DatabaseEntry> ) persist( gene.getAccessions() ) );
        return geneService.findOrCreate( gene );
    }

    /**
     * @param bioSequence
     */
    protected BioSequence persistBioSequence( BioSequence bioSequence ) {
        if ( bioSequence == null ) return null;
        if ( !isTransient( bioSequence ) ) return bioSequence;
        fillInBioSequenceTaxon( bioSequence );
        return bioSequenceService.findOrCreate( bioSequence );
    }

    /**
     * @param bioSequence
     */
    protected void fillInBioSequenceTaxon( BioSequence bioSequence ) {
        Taxon t = bioSequence.getTaxon();
        if ( t == null ) throw new IllegalArgumentException( "BioSequence Taxon cannot be null" );
        if ( !isTransient( t ) ) return;

        // Avoid trips to the database to get the taxon.
        String scientificName = t.getScientificName();
        String commonName = t.getCommonName();
        Integer ncbiId = t.getNcbiId();
        if ( scientificName != null && seenTaxa.get( scientificName ) != null ) {
            bioSequence.setTaxon( seenTaxa.get( scientificName ) );
        } else if ( commonName != null && seenTaxa.get( commonName ) != null ) {
            bioSequence.setTaxon( seenTaxa.get( commonName ) );
        } else if ( ncbiId != null && seenTaxa.get( ncbiId ) != null ) {
            bioSequence.setTaxon( seenTaxa.get( ncbiId ) );
        } else {
            assert isTransient( t );
            Taxon taxon = taxonService.findOrCreate( t );
            log.warn( "Fetched taxon " + taxon.getScientificName() );
            bioSequence.setTaxon( taxon );
            if ( taxon.getScientificName() != null ) {
                seenTaxa.put( taxon.getScientificName(), bioSequence.getTaxon() );
            }
            if ( taxon.getCommonName() != null ) {
                seenTaxa.put( taxon.getCommonName(), bioSequence.getTaxon() );
            }
            if ( taxon.getNcbiId() != null ) {
                seenTaxa.put( taxon.getNcbiId(), bioSequence.getTaxon() );
            }
        }
    }

    // AS
    /**
     * @param taxon
     */
    protected Object persistTaxon( Taxon taxon ) {
        return taxonService.findOrCreate( taxon );
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
}
