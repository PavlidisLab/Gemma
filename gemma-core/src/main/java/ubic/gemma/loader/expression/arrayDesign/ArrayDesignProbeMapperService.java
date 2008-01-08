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

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.sequence.ProbeMapper;
import ubic.gemma.apps.Blat;
import ubic.gemma.externalDb.GoldenPathSequenceAnalysis;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
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
 */
public class ArrayDesignProbeMapperService {

    private static final int QUEUE_SIZE = 2000;

    private static Log log = LogFactory.getLog( ArrayDesignProbeMapperService.class.getName() );

    BlatResultService blatResultService;

    BlatAssociationService blatAssociationService;

    PersisterHelper persisterHelper;

    ArrayDesignService arrayDesignService;

    ProbeMapper probeMapper;

    private double identityThreshold = ProbeMapper.DEFAULT_IDENTITY_THRESHOLD;
    private double scoreThreshold = ProbeMapper.DEFAULT_SCORE_THRESHOLD;
    private double blatScoreThreshold = Blat.DEFAULT_BLAT_SCORE_THRESHOLD;

    /**
     * @param arrayDesign
     */
    @SuppressWarnings("unchecked")
    public void processArrayDesign( ArrayDesign arrayDesign ) {

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

        log.info( "Removing any old associations" );
        arrayDesignService.deleteGeneProductAssociations( arrayDesign );

        int count = 0;
        int hits = 0;
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

                // persisting is done in a separate thread.
                persistingQueue.addAll( col );
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

    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
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
        log.info( "Loaded " + loadedAssociationCount + " blat associations. " );
        loaderDone.set( true );
    }

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
