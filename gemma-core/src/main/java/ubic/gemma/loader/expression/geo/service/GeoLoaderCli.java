/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.loader.expression.geo.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;

import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.analysis.preprocess.TwoChannelMissingValues;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.analysis.stats.ExpressionDataSampleCorrelation;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * TODO Document Me
 * 
 * @author paul
 * @version $Id$
 */
public class GeoLoaderCli extends AbstractSpringAwareCLI {

    private Collection<String> ges;
    protected ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;
    protected ExpressionDataMatrixService expressionDataMatrixService;
    private TwoChannelMissingValues tcmv;
    protected ExpressionExperimentService eeService;
    private ExpressionExperimentReportService expressionExperimentReportService;

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        this.addOption( OptionBuilder.hasArg().withArgName( "series id" ).withDescription(
                "GSE id to load e.g. GSE1001" ).create( 's' ) );
        this.addOption( OptionBuilder.hasArg().withArgName( "file name" ).withDescription( "File with list of GSE ids" )
                .create( "f" ) );

        this.addOption( OptionBuilder.withDescription( "Postprocess only - for data already in system" ).create(
                "postprocessonly" ) );

        super.requireLogin();

    }

    /**
     * @param fileName
     * @return
     * @throws IOException
     */
    private Collection<String> readExpressionExperimentListFileToStrings( String fileName ) throws IOException {
        Collection<String> eeNames = new HashSet<String>();
        BufferedReader in = new BufferedReader( new FileReader( fileName ) );
        while ( in.ready() ) {
            String line = in.readLine().trim();
            String[] toks = StringUtils.splitPreserveAllTokens( line, "\t" );
            if ( toks.length == 0 ) continue;
            String eeName = toks[0];
            if ( eeName.startsWith( "#" ) ) {
                continue;
            }
            eeNames.add( eeName );
        }
        return eeNames;
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception err = super.processCommandLine( "geo loader", args );

        if ( err != null ) return err;

        GeoDatasetService loader = ( GeoDatasetService ) this.getBean( "geoDatasetService" );

        for ( String gse : ges ) {
            log.info( "***** Loading: " + gse + " ******" );
            try {
                
                // Reset.
                loader.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );

                Collection<ExpressionExperiment> results;
                if ( !this.hasOption( "postprocessonly" ) ) {
                    results = loader.fetchAndLoad( gse, false, false, true, true, false, false );
                } else {
                    results = new HashSet<ExpressionExperiment>();
                    ExpressionExperiment existingEE = eeService.findByShortName( gse );
                    if ( existingEE == null ) {
                        throw new IllegalArgumentException( "No such experiment in system available: " + gse );
                    }

                    results.add( existingEE );
                }
                postProcess( results );

                for ( Object object : results ) {
                    successObjects.add( gse + ": " + object );
                }
            } catch ( Exception e ) {
                log.error( e, e );
                this.errorObjects.add( gse + ": " + e.getMessage() );
            }
        }

        super.summarizeProcessing();

        return null;
    }

    /**
     * @param ee
     * @return
     */
    private boolean processForMissingValues( ExpressionExperiment ee, ArrayDesign design ) {

        boolean wasProcessed = false;

        TechnologyType tt = design.getTechnologyType();
        if ( tt == TechnologyType.TWOCOLOR || tt == TechnologyType.DUALMODE ) {
            log.info( ee + " uses a two-color array design, processing for missing values ..." );
            eeService.thawLite( ee );
            tcmv.computeMissingValues( ee );
            wasProcessed = true;
        }

        return wasProcessed;
    }

    /**
     * Do missing value and processed vector creation steps.
     * 
     * @param ees
     */
    private void postProcess( Collection<ExpressionExperiment> ees ) {
        log.info( "Postprocessing ..." );
        for ( ExpressionExperiment ee : ees ) {

            Collection<ArrayDesign> arrayDesignsUsed = eeService.getArrayDesignsUsed( ee );
            if ( arrayDesignsUsed.size() > 1 ) {
                log.warn( "Skipping postprocessing because experiment uses "
                        + "multiple array types. Please check valid entry and run postprocessing separately." );
            }

            ArrayDesign arrayDesignUsed = arrayDesignsUsed.iterator().next();
            processForMissingValues( ee, arrayDesignUsed );
            Collection<ProcessedExpressionDataVector> dataVectors = processedExpressionDataVectorCreateService
                    .computeProcessedExpressionData( ee );

            ExpressionDataDoubleMatrix datamatrix = expressionDataMatrixService.getFilteredMatrix( ee,
                    new FilterConfig(), dataVectors );
            ExpressionDataSampleCorrelation.process( datamatrix, ee );

            this.expressionExperimentReportService.generateSummaryObject( ee.getId() );

        }
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        if ( this.hasOption( 'f' ) ) {
            try {
                this.ges = readExpressionExperimentListFileToStrings( this.getOptionValue( "f" ) );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        } else if ( this.hasOption( 's' ) ) {
            this.ges = new HashSet<String>();
            this.ges.add( this.getOptionValue( 's' ) );
        } else {
            throw new IllegalArgumentException( "You must specify data sets" );
        }
        this.eeService = ( ExpressionExperimentService ) getBean( "expressionExperimentService" );
        this.processedExpressionDataVectorCreateService = ( ProcessedExpressionDataVectorCreateService ) getBean( "processedExpressionDataVectorCreateService" );
        this.tcmv = ( TwoChannelMissingValues ) this.getBean( "twoChannelMissingValues" );
        this.expressionDataMatrixService = ( ExpressionDataMatrixService ) getBean( "expressionDataMatrixService" );
        expressionExperimentReportService = ( ExpressionExperimentReportService ) getBean( "expressionExperimentReportService" );
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        GeoLoaderCli l = new GeoLoaderCli();
        l.doWork( args );
    }

}
