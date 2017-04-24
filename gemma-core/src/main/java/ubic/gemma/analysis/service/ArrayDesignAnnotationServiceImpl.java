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

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.iterators.TransformIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.util.DateUtil;
import ubic.basecode.util.FileTools;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.ontology.providers.GeneOntologyService;
import ubic.gemma.ontology.providers.GeneOntologyServiceImpl;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * @author Paul
 * @see ArrayDesignAnnotationService
 */
@Component
public class ArrayDesignAnnotationServiceImpl implements ArrayDesignAnnotationService {

    private static final String COMMENT_CHARACTER = "#";
    private static final Log log = LogFactory.getLog( ArrayDesignAnnotationServiceImpl.class.getName() );

    private final Transformer goTermExtractor = new Transformer() {
        @Override
        public Object transform( Object input ) {
            return GeneOntologyServiceImpl.asRegularGoId( ( ( OntologyTerm ) input ) );
        }
    };

    @Autowired
    private Gene2GOAssociationService gene2GOAssociationService;
    @Autowired
    private GeneOntologyService goService;

    /* ********************************
     * Public static methods
     * ********************************/

    public static File getFileName( String fileBaseName ) {
        String mungedFileName = mungeFileName( fileBaseName );
        return new File( ANNOT_DATA_DIR + mungedFileName + ANNOTATION_FILE_SUFFIX );
    }

    /**
     * Remove file separators (e.g., "/") from the file names.
     */
    public static String mungeFileName( String fileBaseName ) {
        if ( fileBaseName == null ) {
            return null;
        }
        return fileBaseName.replaceAll( Pattern.quote( File.separator ), "_" );
    }

    /**
     * @return Map of composite sequence ids and transient (incomplete) genes. The genes only have the symbol filled in.
     */
    public static Map<Long, Collection<Gene>> readAnnotationFile( ArrayDesign arrayDesign ) {
        Map<Long, Collection<Gene>> results = new HashMap<>();
        File f = new File( ANNOT_DATA_DIR + mungeFileName( arrayDesign.getShortName() ) + STANDARD_FILE_SUFFIX
                + ANNOTATION_FILE_SUFFIX );
        if ( !f.canRead() ) {
            log.info( "Gene annotations are not available from " + f );
            return results;
        }

        Map<String, Long> probeNameToId = new HashMap<>();
        populateProbeNameToIdMap( arrayDesign, results, probeNameToId );
        log.info( "Reading annotations from: " + f );
        try (InputStream is = FileTools.getInputStreamFromPlainOrCompressedFile( f.getAbsolutePath() )) {
            return parseAnnotationFile( results, is, probeNameToId );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @return Map of composite sequence ids to an array of delimited strings: [probe name,genes symbol, gene Name,
     * gemma gene id, ncbi id] for a given probe id. format of string is geneSymbol then geneNames same as found
     * in annotation file
     */
    public static Map<Long, String[]> readAnnotationFileAsString( ArrayDesign arrayDesign ) {
        Map<Long, String[]> results = new HashMap<>();
        File f = new File( ANNOT_DATA_DIR + mungeFileName( arrayDesign.getShortName() ) + STANDARD_FILE_SUFFIX
                + ANNOTATION_FILE_SUFFIX );
        if ( !f.canRead() ) {
            log.info( "Gene annotations are not available from " + f );
            return results;
        }

        Map<String, Long> probeNameToId = new HashMap<>();

        int FIELDS_PER_GENE = 5; // used to be 3, now is 5;

        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            results.put( cs.getId(), new String[FIELDS_PER_GENE] );
            if ( probeNameToId.containsKey( cs.getName() ) ) {
                log.warn( "Duplicate probe name: " + cs.getName() );
            }
            probeNameToId.put( cs.getName(), cs.getId() );
        }

        try (InputStream is = FileTools.getInputStreamFromPlainOrCompressedFile( f.getAbsolutePath() );
                BufferedReader br = new BufferedReader( new InputStreamReader( is ) )) {
            log.info( "Reading annotations from: " + f );

            String line;

            while ( ( line = br.readLine() ) != null ) {
                if ( StringUtils.isBlank( line ) || line.startsWith( COMMENT_CHARACTER ) ) {
                    continue;
                }
                String[] fields = StringUtils.splitPreserveAllTokens( line, '\t' );

                if ( fields.length < 3 )
                    continue; // means there are no gene annotations.

                String probeName = fields[0];

                if ( !probeNameToId.containsKey( probeName ) )
                    continue;
                Long probeId = probeNameToId.get( probeName );

                results.get( probeId )[0] = probeName; // Probe Name (redundant!)
                results.get( probeId )[1] = fields[1]; // Gene Symbol(s)
                results.get( probeId )[2] = fields[2]; // Gene Name

                // fields[3] is the GO annotations, we skip that.

                if ( fields.length > 4 ) {
                    results.get( probeId )[3] = fields[4]; // Gemma Id
                }

                if ( fields.length > 5 ) {
                    results.get( probeId )[4] = fields[5]; // NCBI id.
                }

            }

            is.close();

            return results;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param is InputStream with the annotations
     * @return Map of composite sequence ids and transient (incomplete) genes. The genes only have the symbol filled in.
     */
    @SuppressWarnings("unused") // Can be used through CLI tools
    public static Map<Long, Collection<Gene>> readAnnotations( ArrayDesign arrayDesign, InputStream is ) {
        Map<Long, Collection<Gene>> results = new HashMap<>();
        Map<String, Long> probeNameToId = new HashMap<>();
        populateProbeNameToIdMap( arrayDesign, results, probeNameToId );
        return parseAnnotationFile( results, is, probeNameToId );
    }

    /* ********************************
     * Private static methods
     * ********************************/

    private static void populateProbeNameToIdMap( ArrayDesign arrayDesign, Map<Long, Collection<Gene>> results,
            Map<String, Long> probeNameToId ) {
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            results.put( cs.getId(), new HashSet<Gene>() );
            if ( probeNameToId.containsKey( cs.getName() ) ) {
                log.warn( "Duplicate probe name: " + cs.getName() );
            }
            probeNameToId.put( cs.getName(), cs.getId() );
        }
    }

    private static Map<Long, Collection<Gene>> parseAnnotationFile( Map<Long, Collection<Gene>> results, InputStream is,
            Map<String, Long> probeNameToId ) {
        try {

            BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
            String line;

            while ( ( line = br.readLine() ) != null ) {
                if ( StringUtils.isBlank( line ) || line.startsWith( COMMENT_CHARACTER ) ) {
                    continue;
                }
                String[] fields = StringUtils.splitPreserveAllTokens( line, '\t' );

                if ( fields.length < 3 )
                    continue; // means there are no gene annotations.

                String probeName = fields[0];

                if ( !probeNameToId.containsKey( probeName ) )
                    continue;
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

                    String[] symbolsB = StringUtils.split( symbol, ',' );
                    String[] namesB = StringUtils.split( name, '$' );

                    for ( int j = 0; j < symbolsB.length; j++ ) {

                        String s = symbolsB[j];

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

                        if ( namesB.length >= j + 1 ) {
                            String n = namesB[j];
                            g.setName( n );
                        }

                        results.get( probeId ).add( g );
                    }
                }
            }

            return results;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /* ********************************
     * Public methods
     * ********************************/

    @Override
    public void deleteExistingFiles( ArrayDesign ad ) throws IOException {
        String shortFileBaseName = ArrayDesignAnnotationServiceImpl.mungeFileName( ad.getShortName() )
                + ArrayDesignAnnotationService.NO_PARENTS_FILE_SUFFIX;
        File sf = ArrayDesignAnnotationServiceImpl.getFileName( shortFileBaseName );
        String biocFileBaseName = ArrayDesignAnnotationServiceImpl.mungeFileName( ad.getShortName() )
                + ArrayDesignAnnotationService.BIO_PROCESS_FILE_SUFFIX;
        File bf = ArrayDesignAnnotationServiceImpl.getFileName( biocFileBaseName );
        String allparFileBaseName = ArrayDesignAnnotationServiceImpl.mungeFileName( ad.getShortName() )
                + ArrayDesignAnnotationService.STANDARD_FILE_SUFFIX;
        File af = ArrayDesignAnnotationServiceImpl.getFileName( allparFileBaseName );

        int numFilesDeleted = 0;
        if ( sf.canWrite() && sf.delete() ) {
            numFilesDeleted++;
        }
        if ( bf.canWrite() && bf.delete() ) {
            numFilesDeleted++;

        }
        if ( af.canWrite() && af.delete() ) {
            numFilesDeleted++;

        }
        log.info( numFilesDeleted + " old annotation files deleted" );

    }

    @Override
    public int generateAnnotationFile( Writer writer, Collection<Gene> genes, OutputType type ) {

        Map<Gene, Collection<VocabCharacteristic>> goMappings = gene2GOAssociationService.findByGenes( genes );

        for ( Gene gene : genes ) {
            Collection<OntologyTerm> ontologyTerms = getGoTerms( goMappings.get( gene ), type );

            Integer ncbiId = gene.getNcbiGeneId();
            String ncbiIds = ncbiId == null ? "" : ncbiId.toString();
            String geneString = gene.getOfficialSymbol();
            String geneDescriptionString = gene.getOfficialName();
            try {
                Long id = gene.getId();
                writeAnnotationLine( writer, geneString, ncbiIds, geneDescriptionString, ontologyTerms, id.toString(),
                        ncbiIds );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        }
        return genes.size();
    }

    @Override
    public int generateAnnotationFile( Writer writer,
            Map<CompositeSequence, Collection<BioSequence2GeneProduct>> genesWithSpecificity, OutputType ty )
            throws IOException {

        int compositeSequencesProcessed = 0;
        int simple = 0;
        int empty = 0;
        int complex = 0;
        // we used LinkedHasSets to keep everything in a predictable order - this is important for the gene symbols,
        // descriptions and NCBIIds (but not important for GO terms). When a probe maps to multiple genes, we list those
        // three items for the genes in the same order. There is a feature request to make
        // the order deterministic (i.e.,lexicographic sort), this could be done by using little gene objects or whatever.
        Collection<OntologyTerm> goTerms = new LinkedHashSet<>();
        Set<String> genes = new LinkedHashSet<>();
        Set<String> geneDescriptions = new LinkedHashSet<>();
        Set<String> geneIds = new LinkedHashSet<>();
        Set<String> ncbiIds = new LinkedHashSet<>();

        Map<Gene, Collection<VocabCharacteristic>> goMappings = getGOMappings( genesWithSpecificity );

        for ( CompositeSequence cs : genesWithSpecificity.keySet() ) {

            Collection<BioSequence2GeneProduct> geneclusters = genesWithSpecificity.get( cs );

            if ( ++compositeSequencesProcessed % 2000 == 0 && log.isInfoEnabled() ) {
                log.info( "Processed " + compositeSequencesProcessed + "/" + genesWithSpecificity.size()
                        + " compositeSequences " + empty + " empty; " + simple + " simple; " + complex + " complex;" );
            }

            if ( geneclusters.isEmpty() ) {
                writeAnnotationLine( writer, cs.getName(), "", "", null, "", "" );
                empty++;
                continue;
            }

            if ( geneclusters.size() == 1 ) {
                // common case, do it quickly.
                BioSequence2GeneProduct b2g = geneclusters.iterator().next();
                Gene g = b2g.getGeneProduct().getGene();
                goTerms = getGoTerms( goMappings.get( g ), ty );
                String gemmaId = g.getId() == null ? "" : g.getId().toString();
                String ncbiId = g.getNcbiGeneId() == null ? "" : g.getNcbiGeneId().toString();
                writeAnnotationLine( writer, cs.getName(), g.getOfficialSymbol(), g.getOfficialName(), goTerms, gemmaId,
                        ncbiId );
                simple++;
                continue;
            }

            goTerms.clear();
            genes.clear();
            geneDescriptions.clear();
            geneIds.clear();
            ncbiIds.clear();

            for ( BioSequence2GeneProduct bioSequence2GeneProduct : geneclusters ) {

                Gene g = bioSequence2GeneProduct.getGeneProduct().getGene();

                genes.add( g.getOfficialSymbol() );
                geneDescriptions.add( g.getOfficialName() );
                geneIds.add( g.getId().toString() );
                Integer ncbiGeneId = g.getNcbiGeneId();
                if ( ncbiGeneId != null ) {
                    ncbiIds.add( ncbiGeneId.toString() );
                }
                goTerms.addAll( getGoTerms( goMappings.get( g ), ty ) );

            }

            String geneString = StringUtils.join( genes, "|" );
            String geneDescriptionString = StringUtils.join( geneDescriptions, "|" );
            String geneIdsString = StringUtils.join( geneIds, "|" );
            String ncbiIdsString = StringUtils.join( ncbiIds, "|" );
            writeAnnotationLine( writer, cs.getName(), geneString, geneDescriptionString, goTerms, geneIdsString,
                    ncbiIdsString );
            complex++;

        }
        writer.close();
        return compositeSequencesProcessed;
    }

    @Override
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
            if ( !parentDir.exists() )
                parentDir.mkdirs();
            writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( f ) ) );
        }
        String buf =
                "# Annotation file generated by Gemma\n" + "# Generated " + DateUtil.convertDateToString( new Date() )
                        + "\n" + ExpressionDataFileService.DISCLAIMER
                        + "# Gemma link for this platform: http://www.chibi.ubc.ca/Gemma/arrays/showArrayDesign.html?id="
                        + arrayDesign.getId() + "\n" + "# " + arrayDesign.getShortName() + "  " + arrayDesign.getName()
                        + "\n" + "# " + arrayDesign.getPrimaryTaxon().getScientificName() + "\n";
        writer.write( buf );
        writer.write( "ProbeName\tGeneSymbols\tGeneNames\tGOTerms\tGemmaIDs\tNCBIids\n" );

        return writer;
    }

    /* ********************************
     * Private methods
     * ********************************/

    private Map<Gene, Collection<VocabCharacteristic>> getGOMappings(
            Map<CompositeSequence, Collection<BioSequence2GeneProduct>> genesWithSpecificity ) {
        log.info( "Fetching GO mappings" );
        Collection<Gene> allGenes = new HashSet<>();
        for ( CompositeSequence cs : genesWithSpecificity.keySet() ) {

            Collection<BioSequence2GeneProduct> geneclusters = genesWithSpecificity.get( cs );
            for ( BioSequence2GeneProduct bioSequence2GeneProduct : geneclusters ) {

                Gene g = bioSequence2GeneProduct.getGeneProduct().getGene();
                allGenes.add( g );
            }
        }
        Map<Gene, Collection<VocabCharacteristic>> goMappings = gene2GOAssociationService.findByGenes( allGenes );
        log.info( "Got GO mappings for " + goMappings.size() + " genes" );
        return goMappings;
    }

    /**
     * @param ty Configures which GO terms to return: With all parents, biological process only, or direct annotations
     *           only.
     * @return the goTerms for a given gene, as configured
     */
    private Collection<OntologyTerm> getGoTerms( Collection<VocabCharacteristic> ontologyTerms, OutputType ty ) {

        Collection<OntologyTerm> results = new HashSet<>();
        if ( ontologyTerms == null || ontologyTerms.size() == 0 )
            return results;

        for ( VocabCharacteristic vc : ontologyTerms ) {
            results.add( GeneOntologyServiceImpl.getTermForId( vc.getValue() ) );
        }

        if ( ty.equals( OutputType.SHORT ) )
            return results;

        if ( ty.equals( OutputType.LONG ) ) {
            Collection<OntologyTerm> oes = goService.getAllParents( results );
            results.addAll( oes );
        } else if ( ty.equals( OutputType.BIOPROCESS ) ) {
            Collection<OntologyTerm> toRemove = new HashSet<>();

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
     * Adds one line at a time to the annotation file.
     */
    private void writeAnnotationLine( Writer writer, String probeId, String gene, String description,
            Collection<OntologyTerm> goTerms, String geneIds, String ncbiIds ) throws IOException {

        if ( log.isDebugEnabled() )
            log.debug( "Generating line for annotation file  \n" );

        if ( gene == null )
            gene = "";

        String formattedDescription = description;
        if ( description == null ) {
            formattedDescription = "";
        } else {
            // Try to help ensure file is readable by third-party programs like R. See bug 1851
            formattedDescription = formattedDescription.replaceAll( "#", "_" );
        }

        writer.write( probeId + "\t" + gene + "\t" + formattedDescription + "\t" );

        if ( goTerms != null && !goTerms.isEmpty() ) {
            String terms = StringUtils.join( new TransformIterator( goTerms.iterator(), goTermExtractor ), "|" );
            writer.write( terms );
        }

        writer.write( "\t" + geneIds + "\t" + ncbiIds + "\n" );

    }

    public enum OutputType {
        BIOPROCESS, LONG, SHORT
    }

}
