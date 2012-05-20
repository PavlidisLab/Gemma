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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
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

        Map<ArrayDesign, Collection<CompositeSequence>> newprobes = new HashMap<ArrayDesign, Collection<CompositeSequence>>();
        Collection<DesignElementDataVector> dataVectorsThatNeedNewProbes = new HashSet<DesignElementDataVector>();

        /*
         * First time through.
         */
        Collection<RawExpressionDataVector> vectors = ee.getRawExpressionDataVectors();
        for ( DesignElementDataVector dataVector : vectors ) {
            CompositeSequence probe = dataVector.getDesignElement();

            assert probe != null;

            ArrayDesign arrayDesign = probe.getArrayDesign();
            assert arrayDesign != null : probe + " does not have an array design";

            arrayDesign = loadOrPersistArrayDesignAndAddToCache( arrayDesign, c );

            CompositeSequence cachedProbe = c.getFromCache( probe );

            if ( cachedProbe == null ) {
                if ( !newprobes.containsKey( arrayDesign ) ) {
                    newprobes.put( arrayDesign, new HashSet<CompositeSequence>() );
                }
                newprobes.get( arrayDesign ).add( probe );
                dataVectorsThatNeedNewProbes.add( dataVector );
            } else {
                dataVector.setDesignElement( cachedProbe );
            }

        }

        /*
         * Second pass - to fill in vectors that needed probes after the first pass.
         */
        if ( !dataVectorsThatNeedNewProbes.isEmpty() ) {

            newprobes = addNewDesignElementToPersistentArrayDesigns( newprobes );

            for ( ArrayDesign ad : newprobes.keySet() ) {
                for ( CompositeSequence cs : newprobes.get( ad ) ) {
                    c.addToCache( cs );
                }
            }

            // associate with vectors. This repeats code from above, needs refactoring...
            for ( DesignElementDataVector v : dataVectorsThatNeedNewProbes ) {
                CompositeSequence probe = v.getDesignElement();

                probe = c.getFromCache( probe );

                if ( probe == null || PersisterHelper.isTransient( probe ) ) {
                    throw new IllegalStateException( "All probes should be persistent by now" );
                }

                v.setDesignElement( probe );

            }

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

        if ( StringUtils.isBlank( arrayDesign.getShortName() ) ) {
            throw new IllegalArgumentException( "Array design must have a 'short name'" );
        }

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

        return persistedDE;

    }

    /**
     * @param toAdd
     * @return
     */
    private Map<ArrayDesign, Collection<CompositeSequence>> addNewDesignElementToPersistentArrayDesigns(
            Map<ArrayDesign, Collection<CompositeSequence>> toAdd ) {

        Map<ArrayDesign, Collection<CompositeSequence>> result = new HashMap<ArrayDesign, Collection<CompositeSequence>>();

        for ( ArrayDesign ad : toAdd.keySet() ) {
            assert ad.getId() != null;
            result.put( ad, new HashSet<CompositeSequence>() );
            Collection<CompositeSequence> newprobes = new HashSet<CompositeSequence>();
            for ( CompositeSequence cs : toAdd.get( ad ) ) {
                newprobes.add( addNewDesignElementToPersistentArrayDesign( ad, cs ) );
            }
            result.get( ad ).addAll( newprobes );

            ad.getCompositeSequences().addAll( newprobes );

            log.info( "Updating " + ad );
            // transaction.
            this.arrayDesignService.update( ad );
            log.info( "Created " + newprobes + " probes" );

        }

        return result;

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
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            c.addToCache( cs );
        }
    }
}
