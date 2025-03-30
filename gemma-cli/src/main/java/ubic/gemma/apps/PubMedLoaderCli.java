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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.loader.entrez.pubmed.PubMedService;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;

import java.io.File;

/**
 * Load PubMed files from XML files -- not used routinely!
 *
 * @author pavlidis
 */
public class PubMedLoaderCli extends AbstractAuthenticatedCLI {

    @Autowired
    private PubMedService pms;

    private String directory;

    @Override
    public String getCommandName() {
        return "pubmedLoad";
    }

    @Override
    public String getShortDesc() {
        return "Loads PubMed records into the database from XML files";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.MISC;
    }

    @Override
    protected void buildOptions( Options options ) {
        Option fileOption = Option.builder( "d" ).required().hasArg().argName( "Directory" )
                .desc( "Directory of PubMed XML files to load" ).longOpt( "dir" ).build();
        options.addOption( fileOption );
    }

    @Override
    protected void doAuthenticatedWork() {
        pms.loadFromDirectory( new File( directory ) );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        if ( commandLine.hasOption( 'd' ) ) {
            this.directory = commandLine.getOptionValue( 'd' );
        }
    }
}
