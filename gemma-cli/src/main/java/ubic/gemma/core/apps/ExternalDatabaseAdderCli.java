/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
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
import org.apache.commons.cli.Options;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.CLI;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;

/**
 * Add a new external database, but requires editing the code to do so. It can be done by SQL manually as well.
 *
 * @author paul
 */
public class ExternalDatabaseAdderCli extends AbstractCLI {

    @Override
    public CommandGroup getCommandGroup() {
        return CLI.CommandGroup.SYSTEM;
    }

    @Override
    public String getCommandName() {
        return "addExternalDatabase";
    }

    @Override
    public String getShortDesc() {
        return "Add a new external database, but requires editing the code to do so. It can be done by SQL manually as well.";
    }

    @Override
    protected void buildOptions( Options options ) {
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {

    }

    @Override
    protected void doWork() throws Exception {
        // ContactService contactService = this.getBean( ContactService.class );

        ExternalDatabase toAdd = ExternalDatabase.Factory.newInstance();

        // Contact c = contactService.findByName( "Affymetrix" ).iterator().next();
        // toAdd.setDatabaseSupplier( c );
        // toAdd.setDescription( "The NetAffx Analysis Center enables researchers to correlate their "
        // + "GeneChip array results with array design and annotation information." );
        // toAdd.setName( "NetAFFX" );
        // toAdd.setType( DatabaseType.SEQUENCE );
        // toAdd.setWebUri( "http://www.affymetrix.com/analysis/index.affx" );

        // Contact c = Contact.Factory.newInstance();
        // c.setName( "McKusick-Nathans Institute of Genetic Medicine" );
        // c = contactService.findOrCreate( c );
        // toAdd.setDatabaseSupplier( c );
        // toAdd.setDescription(
        // "Online Mendelian Inheritance in Man is a comprehensive, authoritative, and timely compendium of human
        // genes and genetic phenotypes. "
        // +
        // "OMIM and Online Mendelian Inheritance in Man are registered trademarks of the Johns Hopkins University."
        // );
        // toAdd.setName( "OMIM" );
        // toAdd.setType( DatabaseType.OTHER );
        // toAdd.setWebUri( "http://omim.org/" );

        ExternalDatabaseService eds = this.getBean( ExternalDatabaseService.class );

        eds.findOrCreate( toAdd );
    }

}
