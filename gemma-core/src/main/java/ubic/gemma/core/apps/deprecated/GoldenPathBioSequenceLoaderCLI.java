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
package ubic.gemma.core.apps.deprecated;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.externalDb.GoldenPathDumper;
import ubic.gemma.core.genome.taxon.service.TaxonService;
import ubic.gemma.core.loader.genome.goldenpath.GoldenPathBioSequenceLoader;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.core.util.AbstractCLIContextCLI;

import java.io.IOException;

/**
 * Bulk load BioSequence instances taken from GoldenPath.
 *
 * @author pavlidis
 * @deprecated
 */
@Deprecated
public class GoldenPathBioSequenceLoaderCLI extends AbstractCLIContextCLI {
    private BioSequenceService bioSequenceService;
    private ExternalDatabaseService externalDatabaseService;
    private String fileArg;
    private int limitArg = -1;
    private String taxonName;
    private TaxonService taxonService;

    public static void main( String[] args ) {
        GoldenPathBioSequenceLoaderCLI p = new GoldenPathBioSequenceLoaderCLI();
        tryDoWorkNoExit( p, args );
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.DEPRECATED;
    }

    @Override
    public String getCommandName() {
        return "goldenPathSequenceLoad";
    }

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
            Exception err = processCommandLine( args );
            if ( err != null )
                return err;

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

        gp.setLimit( limit );
        gp.load( dumper );

    }

}