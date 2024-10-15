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
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignMergeService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

import java.util.Collection;
import java.util.HashSet;

/**
 * <ul>
 * <li>make new array design based on others
 * <li>Keep map of relation between new design elements and old ones
 * <li>Store relationship with mergees
 * </ul>
 * Separate operations:
 * <ul>
 * <li>For an EE, Remap DesignElement references to old array designs to new one, and old BioAssay AD refs to new one.
 * </ul>
 *
 * @author pavlidis
 */
public class ArrayDesignMergeCli extends ArrayDesignSequenceManipulatingCli {

    @Autowired
    private ArrayDesignMergeService arrayDesignMergeService;

    private ArrayDesign arrayDesign;
    private String newName;
    private String newShortName;
    private Collection<ArrayDesign> otherArrayDesigns;
    private boolean add;

    @Override
    public String getCommandName() {
        return "mergePlatforms";
    }

    @Override
    protected void doAuthenticatedWork() {
        arrayDesignMergeService.merge( arrayDesign, otherArrayDesigns, newName, newShortName, add );
    }

    @Override
    public String getShortDesc() {
        return "Make a new array design that combines the reporters from others.";
    }

    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );
        Option otherArrayDesignOption = Option.builder( "o" ).required().hasArg().argName( "Other platforms" )
                .desc(
                        "Short name(s) of arrays to merge with the one given to the -a option, preferably subsumed by it, comma-delimited. "
                                + "If the platform given with -a is already a merged design, these will be added to it if the -add option is given"
                                + "The designs cannot be ones already merged into another design, but they can be mergees." )
                .longOpt( "other" ).build();

        options.addOption( otherArrayDesignOption );

        Option newAdName = Option.builder( "n" ).hasArg().argName( "name" )
                .desc( "Name for new platform, if the given platform is not already a merged design" )
                .longOpt( "name" ).build();
        options.addOption( newAdName );
        Option newAdShortName = Option.builder( "s" ).hasArg().argName( "shortname" )
                .desc( "Short name for new platform, if the given platform is not already a merged design" )
                .longOpt( "shortname" ).build();
        options.addOption( newAdShortName );

        Option addOption = Option.builder( "add" ).desc(
                        "If the given platform is already a merged design, add the -o designs to it. "
                                + "Recommended unless there is a specific reason to create a new design." )
                .build();
        options.addOption( addOption );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        super.processOptions( commandLine );
        if ( commandLine.hasOption( 'o' ) ) {// required
            String otherArrayDesignName = commandLine.getOptionValue( 'o' );
            String[] names = StringUtils.split( otherArrayDesignName, ',' );
            this.otherArrayDesigns = new HashSet<>();
            for ( String string : names ) {
                ArrayDesign o = entityLocator.locateArrayDesign( string );
                this.otherArrayDesigns.add( o );
            }
            this.otherArrayDesigns = getArrayDesignService().thaw( this.otherArrayDesigns );
        }

        if ( this.getArrayDesignsToProcess().size() > 1 ) {
            throw new IllegalArgumentException(
                    "Cannot be applied to more than one array design given to the '-a' option" );
        }

        arrayDesign = this.getArrayDesignsToProcess().iterator().next();

        arrayDesign = getArrayDesignService().thaw( arrayDesign );

        if ( commandLine.hasOption( "add" ) ) {
            if ( arrayDesign.getMergees().isEmpty() ) {
                throw new IllegalArgumentException( "The array given must be a merged design when using -add" );
            }
        } else {

            if ( commandLine.hasOption( "n" ) ) {
                this.newName = commandLine.getOptionValue( 'n' );
            } else {
                throw new IllegalArgumentException( "You must provide a name for the new design unless using -add" );
            }
            if ( commandLine.hasOption( "s" ) ) {
                this.newShortName = commandLine.getOptionValue( 's' );
            } else {
                throw new IllegalArgumentException(
                        "You must provide a short name for the new design unless using -add" );
            }
        }

        this.add = commandLine.hasOption( "add" );
    }

}
