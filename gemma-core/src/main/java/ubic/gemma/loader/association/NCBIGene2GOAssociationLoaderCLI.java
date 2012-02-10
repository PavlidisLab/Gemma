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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.loader.util.fetcher.HttpFetcher;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Load GO -> gene associations from NCBI.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class NCBIGene2GOAssociationLoaderCLI extends AbstractSpringAwareCLI {

    private static final String GENE2GO_FILE = "gene2go.gz";
    private String filePath = null;

    /*
     * (non-Javadoc)
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option pathOption = OptionBuilder.hasArg().withArgName( "Input File Path" ).withDescription(
                "Optional location of the gene2go.gz file" ).withLongOpt( "file" ).create( 'f' );

        addOption( pathOption );
    }

    public static void main( String[] args ) {
        NCBIGene2GOAssociationLoaderCLI p = new NCBIGene2GOAssociationLoaderCLI();
        try {
            p.doWork( args );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'f' ) ) {
            filePath = getOptionValue( 'f' );
        }
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception e = processCommandLine(
                "Populate or update GO associations for all genes; old associations are deleted first.", args );
        if ( e != null ) {
            log.error( e );
            return e;
        }

        TaxonService taxonService = ( TaxonService ) this.getBean( "taxonService" );

        NCBIGene2GOAssociationLoader gene2GOAssLoader = new NCBIGene2GOAssociationLoader();

        gene2GOAssLoader.setPersisterHelper( this.getPersisterHelper() );

        Collection<Taxon> taxa = taxonService.loadAll();

        gene2GOAssLoader.setParser( new NCBIGene2GOAssociationParser( taxa ) );

        HttpFetcher fetcher = new HttpFetcher();

        Collection<LocalFile> files = null;
        if ( filePath != null ) {
            File f = new File( filePath );
            if ( !f.canRead() ) {
                return new IOException( "Cannot read from " + filePath );
            }
            files = new HashSet<LocalFile>();
            LocalFile lf = LocalFile.Factory.newInstance();
            try {
                lf.setLocalURL( f.toURI().toURL() );
            } catch ( MalformedURLException e1 ) {
                return e1;
            }
            files.add( lf );
        } else {
            files = fetcher.fetch( "ftp://ftp.ncbi.nih.gov/gene/DATA/" + GENE2GO_FILE );
        }
        assert files.size() == 1;
        LocalFile gene2Gofile = files.iterator().next();
        Gene2GOAssociationService ggoserv = ( Gene2GOAssociationService ) this.getBean( "gene2GOAssociationService" );
        log.info( "Removing all old GO associations" );
        ggoserv.removeAll();

        log.info( "Done, loading new ones" );
        gene2GOAssLoader.load( gene2Gofile );

        return null;
    }
}
