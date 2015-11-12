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

import ubic.gemma.apps.GemmaCLI.CommandGroup;
import ubic.gemma.loader.genome.taxon.TaxonFetcher;
import ubic.gemma.loader.genome.taxon.TaxonLoader;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.util.AbstractCLIContextCLI;

import java.util.Collection;

/**
 * @author pavlidis
 * @version $Id$
 */
public class TaxonLoaderCli extends AbstractCLIContextCLI {

    public static void main( String[] args ) {
        TaxonLoaderCli p = new TaxonLoaderCli();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#getCommandName()
     */
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
    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @Override
    protected void buildOptions() {
        return;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        try {
            Exception err = processCommandLine( args );
            if ( err != null ) return err;
            TaxonFetcher tf = new TaxonFetcher();
            Collection<LocalFile> files = tf.fetch();
            LocalFile names = null;
            for ( LocalFile file : files ) {
                if ( file.getLocalURL().toString().endsWith( "names.dmp" ) ) {
                    names = file;
                }
            }

            if ( names == null ) {
                throw new IllegalStateException( "No names.dmp file" );
            }

            TaxonLoader tl = new TaxonLoader();
            tl.setPersisterHelper( this.getBean( PersisterHelper.class ) );
            int numLoaded = tl.load( names.asFile() );
            log.info( "Loaded " + numLoaded + " taxa" );
        } catch ( Exception e ) {
            log.error( e );
            return e;
        }
        return null;
    }

}
