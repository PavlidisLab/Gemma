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
package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSubsumeCheckEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;

import java.util.*;

/**
 * Test two array designs to see if one subsumes the other, and if so update their information.
 *
 * @author pavlidis
 */
public class ArrayDesignSubsumptionTesterCli extends ArrayDesignSequenceManipulatingCli {

    private Collection<String> otherArrayDesignNames;
    private boolean allWays = false;

    @Override
    public String getCommandName() {
        return "platformSubsumptionTest";
    }

    @Override
    public String getShortDesc() {
        return "Test microarray designs to see if one subsumes other(s) (in terms of probe sequences)"
                + ", and if so update their information";
    }


    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );
        Option otherArrayDesignOption = Option.builder( "o" ).required().hasArg().argName( "Other platform" )
                .desc( "Short name(s) of platforms to compare to the first one, comma-delimited" )
                .longOpt( "other" ).build();
        Option allways = Option.builder( "all" ).desc( "Test all platforms listed against all (not just to the first one)" ).build();

        options.addOption( otherArrayDesignOption );
        options.addOption( allways );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        super.processOptions( commandLine );
        if ( commandLine.hasOption( 'o' ) ) {
            String otherArrayDesignName = commandLine.getOptionValue( 'o' );
            String[] names = StringUtils.split( otherArrayDesignName, ',' );
            this.otherArrayDesignNames = new HashSet<>();
            this.otherArrayDesignNames.addAll( Arrays.asList( names ) );
        }
        this.allWays = commandLine.hasOption( "all" );
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        if ( this.getArrayDesignsToProcess().size() > 1 ) {
            throw new IllegalArgumentException(
                    "Cannot be applied to more than one array design given to the '-a' option" );
        }

        ArrayDesign arrayDesign = this.getArrayDesignsToProcess().iterator().next();
        arrayDesign = getArrayDesignService().thaw( arrayDesign );
        if ( arrayDesign.getTechnologyType().equals( TechnologyType.SEQUENCING ) ) {
            throw new IllegalArgumentException( // note that GENELIST is also invalid but this is the likely case that could be encountered
                    "This tool is only for microarray platforms; " + arrayDesign.getShortName() + " is a sequencing platform" );
        }

        List<ArrayDesign> allToCompare = new ArrayList<>();
        allToCompare.add( arrayDesign );

        for ( String otherArrayDesignName : otherArrayDesignNames ) {
            ArrayDesign otherArrayDesign = entityLocator.locateArrayDesign( otherArrayDesignName );

            if ( arrayDesign.equals( otherArrayDesign ) ) {
                continue;
            }

            otherArrayDesign = getArrayDesignService().thaw( otherArrayDesign );

            if ( otherArrayDesign.getTechnologyType().equals( TechnologyType.SEQUENCING ) ) {
                throw new IllegalArgumentException(
                        "This tool is only for microarray platforms; " + otherArrayDesign.getShortName() + " is a sequencing platform" );
            }

            if ( allWays ) {
                allToCompare.add( otherArrayDesign );
            } else {
                Boolean aSubsumes = this.getArrayDesignService().updateSubsumingStatus( arrayDesign, otherArrayDesign );
            }
//            if ( !aSubsumes ) {
//                // test other way around, but only if first way failed (to avoid cycles)
//                this.getArrayDesignService().updateSubsumingStatus( otherArrayDesign, arrayDesign );
//            }
//            this.audit( otherArrayDesign, "Tested to see if it is subsumed by " + arrayDesign );
        }


        Collection<ArrayDesign> done = new HashSet<>();
        if ( allWays ) {
            for ( int i = 0; i < allToCompare.size(); i++ ) {
                ArrayDesign a = allToCompare.get( i );
                for ( int j = 1; j < allToCompare.size(); j++ ) {
                    ArrayDesign b = allToCompare.get( j );

                    if ( a.equals( b ) ) continue;

                    if ( done.contains( a ) || done.contains( b ) ) continue;

                    log.info( "--- comparing " + a.getShortName() + " to " + b.getShortName() );

                    boolean subsumes = this.getArrayDesignService().updateSubsumingStatus( a, b );

                    if ( subsumes ) {
                        done.add( b );
                    }

                }
            }
        }

//        this.audit( arrayDesign, "Tested to see if it subsumes: " + StringUtils.join( otherArrayDesignNames, ',' ) );
    }

    private void audit( ArrayDesign arrayDesign, String note ) {
        auditTrailService.addUpdateEvent( arrayDesign, ArrayDesignSubsumeCheckEvent.class, note );
    }
}
