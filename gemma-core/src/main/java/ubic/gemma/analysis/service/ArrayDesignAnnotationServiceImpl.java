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

/**
 * @see ArrayDesignAnnotationService
 * @author Paul
 * @version $Id$
 */
@Component
public class ArrayDesignAnnotationServiceImpl implements ArrayDesignAnnotationService {

    public enum OutputType {
        BIOPROCESS, LONG, SHORT
    }

    private static final String COMMENT_CHARACTER = "#";

    private static Log log = LogFactory.getLog( ArrayDesignAnnotationServiceImpl.class.getName() );

    /**
     * @param mungedFileName
     * @return
     */
    public static File getFileName( String fileBaseName ) {
        String mungedFileName = mungeFileName( fileBaseName );
        return new File( ANNOT_DATA_DIR + mungedFileName + ANNOTATION_FILE_SUFFIX );
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
        log.info( "Reading annotations from: " + f );
        try (InputStream is = FileTools.getInputStreamFromPlainOrCompressedFile( f.getAbsolutePath() );) {
            return parseAnnotationFile( results, is, probeNameToId );
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

        int FIELDS_PER_GENE = 5; // used to be 3, now is 5;

        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            results.put( cs.getId(), new String[FIELDS_PER_GENE] );
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

            is.close();

            return results;
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

    Transformer goTermExtractor = new Transformer() {
        @Override
        public Object transform( Object input ) {
            return GeneOntologyServiceImpl.asRegularGoId( ( ( OntologyTerm ) input ) );
        }
    };

    @Autowired
    private Gene2GOAssociationService gene2GOAssociationService;

    @Autowired
    private GeneOntologyService goService;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.service.ArrayDesignAnnotationService#deleteExistingFiles(ubic.gemma.model.expression.arrayDesign
     * .ArrayDesign)
     */
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.service.ArrayDesignAnnotationService#generateAnnotationFile(java.io.Writer,
     * java.util.Collection, ubic.gemma.analysis.service.ArrayDesignAnnotationServiceImpl.OutputType)
     */
    @Override
    public int generateAnnotationFile( Writer writer, Collection<Gene> genes, OutputType type ) {

        Map<Gene, Collection<VocabCharacteristic>> goMappings = gene2GOAssociationService.findByGenes( genes );

        for ( Gene gene : genes ) {
            Collection<OntologyTerm> ontos = getGoTerms( gene, goMappings.get( gene ), type );

            Integer ncbiGeneId = gene.getNcbiGeneId();
            Integer ncbiId = ncbiGeneId;
            String ncbiIds = ncbiId == null ? "" : ncbiId.toString();
            String geneString = gene.getOfficialSymbol();
            String geneDescriptionString = gene.getOfficialName();
            try {
                Long id = gene.getId();
                writeAnnotationLine( writer, geneString, ncbiIds, geneDescriptionString, ontos, id.toString(), ncbiIds );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        }
        return genes.size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.service.ArrayDesignAnnotationService#generateAnnotationFile(java.io.Writer,
     * java.util.Map, ubic.gemma.analysis.service.ArrayDesignAnnotationServiceImpl.OutputType, boolean)
     */
    @Override
    public int generateAnnotationFile( Writer writer,
            Map<CompositeSequence, Collection<BioSequence2GeneProduct>> genesWithSpecificity, OutputType ty )
            throws IOException {

        int compositeSequencesProcessed = 0;
        int simple = 0;
        int empty = 0;
        int complex = 0;
        Collection<OntologyTerm> goTerms = new LinkedHashSet<OntologyTerm>();
        Set<String> genes = new LinkedHashSet<String>();
        Set<String> geneDescriptions = new LinkedHashSet<String>();
        Set<String> geneIds = new LinkedHashSet<String>();
        Set<String> ncbiIds = new LinkedHashSet<String>();

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
                goTerms = getGoTerms( g, goMappings.get( g ), ty );
                String gemmaId = g.getId() == null ? "" : g.getId().toString();
                String ncbiId = g.getNcbiGeneId() == null ? "" : g.getNcbiGeneId().toString();
                writeAnnotationLine( writer, cs.getName(), g.getOfficialSymbol(), g.getOfficialName(), goTerms,
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
                goTerms.addAll( getGoTerms( g, goMappings.get( g ), ty ) );

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

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.service.ArrayDesignAnnotationService#initOutputFile(ubic.gemma.model.expression.arrayDesign
     * .ArrayDesign, java.lang.String, boolean)
     */
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
            if ( !parentDir.exists() ) parentDir.mkdirs();
            writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( f ) ) );
        }
        StringBuilder buf = new StringBuilder();
        buf.append( "# Annotation file generated by Gemma\n" );
        buf.append( "# Generated " + DateUtil.convertDateToString( new Date() ) + "\n" );
        buf.append( ExpressionDataFileService.DISCLAIMER );
        buf.append( "# Gemma link for this platform: http://www.chibi.ubc.ca/Gemma/arrays/showArrayDesign.html?id="
                + arrayDesign.getId() + "\n" );
        buf.append( "# " + arrayDesign.getShortName() + "  " + arrayDesign.getName() + "\n" );
        buf.append( "# " + arrayDesign.getPrimaryTaxon().getScientificName() + "\n" );
        writer.write( buf.toString() );
        writer.write( "ProbeName\tGeneSymbols\tGeneNames\tGOTerms\tGemmaIDs\tNCBIids\n" );

        return writer;
    }

    /**
     * @param genesWithSpecificity
     * @return
     */
    private Map<Gene, Collection<VocabCharacteristic>> getGOMappings(
            Map<CompositeSequence, Collection<BioSequence2GeneProduct>> genesWithSpecificity ) {
        log.info( "Fetching GO mappings" );
        Collection<Gene> allGenes = new HashSet<Gene>();
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
     * @param gene
     * @param ty Configures which GO terms to return: With all parents, biological process only, or direct annotations
     *        only.
     * @return the goTerms for a given gene, as configured
     */
    private Collection<OntologyTerm> getGoTerms( Gene gene, Collection<VocabCharacteristic> ontos, OutputType ty ) {

        Collection<OntologyTerm> results = new HashSet<OntologyTerm>();
        if ( ontos == null || ontos.size() == 0 ) return results;

        for ( VocabCharacteristic vc : ontos ) {
            results.add( GeneOntologyServiceImpl.getTermForId( vc.getValue() ) );
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
     * Adds one line at a time to the annotation file.
     * 
     * @param writer
     * @param probeId
     * @param gene
     * @param description
     * @param goTerms
     * @param geneIds
     * @param ncbiIds
     * @throws IOException
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

        if ( goTerms != null && !goTerms.isEmpty() ) {
            String goterms = StringUtils.join( new TransformIterator( goTerms.iterator(), goTermExtractor ), "|" );
            writer.write( goterms );
        }

        writer.write( "\t" + geneIds + "\t" + ncbiIds + "\n" );

    }

}
