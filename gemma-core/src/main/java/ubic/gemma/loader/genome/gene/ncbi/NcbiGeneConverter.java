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
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.CytogeneticLocation;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.model.genome.gene.GeneAlias;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductType;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.SequenceBinUtils;

/**
 * Convert NCBIGene2Accession objects into Gemma Gene objects with associated GeneProducts.
 * 
 * @author pavlidis
 * @author jrsantos
 * @version $Id$
 * @see NCBIGene2Accession, NCBIGeneInfo
 */
public class NcbiGeneConverter implements Converter<Object, Object> {

    // configured in project.properties, override in Gemma.properties
    private static final String RETAIN_PROTEIN_INFO_PARAM = "gemma.store.ncbi.proteininfo";

    private static Log log = LogFactory.getLog( NcbiGeneConverter.class.getName() );
    AtomicBoolean producerDone = new AtomicBoolean( false );
    AtomicBoolean sourceDone = new AtomicBoolean( false );
    private static ExternalDatabase genBank;
    private static ExternalDatabase ensembl;

    private static boolean retainProteinInformation = ConfigUtils.getBoolean( RETAIN_PROTEIN_INFO_PARAM, false );

    static {
        genBank = ExternalDatabase.Factory.newInstance();
        genBank.setName( "Genbank" );
        ensembl = ExternalDatabase.Factory.newInstance();
        ensembl.setName( "Ensembl" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.loaderutils.Converter#convert(java.util.Collection)
     */
    public Collection<Object> convert( Collection<? extends Object> sourceDomainObjects ) {
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

        gene.setNcbiGeneId( Integer.parseInt( info.getGeneId() ) );
        gene.setName( info.getDefaultSymbol() );
        gene.setOfficialSymbol( info.getDefaultSymbol() );
        gene.setOfficialName( info.getDescription() );
        gene.setEnsemblId( info.getEnsemblId() );

        if ( info.getHistory() != null ) {
            if ( info.getHistory().getCurrentId() != null ) {
                assert info.getGeneId().equals( info.getHistory().getCurrentId() );
            }

            String previousId = info.getHistory().getPreviousId();
            if ( previousId != null ) {
                gene.setPreviousNcbiId( previousId );
            }
        }

        gene.setDescription( "Imported from NCBI gene; Nomenclature status: " + info.getNomenclatureStatus() );

        Taxon t = Taxon.Factory.newInstance();
        t.setNcbiId( info.getTaxId() );
        t.setIsGenesUsable( false );
        t.setIsSpecies( true );
        gene.setTaxon( t );

        PhysicalLocation pl = PhysicalLocation.Factory.newInstance();
        Chromosome chrom = Chromosome.Factory.newInstance();
        chrom.setTaxon( t );
        chrom.setName( info.getChromosome() );
        pl.setChromosome( chrom );

        CytogeneticLocation cl = CytogeneticLocation.Factory.newInstance();
        cl.setChromosome( chrom );
        cl.setBand( info.getMapLocation() );

        gene.setPhysicalLocation( pl );
        gene.setCytogenicLocation( cl );

        Collection<GeneAlias> aliases = gene.getAliases();
        for ( String alias : info.getSynonyms() ) {
            GeneAlias newAlias = GeneAlias.Factory.newInstance();
            newAlias.setAlias( alias );
            aliases.add( newAlias );
        }

        for ( String dbname : info.getDbXrefs().keySet() ) {
            if ( !dbname.equalsIgnoreCase( "Ensembl" ) ) continue;
            String identifier = info.getDbXrefs().get( dbname );
            DatabaseEntry crossref = DatabaseEntry.Factory.newInstance();
            crossref.setAccession( identifier );
            crossref.setExternalDatabase( getEnsembl() );
            gene.getAccessions().add( crossref );
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
            return this.convert( ( Collection<Object> ) sourceDomainObject );
        }
        assert sourceDomainObject instanceof NCBIGene2Accession;
        NCBIGene2Accession ncbiGene = ( NCBIGene2Accession ) sourceDomainObject;
        return convert( ncbiGene.getInfo() );
    }

    /**
     * @param acc
     * @param gene
     * @return
     */
    public Collection<GeneProduct> convert( NCBIGene2Accession acc, Gene gene ) {
        Collection<GeneProduct> geneProducts = new HashSet<GeneProduct>();
        // initialize up to two Gene Products
        // one for RNA, one for Protein (if retainProteinInformation = true)

        // RNA section
        if ( acc.getRnaNucleotideAccession() != null ) {
            GeneProduct rna = GeneProduct.Factory.newInstance();

            // set available fields
            rna.setNcbiGi( acc.getRnaNucleotideGI() );
            rna.setGene( gene );
            rna.setName( acc.getRnaNucleotideAccession() );
            rna.setType( GeneProductType.RNA );

            String description = "Imported from NCBI Gene";

            if ( acc.getStatus() != null ) {
                description = description + " (Refseq status: " + acc.getStatus() + ").";
            }

            if ( acc.getRnaNucleotideAccession() != null ) {
                DatabaseEntry accession = DatabaseEntry.Factory.newInstance();
                accession.setAccession( acc.getRnaNucleotideAccession() );
                accession.setAccessionVersion( acc.getRnaNucleotideAccessionVersion() );
                accession.setExternalDatabase( genBank );
                if ( rna.getAccessions() == null ) {
                    rna.setAccessions( new HashSet<DatabaseEntry>() );
                }
                rna.getAccessions().add( accession );
            }

            /*
             * Fill in physical location details.
             */
            if ( acc.getGenomicNucleotideAccession() != null && gene.getPhysicalLocation() != null ) {
                getChromosomeDetails( acc, gene );
                PhysicalLocation pl = getPhysicalLocation( acc, gene );
                rna.setPhysicalLocation( pl );
            }

            rna.setDescription( description );
            geneProducts.add( rna );
        }

        // Protein section
        if ( retainProteinInformation && acc.getProteinAccession() != null ) {
            GeneProduct protein = GeneProduct.Factory.newInstance();

            // set available fields
            protein.setNcbiGi( acc.getProteinGI() );
            protein.setGene( gene );
            protein.setName( acc.getProteinAccession() );
            protein.setType( GeneProductType.PROTEIN );
            protein.setDescription( "Imported from NCBI Gene"
                    + ( acc.getStatus() != null ? " (" + acc.getStatus() + ")" : "" ) );

            DatabaseEntry accession = DatabaseEntry.Factory.newInstance();
            accession.setAccession( acc.getProteinAccession() );
            accession.setAccessionVersion( acc.getProteinAccessionVersion() );
            accession.setExternalDatabase( genBank );

            Collection<DatabaseEntry> accessions = new HashSet<DatabaseEntry>();
            accessions.add( accession );
            protein.setAccessions( accessions );
            geneProducts.add( protein );
        }
        return geneProducts;
    }

    /**
     * @param acc
     * @param gene
     * @return
     */
    private PhysicalLocation getPhysicalLocation( NCBIGene2Accession acc, Gene gene ) {
        PhysicalLocation pl = PhysicalLocation.Factory.newInstance();
        pl.setChromosome( gene.getPhysicalLocation().getChromosome() );
        if ( acc.getOrientation() != null ) {
            pl.setStrand( acc.getOrientation() );
        }
        if ( acc.getStartPosition() != null ) {
            pl.setNucleotide( acc.getStartPosition() );
            pl.setNucleotideLength( ( int ) Math.abs( acc.getEndPosition() - acc.getStartPosition() ) );
            pl.setBin( SequenceBinUtils.binFromRange( acc.getStartPosition().intValue(), acc.getEndPosition()
                    .intValue() ) );
        }
        return pl;
    }

    /**
     * @param acc
     * @param gene
     */
    private void getChromosomeDetails( NCBIGene2Accession acc, Gene gene ) {
        Chromosome chrom = gene.getPhysicalLocation().getChromosome();
        BioSequence chromSeq = BioSequence.Factory.newInstance();
        chromSeq.setName( acc.getGenomicNucleotideAccession() );
        chromSeq.setType( SequenceType.WHOLE_CHROMOSOME );
        chromSeq.setTaxon( gene.getTaxon() );
        DatabaseEntry dbe = DatabaseEntry.Factory.newInstance();
        dbe.setExternalDatabase( genBank );
        dbe.setAccession( acc.getGenomicNucleotideAccession() );
        dbe.setAccessionVersion( acc.getGenomicNucleotideAccessionVersion() );
        chromSeq.setSequenceDatabaseEntry( dbe );
        chrom.setSequence( chromSeq );
    }

    public Gene convert( NcbiGeneData data ) {
        // get gene info and fill in gene
        NCBIGeneInfo geneInfo = data.getGeneInfo();
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
     * Threaded conversion of domain objects to Gemma objects.
     */
    public void convert( final BlockingQueue<NcbiGeneData> geneInfoQueue, final BlockingQueue<Gene> geneQueue ) {
        // start up thread to convert a member of geneInfoQueue to a gene/geneproduct/databaseentry
        // then push the gene onto the geneQueue for loading

        if ( !retainProteinInformation ) {
            log.info( "Note that protein information will be ignored; set " + RETAIN_PROTEIN_INFO_PARAM
                    + " to true to change" );
        }

        Thread convertThread = new Thread( new Runnable() {
            @SuppressWarnings("synthetic-access")
            public void run() {
                while ( !( sourceDone.get() && geneInfoQueue.isEmpty() ) ) {
                    try {
                        NcbiGeneData data = geneInfoQueue.poll();
                        if ( data == null ) {
                            continue;
                        }
                        geneQueue.put( convert( data ) );

                    } catch ( InterruptedException e ) {
                        log.warn( "Interrupted" );
                        break;
                    }
                }
                producerDone.set( true );
            }
        }, "Converter" );

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

    /**
     * @return the genBank
     */
    public static ExternalDatabase getGenbank() {
        return genBank;
    }

    /**
     * @return the ensembl
     */
    public static ExternalDatabase getEnsembl() {
        return ensembl;
    }

}
