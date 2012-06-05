/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.ontology.providers.MgedOntologyService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * @author Paul
 * @version $Id$
 * @see ExperimentalDesignImporter, which this is just an interface for.
 */
public class ExperimentalDesignImportCli extends AbstractSpringAwareCLI {

    /**
     * @param args
     */
    public static void main( String[] args ) {
        ExperimentalDesignImportCli e = new ExperimentalDesignImportCli();
        Exception ex = e.doWork( args );
        if ( ex != null ) {
            log.fatal( ex, ex );
        }
        System.exit( 0 );
    }

    private ExpressionExperiment expressionExperiment;
    private InputStream inputStream;

    private boolean dryRun = false;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {

        Option expOption = OptionBuilder
                .isRequired()
                .hasArg()
                .withArgName( "Expression experiment name" )
                .withDescription(
                        "Expression experiment short name. Most tools recognize comma-delimited values given on the command line, "
                                + "and if this option is omitted, the tool will be applied to all expression experiments." )
                .withLongOpt( "experiment" ).create( 'e' );

        addOption( expOption );

        Option designFileOption = OptionBuilder.hasArg().isRequired().withArgName( "Design file" )
                .withDescription( "Experimental design description file" ).withLongOpt( "designFile" ).create( 'f' );
        addOption( designFileOption );

        Option dryRunOption = OptionBuilder.create( "dryrun" );
        addOption( dryRunOption );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception e = processCommandLine( "experimentalDesignImport", args );
        if ( e != null ) return e;

        MgedOntologyService mos = this.getBean( MgedOntologyService.class );
        mos.startInitializationThread( true );
        while ( !mos.isOntologyLoaded() ) {
            try {
                Thread.sleep( 5000 );
            } catch ( InterruptedException e1 ) {
                //
            }
            log.info( "Waiting for mgedontology to load" );
        }

        ExperimentalDesignImporter edimp = this.getBean( ExperimentalDesignImporter.class );

        try {

            edimp.importDesign( expressionExperiment, inputStream, dryRun );
        } catch ( IOException e1 ) {
            return e1;
        }

        return null;
    }

    /**
     * @param short name of the experiment to find.
     * @return
     */
    protected ExpressionExperiment locateExpressionExperiment( String name ) {

        if ( name == null ) {
            errorObjects.add( "Expression experiment short name must be provided" );
            return null;
        }
        ExpressionExperimentService eeService = this.getBean( ExpressionExperimentService.class );
        ExpressionExperiment experiment = eeService.findByShortName( name );

        if ( experiment == null ) {
            log.error( "No experiment " + name + " found" );
            bail( ErrorCode.INVALID_OPTION );
        }
        return experiment;
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        String shortName = this.getOptionValue( 'e' );
        this.expressionExperiment = locateExpressionExperiment( shortName );
        if ( this.expressionExperiment == null ) {
            throw new IllegalArgumentException( shortName + " not found" );
        }

        File f = new File( this.getOptionValue( 'f' ) );
        if ( !f.canRead() ) {
            throw new IllegalArgumentException( "Cannot read from " + f );
        }

        try {
            inputStream = new FileInputStream( f );
        } catch ( FileNotFoundException e ) {
            throw new RuntimeException( e );
        }

        if ( this.hasOption( "dryrun" ) ) {
            this.dryRun = true;
        }

    }

}
