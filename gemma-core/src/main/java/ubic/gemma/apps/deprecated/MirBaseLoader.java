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
package ubic.gemma.apps.deprecated;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import ubic.basecode.util.FileTools;
import ubic.gemma.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.genome.taxon.service.TaxonService;
import ubic.gemma.core.loader.genome.GffParser;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.core.util.AbstractCLIContextCLI;

import java.io.InputStream;
import java.util.Collection;

/**
 * Import genes from MirBASE files (http://microrna.sanger.ac.uk/sequences/ftp.shtml). You have to download the file.
 *
 * @author pavlidis
 * @deprecated because we get these genes from NCBI
 */
@Deprecated
public class MirBaseLoader extends AbstractCLIContextCLI {
    private String fileName;
    private GeneService geneService;
    private Persister persisterHelper;
    private String taxonName = null;
    private TaxonService taxonService;

    public static void main( String[] args ) {
        MirBaseLoader p = new MirBaseLoader();
        tryDoWorkNoExit( p, args );
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.DEPRECATED;
    }

    /* (non-Javadoc)
     * @see ubic.gemma.core.util.AbstractCLI#getCommandName()
     */
    @Override
    public String getCommandName() {
        return null;
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option fileOption = OptionBuilder.hasArg().isRequired().withArgName( "GFF file" )
                .withDescription( "Path to GFF file" ).withLongOpt( "file" ).create( 'f' );

        addOption( fileOption );
        Option taxonOption = OptionBuilder.hasArg().withArgName( "taxon" ).isRequired()
                .withDescription( "Taxon common name (e.g., human) for genes to be loaded" ).create( 't' );

        addOption( taxonOption );

    }

    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( args );
        if ( err != null )
            return err;
        try (InputStream gffFileIs = FileTools.getInputStreamFromPlainOrCompressedFile( fileName )) {
            GffParser parser = new GffParser();

            if ( gffFileIs == null ) {
                log.error( "No file " + fileName + " was readable" );
                bail( ErrorCode.INVALID_OPTION );
                return null;
            }

            Taxon taxon;
            taxon = taxonService.findByCommonName( this.taxonName );
            if ( taxon == null ) {
                throw new IllegalArgumentException( "No taxon named " + taxonName );
            }
            parser.setTaxon( taxon );
            parser.parse( gffFileIs );
            gffFileIs.close();
            Collection<Gene> res = parser.getResults();

            geneService = this.getBean( GeneService.class );
            int numFound = 0;
            int notFound = 0;
            for ( Gene gene : res ) {
                Gene found = geneService.find( gene );
                if ( found != null ) {
                    numFound++;
                } else {
                    notFound++;
                }
                gene.setDescription( "Imported from mirbase: micro RNA" );
            }
            log.info( "Found " + numFound + " didn't find " + notFound + " (seem to be new)" );
            persisterHelper.persist( res );

        } catch ( Exception e ) {
            log.error( e, e );
            return e;
        }
        return null;

    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 'f' ) ) {
            this.fileName = this.getOptionValue( 'f' );
        }
        if ( this.hasOption( 't' ) ) {
            this.taxonName = this.getOptionValue( 't' );
        }
        this.taxonService = this.getBean( TaxonService.class );
        persisterHelper = this.getBean( Persister.class );
    }

}
