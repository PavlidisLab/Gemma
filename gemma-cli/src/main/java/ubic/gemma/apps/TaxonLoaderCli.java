/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.loader.genome.taxon.TaxonFetcher;
import ubic.gemma.core.loader.genome.taxon.TaxonLoader;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.persistence.persister.PersisterHelper;

import java.io.File;
import java.util.Collection;

/**
 * @author pavlidis
 */
public class TaxonLoaderCli extends AbstractAuthenticatedCLI {

    @Autowired
    private PersisterHelper persisterHelper;

    @Override
    public String getCommandName() {
        return "loadTaxa";
    }

    @Override
    public String getShortDesc() {
        return "Populate taxon tables";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.MISC;
    }

    @Override
    protected void buildOptions( Options options ) {

    }

    @Override
    protected void processOptions( CommandLine commandLine ) {

    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        TaxonFetcher tf = new TaxonFetcher();
        Collection<File> files = tf.fetch();
        File names = null;
        for ( File file : files ) {
            if ( file.toString().endsWith( "names.dmp" ) ) {
                names = file;
            }
        }

        if ( names == null ) {
            throw new IllegalStateException( "No names.dmp file" );
        }

        TaxonLoader tl = new TaxonLoader();
        tl.setPersisterHelper( persisterHelper );
        int numLoaded = tl.load( names );
        log.info( "Loaded " + numLoaded + " taxa" );
    }

}
