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
package ubic.gemma.persistence.service.expression.experiment;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.persister.ArrayDesignsForExperimentCache;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Sets up the array designs, put the designelements in the data vectors.
 *
 * @author paul
 */
@Component
public class ExpressionExperimentPrePersistServiceImpl implements ExpressionExperimentPrePersistService {

    private static final Log log = LogFactory.getLog( ExpressionExperimentPrePersistServiceImpl.class );

    @Autowired
    private Persister persisterHelper;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private CompositeSequenceService compositeSequenceService;

    @Value("${gemma.allow.new.probes.onexisting.platforms}")
    private boolean allowNewProbesOnExistingPlatforms;

    @Override
    public ArrayDesignsForExperimentCache prepare( ExpressionExperiment ee ) {
        return this.prepare( ee, new ArrayDesignsForExperimentCache() );
    }

    @Override
    public ArrayDesignsForExperimentCache prepare( ExpressionExperiment ee, ArrayDesignsForExperimentCache cache ) {

        Map<ArrayDesign, Collection<CompositeSequence>> newprobes = new HashMap<>();
        Collection<RawExpressionDataVector> dataVectorsThatNeedNewProbes = new HashSet<>();

        /*
         * First time through.
         */
        Collection<RawExpressionDataVector> vectors = ee.getRawExpressionDataVectors();

        if ( vectors.isEmpty() ) {
            /*
             * That's okay; some data sets don't come with data.
             */
            this.prepareWithoutData( ee, cache );
        }

        for ( RawExpressionDataVector dataVector : vectors ) {
            CompositeSequence probe = dataVector.getDesignElement();

            assert probe != null;

            ArrayDesign arrayDesign = probe.getArrayDesign();
            assert arrayDesign != null : probe + " does not have an array design";

            arrayDesign = this.loadOrPersistArrayDesignAndAddToCache( arrayDesign, cache );

            CompositeSequence cachedProbe = cache.getFromCache( probe );

            if ( cachedProbe == null ) {
                if ( !newprobes.containsKey( arrayDesign ) ) {
                    newprobes.put( arrayDesign, new HashSet<>() );
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

            if ( !allowNewProbesOnExistingPlatforms ) {
                // then these vectors have to be removed, we can't load them. 

                log.warn( dataVectorsThatNeedNewProbes.size()
                        + " vectors do not match probes in the existing platform, "
                        + "and gemma.allow.new.probes.onexisting.platforms=false, so they will be ignored." );
                ee.getRawExpressionDataVectors().removeAll( dataVectorsThatNeedNewProbes );
                return cache;
            }

            ExpressionExperimentPrePersistServiceImpl.log.info( dataVectorsThatNeedNewProbes.size()
                    + " vectors don't have probes, may add to the platform." );

            newprobes = this.addNewDesignElementToPersistentArrayDesigns( newprobes );

            if ( newprobes.isEmpty() ) {
                ExpressionExperimentPrePersistServiceImpl.log.info( "No probes were added" );
                // this is okay if there were none to add, but a problem otherwise.
            } else {

                // don't forget to cache them.
                for ( ArrayDesign ad : newprobes.keySet() ) {
                    for ( CompositeSequence cs : newprobes.get( ad ) ) {
                        cache.addToCache( cs );
                    }
                }

                // associate with vectors. This repeats code from above, needs refactoring...
                for ( RawExpressionDataVector v : dataVectorsThatNeedNewProbes ) {
                    CompositeSequence probe = v.getDesignElement();

                    probe = cache.getFromCache( probe );

                    if ( probe == null || probe.getId() == null ) {
                        throw new IllegalStateException( "All probes should be persistent by now" );
                    }

                    v.setDesignElement( probe );

                }
            }

        }

        return cache;
    }

    private void prepareWithoutData( ExpressionExperiment ee, ArrayDesignsForExperimentCache cache ) {
        for ( BioAssay ba : ee.getBioAssays() ) {
            ArrayDesign arrayDesign = ba.getArrayDesignUsed();
            arrayDesign = this.loadOrPersistArrayDesignAndAddToCache( arrayDesign, cache );
            ba.setArrayDesignUsed( arrayDesign );
        }
    }

    private CompositeSequence addNewDesignElementToPersistentArrayDesign( ArrayDesign arrayDesign,
            @Nullable CompositeSequence designElement ) {
        if ( designElement == null )
            return null;

        if ( designElement.getId() != null )
            return designElement;

        /*
         * No sequence, or the sequence name isn't provided. Of course, if there is no sequence it isn't going to be
         * very useful.
         */
        BioSequence biologicalCharacteristic = designElement.getBiologicalCharacteristic();

        assert arrayDesign.getId() != null;

        designElement.setArrayDesign( arrayDesign );

        if ( biologicalCharacteristic.getId() == null ) {
            // transaction.
            designElement
                    .setBiologicalCharacteristic( ( BioSequence ) persisterHelper.persist( biologicalCharacteristic ) );

        }

        // transaction.

        return compositeSequenceService.create( designElement );

    }

    /**
     * @param toAdd (might turn out to be empty ,in which case this is a no-op.
     * @return added probes, keys are the affect array designs (might be none)
     */
    private Map<ArrayDesign, Collection<CompositeSequence>> addNewDesignElementToPersistentArrayDesigns(
            Map<ArrayDesign, Collection<CompositeSequence>> toAdd ) {

        Map<ArrayDesign, Collection<CompositeSequence>> result = new HashMap<>();

        for ( ArrayDesign ad : toAdd.keySet() ) {

            assert ad.getId() != null;
            result.put( ad, new HashSet<>() );
            Collection<CompositeSequence> newprobes = new HashSet<>();

            Collection<CompositeSequence> probesToAdd = toAdd.get( ad );

            ExpressionExperimentPrePersistServiceImpl.log
                    .info( "Adding " + probesToAdd.size() + " new probes to " + ad );

            for ( CompositeSequence cs : probesToAdd ) {
                CompositeSequence np = this.addNewDesignElementToPersistentArrayDesign( ad, cs );
                newprobes.add( np );

                ExpressionExperimentPrePersistServiceImpl.log
                        .warn( "Adding new probe to existing array design " + ad.getShortName() + ": " + cs + " bioseq="
                                + cs.getBiologicalCharacteristic() );

            }
            result.get( ad ).addAll( newprobes );

            arrayDesignService.addProbes( ad, newprobes );
            ExpressionExperimentPrePersistServiceImpl.log.info( "Created " + newprobes.size() + " new probes" );

        }

        return result;

    }

    /**
     * Put an array design in the cache (if it already isn't there). This is needed when loading
     * designelementdatavectors, for example, to avoid repeated (and one-at-a-time) fetching of designelement.
     *
     * @return the persistent array design.
     */
    private ArrayDesign loadOrPersistArrayDesignAndAddToCache( ArrayDesign arrayDesign,
            ArrayDesignsForExperimentCache cache ) {
        if ( StringUtils.isBlank( arrayDesign.getShortName() ) ) {
            throw new IllegalArgumentException( "Array design must have a 'short name'" );
        }

        if ( cache.getArrayDesignCache().containsKey( arrayDesign.getShortName() ) ) {
            // already done.
            return cache.getArrayDesignCache().get( arrayDesign.getShortName() );
        }

        StopWatch timer = new StopWatch();
        timer.start();

        // transaction, but fast if the design already exists.
        arrayDesign = ( ArrayDesign ) persisterHelper.persist( arrayDesign );

        // transaction (read-only). Wasteful, if this is an existing design.
        // arrayDesign = arrayDesignService.thawRawAndProcessed( arrayDesign );
        Map<CompositeSequence, BioSequence> sequences = arrayDesignService.getBioSequences( arrayDesign );
        cache.add( arrayDesign, sequences.keySet() );

        if ( timer.getTime() > 20000 ) {
            ExpressionExperimentPrePersistServiceImpl.log
                    .info( "Load/persist & thaw array design: " + timer.getTime() + "ms" );
        }

        return arrayDesign;
    }
}
