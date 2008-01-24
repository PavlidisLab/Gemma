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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.loader.expression.arrayExpress.ArrayExpressLoadService;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.model.common.Describable;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Simple command line to load expression experiments, either singly or in batches defined on the command line or in a
 * file.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class LoadExpressionDataCli extends AbstractSpringAwareCLI {

    private enum Formats {
        AE, GEO
    };

    @Override
    public String getShortDesc() {
        return "Load data from GEO or ArrayExpress";
    }

    // Command line Options
    protected String accessionFile = null;
    protected String accessions = null;
    protected boolean platformOnly = false;
    protected boolean doMatching = true;
    protected boolean force = false;
    protected String fileFormat = Formats.GEO.toString();
    protected String adName = "none";
    protected boolean aggressive;

    // Service Beans
    protected ExpressionExperimentService eeService;
    protected ArrayDesignService adService;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option fileOption = OptionBuilder.hasArg().withArgName( "Input file" ).withDescription(
                "Optional path to file with list of experiment accessions to load" ).withLongOpt( "file" ).create( 'f' );

        addOption( fileOption );

        Option accessionOption = OptionBuilder.hasArg().withArgName( "Accession(s)" ).withDescription(
                "Optional comma-delimited list of accessions (GSE or GDS) to load" ).withLongOpt( "acc" ).create( 'e' );
        addOption( accessionOption );

        Option platformOnlyOption = OptionBuilder.withArgName( "Platforms only" ).withDescription(
                "Load platforms (array designs) only" ).withLongOpt( "platforms" ).create( 'y' );
        addOption( platformOnlyOption );

        Option noBioAssayMatching = OptionBuilder.withDescription( "Do not try to match samples across platforms" )
                .withLongOpt( "nomatch" ).create( 'n' );

        addOption( noBioAssayMatching );

        Option forceOption = OptionBuilder.withDescription( "Reload data set if it already exists in system" )
                .withLongOpt( "force" ).create( "force" );
        addOption( forceOption );
        Option aggressiveQtRemoval = OptionBuilder.withDescription(
                "Aggressively remove all unneeded quantitation types" ).withLongOpt( "aggressive" ).create(
                "aggressive" );

        addOption( aggressiveQtRemoval );

        Option fileFormat = OptionBuilder.hasArg().withArgName( "File Format" ).withDescription(
                "Either AE or GEO; defaults to GEO (using batch file does not work with Array Express)" ).withLongOpt(
                "format" ).create( 'm' );

        addOption( fileFormat );

        Option arrayDesign = OptionBuilder.hasArg().withArgName( "array design name" ).withDescription(
                "Specify the name or short name of the platform the experiment uses (AE only)" ).withLongOpt( "array" )
                .create( 'a' );

        addOption( arrayDesign );

    }

    /**
     * @param args
     */
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
            log.info( watch.getTime() );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Expression Data loader", args );
        if ( err != null ) {
            return err;
        }
        try {

            GeoDatasetService geoService = ( GeoDatasetService ) this.getBean( "geoDatasetService" );
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );

            ArrayExpressLoadService aeService = ( ArrayExpressLoadService ) this.getBean( "arrayExpressLoadService" );

            if ( accessions == null && accessionFile == null ) {
                return new IllegalArgumentException(
                        "You must specific either a file or accessions on the command line" );
            }

            Boolean aeFlag = false;
            ArrayDesign ad;
            if ( StringUtils.equalsIgnoreCase( Formats.AE.toString(), fileFormat ) ) {

                if ( platformOnly )
                    return new IllegalArgumentException( "Loading 'platform only' not supported for Array Express. " );

                if ( accessionFile != null )
                    return new IllegalArgumentException(
                            "Batch loading via text file not supported for Array Express file formats. " );

                ad = adService.findByShortName( this.adName );
                if ( ad == null ) ad = adService.findByName( this.adName );

                if ( ad == null ) {
                    return new IllegalArgumentException( "Array Design Specified was not valid: " + adName
                            + " Either name is incorrect that Array Design is not in Gemma:" );
                }
                aeFlag = true;
            } else if ( !StringUtils.equalsIgnoreCase( Formats.GEO.toString(), fileFormat ) ) {
                return new IllegalArgumentException( "File format '" + fileFormat + "' is not understood" );
            }

            if ( accessions != null ) {
                log.info( "Got accession(s) from command line " + accessions );
                String[] accsToRun = StringUtils.split( accessions, ',' );

                for ( String accession : accsToRun ) {

                    accession = StringUtils.strip( accession );

                    if ( StringUtils.isBlank( accession ) ) {
                        continue;
                    }

                    if ( aeFlag ) {
                        processAEAccession( aeService, accession );

                    } else if ( platformOnly ) {
                        Collection designs = geoService.fetchAndLoad( accession, true, true, false );
                        for ( Object object : designs ) {
                            assert object instanceof ArrayDesign;
                            successObjects.add( ( ( Describable ) object ).getName()
                                    + " ("
                                    + ( ( ArrayDesign ) object ).getExternalReferences().iterator().next()
                                            .getAccession() + ")" );
                        }
                    } else {
                        processAccession( geoService, accession );
                    }
                }

            }

            if ( accessionFile != null ) {
                log.info( "Loading accessions from " + accessionFile );
                InputStream is = new FileInputStream( accessionFile );
                BufferedReader br = new BufferedReader( new InputStreamReader( is ) );

                String accession = null;
                while ( ( accession = br.readLine() ) != null ) {

                    if ( StringUtils.isBlank( accession ) ) {
                        continue;
                    }

                    processAccession( geoService, accession );

                }
            }
            summarizeProcessing();
        } catch ( Exception e ) {
            log.error( e );
            return e;
        }
        return null;
    }

    protected void processAEAccession( ArrayExpressLoadService aeService, String accession ) {

        try {
            ExpressionExperiment aeExperiment = aeService.load( accession, adName );
            successObjects.add( ( ( Describable ) aeExperiment ).getName() + " (" + ( aeExperiment ).getShortName()
                    + ")" );

        } catch ( Exception e ) {
            errorObjects.add( accession + ": " + e.getMessage() );
            log.error( "**** Exception while processing " + accession + ": " + e.getMessage() + " ********" );
            log.error( e, e );

        }
    }

    @SuppressWarnings("unchecked")
    protected void processAccession( GeoDatasetService geoService, String accession ) {
        try {

            if ( force ) {
                removeIfExists( accession );
            }

            Collection<ExpressionExperiment> ees = geoService.fetchAndLoad( accession, false, doMatching,
                    this.aggressive );

            for ( Object object : ees ) {
                assert object instanceof ExpressionExperiment;
                successObjects.add( ( ( Describable ) object ).getName() + " ("
                        + ( ( ExpressionExperiment ) object ).getAccession().getAccession() + ")" );
            }
        } catch ( Exception e ) {
            errorObjects.add( accession + ": " + e.getMessage() );
            log.error( "**** Exception while processing " + accession + ": " + e.getMessage() + " ********" );
            log.error( e, e );
        }
    }

    /**
     * Delete previous version of the experiment.
     * 
     * @param accession
     */
    protected void removeIfExists( String accession ) {
        DatabaseEntry acDbe = DatabaseEntry.Factory.newInstance();
        acDbe.setAccession( accession );
        ExternalDatabase geo = ExternalDatabase.Factory.newInstance();
        geo.setName( "GEO" );
        acDbe.setExternalDatabase( geo );
        ExpressionExperiment existing = eeService.findByAccession( acDbe );

        if ( existing != null ) {
            log.info( "Deleting existing version of " + accession );
            eeService.delete( existing );
        }
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'f' ) ) {
            accessionFile = getOptionValue( 'f' );
        }

        if ( hasOption( 'e' ) ) {
            accessions = getOptionValue( 'e' );
        }

        if ( hasOption( 'y' ) ) {
            platformOnly = true;
        }

        if ( hasOption( 'n' ) ) {
            doMatching = false;
        }

        if ( hasOption( "force" ) ) {
            force = true;
        }

        if ( hasOption( "aggressive" ) ) {
            this.aggressive = true;
        }

        if ( hasOption( 'm' ) ) {
            this.fileFormat = getOptionValue( 'm' );
        }

        if ( hasOption( 'a' ) ) {
            this.adName = getOptionValue( 'a' );
        }

        this.eeService = ( ExpressionExperimentService ) getBean( "expressionExperimentService" );
        this.adService = ( ArrayDesignService ) getBean( "arrayDesignService" );

    }

}
