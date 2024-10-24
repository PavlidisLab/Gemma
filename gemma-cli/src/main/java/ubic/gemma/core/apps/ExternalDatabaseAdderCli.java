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
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.AbstractAuthenticatedCLI;
import ubic.gemma.core.util.CLI;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;

/**
 * Add a new external database, but requires editing the code to do so. It can be done by SQL manually as well.
 *
 * @author paul
 */
public class ExternalDatabaseAdderCli extends AbstractAuthenticatedCLI {

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    private String name;
    private DatabaseType type;

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
        return "Add a new external database.";
    }

    @Override
    protected void buildOptions( Options options ) {
        options.addOption( Option.builder( "n" ).longOpt( "name" ).hasArg().required().build() );
        options.addOption( Option.builder( "t" ).longOpt( "type" ).hasArg().required().build() );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        this.name = commandLine.getOptionValue( "n" );
        this.type = DatabaseType.valueOf( commandLine.getOptionValue( "t" ).toUpperCase() );
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        ExternalDatabase created = externalDatabaseService.create( ExternalDatabase.Factory.newInstance( name, type ) );
        log.info( "Created " + created );
    }
}
