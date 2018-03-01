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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import ubic.gemma.core.analysis.preprocess.PreprocessingException;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.model.common.Describable;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

/**
 * Simple command line to load expression experiments, either singly or in batches defined on the command line or in a
 * file.
 *
 * @author pavlidis
 */
public class LoadExpressionDataCli extends AbstractCLIContextCLI {

    // Command line Options
    private String accessionFile = null;
    private String accessions = null;
    private boolean doMatching = true;
    private boolean force = false;
    private boolean platformOnly = false;
    private boolean allowSubSeriesLoad = false;
    private boolean allowSuperSeriesLoad = false;
    // Service Beans
    private ExpressionExperimentService eeService;
    private PreprocessorService preprocessorService;
    private boolean splitByPlatform = false;
    private boolean suppressPostProcessing = false;

    public static void main( String[] args ) {
        LoadExpressionDataCli p = new LoadExpressionDataCli();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            AbstractCLI.log.info( watch.getTime() );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.EXPERIMENT;
    }

    @Override
    public String getCommandName() {
        return "addGEOData";
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option fileOption = OptionBuilder.hasArg().withArgName( "Input file" )
                .withDescription( "Optional path to file with list of experiment accessions to load" )
                .withLongOpt( "file" ).create( 'f' );

        this.addOption( fileOption );

        Option accessionOption = OptionBuilder.hasArg().withArgName( "Accession(s)" )
                .withDescription( "Optional comma-delimited list of accessions (GSE or GDS or GPL) to load" )
                .withLongOpt( "acc" ).create( 'e' );
        this.addOption( accessionOption );

        Option platformOnlyOption = OptionBuilder.withArgName( "Platforms only" ).withDescription(
                "Load platforms (array designs) only; implied if you supply GPL instead of GSE or GDS" )
                .withLongOpt( "platforms" ).create( 'y' );
        this.addOption( platformOnlyOption );

        Option noBioAssayMatching = OptionBuilder.withDescription( "Do not try to match samples across platforms" )
                .withLongOpt( "nomatch" ).create( 'n' );

        this.addOption( noBioAssayMatching );

        Option splitByPlatformOption = OptionBuilder
                .withDescription( "Force data from each platform into a separate experiment. This implies '-nomatch'" )
                .create( "splitByPlatform" );
        this.addOption( splitByPlatformOption );

        Option forceOption = OptionBuilder.withDescription( "Reload data set if it already exists in system" )
                .withLongOpt( "force" ).create( "force" );
        this.addOption( forceOption );

        // Option arrayDesign = OptionBuilder.hasArg().withArgName( "array design name" )
        // .withDescription( "Specify the name or short name of the platform the experiment uses (AE only)" )
        // .withLongOpt( "array" ).create( 'a' );

        // addOption( arrayDesign );

        this.addOption( OptionBuilder.withDescription( "Suppress postprocessing steps" ).create( "nopost" ) );

        /*
         * add 'allowsub/super' series option;
         */
        this.addOption( OptionBuilder.withDescription( "Allow sub/super series to be loaded" ).create( "allowsuper" ) );
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception err = this.processCommandLine( args );
        if ( err != null ) {
            return err;
        }
        try {

            GeoService geoService = this.getBean( GeoService.class );
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );

            if ( accessions == null && accessionFile == null ) {
                return new IllegalArgumentException(
                        "You must specific either a file or accessions on the command line" );
            }

            if ( accessions != null ) {
                AbstractCLI.log.info( "Got accession(s) from command line " + accessions );
                String[] accsToRun = StringUtils.split( accessions, ',' );

                for ( String accession : accsToRun ) {

                    accession = StringUtils.strip( accession );

                    if ( StringUtils.isBlank( accession ) ) {
                        continue;
                    }

                    if ( platformOnly ) {
                        Collection<?> designs = geoService.fetchAndLoad( accession, true, true, false, true, true );
                        ArrayDesignService ads = this.getBean( ArrayDesignService.class );
                        for ( Object object : designs ) {
                            assert object instanceof ArrayDesign;
                            ArrayDesign ad = ( ArrayDesign ) object;
                            ad = ads.thawLite( ad );

                            successObjects.add( ad.getName() + " (" + ad.getExternalReferences().iterator().next()
                                    .getAccession() + ")" );
                        }
                    } else {
                        this.processAccession( geoService, accession );
                    }
                }

            }

            if ( accessionFile != null ) {
                AbstractCLI.log.info( "Loading accessions from " + accessionFile );
                InputStream is = new FileInputStream( accessionFile );
                try (BufferedReader br = new BufferedReader( new InputStreamReader( is ) )) {

                    String accession;
                    while ( ( accession = br.readLine() ) != null ) {

                        if ( StringUtils.isBlank( accession ) ) {
                            continue;
                        }

                        this.processAccession( geoService, accession );

                    }
                }
            }
            this.summarizeProcessing();
        } catch ( Exception e ) {
            AbstractCLI.log.error( e );
            return e;
        }
        return null;
    }

    @Override
    public String getShortDesc() {
        return "Load data from GEO";
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 'f' ) ) {
            accessionFile = this.getOptionValue( 'f' );
        }

        if ( this.hasOption( 'e' ) ) {
            accessions = this.getOptionValue( 'e' );
        }

        if ( this.hasOption( 'y' ) ) {
            platformOnly = true;
        }

        if ( this.hasOption( "force" ) ) {
            force = true;
        }

        this.allowSubSeriesLoad = this.hasOption( "allowsuper" );
        this.allowSuperSeriesLoad = this.hasOption( "allowsuper" );

        if ( this.hasOption( "splitByPlatform" ) ) {
            this.splitByPlatform = true;
            this.doMatching = false; // defensive
        } else {
            this.splitByPlatform = false;
            this.doMatching = !this.hasOption( 'n' );
        }

        this.suppressPostProcessing = this.hasOption( "nopost" );

        this.eeService = this.getBean( ExpressionExperimentService.class );
        this.preprocessorService = this.getBean( PreprocessorService.class );

    }

    private void processAccession( GeoService geoService, String accession ) {
        try {

            if ( force ) {
                this.removeIfExists( accession );
            }

            @SuppressWarnings("unchecked") Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) geoService
                    .fetchAndLoad( accession, false, doMatching, this.splitByPlatform, this.allowSuperSeriesLoad,
                            this.allowSubSeriesLoad );

            if ( !suppressPostProcessing ) {
                this.postProcess( ees );
            }

            for ( Object object : ees ) {
                assert object instanceof ExpressionExperiment;
                successObjects.add( ( ( Describable ) object ).getName() + " (" + ( ( ExpressionExperiment ) object )
                        .getAccession().getAccession() + ")" );
            }
        } catch ( Exception e ) {
            errorObjects.add( accession + ": " + e.getMessage() );
            AbstractCLI.log
                    .error( "**** Exception while processing " + accession + ": " + e.getMessage() + " ********" );
            AbstractCLI.log.error( e, e );
        }
    }

    /**
     * Delete previous version of the experiment.
     *
     * @param accession accession
     */
    private void removeIfExists( String accession ) {
        DatabaseEntry acDbe = DatabaseEntry.Factory.newInstance();
        acDbe.setAccession( accession );
        ExternalDatabase geo = ExternalDatabase.Factory.newInstance();
        geo.setName( "GEO" );
        acDbe.setExternalDatabase( geo );
        Collection<ExpressionExperiment> existing = eeService.findByAccession( acDbe );

        if ( !existing.isEmpty() ) {
            AbstractCLI.log.info( "Deleting existing version of " + accession );
            for ( ExpressionExperiment expressionExperiment : existing ) {
                eeService.remove( expressionExperiment );
            }
        }
    }

    /**
     * Do missing value and processed vector creation steps.
     *
     * @param ees experiments
     */
    private void postProcess( Collection<ExpressionExperiment> ees ) {
        AbstractCLI.log.info( "Postprocessing ..." );
        for ( ExpressionExperiment ee : ees ) {

            try {
                preprocessorService.process( ee );
            } catch ( PreprocessingException e ) {
                AbstractCLI.log.error( "Experiment was loaded, but there was an error during postprocessing: " + ee
                        + " , make sure additional steps are completed", e );
                errorObjects.add( ee.getShortName() + ": " + e.getMessage() );
            }

        }
    }

}
