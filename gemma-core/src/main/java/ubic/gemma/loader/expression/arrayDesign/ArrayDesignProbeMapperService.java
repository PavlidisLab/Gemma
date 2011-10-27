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
package ubic.gemma.loader.expression.arrayDesign;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import ubic.gemma.analysis.report.ArrayDesignReportService;
import ubic.gemma.analysis.sequence.ProbeMapper;
import ubic.gemma.analysis.sequence.ProbeMapperConfig;
import ubic.gemma.externalDb.GoldenPathSequenceAnalysis;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociationService;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultService;
import ubic.gemma.persistence.PersisterHelper;

/**
 * For an array design, generate gene product mappings for the sequences.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Service
public class ArrayDesignProbeMapperService {

    private static Log log = LogFactory.getLog( ArrayDesignProbeMapperService.class.getName() );

    private static final int QUEUE_SIZE = 20000;

    @Autowired
    private AnnotationAssociationService annotationAssociationService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private BioSequenceService bioSequenceService;

    @Autowired
    private BlatResultService blatResultService;

    @Autowired
    private CompositeSequenceService compositeSequenceService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private PersisterHelper persisterHelper;

    @Autowired
    private ProbeMapper probeMapper;

    @Autowired
    ArrayDesignReportService arrayDesignReportService;

    /**
     * Print results to STDOUT
     * 
     * @param compositeSequence
     * @param col
     */
    public void printResult( CompositeSequence compositeSequence, Collection<BlatAssociation> col ) {
        for ( BlatAssociation blatAssociation : col ) {
            printResult( compositeSequence, blatAssociation );
        }
    }

    /**
     * Do probe mapping, writing the results to the database and using default settings.
     * 
     * @param arrayDesign
     */
    public void processArrayDesign( ArrayDesign arrayDesign ) {
        this.processArrayDesign( arrayDesign, new ProbeMapperConfig(), true );
    }

    /**
     * @param arrayDesign
     * @param config
     * @param useDB if false, the results will not be written to the database, but printed to stdout instead.
     */
    public void processArrayDesign( ArrayDesign arrayDesign, ProbeMapperConfig config, boolean useDB ) {

        Collection<Taxon> taxa = arrayDesignService.getTaxa( arrayDesign.getId() );

        Taxon taxon = arrayDesign.getPrimaryTaxon();
        if ( taxa.size() > 1 && taxon == null ) {
            throw new IllegalArgumentException(
                    "Array design has sequence from multiple taxa and has no primary taxon set: " + arrayDesign );
        }

        GoldenPathSequenceAnalysis goldenPathDb = new GoldenPathSequenceAnalysis( taxon );

        BlockingQueue<BlatAssociation> persistingQueue = new ArrayBlockingQueue<BlatAssociation>( QUEUE_SIZE );
        AtomicBoolean generatorDone = new AtomicBoolean( false );
        AtomicBoolean loaderDone = new AtomicBoolean( false );

        load( persistingQueue, generatorDone, loaderDone );

        if ( useDB ) {
            log.info( "Removing any old associations" );
            arrayDesignService.deleteGeneProductAssociations( arrayDesign );
        }

        int count = 0;
        int hits = 0;
        log.info( "Start processing " + arrayDesign.getCompositeSequences().size() + " probes ..." );
        for ( CompositeSequence compositeSequence : arrayDesign.getCompositeSequences() ) {

            Map<String, Collection<BlatAssociation>> results = processCompositeSequence( config, taxon, goldenPathDb,
                    compositeSequence );

            if ( results == null ) continue;

            for ( Collection<BlatAssociation> col : results.values() ) {
                for ( BlatAssociation association : col ) {
                    if ( log.isDebugEnabled() ) log.debug( association );
                }

                if ( useDB ) {
                    // persisting is done in a separate thread.
                    persistingQueue.addAll( col );
                } else {
                    printResult( compositeSequence, col );
                }
                ++hits;
            }

            if ( ++count % 200 == 0 ) {
                log.info( "Processed " + count + " composite sequences" + " with blat results; " + hits
                        + " mappings found." );
            }
        }

        generatorDone.set( true );

        log.info( "Waiting for loading to complete ..." );
        while ( !loaderDone.get() ) {
            try {
                Thread.sleep( 1000 );
            } catch ( InterruptedException e ) {
                throw new RuntimeException( e );
            }
        }

        log.info( "Processed " + count + " composite sequences with blat results; " + hits + " mappings found." );

        arrayDesignReportService.generateArrayDesignReport( arrayDesign.getId() );

    }

    /**
     * Annotate an array design using a direct source file. This should only be used if we can't run sequence analysis
     * ourselves.
     * <p>
     * The expected file format is tab-delimited with the following columns:
     * <ul>
     * <li>Probe name which must match the probe names Gemma uses for the array design.
     * <li>Sequence name. If blank, it will be ignored but the probe will still be mapped if possible. The probe will be
     * skipped if it isn't already associated with a sequence. If not blank, it will be checked against the sequence for
     * the probe. If the probe has no sequence, it will be used to create one. If it does, it will be checked for a name
     * match.
     * <li>Gene symbol. More than one gene can be specified, delimited by '|'. Genes will only be found if Gemma has a
     * unambiguous match to the name. The gene must already exist in the system.
     * </ul>
     * Comment lines begin with '#';
     * <p>
     * Note that <em>all</em> the RNA gene products of the gene will be associated with the sequence. This is necessary
     * because 1) Gemma associates sequences with transcripts, not genes and 2) if all we get is a gene, we have to
     * assume all gene products are relevant.
     * 
     * @param arrayDesign.
     * @param taxon. We require this to ensure correct association of the sequences with the genes.
     * @param source
     * @param sourceDB describes where the annotations came from. Can be null if you really don't know.
     * @throws IllegalStateException if the input file doesn't match the array design.
     */
    public void processArrayDesign( ArrayDesign arrayDesign, Taxon taxon, File source, ExternalDatabase sourceDB )
            throws IOException {

        BufferedReader b = new BufferedReader( new FileReader( source ) );
        String line = null;
        int numSkipped = 0;

        log.info( "Removing any old associations" );
        arrayDesignService.deleteGeneProductAssociations( arrayDesign );

        while ( ( line = b.readLine() ) != null ) {

            if ( StringUtils.isBlank( line ) ) {
                continue;
            }
            if ( line.startsWith( "#" ) ) {
                continue;
            }

            String[] fields = StringUtils.splitPreserveAllTokens( line, '\t' );
            if ( fields.length != 3 ) {
                throw new IOException( "Illegal format, expected three columns, got " + fields.length );
            }

            String probeId = fields[0];
            String seqName = fields[1];

            /*
             * FIXME. We have to allow NCBI gene ids here.
             */
            String geneSymbol = fields[2];

            if ( StringUtils.isBlank( geneSymbol ) ) {
                numSkipped++;
                continue;
            }

            CompositeSequence c = compositeSequenceService.findByName( arrayDesign, probeId );

            if ( c == null ) {
                log.warn( "No probe found for '" + probeId + "' on " + arrayDesign + ", skipping" );
                numSkipped++;
                continue;
            }

            // a probe can have more than one gene associated with it if so they are piped |
            Collection<Gene> geneListProbe = new HashSet<Gene>();

            // indicate multiple genes
            Gene geneDetails = null;

            StringTokenizer st = new StringTokenizer( geneSymbol, "|" );
            while ( st.hasMoreTokens() ) {
                String geneToken = st.nextToken();
                geneDetails = geneService.findByOfficialSymbol( geneToken.trim(), taxon );
                if ( geneDetails != null ) {
                    geneListProbe.add( geneDetails );
                }
            }

            if ( geneListProbe.size() == 0 ) {
                log.warn( "No gene found for '" + geneSymbol + "' in " + taxon + ", skipping" );
                numSkipped++;
                continue;
            } else if ( geneListProbe.size() > 1 ) {
                // this is a common situation, when the geneSymbol actually has |-separated genes, so no need to make a
                // lot of fuss.
                log.debug( "More than one gene found for '" + geneSymbol + "' in " + taxon );
            }

            BioSequence bs = c.getBiologicalCharacteristic();

            if ( bs != null ) {
                bs = bioSequenceService.thaw( bs );
                if ( StringUtils.isNotBlank( seqName ) && !bs.getName().equals( seqName ) ) {
                    log.warn( "Sequence name '" + seqName + "' given for " + probeId
                            + " does not match existing entry " + bs.getName() + ", skipping" );
                    numSkipped++;
                    continue;
                }
                // Otherwise, we just forget about the text they provided for the bs name, it's fine.

            } else {
                // create one based on the text.
                if ( StringUtils.isBlank( seqName ) ) {
                    log.warn( "You must provide sequence names for probes which are not already mapped. " + probeId
                            + " had no sequence associated an no name provided; skipping" );
                    numSkipped++;
                    continue;
                }

                bs = BioSequence.Factory.newInstance();
                bs.setName( seqName );
                bs.setTaxon( taxon );
                bs.setDescription( "Imported from annotation file" );

                // Placeholder.
                bs.setType( SequenceType.OTHER );

                bs = bioSequenceService.create( bs );

            }

            assert bs.getId() != null;
            for ( Gene gene : geneListProbe ) {
                gene = geneService.thaw( gene );
                if ( gene.getProducts().size() == 0 ) {
                    log.warn( "There are no gene products for " + gene + ", it cannot be mapped to probes. Skipping" );
                    numSkipped++;
                    continue;
                }
                for ( GeneProduct gp : gene.getProducts() ) {
                    AnnotationAssociation association = AnnotationAssociation.Factory.newInstance();
                    association.setBioSequence( bs );
                    association.setGeneProduct( gp );
                    association.setSource( sourceDB );
                    annotationAssociationService.create( association );
                }

            }

        }

        arrayDesignReportService.generateArrayDesignReport( arrayDesign.getId() );
        log.info( "Completed association processing for " + arrayDesign + ", " + numSkipped + " were skipped" );
        b.close();
    }

    /**
     * @param config
     * @param taxon
     * @param goldenPathDb
     * @param compositeSequence
     * @param bs
     * @return
     */
    public Map<String, Collection<BlatAssociation>> processCompositeSequence( ProbeMapperConfig config, Taxon taxon,
            GoldenPathSequenceAnalysis goldenPathDb, CompositeSequence compositeSequence ) {
        BioSequence bs = compositeSequence.getBiologicalCharacteristic();
        if ( bs == null ) return null;

        /*
         * It isn't 100% clear what the right thing to do is. But this seems at least _reasonable_ when there is a
         * mismatch
         */
        if ( taxon != null && !bs.getTaxon().equals( taxon ) ) {
            return null;
        }

        GoldenPathSequenceAnalysis db;
        if ( goldenPathDb == null ) {
            db = new GoldenPathSequenceAnalysis( bs.getTaxon() );
        } else {
            db = goldenPathDb;
        }

        final Collection<BlatResult> blatResults = blatResultService.findByBioSequence( bs );

        if ( blatResults == null || blatResults.isEmpty() ) return null;

        Map<String, Collection<BlatAssociation>> results = probeMapper.processBlatResults( db, blatResults, config );

        if ( log.isDebugEnabled() )
            log.debug( "Found " + results.size() + " mappings for " + compositeSequence + " (" + blatResults.size()
                    + " BLAT results)" );
        return results;
    }

    /**
     * @param queue
     * @param generatorDone
     * @param loaderDone
     */
    private void load( final BlockingQueue<BlatAssociation> queue, final AtomicBoolean generatorDone,
            final AtomicBoolean loaderDone ) {
        final SecurityContext context = SecurityContextHolder.getContext();
        assert context != null;

        Thread loadThread = new Thread( new Runnable() {
            public void run() {
                SecurityContextHolder.setContext( context );
                doLoad( queue, generatorDone, loaderDone );
            }

        }, "PersistBlatAssociations" );

        loadThread.start();

    }

    /**
     * Print line of result to STDOUT.
     * 
     * @param cs
     * @param blatAssociation
     */
    private void printResult( CompositeSequence cs, BlatAssociation blatAssociation ) {

        GeneProduct geneProduct = blatAssociation.getGeneProduct();
        Gene gene = geneProduct.getGene();
        System.out.println( cs.getName() + '\t' + blatAssociation.getBioSequence().getName() + '\t'
                + geneProduct.getName() + '\t' + gene.getOfficialSymbol() + "\t" + gene.getClass().getSimpleName() );
    }

    /**
     * @param queue
     * @param generatorDone
     * @param loaderDone
     */
    void doLoad( final BlockingQueue<BlatAssociation> queue, AtomicBoolean generatorDone, AtomicBoolean loaderDone ) {
        int loadedAssociationCount = 0;
        while ( !( generatorDone.get() && queue.isEmpty() ) ) {

            try {
                BlatAssociation ba = queue.poll();
                if ( ba == null ) {
                    continue;
                }

                persisterHelper.persist( ba );

                if ( ++loadedAssociationCount % 1000 == 0 ) {
                    log.info( "Persisted " + loadedAssociationCount + " blat associations. " + "Current queue has "
                            + queue.size() + " items." );
                }

            } catch ( Exception e ) {
                log.error( e, e );
                loaderDone.set( true );
                throw new RuntimeException( e );
            }
        }
        log.info( "Load thread done: loaded " + loadedAssociationCount + " blat associations. " );
        loaderDone.set( true );
    }

}
