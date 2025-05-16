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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.service.GeneMultifunctionalityPopulationService;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.cli.util.CLI;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.model.genome.Taxon;

import javax.annotation.Nullable;

import static ubic.gemma.cli.util.EntityOptionsUtils.addTaxonOption;

/**
 * @author paul
 */
public class MultifunctionalityCli extends AbstractAuthenticatedCLI {

    @Autowired
    private GeneMultifunctionalityPopulationService gfs;
    @Autowired
    private EntityLocator entityLocator;

    @Nullable
    private String taxonName;

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
        addTaxonOption( options, "t", "taxon", "Taxon to process" );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        if ( commandLine.hasOption( 't' ) ) {
            this.taxonName = commandLine.getOptionValue( 't' );
        }
    }

    @Override
    protected void doAuthenticatedWork() {
        if ( taxonName != null ) {
            Taxon taxon = entityLocator.locateTaxon( taxonName );
            gfs.updateMultifunctionality( taxon );
        } else {
            gfs.updateMultifunctionality();
        }
    }
}
