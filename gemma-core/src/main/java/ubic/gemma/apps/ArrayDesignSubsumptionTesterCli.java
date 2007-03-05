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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.util.AbstractCLI.ErrorCode;

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

    private String otherArrayDesigName;

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();
        Option otherArrayDesignOption = OptionBuilder.isRequired().hasArg().withArgName( "Other array design" )
                .withDescription( "A second array design name (or short name)" ).withLongOpt( "other" ).create( 'o' );

        addOption( otherArrayDesignOption );
    }

    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "subsumption tester", args );
        if ( err != null ) {
            bail( ErrorCode.INVALID_OPTION );
            return err;
        }

        ArrayDesign arrayDesign = locateArrayDesign( arrayDesignName );
        ArrayDesign otherArrayDesign = locateArrayDesign( otherArrayDesigName );

        if ( arrayDesign == null ) {
            log.error( "No arrayDesign " + arrayDesignName + " found" );
            bail( ErrorCode.INVALID_OPTION );
        }

        if ( otherArrayDesign == null ) {
            log.error( "No arrayDesign " + otherArrayDesigName + " found" );
            bail( ErrorCode.INVALID_OPTION );
        }

        unlazifyArrayDesign( arrayDesign );
        unlazifyArrayDesign( otherArrayDesign );

        Boolean aSubsumeso = this.arrayDesignService.updateSubsumingStatus( arrayDesign, otherArrayDesign );

        if ( !aSubsumeso ) {
            // test other way around.
            this.arrayDesignService.updateSubsumingStatus( otherArrayDesign, arrayDesign );
        }

        return null;
    }

    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 'o' ) ) {
            this.otherArrayDesigName = getOptionValue( 'o' );
        }
    }

}
