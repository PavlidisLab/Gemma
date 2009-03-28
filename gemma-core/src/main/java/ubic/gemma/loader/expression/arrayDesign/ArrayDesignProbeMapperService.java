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
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.sequence.ProbeMapper;
import ubic.gemma.apps.Blat;
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
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationService;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultService;
import ubic.gemma.persistence.PersisterHelper;

/**
 * For an array design, generate gene product mappings for the sequences.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean name="arrayDesignProbeMapperService"
 * @spring.property name="blatResultService" ref="blatResultService"
 * @spring.property name="blatAssociationService" ref="blatAssociationService"
 * @spring.property name="persisterHelper" ref="persisterHelper"
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 * @spring.property name="probeMapper" ref="probeMapper"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="compositeSequenceService" ref="compositeSequenceService"
 * @spring.property name="bioSequenceService" ref="bioSequenceService"
 * @spring.property name="annotationAssociationService" ref="annotationAssociationService"
 */
public class ArrayDesignProbeMapperService {

    private static Log log = LogFactory.getLog( ArrayDesignProbeMapperService.class.getName() );

    private static final int QUEUE_SIZE = 20000;

    AnnotationAssociationService annotationAssociationService;

    ArrayDesignService arrayDesignService;

    BioSequenceService bioSequenceService;

    BlatAssociationService blatAssociationService;

    BlatResultService blatResultService;

    CompositeSequenceService compositeSequenceService;

    GeneService geneService;

    PersisterHelper persisterHelper;

    ProbeMapper probeMapper;

    private double blatScoreThreshold = Blat.DEFAULT_BLAT_SCORE_THRESHOLD;

    private double identityThreshold = ProbeMapper.DEFAULT_IDENTITY_THRESHOLD;

    private double scoreThreshold = ProbeMapper.DEFAULT_SCORE_THRESHOLD;

    /**
     * Do probe mapping, writing the results to the database.
     * 
     * @param arrayDesign
     */
    public void processArrayDesign( ArrayDesign arrayDesign ) {
        this.processArrayDesign( arrayDesign, true );
    }

    /**
     * @param arrayDesign
     * @param useDB if false, the results will not be written to the database, but printed to stdout instead.
     */
    @SuppressWarnings("unchecked")
    public void processArrayDesign( ArrayDesign arrayDesign, boolean useDB ) {

        Taxon taxon = arrayDesignService.getTaxon( arrayDesign.getId() );
        if ( taxon == null ) {
            throw new IllegalArgumentException( "Cannot analyze " + arrayDesign + ", taxon could not be determined" );
        }
        GoldenPathSequenceAnalysis goldenPathDb;
        try {
            goldenPathDb = new GoldenPathSequenceAnalysis( taxon );
        } catch ( SQLException e ) {
            throw new RuntimeException( e );
        }

        probeMapper.setIdentityThreshold( identityThreshold );
        probeMapper.setScoreThreshold( scoreThreshold );
        probeMapper.setBlatScoreThreshold( blatScoreThreshold );

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
        log.info( "Start processing probes ..." );
        for ( CompositeSequence compositeSequence : arrayDesign.getCompositeSequences() ) {
            BioSequence bs = compositeSequence.getBiologicalCharacteristic();

            if ( bs == null ) continue;

            final Collection<BlatResult> blatResults = blatResultService.findByBioSequence( bs );

            if ( blatResults == null || blatResults.isEmpty() ) continue;

            Map<String, Collection<BlatAssociation>> results = probeMapper.processBlatResults( goldenPathDb,
                    blatResults );

            if ( log.isDebugEnabled() )
                log.debug( "Found " + results.size() + " mappings for " + compositeSequence + " (" + blatResults.size()
                        + " BLAT results)" );

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

            if ( ++count % 100 == 0 ) {
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
                e.printStackTrace();
            }
        }

        log.info( "Processed " + count + " composite sequences with blat results; " + hits + " mappings found." );
    }

    /**
     * Print results to STDOUT
     * 
     * @param compositeSequence
     * @param col
     */
    private void printResult( CompositeSequence compositeSequence, Collection<BlatAssociation> col ) {
        for ( BlatAssociation blatAssociation : col ) {
            printResult( compositeSequence, blatAssociation );
        }
    }

    /**
     * Print line of result to STDOUT.
     * 
     * @param cs
     * @param blatAssociation
     */
    private void printResult( CompositeSequence cs, BlatAssociation blatAssociation ) {

        System.out.println( cs.getName() + '\t' + blatAssociation.getBioSequence().getName() + '\t'
                + blatAssociation.getGeneProduct().getName() + '\t'
                + blatAssociation.getGeneProduct().getGene().getOfficialName() );
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
     * @param arrayDesign. If
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
            String geneSymbol = fields[2];

            CompositeSequence c = compositeSequenceService.findByName( arrayDesign, probeId );

            if ( c == null ) {
                log.warn( "No probe found for '" + probeId + "' on " + arrayDesign + ", skipping" );
                numSkipped++;
                continue;
            }

            Gene g = geneService.findByOfficialSymbol( geneSymbol, taxon );

            if ( g == null ) {
                log.warn( "No gene found for '" + geneSymbol + "' in " + taxon + ", skipping" );
                numSkipped++;
                continue;
            }

            BioSequence bs = c.getBiologicalCharacteristic();

            if ( bs != null ) {
                bioSequenceService.thaw( bs );
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

            geneService.thaw( g );
            if ( g.getProducts().size() == 0 ) {
                log.warn( "There are no gene products for " + g + ", it cannot be mapped to probes. Skipping" );
                numSkipped++;
                continue;
            }
            for ( GeneProduct gp : g.getProducts() ) {
                AnnotationAssociation association = AnnotationAssociation.Factory.newInstance();
                association.setBioSequence( bs );
                association.setGeneProduct( gp );
                association.setSource( sourceDB );
                annotationAssociationService.create( association );
            }

        }

        log.info( "Completed association processing for " + arrayDesign + ", " + numSkipped + " were skipped" );

    }

    /**
     * @param annotationAssociationService the annotationAssociationService to set
     */
    public void setAnnotationAssociationService( AnnotationAssociationService annotationAssociationService ) {
        this.annotationAssociationService = annotationAssociationService;
    }

    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @param bioSequenceService the bioSequenceService to set
     */
    public void setBioSequenceService( BioSequenceService bioSequenceService ) {
        this.bioSequenceService = bioSequenceService;
    }

    /**
     * @param blatAssociationService the blatAssociationService to set
     */
    public void setBlatAssociationService( BlatAssociationService blatAssociationService ) {
        this.blatAssociationService = blatAssociationService;
    }

    /**
     * @param bioSequenceService the bioSequenceService to set
     */
    public void setBlatResultService( BlatResultService blatResultService ) {
        this.blatResultService = blatResultService;
    }

    /**
     * @param blatScoreThreshold the blatScoreThreshold to set
     */
    public void setBlatScoreThreshold( double blatScoreThreshold ) {
        this.blatScoreThreshold = blatScoreThreshold;
    }

    /**
     * @param compositeSequenceService the compositeSequenceService to set
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

    /**
     * @param geneService the geneService to set
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * @param identityThreshold the identityThreshold to set
     */
    public void setIdentityThreshold( double identityThreshold ) {
        this.identityThreshold = identityThreshold;
    }

    /**
     * @param persisterHelper the persisterHelper to set
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    public void setProbeMapper( ProbeMapper probeMapper ) {
        this.probeMapper = probeMapper;
    }

    /**
     * @param scoreThreshold the scoreThreshold to set
     */
    public void setScoreThreshold( double scoreThreshold ) {
        this.scoreThreshold = scoreThreshold;
    }

    /**
     * @param queue
     * @param generatorDone
     * @param loaderDone
     */
    private void doLoad( final BlockingQueue<BlatAssociation> queue, AtomicBoolean generatorDone,
            AtomicBoolean loaderDone ) {
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

}
