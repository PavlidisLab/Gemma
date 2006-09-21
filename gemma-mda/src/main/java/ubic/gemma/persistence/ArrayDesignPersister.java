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
import java.util.HashSet;
import java.util.Map;

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
        }

        Collection<DesignElement> reporters = arrayDesignService.loadReporters( arrayDesign );

        for ( DesignElement element : reporters ) {
            assert element.getId() != null;
            designElementCache.put( element.getName() + adName, element );
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

    // /**
    // * @param persistentDesignElement
    // * @param maybeExistingDesignElement
    // * @param key
    // * @return
    // */
    // protected DesignElement getPersistentDesignElement( DesignElement persistentDesignElement,
    // DesignElement maybeExistingDesignElement, String key ) {
    // if ( maybeExistingDesignElement instanceof CompositeSequence ) {
    // persistentDesignElement = persistDesignElement( maybeExistingDesignElement );
    // } else if ( maybeExistingDesignElement instanceof Reporter ) {
    // persistentDesignElement = persistDesignElement( maybeExistingDesignElement );
    // }
    // if ( persistentDesignElement == null ) {
    // throw new IllegalStateException( maybeExistingDesignElement + " does not have a persistent version" );
    // }
    //
    // designElementCache.put( key, persistentDesignElement );
    // return persistentDesignElement;
    // }
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

        // throw new IllegalArgumentException( "Array design \"" + existing.getName() + "\" already exists in database."
        // );
        // updateArrayDesign( existing, arrayDesign );

        return existing;

    }

    // /**
    // * @param existing
    // * @param arrayDesign
    // */
    // protected void updateArrayDesign( ArrayDesign existing, ArrayDesign arrayDesign ) {
    //
    // assert existing.getId() != null;
    //
    // if ( StringUtils.isNotBlank( arrayDesign.getName() ) ) existing.setName( arrayDesign.getName() );
    //
    // if ( StringUtils.isNotBlank( arrayDesign.getDescription() ) )
    // existing.setDescription( arrayDesign.getDescription() );
    //
    // // manufacturer. Replace it.
    // if ( arrayDesign.getDesignProvider() != null ) {
    // existing.setDesignProvider( persistContact( arrayDesign.getDesignProvider() ) );
    // }
    //
    // if ( arrayDesign.getAdvertisedNumberOfDesignElements() != null ) {
    // existing.setAdvertisedNumberOfDesignElements( arrayDesign.getAdvertisedNumberOfDesignElements() );
    // }
    //
    // // localfiles. We add them.
    // for ( LocalFile file : arrayDesign.getLocalFiles() ) {
    // existing.getLocalFiles().add( persistLocalFile( file ) );
    // }
    //
    // // designelement, biosequences.
    // existing.getCompositeSequences().clear();
    // existing.getReporters().clear();
    //
    // for ( CompositeSequence cs : existing.getCompositeSequences() ) {
    // cs.setArrayDesign( existing );
    // existing.getCompositeSequences().add( cs );
    // cs.setBiologicalCharacteristic( persistBioSequence( cs.getBiologicalCharacteristic() ) );
    // }
    //
    // for ( Reporter rep : existing.getReporters() ) {
    // rep.setArrayDesign( existing );
    // existing.getReporters().add( rep );
    // rep.setImmobilizedCharacteristic( persistBioSequence( rep.getImmobilizedCharacteristic() ) );
    // }
    //
    // arrayDesignService.update( existing );
    // }

    /**
     * @param arrayDesign
     */
    @SuppressWarnings("unchecked")
    private ArrayDesign persistArrayDesignCompositeSequenceAssociations( ArrayDesign arrayDesign ) {
        if ( arrayDesign.getCompositeSequences().size() == 0 ) return arrayDesign;
        log.info( "Filling in or updating sequences in composite seqences for " + arrayDesign );

        // int persistedBioSequences = 0;

        assert arrayDesign.getId() != null;
        for ( CompositeSequence compositeSequence : arrayDesign.getCompositeSequences() ) {
            compositeSequence.setArrayDesign( arrayDesign );
        }

        // for ( CompositeSequence compositeSequence : arrayDesign.getCompositeSequences() ) {
        // compositeSequence.setBiologicalCharacteristic( persistBioSequence( compositeSequence
        // .getBiologicalCharacteristic() ) );
        //
        // if ( ++persistedBioSequences % 5000 == 0 && log.isInfoEnabled() ) {
        // log.info( persistedBioSequences + " compositeSequence sequences examined for " + arrayDesign );
        // }
        //
        // }
        //
        // if ( persistedBioSequences > 0 ) {
        // log.info( persistedBioSequences + " compositeSequence sequences examined for " + arrayDesign );
        // }

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

        assert arrayDesign.getId() != null;
        for ( Reporter reporter : arrayDesign.getReporters() ) {
            reporter.setArrayDesign( arrayDesign );
        }

        for ( Reporter reporter : arrayDesign.getReporters() ) {
            reporter.setImmobilizedCharacteristic( persistBioSequence( reporter.getImmobilizedCharacteristic() ) );

            if ( ++persistedBioSequences % 5000 == 0 && log.isInfoEnabled() ) {
                log.info( persistedBioSequences + " reporter sequences examined for " + arrayDesign );
            }
        }

        if ( persistedBioSequences > 0 ) {
            log.info( persistedBioSequences + " reporter sequences examined for " + arrayDesign );
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
            arrayDesign.getCompositeSequences().add( ( CompositeSequence ) designElement );

        } else if ( designElement instanceof Reporter ) {
            if ( isTransient( ( ( Reporter ) designElement ).getImmobilizedCharacteristic() ) ) {
                ( ( Reporter ) designElement )
                        .setImmobilizedCharacteristic( persistBioSequence( ( ( Reporter ) designElement )
                                .getImmobilizedCharacteristic() ) );
            }
            designElement = reporterService.create( ( Reporter ) designElement );
            arrayDesign.getReporters().add( ( Reporter ) designElement );
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

        log.debug( "Persisting new array design " + arrayDesign.getName() );

        arrayDesign.setDesignProvider( persistContact( arrayDesign.getDesignProvider() ) );

        if ( arrayDesign.getLocalFiles() != null ) {
            for ( LocalFile file : arrayDesign.getLocalFiles() ) {
                file = persistLocalFile( file );
            }
        }
        
        Collection<CompositeSequence> c = arrayDesign.getCompositeSequences();
        arrayDesign.setCompositeSequences( null ); // so we can perist it.
        for ( CompositeSequence sequence : c ) {
            sequence.setBiologicalCharacteristic( persistBioSequence( sequence.getBiologicalCharacteristic() ) );
        }

        Collection<Reporter> r = arrayDesign.getReporters();
        arrayDesign.setReporters( null );
     

        arrayDesign = arrayDesignService.create( arrayDesign );

        // may need to flush before update?
        arrayDesign.setCompositeSequences( c ); // this is enough to trigger an update of arrayDesign?
        arrayDesign.setReporters( r );
        arrayDesign = persistArrayDesignReporterAssociations( arrayDesign );
        arrayDesign = persistArrayDesignCompositeSequenceAssociations( arrayDesign );

        arrayDesignService.update( arrayDesign );

        return arrayDesign;
    }
}
