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

import java.io.IOException;
import java.util.Collection;

import ubic.gemma.loader.description.GeneOntologyFetcher;
import ubic.gemma.loader.description.GeneOntologyLoader;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Simple command line to load the GO into Gemma.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GOLoaderCli extends AbstractSpringAwareCLI {

    public static void main( String[] args ) {
        GOLoaderCli p = new GOLoaderCli();
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
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @Override
    protected void buildOptions() {
        // nothing to do.
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Gene Ontology Loader", args );
        if ( err != null ) return err;

        try {
            GeneOntologyFetcher fetcher = new GeneOntologyFetcher();
            Collection<LocalFile> files = fetcher.fetch( "GO" );
            LocalFile f = files.iterator().next();
            GeneOntologyLoader loader = new GeneOntologyLoader();
            loader.setPersisterHelper( ( PersisterHelper ) this.getBean( "persisterHelper" ) );
            loader.load( f.asFile() );
        } catch ( IOException e ) {
            log.error( e );
            return e;
        }
        return null;

    }

}
