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
package ubic.gemma.apps;

import java.io.IOException;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;

import ubic.gemma.externalDb.GoldenPathDumper;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.loader.genome.goldenpath.GoldenPathBioSequenceLoader;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Bulk load BioSequence instances taken from GoldenPath.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GoldenPathBioSequenceLoaderCLI extends AbstractSpringAwareCLI {

    public static void main( String[] args ) {
        GoldenPathBioSequenceLoaderCLI p = new GoldenPathBioSequenceLoaderCLI();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    private ExternalDatabaseService externalDatabaseService;
    private BioSequenceService bioSequenceService;
    private TaxonService taxonService;
    private String taxonName;
    private String fileArg;

    private int limitArg = -1;

    public void load( String taxonCommonName, int limit ) {

        Taxon taxon = taxonService.findByCommonName( taxonCommonName );
        if ( taxon == null ) {
            throw new IllegalArgumentException( "No such taxon in system: " + taxonCommonName );
        }
        doLoad( taxon, limit );
    }

    /**
     * Load BioSequences (ESTs and mRNAs) for given taxon from a dump from GoldenPath.
     * 
     * @param taxonCommonName e.g., "rat", "human", "mouse".
     * @param file
     * @throws IOException
     */
    public void load( String taxonCommonName, String file, int limit ) throws IOException {
        Taxon taxon = taxonService.findByCommonName( taxonCommonName );
        if ( taxon == null ) {
            throw new IllegalArgumentException( "No such taxon in system: " + taxonCommonName );
        }
        doLoad( file, taxon, limit );
    }

    /**
     * Load BioSequences (ESTs and mRNAs) for given taxon.
     * 
     * @param taxon
     * @param file
     * @param limit
     * @throws IOException
     */
    public void load( Taxon taxon, String file, int limit ) throws IOException {
        doLoad( file, taxon, limit );
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {

        Option taxonOption = OptionBuilder.hasArg().isRequired().withArgName( "Taxon name" )
                .withDescription( "Taxon common name, e.g., 'rat'" ).withLongOpt( "taxon" ).create( 't' );

        addOption( taxonOption );

        Option fileOption = OptionBuilder.hasArg().withArgName( "Input file" )
                .withDescription( "Path to file (two columns)" ).withLongOpt( "file" ).create( 'f' );

        addOption( fileOption );

        Option limitOption = OptionBuilder.hasArg().withArgName( "Limit" ).withDescription( "Maximum number to load" )
                .withLongOpt( "limit" ).create( 'L' );

        addOption( limitOption );

    }

    @Override
    protected Exception doWork( String[] args ) {
        try {
            Exception err = processCommandLine( "Seqref loader", args );
            if ( err != null ) return err;

            if ( StringUtils.isNotBlank( fileArg ) ) {
                this.load( taxonName, fileArg, limitArg );
            } else {
                this.load( taxonName, limitArg );
            }

        } catch ( Exception e ) {
            log.error( e, e );
            return e;
        }
        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 't' ) ) { // um, required.
            taxonName = getOptionValue( 't' );
        }

        if ( hasOption( 'f' ) ) {
            fileArg = getOptionValue( 'f' );
        }

        if ( hasOption( 'L' ) ) {
            limitArg = getIntegerOptionValue( 'L' );
        }
        // MethodSecurityInterceptor msi = ( MethodSecurityInterceptor ) getBean( "methodSecurityInterceptor" );
        this.bioSequenceService = getBean( BioSequenceService.class );
        this.externalDatabaseService = getBean( ExternalDatabaseService.class );
        this.taxonService = getBean( TaxonService.class );
    }

    private void doLoad( String file, Taxon taxon, int limit ) throws IOException {
        GoldenPathBioSequenceLoader gp = new GoldenPathBioSequenceLoader( taxon );

        gp.setExternalDatabaseService( externalDatabaseService );
        gp.setBioSequenceService( bioSequenceService );
        gp.setLimit( limit );
        gp.load( file );
    }

    private void doLoad( Taxon taxon, int limit ) {
        GoldenPathBioSequenceLoader gp = new GoldenPathBioSequenceLoader( taxon );
        gp.setExternalDatabaseService( externalDatabaseService );
        gp.setBioSequenceService( bioSequenceService );
        GoldenPathDumper dumper = new GoldenPathDumper( taxon );
        externalDatabaseService = this.getBean( ExternalDatabaseService.class );

        dumper.setExternalDatabaseService( externalDatabaseService );
        gp.setLimit( limit );
        gp.load( dumper );

    }

}
