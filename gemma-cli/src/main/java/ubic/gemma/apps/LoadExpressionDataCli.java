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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.core.analysis.preprocess.PreprocessingException;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;

/**
 * Simple command line to load expression experiments, either singly or in batches defined on the command line or in a
 * file.
 *
 * @author pavlidis
 */
public class LoadExpressionDataCli extends AbstractAuthenticatedCLI {

    @Autowired
    private ExpressionExperimentService eeService;
    @Autowired
    private PreprocessorService preprocessorService;
    @Autowired
    private GeoService geoService;
    @Autowired
    private ArrayDesignService ads;

    // Command line Options
    private String accessionFile = null;
    private String accessions = null;
    private boolean doMatching = false;
    private boolean force = false;
    private boolean platformOnly = false;
    private boolean allowSubSeriesLoad = false;
    private boolean allowSuperSeriesLoad = false;
    // Service Beans
    private boolean splitByPlatform = false;
    private boolean suppressPostProcessing = false;
    private boolean updateOnly = false;
    private String softFile = null;

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.EXPERIMENT;
    }

    @Override
    public String getCommandName() {
        return "addGEOData";
    }

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

        Option noBioAssayMatching = Option.builder( "m" ).desc( "Try to match samples across platforms (e.g. multi-part microarray platforms)" )
                .longOpt( "match" ).build();

        options.addOption( noBioAssayMatching );

        Option splitByPlatformOption = Option.builder( "splitByPlatform" )
                .desc( "Force data from each platform into a separate experiment" )
                .build();
        options.addOption( splitByPlatformOption );

        Option forceOption = Option.builder( "force" ).desc( "Reload data set if it already exists in system" )
                .longOpt( "force" ).build();
        options.addOption( forceOption );

        Option updateOnly = Option.builder( "update" ).desc( "Update existing experiment in Gemma; overrides all other options except those choosing experiments" )
                .longOpt( "update" ).build();
        options.addOption( updateOnly );

        options.addOption( Option.builder( "nopost" ).desc( "Suppress postprocessing steps" ).build() );

        options.addOption( Option.builder( "softfile" ).desc( "Load directly from soft.gz file; use with single accession via -e" ).hasArg().argName( "soft file path" ).build() );

        /*
         * add 'allowsub/super' series option;
         */
        options.addOption( Option.builder( "allowsuper" ).desc( "Allow sub/super series to be loaded" ).build() );

        addBatchOption( options );
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        if ( accessions == null && accessionFile == null ) {
            throw new IllegalArgumentException(
                    "You must specific either a file or accessions on the command line" );
        }

        if ( softFile != null ) {
            Collection<?> ees = geoService.loadFromSoftFile( accessions, softFile, platformOnly, doMatching, splitByPlatform );
            for ( Object object : ees ) {
                if ( object instanceof ExpressionExperiment ) {
                    addSuccessObject( ( ( ExpressionExperiment ) object ).getShortName() );
                }
            }
            return;
        }

        if ( accessions != null ) {
            log.info( "Got accession(s) from command line " + accessions );
            String[] accsToRun = StringUtils.split( accessions, ',' );

            for ( String accession : accsToRun ) {

                accession = StringUtils.strip( accession );

                if ( StringUtils.isBlank( accession ) ) {
                    continue;
                }

                if ( platformOnly ) {
                    Collection<?> designs = geoService.fetchAndLoad( accession, true, true, false, true, true );
                    for ( Object object : designs ) {
                        assert object instanceof ArrayDesign;
                        ArrayDesign ad = ( ArrayDesign ) object;
                        ad = ads.thawLite( ad );

                        addSuccessObject( ad.getShortName() );
                    }
                } else {
                    this.processAccession( accession );
                }
            }

        }

        if ( accessionFile != null ) {
            log.info( "Loading accessions from " + accessionFile );
            try ( BufferedReader br = Files.newBufferedReader( Paths.get( accessionFile ) ) ) {

                String accession;
                while ( ( accession = br.readLine() ) != null ) {

                    if ( StringUtils.isBlank( accession ) ) {
                        continue;
                    }

                    this.processAccession( accession );

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
            this.doMatching = commandLine.hasOption( 'm' );
        }

        this.suppressPostProcessing = commandLine.hasOption( "nopost" );

        if ( commandLine.hasOption( "softfile" ) ) {
            this.softFile = commandLine.getOptionValue( "softfile" );
            if ( accessions == null || accessions.split( "," ).length > 1 ) {
                throw new IllegalArgumentException( "You must specify exactly one accession to load from a SOFT file" );
            }

        }
    }

    private void processAccession( String accession ) {
        try {

            log.info( " ***** Starting processing of " + accession + " *****" );
            if ( updateOnly ) {
                geoService.updateFromGEO( accession, GeoService.GeoUpdateConfig.builder()
                        .experimentTags( true )
                        .sampleCharacteristics( true )
                        .publications( true )
                        .build() );
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
                addSuccessObject( object.getShortName() );
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
            log.info( "Deleting existing version of " + accession );
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
        log.info( "Postprocessing ..." );
        for ( ExpressionExperiment ee : ees ) {

            try {
                preprocessorService.process( ee );
            } catch ( PreprocessingException e ) {
                addErrorObject( ee.getShortName(), "Experiment was loaded, but there was an error during postprocessing, make sure additional steps are completed", e );
            }
        }
    }
}
