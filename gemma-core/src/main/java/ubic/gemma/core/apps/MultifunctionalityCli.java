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
package ubic.gemma.core.apps;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import ubic.gemma.core.analysis.service.GeneMultifunctionalityPopulationService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

/**
 * @author paul
 */
public class MultifunctionalityCli extends AbstractCLIContextCLI {

    private Taxon taxon;

    public static void main( String[] args ) {
        MultifunctionalityCli c = new MultifunctionalityCli();
        Exception e = c.doWork( args );
        if ( e != null ) {
            AbstractCLI.log.fatal( e );
        }
    }

    @Override
    public String getCommandName() {
        return "updateMultifunc";
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.addUserNameAndPasswordOptions( true );
        Option taxonOption = OptionBuilder.hasArg().withDescription( "taxon name" )
                .withDescription( "Taxon of the expression experiments and genes" ).withLongOpt( "taxon" )
                .create( 't' );
        this.addOption( taxonOption );
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception e = super.processCommandLine( args );
        if ( e != null )
            return e;

        GeneMultifunctionalityPopulationService gfs = this.getBean( GeneMultifunctionalityPopulationService.class );

        if ( this.taxon != null ) {
            gfs.updateMultifunctionality( taxon );
        } else {
            gfs.updateMultifunctionality();
        }

        return null;
    }

    @Override
    public String getShortDesc() {
        return "Update or create gene multifunctionality metrics";
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        if ( this.hasOption( 't' ) ) {
            String taxonName = this.getOptionValue( 't' );
            TaxonService taxonService = this.getBean( TaxonService.class );
            this.taxon = taxonService.findByCommonName( taxonName );
            if ( taxon == null ) {
                AbstractCLI.log.error( "ERROR: Cannot find taxon " + taxonName );
            }
        }
    }

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.SYSTEM;
    }

}
