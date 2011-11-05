/*
 * The Gemma project
 *import org.apache.commons.cli.Option;
 import org.apache.commons.cli.OptionBuilder;

 import java.util.Collection;

 import ubic.gemma.loader.genome.llnl.ImageCumulativePlatesFetcher;
 import ubic.gemma.model.common.description.LocalFile;
 import ubic.gemma.persistence.PersisterHelper;
 import ubic.gemma.loader.genome.llnl.ImageCumulativePlatesLoader;
 import ubic.gemma.model.common.description.ExternalDatabaseService;
 import ubic.gemma.model.genome.biosequence.BioSequenceService;
 
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

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.loader.genome.llnl.ImageCumulativePlatesFetcher;
import ubic.gemma.loader.genome.llnl.ImageCumulativePlatesLoader;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * @author pavlidis
 * @version $Id$
 * @deprecated
 */
@Deprecated
public class ImageCloneLoaderCli extends AbstractSpringAwareCLI {

    public static void main( String[] args ) {
        ImageCloneLoaderCli p = new ImageCloneLoaderCli();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    private String dateString;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option dateOption = OptionBuilder.hasArg().isRequired().withArgName( "Date on file" )
                .withDescription( "Date in the clone file, such as 20060901" ).withLongOpt( "date" ).create( 'd' );

        addOption( dateOption );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "IMAGE clone loader", args );
        if ( err != null ) return err;
        try {
            ImageCumulativePlatesFetcher fetcher = new ImageCumulativePlatesFetcher();
            Collection<LocalFile> files = fetcher.fetch( dateString );

            assert files.size() == 1;

            LocalFile toLoad = files.iterator().next();

            ImageCumulativePlatesLoader loader = new ImageCumulativePlatesLoader();
            loader.setPersisterHelper( ( PersisterHelper ) this.getBean( "persisterHelper" ) );
            loader.setBioSequenceService( ( BioSequenceService ) this.getBean( "bioSequenceService" ) );
            loader.setExternalDatabaseService( ( ExternalDatabaseService ) this.getBean( "externalDatabaseService" ) );
            int numLoaded = loader.load( toLoad.asFile() );
            log.info( "Loaded " + numLoaded + " IMAGE clones" );
        } catch ( IOException e ) {
            log.error( e );
            return e;
        }
        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'd' ) ) { // um, required.
            dateString = getOptionValue( 'd' );
        }
    }
}
