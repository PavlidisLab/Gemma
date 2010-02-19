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
     * @param arrayDesign
     * @param otherArrayDesigns
     * @param nameOfNewDesign
     * @param shortNameOfNewDesign
     */
    public void merge( ArrayDesign arrayDesign, Collection<ArrayDesign> otherArrayDesigns, String nameOfNewDesign,
            String shortNameOfNewDesign ) {

        if ( otherArrayDesigns.isEmpty() )
            throw new IllegalArgumentException( "Must merge at least one array design" );

        // make map of biosequence -> design elements for all the array designs. But watch out for biosequences that
        // appear more than once per array design.
        Map<BioSequence, Collection<CompositeSequence>> globalBsMap = new HashMap<BioSequence, Collection<CompositeSequence>>();
        arrayDesignService.thawLite( arrayDesign );

        /*
         * TODO: gracefully handle this situation. Bug 1681
         */

        if ( arrayDesign.getMergedInto() != null || arrayDesign.getMergees().size() > 0 ) {
            throw new IllegalArgumentException(
                    "Sorry, can't merge an array design that is already merged or is itself a merged design ("
                            + arrayDesign + ")" );
        }

        makeBioSeqMap( globalBsMap, arrayDesign );

        log.info( globalBsMap.keySet().size() + " sequences encountered in first array design to be merged." );

        for ( ArrayDesign otherArrayDesign : otherArrayDesigns ) {
            log.info( "Processing " + otherArrayDesign );

            if ( otherArrayDesign.getMergedInto() != null || otherArrayDesign.getMergees().size() > 0 ) {
                throw new IllegalArgumentException(
                        "Sorry, can't merge an array design that is already merged or is itself a merged design ("
                                + arrayDesign + ")" );
            }

            if ( arrayDesign.equals( otherArrayDesign ) ) {
                continue;
            }

            arrayDesignService.thawLite( otherArrayDesign );

            makeBioSeqMap( globalBsMap, otherArrayDesign );

            log.info( globalBsMap.keySet().size() + " sequences encountered in total" );

        }

        createMerged( arrayDesign, otherArrayDesigns, globalBsMap, nameOfNewDesign, shortNameOfNewDesign );
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
     */
    private void createMerged( ArrayDesign arrayDesign, Collection<ArrayDesign> otherArrayDesigns,
            Map<BioSequence, Collection<CompositeSequence>> globalBsMap, String newName, String newShortName ) {

        Collection<ArrayDesign> existingMergees = arrayDesign.getMergees();
        boolean mergeWithExisting = existingMergees.size() > 0;

        StringBuilder mergeeList = new StringBuilder();
        for ( ArrayDesign ad : otherArrayDesigns ) {
            mergeeList.append( ad.getShortName() + ", " );
        }
        ArrayDesign mergedAd;
        if ( mergeWithExisting ) {
            mergeWithExisting = true;
            log.info( arrayDesign + " is already a merged design, others will be added in" );
            mergedAd = arrayDesign;
            mergedAd.setDescription( "Additional designs merged in: " + StringUtils.chop( mergeeList.toString() ) );
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
            // TODO add new probes as needed.
            arrayDesignService.update( mergedAd );
            audit( arrayDesign, "More array design(s) added to merge" );
        } else {
            mergedAd.setCompositeSequences( newProbes );
            mergedAd = ( ArrayDesign ) persisterHelper.persist( mergedAd );
            arrayDesign.setMergedInto( mergedAd );
            audit( arrayDesign, "Merged into " + mergedAd );
        }

        for ( ArrayDesign otherArrayDesign : otherArrayDesigns ) {
            otherArrayDesign.setMergedInto( mergedAd );
            arrayDesignService.update( otherArrayDesign );
            audit( otherArrayDesign, "Merged into " + mergedAd );
        }
        arrayDesignReportService.generateArrayDesignReport( mergedAd.getId() );
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
     * @param otherArrayDesign
     */
    private void makeBioSeqMap( Map<BioSequence, Collection<CompositeSequence>> globalBsMap,
            ArrayDesign otherArrayDesign ) {
        Map<BioSequence, Collection<CompositeSequence>> bsMap = new HashMap<BioSequence, Collection<CompositeSequence>>();
        int count = 0;
        for ( CompositeSequence cs : otherArrayDesign.getCompositeSequences() ) {
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
