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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.FileTools;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PredictedGene;
import ubic.gemma.model.genome.ProbeAlignedRegion;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.ontology.GeneOntologyService;
import ubic.gemma.ontology.OntologyTerm;
import ubic.gemma.util.ConfigUtils;

/**
 * Methods to generate annotations for array designs, based on information alreay in the database. This can be used to
 * generate annotation files used for ermineJ, for eexample.
 * 
 * @spring.bean id="arrayDesignAnnotationService"
 * @spring.property name="gene2GOAssociationService" ref="gene2GOAssociationService"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="compositeSequenceService" ref="compositeSequenceService"
 * @spring.property name="goService" ref="geneOntologyService"
 * @author paul
 * @version $Id$
 */
public class ArrayDesignAnnotationService {

    public static final String ANNOTATION_FILE_SUFFIX = ".an.txt.gz";

    public static final String ANNOT_DATA_DIR = ConfigUtils.getString( "gemma.appdata.home" ) + "/microAnnots/";

    private static Log log = LogFactory.getLog( ArrayDesignAnnotationService.class.getName() );;

    private Gene2GOAssociationService gene2GOAssociationService;

    private GeneService geneService;

    private CompositeSequenceService compositeSequenceService;

    private GeneOntologyService goService;

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
    public int generateAnnotationFile( Writer writer, Collection<CompositeSequence> compositeSequences, OutputType ty,
            boolean knownGenesOnly ) throws IOException {

        int compositeSequencesProcessed = 0;

        for ( CompositeSequence sequence : compositeSequences ) {

            Collection<Gene> genes = compositeSequenceService.getGenes( sequence );

            ++compositeSequencesProcessed;

            if ( ( genes == null ) || ( genes.isEmpty() ) ) {
                writeAnnotationLine( writer, sequence.getName(), "", "", null );
                continue;
            }

            // actually the collection gotten back is a collection of proxies
            // which causes issues. Need to reload the
            // genes from the db.
            Collection<Long> geneIds = new ArrayList<Long>();

            for ( Gene g : genes ) {
                geneIds.add( g.getId() );
            }

            genes = geneService.loadMultiple( geneIds );

            String geneNames = null;
            String geneDescriptions = null;
            Collection<OntologyTerm> goTerms = new ArrayList<OntologyTerm>();

            // Might be mulitple genes for a given cs. Need to hash it into one.
            for ( Gene gene : genes ) {

                if ( gene == null ) continue;

                // Add PARs or predicted gene info to annotation file?
                if ( knownGenesOnly && ( ( gene instanceof ProbeAlignedRegion ) || ( gene instanceof PredictedGene ) ) ) {
                    log.debug( "Gene:  " + gene.getOfficialSymbol()
                            + "  not included in annotations because it is a probeAligedRegion or predictedGene" );
                    continue;
                }

                if ( log.isDebugEnabled() )
                    log.debug( "Adding gene: " + gene.getOfficialSymbol() + " of type: " + gene.getClass() );

                addGoTerms( goTerms, gene, ty );
                geneNames = addGeneSymbol( geneNames, gene );
                geneDescriptions = addGeneName( geneDescriptions, gene );

            }

            writeAnnotationLine( writer, sequence.getName(), geneNames, geneDescriptions, goTerms );

            if ( compositeSequencesProcessed % 500 == 0 && log.isInfoEnabled() ) {
                log.info( "Processed " + compositeSequencesProcessed + "/" + compositeSequences.size()
                        + " compositeSequences " );
            }

        }
        writer.close();
        return compositeSequencesProcessed;
    }

    /**
     * @param arrayDesign
     * @return Map of (persistent) composite sequences and transient (incomplete) genes. The genes only have the symbol
     *         filled in.
     */
    public static Map<CompositeSequence, Collection<Gene>> readAnnotationFile( ArrayDesign arrayDesign ) {
        Map<CompositeSequence, Collection<Gene>> results = new HashMap<CompositeSequence, Collection<Gene>>();
        File f = new File( ANNOT_DATA_DIR + arrayDesign.getShortName() + ANNOTATION_FILE_SUFFIX );
        if ( !f.canRead() ) return results;

        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            results.put( cs, new HashSet<Gene>() );
        }

        try {
            log.info( "Reading annotations from: " + f );
            InputStream is = FileTools.getInputStreamFromPlainOrCompressedFile( f.getAbsolutePath() );
            BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
            String line = null;
            br.readLine(); // discard header line.
            while ( ( line = br.readLine() ) != null ) {
                if ( StringUtils.isBlank( line ) ) {
                    continue;
                }
                String[] fields = StringUtils.splitPreserveAllTokens( line, '\t' );
                String probeName = fields[0];

                if ( !results.containsKey( probeName ) ) continue;

                List<String> geneSymbols = Arrays.asList( StringUtils.split( fields[1], '|' ) );
                List<String> geneNames = Arrays.asList( StringUtils.split( fields[2], '|' ) );

                for ( int i = 0; i < geneSymbols.size(); i++ ) {
                    Gene g = Gene.Factory.newInstance();
                    String symbol = geneSymbols.get( i );
                    if ( StringUtils.isBlank( symbol ) ) {
                        continue;
                    }
                    g.setOfficialSymbol( symbol );
                    g.setOfficialName( geneNames.get( i ) );

                    results.get( probeName ).add( g );
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
            f.createNewFile();

            writer = new OutputStreamWriter( new GZIPOutputStream( new FileOutputStream( f ) ) );
        }

        writer.write( "ProbeName\tGeneSymbols\tGeneNames\tGOTerms\n" );

        return writer;
    }

    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

    public void setGene2GOAssociationService( Gene2GOAssociationService gene2GOAssociationService ) {
        this.gene2GOAssociationService = gene2GOAssociationService;
    }

    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    public void setGoService( GeneOntologyService goService ) {
        this.goService = goService;
    }

    /**
     * @param gene
     * @return the goTerms for a given gene, as configured
     */
    @SuppressWarnings("unchecked")
    protected Collection<OntologyTerm> getGoTerms( Gene gene, OutputType ty ) {

        Collection<VocabCharacteristic> ontos = new HashSet<VocabCharacteristic>( gene2GOAssociationService
                .findByGene( gene ) );

        Collection<OntologyTerm> results = new HashSet<OntologyTerm>();
        for ( VocabCharacteristic vc : ontos ) {
            results.add( GeneOntologyService.getTermForId( vc.getValue() ) );
        }

        if ( ( ontos == null ) || ( ontos.size() == 0 ) ) return results;

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
    protected void writeAnnotationLine( Writer writer, String probeId, String gene, String description,
            Collection<OntologyTerm> goTerms ) throws IOException {

        if ( log.isDebugEnabled() ) log.debug( "Generating line for annotation file  \n" );

        if ( gene == null ) gene = "";

        if ( description == null ) description = "";

        writer.write( probeId + "\t" + gene + "\t" + description + "\t" );

        if ( ( goTerms == null ) || goTerms.isEmpty() ) {
            writer.write( "\n" );
            return;
        }

        boolean wrote = false;
        for ( OntologyTerm oe : goTerms ) {
            if ( oe == null ) continue;
            if ( wrote ) {
                writer.write( "|" + GeneOntologyService.asRegularGoId( oe ) );
            } else {
                writer.write( GeneOntologyService.asRegularGoId( oe ) );
            }
            wrote = true;
        }

        writer.write( "\n" );
        writer.flush();

    }

    /**
     * @param geneDescriptions
     * @param gene
     * @return
     */
    private String addGeneName( String geneDescriptions, Gene gene ) {
        if ( gene.getOfficialName() != null ) {
            if ( geneDescriptions == null ) {
                geneDescriptions = gene.getOfficialName();
            } else {
                geneDescriptions += "|" + gene.getOfficialName();
            }
        }
        return geneDescriptions;
    }

    /**
     * @param geneNames
     * @param gene
     * @return
     */
    private String addGeneSymbol( String geneNames, Gene gene ) {
        if ( gene.getOfficialSymbol() != null ) {
            if ( geneNames == null ) {
                geneNames = gene.getOfficialSymbol();
            } else {
                geneNames += "|" + gene.getOfficialSymbol();
            }
        }
        return geneNames;
    }

    /**
     * @param goTerms
     * @param gene
     * @param ty
     * @return
     */
    private Collection<OntologyTerm> addGoTerms( Collection<OntologyTerm> goTerms, Gene gene, OutputType ty ) {
        Collection<OntologyTerm> terms = getGoTerms( gene, ty );
        goTerms.addAll( terms );
        return terms;
    }

    public enum OutputType {
        SHORT, LONG, BIOPROCESS
    }

}
