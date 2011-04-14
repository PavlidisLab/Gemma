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

import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;

import ubic.basecode.util.CancellationException;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceImpl;

/**
 * This class handles persisting array designs. This is a bit of a special case, because Arraydesigns are very large
 * (with associated reporters, compositesequences, and biosequences), and also very likely to be submitted more than
 * once to the system. Therefore we want to take care not to get multiple slighly different copies of them, but we also
 * don't want to have to spend an inordinate amount of time checking a submitted version against the database.
 * <p>
 * The association between Arraydesign and DesignElement is compositional - the lifecycle of a designelement is tied to
 * the arraydesign. However, designelements have associations with biosequence, which have their own lifecycle, in
 * general.
 * 
 * @author pavlidis
 * @version $Id$
 */
abstract public class ArrayDesignPersister extends GenomePersister {

    protected static final String DESIGN_ELEMENT_KEY_SEPARATOR = ":::";

    @Autowired
    protected ArrayDesignService arrayDesignService;

    @Autowired
    protected CompositeSequenceService compositeSequenceService;

    private Map<String, ArrayDesign> arrayDesignCache = new HashMap<String, ArrayDesign>();

    Map<String, CompositeSequence> designElementCache = new HashMap<String, CompositeSequence>();

    Map<String, CompositeSequence> designElementSequenceCache = new HashMap<String, CompositeSequence>();

    public ArrayDesignPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

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
            result = persistArrayDesign( ( ArrayDesign ) entity );
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
        CompositeSequence persistedDE = compositeSequenceService.create( designElement );

        arrayDesign.getCompositeSequences().add( persistedDE );

        /*
         * FIXME This is VERY slow if we have to add a lot of designelements to the array.
         */
        this.arrayDesignService.update( arrayDesign );

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

        assert !isTransient( arrayDesign );

        log.info( "Caching array design elements for " + arrayDesign );

        int startCacheSize = designElementCache.keySet().size();
        this.getHibernateTemplate().executeWithNativeSession( new HibernateCallback<Object>() {

            // We need to do this in hibernate to thaw the biosequences.
            public Object doInHibernate( Session session ) throws HibernateException {
                final Collection<CompositeSequence> compositeSequences = arrayDesignService
                        .loadCompositeSequences( arrayDesign );

                String adName = DESIGN_ELEMENT_KEY_SEPARATOR + arrayDesign.getName();
                int count = 0;
                for ( CompositeSequence element : compositeSequences ) {
                    assert element.getId() != null;
                    designElementCache.put( element.getName() + adName, element );
                    BioSequence seq = element.getBiologicalCharacteristic();
                    if ( seq != null ) {
                        seq = ( BioSequence ) session.get( BioSequenceImpl.class, seq.getId() );
                        if ( StringUtils.isNotBlank( seq.getName() ) ) {
                            designElementSequenceCache.put( seq.getName(), element );
                        }
                    }
                    if ( ++count % 20000 == 0 ) {
                        log.info( "Cached " + count + " probes" );
                    }
                    if ( count % 100 == 0 ) {
                        session.clear();
                    }
                }
                return null;
            }
        } );

        int endCacheSize = designElementCache.keySet().size();
        log.debug( "Filled cache with " + ( endCacheSize - startCacheSize ) + " elements." );
    }

    /**
     * Put an array design in the cache. This is needed when loading designelementdatavectors, for example, to avoid
     * repeated (and one-at-a-time) fetching of designelement.
     * 
     * @param arrayDesignCache
     * @param cacheIsSetUp
     * @param designElementCache
     * @param ad
     * @return the persistent array design.
     */
    protected ArrayDesign cacheArrayDesign( ArrayDesign ad ) {
        assert ad != null;
        ArrayDesign cachedAd = ad;
        if ( !arrayDesignCache.containsKey( cachedAd.getName() )
                && !( cachedAd.getShortName() != null && arrayDesignCache.containsKey( cachedAd.getShortName() ) ) ) {
            cachedAd = persistArrayDesign( ad );
            cachedAd = arrayDesignService.thaw( cachedAd );
            assert !isTransient( cachedAd );
            addToDesignElementCache( cachedAd );
            arrayDesignCache.put( ad.getName(), cachedAd );
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
    protected ArrayDesign persistArrayDesign( ArrayDesign arrayDesign ) {
        if ( arrayDesign == null ) return null;

        if ( !isTransient( arrayDesign ) ) return arrayDesign;

        /*
         * Note we don't do a full find here.
         */
        ArrayDesign existing = arrayDesignService.find( arrayDesign );

        if ( existing == null ) {

            /*
             * Try less stringent search.
             */
            existing = arrayDesignService.findByShortName( arrayDesign.getShortName() );

            if ( existing == null ) {
                log.info( arrayDesign + " is new, processing..." );
                return persistNewArrayDesign( arrayDesign );
            }

            log.info( "Array design exactly matching " + arrayDesign + " doesn't exist, but found " + existing
                    + "; returning" );

        } else {
            log.info( "Array Design " + arrayDesign + " already exists, returning..." );
        }

        existing = arrayDesignService.thawLite( existing );
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

        Collection<CompositeSequence> c = arrayDesign.getCompositeSequences();
        arrayDesign.setCompositeSequences( null ); // so we can perist it.
        int count = 0;
        long startTime = System.currentTimeMillis();
        int numElementsPerUpdate = numElementsPerUpdate( c );
        for ( CompositeSequence sequence : c ) {
            sequence.setBiologicalCharacteristic( persistBioSequence( sequence.getBiologicalCharacteristic() ) );
            if ( ++count % numElementsPerUpdate == 0 && log.isInfoEnabled() && c.size() > 10000 ) {
                log.info( count + " compositeSequence biologicalCharacteristics checked for " + arrayDesign
                        + "( elapsed time=" + elapsedMinutes( startTime ) + " minutes)" );

            }
            if ( count % SESSION_BATCH_SIZE == 0 ) {
                this.getHibernateTemplate().flush();
                this.getHibernateTemplate().clear();
                if ( Thread.currentThread().isInterrupted() ) {
                    log.info( "Cancelled" );
                    /*
                     * TODO after cancelling, we should clean up after ourselves (need to remove all the sequences that
                     * were all ready persisted this will try to remove the entire collection but only some are in the
                     * DB. Not sure how the collection delete will handle deletion of transtive objects not in db. might
                     * need to fix.
                     */
                    compositeSequenceService.remove( c ); // etc
                    throw new CancellationException( "Thread was terminated during persisting the arraydesign. "
                            + this.getClass() );
                }
            }
        }

        if ( log.isInfoEnabled() && count > MINIMUM_COLLECTION_SIZE_FOR_NOTFICATIONS ) {
            log.info( count + " compositeSequence biologicalCharacteristics checked for " + arrayDesign
                    + "( elapsed time=" + elapsedMinutes( startTime ) + " minutes)" );
        }

        arrayDesign.setCompositeSequences( null );

        arrayDesign = arrayDesignService.create( arrayDesign );

        arrayDesign.setCompositeSequences( c );

        arrayDesign = persistArrayDesignCompositeSequenceAssociations( arrayDesign );

        log.info( "Persisting " + arrayDesign );

        arrayDesignService.update( arrayDesign );

        if ( Thread.currentThread().isInterrupted() ) {
            log.info( "Cancelled" );
            // TODO after cancelling, we should clean up after ourselves.
            arrayDesignService.remove( arrayDesign ); // etc
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

        assert arrayDesign.getId() != null;
        int numElementsPerUpdate = numElementsPerUpdate( arrayDesign.getCompositeSequences() );
        for ( CompositeSequence compositeSequence : arrayDesign.getCompositeSequences() ) {

            compositeSequence.setArrayDesign( arrayDesign );

            compositeSequence.setBiologicalCharacteristic( persistBioSequence( compositeSequence
                    .getBiologicalCharacteristic() ) );

            if ( ++persistedBioSequences % numElementsPerUpdate == 0 && numElements > 1000 ) {
                log.info( persistedBioSequences + "/" + numElements + " compositeSequence sequences examined for "
                        + arrayDesign );
            }

            if ( persistedBioSequences % SESSION_BATCH_SIZE == 0 ) {
                this.getHibernateTemplate().flush();
            }

        }

        if ( persistedBioSequences > 0 ) {
            log.info( "Total of " + persistedBioSequences + " compositeSequence sequences examined for " + arrayDesign );
        }

        return arrayDesign;
    }

}
