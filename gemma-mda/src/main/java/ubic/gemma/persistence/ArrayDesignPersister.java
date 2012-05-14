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
package ubic.gemma.persistence;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignDao;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceDao;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * This class handles persisting array designs. This is a bit of a special case, because ArrayDesigns are very large
 * (with associated reporters, CompositeSequences, and BioSequences), and also very likely to be submitted more than
 * once to the system. Therefore we want to take care not to get multiple slightly different copies of them, but we also
 * don't want to have to spend an inordinate amount of time checking a submitted version against the database.
 * <p>
 * The association between ArrayDesign and DesignElement is compositional - the lifecycle of a designelement is tied to
 * the arraydesign. However, designelements have associations with biosequence, which have their own lifecycle, in
 * general.
 * 
 * @author pavlidis
 * @version $Id$
 */
abstract public class ArrayDesignPersister extends GenomePersister {

    protected static final String DESIGN_ELEMENT_KEY_SEPARATOR = ":::";

    @Autowired
    protected ArrayDesignDao arrayDesignDao;

    @Autowired
    protected CompositeSequenceDao compositeSequenceDao;

    private Map<String, ArrayDesign> arrayDesignCache = new HashMap<String, ArrayDesign>();

    private Map<String, CompositeSequence> designElementCache = new HashMap<String, CompositeSequence>();

    private Map<String, CompositeSequence> designElementSequenceCache = new HashMap<String, CompositeSequence>();

    public Map<String, CompositeSequence> getDesignElementCache() {
        return designElementCache;
    }

    public Map<String, CompositeSequence> getDesignElementSequenceCache() {
        return designElementSequenceCache;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.persister.Persister#persist(java.lang.Object)
     */
    @Override
    public Object persist( Object entity ) {
        Object result;

        if ( entity instanceof ArrayDesign ) {
            result = findOrPersistArrayDesign( ( ArrayDesign ) entity );
            clearArrayDesignCache();
            return result;
        }

        return super.persist( entity );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.CommonPersister#persistOrUpdate(java.lang.Object)
     */
    @Override
    public Object persistOrUpdate( Object entity ) {
        if ( entity == null ) return null;
        return super.persistOrUpdate( entity );
    }

    /**
     * @param arrayDesignDao The arrayDesignDao to set.
     */
    public void setArrayDesignDao( ArrayDesignDao arrayDesignDao ) {
        this.arrayDesignDao = arrayDesignDao;
    }

    /**
     * @param compositeSequenceDao The compositeSequenceDao to set.
     */
    public void setCompositeSequenceDao( CompositeSequenceDao compositeSequenceDao ) {
        this.compositeSequenceDao = compositeSequenceDao;
    }

    /**
     * @param designElement
     * @return
     */
    protected CompositeSequence addNewDesignElementToPersistentArrayDesign( ArrayDesign arrayDesign,
            CompositeSequence designElement ) {
        if ( designElement == null ) return null;

        if ( !isTransient( designElement ) ) return designElement;

        assert arrayDesign.getId() != null;

        designElement.setArrayDesign( arrayDesign );

        if ( isTransient( designElement.getBiologicalCharacteristic() ) ) {
            designElement
                    .setBiologicalCharacteristic( persistBioSequence( designElement.getBiologicalCharacteristic() ) );
        }
        CompositeSequence persistedDE = compositeSequenceDao.create( designElement );

        arrayDesign.getCompositeSequences().add( persistedDE );

        /*
         * FIXME This is VERY slow if we have to add a lot of designelements to the array.
         */
        this.arrayDesignDao.update( arrayDesign );

        return persistedDE;

    }

    /**
     * Cache array design design elements (used for associating with ExpressionExperiments)
     * <p>
     * Note that reporters are ignored, as we are not persisting them.
     * 
     * @param arrayDesign To add to the cache.
     * @see ExpressionPersister.fillInDesignElementDataVectorAssociations
     */
    protected void addToDesignElementCache( final ArrayDesign arrayDesign ) {
        StopWatch timer = new StopWatch();
        timer.start();
        assert !isTransient( arrayDesign );

        log.info( "Loading sequence elements for " + arrayDesign );

        final Collection<CompositeSequence> compositeSequences = arrayDesignDao.thaw( arrayDesign )
                .getCompositeSequences();

        // final Collection<CompositeSequence> compositeSequences = arrayDesignDao.loadCompositeSequences( arrayDesign
        // .getId() );

        String adName = DESIGN_ELEMENT_KEY_SEPARATOR + arrayDesign.getName();
        int count = 0;
        for ( CompositeSequence element : compositeSequences ) {
            assert element.getId() != null;
            designElementCache.put( element.getName() + adName, element );
            BioSequence seq = element.getBiologicalCharacteristic();
            if ( seq != null ) {
                if ( StringUtils.isNotBlank( seq.getName() ) ) {
                    designElementSequenceCache.put( seq.getName(), element );
                }
            }
            if ( ++count % 20000 == 0 ) {
                log.info( "Cached " + count + " probes (" + timer.getTime() + "ms)" );
            }
            if ( count % 100 == 0 ) {
                // session.clear();
            }
        }

        log.info( timer.getTime() + "ms elapsed" );

    }

    /**
     * Put an array design in the cache. This is needed when loading designelementdatavectors, for example, to avoid
     * repeated (and one-at-a-time) fetching of designelement.
     * 
     * @param arrayDesignCache
     * @param cacheIsSetUp
     * @param designElementCache
     * @param arrayDesign
     * @return the persistent array design.
     */
    protected ArrayDesign loadOrPersistArrayDesignAndAddToCache( ArrayDesign arrayDesign ) {
        assert arrayDesign != null;
        ArrayDesign cachedAd = arrayDesign;
        if ( !arrayDesignCache.containsKey( cachedAd.getName() )
                && !( cachedAd.getShortName() != null && arrayDesignCache.containsKey( cachedAd.getShortName() ) ) ) {
            cachedAd = findOrPersistArrayDesign( arrayDesign );
            // cachedAd = arrayDesignDao.thaw( cachedAd );
            assert !isTransient( cachedAd );
            addToDesignElementCache( cachedAd );
            arrayDesignCache.put( arrayDesign.getName(), cachedAd );
            if ( cachedAd.getShortName() != null ) {
                arrayDesignCache.put( cachedAd.getShortName(), cachedAd );
            }
        }
        if ( arrayDesignCache.containsKey( cachedAd.getName() ) ) {
            return arrayDesignCache.get( cachedAd.getName() );
        }
        return arrayDesignCache.get( cachedAd.getShortName() );

    }

    /**
     * 
     *
     */
    protected void clearArrayDesignCache() {
        log.debug( "Clearing cache" );
        this.arrayDesignCache.clear();
        this.designElementCache.clear();
        this.designElementSequenceCache.clear();
    }

    /**
     * Persist an array design.
     * 
     * @param arrayDesign
     */
    protected ArrayDesign findOrPersistArrayDesign( ArrayDesign arrayDesign ) {
        if ( arrayDesign == null ) return null;

        if ( !isTransient( arrayDesign ) ) return arrayDesign;

        /*
         * Note we don't do a full find here.
         */
        ArrayDesign existing = arrayDesignDao.find( arrayDesign );

        if ( existing == null ) {

            /*
             * Try less stringent search.
             */
            existing = arrayDesignDao.findByShortName( arrayDesign.getShortName() );

            if ( existing == null ) {
                log.info( arrayDesign + " is new, processing..." );
                return persistNewArrayDesign( arrayDesign );
            }

            log.info( "Array design exactly matching " + arrayDesign + " doesn't exist, but found " + existing
                    + "; returning" );

        } else {
            log.info( "Array Design " + arrayDesign + " already exists, returning..." );
        }

        return existing;

    }

    /**
     * Persist an entirely new array design, including composite sequences and any associated new sequences.
     * 
     * @param arrayDesign
     * @return
     */
    protected ArrayDesign persistNewArrayDesign( ArrayDesign arrayDesign ) {
        assert isTransient( arrayDesign );

        if ( arrayDesign == null ) return null;

        log.info( "Persisting new array design " + arrayDesign.getName() );

        if ( arrayDesign.getDesignProvider() != null )
            arrayDesign.setDesignProvider( persistContact( arrayDesign.getDesignProvider() ) );

        if ( arrayDesign.getLocalFiles() != null ) {
            for ( LocalFile file : arrayDesign.getLocalFiles() ) {
                file = persistLocalFile( file );
            }
        }

        if ( arrayDesign.getPrimaryTaxon() == null ) {
            throw new IllegalArgumentException( "Primary taxon cannot be null" );
        }

        arrayDesign.setPrimaryTaxon( ( Taxon ) persist( arrayDesign.getPrimaryTaxon() ) );

        for ( DatabaseEntry externalRef : arrayDesign.getExternalReferences() ) {
            externalRef.setExternalDatabase( persistExternalDatabase( externalRef.getExternalDatabase() ) );
        }

        log.info( "Persisting " + arrayDesign );

        arrayDesign = persistArrayDesignCompositeSequenceAssociations( arrayDesign );

        arrayDesign = arrayDesignDao.create( arrayDesign );

        if ( Thread.currentThread().isInterrupted() ) {
            log.info( "Cancelled" );
            /*
             * FIXME this shouldn't be necessary as this method now runs in a transaction.
             */
            arrayDesignDao.remove( arrayDesign );
            throw new CancellationException(
                    "Thread was terminated during the final stage of persisting the arraydesign. " + this.getClass() );
        }

        return arrayDesign;
    }

    /**
     * @param arrayDesign
     */
    private ArrayDesign persistArrayDesignCompositeSequenceAssociations( ArrayDesign arrayDesign ) {
        int numElements = arrayDesign.getCompositeSequences().size();
        if ( numElements == 0 ) return arrayDesign;
        log.info( "Filling in or updating sequences in composite seqences for " + arrayDesign );

        int persistedBioSequences = 0;

        assert arrayDesign.getId() == null;
        int numElementsPerUpdate = numElementsPerUpdate( arrayDesign.getCompositeSequences() );
        for ( CompositeSequence compositeSequence : arrayDesign.getCompositeSequences() ) {

            compositeSequence.setArrayDesign( arrayDesign );

            compositeSequence.setBiologicalCharacteristic( persistBioSequence( compositeSequence
                    .getBiologicalCharacteristic() ) );

            if ( ++persistedBioSequences % numElementsPerUpdate == 0 && numElements > 1000 ) {
                log.info( persistedBioSequences + "/" + numElements + " compositeSequence sequences examined for "
                        + arrayDesign );
            }

        }

        if ( persistedBioSequences > 0 ) {
            log.info( "Total of " + persistedBioSequences + " compositeSequence sequences examined for " + arrayDesign );
        }

        return arrayDesign;
    }

}
