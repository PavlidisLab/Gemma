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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.service.GeneMultifunctionalityPopulationService;
import ubic.gemma.core.util.AbstractAuthenticatedCLI;
import ubic.gemma.core.util.CLI;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

/**
 * @author paul
 */
public class MultifunctionalityCli extends AbstractAuthenticatedCLI {

    @Autowired
    private GeneMultifunctionalityPopulationService gfs;
    @Autowired
    private TaxonService taxonService;

    private Taxon taxon;

    public MultifunctionalityCli() {
        setRequireLogin();
    }

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
        return CLI.CommandGroup.SYSTEM;
    }

    @Override
    protected void buildOptions( Options options ) {
        Option taxonOption = Option.builder( "t" ).hasArg()
                .desc( "Taxon to process" ).longOpt( "taxon" )
                .build();
        options.addOption( taxonOption );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        if ( commandLine.hasOption( 't' ) ) {
            String taxonName = commandLine.getOptionValue( 't' );
            this.taxon = taxonService.findByCommonName( taxonName );
            if ( taxon == null ) {
                log.error( "ERROR: Cannot find taxon " + taxonName );
            }
        }
    }

    @Override
    protected void doAuthenticatedWork() {
        if ( this.taxon != null ) {
            gfs.updateMultifunctionality( taxon );
        } else {
            gfs.updateMultifunctionality();
        }
    }
}
