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
package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.loader.association.NCBIGene2GOAssociationLoader;
import ubic.gemma.core.loader.association.NCBIGene2GOAssociationParser;
import ubic.gemma.core.loader.util.fetcher.HttpFetcher;
import ubic.gemma.core.util.AbstractAuthenticatedCLI;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.association.Gene2GOAssociationService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

/**
 * Load GO -&gt; gene associations from NCBI.
 *
 * @author pavlidis
 */
public class NCBIGene2GOAssociationLoaderCLI extends AbstractAuthenticatedCLI {

    private static final String GENE2GO_FILE = "gene2go.gz";

    @Autowired
    private TaxonService taxonService;
    @Autowired
    private Persister persisterHelper;
    @Autowired
    private Gene2GOAssociationService gene2GOAssociationService;
    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    private String filePath = null;

    @Override
    public String getCommandName() {
        return "updateGOAnnots";
    }

    @Override
    protected void buildOptions( Options options ) {
        Option pathOption = Option.builder( "f" ).hasArg().argName( "Input File Path" )
                .desc( "Optional location of the gene2go.gz file" ).longOpt( "file" ).build();

        options.addOption( pathOption );
    }

    @Override
    protected void doWork() throws Exception {
        NCBIGene2GOAssociationLoader gene2GOAssLoader = new NCBIGene2GOAssociationLoader();

        gene2GOAssLoader.setPersisterHelper( persisterHelper );

        Collection<Taxon> taxa = taxonService.loadAll();

        gene2GOAssLoader.setParser( new NCBIGene2GOAssociationParser( taxa ) );

        HttpFetcher fetcher = new HttpFetcher();

        Collection<LocalFile> files;
        if ( filePath != null ) {
            File f = new File( filePath );
            if ( !f.canRead() ) {
                throw new IOException( "Cannot read from " + filePath );
            }
            files = new HashSet<>();
            LocalFile lf = LocalFile.Factory.newInstance();
            lf.setLocalURL( f.toURI() );
            files.add( lf );
        } else {
            files = fetcher.fetch( "ftp://ftp.ncbi.nih.gov/gene/DATA/" + NCBIGene2GOAssociationLoaderCLI.GENE2GO_FILE );
        }
        assert files.size() == 1;
        LocalFile gene2Gofile = files.iterator().next();
        AbstractCLI.log.info( "Removing all old GO associations" );
        gene2GOAssociationService.removeAll();

        AbstractCLI.log.info( "Done, loading new ones" );
        gene2GOAssLoader.load( gene2Gofile );

        ExternalDatabase ed = externalDatabaseService.findByNameWithAuditTrail( ExternalDatabases.GO );
        if ( ed != null ) {
            externalDatabaseService.updateReleaseLastUpdated( ed, null, new Date() );
        } else {
            log.warn( String.format( "No external database with name %s.", ExternalDatabases.GO ) );
        }

        AbstractCLI.log.info( "Don't forget to update the annotation files for platforms." );
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.SYSTEM;
    }

    @Override
    public String getShortDesc() {
        return "Update GO annotations";
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        if ( commandLine.hasOption( 'f' ) ) {
            filePath = commandLine.getOptionValue( 'f' );
        }
    }
}
