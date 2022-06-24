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
package ubic.gemma.core.loader.genome.gene.ncbi;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.loader.genome.gene.ncbi.model.NCBIGene2Accession;
import ubic.gemma.core.loader.genome.gene.ncbi.model.NCBIGeneInfo;
import ubic.gemma.core.loader.util.QueuingParser;
import ubic.gemma.core.loader.util.parser.BasicLineParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Class to parse the NCBI gene2accession files. Results are stored in a "Source domain object", not a Gemma Gene.
 *
 * @author pavlidis
 * @see NCBIGene2Accession
 */
public class NcbiGene2AccessionParser extends BasicLineParser<NCBIGene2Accession>
        implements QueuingParser<NcbiGeneData> {

    /**
     * They keep changing this...this is now a minimum value.
     */
    private static final int NCBI_GENE2ACCESSION_FIELDS_PER_ROW = 13;

    private final Collection<NCBIGene2Accession> results = new HashSet<>();
    Map<String, NCBIGeneInfo> geneInfo = null;
    private BlockingQueue<NcbiGeneData> queue = null;
    private String lastGeneId = null;
    // a grouping of Gene2Accessions with the same gene Id
    private NcbiGeneData geneData = new NcbiGeneData();
    private int count = 0;

    private Integer startingNcbiId = null;
    private boolean hasStarted = false;

    @Override
    public void parse( InputStream is, BlockingQueue<NcbiGeneData> aQueue ) throws IOException {
        if ( is == null )
            throw new IllegalArgumentException( "InputStream was null" );
        this.queue = aQueue;
        if ( startingNcbiId == null )
            hasStarted = true;
        super.parse( is );
    }

    public void parse( File f, BlockingQueue<NcbiGeneData> queue1, Map<String, NCBIGeneInfo> geneInfo1 )
            throws IOException {
        this.queue = queue1;
        this.geneInfo = geneInfo1;
        if ( startingNcbiId == null )
            hasStarted = true;

        super.parse( f );
    }

    @Override
    public NCBIGene2Accession parseOneLine( String line ) {
        String[] fields = StringUtils.splitPreserveAllTokens( line, '\t' );

        if ( fields.length < NcbiGene2AccessionParser.NCBI_GENE2ACCESSION_FIELDS_PER_ROW ) {
            throw new IllegalArgumentException(
                    "Line is not in the right format: has " + fields.length + " fields, expected "
                            + NcbiGene2AccessionParser.NCBI_GENE2ACCESSION_FIELDS_PER_ROW );
        }

        NCBIGene2Accession currentAccession = this.processFields( fields );

        if ( currentAccession == null ) {
            return null;
        }

        this.addResult( currentAccession ); // really doesn't serve much of a purpose

        /*
         * Only some genes are relevant - for example, we might have filtered them by taxon.
         */
        if ( geneInfo != null && !geneInfo.containsKey( currentAccession.getGeneId() ) ) {
            return null;
        }

        // if the current gene Id is different from this current one, then
        // we are done with the gene Id. Push the geneCollection into the queue.
        if ( lastGeneId != null && !lastGeneId.equalsIgnoreCase( currentAccession.getGeneId() ) ) {
            // push the gene set to the queue
            try {
                queue.put( geneData );
            } catch ( InterruptedException e ) {
                throw new RuntimeException( e );
            }
            // clear the gene set
            geneData = new NcbiGeneData();
            if ( geneInfo != null )
                geneInfo.remove( lastGeneId );
        }

        assert currentAccession.getGeneId() != null;

        // we're either starting a new one, or continuing with an old one.
        lastGeneId = currentAccession.getGeneId();
        geneData.addAccession( currentAccession );
        geneData.setGeneInfo( geneInfo.get( currentAccession.getGeneId() ) );

        // this will be a trailing accession.?
        return currentAccession;
    }

    @Override
    public Collection<NCBIGene2Accession> getResults() {
        return results;
    }

    /*
     * This has been overridden to add postprocessing to the gene2accession file. This involves adding the
     * last gene that had accessions (if available) and adding the remaining genes without accessions
     *
     */
    @Override
    public void parse( InputStream is ) throws IOException {
        if ( startingNcbiId == null )
            hasStarted = true;
        super.parse( is );

        // add last gene with an accession
        if ( geneData.getGeneInfo() != null ) {
            try {
                queue.put( geneData );
            } catch ( InterruptedException e ) {
                throw new RuntimeException( e );
            }
            geneInfo.remove( lastGeneId );
        }
        // add remaining genes
        // push in remaining genes that did not have accessions
        Collection<NCBIGeneInfo> remainingGenes = geneInfo.values();
        for ( NCBIGeneInfo o : remainingGenes ) {
            NcbiGeneData geneCollection = new NcbiGeneData();
            geneCollection.setGeneInfo( o );
            try {
                queue.put( geneCollection );
            } catch ( InterruptedException e ) {
                throw new RuntimeException();
            }
        }
    }

    @Override
    protected void addResult( NCBIGene2Accession obj ) {
        count++;
        // results.add( ( NCBIGene2Accession ) obj );
        // no-op - save memory as we use a queue instead.
    }

    public int getCount() {
        return count;
    }

    public void setStartingNbiId( Integer startingNcbiId ) {
        this.startingNcbiId = startingNcbiId;
    }

    private NCBIGene2Accession processFields( String[] fields ) {
        NCBIGene2Accession newGene = new NCBIGene2Accession();
        try {

            /*
             * Skip lines that refer to locations in non-reference assemblies.
             */
            if ( fields[12].startsWith( "Alternate assembly" ) ) {
                return null;
            }

            newGene.setGeneId( fields[1] );

            if ( !hasStarted ) {
                assert startingNcbiId != null;
                if ( startingNcbiId.equals( Integer.parseInt( fields[1] ) ) ) {
                    log.info( "Found the starting gene " + startingNcbiId );
                    hasStarted = true;
                } else {
                    return null;
                }
            }

            // #Format:
            // tax_id 0
            // GeneID 1
            // status 2
            // RNA_nucleotide_accession.version 3
            // RNA_nucleotide_gi 4
            // protein_accession.version 5
            // protein_gi 6
            // genomic_nucleotide_accession.version 7
            // genomic_nucleotide_gi 8
            // start_position_on_the_genomic_accession 9
            // end_position_on_the_genomic_accession 10
            // orientation 11
            // assembly 12
            // mature_peptide_accession.version 13
            // mature_peptide_gi 14
            // Symbol 15

            newGene.setTaxId( Integer.parseInt( fields[0] ) );
            newGene.setStatus( fields[2].equals( "-" ) ? null : fields[2] );
            newGene.setRnaNucleotideAccession( fields[3].equals( "-" ) ? null : fields[3] );
            newGene.setRnaNucleotideGI( fields[4].equals( "-" ) ? null : fields[4] );
            newGene.setProteinAccession( fields[5].equals( "-" ) ? null : fields[5] );
            newGene.setProteinGI( fields[6].equals( "-" ) ? null : fields[6] );
            newGene.setGenomicNucleotideAccession( fields[7].equals( "-" ) ? null : fields[7] );
            newGene.setGenomicNucleotideGI( fields[8].equals( "-" ) ? null : fields[8] );
            newGene.setStartPosition( fields[9].equals( "-" ) ? null : Long.parseLong( fields[9] ) );
            newGene.setEndPosition( fields[10].equals( "-" ) ? null : Long.parseLong( fields[10] ) );
            newGene.setOrientation( fields[11].equals( "?" ) ? null : fields[11] );

            // set accession version numbers (additional parsing)
            // the assumption is that the string is delimited by a dot
            // and it only has one dot with one version number (ie GS001.1, not GS001.1.1)
            // RNA
            String rnaAccession = newGene.getRnaNucleotideAccession();
            if ( StringUtils.isNotBlank( rnaAccession ) ) {
                String[] tokens = StringUtils.splitPreserveAllTokens( rnaAccession, '.' );
                switch ( tokens.length ) {
                    case 1:
                        newGene.setRnaNucleotideAccession( tokens[0] );
                        newGene.setRnaNucleotideAccessionVersion( null );
                        break;
                    case 2:
                        newGene.setRnaNucleotideAccession( tokens[0] );
                        newGene.setRnaNucleotideAccessionVersion( tokens[1] );
                        break;
                    default:
                        throw new UnsupportedOperationException( "Don't know how to deal with " + rnaAccession );
                }
            } else {
                newGene.setRnaNucleotideAccessionVersion( null );
                newGene.setRnaNucleotideAccessionVersion( null );
            }

            // protein
            String proteinAccession = newGene.getProteinAccession();
            if ( StringUtils.isNotBlank( proteinAccession ) ) {
                String[] tokens = StringUtils.splitPreserveAllTokens( proteinAccession, '.' );
                switch ( tokens.length ) {
                    case 1:
                        newGene.setProteinAccession( tokens[0] );
                        newGene.setProteinAccessionVersion( null );
                        break;
                    case 2:
                        newGene.setProteinAccession( tokens[0] );
                        newGene.setProteinAccessionVersion( tokens[1] );
                        break;
                    default:
                        throw new UnsupportedOperationException( "Don't know how to deal with " + proteinAccession );
                }
            } else {
                newGene.setProteinAccessionVersion( null );
                newGene.setProteinAccessionVersion( null );
            }

            // Genome (chromosome information)
            String genomicAccession = newGene.getGenomicNucleotideAccession();
            if ( StringUtils.isNotBlank( genomicAccession ) ) {
                String[] tokens = StringUtils.splitPreserveAllTokens( genomicAccession, '.' );
                switch ( tokens.length ) {
                    case 1:
                        newGene.setGenomicNucleotideAccession( tokens[0] );
                        newGene.setGenomicNucleotideAccessionVersion( null );
                        break;
                    case 2:
                        newGene.setGenomicNucleotideAccession( tokens[0] );
                        newGene.setGenomicNucleotideAccessionVersion( tokens[1] );
                        break;
                    default:
                        throw new UnsupportedOperationException( "Don't know how to deal with " + genomicAccession );
                }
            } else {
                newGene.setGenomicNucleotideAccessionVersion( null );
                newGene.setGenomicNucleotideAccessionVersion( null );
            }

        } catch ( NumberFormatException e ) {
            throw new RuntimeException( e );
        }
        return newGene;
    }

}
