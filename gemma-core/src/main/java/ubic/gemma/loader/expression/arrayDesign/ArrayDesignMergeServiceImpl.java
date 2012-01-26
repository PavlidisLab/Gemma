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
package ubic.gemma.loader.expression.arrayDesign;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.analysis.report.ArrayDesignReportService;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignMergeEventImpl;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * @author paul
 * @version $Id$
 */
@Service
public class ArrayDesignMergeServiceImpl implements ArrayDesignMergeService {

    private static Log log = LogFactory.getLog( ArrayDesignMergeServiceImpl.class.getName() );

    /**
     * This is used when we hit a duplicate probe name, and we need to mangle them.
     */
    private static final String PROBE_NAME_DISAMBIGUATION_SUFFIX_SEPARATOR = "___";

    /**
     * 
     */
    private static final String PROBE_NAME_DISAMBIGUATION_REGEX = PROBE_NAME_DISAMBIGUATION_SUFFIX_SEPARATOR
            + "(\\d )+$";

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private ArrayDesignReportService arrayDesignReportService;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.loader.expression.arrayDesign.ArrayDesignMergeService#merge(ubic.gemma.model.expression.arrayDesign
     * .ArrayDesign, java.util.Collection, java.lang.String, java.lang.String, boolean)
     */
    @Override
    public ArrayDesign merge( ArrayDesign arrayDesign, Collection<ArrayDesign> otherArrayDesigns,
            String nameOfNewDesign, String shortNameOfNewDesign, boolean add ) {

        if ( otherArrayDesigns.isEmpty() )
            throw new IllegalArgumentException( "Must merge at least one array design" );

        /*
         * We allow merging of, or into, an already merged design, but array designs can't be merged into more than one.
         */
        if ( arrayDesign.getMergedInto() != null ) {
            throw new IllegalArgumentException( "Sorry, can't merge an array design that is already a mergee ("
                    + arrayDesign + ")" );
        }

        if ( add && arrayDesign.getMergees().isEmpty() ) {
            throw new IllegalArgumentException( "Can't use 'add' when arrayDesign isn't already a mergee ("
                    + arrayDesign + ")" );
        }

        // make map of biosequence -> design elements for all the array designs. But watch out for biosequences that
        // appear more than once per array design.
        Map<BioSequence, Collection<CompositeSequence>> globalBsMap = new HashMap<BioSequence, Collection<CompositeSequence>>();

        makeBioSeqMap( globalBsMap, arrayDesign );

        log.info( globalBsMap.keySet().size() + " sequences in first array design." );

        for ( ArrayDesign otherArrayDesign : otherArrayDesigns ) {
            if ( otherArrayDesign.getMergedInto() != null ) {
                throw new IllegalArgumentException( "Sorry, can't merge an array design that is already a mergee ("
                        + otherArrayDesign + ")" );
            }

            if ( arrayDesign.equals( otherArrayDesign ) ) {
                // defensive.
                continue;
            }

            log.info( "Examining " + otherArrayDesign );
            makeBioSeqMap( globalBsMap, otherArrayDesign );

            log.info( globalBsMap.keySet().size() + " unique sequences encountered in total so far" );
        }

        return createMerged( arrayDesign, otherArrayDesigns, globalBsMap, nameOfNewDesign, shortNameOfNewDesign, add );
    }

    /**
     * Add an ArrayDesignMergeEvent event to the audit trail. Does not persist it.
     * 
     * @param arrayDesign
     */
    private void audit( ArrayDesign arrayDesign, String note ) {
        AuditEvent auditEvent = AuditEvent.Factory.newInstance();
        auditEvent.setDate( new Date() );
        auditEvent.setAction( AuditAction.UPDATE );
        auditEvent.setEventType( new ArrayDesignMergeEventImpl() );
        auditEvent.setNote( note );
        arrayDesign.getAuditTrail().addEvent( auditEvent );
    }

    /**
     * @param arrayDesign
     * @param otherArrayDesigns
     * @param globalBsMap
     * @param newName
     * @param newShortName
     * @param mergeWithExisting i.e., "add", assuming arrayDesign is already a merged design.
     */
    private ArrayDesign createMerged( ArrayDesign arrayDesign, Collection<ArrayDesign> otherArrayDesigns,
            Map<BioSequence, Collection<CompositeSequence>> globalBsMap, String newName, String newShortName,
            boolean mergeWithExisting ) {

        ArrayDesign result = formMergedDesign( arrayDesign, otherArrayDesigns, newName, newShortName, mergeWithExisting );

        Collection<CompositeSequence> newProbes = makeNewProbes( result, globalBsMap, mergeWithExisting );

        result = persistMerging( result, arrayDesign, otherArrayDesigns, mergeWithExisting, newProbes );

        arrayDesignReportService.generateArrayDesignReport( result.getId() );

        return result;
    }

    /**
     * Populate the initial skeleton of the merged design. If mergeWithExisting=true, then the description of
     * arrayDesign is updated
     * 
     * @param arrayDesign
     * @param otherArrayDesigns
     * @param newName
     * @param newShortName
     * @param mergeWithExisting
     * @return either a new non-persistent arrayDesign ready to be populated, or arrayDesign with an updated
     *         description.
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
            log.info( arrayDesign + " is already a merged design, others will be added in" );
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
     * 
     * @param probeNames
     * @param cs
     * @return
     */
    private String getProbeName( Collection<String> probeNames, CompositeSequence cs ) {
        String name = cs.getName();
        int i = 1;

        if ( name.matches( PROBE_NAME_DISAMBIGUATION_REGEX ) ) {
            // FIXME then we can't do the stuff below without some fix.
        }

        while ( probeNames.contains( name ) ) {
            if ( name.matches( PROBE_NAME_DISAMBIGUATION_REGEX ) ) {
                name = name
                        .replaceAll( PROBE_NAME_DISAMBIGUATION_REGEX, PROBE_NAME_DISAMBIGUATION_SUFFIX_SEPARATOR + i );
            } else {
                name = name + PROBE_NAME_DISAMBIGUATION_SUFFIX_SEPARATOR + i;
            }
            i++;
        }
        return name;
    }

    /**
     * @param globalBsMap
     * @param arrayDesign
     */
    private void makeBioSeqMap( Map<BioSequence, Collection<CompositeSequence>> globalBsMap, ArrayDesign arrayDesign ) {
        Map<BioSequence, Collection<CompositeSequence>> bsMap = new HashMap<BioSequence, Collection<CompositeSequence>>();
        int count = 0;
        for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
            BioSequence bs = cs.getBiologicalCharacteristic();

            if ( !globalBsMap.containsKey( bs ) ) {
                globalBsMap.put( bs, new HashSet<CompositeSequence>() );
            }

            if ( !bsMap.containsKey( bs ) ) {
                bsMap.put( bs, new HashSet<CompositeSequence>() );
            }

            bsMap.get( bs ).add( cs );

            if ( globalBsMap.get( bs ).size() < bsMap.get( bs ).size() ) {
                globalBsMap.get( bs ).add( cs );
            }
        }

        if ( ++count % 10000 == 0 ) {
            log.info( "Processed " + count + " probes" );
        }

    }

    /**
     * Makes the new or additional probes (non-persistent) for the merged array design. If mergeWithExisting=true,
     * probes from arrayDesign will not be included; just the ones that we need to add to it will be returned.
     * 
     * @param arrayDesign
     * @param globalBsMap
     * @param mergeWithExisting
     * @return
     */
    private Collection<CompositeSequence> makeNewProbes( ArrayDesign arrayDesign,
            Map<BioSequence, Collection<CompositeSequence>> globalBsMap, boolean mergeWithExisting ) {
        int count;
        count = 0;
        Collection<CompositeSequence> newProbes = new HashSet<CompositeSequence>();

        Collection<String> probeNames = new HashSet<String>();
        for ( BioSequence bs : globalBsMap.keySet() ) {
            for ( CompositeSequence cs : globalBsMap.get( bs ) ) {
                if ( mergeWithExisting && cs.getArrayDesign().equals( arrayDesign ) ) {
                    assert arrayDesign.getId() != null;
                    /*
                     * Only add probes from the _other_ array designs.
                     */
                    continue;
                }

                CompositeSequence newCs = CompositeSequence.Factory.newInstance();
                newCs.setBiologicalCharacteristic( bs );

                String name = getProbeName( probeNames, cs );

                probeNames.add( name );
                newCs.setName( name );
                newCs.setDescription( ( cs.getDescription() == null ? "" : cs.getDescription() ) + " (via merge)" );
                newCs.setArrayDesign( arrayDesign );
                newProbes.add( newCs );
            }
        }
        log.info( "Made " + count + " new probes" );
        return newProbes;
    }

    /**
     * Finalize the assembly and persistence of the merged array design.
     * 
     * @param result the final merged design
     * @param arrayDesign
     * @param otherArrayDesigns
     * @param mergeWithExisting
     * @param newProbes Probes that have to be added to make up the merged design. In the case of "mergeWithExisting",
     *        this might even be empty.
     * @return the final persistent merged design
     */
    private ArrayDesign persistMerging( ArrayDesign result, ArrayDesign arrayDesign,
            Collection<ArrayDesign> otherArrayDesigns, boolean mergeWithExisting,
            Collection<CompositeSequence> newProbes ) {

        for ( ArrayDesign otherArrayDesign : otherArrayDesigns ) {
            otherArrayDesign.setMergedInto( result );
            audit( otherArrayDesign, "Merged into " + result );
        }

        result.getMergees().addAll( otherArrayDesigns );
        result.getCompositeSequences().addAll( newProbes );

        if ( mergeWithExisting ) {
            /* we're merging into the given arrayDesign. */
            assert result.equals( arrayDesign );
            assert result.getId() != null;
            assert !result.getCompositeSequences().isEmpty();

            audit( result, "More array design(s) added to merge" );

            arrayDesignService.update( result );
        } else {
            /* we're making a new one. In this case arrayDesign is treated just like the other ones, so we pile it in. */

            assert result.getId() == null;

            result.getMergees().add( arrayDesign );
            arrayDesign.setMergedInto( result );
            audit( arrayDesign, "Merged into " + result );

            result = arrayDesignService.create( result );
        }

        return result;
    }
}
