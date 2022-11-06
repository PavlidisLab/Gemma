/*
 * The Gemma project
 *
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.core.loader.expression.arrayDesign;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.core.loader.expression.ExpressionExperimentPlatformSwitchService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author paul
 */
@Component
public class ArrayDesignMergeServiceImpl implements ArrayDesignMergeService {

    private static final Log log = LogFactory.getLog( ArrayDesignMergeServiceImpl.class.getName() );

    /**
     * This is used when we hit a duplicate probe name, and we need to mangle them.
     */
    private static final String PROBE_NAME_DISAMBIGUATION_SUFFIX_SEPARATOR = "___";

    private static final String PROBE_NAME_DISAMBIGUATION_REGEX =
            ArrayDesignMergeServiceImpl.PROBE_NAME_DISAMBIGUATION_SUFFIX_SEPARATOR + "(\\d)+";

    private final ArrayDesignService arrayDesignService;
    private final ArrayDesignReportService arrayDesignReportService;
    private final ArrayDesignMergeHelperService mergeServiceHelper;

    @Autowired
    public ArrayDesignMergeServiceImpl( ArrayDesignService arrayDesignService,
            ArrayDesignReportService arrayDesignReportService, ArrayDesignMergeHelperService mergeServiceHelper ) {
        this.arrayDesignService = arrayDesignService;
        this.arrayDesignReportService = arrayDesignReportService;
        this.mergeServiceHelper = mergeServiceHelper;
    }

    @Override
    public ArrayDesign merge( ArrayDesign arrayDesign, Collection<ArrayDesign> otherArrayDesigns,
            String nameOfNewDesign, String shortNameOfNewDesign, boolean add ) {

        if ( otherArrayDesigns.isEmpty() )
            throw new IllegalArgumentException( "Must merge at least one array design" );

        /*
         * We allow merging of, or into, an already merged design, but array designs can't be merged into more than one.
         */
        if ( arrayDesign.getMergedInto() != null ) {
            throw new IllegalArgumentException(
                    "Sorry, can't merge an array design that is already a mergee (" + arrayDesign + ")" );
        }

        if ( add && arrayDesign.getMergees().isEmpty() ) {
            throw new IllegalArgumentException(
                    "Can't use 'add' when arrayDesign isn't already a mergee (" + arrayDesign + ")" );
        }

        // make map of biosequence -> design elements for all the array designs. But watch out for biosequences that
        // appear more than once per array design.
        Map<BioSequence, Collection<CompositeSequence>> globalBsMap = new HashMap<>();

        ArrayDesign thawed = this.makeBioSeqMap( globalBsMap, arrayDesign );

        ArrayDesignMergeServiceImpl.log.info( globalBsMap.keySet().size() + " sequences in first array design." );
        // Now check the other designs, add slots for additional probes if necessary.
        Collection<ArrayDesign> thawedOthers = new HashSet<>();
        for ( ArrayDesign otherArrayDesign : otherArrayDesigns ) {

            if ( otherArrayDesign.getMergedInto() != null ) {
                throw new IllegalArgumentException(
                        "Sorry, can't merge an array design that is already a mergee (" + otherArrayDesign + ")" );
            }

            if ( arrayDesign.equals( otherArrayDesign ) ) {
                // defensive.
                continue;
            }

            ArrayDesignMergeServiceImpl.log.info( "Examining " + otherArrayDesign );
            thawedOthers.add( this.makeBioSeqMap( globalBsMap, otherArrayDesign ) );

            ArrayDesignMergeServiceImpl.log
                    .info( globalBsMap.keySet().size() + " unique sequences encountered in total so far" );
        }

        return this.createMerged( thawed, thawedOthers, globalBsMap, nameOfNewDesign, shortNameOfNewDesign, add );
    }

    /**
     * @param mergeWithExisting i.e., "add", assuming arrayDesign is already a merged design.
     */
    private ArrayDesign createMerged( ArrayDesign arrayDesign, Collection<ArrayDesign> otherArrayDesigns,
            Map<BioSequence, Collection<CompositeSequence>> globalBsMap, String newName, String newShortName,
            boolean mergeWithExisting ) {

        ArrayDesign result = this
                .formMergedDesign( arrayDesign, otherArrayDesigns, newName, newShortName, mergeWithExisting );

        Collection<CompositeSequence> newProbes = this.makeNewProbes( result, globalBsMap, mergeWithExisting );

        result = mergeServiceHelper
                .persistMerging( result, arrayDesign, otherArrayDesigns, mergeWithExisting, newProbes );

        arrayDesignReportService.generateArrayDesignReport( result.getId() );

        return result;
    }

    /**
     * Populate the initial skeleton of the merged design. If mergeWithExisting=true, then the description of
     * arrayDesign is updated
     *
     * @return either a new non-persistent arrayDesign ready to be populated, or arrayDesign with an updated
     * description.
     */
    private ArrayDesign formMergedDesign( ArrayDesign arrayDesign, Collection<ArrayDesign> otherArrayDesigns,
            String newName, String newShortName, boolean mergeWithExisting ) {

        String mergeeListString = StringUtils.join( otherArrayDesigns, "," );

        ArrayDesign result;

        if ( mergeWithExisting ) {
            if ( arrayDesign.getMergees().isEmpty() ) {
                throw new IllegalArgumentException(
                        "Cannot use 'add' unless the array design is already a merged design: " + arrayDesign );
            }
            assert arrayDesign.getId() != null;
            ArrayDesignMergeServiceImpl.log
                    .info( arrayDesign + " is already a merged design, others will be added in" );
            result = arrayDesign;
            result.setDescription( result.getDescription() + "; Additional designs merged in: " + mergeeListString );
        } else {
            result = ArrayDesign.Factory.newInstance();
            result.setName( newName );
            result.setPrimaryTaxon( arrayDesign.getPrimaryTaxon() ); // assume this is ok.
            result.setShortName( newShortName );
            result.setTechnologyType( arrayDesign.getTechnologyType() );
            result.setDesignProvider( arrayDesign.getDesignProvider() );
            mergeeListString = mergeeListString + "," + arrayDesign.getShortName();
            result.setDescription( "Created by merging the following array designs: " + mergeeListString );
        }
        return result;
    }

    /**
     * Names won't be re-used, they will get names like "fooo___1".
     */
    private String getProbeName( Collection<String> probeNames, CompositeSequence cs ) {
        String name = cs.getName();
        int i = 1;

        //noinspection StatementWithEmptyBody // FIXME then we can't do the stuff below without some fix.
        if ( name.matches( ArrayDesignMergeServiceImpl.PROBE_NAME_DISAMBIGUATION_REGEX ) ) {
        }

        while ( probeNames.contains( name ) ) {
            if ( name.matches( ".+?" + ArrayDesignMergeServiceImpl.PROBE_NAME_DISAMBIGUATION_REGEX ) ) {
                name = name.replaceAll( ArrayDesignMergeServiceImpl.PROBE_NAME_DISAMBIGUATION_REGEX,
                        ArrayDesignMergeServiceImpl.PROBE_NAME_DISAMBIGUATION_SUFFIX_SEPARATOR + i );
            } else {
                name = name + ArrayDesignMergeServiceImpl.PROBE_NAME_DISAMBIGUATION_SUFFIX_SEPARATOR + i;
            }
            i++;
        }
        return name;
    }

    /**
     * If we're merging into an existing platform, it is important that this method is called for that platform first.
     *
     * @param globalBsMap Map that tells us, in effect, how many probes to make for the sequence. Modified by this.
     */
    private ArrayDesign makeBioSeqMap( Map<BioSequence, Collection<CompositeSequence>> globalBsMap,
            ArrayDesign arrayDesign ) {
        Map<BioSequence, Collection<CompositeSequence>> bsMap = new HashMap<>();
        arrayDesign = this.arrayDesignService.thaw( arrayDesign );
        int count = 0;
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            BioSequence bs = cs.getBiologicalCharacteristic();

            if ( bs == null ) {
                // this is common. We could try to match them up based on the probe name, because there is nothing else
                // to go on.
                bs = ExpressionExperimentPlatformSwitchService.NULL_BIOSEQUENCE;
            }

            if ( !globalBsMap.containsKey( bs ) ) {
                globalBsMap.put( bs, new HashSet<CompositeSequence>() );
            }

            if ( !bsMap.containsKey( bs ) ) {
                bsMap.put( bs, new HashSet<CompositeSequence>() );
            }

            bsMap.get( bs ).add( cs );

            /*
             * We need at one probe for each time the sequence appears on one platform. Here we ensure that the global
             * map has enough 'slots'.
             */
            if ( globalBsMap.get( bs ).size() < bsMap.get( bs ).size() ) {
                globalBsMap.get( bs ).add( cs );
            }
        }

        if ( ++count % 10000 == 0 ) {
            ArrayDesignMergeServiceImpl.log.info( "Processed " + count + " probes" );
        }
        return arrayDesign;
    }

    /**
     * Makes the new or additional probes (non-persistent) for the merged array design. If mergeWithExisting=true,
     * probes from arrayDesign will not be included; just the ones that we need to add to it will be returned.
     *
     * @param globalBsMap Map that tells us, in effect, how many probes to make for the sequence.
     */
    private Collection<CompositeSequence> makeNewProbes( ArrayDesign arrayDesign,
            Map<BioSequence, Collection<CompositeSequence>> globalBsMap, boolean mergeWithExisting ) {

        Collection<CompositeSequence> newProbes = new HashSet<>();
        ArrayDesignMergeServiceImpl.log.info( globalBsMap.size() + " unique sequences" );

        Collection<String> probeNames = new HashSet<>();
        for ( BioSequence bs : globalBsMap.keySet() ) {
            assert bs != null; // should be the placeholder NULL_BIOSEQUENCE
            for ( CompositeSequence cs : globalBsMap.get( bs ) ) {

                if ( mergeWithExisting && cs.getArrayDesign().equals( arrayDesign ) ) {
                    assert arrayDesign.getId() != null;
                    /*
                     * Only add probes from the _other_ array designs.
                     */
                    continue;
                }

                CompositeSequence newCs = CompositeSequence.Factory.newInstance();

                if ( !bs.equals( ExpressionExperimentPlatformSwitchService.NULL_BIOSEQUENCE ) ) {
                    newCs.setBiologicalCharacteristic( bs );
                }

                String name = this.getProbeName( probeNames, cs );

                probeNames.add( name );
                newCs.setName( name );
                newCs.setDescription( ( cs.getDescription() == null ? "" : cs.getDescription() ) + " (via merge)" );
                newCs.setArrayDesign( arrayDesign );

                newProbes.add( newCs );

                if ( ArrayDesignMergeServiceImpl.log.isDebugEnabled() )
                    ArrayDesignMergeServiceImpl.log
                            .debug( "Made merged probe for " + bs + ": " + newCs + " for old probe on " + cs
                                    .getArrayDesign().getShortName() );
            }
        }
        ArrayDesignMergeServiceImpl.log.info( "Made " + newProbes.size() + " new probes" );
        return newProbes;
    }

}
