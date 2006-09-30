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
package ubic.gemma.loader.genome.gene.ncbi;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.genome.gene.ncbi.model.NCBIGene2Accession;
import ubic.gemma.loader.genome.gene.ncbi.model.NCBIGeneInfo;
import ubic.gemma.loader.util.converter.Converter;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneAlias;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductType;

/**
 * Convert NCBIGene2Accession objects into Gemma Gene objects.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class NcbiGeneConverter implements Converter {

    private static Log log = LogFactory.getLog( NcbiGeneConverter.class.getName() );
    private AtomicBoolean producerDone = new AtomicBoolean( false );
    private AtomicBoolean sourceDone = new AtomicBoolean( false );
    private ExternalDatabase genBank;

    public NcbiGeneConverter() {
        genBank = ExternalDatabase.Factory.newInstance();
        genBank.setName( "Genbank" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.Converter#convert(java.util.Collection)
     */
    public Collection<Object> convert( Collection sourceDomainObjects ) {
        Collection<Object> results = new HashSet<Object>();
        for ( Object object : sourceDomainObjects ) {
            results.add( this.convert( object ) );
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.Converter#convert(java.lang.Object)
     */
    public Gene convert( NCBIGeneInfo info ) {
        Gene gene = Gene.Factory.newInstance();

        gene.setNcbiId( info.getGeneId() );
        gene.setOfficialSymbol( info.getDefaultSymbol() );
        gene.setOfficialName( info.getDefaultSymbol() );
        gene.setDescription( info.getDescription() );

        Taxon t = Taxon.Factory.newInstance();
        t.setNcbiId( new Integer( info.getTaxId() ) );
        gene.setTaxon( t );

        Collection<GeneAlias> aliases = gene.getAliases();
        for ( String alias : info.getSynonyms() ) {
            GeneAlias newAlias = GeneAlias.Factory.newInstance();
            newAlias.setGene( gene );
            newAlias.setSymbol( gene.getOfficialSymbol() );
            newAlias.setAlias( alias );
            aliases.add( newAlias );
        }
        return gene;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.Converter#convert(java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public Object convert( Object sourceDomainObject ) {
        if ( sourceDomainObject instanceof Collection ) {
            return this.convert( ( Collection ) sourceDomainObject );
        }
        assert sourceDomainObject instanceof NCBIGene2Accession;
        NCBIGene2Accession ncbiGene = ( NCBIGene2Accession ) sourceDomainObject;
        return convert( ncbiGene.getInfo() );
    }

    public Collection<GeneProduct> convert( NCBIGene2Accession acc, Gene gene ) {
        Collection<GeneProduct> geneProducts = new HashSet<GeneProduct>();
        // initialize up to two Gene Products
        // one for RNA, one for Protein

        // RNA section
        if ( acc.getRnaNucleotideAccession() != null ) {
            GeneProduct geneProduct = GeneProduct.Factory.newInstance();

            // set available fields
            geneProduct.setNcbiId( acc.getRnaNucleotideGI() );
            geneProduct.setGene( gene );
            geneProduct.setName( acc.getRnaNucleotideAccession() );
            geneProduct.setType( GeneProductType.RNA );

            DatabaseEntry accession = DatabaseEntry.Factory.newInstance();
            accession.setAccession( acc.getRnaNucleotideAccession() );
            accession.setAccessionVersion( acc.getRnaNucleotideAccessionVersion() );
            accession.setExternalDatabase( genBank );

            Collection<DatabaseEntry> accessions = new HashSet<DatabaseEntry>();
            accessions.add( accession );
            geneProduct.setAccessions( accessions );
            geneProducts.add( geneProduct );
        }

        // Protein section
        if ( acc.getProteinAccession() != null ) {
            GeneProduct geneProduct = GeneProduct.Factory.newInstance();

            // set available fields
            geneProduct.setNcbiId( acc.getProteinGI() );
            geneProduct.setGene( gene );
            geneProduct.setName( acc.getProteinAccession() );
            geneProduct.setType( GeneProductType.PROTEIN );

            DatabaseEntry accession = DatabaseEntry.Factory.newInstance();
            accession.setAccession( acc.getProteinAccession() );
            accession.setAccessionVersion( acc.getProteinAccessionVersion() );
            accession.setExternalDatabase( genBank );

            Collection<DatabaseEntry> accessions = new HashSet<DatabaseEntry>();
            accessions.add( accession );
            geneProduct.setAccessions( accessions );
            geneProducts.add( geneProduct );
        }
        return geneProducts;
    }

    public Gene convert( NcbiGeneData data ) {
        // get gene info and fill in gene
        NCBIGeneInfo geneInfo = data.getGeneInfo();
        assert geneInfo != null;
        Gene gene = convert( geneInfo );

        // grab all accessions and fill in GeneProduct/DatabaseEntry
        // and associate with Gene
        Collection<NCBIGene2Accession> gene2accession = data.getAccessions();
        Collection<GeneProduct> geneProducts = new HashSet<GeneProduct>();

        for ( NCBIGene2Accession acc : gene2accession ) {
            geneProducts.addAll( convert( acc, gene ) );
        }
        gene.setProducts( geneProducts );

        return gene;
    }

    /*
     * Threaded conversion of domain objects to
     */
    public void convert( final BlockingQueue<NcbiGeneData> geneInfoQueue, final BlockingQueue<Gene> geneQueue ) {
        // start up thread to convert a member of geneInfoQueue to a gene/geneproduct/databaseentry
        // then push the gene onto the geneQueue for loading
        Thread convertThread = new Thread( new Runnable() {
            public void run() {
                while ( !( sourceDone.get() && geneInfoQueue.isEmpty() ) ) {
                    try {
                        NcbiGeneData data = geneInfoQueue.poll();
                        if ( data == null || data.getGeneInfo() == null ) {
                            continue;
                        }
                        assert data.getGeneInfo() != null;
                        geneQueue.put( convert( data ) );

                    } catch ( InterruptedException e ) {
                        log.info( "Interrupted." );
                    }
                }
                producerDone.set( true );
            }
        } );

        convertThread.start();
    }

    public boolean isProducerDone() {
        return this.producerDone.get();
    }

    public void setProducerDoneFlag( AtomicBoolean flag ) {
        this.producerDone = flag;
    }

    public void setSourceDoneFlag( AtomicBoolean flag ) {
        this.sourceDone = flag;
    }

}
