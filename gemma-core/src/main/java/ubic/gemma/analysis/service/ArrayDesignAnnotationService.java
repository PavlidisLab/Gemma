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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.iterators.TransformIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.util.FileTools;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PredictedGene;
import ubic.gemma.model.genome.ProbeAlignedRegion;
import ubic.gemma.ontology.providers.GeneOntologyService;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.DateUtil;

/**
 * Methods to generate annotations for array designs, based on information alreay in the database. This can be used to
 * generate annotation files used for ermineJ, for eexample. The file format:
 * <ul>
 * <li>The file is tab-delimited text. Comma-delimited files or Excel spreadsheets (for example) are not supported.</li>
 * <li>There is a one-line header included in the file for readability.</li>
 * <li>The first column contains the probe identifier</li>
 * <li>The second column contains a gene symbol(s). Clusters are delimited by '|' and genes within clusters are
 * delimited by ','</li>
 * <li>The third column contains the gene names (or description). Clusters are delimited by '|' and names within
 * clusters are delimited by '$'</li>
 * <li>The fourth column contains a delimited list of GO identifiers. These include the "GO:" prefix. Thus they read
 * "GO:00494494" and not "494494". Delimited by '|'.</li>
 * </ul>
 * <p>
 * Note that for backwards compatibility, GO terms are not segregated by gene cluster.
 * </p>
 * 
 * @author paul
 * @version $Id$
 */
@Service
public class ArrayDesignAnnotationService {

    public static final String ANNOTATION_FILE_SUFFIX = ".an.txt.gz";

    public static final String BIO_PROCESS_FILE_SUFFIX = "_bioProcess";

    public static final String NO_PARENTS_FILE_SUFFIX = "_noParents";

    /**
     * String included in file names for standard (default) annotation files. These include GO terms and all parents.
     */
    public static final String STANDARD_FILE_SUFFIX = "";

    public static final String ANNOT_DATA_DIR = ConfigUtils.getString( "gemma.appdata.home" ) + File.separatorChar
            + "microAnnots" + File.separatorChar;

    private static final String COMMENT_CHARACTER = "#";

    private static Log log = LogFactory.getLog( ArrayDesignAnnotationService.class.getName() );

    /**
     * @param arrayDesign
     * @return Map of composite sequence ids and transient (incomplete) genes. The genes only have the symbol filled in.
     */
    public static Map<Long, Collection<Gene>> readAnnotationFile( ArrayDesign arrayDesign ) {
        Map<Long, Collection<Gene>> results = new HashMap<Long, Collection<Gene>>();
        File f = new File( ANNOT_DATA_DIR + mungeFileName( arrayDesign.getShortName() ) + STANDARD_FILE_SUFFIX
                + ANNOTATION_FILE_SUFFIX );
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
            return parseAnnotationFile( results, is, probeNameToId );
        } catch ( FileNotFoundException e ) {
            throw new RuntimeException( e );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param arrayDesign
     * @param is InputStream with the annotations
     * @return Map of composite sequence ids and transient (incomplete) genes. The genes only have the symbol filled in.
     */
    public static Map<Long, Collection<Gene>> readAnnotations( ArrayDesign arrayDesign, InputStream is ) {
        Map<Long, Collection<Gene>> results = new HashMap<Long, Collection<Gene>>();
        Map<String, Long> probeNameToId = new HashMap<String, Long>();
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            results.put( cs.getId(), new HashSet<Gene>() );
            if ( probeNameToId.containsKey( cs.getName() ) ) {
                log.warn( "Duplicate probe name: " + cs.getName() );
            }
            probeNameToId.put( cs.getName(), cs.getId() );
        }

        return parseAnnotationFile( results, is, probeNameToId );
    }

    /**
     * @param results
     * @param f
     * @param probeNameToId
     * @return
     */
    private static Map<Long, Collection<Gene>> parseAnnotationFile( Map<Long, Collection<Gene>> results,
            InputStream is, Map<String, Long> probeNameToId ) {
        try {

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

                List<String> geneSymbols = Arrays.asList( StringUtils.splitPreserveAllTokens( fields[1], '|' ) );
                List<String> geneNames = Arrays.asList( StringUtils.splitPreserveAllTokens( fields[2], '|' ) );

                if ( geneSymbols.size() != geneNames.size() ) {
                    log.warn( "Annotation file format error: Unequal number of gene symbols and names for probe="
                            + probeName + ", skipping row" );
                    continue;
                }

                List<String> gemmaGeneIds = null;
                List<String> ncbiIds = null;

                if ( fields.length > 4 ) { // new style. fields[3] is the GO annotations.
                    gemmaGeneIds = Arrays.asList( StringUtils.splitPreserveAllTokens( fields[4], '|' ) );
                }
                if ( fields.length > 5 ) {
                    ncbiIds = Arrays.asList( StringUtils.splitPreserveAllTokens( fields[5], '|' ) );
                }

                for ( int i = 0; i < geneSymbols.size(); i++ ) {

                    String symbol = geneSymbols.get( i );
                    String name = geneNames.get( i );

                    if ( StringUtils.isBlank( symbol ) ) {
                        continue;
                    }

                    String[] symbolsb = StringUtils.split( symbol, ',' );
                    String[] namesb = StringUtils.split( name, '$' );

                    for ( int j = 0; j < symbolsb.length; j++ ) {

                        String s = symbolsb[j];

                        Gene g = Gene.Factory.newInstance();
                        g.setOfficialSymbol( s );

                        try {
                            if ( gemmaGeneIds != null ) {
                                g.setId( Long.parseLong( gemmaGeneIds.get( j ) ) );
                            }

                            if ( ncbiIds != null ) {
                                g.setNcbiGeneId( Integer.parseInt( ncbiIds.get( j ) ) );
                            }
                        } catch ( NumberFormatException e ) {
                            // oh well, couldn't populate extra info.
                        }

                        if ( namesb.length >= j + 1 ) {
                            String n = namesb[j];
                            g.setName( n );
                        }

                        results.get( probeId ).add( g );
                    }
                }
            }

            return results;
        } catch ( FileNotFoundException e ) {
            throw new RuntimeException( e );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param arrayDesign
     * @return Map of composite sequence ids to an array of delimited strings: [probe name,genes symbol, gene Name,
     *         gemma gene id, ncbi id] for a given probe id. format of string is geneSymbol then geneNames same as found
     *         in annotation file
     */
    public static Map<Long, String[]> readAnnotationFileAsString( ArrayDesign arrayDesign ) {
        Map<Long, String[]> results = new HashMap<Long, String[]>();
        File f = new File( ANNOT_DATA_DIR + mungeFileName( arrayDesign.getShortName() ) + STANDARD_FILE_SUFFIX
                + ANNOTATION_FILE_SUFFIX );
        if ( !f.canRead() ) {
            log.info( "Gene annotations are not available from " + f );
            return results;
        }

        Map<String, Long> probeNameToId = new HashMap<String, Long>();
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            results.put( cs.getId(), new String[3] );
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

                results.get( probeId )[0] = probeName; // Probe Name (redundant!)
                results.get( probeId )[1] = fields[1]; // Gene Symbol
                results.get( probeId )[2] = fields[2]; // Gene Name

                // fields[3] is the GO annotations, we skip that.

                if ( fields.length > 4 ) {
                    results.get( probeId )[3] = fields[4]; // Gemma Id
                }

                if ( fields.length > 5 ) {
                    results.get( probeId )[4] = fields[5]; // NCBI id.
                }

            }

            return results;
        } catch ( FileNotFoundException e ) {
            throw new RuntimeException( e );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @Autowired
    private Gene2GOAssociationService gene2GOAssociationService;

    @Autowired
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

    Transformer ncbiIdExtractor = new Transformer() {
        public Object transform( Object input ) {
            Gene gene = ( Gene ) input;
            return gene.getNcbiGeneId();
        }
    };

    Transformer idExtractor = new Transformer() {
        public Object transform( Object input ) {
            Gene gene = ( Gene ) input;
            return gene.getId();
        }
    };

    Transformer goTermExtractor = new Transformer() {
        public Object transform( Object input ) {
            return GeneOntologyService.asRegularGoId( ( ( OntologyTerm ) input ) );
        }
    };

    /**
     * Format details:
     * <p>
     * There is a one-line header. The columns are:
     * <ol>
     * <li>Probe name
     * <li>Gene symbol. Genes located at different genome locations are delimited by "|"; multiple genes at the same
     * location are delimited by ",". Both can happen simultaneously.
     * <li>Gene name, delimited as for the symbol except '$' is used instead of ','.
     * <li>GO terms, delimited by '|'; multiple genes are not handled specially (for compatibility with ermineJ)
     * </ol>
     * 
     * @param writer
     * @param genesWithSpecificity map of cs ->* physical location ->* ( blat association ->* gene product -> gene)
     * @param ty whether to include parents (OutputType.LONG); only use biological process (OutputType.BIOPROCESS) or
     *        'standard' output (OutputType.SHORT).
     * @param knownGenesOnly Whether output should include PARs and predicted genes. If true, they will be excluded.
     * @return number processed.
     * @throws IOException
     */
    public int generateAnnotationFile( Writer writer,
            Map<CompositeSequence, Collection<BioSequence2GeneProduct>> genesWithSpecificity, OutputType ty,
            boolean knownGenesOnly ) throws IOException {

        int compositeSequencesProcessed = 0;

        for ( CompositeSequence cs : genesWithSpecificity.keySet() ) {

            // Collection<Gene> genes = compositeSequenceService.getGenes( sequence );
            Collection<BioSequence2GeneProduct> geneclusters = genesWithSpecificity.get( cs );

            if ( geneclusters.isEmpty() ) {
                writeAnnotationLine( writer, cs.getName(), "", "", null, "", "" );
                continue;
            }

            Set<OntologyTerm> goTerms = new LinkedHashSet<OntologyTerm>();
            Set<String> genes = new LinkedHashSet<String>();
            Set<String> geneDescriptions = new LinkedHashSet<String>();
            Set<String> geneIds = new LinkedHashSet<String>();
            Set<String> ncbiIds = new LinkedHashSet<String>();

            for ( BioSequence2GeneProduct bioSequence2GeneProduct : geneclusters ) {

                Collection<Gene> retained = new HashSet<Gene>();

                Collection<OntologyTerm> clusterGoTerms = new HashSet<OntologyTerm>();

                Gene g = bioSequence2GeneProduct.getGeneProduct().getGene();
                if ( knownGenesOnly && ( g instanceof PredictedGene || g instanceof ProbeAlignedRegion ) ) {
                    continue;
                }

                if ( log.isDebugEnabled() )
                    log.debug( "Adding gene: " + g.getOfficialSymbol() + " of type: " + g.getClass() );

                retained.add( g );

                if ( retained.size() == 0 ) continue;

                List<Gene> retainedGenes = new ArrayList<Gene>( retained );
                for ( Gene gene : retainedGenes ) {
                    clusterGoTerms.addAll( getGoTerms( gene, ty ) );
                }

                // This will break if gene symbols contain ",".
                genes.add( StringUtils.join( new TransformIterator( retained.iterator(), officialSymbolExtractor ), "," ) );

                // This breaks if the descriptions contain "$".
                geneDescriptions.add( StringUtils.join( new TransformIterator( retained.iterator(),
                        descriptionExtractor ), "$" ) );

                geneIds.add( StringUtils.join( new TransformIterator( retained.iterator(), idExtractor ), "," ) );

                ncbiIds.add( StringUtils.join( new TransformIterator( retained.iterator(), ncbiIdExtractor ), "," ) );

                goTerms.addAll( clusterGoTerms );
            }

            String geneString = StringUtils.join( genes, "|" );
            String geneDescriptionString = StringUtils.join( geneDescriptions, "|" );
            String geneIdsString = StringUtils.join( geneIds, "|" );
            String ncbiIdsString = StringUtils.join( ncbiIds, "|" );
            writeAnnotationLine( writer, cs.getName(), geneString, geneDescriptionString, goTerms, geneIdsString,
                    ncbiIdsString );

            if ( ++compositeSequencesProcessed % 2000 == 0 && log.isInfoEnabled() ) {
                log.info( "Processed " + compositeSequencesProcessed + "/" + genesWithSpecificity.size()
                        + " compositeSequences " );
            }

        }
        writer.close();
        return compositeSequencesProcessed;
    }

    /**
     * Generate an annotation for a list of genes, instead of probes. The second column will contain the NCBI id, if
     * available.
     * 
     * @param writer
     * @param genes
     * @param type
     * @return
     */
    public int generateAnnotationFile( Writer writer, Collection<Gene> genes, OutputType type ) {
        for ( Gene gene : genes ) {
            Collection<OntologyTerm> ontos = getGoTerms( gene, type );

            Integer ncbiId = gene.getNcbiGeneId();
            String ncbiIds = ncbiId == null ? "" : ncbiId.toString();
            String geneString = gene.getOfficialSymbol();
            String geneDescriptionString = gene.getOfficialName();
            try {
                writeAnnotationLine( writer, geneString, ncbiIds, geneDescriptionString, ontos,
                        gene.getId().toString(), gene.getNcbiGeneId().toString() );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        }
        return genes.size();
    }

    /**
     * Remove file separators (e.g., "/") from the file names.
     * 
     * @param fileBaseName
     * @return
     */
    public static String mungeFileName( String fileBaseName ) {
        if ( fileBaseName == null ) {
            return null;
        }
        return fileBaseName.replaceAll( Pattern.quote( File.separator ), "_" );
    }

    /**
     * Opens a file for writing and adds the header.
     * 
     * @param arrayDesign
     * @param fileBaseName if Null, output will be written to standard output.
     * @param overWrite clobber existing file. Otherwise returns null.
     * @return writer to use
     * @throws IOException
     */
    public Writer initOutputFile( ArrayDesign arrayDesign, String fileBaseName, boolean overWrite ) throws IOException {

        Writer writer;
        if ( StringUtils.isBlank( fileBaseName ) ) {
            log.info( "Output to stdout" );
            writer = new PrintWriter( System.out );
        } else {

            File f = getFileName( fileBaseName );

            if ( f.exists() ) {
                if ( overWrite ) {
                    log.warn( "Will overwrite existing file " + f );
                    f.delete();
                } else {
                    return null;
                }
            } else {
                log.info( "Creating new annotation file " + f + " \n" );
            }

            File parentDir = f.getParentFile();
            if ( !parentDir.exists() ) parentDir.mkdirs();
            writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( f ) ) );
        }
        StringBuilder buf = new StringBuilder();
        buf.append( "# Annotation file generated by Gemma\n" );
        buf.append( "# Generated " + DateUtil.convertDateToString( new Date() ) + "\n" );
        buf.append( "# If you use this file for your research, please cite the Gemma web site.\n" );
        buf.append( "# Gemma link for this platform: http://www.chibi.ubc.ca/Gemma/arrays/showArrayDesign.html?id="
                + arrayDesign.getId() + "\n" );
        buf.append( "# " + arrayDesign.getShortName() + "  " + arrayDesign.getName() );
        buf.append( "# " + arrayDesign.getPrimaryTaxon().getScientificName() );
        writer.write( buf.toString() );
        writer.write( "ProbeName\tGeneSymbols\tGeneNames\tGOTerms\tGemmaIDs\tNCBIids\n" );

        return writer;
    }

    /**
     * @param mungedFileName
     * @return
     */
    public static File getFileName( String fileBaseName ) {
        String mungedFileName = mungeFileName( fileBaseName );
        return new File( ANNOT_DATA_DIR + mungedFileName + ANNOTATION_FILE_SUFFIX );
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
    private Collection<OntologyTerm> getGoTerms( Gene gene, OutputType ty ) {

        Collection<VocabCharacteristic> ontos = new HashSet<VocabCharacteristic>(
                gene2GOAssociationService.findByGene( gene ) );

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
            Collection<OntologyTerm> goTerms, String geneIds, String ncbiIds ) throws IOException {

        if ( log.isDebugEnabled() ) log.debug( "Generating line for annotation file  \n" );

        if ( gene == null ) gene = "";

        String formattedDescription = description;
        if ( description == null ) {
            formattedDescription = "";
        } else {
            // Try to help ensure file is readable by third-party programs like R. See bug 1851
            formattedDescription = formattedDescription.replaceAll( "#", "_" );
        }

        writer.write( probeId + "\t" + gene + "\t" + formattedDescription + "\t" );

        if ( ( goTerms == null ) || goTerms.isEmpty() ) {
            writer.write( "\n" );
            return;
        }

        String goterms = StringUtils.join( new TransformIterator( goTerms.iterator(), goTermExtractor ), "|" );
        writer.write( goterms );

        writer.write( "\t" + geneIds + "\t" + ncbiIds );

        writer.write( "\n" );
        writer.flush();

    }

    public enum OutputType {
        SHORT, LONG, BIOPROCESS
    }

}
