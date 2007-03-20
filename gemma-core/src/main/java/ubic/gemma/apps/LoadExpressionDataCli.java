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
import java.util.HashSet;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.model.common.Describable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Simple command line to load expression experiments, either singly or in batches defined on the command line or in a
 * file.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class LoadExpressionDataCli extends AbstractSpringAwareCLI {

    private String accessionFile = null;
    private String accessions = null;
    private boolean platformOnly = false;
    private boolean doMatching = true;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option fileOption = OptionBuilder.hasArg().withArgName( "Input file" ).withDescription(
                "Optional path to file (one columns of accessions)" ).withLongOpt( "file" ).create( 'f' );

        addOption( fileOption );

        Option accessionOption = OptionBuilder.hasArg().withArgName( "Accessions" ).withDescription(
                "Optional comma-delimited list of accessions (GSE or GDS)" ).withLongOpt( "acc" ).create( 'a' );
        addOption( accessionOption );

        Option platformOnlyOption = OptionBuilder.withArgName( "Platforms only" ).withDescription(
                "Load platforms (array designs) only" ).withLongOpt( "platforms" ).create( 'y' );
        addOption( platformOnlyOption );

        Option noBioAssayMatching = OptionBuilder.withDescription( "Do not try to match samples across platforms" )
                .withLongOpt( "nomatch" ).create( 'n' );

        addOption( noBioAssayMatching );
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

            Collection<String> errorObjects = new HashSet<String>();
            Collection<String> persistedObjects = new HashSet<String>();

            GeoDatasetService geoService = ( GeoDatasetService ) this.getBean( "geoDatasetService" );
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );

            if ( accessions == null && accessionFile == null ) {
                return new IllegalArgumentException(
                        "You must specific either a file or accessions on the command line" );
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
                        Collection designs = geoService.fetchAndLoad( accession, true, true );
                        for ( Object object : designs ) {
                            assert object instanceof ArrayDesign;
                            persistedObjects.add( ( ( Describable ) object ).getName()
                                    + " ("
                                    + ( ( ArrayDesign ) object ).getExternalReferences().iterator().next()
                                            .getAccession() + ")" );
                        }
                    } else {
                        processAccession( errorObjects, persistedObjects, geoService, accession );
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

                    processAccession( errorObjects, persistedObjects, geoService, accession );

                }
            }

            summarizeProcessing( errorObjects, persistedObjects );

        } catch ( Exception e ) {
            log.error( e );
            return e;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void processAccession( Collection<String> errorObjects, Collection<String> persistedObjects,
            GeoDatasetService geoService, String accession ) {
        try {
            Collection<ExpressionExperiment> ees = geoService.fetchAndLoad( accession, false, doMatching );
            for ( Object object : ees ) {
                assert object instanceof ExpressionExperiment;
                persistedObjects.add( ( ( Describable ) object ).getName() + " ("
                        + ( ( ExpressionExperiment ) object ).getAccession().getAccession() + ")" );
            }
        } catch ( Exception e ) {
            errorObjects.add( accession + ": " + e.getMessage() );
            log.error( "**** Exception while processing " + accession + ": " + e.getMessage() + " ********" );
            log.error( e, e );
        }
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'f' ) ) {
            accessionFile = getOptionValue( 'f' );
        }

        if ( hasOption( 'a' ) ) {
            accessions = getOptionValue( 'a' );
        }

        if ( hasOption( 'y' ) ) {
            platformOnly = true;
        }

        if ( hasOption( 'n' ) ) {
            doMatching = false;
        }
    }

}
