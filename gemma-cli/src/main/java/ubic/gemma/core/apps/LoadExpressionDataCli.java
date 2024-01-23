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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.preprocess.PreprocessingException;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.util.AbstractAuthenticatedCLI;
import ubic.gemma.core.util.AbstractCLI;
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
public class LoadExpressionDataCli extends AbstractAuthenticatedCLI {

    // Command line Options
    private String accessionFile = null;
    private String accessions = null;
    private boolean doMatching = true;
    private boolean force = false;
    private boolean platformOnly = false;
    private boolean allowSubSeriesLoad = false;
    private boolean allowSuperSeriesLoad = false;
    // Service Beans
    @Autowired
    private ExpressionExperimentService eeService;
    @Autowired
    private PreprocessorService preprocessorService;
    private boolean splitByPlatform = false;
    private boolean suppressPostProcessing = false;
    private boolean updateOnly = false;

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
    protected void buildOptions( Options options ) {
        Option fileOption = Option.builder( "f" ).hasArg().argName( "Input file" )
                .desc( "Optional path to file with list of experiment accessions to load" )
                .longOpt( "file" ).build();

        options.addOption( fileOption );

        Option accessionOption = Option.builder( "e" ).hasArg().argName( "Accession(s)" )
                .desc( "Optional comma-delimited list of accessions (GSE or GDS or GPL) to load" )
                .longOpt( "acc" ).build();
        options.addOption( accessionOption );

        Option platformOnlyOption = Option.builder( "y" ).argName( "Platforms only" ).desc(
                        "Load platforms (array designs) only; implied if you supply GPL instead of GSE or GDS" )
                .longOpt( "platforms" ).build();
        options.addOption( platformOnlyOption );

        Option noBioAssayMatching = Option.builder( "n" ).desc( "Do not try to match samples across platforms" )
                .longOpt( "nomatch" ).build();

        options.addOption( noBioAssayMatching );

        Option splitByPlatformOption = Option.builder( "splitByPlatform" )
                .desc( "Force data from each platform into a separate experiment. This implies '-nomatch'" )
                .build();
        options.addOption( splitByPlatformOption );

        Option forceOption = Option.builder( "force" ).desc( "Reload data set if it already exists in system" )
                .longOpt( "force" ).build();
        options.addOption( forceOption );

        Option updateOnly = Option.builder( "update" ).desc( "Update existing experiment in Gemma; overrides all other options except those choosing experiments" )
                .longOpt( "update" ).build();
        options.addOption( updateOnly );

        options.addOption( Option.builder( "nopost" ).desc( "Suppress postprocessing steps" ).build() );

        /*
         * add 'allowsub/super' series option;
         */
        options.addOption( Option.builder( "allowsuper" ).desc( "Allow sub/super series to be loaded" ).build() );
    }

    @Override
    protected void doWork() throws Exception {
        GeoService geoService = this.getBean( GeoService.class );
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );

        if ( accessions == null && accessionFile == null ) {
            throw new IllegalArgumentException(
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

                        addSuccessObject( ad );
                    }
                } else {
                    this.processAccession( geoService, accession );
                }
            }

        }

        if ( accessionFile != null ) {
            AbstractCLI.log.info( "Loading accessions from " + accessionFile );
            InputStream is = new FileInputStream( accessionFile );
            try ( BufferedReader br = new BufferedReader( new InputStreamReader( is ) ) ) {

                String accession;
                while ( ( accession = br.readLine() ) != null ) {

                    if ( StringUtils.isBlank( accession ) ) {
                        continue;
                    }

                    this.processAccession( geoService, accession );

                }
            }
        }
    }

    @Override
    public String getShortDesc() {
        return "Load data from GEO";
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        if ( commandLine.hasOption( 'f' ) ) {
            accessionFile = commandLine.getOptionValue( 'f' );
        }

        if ( commandLine.hasOption( 'e' ) ) {
            accessions = commandLine.getOptionValue( 'e' );
        }

        if ( commandLine.hasOption( "update" ) ) {
            this.updateOnly = true;
        }

        if ( commandLine.hasOption( 'y' ) ) {
            platformOnly = true;
        }

        if ( commandLine.hasOption( "force" ) ) {
            force = true;
        }

        this.allowSubSeriesLoad = commandLine.hasOption( "allowsuper" );
        this.allowSuperSeriesLoad = commandLine.hasOption( "allowsuper" );

        if ( commandLine.hasOption( "splitByPlatform" ) ) {
            this.splitByPlatform = true;
            this.doMatching = false; // defensive
        } else {
            this.splitByPlatform = false;
            this.doMatching = !commandLine.hasOption( 'n' );
        }

        this.suppressPostProcessing = commandLine.hasOption( "nopost" );

    }

    private void processAccession( GeoService geoService, String accession ) {
        try {

            log.info(" ***** Starting processing of " + accession + " *****");
            if ( updateOnly ) {
                geoService.updateFromGEO( accession );
                addSuccessObject( accession, "Updated" );
                return;
            }

            if ( force ) {
                this.removeIfExists( accession );
            }

            @SuppressWarnings("unchecked")
            Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) geoService
                    .fetchAndLoad( accession, false, doMatching, this.splitByPlatform, this.allowSuperSeriesLoad,
                            this.allowSubSeriesLoad );

            if ( !suppressPostProcessing ) {
                this.postProcess( ees );
            }

            for ( ExpressionExperiment object : ees ) {
                addSuccessObject( object );
            }
        } catch ( Exception e ) {
            addErrorObject( accession, e );
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
                addErrorObject( ee, "Experiment was loaded, but there was an error during postprocessing, make sure additional steps are completed", e );
            }

        }
    }

}
