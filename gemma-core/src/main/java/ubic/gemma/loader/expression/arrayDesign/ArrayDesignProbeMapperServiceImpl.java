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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.analysis.report.ArrayDesignReportService;
import ubic.gemma.analysis.sequence.ProbeMapper;
import ubic.gemma.analysis.sequence.ProbeMapperConfig;
import ubic.gemma.analysis.service.ArrayDesignAnnotationService;
import ubic.gemma.analysis.service.ExpressionDataFileService;
import ubic.gemma.externalDb.GoldenPathSequenceAnalysis;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.model.genome.gene.GeneAlias;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductService;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociationService;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultService;
import ubic.gemma.persistence.Persister;

/**
 * For an array design, generate gene product mappings for the sequences.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Component
public class ArrayDesignProbeMapperServiceImpl implements ArrayDesignProbeMapperService {

    private static Log log = LogFactory.getLog( ArrayDesignProbeMapperServiceImpl.class.getName() );

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
    private GeneProductService geneProductService;

    @Autowired
    private Persister persisterHelper;

    @Autowired
    private ProbeMapper probeMapper;

    @Autowired
    private ArrayDesignReportService arrayDesignReportService;

    @Autowired
    private ArrayDesignAnnotationService arrayDesignAnnotationService;

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.loader.expression.arrayDesign.ArrayDesignProbeMapperService#printResult(ubic.gemma.model.expression
     * .designElement.CompositeSequence, java.util.Collection)
     */
    @Override
    public void printResult( CompositeSequence compositeSequence, Collection<BlatAssociation> col ) {
        for ( BlatAssociation blatAssociation : col ) {
            printResult( compositeSequence, blatAssociation );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.loader.expression.arrayDesign.ArrayDesignProbeMapperService#processArrayDesign(ubic.gemma.model.expression
     * .arrayDesign.ArrayDesign)
     */
    @Override
    public void processArrayDesign( ArrayDesign arrayDesign ) {
        this.processArrayDesign( arrayDesign, new ProbeMapperConfig(), true );
    }

    /*
     * (non-Javadoc) - mapping to the genome ourselves.
     * 
     * @see
     * ubic.gemma.loader.expression.arrayDesign.ArrayDesignProbeMapperService#processArrayDesign(ubic.gemma.model.expression
     * .arrayDesign.ArrayDesign, ubic.gemma.analysis.sequence.ProbeMapperConfig, boolean)
     */
    @Override
    public void processArrayDesign( ArrayDesign arrayDesign, ProbeMapperConfig config, boolean useDB ) {

        assert config != null;

        if ( arrayDesign.getTechnologyType().equals( TechnologyType.NONE ) ) {
            throw new IllegalArgumentException(
                    "Do not use this service to process platforms that do not use an probe-based technology." );
        }

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
        try {
            this.deleteOldFiles( arrayDesign );
        } catch ( IOException e ) {
            log.error( "Failed to delete all old files associated with " + arrayDesign
                    + ", be sure to clean them up manually or regenerate them" );
        }
        arrayDesignReportService.generateArrayDesignReport( arrayDesign.getId() );

    }

    /*
     * (non-Javadoc) - from an input file, not mapped with sequences.
     * 
     * @see
     * ubic.gemma.loader.expression.arrayDesign.ArrayDesignProbeMapperService#processArrayDesign(ubic.gemma.model.expression
     * .arrayDesign.ArrayDesign, ubic.gemma.model.genome.Taxon, java.io.File,
     * ubic.gemma.model.common.description.ExternalDatabase, boolean)
     */
    @Override
    public void processArrayDesign( ArrayDesign arrayDesign, Taxon taxon, File source, ExternalDatabase sourceDB,
            boolean ncbiIds ) throws IOException {

        if ( taxon == null && !ncbiIds ) {
            throw new IllegalArgumentException( "You must provide a taxon unless passing ncbiIds = true" );
        }

        if ( arrayDesign.getTechnologyType().equals( TechnologyType.NONE ) ) {
            throw new IllegalArgumentException(
                    "Do not use this service to process platforms that do not use an probe-based technology." );
        }

        try (BufferedReader b = new BufferedReader( new FileReader( source ) );) {
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
                    if ( log.isDebugEnabled() )
                        log.debug( "No probe found for '" + probeId + "' on " + arrayDesign + ", skipping" );
                    numSkipped++;
                    continue;
                }

                // a probe can have more than one gene associated with it if so they are piped |
                Collection<Gene> geneListProbe = new HashSet<Gene>();

                // indicate multiple genes
                Gene geneDetails = null;

                StringTokenizer st = new StringTokenizer( geneSymbol, "|" );
                while ( st.hasMoreTokens() ) {
                    String geneToken = st.nextToken().trim();
                    if ( ncbiIds ) {
                        geneDetails = geneService.findByNCBIId( Integer.parseInt( geneToken ) );
                    } else {
                        geneDetails = geneService.findByOfficialSymbol( geneToken, taxon );
                    }
                    if ( geneDetails != null ) {
                        geneListProbe.add( geneDetails );
                    }
                }

                if ( geneListProbe.size() == 0 ) {
                    log.warn( "No gene(s) found for '" + geneSymbol + "' in " + taxon + ", skipping" );
                    numSkipped++;
                    continue;
                } else if ( geneListProbe.size() > 1 ) {
                    // this is a common situation, when the geneSymbol actually has |-separated genes, so no need to
                    // make a
                    // lot of fuss.
                    log.debug( "More than one gene found for '" + geneSymbol + "' in " + taxon );
                }

                BioSequence bs = c.getBiologicalCharacteristic();

                if ( bs != null ) {
                    if ( StringUtils.isNotBlank( seqName ) ) {
                        bs = bioSequenceService.thaw( bs );
                        if ( !bs.getName().equals( seqName ) ) {
                            log.warn( "Sequence name '" + seqName + "' given for " + probeId
                                    + " does not match existing entry " + bs.getName() + ", skipping" );
                            numSkipped++;
                            continue;
                        }

                    }
                    // otherwise we assume everything is okay.
                } else {
                    // create one based on the text provided.
                    if ( StringUtils.isBlank( seqName ) ) {
                        log.warn( "You must provide sequence names for probes which are not already mapped. probeName="
                                + probeId + " had no sequence associated and no name provided; skipping" );
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

                    c.setBiologicalCharacteristic( bs );

                    // fixme: possibly move outside the loop if that's faster.
                    compositeSequenceService.update( c );
                }

                assert bs != null;
                assert bs.getId() != null;
                for ( Gene gene : geneListProbe ) {
                    gene = geneService.thaw( gene );
                    if ( gene.getProducts().size() == 0 ) {
                        log.warn( "There are no gene products for " + gene
                                + ", it cannot be mapped to probes. Skipping" );
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

            this.deleteOldFiles( arrayDesign );

            log.info( "Completed association processing for " + arrayDesign + ", " + numSkipped + " were skipped" );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.loader.expression.arrayDesign.ArrayDesignProbeMapperService#processCompositeSequence(ubic.gemma.analysis
     * .sequence.ProbeMapperConfig, ubic.gemma.model.genome.Taxon, ubic.gemma.externalDb.GoldenPathSequenceAnalysis,
     * ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    @Override
    @Transactional
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
    void doLoad( final BlockingQueue<BlatAssociation> queue, AtomicBoolean generatorDone, AtomicBoolean loaderDone ) {
        int loadedAssociationCount = 0;
        while ( !( generatorDone.get() && queue.isEmpty() ) ) {

            try {
                BlatAssociation ba = queue.poll();
                if ( ba == null ) {
                    continue;
                }

                GeneProduct geneProduct = ba.getGeneProduct();
                if ( geneProduct.getId() == null ) {
                    GeneProduct existing = geneProductService.find( geneProduct );

                    if ( existing == null ) {

                        existing = checkForAlias( geneProduct );
                        if ( existing == null ) {
                            /*
                             * We have to be careful not to cruft up the gene table now that I so carefully cleaned it.
                             * But this is a problem if we aren't adding some other association to the gene at least.
                             * But generally the mRNAs that GP has that NCBI doesn't are "alternative" or "additional".
                             */
                            if ( log.isDebugEnabled() )
                                log.debug( "New gene product from GoldenPath is not in Gemma: " + geneProduct
                                        + " skipping association to " + ba.getBioSequence()
                                        + " [skipping policy in place]" );
                            continue;
                        }
                    }
                    ba.setGeneProduct( existing );
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

    /**
     * @param ba
     * @param geneProduct
     */
    private final GeneProduct checkForAlias( GeneProduct geneProduct ) {
        Collection<GeneProduct> candidates = geneProductService.findByName( geneProduct.getName(), geneProduct
                .getGene().getTaxon() );

        if ( candidates.isEmpty() ) return null;

        Gene gene = geneProduct.getGene();
        for ( GeneProduct existing2 : candidates ) {
            Collection<GeneAlias> aliases = existing2.getGene().getAliases();
            for ( GeneAlias geneAlias : aliases ) {
                if ( geneAlias.getAlias().equalsIgnoreCase( gene.getOfficialSymbol() ) ) {
                    /*
                     * So, our gene products match, and the genes match but via an alias. That's pretty solid.
                     */
                    log.info( "Associated gene product " + geneProduct
                            + " has a match in Gemma through an aliased gene: " + existing2 );
                    return existing2;
                }
            }

        }
        return null;
    }

    /**
     * Delete outdated annotation and associated experiment files.
     * 
     * @param arrayDesign
     */
    private void deleteOldFiles( ArrayDesign arrayDesign ) throws IOException {
        arrayDesignAnnotationService.deleteExistingFiles( arrayDesign );
        Collection<ExpressionExperiment> ees4platform = arrayDesignService.getExpressionExperiments( arrayDesign );
        for ( ExpressionExperiment ee : ees4platform ) {
            expressionDataFileService.deleteAllFiles( ee );
        }

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
            @Override
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

}
