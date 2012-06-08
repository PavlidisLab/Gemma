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
import java.util.HashSet;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;

import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSubsumeCheckEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

/**
 * Test two array designs to see if one subsumes the other, and if so update their information.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignSubsumptionTesterCli extends ArrayDesignSequenceManipulatingCli {

    public static void main( String[] args ) {
        ArrayDesignSubsumptionTesterCli tester = new ArrayDesignSubsumptionTesterCli();
        tester.doWork( args );
    }

    private Collection<String> otherArrayDesignNames;

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();
        Option otherArrayDesignOption = OptionBuilder.isRequired().hasArg().withArgName( "Other platform" )
                .withDescription( "Short name(s) of platforms to compare to the first one, comma-delimited" )
                .withLongOpt( "other" ).create( 'o' );

        addOption( otherArrayDesignOption );
    }

    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "subsumption tester", args );
        if ( err != null ) {
            bail( ErrorCode.INVALID_OPTION );
            return err;
        }

        if ( this.arrayDesignsToProcess.size() > 1 ) {
            throw new IllegalArgumentException(
                    "Cannot be applied to more than one array design given to the '-a' option" );
        }

        ArrayDesign arrayDesign = this.arrayDesignsToProcess.iterator().next();
        arrayDesign = unlazifyArrayDesign( arrayDesign );

        for ( String otherArrayDesigName : otherArrayDesignNames ) {
            ArrayDesign otherArrayDesign = locateArrayDesign( otherArrayDesigName );

            if ( arrayDesign.equals( otherArrayDesign ) ) {
                continue;
            }

            if ( otherArrayDesign == null ) {
                log.error( "No arrayDesign " + otherArrayDesigName + " found" );
                bail( ErrorCode.INVALID_OPTION );
            }

            otherArrayDesign = unlazifyArrayDesign( otherArrayDesign );

            Boolean aSubsumeso = this.arrayDesignService.updateSubsumingStatus( arrayDesign, otherArrayDesign );

            if ( !aSubsumeso ) {
                // test other way around, but only if first way failed (to avoid cycles)
                this.arrayDesignService.updateSubsumingStatus( otherArrayDesign, arrayDesign );
            }
            audit( otherArrayDesign, "Tested to see if it is subsumed by " + arrayDesign );
        }

        audit( arrayDesign, "Tested to see if it subsumes: " + StringUtils.join( otherArrayDesignNames, ',' ) );

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
    }

    private void audit( ArrayDesign arrayDesign, String note ) {
        AuditEventType eventType = ArrayDesignSubsumeCheckEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( arrayDesign, eventType, note );
    }

}
