/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.loader.protein;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.protein.biomart.model.Ensembl2NcbiValueObject;
import ubic.gemma.loader.protein.string.model.StringProteinProteinInteraction;
import ubic.gemma.loader.util.converter.Converter;
import ubic.gemma.model.association.Gene2GeneProteinAssociation;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.Settings;

/**
 * Class that is responsible for converting value objects generated from the parsing of STRING files
 * (StringProteinProteinInteraction) into Gemma Gene2GeneProteinAssociations. To do that it refers to a map ensembl2ncbi
 * ids
 * 
 * @author ldonnison
 * @version $Id$
 */
public class StringProteinProteinInteractionConverter implements Converter<Object, Object> {

    private static Log log = LogFactory.getLog( StringProteinProteinInteractionConverter.class );

    /** The joining string between two protein ids to create the url link in string for the interaction */
    private static final String PROTEIN2PROTEINLINK = "%0D";

    /** String url **/
    private static String stringUrl;

    /** Version of string being used */
    private static String stringVersion;

    AtomicBoolean producerDone = new AtomicBoolean( false );

    /** The key is the ensembl protein id. */
    private Map<String, Ensembl2NcbiValueObject> ensembl2ncbi = null;

    /** Reference to external database as held in gemma system */
    private ExternalDatabase stringExternalDatabase;

    /**
     * @param ensembl2ncbi Map of ensembl peptide ids to entrez/ncbi id genes.
     */
    public StringProteinProteinInteractionConverter( Map<String, Ensembl2NcbiValueObject> ensembl2ncbi ) {
        this.ensembl2ncbi = ensembl2ncbi;

        stringVersion = Settings.getString( "protein.string.version" );
        stringUrl = Settings.getString( "protein.string.linksurl" );
        if ( stringUrl == null || stringUrl.length() == 0 )
            throw new RuntimeException( new ConfigurationException( "stringUrl was null or empty" ) );
        if ( stringVersion == null || stringVersion.length() == 0 )
            throw new RuntimeException( new ConfigurationException( "stringVersion was null or empty" ) );
    }

    /**
     * Threaded conversion of domain objects to Gemma objects.
     */
    public void convert( final BlockingQueue<Gene2GeneProteinAssociation> gene2GeneProteinAssociationQueue,
            final Collection<StringProteinProteinInteraction> stringProteinProteinInteractions ) {
        // start up thread to convert a member of geneInfoQueue to a gene/geneproduct/databaseentry
        // then push the gene onto the geneQueue for loading
        Thread convertThread = new Thread( new Runnable() {
            @Override
            public void run() {

                try {
                    for ( StringProteinProteinInteraction stringProteinProteinInteraction : stringProteinProteinInteractions ) {
                        if ( stringProteinProteinInteraction == null ) {
                            continue;
                        }
                        // converter
                        Collection<Gene2GeneProteinAssociation> dataColl = convert( stringProteinProteinInteraction );
                        // this returns a collection so split out and put on queue
                        for ( Gene2GeneProteinAssociation gene2GeneProteinAssociation : dataColl ) {
                            gene2GeneProteinAssociationQueue.put( gene2GeneProteinAssociation );
                        }
                    }
                } catch ( InterruptedException e ) {
                    log.info( "Interrupted." );
                }
                producerDone.set( true );
            }

        }, "Converter" );

        convertThread.start();
    }

    @Override
    public Collection<Object> convert( Collection<? extends Object> sourceDomainObjects ) {
        long startTime = System.currentTimeMillis();
        Collection<Object> results = new HashSet<>();
        for ( Object object : sourceDomainObjects ) {
            results.add( this.convert( object ) );
        }
        long EndTime = System.currentTimeMillis();
        long time = ( EndTime - startTime ) / 1000;
        log.info( "Time taken for conversion call is  " + time );
        return results;
    }

    /**
     * Standard converter code
     * 
     * @see ubic.gemma.loader.loaderutils.Converter#convert(java.lang.Object)
     */
    @Override
    public Object convert( Object sourceDomainObject ) {

        Object processedObject = null;
        if ( sourceDomainObject instanceof Collection ) {
            processedObject = this.convert( ( Collection<?> ) sourceDomainObject );
        } else if ( sourceDomainObject instanceof StringProteinProteinInteraction ) {
            StringProteinProteinInteraction stringProteinProteinInteraction = ( StringProteinProteinInteraction ) sourceDomainObject;
            processedObject = convert( stringProteinProteinInteraction );
        } else {
            throw new RuntimeException( "Incorrect domain object passed" );
        }

        return processedObject;

    }

    /**
     * Given a StringProteinProteinInteraction value object create a gemma Gene2GeneProteinAssociation. One
     * StringProteinProteinInteraction can potentially create many Gene2GeneProteinAssociation objects If the call to
     * getNcbiGene returns more than 1 gene then each gene returned is turned into an interaction. Which means that the
     * same ensemble protein protein id interaction could be duplicated as many times as there is gene mappings. This is
     * done for both protein 1 and protein2 so a matrix is formed.
     * 
     * @param sourceDomainObject the domain object to process
     * @return collection of Gene2GeneProteinAssociation representing this interaction
     */
    public Collection<Gene2GeneProteinAssociation> convert( StringProteinProteinInteraction sourceDomainObject ) {

        Collection<Gene2GeneProteinAssociation> gene2GeneProteinAssociations = new ArrayList<Gene2GeneProteinAssociation>();

        // if(sourceDomainObject instanceof StringProteinProteinInteraction){
        StringProteinProteinInteraction stringProteinProteinInteraction = sourceDomainObject;

        // have to create a matrix of interactions take the ensemble id and see how many ncbi ids it maps to
        Collection<Gene> genesForProteinOne = this.getNcbiGene( stringProteinProteinInteraction.getProtein1() );
        Collection<Gene> genesForProteinTwo = this.getNcbiGene( stringProteinProteinInteraction.getProtein2() );

        // empty if no mapping found
        if ( genesForProteinOne.isEmpty() ) {
            log.warn( "No ncbi gene mapping for protein 1: " + stringProteinProteinInteraction.getProtein1() );
        } else if ( genesForProteinTwo.isEmpty() ) {
            log.warn( "No ncbi gene mapping for protein 2: " + stringProteinProteinInteraction.getProtein2() );
        } else {
            // create the one to many mapping from ensembl to ncbi/entrez
            for ( Gene geneProtein1 : genesForProteinOne ) {
                for ( Gene geneProtein2 : genesForProteinTwo ) {
                    Gene2GeneProteinAssociation gene2GeneProteinAssociation = Gene2GeneProteinAssociation.Factory
                            .newInstance( geneProtein1, geneProtein2,
                                    this.getDataBaseEntry( stringProteinProteinInteraction ),
                                    stringProteinProteinInteraction.getEvidenceVector(),
                                    stringProteinProteinInteraction.getCombined_score() );

                    gene2GeneProteinAssociations.add( gene2GeneProteinAssociation );
                }
            }
        }
        return gene2GeneProteinAssociations;
    }

    /**
     * Create a database entry which represents the external record as held in string
     * 
     * @param StringProteinProteinInteraction object which contains the two protein ids
     * @return DatabaseEntry representing the record as held in string
     */
    public DatabaseEntry getDataBaseEntry( StringProteinProteinInteraction stringProteinProteinInteractionId ) {
        String proteinProteinInteraction = this.getProteinProteinInteractionId( stringProteinProteinInteractionId );
        DatabaseEntry databaseEntry = DatabaseEntry.Factory.newInstance( proteinProteinInteraction, stringVersion,
                stringUrl, stringExternalDatabase );
        return databaseEntry;
    }

    /**
     * One ensemblProteinID can map to multiple ncbi genes. This method takes the ensembl gene and creates a collection
     * of entrez ncbi genes. It first has to remove the taxon id from the beginning of the peptide id as given by
     * string.
     * 
     * @param ensemblProteinId The ensembl protein id in this interaction
     * @return Collection of genes as represented in ncbi entrez gene
     */
    public Collection<Gene> getNcbiGene( String ensemblProteinId ) {
        // log.debug("getting ncbi gene for ensembl id " + ensemblProteinId);
        Collection<Gene> genes = new ArrayList<>();

        // in case species id is still on there from STRING like 12334.ENSD....
        String eid = ensemblProteinId.replaceFirst( "[0-9]+\\.", "" );

        Ensembl2NcbiValueObject e2n = ensembl2ncbi.get( eid );
        if ( e2n == null || e2n.getEntrezgenes().isEmpty() ) {
            return genes;
        }

        String ensemblGeneId = e2n.getEnsemblGeneId();

        Collection<String> entreGeneids = ( e2n.getEntrezgenes() );
        for ( String entrezGeneid : entreGeneids ) {
            if ( !entrezGeneid.isEmpty() ) {
                Gene gene = Gene.Factory.newInstance();
                gene.setNcbiGeneId( Integer.parseInt( entrezGeneid ) );
                gene.setEnsemblId( ensemblGeneId );
                genes.add( gene );
                if ( log.isDebugEnabled() ) log.debug( "Entry found for entrezGeneid " + entrezGeneid );
            }
        }

        return genes;
    }

    /**
     * This is a made up value for the accessionId which is the protein peptide id 1 and the protein peptide 2 combined
     * and separated by a percentage This is so that it can be sent as a whole to string to retrieve the record in
     * string
     * 
     * @param protein1 Protein 1 id in the interaction
     * @param protein2 Protein 2 in the interaction
     * @return Combined protein 1 and protein 2 ids representing an identifier for this protein interaction
     */
    public String getProteinProteinInteractionId( StringProteinProteinInteraction stringProteinProteinInteraction ) {
        return stringProteinProteinInteraction.getProtein1().concat( PROTEIN2PROTEINLINK )
                .concat( stringProteinProteinInteraction.getProtein2() );
    }

    /**
     * @return the stringExternalDatabase
     */
    public ExternalDatabase getStringExternalDatabase() {
        return stringExternalDatabase;
    }

    public boolean isProducerDone() {
        return this.producerDone.get();
    }

    /**
     * Set the map of ids
     * 
     * @param bioMartStringEntreGeneMapping
     */
    public void setEnsemblEntrzMap( Map<String, Ensembl2NcbiValueObject> bioMartStringEntreGeneMapping ) {
        this.ensembl2ncbi = bioMartStringEntreGeneMapping;
    }

    public void setProducerDoneFlag( AtomicBoolean flag ) {
        this.producerDone = flag;
    }

    /**
     * @return the stringExternalDatabase
     */
    public void setStringExternalDatabase( ExternalDatabase externalDatabase ) {
        this.stringExternalDatabase = externalDatabase;
    }

}
