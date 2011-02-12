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

import java.util.HashSet;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;

import ubic.gemma.loader.expression.arrayDesign.ArrayDesignMergeService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

/**
 * <ul>
 * <li>make new array design based on others
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

    public static void main( String[] args ) {
        ArrayDesignMergeCli b = new ArrayDesignMergeCli();
        b.doWork( args );
    }

    private HashSet<ArrayDesign> otherArrayDesigns;
    private String newShortName;
    private String newName;
    private ArrayDesignMergeService arrayDesignMergeService;

    private ArrayDesign arrayDesign;

    @Override
    public String getShortDesc() {
        return "Make a new array design that combines the reporters from others.";
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
                        "Short name(s) of arrays to merge with the one given to the -a option, preferably subsumed by it, comma-delimited. "
                                + "If the array given with -a is already a merged design, these will be added to it if the -add option is given"
                                + "The designs cannot be ones already merged into another design, but they can be mergees." )
                .withLongOpt( "other" ).create( 'o' );

        addOption( otherArrayDesignOption );

        Option newAdName = OptionBuilder.hasArg().withArgName( "name" ).withDescription(
                "Name for new array design, if the given array is not already a merged design" ).withLongOpt( "name" )
                .create( 'n' );
        addOption( newAdName );
        Option newAdShortName = OptionBuilder.hasArg().withArgName( "name" ).withDescription(
                "Short name for new array design, if the given array is not already a merged design" ).withLongOpt(
                "shortname" ).create( 's' );
        addOption( newAdShortName );

        Option addOption = OptionBuilder.withDescription(
                "If the given array is already a merged design, add the -o designs to it. "
                        + "Recommended unless there is a specific reason to create a new design." ).create( "add" );
        addOption( addOption );
    }

    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "subsumption tester", args );
        if ( err != null ) {
            bail( ErrorCode.INVALID_OPTION );
            return err;
        }

        arrayDesignMergeService.merge( arrayDesign, otherArrayDesigns, newName, newShortName, this.hasOption( "add" ) );

        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 'o' ) ) {// required
            String otherArrayDesigName = getOptionValue( 'o' );
            String[] names = StringUtils.split( otherArrayDesigName, ',' );
            this.otherArrayDesigns = new HashSet<ArrayDesign>();
            for ( String string : names ) {
                ArrayDesign o = locateArrayDesign( string );
                if ( o == null ) {
                    throw new IllegalArgumentException( "Array design " + string + " not found" );
                }
                o = unlazifyArrayDesign( o );
                this.otherArrayDesigns.add( o );
            }
        }

        if ( this.arrayDesignsToProcess.size() > 1 ) {
            throw new IllegalArgumentException(
                    "Cannot be applied to more than one array design given to the '-a' option" );
        }

        arrayDesign = this.arrayDesignsToProcess.iterator().next();

        arrayDesign = unlazifyArrayDesign( arrayDesign );

        if ( this.hasOption( "add" ) ) {
            if ( arrayDesign.getMergees().isEmpty() ) {
                throw new IllegalArgumentException( "The array given must be a merged design when using -add" );
            }
        } else {

            if ( this.hasOption( "n" ) ) {
                this.newName = getOptionValue( 'n' );
            } else {
                throw new IllegalArgumentException( "You must provide a name for the new design unless using -add" );
            }
            if ( this.hasOption( "s" ) ) {
                this.newShortName = getOptionValue( 's' );
            } else {
                throw new IllegalArgumentException(
                        "You must provide a short name for the new design unless using -add" );
            }
        }
        arrayDesignMergeService = ( ArrayDesignMergeService ) this.getBean( "arrayDesignMergeService" );

    }

}
