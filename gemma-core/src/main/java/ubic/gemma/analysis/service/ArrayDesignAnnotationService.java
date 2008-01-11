/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.analysis.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.iterators.TransformIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.FileTools;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.PredictedGene;
import ubic.gemma.model.genome.ProbeAlignedRegion;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.ontology.GeneOntologyService;
import ubic.gemma.ontology.OntologyTerm;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.DateUtil;

/**
 * Methods to generate annotations for array designs, based on information alreay in the database. This can be used to
 * generate annotation files used for ermineJ, for eexample. The file format:
 * <ul>
 * <li> The file is tab-delimited text. Comma-delimited files or Excel spreadsheets (for example) are not supported.
 * </li>
 * <li> There is a one-line header included in the file for readability. </li>
 * <li> The first column contains the probe identifier </li>
 * <li> The second column contains a gene symbol(s). Clusters are delimited by '|' and genes within clusters are
 * delimited by ','</li>
 * <li> The third column contains the gene names (or description). Clusters are delimited by '|' and names within
 * clusters are delimited by '$'</li>
 * <li> The fourth column contains a delimited list of GO identifiers. These include the "GO:" prefix. Thus they read
 * "GO:00494494" and not "494494". Delimited by '|'. </li>
 * </ul>
 * <p>
 * Note that for backwards compatibility, GO terms are not segregated by gene cluster.
 * </p>
 * 
 * @spring.bean id="arrayDesignAnnotationService"
 * @spring.property name="gene2GOAssociationService" ref="gene2GOAssociationService"
 * @spring.property name="goService" ref="geneOntologyService"
 * @author paul
 * @version $Id$
 */
public class ArrayDesignAnnotationService {

    public static final String ANNOTATION_FILE_SUFFIX = ".an.txt.gz";

    public static final String ANNOT_DATA_DIR = ConfigUtils.getString( "gemma.appdata.home" ) + File.separatorChar
            + "microAnnots" + File.separatorChar;

    private static final String COMMENT_CHARACTER = "#";

    private static Log log = LogFactory.getLog( ArrayDesignAnnotationService.class.getName() );;

    /**
     * @param arrayDesign
     * @return Map of composite sequence ids and transient (incomplete) genes. The genes only have the symbol filled in.
     */
    public static Map<Long, Collection<Gene>> readAnnotationFile( ArrayDesign arrayDesign ) {
        Map<Long, Collection<Gene>> results = new HashMap<Long, Collection<Gene>>();
        File f = new File( ANNOT_DATA_DIR + arrayDesign.getShortName() + ANNOTATION_FILE_SUFFIX );
        if ( !f.canRead() ) {
            log.info( "Gene annotations are not available from " + f );
            return results;
        }

        Map<String, Long> probeNameToId = new HashMap<String, Long>();
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            results.put( cs.getId(), new HashSet<Gene>() );
            if ( probeNameToId.containsKey( cs.getName() ) ) {
                log.warn( "Duplicate probe name: " + cs.getName() );
            }
            probeNameToId.put( cs.getName(), cs.getId() );
        }

        try {
            log.info( "Reading annotations from: " + f );
            InputStream is = FileTools.getInputStreamFromPlainOrCompressedFile( f.getAbsolutePath() );
            BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
            String line = null;

            while ( ( line = br.readLine() ) != null ) {
                if ( StringUtils.isBlank( line ) || line.startsWith( COMMENT_CHARACTER ) ) {
                    continue;
                }
                String[] fields = StringUtils.splitPreserveAllTokens( line, '\t' );

                if ( fields.length < 3 ) continue; // means there are no gene annotations.

                String probeName = fields[0];

                if ( !probeNameToId.containsKey( probeName ) ) continue;
                Long probeId = probeNameToId.get( probeName );

                List<String> geneSymbols = Arrays.asList( StringUtils.split( fields[1], '|' ) );
                List<String> geneNames = Arrays.asList( StringUtils.split( fields[2], '|' ) );

                for ( int i = 0; i < geneSymbols.size(); i++ ) {
                    Gene g = Gene.Factory.newInstance();
                    String symbol = geneSymbols.get( i );
                    if ( StringUtils.isBlank( symbol ) ) {
                        continue;
                    }
                    g.setOfficialSymbol( symbol );

                    if ( i < geneNames.size() ) {
                        g.setOfficialName( geneNames.get( i ) );
                    }

                    results.get( probeId ).add( g );
                }
            }

            return results;
        } catch ( FileNotFoundException e ) {
            throw new RuntimeException( e );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    private Gene2GOAssociationService gene2GOAssociationService;

    private GeneOntologyService goService;

    Transformer officialSymbolExtractor = new Transformer() {
        public Object transform( Object input ) {
            return ( ( Gene ) input ).getOfficialSymbol();
        }
    };

    Transformer descriptionExtractor = new Transformer() {
        public Object transform( Object input ) {
            Gene gene = ( Gene ) input;
            return gene.getOfficialName();
        }
    };

    Transformer goTermExtractor = new Transformer() {
        public Object transform( Object input ) {
            return GeneOntologyService.asRegularGoId( ( ( OntologyTerm ) input ) );
        }
    };

    /**
     * @param writer
     * @param compositeSequences
     * @param ty whether to include parents (OutputType.LONG); only use biological process (OutputType.BIOPROCESS) or
     *        'standard' output (OutputType.SHORT).
     * @param knownGenesOnly Whether output should include PARs and predicted genes. If true, they will be excluded.
     * @return number processed.
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public int generateAnnotationFile( Writer writer,
            Map<CompositeSequence, Map<PhysicalLocation, Collection<BlatAssociation>>> genesWithSpecificity,
            OutputType ty, boolean knownGenesOnly ) throws IOException {

        int compositeSequencesProcessed = 0;

        for ( CompositeSequence cs : genesWithSpecificity.keySet() ) {

            // Collection<Gene> genes = compositeSequenceService.getGenes( sequence );
            Map<PhysicalLocation, Collection<BlatAssociation>> geneclusters = genesWithSpecificity.get( cs );

            if ( geneclusters.isEmpty() ) {
                writeAnnotationLine( writer, cs.getName(), "", "", null );
                continue;
            }

            List<Collection<OntologyTerm>> goTerms = new ArrayList<Collection<OntologyTerm>>();
            List<String> genes = new ArrayList<String>();
            List<String> geneDescriptions = new ArrayList<String>();
            for ( Collection<BlatAssociation> cluster : geneclusters.values() ) {

                Collection<Gene> retained = new HashSet<Gene>();

                Collection<OntologyTerm> clusterGoTerms = new HashSet<OntologyTerm>();
                for ( BlatAssociation bla : cluster ) {
                    Gene g = bla.getGeneProduct().getGene();
                    if ( knownGenesOnly && ( g instanceof PredictedGene || g instanceof ProbeAlignedRegion ) ) {
                        continue;
                    }

                    if ( log.isDebugEnabled() )
                        log.debug( "Adding gene: " + g.getOfficialSymbol() + " of type: " + g.getClass() );

                    retained.add( g );
                }

                if ( retained.size() == 0 ) continue;

                List<Gene> retainedGenes = new ArrayList<Gene>( retained );
                for ( Gene g : retainedGenes ) {
                    clusterGoTerms.addAll( getGoTerms( g, ty ) );
                }

                genes.add( StringUtils
                        .join( new TransformIterator( retained.iterator(), officialSymbolExtractor ), "," ) );

                // This breaks if the descriptions contain "$".
                geneDescriptions.add( StringUtils.join( new TransformIterator( retained.iterator(),
                        descriptionExtractor ), "$" ) );

                goTerms.add( clusterGoTerms );
            }

            String geneString = StringUtils.join( genes, "|" );
            String geneDescriptionString = StringUtils.join( geneDescriptions, "|" );
            writeAnnotationLine( writer, cs.getName(), geneString, geneDescriptionString, goTerms );

            if ( ++compositeSequencesProcessed % 500 == 0 && log.isInfoEnabled() ) {
                log.info( "Processed " + compositeSequencesProcessed + "/" + genesWithSpecificity.size()
                        + " compositeSequences " );
            }

        }
        writer.close();
        return compositeSequencesProcessed;
    }

    /**
     * Opens a file for writing and adds the header.
     * 
     * @param fileBaseName if Null, output will be written to standard output.
     * @param overWrite clobber existing file. Otherwise returns null.
     * @return writer to use
     * @throws IOException
     */
    public Writer initOutputFile( String fileBaseName, boolean overWrite ) throws IOException {

        Writer writer;
        if ( StringUtils.isBlank( fileBaseName ) ) {
            log.info( "Output to stdout" );
            writer = new PrintWriter( System.out );
        } else {

            log.info( "Attempting to create new annotation file " + fileBaseName + " \n" );

            File f = new File( ANNOT_DATA_DIR + fileBaseName + ANNOTATION_FILE_SUFFIX );

            if ( f.exists() ) {
                if ( overWrite ) {
                    log.warn( "Will overwrite existing file " + f );
                    f.delete();
                } else {
                    return null;
                }
            }

            File parentDir = f.getParentFile();
            if ( !parentDir.exists() ) parentDir.mkdirs();
            writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( f ) ) );
        }
        StringBuilder buf = new StringBuilder();
        buf.append( "# Array design annotation file generated by Gemma\n" );
        buf.append( "# Generated " + DateUtil.convertDateToString( new Date() ) + "\n" );
        buf.append( "# If you use this file for your research, please cite the Gemma web site.\n" );
        writer.write( buf.toString() );
        writer.write( "ProbeName\tGeneSymbols\tGeneNames\tGOTerms\n" );

        return writer;
    }

    public void setGene2GOAssociationService( Gene2GOAssociationService gene2GOAssociationService ) {
        this.gene2GOAssociationService = gene2GOAssociationService;
    }

    public void setGoService( GeneOntologyService goService ) {
        this.goService = goService;
    }

    /**
     * @param gene
     * @param ty Configures which GO terms to return: With all parents, biological process only, or direct annotations
     *        only.
     * @return the goTerms for a given gene, as configured
     */
    @SuppressWarnings("unchecked")
    private Collection<OntologyTerm> getGoTerms( Gene gene, OutputType ty ) {

        Collection<VocabCharacteristic> ontos = new HashSet<VocabCharacteristic>( gene2GOAssociationService
                .findByGene( gene ) );

        Collection<OntologyTerm> results = new HashSet<OntologyTerm>();
        if ( ontos.size() == 0 ) return results;

        for ( VocabCharacteristic vc : ontos ) {
            results.add( GeneOntologyService.getTermForId( vc.getValue() ) );
        }

        if ( ty.equals( OutputType.SHORT ) ) return results;

        if ( ty.equals( OutputType.LONG ) ) {
            Collection<OntologyTerm> oes = goService.getAllParents( results );
            results.addAll( oes );
        } else if ( ty.equals( OutputType.BIOPROCESS ) ) {
            Collection<OntologyTerm> toRemove = new HashSet<OntologyTerm>();

            for ( OntologyTerm ont : results ) {
                if ( ( ont == null ) ) {
                    continue; // / shouldn't happen!
                }
                if ( !goService.isBiologicalProcess( ont ) ) {
                    toRemove.add( ont );
                }
            }

            for ( OntologyTerm toRemoveOnto : toRemove ) {
                results.remove( toRemoveOnto );
            }
        }
        return results;
    }

    /**
     * @param probeId
     * @param gene
     * @param description
     * @param goTerms
     * @throws IOException Adds one line at a time to the annotation file
     */
    private void writeAnnotationLine( Writer writer, String probeId, String gene, String description,
            List<Collection<OntologyTerm>> goTerms ) throws IOException {

        if ( log.isDebugEnabled() ) log.debug( "Generating line for annotation file  \n" );

        if ( gene == null ) gene = "";

        if ( description == null ) description = "";

        writer.write( probeId + "\t" + gene + "\t" + description + "\t" );

        if ( ( goTerms == null ) || goTerms.isEmpty() ) {
            writer.write( "\n" );
            return;
        }

        List<String> clusterGoterms = new ArrayList<String>();
        for ( Collection<OntologyTerm> oe : goTerms ) {
            clusterGoterms.add( StringUtils.join( new TransformIterator( oe.iterator(), goTermExtractor ), "|" ) );
        }
        String goterms = StringUtils.join( clusterGoterms, "|" );
        writer.write( goterms );

        writer.write( "\n" );
        writer.flush();

    }

    public enum OutputType {
        SHORT, LONG, BIOPROCESS
    }

}
