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
package ubic.gemma.core.analysis.service;

import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.iterators.TransformIterator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.util.DateUtil;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.ontology.providers.GeneOntologyServiceImpl;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.association.Gene2GOAssociationService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.util.EntityUtils;
import ubic.gemma.persistence.util.Settings;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * @author Paul
 * @see    ArrayDesignAnnotationService
 */
@Component
public class ArrayDesignAnnotationServiceImpl implements ArrayDesignAnnotationService {

    private static final String COMMENT_CHARACTER = "#";
    private static final Log log = LogFactory.getLog( ArrayDesignAnnotationServiceImpl.class.getName() );

    public static File getFileName( String fileBaseName ) {
        String mungedFileName = ArrayDesignAnnotationServiceImpl.mungeFileName( fileBaseName );
        return new File( ArrayDesignAnnotationService.ANNOT_DATA_DIR + mungedFileName
                + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX );
    }

    /**
     * Remove file separators (e.g., "/") from the file names.
     *
     * @param  fileBaseName file base name
     * @return              munged name
     */
    public static String mungeFileName( String fileBaseName ) {
        if ( fileBaseName == null ) {
            return null;
        }
        return fileBaseName.replaceAll( Pattern.quote( File.separator ), "_" );
    }

    /**
     * @param  arrayDesign array design
     * @return             Map of composite sequence ids and transient (incomplete) genes. The genes only have the
     *                     symbol filled in.
     */
    public static Map<Long, Collection<Gene>> readAnnotationFile( ArrayDesign arrayDesign ) {
        Map<Long, Collection<Gene>> results = new HashMap<>();
        File f = new File( ArrayDesignAnnotationService.ANNOT_DATA_DIR + ArrayDesignAnnotationServiceImpl
                .mungeFileName( arrayDesign.getShortName() ) + ArrayDesignAnnotationService.STANDARD_FILE_SUFFIX
                + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX );
        if ( !f.canRead() ) {
            ArrayDesignAnnotationServiceImpl.log.info( "Gene annotations are not available from " + f );
            return results;
        }

        Map<String, Long> probeNameToId = new HashMap<>();
        ArrayDesignAnnotationServiceImpl.populateProbeNameToIdMap( arrayDesign, results, probeNameToId );
        ArrayDesignAnnotationServiceImpl.log.info( "Reading annotations from: " + f );
        try (InputStream is = FileTools.getInputStreamFromPlainOrCompressedFile( f.getAbsolutePath() )) {
            return ArrayDesignAnnotationServiceImpl.parseAnnotationFile( results, is, probeNameToId );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * This tries to read one of the annotation files (noparents, bioprocess or regular) to get the gene information -
     * GO annotations are not part of the result.
     *
     * @param  arrayDesign array design
     * @return             Map of composite sequence ids to an array of delimited strings: [probe name,genes symbol,
     *                     gene Name,
     *                     gemma gene id, ncbi id] for a given probe id. format of string is geneSymbol then geneNames
     *                     same as found
     *                     in annotation file.
     */
    public static Map<Long, String[]> readAnnotationFileAsString( ArrayDesign arrayDesign ) {
        Map<Long, String[]> results = new HashMap<>();
        File f = new File( ArrayDesignAnnotationService.ANNOT_DATA_DIR + ArrayDesignAnnotationServiceImpl
                .mungeFileName( arrayDesign.getShortName() ) + ArrayDesignAnnotationService.STANDARD_FILE_SUFFIX
                + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX );
        if ( !f.canRead() ) {
            /*
             * Look for more files.
             */
            f = new File( ArrayDesignAnnotationService.ANNOT_DATA_DIR + ArrayDesignAnnotationServiceImpl
                    .mungeFileName( arrayDesign.getShortName() ) + ArrayDesignAnnotationService.NO_PARENTS_FILE_SUFFIX
                    + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX );

            if ( !f.canRead() ) {
                f = new File( ArrayDesignAnnotationService.ANNOT_DATA_DIR + ArrayDesignAnnotationServiceImpl
                        .mungeFileName( arrayDesign.getShortName() )
                        + ArrayDesignAnnotationService.BIO_PROCESS_FILE_SUFFIX
                        + ArrayDesignAnnotationService.ANNOTATION_FILE_SUFFIX );
            }

            if ( !f.canRead() ) {
                ArrayDesignAnnotationServiceImpl.log
                        .info( "Gene annotations are not available in " + ArrayDesignAnnotationService.ANNOT_DATA_DIR );
                return results;
            }
        }

        Map<String, Long> probeNameToId = new HashMap<>();

        int FIELDS_PER_GENE = 5; // used to be 3, now is 5;

        boolean warned = false;
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            results.put( cs.getId(), new String[FIELDS_PER_GENE] );
            if ( probeNameToId.containsKey( cs.getName() ) && !warned ) {
                ArrayDesignAnnotationServiceImpl.log
                        .warn( "Duplicate probe name: " + cs.getName() + " for " + arrayDesign + " (further warnings suppressed)" );
                warned = true;
            }
            probeNameToId.put( cs.getName(), cs.getId() );
        }

        try (InputStream is = FileTools.getInputStreamFromPlainOrCompressedFile( f.getAbsolutePath() );
                BufferedReader br = new BufferedReader( new InputStreamReader( is ) )) {
            ArrayDesignAnnotationServiceImpl.log.info( "Reading annotations from: " + f );

            String line;

            while ( ( line = br.readLine() ) != null ) {
                if ( StringUtils.isBlank( line ) || line
                        .startsWith( ArrayDesignAnnotationServiceImpl.COMMENT_CHARACTER ) ) {
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

    private static Map<Long, Collection<Gene>> parseAnnotationFile( Map<Long, Collection<Gene>> results, InputStream is,
            Map<String, Long> probeNameToId ) {
        try {

            BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
            String line;

            while ( ( line = br.readLine() ) != null ) {
                if ( StringUtils.isBlank( line ) || line
                        .startsWith( ArrayDesignAnnotationServiceImpl.COMMENT_CHARACTER ) ) {
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
                    ArrayDesignAnnotationServiceImpl.log
                            .warn( "Annotation file format error: Unequal number of gene symbols and names for probe="
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

    private static void populateProbeNameToIdMap( ArrayDesign arrayDesign, Map<Long, Collection<Gene>> results,
            Map<String, Long> probeNameToId ) {
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            results.put( cs.getId(), new HashSet<Gene>() );
            if ( probeNameToId.containsKey( cs.getName() ) ) {
                ArrayDesignAnnotationServiceImpl.log.warn( "Duplicate probe name: " + cs.getName() );
            }
            probeNameToId.put( cs.getName(), cs.getId() );
        }
    }

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private CompositeSequenceService compositeSequenceService;

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    @Autowired
    private Gene2GOAssociationService gene2GOAssociationService;

    @Autowired
    private GeneOntologyService goService;

    private final Transformer goTermExtractor = new Transformer() {
        @Override
        public Object transform( Object input ) {
            return GeneOntologyServiceImpl.asRegularGoId( ( ( OntologyTerm ) input ) );
        }
    };

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.core.analysis.service.ArrayDesignAnnotationService#create(ubic.gemma.model.expression.arrayDesign.
     * ArrayDesign, java.lang.Boolean)
     */
    @Override
    public void create( ArrayDesign inputAd, Boolean overWrite ) throws IOException {

        if ( !goService.isReady() ) {
            throw new IllegalStateException( "GO was not loaded" );
        }

        ArrayDesign ad = arrayDesignService.thaw( inputAd );

        log.info( "== Creating annotation files for: " + ad );

        String shortFileBaseName = ArrayDesignAnnotationServiceImpl.mungeFileName( ad.getShortName() )
                + ArrayDesignAnnotationService.NO_PARENTS_FILE_SUFFIX;
        File sf = ArrayDesignAnnotationServiceImpl.getFileName( shortFileBaseName );
        String bioFileBaseName = ArrayDesignAnnotationServiceImpl.mungeFileName( ad.getShortName() )
                + ArrayDesignAnnotationService.BIO_PROCESS_FILE_SUFFIX;
        File bf = ArrayDesignAnnotationServiceImpl.getFileName( bioFileBaseName );
        String allParFileBaseName = ArrayDesignAnnotationServiceImpl.mungeFileName( ad.getShortName() )
                + ArrayDesignAnnotationService.STANDARD_FILE_SUFFIX;
        File af = ArrayDesignAnnotationServiceImpl.getFileName( allParFileBaseName );

        if ( !overWrite && sf.exists() && bf.exists() && af.exists() ) {
            log.info( "Files exist already, will not overwrite (use overWrite option to override)" );
            return;
        }

        Collection<CompositeSequence> compositeSequences = ad.getCompositeSequences();
        log.info( "Starting getting probe specificity" );

        Map<CompositeSequence, Collection<BioSequence2GeneProduct>> genesWithSpecificity = compositeSequenceService
                .getGenesWithSpecificity( compositeSequences );

        log.info( "Done getting probe specificity" );

        boolean hasAtLeastOneGene = false;
        for ( CompositeSequence c : genesWithSpecificity.keySet() ) {
            if ( genesWithSpecificity.get( c ).isEmpty() ) {
                continue;
            }
            hasAtLeastOneGene = true;
            break;
        }

        if ( !hasAtLeastOneGene ) {
            log.warn( "No genes: " + ad + ", skipping" );
            return;
        }

        if ( overWrite || !sf.exists() ) {
            this.processCompositeSequences( ad, shortFileBaseName, OutputType.SHORT, genesWithSpecificity, overWrite );

            /*
             * Delete the data files for experiments that used this platform, since they have the old annotations in
             * them (or no annotations)
             */
            Collection<ExpressionExperiment> ees = arrayDesignService.getExpressionExperiments( ad );
            if ( !ees.isEmpty() ) log.info( "Deleting data files for " + ees.size() + " experiments which use " + ad.getShortName()
                    + ", that may have outdated annotations" );
            for ( ExpressionExperiment ee : ees ) {
                this.expressionDataFileService.deleteAllFiles( ee );
            }

        } else {
            log.info( sf + " exists, will not overwrite" );
        }

        if ( overWrite || !bf.exists() ) {
            this.processCompositeSequences( ad, bioFileBaseName, OutputType.BIOPROCESS, genesWithSpecificity, overWrite );
        } else {
            log.info( bf + " exists, will not overwrite" );
        }

        if ( overWrite || !af.exists() ) {
            this.processCompositeSequences( ad, allParFileBaseName, OutputType.LONG, genesWithSpecificity, overWrite );
        } else {
            log.info( af + " exists, will not overwrite" );
        }
    }

    @Override
    public void deleteExistingFiles( ArrayDesign ad ) {
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
        ArrayDesignAnnotationServiceImpl.log.info( numFilesDeleted + " old annotation files deleted" );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.analysis.service.ArrayDesignAnnotationService#generateAnnotationFile(java.io.Writer,
     * java.util.Collection)
     */
    @Override
    public int generateAnnotationFile( Writer writer, Collection<Gene> genes ) {

        Map<Gene, Collection<Characteristic>> goMappings = gene2GOAssociationService.findByGenes( genes );

        for ( Gene gene : genes ) {
            Collection<OntologyTerm> ontologyTerms = this.getGoTerms( goMappings.get( gene ), OutputType.SHORT );

            Integer ncbiId = gene.getNcbiGeneId();
            String ncbiIds = ncbiId == null ? "" : ncbiId.toString();
            String geneString = gene.getOfficialSymbol();
            String geneDescriptionString = gene.getOfficialName();
            try {
                Long id = gene.getId();
                this.writeAnnotationLine( writer, geneString, ncbiIds, geneDescriptionString, ontologyTerms,
                        id.toString(), ncbiIds );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        }
        return genes.size();
    }

    private int generateAnnotationFile( Writer writer,
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

        Map<Gene, Collection<Characteristic>> goMappings = this.getGOMappings( genesWithSpecificity );

        for ( CompositeSequence cs : genesWithSpecificity.keySet() ) {

            Collection<BioSequence2GeneProduct> geneclusters = genesWithSpecificity.get( cs );

            if ( ++compositeSequencesProcessed % 2000 == 0 && ArrayDesignAnnotationServiceImpl.log.isInfoEnabled() ) {
                ArrayDesignAnnotationServiceImpl.log
                        .info( "Processed " + compositeSequencesProcessed + "/" + genesWithSpecificity.size()
                                + " compositeSequences " + empty + " empty; " + simple + " simple; " + complex
                                + " complex;" );
            }

            if ( geneclusters.isEmpty() ) {
                this.writeAnnotationLine( writer, cs.getName(), "", "", null, "", "" );
                empty++;
                continue;
            }

            if ( geneclusters.size() == 1 ) {
                // common case, do it quickly.
                BioSequence2GeneProduct b2g = geneclusters.iterator().next();
                Gene g = b2g.getGeneProduct().getGene();
                goTerms = this.getGoTerms( goMappings.get( g ), ty );
                String gemmaId = g.getId() == null ? "" : g.getId().toString();
                String ncbiId = g.getNcbiGeneId() == null ? "" : g.getNcbiGeneId().toString();
                this.writeAnnotationLine( writer, cs.getName(), g.getOfficialSymbol(), g.getOfficialName(), goTerms,
                        gemmaId, ncbiId );
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
                goTerms.addAll( this.getGoTerms( goMappings.get( g ), ty ) );

            }

            String geneString = StringUtils.join( genes, "|" );
            String geneDescriptionString = StringUtils.join( geneDescriptions, "|" );
            String geneIdsString = StringUtils.join( geneIds, "|" );
            String ncbiIdsString = StringUtils.join( ncbiIds, "|" );
            this.writeAnnotationLine( writer, cs.getName(), geneString, geneDescriptionString, goTerms, geneIdsString,
                    ncbiIdsString );
            complex++;

        }
        writer.close();

        return compositeSequencesProcessed;
    }

    private Map<Gene, Collection<Characteristic>> getGOMappings(
            Map<CompositeSequence, Collection<BioSequence2GeneProduct>> genesWithSpecificity ) {
        ArrayDesignAnnotationServiceImpl.log.info( "Fetching GO mappings" );
        Collection<Gene> allGenes = new HashSet<>();
        for ( CompositeSequence cs : genesWithSpecificity.keySet() ) {

            Collection<BioSequence2GeneProduct> geneclusters = genesWithSpecificity.get( cs );
            for ( BioSequence2GeneProduct bioSequence2GeneProduct : geneclusters ) {

                Gene g = bioSequence2GeneProduct.getGeneProduct().getGene();
                allGenes.add( g );
            }
        }
        Map<Gene, Collection<Characteristic>> goMappings = gene2GOAssociationService.findByGenes( allGenes );
        ArrayDesignAnnotationServiceImpl.log.info( "Got GO mappings for " + goMappings.size() + " genes" );
        return goMappings;
    }

    /**
     * @param  ty Configures which GO terms to return: With all parents, biological process only, or direct annotations
     *            only.
     * @return    the goTerms for a given gene, as configured
     */
    private Collection<OntologyTerm> getGoTerms( Collection<Characteristic> ontologyTerms, OutputType ty ) {

        Collection<OntologyTerm> results = new HashSet<>();
        if ( ontologyTerms == null || ontologyTerms.size() == 0 )
            return results;

        for ( Characteristic vc : ontologyTerms ) {
            results.add( goService.getTermForId( vc.getValue() ) );
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

    private Writer initOutputFile( ArrayDesign arrayDesign, String fileBaseName, boolean overWrite ) throws IOException {

        Writer writer;
        if ( StringUtils.isBlank( fileBaseName ) ) {
            ArrayDesignAnnotationServiceImpl.log.info( "Output to stdout" );
            writer = new PrintWriter( System.out );
        } else {

            File f = ArrayDesignAnnotationServiceImpl.getFileName( fileBaseName );

            if ( f.exists() ) {
                if ( overWrite ) {
                    ArrayDesignAnnotationServiceImpl.log.warn( "Will overwrite existing file " + f );
                    EntityUtils.deleteFile( f );
                } else {
                    return null;
                }
            } else {
                ArrayDesignAnnotationServiceImpl.log.info( "Creating new annotation file " + f + " \n" );
            }

            // ensure the parent directory exists
            FileUtils.forceMkdirParent( f );

            writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( f ) ) );
        }
        StringWriter buf = new StringWriter();
        buf.append( "# Annotation file generated by Gemma\n" );
        buf.append( "# Generated " ).append( DateUtil.convertDateToString( new Date() ) ).append( "\n" );
        buf.append( ExpressionDataFileService.DISCLAIMER );
        buf.append( "# Gemma link for this platform: " ).append( Settings.getBaseUrl() )
                .append( " arrays/showArrayDesign.html?id=" ).append( arrayDesign.getId().toString() ).append( "\n" );
        buf.append( "# " ).append( arrayDesign.getShortName() ).append( "  " ).append( arrayDesign.getName() )
                .append( "\n" );
        buf.append( "# " ).append( arrayDesign.getPrimaryTaxon().getScientificName() ).append( "\n" );

        writer.write( buf.toString() );
        writer.write( "ProbeName\tGeneSymbols\tGeneNames\tGOTerms\tGemmaIDs\tNCBIids\n" );

        return writer;
    }

    /**
     * 
     * @param  arrayDesign
     * @param  fileBaseName
     * @param  outputType
     * @param  genesWithSpecificity
     * @param  overWrite
     * @throws IOException
     */
    private void processCompositeSequences( ArrayDesign arrayDesign, String fileBaseName, OutputType outputType,
            Map<CompositeSequence, Collection<BioSequence2GeneProduct>> genesWithSpecificity, Boolean overWrite ) throws IOException {

        if ( genesWithSpecificity.size() == 0 ) {
            log.info( "No sequence information for " + arrayDesign + ", skipping" );
            return;
        }

        try (Writer writer = initOutputFile( arrayDesign, fileBaseName, overWrite )) {

            // if no writer then we should abort (this could happen in case where we don't want to overwrite files)
            if ( writer == null ) {
                log.info( arrayDesign.getName() + " annotation file already exits.  Skipping. " );
                return;
            }

            log.info( arrayDesign.getName() + " has " + genesWithSpecificity.size() + " composite sequences" );

            generateAnnotationFile( writer, genesWithSpecificity, outputType );

            log.info( "Finished processing platform: " + arrayDesign.getName() );

        }
    }

    /**
     * Adds one line at a time to the annotation file.
     */
    private void writeAnnotationLine( Writer writer, String probeId, String gene, String description,
            Collection<OntologyTerm> goTerms, String geneIds, String ncbiIds ) throws IOException {

        if ( ArrayDesignAnnotationServiceImpl.log.isDebugEnabled() )
            ArrayDesignAnnotationServiceImpl.log.debug( "Generating line for annotation file  \n" );

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

}
