/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.persistence;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * Sets up the array designs.
 * 
 * @author paul
 * @version $Id$
 */
@Component
public class ExpressionExperimentPrePersistServiceImpl implements ExpressionExperimentPrePersistService {

    private static Log log = LogFactory.getLog( ExpressionExperimentPrePersistServiceImpl.class );

    @Autowired
    private Persister persisterHelper;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private CompositeSequenceService compositeSequenceService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.ExpressionExperimentPrePersistService#prepare(ubic.gemma.model.expression.experiment.
     * ExpressionExperiment)
     */
    @Override
    public ArrayDesignsForExperimentCache prepare( ExpressionExperiment ee ) {
        ArrayDesignsForExperimentCache c = new ArrayDesignsForExperimentCache();

        for ( DesignElementDataVector dataVector : ee.getRawExpressionDataVectors() ) {
            CompositeSequence probe = dataVector.getDesignElement();

            assert probe != null;

            ArrayDesign arrayDesign = probe.getArrayDesign();
            assert arrayDesign != null : probe + " does not have an array design";

            arrayDesign = loadOrPersistArrayDesignAndAddToCache( arrayDesign, c );

            String key = probe.getName() + ArrayDesignsForExperimentCache.DESIGN_ELEMENT_KEY_SEPARATOR
                    + arrayDesign.getName();
            String seqName = null;

            if ( probe.getBiologicalCharacteristic() != null ) {
                seqName = probe.getBiologicalCharacteristic().getName();
            }

            if ( log.isDebugEnabled() ) log.debug( "Seeking design element matching key=" + key );
            if ( c.getDesignElementCache().containsKey( key ) ) {
                probe = c.getDesignElementCache().get( key );
                if ( log.isDebugEnabled() ) log.debug( "Found " + probe + " with key=" + key );
            } else {
                /*
                 * Because the names of design elements can change, we should try to go by the _sequence_.
                 */
                if ( StringUtils.isNotBlank( seqName ) && c.getDesignElementSequenceCache().containsKey( seqName ) ) {
                    if ( log.isDebugEnabled() )
                        log.debug( "Using sequence name " + seqName + " to identify sequence" );
                    probe = c.getDesignElementSequenceCache().get( seqName );
                    if ( log.isDebugEnabled() ) log.debug( "Found " + probe + " with sequence key=" + seqName );
                } else {

                    probe = addNewDesignElementToPersistentArrayDesign( arrayDesign, probe );
                }
            }

            assert probe != null && probe.getId() != null;
            dataVector.setDesignElement( probe );

        }
        return c;
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
    protected ArrayDesign loadOrPersistArrayDesignAndAddToCache( ArrayDesign arrayDesign,
            ArrayDesignsForExperimentCache c ) {

        assert arrayDesign != null;

        assert StringUtils.isNotBlank( arrayDesign.getShortName() );

        if ( c.getArrayDesignCache().containsKey( arrayDesign.getShortName() ) ) {
            return c.getArrayDesignCache().get( arrayDesign.getShortName() );
        }

        StopWatch timer = new StopWatch();
        timer.start();

        // transaction, but fast if the design already exists.
        arrayDesign = ( ArrayDesign ) persisterHelper.persist( arrayDesign );

        // transaction (read-only)
        arrayDesign = arrayDesignService.thaw( arrayDesign ); // horrible.

        addToDesignElementCache( arrayDesign, c );

        c.getArrayDesignCache().put( arrayDesign.getShortName(), arrayDesign );

        if ( timer.getTime() > 20000 ) {
            log.info( "Load/persist & thaw array design: " + timer.getTime() + "ms" );
        }

        return arrayDesign;
    }

    /**
     * FIXME This is VERY slow if we have to add a lot of DesignElements to the array.
     * 
     * @param designElement
     * @return
     */
    private CompositeSequence addNewDesignElementToPersistentArrayDesign( ArrayDesign arrayDesign,
            CompositeSequence designElement ) {
        if ( designElement == null ) return null;

        if ( !PersisterHelper.isTransient( designElement ) ) return designElement;

        /*
         * No sequence, or the sequence name isn't provided. Of course, if there is no sequence it isn't going to be
         * very useful.
         */
        BioSequence biologicalCharacteristic = designElement.getBiologicalCharacteristic();
        log.warn( "Adding new probe to existing array design " + arrayDesign.getShortName() + ": " + designElement
                + " bioseq=" + biologicalCharacteristic );

        assert arrayDesign.getId() != null;

        designElement.setArrayDesign( arrayDesign );

        if ( PersisterHelper.isTransient( biologicalCharacteristic ) ) {
            // transaction.
            designElement.setBiologicalCharacteristic( ( BioSequence ) persisterHelper
                    .persist( biologicalCharacteristic ) );

            log.info( "Got new sequence for probe" );

        }

        // transaction.
        CompositeSequence persistedDE = compositeSequenceService.create( designElement );

        log.info( "Created probe" );
        arrayDesign.getCompositeSequences().add( persistedDE );

        log.info( "Added it to the array" );

        // transaction.
        this.arrayDesignService.update( arrayDesign );

        log.info( "Updated the array" );

        return persistedDE;

    }

    /**
     * Cache array design design elements (used for associating with ExpressionExperiments)
     * <p>
     * Note that reporters are ignored, as we are not persisting them.
     * 
     * @param arrayDesign To add to the cache, must be thawed already.
     * @param c cache
     */
    private void addToDesignElementCache( final ArrayDesign arrayDesign, ArrayDesignsForExperimentCache c ) {
        StopWatch timer = new StopWatch();
        timer.start();

        String adName = ArrayDesignsForExperimentCache.DESIGN_ELEMENT_KEY_SEPARATOR + arrayDesign.getName();
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            assert cs.getId() != null;
            c.getDesignElementCache().put( cs.getName() + adName, cs );
            BioSequence seq = cs.getBiologicalCharacteristic();
            if ( seq != null ) {
                if ( StringUtils.isNotBlank( seq.getName() ) ) {
                    c.getDesignElementSequenceCache().put( seq.getName(), cs );
                }
            }
        }

    }
}
