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

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Session;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.designElement.Reporter;
import ubic.gemma.model.expression.designElement.ReporterService;

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
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 * @spring.property name="compositeSequenceService" ref="compositeSequenceService"
 * @spring.property name="reporterService" ref="reporterService"
 * @author pavlidis
 * @version $Id$
 */
abstract public class ArrayDesignPersister extends GenomePersister {

    protected static final String DESIGN_ELEMENT_KEY_SEPARATOR = ":::";

    protected ArrayDesignService arrayDesignService;

    protected CompositeSequenceService compositeSequenceService;

    protected ReporterService reporterService;

    Map<String, ArrayDesign> arrayDesignCache = new HashMap<String, ArrayDesign>();

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

        assert !isTransient( arrayDesign );

        log.debug( "Loading array design elements for " + arrayDesign );

        Collection<DesignElement> compositeSequences = arrayDesignService.loadCompositeSequences( arrayDesign );

        int startCacheSize = designElementCache.keySet().size();
        String adName = DESIGN_ELEMENT_KEY_SEPARATOR + arrayDesign.getName();
        for ( DesignElement element : compositeSequences ) {
            assert element.getId() != null;
            designElementCache.put( element.getName() + adName, element );

            Collection<Reporter> reporters = ( ( CompositeSequence ) element ).getComponentReporters();
            for ( DesignElement de : reporters ) {
                assert de.getId() != null;
                designElementCache.put( de.getName() + adName, de );

            }

        }

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
        if ( !arrayDesignCache.containsKey( ad.getName() ) ) {
            ad = persistArrayDesign( ad );
            assert !isTransient( ad );
            addToDesignElementCache( ad );
            arrayDesignCache.put( ad.getName(), ad );
        }
        return arrayDesignCache.get( ad.getName() );
    }

    /**
     * Persist an array design.
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

        return existing;

    }

    /**
     * @param arrayDesign
     */
    @SuppressWarnings("unchecked")
    private ArrayDesign persistArrayDesignCompositeSequenceAssociations( ArrayDesign arrayDesign ) {
        if ( arrayDesign.getCompositeSequences().size() == 0 ) return arrayDesign;
        log.info( "Filling in or updating sequences in composite seqences for " + arrayDesign );

        int persistedBioSequences = 0;

        assert arrayDesign.getId() != null;
        int numElementsPerUpdate = numElementsPerUpdate( arrayDesign.getCompositeSequences() );
        for ( CompositeSequence compositeSequence : arrayDesign.getCompositeSequences() ) {

            compositeSequence.setArrayDesign( arrayDesign );

            compositeSequence.setBiologicalCharacteristic( persistBioSequence( compositeSequence
                    .getBiologicalCharacteristic() ) );

            if ( ++persistedBioSequences % numElementsPerUpdate == 0 ) {
                log.info( persistedBioSequences + " compositeSequence sequences examined for " + arrayDesign );
            }

            if ( persistedBioSequences % SESSION_BATCH_SIZE == 0 ) {
                this.getCurrentSession().flush();
            }

        }

        if ( persistedBioSequences > 0 ) {
            log.info( "Total of " + persistedBioSequences + " compositeSequence sequences examined for " + arrayDesign );
        }

        return arrayDesign;
    }

    /**
     * Note: Update is not called on the array design.
     * 
     * @param designElement
     * @return
     */
    protected DesignElement addNewDesignElementToPersistentArrayDesign( ArrayDesign arrayDesign,
            DesignElement designElement ) {
        if ( designElement == null ) return null;

        if ( !isTransient( designElement ) ) return designElement;

        assert arrayDesign.getId() != null;

        designElement.setArrayDesign( arrayDesign );

        if ( designElement instanceof CompositeSequence ) {
            if ( isTransient( ( ( CompositeSequence ) designElement ).getBiologicalCharacteristic() ) ) {
                ( ( CompositeSequence ) designElement )
                        .setBiologicalCharacteristic( persistBioSequence( ( ( CompositeSequence ) designElement )
                                .getBiologicalCharacteristic() ) );
            }
            designElement = compositeSequenceService.create( ( CompositeSequence ) designElement );

            try {
                Hibernate.initialize( arrayDesign.getCompositeSequences() );
            } catch ( HibernateException e ) {
                Session sess = this.getCurrentSession();
                sess.lock( arrayDesign, LockMode.NONE );
            }
            arrayDesign.getCompositeSequences().add( ( CompositeSequence ) designElement );

        } else {
            throw new IllegalArgumentException( "Unknown subclass of DesignElement" );
        }

        return designElement;

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
                file = persistLocalFile( file );
            }
        }

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
            if ( ++count % numElementsPerUpdate == 0 && log.isInfoEnabled() ) {
                log.info( count + " compositeSequence biologicalCharacteristics checked for " + arrayDesign
                        + "( elapsed time=" + elapsedMinutes( startTime ) + " minutes)" );
            }
            if ( count % SESSION_BATCH_SIZE == 0 ) {
                this.getCurrentSession().flush();
                this.getCurrentSession().clear();
            }
        }

        log.info( count + " compositeSequence biologicalCharacteristics checked for " + arrayDesign + "( elapsed time="
                + elapsedMinutes( startTime ) + " minutes)" );

        arrayDesign.setCompositeSequences( null );

        arrayDesign = arrayDesignService.create( arrayDesign );

        arrayDesign.setCompositeSequences( c );

        Map<String, Collection<Reporter>> csNameReporterMap = new HashMap<String, Collection<Reporter>>();
        for ( CompositeSequence sequence : arrayDesign.getCompositeSequences() ) {
            if ( csNameReporterMap.containsKey( sequence.getName() ) ) {
                throw new IllegalStateException( "Two composite sequences share a name " + sequence.getName() );
            }
            csNameReporterMap.put( sequence.getName(), sequence.getComponentReporters() );
            sequence.setComponentReporters( null );
        }

        arrayDesign = persistArrayDesignCompositeSequenceAssociations( arrayDesign );

        arrayDesignService.update( arrayDesign );

        // now have persistent CS
        for ( CompositeSequence sequence : arrayDesign.getCompositeSequences() ) {
            sequence.setComponentReporters( csNameReporterMap.get( sequence.getName() ) );
            for ( Reporter reporter : sequence.getComponentReporters() ) {
                reporter.setCompositeSequence( sequence );
            }
        }

        arrayDesignService.update( arrayDesign );
        return arrayDesign;
    }

}
