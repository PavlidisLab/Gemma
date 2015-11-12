/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.apps;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.analysis.service.GeneMultifunctionalityPopulationService;
import ubic.gemma.apps.GemmaCLI.CommandGroup;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.AbstractCLIContextCLI;

/**
 * @author paul
 * @version $Id$
 */
public class MultifunctionalityCli extends AbstractCLIContextCLI {

    public static void main( String[] args ) {
        MultifunctionalityCli c = new MultifunctionalityCli();
        Exception e = c.doWork( args );
        if ( e != null ) {
            log.fatal( e );
        }
    }

    private Taxon taxon;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#getCommandName()
     */
    @Override
    public String getCommandName() {
        return "updateMultifunc";
    }

    @Override
    public String getShortDesc() {
        return "Update or create gene multifunctionality metrics";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.SYSTEM;
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.addUserNameAndPasswordOptions( true );
        Option taxonOption = OptionBuilder.hasArg().withDescription( "taxon name" )
                .withDescription( "Taxon of the expression experiments and genes" ).withLongOpt( "taxon" ).create( 't' );
        addOption( taxonOption );
    }

    @Override
    protected Exception doWork( String[] args ) {
        super.processCommandLine( args );

        GeneMultifunctionalityPopulationService gfs = this.getBean( GeneMultifunctionalityPopulationService.class );

        if ( this.taxon != null ) {
            gfs.updateMultifunctionality( taxon );
        } else {
            gfs.updateMultifunctionality();
        }

        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        if ( hasOption( 't' ) ) {
            String taxonName = getOptionValue( 't' );
            TaxonService taxonService = getBean( TaxonService.class );
            this.taxon = taxonService.findByCommonName( taxonName );
            if ( taxon == null ) {
                log.error( "ERROR: Cannot find taxon " + taxonName );
            }
        }
    }

}
