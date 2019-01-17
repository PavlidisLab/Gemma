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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSubsumeCheckEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * Test two array designs to see if one subsumes the other, and if so update their information.
 *
 * @author pavlidis
 */
public class ArrayDesignSubsumptionTesterCli extends ArrayDesignSequenceManipulatingCli {

    private Collection<String> otherArrayDesignNames;

    public static void main( String[] args ) {
        ArrayDesignSubsumptionTesterCli tester = new ArrayDesignSubsumptionTesterCli();
        Exception e = tester.doWork( args );
        if ( e != null ) {
            e.printStackTrace();
        }
    }

    @Override
    public String getCommandName() {
        return "platformSubsumptionTest";
    }

    @Override
    protected Exception doWork( String[] args ) {

        Exception err = this.processCommandLine( args );
        if ( err != null ) {
            this.bail( ErrorCode.INVALID_OPTION );
            return err;
        }

        if ( this.getArrayDesignsToProcess().size() > 1 ) {
            throw new IllegalArgumentException(
                    "Cannot be applied to more than one array design given to the '-a' option" );
        }

        ArrayDesign arrayDesign = this.getArrayDesignsToProcess().iterator().next();
        arrayDesign = this.thaw( arrayDesign );

        for ( String otherArrayDesignName : otherArrayDesignNames ) {
            ArrayDesign otherArrayDesign = this.locateArrayDesign( otherArrayDesignName, getArrayDesignService() );

            if ( arrayDesign.equals( otherArrayDesign ) ) {
                continue;
            }

            if ( otherArrayDesign == null ) {
                AbstractCLI.log.error( "No arrayDesign " + otherArrayDesignName + " found" );
                this.bail( ErrorCode.INVALID_OPTION );
            }

            otherArrayDesign = this.thaw( otherArrayDesign );

            Boolean aSubsumes = this.getArrayDesignService().updateSubsumingStatus( arrayDesign, otherArrayDesign );

            if ( !aSubsumes ) {
                // test other way around, but only if first way failed (to avoid cycles)
                this.getArrayDesignService().updateSubsumingStatus( otherArrayDesign, arrayDesign );
            }
            this.audit( otherArrayDesign, "Tested to see if it is subsumed by " + arrayDesign );
        }

        this.audit( arrayDesign, "Tested to see if it subsumes: " + StringUtils.join( otherArrayDesignNames, ',' ) );

        return null;
    }

    @Override
    public String getShortDesc() {
        return "Test array designs to see if one subsumes other(s) (in terms of sequences analyzed)"
                + ", and if so update their information";
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();
        Option otherArrayDesignOption = OptionBuilder.isRequired().hasArg().withArgName( "Other platform" )
                .withDescription( "Short name(s) of platforms to compare to the first one, comma-delimited" )
                .withLongOpt( "other" ).create( 'o' );

        this.addOption( otherArrayDesignOption );
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 'o' ) ) {
            String otherArrayDesignName = this.getOptionValue( 'o' );
            String[] names = StringUtils.split( otherArrayDesignName, ',' );
            this.otherArrayDesignNames = new HashSet<>();
            this.otherArrayDesignNames.addAll( Arrays.asList( names ) );
        }
    }

    private void audit( ArrayDesign arrayDesign, String note ) {
        AuditEventType eventType = ArrayDesignSubsumeCheckEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( arrayDesign, eventType, note );
    }

}
