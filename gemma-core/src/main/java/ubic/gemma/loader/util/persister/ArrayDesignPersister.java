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
package ubic.gemma.loader.util.persister;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.designElement.Reporter;
import ubic.gemma.model.expression.designElement.ReporterService;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 * @spring.property name="compositeSequenceService" ref="compositeSequenceService"
 * @spring.property name="reporterService" ref="reporterService"
 * @author pavlidis
 * @version $Id$
 */
abstract public class ArrayDesignPersister extends GenomePersister {

    protected ArrayDesignService arrayDesignService;

    protected CompositeSequenceService compositeSequenceService;

    protected ReporterService reporterService;

    Collection<String> arrayDesignCache = new HashSet<String>();

    Map<String, DesignElement> designElementCache = new HashMap<String, DesignElement>();

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.persister.Persister#persist(java.lang.Object)
     */
    public Object persist( Object entity ) {
        if ( entity instanceof ArrayDesign ) {
            return persistArrayDesign( ( ArrayDesign ) entity );
        }
        return super.persist( entity );
    }

    /**
     * @param arrayDesignService The arrayDesignService to set.
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @param compositeSequenceService The compositeSequenceService to set.
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

    /**
     * @param designElementCache The designElementCache to set.
     */
    public void setDesignElementCache( Map<String, DesignElement> designElementCache ) {
        this.designElementCache = designElementCache;
    }

    /**
     * @param reporterService The reporterService to set.
     */
    public void setReporterService( ReporterService reporterService ) {
        this.reporterService = reporterService;
    }

    /**
     * @param designElementCache
     * @param arrayDesign To add to the cache.
     */
    @SuppressWarnings("unchecked")
    protected void addToDesignElementCache( ArrayDesign arrayDesign ) {

        ArrayDesign pArrayDesign;
        if ( isTransient( arrayDesign ) ) {
            pArrayDesign = arrayDesignService.find( arrayDesign );
        } else {
            pArrayDesign = arrayDesign;
        }

        log.info( "Loading array design elements for " + arrayDesign );

        Collection<DesignElement> compositeSequences = arrayDesignService.loadCompositeSequences( pArrayDesign );
        int csNum = arrayDesignService.getCompositeSequenceCount( pArrayDesign );
        if ( compositeSequences != null && csNum > 0 ) {
            log.info( "Filling cache with " + csNum + " compositeSequences" );
        }

        String adName = " " + arrayDesign.getName();
        for ( DesignElement element : compositeSequences ) {
            assert element.getId() != null;
            designElementCache.put( element.getName() + adName, element );
        }

        Collection<DesignElement> reporters = arrayDesignService.loadReporters( pArrayDesign );
        int repNum = arrayDesignService.getReporterCount( pArrayDesign );
        if ( reporters != null && repNum > 0 ) {
            log.info( "Filling cache with " + repNum + " reporters " );
        }
        adName = " " + arrayDesign.getName();
        for ( DesignElement element : reporters ) {
            assert element.getId() != null;
            designElementCache.put( element.getName() + adName, element );
        }

        log.info( "Filled cache" );
    }

    /**
     * Make sure an array design is persistent and in the cache.
     * 
     * @param arrayDesignCache
     * @param cacheIsSetUp
     * @param designElementCache
     * @param ad
     */
    protected void cacheArrayDesign( ArrayDesign ad ) {
        if ( ad == null ) return;
        if ( arrayDesignCache.contains( ad.getName() ) ) {
            return;
        }

        ad = persistArrayDesign( ad );
        addToDesignElementCache( ad );
        arrayDesignCache.add( ad.getName() );

    }

    /**
     * @param persistentDesignElement
     * @param maybeExistingDesignElement
     * @param key
     * @return
     */
    protected DesignElement getPersistentDesignElement( DesignElement persistentDesignElement,
            DesignElement maybeExistingDesignElement, String key ) {
        if ( maybeExistingDesignElement instanceof CompositeSequence ) {
            persistentDesignElement = persistDesignElement( maybeExistingDesignElement );
        } else if ( maybeExistingDesignElement instanceof Reporter ) {
            persistentDesignElement = persistDesignElement( maybeExistingDesignElement );
        }
        if ( persistentDesignElement == null ) {
            throw new IllegalStateException( maybeExistingDesignElement + " does not have a persistent version" );
        }

        designElementCache.put( key, persistentDesignElement );
        return persistentDesignElement;
    }

    /**
     * Persist an array design. If possible we avoid re-checking all the design elements. This is done by comparing the
     * number of design elements that already exist for the array design. If it is the same, no additional action is
     * going to be taken. In this case the array design will not be updated at all.
     * <p>
     * Therefore, if an array design needs to be updated (e.g., manufacturer or description) but the design elements
     * have already been entered, a different mechanism must be used.
     * 
     * @param arrayDesign
     */
    protected ArrayDesign persistArrayDesign( ArrayDesign arrayDesign ) {
        if ( arrayDesign == null ) return null;
        if ( !isTransient( arrayDesign ) ) return arrayDesign;

        ArrayDesign existing = arrayDesignService.find( arrayDesign );

        if ( existing == null ) {
            log.debug( "Array Design " + arrayDesign + " is new, processing..." );
            return persistNewArrayDesign( arrayDesign );
        }

        return persistExistingArrayDesign( arrayDesign, existing );
    }

    /**
     * @param arrayDesign
     * @param existing
     * @return
     */
    private ArrayDesign persistExistingArrayDesign( ArrayDesign arrayDesign, ArrayDesign existing ) {
        if ( log.isDebugEnabled() ) {
            log.debug( "Array design \"" + existing.getName()
                    + "\" already exists in database, checking if update is needed." );
        }

        assert !isTransient( existing );

        int numExistingCompositeSequences = arrayDesignService.getCompositeSequenceCount( existing );
        int numCompositeSequencesInNew = arrayDesign.getCompositeSequences().size(); // in memory.

        int numExistingReporters = arrayDesignService.getReporterCount( existing );
        int numReportersInNew = arrayDesign.getReporters().size(); // in memory.

        if ( numExistingCompositeSequences >= numCompositeSequencesInNew && numExistingReporters >= numReportersInNew ) {
            if ( log.isInfoEnabled() ) {
                log.debug( "No action needed: Number of design elements in existing version of " + arrayDesign
                        + " is the same or greater (" + numExistingCompositeSequences
                        + " composite sequences in existing design, updated one has " + numCompositeSequencesInNew
                        + ", " + numExistingReporters + " reporters in existing design, updated one has "
                        + numReportersInNew + ")  No further processing of it will be done." );
            }
            arrayDesign = arrayDesignService.findOrCreate( existing );
            return arrayDesign;
        }

        if ( numExistingCompositeSequences < numCompositeSequencesInNew ) {
            log.info( "Update to array design needed: " + arrayDesign
                    + " exists but compositeSequences are to be updated (" + numExistingCompositeSequences
                    + " composite sequences in existing design, updated one has " + numCompositeSequencesInNew + ")" );

            existing.setCompositeSequences( arrayDesign.getCompositeSequences() );
            arrayDesign = existing;
            assert !isTransient( arrayDesign );
            persistArrayDesignCompositeSequenceAssociations( arrayDesign );
            existing.setCompositeSequences( arrayDesign.getCompositeSequences() );
            arrayDesignService.update( existing );
        }

        if ( numExistingReporters < numReportersInNew ) {
            log.debug( "Update to array design needed: " + arrayDesign + " exists but reporters are to be updated ("
                    + numExistingReporters + " reporters in existing design, updated one has " + numReportersInNew
                    + ")" );

            // FIXME - make this the same (more or less) as the composite sequence code.
            int count = 0;
            existing.setReporters( arrayDesign.getReporters() );
            arrayDesign = existing;
            assert !isTransient( arrayDesign );
            persistArrayDesignReporterAssociations( arrayDesign );
            for ( Reporter rep : arrayDesign.getReporters() ) {
                rep.setArrayDesign( existing );
                rep = ( Reporter ) persistDesignElement( rep );

                if ( ++count % SESSION_BATCH_SIZE == 0 ) {
                    this.flushAndClearSession();
                }
            }

            existing.setReporters( arrayDesign.getReporters() );
            arrayDesignService.update( existing );
        }
        arrayDesign = arrayDesignService.findOrCreate( existing );
        return arrayDesign;
    }

    /**
     * @param arrayDesign
     */
    @SuppressWarnings("unchecked")
    private ArrayDesign persistArrayDesignCompositeSequenceAssociations( ArrayDesign arrayDesign ) {
        if ( arrayDesign.getCompositeSequences().size() == 0 ) return arrayDesign;
        log.info( "Filling in or updating sequences in composite seqences for " + arrayDesign );
        int count = 0;

        List<CompositeSequence> listedCompositeSequences = new ArrayList<CompositeSequence>( arrayDesign
                .getCompositeSequences() );
        List<BioSequence> biosequences = new ArrayList<BioSequence>();
        for ( CompositeSequence cs : listedCompositeSequences ) {
            cs.setArrayDesign( arrayDesign );
            biosequences.add( cs.getBiologicalCharacteristic() );
        }
        arrayDesign.setCompositeSequences( null ); // temporarily.

        List<BioSequence> persistedBioSequences = new ArrayList<BioSequence>();
        log.info( "Persisting biosequences" );
        for ( BioSequence sequence : biosequences ) {

            // if the biosequence is already persistent, don't bother checking.
            if ( !isTransient( sequence ) ) {
                persistedBioSequences.add( sequence );
            } else {
                persistedBioSequences.add( persistBioSequence( sequence ) );
            }

            // flush a batch of inserts and release memory:
            if ( ++count % SESSION_BATCH_SIZE == 0 ) {
                this.flushAndClearSession();
            }
            if ( count % 500 == 0 ) {
                log.info( count + " biosequences created or updated for " + arrayDesign );
            }

        }
        log.info( "Persisted biosequences" );

        Iterator<BioSequence> itr = persistedBioSequences.iterator();
        for ( CompositeSequence cs : listedCompositeSequences ) {
            cs.setBiologicalCharacteristic( itr.next() );
            // FIXME what about reporters.
        }
        arrayDesign.setCompositeSequences( new HashSet<CompositeSequence>( listedCompositeSequences ) );

        log.info( "Refreshed BiologicalCharacteristic" );

        // batch it.
        arrayDesign.setCompositeSequences( compositeSequenceService.create( arrayDesign.getCompositeSequences() ) );
        return arrayDesign;
    }

    /**
     * @param arrayDesign
     */
    private ArrayDesign persistArrayDesignReporterAssociations( ArrayDesign arrayDesign ) {
        if ( arrayDesign.getReporters().size() == 0 ) {
            arrayDesign.setReporters( new HashSet<Reporter>() );
            return arrayDesign;
        }

        log.debug( "Filling in or updating sequences in reporters for " + arrayDesign );
        int persistedBioSequences = 0;

        for ( Reporter reporter : arrayDesign.getReporters() ) {
            reporter.setArrayDesign( arrayDesign );
        }

        for ( Reporter reporter : arrayDesign.getReporters() ) {
            reporter.setImmobilizedCharacteristic( persistBioSequence( reporter.getImmobilizedCharacteristic() ) );
            // reporter.setArrayDesign( arrayDesign );
            this.reporterService.create( reporter );

            // if ( ++persistedBioSequences % SESSION_BATCH_SIZE == 0 ) {
            // this.flushAndClearSession();
            // }

            if ( persistedBioSequences > 0 && persistedBioSequences % 5000 == 0 ) {
                log.info( persistedBioSequences + " reporter sequences examined for " + arrayDesign );
            }

        }

        if ( persistedBioSequences > 0 ) {
            log.info( persistedBioSequences + " reporter sequences examined for " + arrayDesign );
            // refreshCollections( arrayDesign );
            // this.flushAndClearSession();
        }

        return arrayDesign;
    }

    /**
     * @param designElement
     * @return
     */
    protected DesignElement persistDesignElement( DesignElement designElement ) {
        if ( designElement == null ) return null;

        if ( !isTransient( designElement ) ) return designElement;

        assert designElement.getArrayDesign() != null;

        designElement.setArrayDesign( persistArrayDesign( designElement.getArrayDesign() ) ); // get from cache
        // instead.

        if ( designElement instanceof CompositeSequence ) {
            if ( isTransient( ( ( CompositeSequence ) designElement ).getBiologicalCharacteristic() ) ) {
                ( ( CompositeSequence ) designElement )
                        .setBiologicalCharacteristic( persistBioSequence( ( ( CompositeSequence ) designElement )
                                .getBiologicalCharacteristic() ) );
            }
            return compositeSequenceService.findOrCreate( ( CompositeSequence ) designElement );
        } else if ( designElement instanceof Reporter ) {
            if ( isTransient( ( ( Reporter ) designElement ).getImmobilizedCharacteristic() ) ) {
                ( ( Reporter ) designElement )
                        .setImmobilizedCharacteristic( persistBioSequence( ( ( Reporter ) designElement )
                                .getImmobilizedCharacteristic() ) );
            }
            return reporterService.findOrCreate( ( Reporter ) designElement );
        } else {
            throw new IllegalArgumentException( "Unknown subclass of DesignElement" );
        }

    }

    /**
     * Persist an entirely new array design.
     * 
     * @param arrayDesign
     * @param existing
     * @return
     */
    protected ArrayDesign persistNewArrayDesign( ArrayDesign arrayDesign ) {
        assert isTransient( arrayDesign );

        if ( arrayDesign == null ) return null;

        log.info( "Persisting new array design " + arrayDesign.getName() );

        arrayDesign.setDesignProvider( persistContact( arrayDesign.getDesignProvider() ) );

        if ( arrayDesign.getLocalFiles() != null ) {
            for ( LocalFile file : arrayDesign.getLocalFiles() ) {
                // file.setId( persistLocalFile( file ).getId() );
                file = persistLocalFile( file );
            }
        }

        // bare-bones - we don't persist the probes by composition.
        // arrayDesign.setId( arrayDesignService.create( arrayDesign ).getId() );
        arrayDesign = arrayDesignService.create( arrayDesign );

        // arrayDesign.setId( persistArrayDesignReporterAssociations( arrayDesign ).getId() );
        persistArrayDesignReporterAssociations( arrayDesign );

        // arrayDesign.setId( persistArrayDesignCompositeSequenceAssociations( arrayDesign ).getId() );
        persistArrayDesignCompositeSequenceAssociations( arrayDesign );

        // arrayDesignService.update( arrayDesign );

        // arrayDesign.setId( arrayDesign.getId() );

        return arrayDesign;
    }

}
