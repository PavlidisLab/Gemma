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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.analysis.report.ArrayDesignReportService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignMergeEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.PersisterHelper;

/**
 * <ul>
 * <li>Make new array design based on others
 * <li>Keep map of relation between new design elements and old ones
 * <li>Store relationship with mergees
 * </ul>
 * 
 * @author paul
 * @version $Id$
 */
@Service
public class ArrayDesignMergeService {

    private static Log log = LogFactory.getLog( ArrayDesignMergeService.class.getName() );

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
    AuditTrailService auditTrailService;
    @Autowired
    ArrayDesignService arrayDesignService;
    @Autowired
    PersisterHelper persisterHelper;

    @Autowired
    ArrayDesignReportService arrayDesignReportService;

    /**
     * @param arrayDesign, used as a "top level" design when 'add' is true; otherwise just treated as one of the designs
     *        to be merged into a new design.
     * @param otherArrayDesigns array designs to merge with the arrayDesign
     * @param nameOfNewDesign can be null if add is true (ignored)
     * @param shortNameOfNewDesign can be null if add is true (ignored)
     * @param add if arrayDesign is already merged, add the otherArrayDesigns to it. Otherwise force the creation of a
     *        new design.
     * @return the merged design
     */
    public ArrayDesign merge( ArrayDesign arrayDesign, Collection<ArrayDesign> otherArrayDesigns,
            String nameOfNewDesign, String shortNameOfNewDesign, boolean add ) {

        if ( otherArrayDesigns.isEmpty() )
            throw new IllegalArgumentException( "Must merge at least one array design" );

        // make map of biosequence -> design elements for all the array designs. But watch out for biosequences that
        // appear more than once per array design.
        Map<BioSequence, Collection<CompositeSequence>> globalBsMap = new HashMap<BioSequence, Collection<CompositeSequence>>();

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

        makeBioSeqMap( globalBsMap, arrayDesignService.thaw( arrayDesign ) );

        log.info( globalBsMap.keySet().size() + " sequences in first array design." );

        for ( ArrayDesign otherArrayDesign : otherArrayDesigns ) {

            if ( otherArrayDesign.getMergedInto() != null ) {
                throw new IllegalArgumentException( "Sorry, can't merge an array design that is already a mergee   ("
                        + otherArrayDesign + ")" );
            }

            if ( arrayDesign.equals( otherArrayDesign ) ) {
                continue;
            }
            log.info( "Processing " + otherArrayDesign );

            makeBioSeqMap( globalBsMap, arrayDesignService.thaw( otherArrayDesign ) );

            log.info( globalBsMap.keySet().size() + " sequences encountered in total so far" );

        }

        return createMerged( arrayDesign, otherArrayDesigns, globalBsMap, nameOfNewDesign, shortNameOfNewDesign, add );
    }

    public void setArrayDesignReportService( ArrayDesignReportService arrayDesignReportService ) {
        this.arrayDesignReportService = arrayDesignReportService;
    }

    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    public void setAuditTrailService( AuditTrailService auditTrailService ) {
        this.auditTrailService = auditTrailService;
    }

    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * @param arrayDesign
     */
    private void audit( ArrayDesign arrayDesign, String note ) {
        AuditEventType eventType = ArrayDesignMergeEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( arrayDesign, eventType, note );
    }

    /**
     * @param arrayDesign
     * @param globalBsMap
     * @param add
     */
    private ArrayDesign createMerged( ArrayDesign arrayDesign, Collection<ArrayDesign> otherArrayDesigns,
            Map<BioSequence, Collection<CompositeSequence>> globalBsMap, String newName, String newShortName,
            boolean mergeWithExisting ) {

        StringBuilder mergeeList = new StringBuilder();
        for ( ArrayDesign ad : otherArrayDesigns ) {
            mergeeList.append( ad.getShortName() + ", " );
        }
        ArrayDesign mergedAd;
        if ( mergeWithExisting ) {
            if ( arrayDesign.getMergees().isEmpty() ) {
                throw new IllegalArgumentException(
                        "Cannot use 'add' unless the array design is already a merged design: " + arrayDesign );
            }
            assert arrayDesign.getId() != null;
            log.info( arrayDesign + " is already a merged design, others will be added in" );
            mergedAd = arrayDesign;
            mergedAd.setDescription( mergedAd.getDescription() + "; Additional designs merged in: "
                    + StringUtils.chop( mergeeList.toString() ) );
        } else {
            mergedAd = ArrayDesign.Factory.newInstance();
            mergedAd.setName( newName );
            mergedAd.setPrimaryTaxon( arrayDesign.getPrimaryTaxon() ); // assume this is ok.
            mergedAd.setShortName( newShortName );
            mergedAd.setTechnologyType( arrayDesign.getTechnologyType() );
            mergedAd.setDesignProvider( arrayDesign.getDesignProvider() );
            mergeeList.append( arrayDesign.getShortName() );
            mergedAd.setDescription( "Created by merging the following array designs: " + mergeeList.toString() );
        }

        int count;
        count = 0;
        Collection<CompositeSequence> newProbes = new HashSet<CompositeSequence>();

        // make sure design elements get unique names.
        Collection<String> probeNames = new HashSet<String>();

        for ( BioSequence bs : globalBsMap.keySet() ) {
            for ( CompositeSequence cs : globalBsMap.get( bs ) ) {

                if ( mergeWithExisting && cs.getArrayDesign().equals( mergedAd ) ) {
                    assert mergedAd.getId() != null;
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
                newCs.setArrayDesign( mergedAd );
                newProbes.add( newCs );
                if ( ++count % 2000 == 0 ) {
                    log.info( "Made " + count + " new probes" );
                }
            }
        }

        if ( mergeWithExisting ) {
            // add new probes as needed.
            assert mergedAd.getId() != null;
            assert !mergedAd.getCompositeSequences().isEmpty();
            mergedAd.getCompositeSequences().addAll( newProbes );
            mergedAd.getMergees().addAll( otherArrayDesigns );
            arrayDesignService.update( mergedAd );
            audit( mergedAd, "More array design(s) added to merge" );
        } else {
            assert mergedAd.getId() == null;
            assert mergedAd.getCompositeSequences().isEmpty();
            mergedAd.setCompositeSequences( newProbes );
            mergedAd = ( ArrayDesign ) persisterHelper.persist( mergedAd );
            mergedAd.getMergees().addAll( otherArrayDesigns );
            arrayDesign.setMergedInto( mergedAd );
            arrayDesignService.update( mergedAd );
            arrayDesignService.update( arrayDesign );

            audit( arrayDesign, "Merged into " + mergedAd );
        }

        for ( ArrayDesign otherArrayDesign : otherArrayDesigns ) {
            otherArrayDesign.setMergedInto( mergedAd );
            arrayDesignService.update( otherArrayDesign );
            audit( otherArrayDesign, "Merged into " + mergedAd );
        }
        arrayDesignReportService.generateArrayDesignReport( mergedAd.getId() );
        return mergedAd;
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
}
