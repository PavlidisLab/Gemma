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
package ubic.gemma.core.apps;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.loader.expression.DataUpdater;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import java.io.IOException;
import java.util.Collection;

/**
 * Designed to add count and/or RPKM data to a data set that has only meta-data.
 *
 * @author Paul
 */
public class RNASeqDataAddCli extends ExpressionExperimentManipulatingCLI {

    private static final String ALLOW_MISSING = "allowMissing";
    private static final String COUNT_FILE_OPT = "count";
    private static final String METADATAOPT = "rlen";
    private static final String RPKM_FILE_OPT = "rpkm";
    private boolean allowMissingSamples = false;
    private String countFile = null;
    private boolean isPairedReads = false;
    private String platformName = null;
    private Integer readLength = null;
    private String rpkmFile = null;
    private boolean justbackfillLog2cpm = false;

    public static void main( String[] args ) {
        RNASeqDataAddCli c = new RNASeqDataAddCli();
        Exception e = c.doWork( args );
        if ( e != null ) {
            e.printStackTrace();
        }
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.EXPERIMENT;
    }

    @Override
    protected void buildOptions() {
        super.buildOptions();
        super.addOption( RNASeqDataAddCli.RPKM_FILE_OPT, true, "File with RPKM data" );
        super.addOption( RNASeqDataAddCli.COUNT_FILE_OPT, true, "File with count data" );

        super.addOption( RNASeqDataAddCli.COUNT_FILE_OPT, true, "File with count data" );
        super.addOption( RNASeqDataAddCli.ALLOW_MISSING, false,
                "Set this if your data files don't have information for all samples." );
        super.addOption( "a", true, "Target platform (must already exist in the system)" );

        super.addOption( RNASeqDataAddCli.METADATAOPT, true,
                "Information on read length given as a string like '100:paired', '36 (assumed unpaired)', or '36:unpaired' " );

        super.addOption( "log2cpm", false,
                "Just compute log2cpm from the existing stored count data (backfill); batchmode OK, no other options needed" );

    }

    @Override
    protected void processOptions() {
        super.processOptions();

        if ( this.hasOption( "log2cpm" ) ) {
            this.justbackfillLog2cpm = true;

            if ( this.hasOption( RNASeqDataAddCli.RPKM_FILE_OPT ) || this
                    .hasOption( RNASeqDataAddCli.COUNT_FILE_OPT ) ) {
                throw new IllegalArgumentException(
                        "Don't use the log2cpm option when loading new data; just use it to backfill old experiments." );
            }
            return;
        }

        if ( this.hasOption( RNASeqDataAddCli.RPKM_FILE_OPT ) ) {
            this.rpkmFile = this.getOptionValue( RNASeqDataAddCli.RPKM_FILE_OPT );
        }

        if ( this.hasOption( RNASeqDataAddCli.COUNT_FILE_OPT ) ) {
            this.countFile = this.getOptionValue( RNASeqDataAddCli.COUNT_FILE_OPT );
        }

        if ( this.hasOption( RNASeqDataAddCli.METADATAOPT ) ) {
            String metaString = this.getOptionValue( RNASeqDataAddCli.METADATAOPT );
            String[] msf = metaString.split( ":" );

            if ( msf.length > 2 ) {
                throw new IllegalArgumentException(
                        RNASeqDataAddCli.METADATAOPT + " must be supplied with string in format N:{unpaired|paired}" );
            }

            this.readLength = Integer.parseInt( msf[0] );

            if ( msf.length == 2 ) {
                if ( msf[1].equalsIgnoreCase( "paired" ) ) {
                    this.isPairedReads = true;
                } else if ( msf[1].equalsIgnoreCase( "unpaired" ) ) {
                    this.isPairedReads = false;
                } else {
                    throw new IllegalArgumentException( "Value must be either 'paired' or 'unpaired' or left blank" );
                }
            }

        }

        this.allowMissingSamples = this.hasOption( RNASeqDataAddCli.ALLOW_MISSING );

        if ( rpkmFile == null && countFile == null )
            throw new IllegalArgumentException( "Must provide either RPKM or count data (or both)" );

        if ( !this.hasOption( "a" ) ) {
            throw new IllegalArgumentException( "Must provide target platform" );
        }

        this.platformName = this.getOptionValue( "a" );

    }

    @Override
    public String getCommandName() {
        return "rnaseqDataAdd";
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception exception = super.processCommandLine( args );
        if ( exception != null )
            return exception;

        DataUpdater serv = this.getBean( DataUpdater.class );

        if ( this.justbackfillLog2cpm ) {
            for ( BioAssaySet bas : this.expressionExperiments ) {
                try {
                    ExpressionExperiment ee = ( ExpressionExperiment ) bas;
                    Collection<QuantitationType> pqts = this.eeService.getPreferredQuantitationType( ee );
                    if ( pqts.size() > 1 )
                        throw new IllegalArgumentException( "Cannot process when there is more than one preferred QT" );
                    if ( pqts.isEmpty() )
                        throw new IllegalArgumentException( "No preferred quantitation type for " + ee.getShortName() );
                    QuantitationType qt = pqts.iterator().next();
                    if ( !qt.getType().equals( StandardQuantitationType.COUNT ) ) {
                        AbstractCLI.log.warn( "Preferred data is not counts for " + ee );
                        this.errorObjects.add( ee.getShortName() + ": Preferred data is not counts" );
                        continue;
                    }
                    serv.log2cpmFromCounts( ee, qt );
                    this.successObjects.add( ee );
                } catch ( Exception e ) {
                    AbstractCLI.log.error( e, e );
                    this.errorObjects.add( ( ( ExpressionExperiment ) bas ).getShortName() + ": " + e.getMessage() );
                }
            }

            this.summarizeProcessing();
            return null;
        }

        /*
         * Usual cases.
         */
        if ( this.expressionExperiments.size() > 1 ) {
            throw new IllegalArgumentException( "Sorry, can only process one experiment with this tool." );
        }
        ArrayDesign targetArrayDesign = this.locateArrayDesign( this.platformName );

        ExpressionExperiment ee = ( ExpressionExperiment ) this.expressionExperiments.iterator().next();

        if ( this.expressionExperiments.size() > 1 ) {
            AbstractCLI.log
                    .warn( "This CLI can only deal with one experiment at a time; only the first one will be processed" );
        }
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
            AbstractCLI.log.error( "Failed while processing " + ee, e );
            return e;
        }
        return null;
    }

    @Override
    public String getShortDesc() {
        return "Add expression quantifiation to an RNA-seq experiment";
    }

    private ArrayDesign locateArrayDesign( String name ) {

        ArrayDesign arrayDesign = null;
        ArrayDesignService arrayDesignService = this.getBean( ArrayDesignService.class );
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
            AbstractCLI.log.error( "No arrayDesign " + name + " found" );
            this.bail( ErrorCode.INVALID_OPTION );
        }
        return arrayDesign;
    }

}
