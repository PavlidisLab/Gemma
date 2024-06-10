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
import ubic.gemma.core.loader.entrez.pubmed.PubMedService;
import ubic.gemma.core.util.AbstractAuthenticatedCLI;

import java.io.File;

/**
 * Load PubMed files from XML files -- not used routinely!
 *
 * @author pavlidis
 */
public class PubMedLoaderCli extends AbstractAuthenticatedCLI {

    private String directory;

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.MISC;
    }

    @Override
    public String getCommandName() {
        return "pubmedLoad";
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions( Options options ) {
        Option fileOption = Option.builder( "d" ).required().hasArg().argName( "Directory" )
                .desc( "Directory of PubMed XML files to load" ).longOpt( "dir" ).build();
        options.addOption( fileOption );

    }

    @Override
    protected void doWork() throws Exception {
        PubMedService pms = this.getBean( PubMedService.class );
        pms.loadFromDirectory( new File( directory ) );
    }

    @Override
    public String getShortDesc() {
        return "Loads PubMed records into the database from XML files";
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        if ( commandLine.hasOption( 'd' ) ) {
            this.directory = commandLine.getOptionValue( 'd' );
        }
    }

}
