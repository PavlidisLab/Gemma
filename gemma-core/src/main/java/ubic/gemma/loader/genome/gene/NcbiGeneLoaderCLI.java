/*
 * The Gemma project
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.loader.genome.gene;

import java.io.File;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.loader.genome.gene.ncbi.NcbiGeneLoader;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Command line interface to gene parsing and loading
 * 
 * @author joseph
 * @version $Id$
 */
public class NcbiGeneLoaderCLI extends AbstractSpringAwareCLI {
    private NcbiGeneLoader loader;

    private String GENEINFO_FILE = "gene_info.gz";
    private String GENE2ACCESSION_FILE = "gene2accession.gz";
    private String GENEHISTORY_FILE = "gene_history.gz";
    private String GENE2ENSEMBL_FILE = "gene2ensembl.gz";
    
    private String filePath = null;

    public NcbiGeneLoaderCLI() {
        super();
    }

    public static void main( String[] args ) {
        NcbiGeneLoaderCLI p = new NcbiGeneLoaderCLI();
        try {
            p.doWork( args );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "NcbiGeneLoaderCLI", args );
        if ( err != null ) return err;
        loader = new NcbiGeneLoader();
        loader.setTaxonService((TaxonService) this.getBean("taxonService"));
        loader.setPersisterHelper( this.getPersisterHelper() );

        if ( filePath != null ) {
            String geneInfoFile = filePath + File.separatorChar + GENEINFO_FILE;
            String gene2AccFile = filePath + File.separatorChar + GENE2ACCESSION_FILE;
            String geneHistoryFile = filePath + File.separatorChar + GENEHISTORY_FILE;
            String geneEnsemblFile = filePath + File.separatorChar + GENE2ENSEMBL_FILE;
            
            loader.load( geneInfoFile, gene2AccFile, geneHistoryFile, geneEnsemblFile, true ); // do filtering of taxa
        } else { /* defaults to download files remotely. */
            loader.load( true );
        }

        return null;
    }

    /**
     * @return Returns the loader
     */
    public NcbiGeneLoader getLoader() {
        return this.loader;
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.loader.util.AbstractSpringAwareCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option pathOption = OptionBuilder.hasArg().withArgName( "Input File Path" ).withDescription(
                "Optional path to the gene_info and gene2accession files" ).withLongOpt( "file" ).create( 'f' );

        addOption( pathOption );

        requireLogin();
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'f' ) ) {
            filePath = getOptionValue( 'f' );
        }
    }

}
