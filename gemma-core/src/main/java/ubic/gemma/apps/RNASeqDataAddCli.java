/*
 * The Gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
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
package ubic.gemma.apps;

import java.io.IOException;
import java.util.Collection;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.loader.expression.geo.DataUpdater;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Designed to add count and/or RPKM data to a data set that has only meta-data.
 * 
 * @author Paul
 * @version $Id$
 */
public class RNASeqDataAddCli extends ExpressionExperimentManipulatingCLI {

    private static final String ALLOW_MISSING = "allowMissing";
    private static final String COUNT_FILE_OPT = "count";
    private static final String METADATAOPT = "rlen";
    private static final String RPKM_FILE_OPT = "rpkm";

    /**
     * @param args
     */
    public static void main( String[] args ) {
        RNASeqDataAddCli c = new RNASeqDataAddCli();
        c.doWork( args );
    }

    private boolean allowMissingSamples = false;

    private String countFile = null;

    private boolean isPairedReads = false;
    private String platformName = null;
    private Integer readLength = null;
    private String rpkmFile = null;
    @Override
    public String getShortDesc() {
        return "Add expression quantifiation to an RNA-seq experiment";
    }

    @Override
    protected void buildOptions() {
        super.buildOptions();
        super.addOption( RPKM_FILE_OPT, true, "File with RPKM data" );
        super.addOption( COUNT_FILE_OPT, true, "File with count data" );

        super.addOption( COUNT_FILE_OPT, true, "File with count data" );
        super.addOption( ALLOW_MISSING, false, "Set this if your data files don't have information for all samples." );
        super.addOption( "a", true, "Target platform (must already exist in the system)" );

        super.addOption( METADATAOPT, true,
                "Information on read length given as a string like '100:paired', '36 (assumed unpaired)', or '36:unpaired' " );

    }

    @Override
    protected Exception doWork( String[] args ) {
        super.processCommandLine( "RNASeqDataAdd", args );

        DataUpdater serv = getBean( DataUpdater.class );

        if ( this.expressionExperiments.size() > 1 ) {
            throw new IllegalArgumentException( "Sorry, can only process one experiment with this tool." );
        }

        ArrayDesign targetArrayDesign = locateArrayDesign( this.platformName );

        if ( this.expressionExperiments.size() > 1 ) {
            log.warn( "This CLI can only deal with one experiment at a time; only the first one will be processed" );
        }

        ExpressionExperiment ee = ( ExpressionExperiment ) this.expressionExperiments.iterator().next();

        DoubleMatrixReader reader = new DoubleMatrixReader();
        try {
            DoubleMatrix<String, String> countMatrix = null;
            DoubleMatrix<String, String> rpkmMatrix = null;
            if ( this.countFile != null ) {
                countMatrix = reader.read( countFile );
            }

            if ( this.rpkmFile != null ) {
                rpkmMatrix = reader.read( rpkmFile );
            }

            serv.addCountData( ee, targetArrayDesign, countMatrix, rpkmMatrix, readLength, isPairedReads,
                    allowMissingSamples );

        } catch ( IOException e ) {
            log.error( "Failed while processing " + ee, e );
            return e;
        }
        return null;
    }

    protected ArrayDesign locateArrayDesign( String name ) {

        ArrayDesign arrayDesign = null;
        ArrayDesignService arrayDesignService = getBean( ArrayDesignService.class );
        Collection<ArrayDesign> byname = arrayDesignService.findByName( name.trim().toUpperCase() );
        if ( byname.size() > 1 ) {
            throw new IllegalArgumentException( "Ambiguous name: " + name );
        } else if ( byname.size() == 1 ) {
            arrayDesign = byname.iterator().next();
        }

        if ( arrayDesign == null ) {
            arrayDesign = arrayDesignService.findByShortName( name );
        }

        if ( arrayDesign == null ) {
            log.error( "No arrayDesign " + name + " found" );
            bail( ErrorCode.INVALID_OPTION );
        }
        return arrayDesign;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( RPKM_FILE_OPT ) ) {
            this.rpkmFile = getOptionValue( RPKM_FILE_OPT );
        }

        if ( hasOption( COUNT_FILE_OPT ) ) {
            this.countFile = getOptionValue( COUNT_FILE_OPT );
        }

        if ( hasOption( METADATAOPT ) ) {
            String metaString = getOptionValue( METADATAOPT );
            String[] msf = metaString.split( ":" );

            if ( msf.length > 2 ) {
                throw new IllegalArgumentException( METADATAOPT
                        + " must be supplied with string in format N:{unpaired|paired}" );
            }

            this.readLength = Integer.parseInt( msf[0] );

            if ( msf.length == 2 ) {
                if ( msf.equals( "paired" ) ) {
                    this.isPairedReads = true;
                } else if ( msf.equals( "unpaired" ) ) {
                    this.isPairedReads = false;
                } else {
                    throw new IllegalArgumentException( "Value must be either 'paired' or 'unpaired' or left blank" );
                }
            }

        }

        this.allowMissingSamples = hasOption( ALLOW_MISSING );

        if ( rpkmFile == null && countFile == null )
            throw new IllegalArgumentException( "Must provide either RPKM or count data (or both)" );

        if ( !hasOption( "a" ) ) {
            throw new IllegalArgumentException( "Must provide target platform" );
        }

        this.platformName = getOptionValue( "a" );

    }

}
