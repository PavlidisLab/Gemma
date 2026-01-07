/*
 * The Gemma project
 *
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.core.loader.genome.gene.ncbi;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.core.analysis.sequence.SequenceBinUtils;
import ubic.gemma.core.loader.genome.gene.ncbi.model.NCBIGene2Accession;
import ubic.gemma.core.loader.genome.gene.ncbi.model.NCBIGeneInfo;
import ubic.gemma.core.loader.util.converter.Converter;
import ubic.gemma.core.util.concurrent.ThreadUtils;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.model.genome.gene.GeneAlias;
import ubic.gemma.model.genome.gene.GeneProduct;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Convert NCBIGene2Accession objects into Gemma Gene objects with associated GeneProducts. Genes without products are
 * ignored.
 *
 * @author pavlidis
 * @author jrsantos
 */
@SuppressWarnings({ "WeakerAccess", "unused" }) // Possible external use
public class NcbiGeneConverter implements Converter<Object, Object> {

    private static final Log log = LogFactory.getLog( NcbiGeneConverter.class.getName() );
    private static final ExternalDatabase genBank;
    private static final ExternalDatabase ensembl;

    static {
        genBank = ExternalDatabase.Factory.newInstance();
        NcbiGeneConverter.genBank.setName( "Genbank" );
        ensembl = ExternalDatabase.Factory.newInstance();
        NcbiGeneConverter.ensembl.setName( "Ensembl" );
    }

    AtomicBoolean producerDone = new AtomicBoolean( false );
    AtomicBoolean sourceDone = new AtomicBoolean( false );

    /**
     * @return the genBank
     */
    public static ExternalDatabase getGenbank() {
        return NcbiGeneConverter.genBank;
    }

    /**
     * @return the ensembl
     */
    public static ExternalDatabase getEnsembl() {
        return NcbiGeneConverter.ensembl;
    }

    @Override
    public Collection<Object> convert( Collection<?> sourceDomainObjects ) {
        Collection<Object> results = new HashSet<>();
        for ( Object object : sourceDomainObjects ) {
            results.add( this.convert( object ) );
        }
        return results;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object convert( Object sourceDomainObject ) {
        if ( sourceDomainObject instanceof Collection ) {
            return this.convert( ( Collection<Object> ) sourceDomainObject );
        }
        assert sourceDomainObject instanceof NCBIGene2Accession;
        NCBIGene2Accession ncbiGene = ( NCBIGene2Accession ) sourceDomainObject;
        return this.convert( ncbiGene.getInfo() );
    }

    public Gene convert( NCBIGeneInfo info ) {
        Gene gene = Gene.Factory.newInstance();

        gene.setNcbiGeneId( Integer.parseInt( info.getGeneId() ) );
        gene.setName( info.getDefaultSymbol() );
        gene.setOfficialSymbol( info.getDefaultSymbol() );
        gene.setOfficialName( info.getDescription() );
        gene.setEnsemblId( info.getEnsemblId() );

        /*
         * NOTE we allow multiple discontinued or previous ids, separated by commas. This is a hack to account for cases
         * uncovered recently...can be minimized by running this regularly.
         */
        if ( info.getHistory() != null ) {
            assert info.getHistory().getCurrentId() == null || info.getGeneId()
                    .equals( info.getHistory().getCurrentId() );

            assert info.getHistory().getPreviousIds() != null;
            if ( !info.getHistory().getPreviousIds().isEmpty() ) {
                String previousIds = StringUtils.join( info.getHistory().getPreviousIds(), "," );
                gene.setPreviousNcbiGeneId( previousIds );
            }

        } else if ( StringUtils.isNotBlank( info.getDiscontinuedId() ) ) {
            if ( NcbiGeneConverter.log.isDebugEnabled() )
                NcbiGeneConverter.log
                        .debug( "Gene matches a gene that was discontinued: " + gene + " matches gene that had id "
                                + info.getDiscontinuedId() );
            gene.setPreviousNcbiGeneId( info.getDiscontinuedId() );
        }

        gene.setDescription( "Imported from NCBI gene; Nomenclature status: " + info.getNomenclatureStatus() );

        Taxon t = Taxon.Factory.newInstance();
        t.setNcbiId( info.getTaxId() );
        t.setIsGenesUsable( false );
        gene.setTaxon( t );

        /*
         * We are going to stop maintaining this information
         */
        PhysicalLocation pl = PhysicalLocation.Factory.newInstance();
        Chromosome chrom = new Chromosome( info.getChromosome(), t );
        pl.setChromosome( chrom );

        gene.setPhysicalLocation( pl );

        Collection<GeneAlias> aliases = gene.getAliases();
        for ( String alias : info.getSynonyms() ) {
            GeneAlias newAlias = GeneAlias.Factory.newInstance();
            newAlias.setAlias( alias );
            aliases.add( newAlias );
        }

        for ( String dbname : info.getDbXrefs().keySet() ) {
            if ( !dbname.equalsIgnoreCase( "Ensembl" ) )
                continue;
            String identifier = info.getDbXrefs().get( dbname );
            DatabaseEntry crossref = DatabaseEntry.Factory.newInstance();
            crossref.setAccession( identifier );
            crossref.setExternalDatabase( NcbiGeneConverter.getEnsembl() );
            gene.getAccessions().add( crossref );
        }

        return gene;

    }

    public Collection<GeneProduct> convert( NCBIGene2Accession acc, Gene gene ) {
        Collection<GeneProduct> geneProducts = new HashSet<>();
        // initialize up to two Gene Products

        // RNA section
        if ( acc.getRnaNucleotideAccession() != null ) {
            GeneProduct rna = GeneProduct.Factory.newInstance();

            // set available fields
            rna.setNcbiGi( acc.getRnaNucleotideGI() );
            rna.setGene( gene );
            rna.setName( acc.getRnaNucleotideAccession() );

            String description = "Imported from NCBI Gene";

            if ( acc.getStatus() != null ) {
                description = description + " (Refseq status: " + acc.getStatus() + ").";
            }

            if ( acc.getRnaNucleotideAccession() != null ) {
                DatabaseEntry accession = DatabaseEntry.Factory.newInstance();
                accession.setAccession( acc.getRnaNucleotideAccession() );
                accession.setAccessionVersion( acc.getRnaNucleotideAccessionVersion() );
                accession.setExternalDatabase( NcbiGeneConverter.genBank );
                if ( rna.getAccessions() == null ) {
                    rna.setAccessions( new HashSet<DatabaseEntry>() );
                }
                rna.getAccessions().add( accession );
            }

            /*
             * Fill in physical location details.
             */
            if ( acc.getGenomicNucleotideAccession() != null && gene.getPhysicalLocation() != null ) {
                this.getChromosomeDetails( acc, gene );
                PhysicalLocation pl = this.getPhysicalLocation( acc, gene );
                rna.setPhysicalLocation( pl );
            }

            rna.setDescription( description );
            geneProducts.add( rna );
        }

        return geneProducts;
    }

    public Gene convert( NcbiGeneData data ) {
        // get gene info and fill in gene
        NCBIGeneInfo geneInfo = data.getGeneInfo();
        Gene gene = this.convert( geneInfo );

        // grab all accessions and fill in GeneProduct/DatabaseEntry
        // and associate with Gene
        Collection<NCBIGene2Accession> gene2accession = data.getAccessions();
        Set<GeneProduct> geneProducts = new HashSet<>();

        for ( NCBIGene2Accession acc : gene2accession ) {
            geneProducts.addAll( this.convert( acc, gene ) );
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

        Thread convertThread = ThreadUtils.newThread( new Runnable() {
            @Override
            @SuppressWarnings("synthetic-access")
            public void run() {
                while ( !( sourceDone.get() && geneInfoQueue.isEmpty() ) ) {
                    try {
                        NcbiGeneData data = geneInfoQueue.poll();
                        if ( data == null ) {
                            continue;
                        }
                        Gene converted = NcbiGeneConverter.this.convert( data );

                        if ( converted.getProducts().isEmpty() ) {
                            if ( log.isDebugEnabled() ) log.debug( "Gene with no products skipped: " + converted );
                            continue;
                        }

                        geneQueue.put( converted );

                    } catch ( InterruptedException e ) {
                        NcbiGeneConverter.log.warn( "Interrupted" );
                        break;
                    } catch ( Exception e ) {
                        NcbiGeneConverter.log.error( e, e );
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

    private PhysicalLocation getPhysicalLocation( NCBIGene2Accession acc, Gene gene ) {
        PhysicalLocation pl = PhysicalLocation.Factory.newInstance();
        pl.setChromosome( gene.getPhysicalLocation().getChromosome() );
        if ( acc.getOrientation() != null ) {
            pl.setStrand( acc.getOrientation() );
        }
        if ( acc.getStartPosition() != null ) {
            pl.setNucleotide( acc.getStartPosition() );
            pl.setNucleotideLength( ( int ) Math.abs( acc.getEndPosition() - acc.getStartPosition() ) );
            pl.setBin( SequenceBinUtils
                    .binFromRange( acc.getStartPosition().intValue(), acc.getEndPosition().intValue() ) );
        }
        return pl;
    }

    private void getChromosomeDetails( NCBIGene2Accession acc, Gene gene ) {
        Chromosome chrom = gene.getPhysicalLocation().getChromosome();
        BioSequence chromSeq = BioSequence.Factory.newInstance();
        chromSeq.setName( acc.getGenomicNucleotideAccession() );
        chromSeq.setType( SequenceType.WHOLE_CHROMOSOME );
        chromSeq.setTaxon( gene.getTaxon() );
        DatabaseEntry dbe = DatabaseEntry.Factory.newInstance();
        dbe.setExternalDatabase( NcbiGeneConverter.genBank );
        dbe.setAccession( acc.getGenomicNucleotideAccession() );
        dbe.setAccessionVersion( acc.getGenomicNucleotideAccessionVersion() );
        chromSeq.setSequenceDatabaseEntry( dbe );
        try {
            FieldUtils.writeField( chrom, "sequence", chromSeq, true );
        } catch ( IllegalAccessException e ) {
            throw new RuntimeException( e );
        }

    }

}
