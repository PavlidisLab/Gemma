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
package ubic.gemma.loader.association;

import java.io.IOException;
import java.util.Collection;

import ubic.gemma.loader.util.fetcher.HttpFetcher;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Load GO -> gene associations from NCBI.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class NCBIGene2GOAssociationLoaderCLI extends AbstractSpringAwareCLI {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @Override
    protected void buildOptions() {
        // none needed yet.

    }

    public static void main( String[] args ) {
        NCBIGene2GOAssociationLoaderCLI p = new NCBIGene2GOAssociationLoaderCLI();
        try {
            p.doWork( args );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception e = processCommandLine( "GO association loader", args );
        if ( e != null ) {
            log.error( e );
            return e;
        }

        try {
            NCBIGene2GOAssociationParser gene2GOAssParser = new NCBIGene2GOAssociationParser();
            NCBIGene2GOAssociationLoader gene2GOAssLoader = new NCBIGene2GOAssociationLoader();
            gene2GOAssLoader.setPersisterHelper( ( PersisterHelper ) this.getBean( "persisterHelper" ) );

            HttpFetcher fetcher = new HttpFetcher();

            Collection<LocalFile> files = fetcher.fetch( "ftp://ftp.ncbi.nih.gov/gene/DATA/gene2go.gz" );

            assert files.size() == 1;

            LocalFile gene2Gofile = files.iterator().next();

            gene2GOAssParser.parse( gene2Gofile.asFile() );

            Collection<Gene2GOAssociation> results = gene2GOAssLoader.load( gene2GOAssParser.getResults() );

            log.info( results.size() + " GO associations loaded" );

        } catch ( IOException e1 ) {
            log.error( e );
            return e1;
        }

        return null;
    }
}
