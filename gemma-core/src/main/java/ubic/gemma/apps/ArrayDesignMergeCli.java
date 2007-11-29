/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.apps;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;

import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignMergeEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * <ul>
 * <li> make new array design based on others
 * <li>Keep map of relation between new design elements and old ones
 * <li>Store relationship with mergees
 * </ul>
 * <p>
 * Separate operations:
 * <ul>
 * <li>For an EE, Remap DesignElement references to old array designs to new one, and old BioAssay AD refs to new one.
 * </ul>
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignMergeCli extends ArrayDesignSequenceManipulatingCli {

    /**
     * This is used when we hit a duplicate probe name, and we need to mangle them.
     */
    private static final String PROBE_NAME_DISAMBIGUATION_SUFFIX_SEPARATOR = "___";
    
    /**
     * 
     */
    private static final String PROBE_NAME_DISAMBIGUATION_REGEX = PROBE_NAME_DISAMBIGUATION_SUFFIX_SEPARATOR
            + "(\\d )+$";

    public static void main( String[] args ) {
        ArrayDesignMergeCli b = new ArrayDesignMergeCli();
        b.doWork( args );
    }

    private Collection<String> otherArrayDesignNames;
    private String newShortName;
    private String newName;

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
    private void createMerged( ArrayDesign arrayDesign, Map<BioSequence, Collection<CompositeSequence>> globalBsMap ) {

        Collection<ArrayDesign> existingMergees = arrayDesign.getMergees();
        boolean mergeWithExisting = existingMergees.size() > 0;

        StringBuilder mergeeList = new StringBuilder();
        for ( String s : otherArrayDesignNames ) {
            mergeeList.append( s + ", " );
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
            mergedAd = ( ArrayDesign ) this.getPersisterHelper().persist( mergedAd );
            arrayDesign.setMergedInto( mergedAd );
            audit( arrayDesign, "Merged into " + mergedAd );
        }

        for ( String otherArrayDesigName : otherArrayDesignNames ) {
            ArrayDesign otherArrayDesign = locateArrayDesign( otherArrayDesigName );
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

        // }
    }

    /**
     * @param arrayDesign
     */
    private void merge( ArrayDesign arrayDesign ) {

        // Collection<ArrayDesign> existingMergees = arrayDesign.getMergees();
        // boolean mergeWithExisting = existingMergees.size() > 0;

        // FIXME refactor this into a separate service.

        // make map of biosequence -> design elements for all the array designs. But watch out for biosequences that
        // appear more than once per array design.
        Map<BioSequence, Collection<CompositeSequence>> globalBsMap = new HashMap<BioSequence, Collection<CompositeSequence>>();

        makeBioSeqMap( globalBsMap, arrayDesign );

        log.info( globalBsMap.keySet().size() + " sequences encountered in first array design to be merged." );

        for ( String otherArrayDesignName : otherArrayDesignNames ) {
            log.info( "Processing " + otherArrayDesignName );
            // collect mergees
            ArrayDesign otherArrayDesign = locateArrayDesign( otherArrayDesignName );

            if ( arrayDesign.equals( otherArrayDesign ) ) {
                continue;
            }

            if ( otherArrayDesign == null ) {
                log.error( "No arrayDesign " + otherArrayDesignName + " found" );
                bail( ErrorCode.INVALID_OPTION );
            }

            unlazifyArrayDesign( otherArrayDesign );

            makeBioSeqMap( globalBsMap, otherArrayDesign );

            log.info( globalBsMap.keySet().size() + " sequences encountered in total" );

        }

        createMerged( arrayDesign, globalBsMap );
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();
        Option otherArrayDesignOption = OptionBuilder
                .isRequired()
                .hasArg()
                .withArgName( "Other array designs" )
                .withDescription(
                        "Short name(s) of arrays to merge with the one given to the -a option, preferably subsumed by it, comma-delimited" )
                .withLongOpt( "other" ).create( 'o' );

        addOption( otherArrayDesignOption );

        Option newAdName = OptionBuilder.isRequired().hasArg().withArgName( "name" ).withDescription(
                "Name for new array design" ).withLongOpt( "name" ).create( 'n' );
        addOption( newAdName );
        Option newAdShortName = OptionBuilder.isRequired().hasArg().withArgName( "name" ).withDescription(
                "Short name for new array design" ).withLongOpt( "shortname" ).create( 's' );
        addOption( newAdShortName );
    }

    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "subsumption tester", args );
        if ( err != null ) {
            bail( ErrorCode.INVALID_OPTION );
            return err;
        }

        ArrayDesign arrayDesign = locateArrayDesign( arrayDesignName );

        if ( arrayDesign == null ) {
            log.error( "No arrayDesign " + arrayDesignName + " found" );
            bail( ErrorCode.INVALID_OPTION );
        }

        unlazifyArrayDesign( arrayDesign );

        merge( arrayDesign );

        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 'o' ) ) {
            String otherArrayDesigName = getOptionValue( 'o' );
            String[] names = StringUtils.split( otherArrayDesigName, ',' );
            this.otherArrayDesignNames = new HashSet<String>();
            for ( String string : names ) {
                this.otherArrayDesignNames.add( string );
            }
        }

        this.newName = getOptionValue( 'n' );
        this.newShortName = getOptionValue( 's' );

    }

}
