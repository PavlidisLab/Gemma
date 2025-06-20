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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.service.ExpressionMetadataChangelogFileService;
import ubic.gemma.core.loader.expression.DataUpdater;
import ubic.gemma.core.loader.expression.sequencing.SequencingMetadata;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static ubic.gemma.cli.util.EntityOptionsUtils.addGenericPlatformOption;

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
    private static final String MULTIQC_METADATA_FILE_OPT = "multiqc";

    @Autowired
    private DataUpdater serv;
    @Autowired
    private ExpressionDataFileService expressionDataFileService;
    @Autowired
    private ExpressionMetadataChangelogFileService expressionMetadataChangelogFileService;

    private boolean allowMissingSamples = false;
    private String countFile = null;
    private Boolean isPairedReads = null;
    private String platformName = null;
    private Integer readLength = null;
    private String rpkmFile = null;
    private boolean justbackfillLog2cpm = false;
    private Path qualityControlReportFile = null;

    @Override
    public String getCommandName() {
        return "rnaseqDataAdd";
    }

    @Override
    public String getShortDesc() {
        return "Add expression quantification to an RNA-seq experiment";
    }

    @Override
    protected void buildExperimentOptions( Options options ) {
        options.addOption( Option.builder( RNASeqDataAddCli.RPKM_FILE_OPT ).longOpt( null ).desc( "File with RPKM data" ).argName( "file path" ).hasArg().build() );
        options.addOption( Option.builder( RNASeqDataAddCli.COUNT_FILE_OPT ).longOpt( null ).desc( "File with count data" ).argName( "file path" ).hasArg().build() );

        options.addOption( Option.builder( RNASeqDataAddCli.COUNT_FILE_OPT ).longOpt( null ).desc( "File with count data" ).argName( "file path" ).hasArg().build() );
        options.addOption( RNASeqDataAddCli.ALLOW_MISSING, "Set this if your data files don't have information for all samples." );
        addGenericPlatformOption( options, "a", "array", "Target platform (must already exist in the system)" );

        options.addOption( Option.builder( RNASeqDataAddCli.METADATAOPT ).longOpt( null ).desc( "Information on read length given as a string like '100:paired', '36:unpaired' or simply '36' if pairedness is unknown" ).argName( "length" ).hasArg().build() );

        options.addOption( "log2cpm", "Just compute log2cpm from the existing stored count data (backfill); batchmode OK, no other options needed" );

        // RNA-Seq pipeline QC report
        options.addOption( RNASeqDataAddCli.MULTIQC_METADATA_FILE_OPT, true, "File containing a MultiQC report" );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.hasOption( "log2cpm" ) ) {
            this.justbackfillLog2cpm = true;

            if ( commandLine.hasOption( RNASeqDataAddCli.RPKM_FILE_OPT ) || commandLine.hasOption( RNASeqDataAddCli.COUNT_FILE_OPT ) ) {
                throw new IllegalArgumentException(
                        "Don't use the log2cpm option when loading new data; just use it to backfill old experiments." );
            }
            return;
        }

        if ( commandLine.hasOption( RNASeqDataAddCli.RPKM_FILE_OPT ) ) {
            this.rpkmFile = commandLine.getOptionValue( RNASeqDataAddCli.RPKM_FILE_OPT );
        }

        if ( commandLine.hasOption( RNASeqDataAddCli.COUNT_FILE_OPT ) ) {
            this.countFile = commandLine.getOptionValue( RNASeqDataAddCli.COUNT_FILE_OPT );
        }

        if ( commandLine.hasOption( RNASeqDataAddCli.METADATAOPT ) ) {
            String metaString = commandLine.getOptionValue( RNASeqDataAddCli.METADATAOPT );
            String[] msf = metaString.split( ":", 2 );

            try {
                this.readLength = Integer.parseInt( msf[0] );
            } catch ( NumberFormatException e ) {
                throw new IllegalArgumentException(
                        RNASeqDataAddCli.METADATAOPT + " must be supplied with string in format 'N:{unpaired|paired}' or simply 'N' if pairedness is unknown." );
            }

            if ( msf.length == 2 ) {
                if ( msf[1].equalsIgnoreCase( "paired" ) ) {
                    this.isPairedReads = true;
                } else if ( msf[1].equalsIgnoreCase( "unpaired" ) ) {
                    this.isPairedReads = false;
                } else {
                    throw new IllegalArgumentException( "Value must be either 'paired' or 'unpaired' or omitted if unknown" );
                }
            }

        }

        this.allowMissingSamples = commandLine.hasOption( RNASeqDataAddCli.ALLOW_MISSING );

        if ( rpkmFile == null && countFile == null )
            throw new IllegalArgumentException( "Must provide either RPKM (-rpkm) or count (-count) data (or both)" );

        if ( !commandLine.hasOption( "a" ) ) {
            throw new IllegalArgumentException( "Must provide target platform (-a)" );
        }

        this.platformName = commandLine.getOptionValue( "a" );

        if ( commandLine.hasOption( RNASeqDataAddCli.MULTIQC_METADATA_FILE_OPT ) ) {
            qualityControlReportFile = Paths.get( commandLine.getOptionValue( RNASeqDataAddCli.MULTIQC_METADATA_FILE_OPT ) );
            if ( !Files.exists( qualityControlReportFile ) || !Files.isReadable( qualityControlReportFile ) ) {
                throw new IllegalArgumentException( "The MultiQC report file must exist and be readable." );
            }
        }
    }

    @Override
    protected void processBioAssaySets( Collection<BioAssaySet> bas ) {
        if ( !justbackfillLog2cpm ) {
            throw new IllegalArgumentException( "Sorry, can only process one experiment with this tool, unless -log2cpm is used." );
        }
        super.processBioAssaySets( bas );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        ee = eeService.thawLite( ee );

        if ( this.justbackfillLog2cpm ) {
            try {
                ExpressionExperiment finalEe = ee;
                QuantitationType qt = this.eeService.getPreferredQuantitationType( ee )
                        .orElseThrow( () -> new IllegalArgumentException( "No preferred quantitation type for " + finalEe.getShortName() ) );
                if ( !qt.getType().equals( StandardQuantitationType.COUNT ) ) {
                    log.warn( "Preferred data is not counts for " + ee );
                    addErrorObject( ee.getShortName(), "Preferred data is not counts" );
                }
                serv.log2cpmFromCounts( ee, qt );
                addSuccessObject( ee );
            } catch ( Exception e ) {
                addErrorObject( ee, e );
            }
            return;
        }

        /*
         * Usual cases.
         */
        ArrayDesign targetArrayDesign = entityLocator.locateArrayDesign( this.platformName );

        try {
            DoubleMatrixReader reader = new DoubleMatrixReader();
            DoubleMatrix<String, String> countMatrix = null;
            DoubleMatrix<String, String> rpkmMatrix = null;
            if ( this.countFile != null ) {
                countMatrix = reader.read( countFile );
            }

            if ( this.rpkmFile != null ) {
                rpkmMatrix = reader.read( rpkmFile );
            }

            // TODO: support per-assay read length and pairedness
            // TODO: support supplying library size from the CLI
            Map<BioAssay, SequencingMetadata> sequencingMetadata = new HashMap<>();
            for ( BioAssay ba : ee.getBioAssays() ) {
                sequencingMetadata.put( ba, SequencingMetadata.builder()
                        .readLength( readLength )
                        .isPaired( isPairedReads )
                        .build() );
            }

            serv.addCountData( ee, targetArrayDesign, countMatrix, rpkmMatrix, sequencingMetadata, allowMissingSamples );
        } catch ( IOException e ) {
            addErrorObject( ee, "Failed to add count and RPKM data.", e );
            return;
        }

        /* copy metadata files */
        if ( qualityControlReportFile != null ) {
            try {
                Path dest = expressionDataFileService.copyMultiQCReport( ee, qualityControlReportFile, true );
                expressionMetadataChangelogFileService.addChangelogEntry( ee, "Added a QC report file." );
                log.info( "Copied QC report file to " + dest + "." );
            } catch ( IOException e ) {
                addErrorObject( ee, "Could not copy the MultiQC report.", e );
            }
        }
    }
}
