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
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.file.PathUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.service.ExpressionDataFileUtils;
import ubic.gemma.core.datastructure.matrix.io.ExperimentalDesignWriter;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.TsvUtils;
import ubic.gemma.core.util.locking.LockedPath;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static ubic.gemma.cli.util.OptionsUtils.formatOption;

/**
 * Writes out the experimental design for a given experiment. This can be directly read into R.
 *
 * @author keshav
 */
public class ExperimentalDesignWriterCLI extends ExpressionExperimentManipulatingCLI {

    private static final String
            USE_MULTIPLE_ROWS_FOR_ASSAYS = "useMultipleRowsForAssays",
            SEPARATE_SAMPLE_FROM_ASSAYS_IDENTIFIERS_OPTION = "separateSampleFromAssayIdentifiers",
            USE_BIO_ASSAY_IDS = "useBioAssayIds",
            USE_RAW_COLUMN_NAMES_OPTION = "useRawColumnNames",
            USE_PROCESSED_DATA_OPTION = "useProcessedData",
            QUANTITATION_TYPE_OPTION = "quantitationType";

    @Autowired
    private BuildInfo buildInfo;

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    private boolean useMultipleRowsForAssays;
    private boolean separateSampleFromAssaysIdentifiers;
    private boolean useBioAssayIds;
    private boolean useRawColumnNames;
    private boolean useProcessedData;
    @Nullable
    private String quantitationTypeIdentifier;
    private DataFileOptionValue destination;

    @Override
    public String getCommandName() {
        return "printExperimentalDesign";
    }

    @Override
    public String getShortDesc() {
        return "Prints experimental design to a file in a R-friendly format";
    }

    @Override
    protected void buildExperimentOptions( Options options ) {
        options.addOption( STANDARD_LOCATION_OPTION, "standard-location", false, "Write the experimental design to the standard location." );
        options.addOption( Option.builder( OUTPUT_DIR_OPTION ).longOpt( "output-dir" ).hasArg().type( Path.class ).desc( "Directory where to write output files. This option is incompatible with " + formatOption( options, STANDARD_LOCATION_OPTION ) + "." ).get() );
        options.addOption( USE_MULTIPLE_ROWS_FOR_ASSAYS, "use-multiple-rows-for-assays", false, "Use multiple rows for assays." );
        options.addOption( SEPARATE_SAMPLE_FROM_ASSAYS_IDENTIFIERS_OPTION, "separate-sample-from-assays-identifiers", false,
                "Separate sample and assay(s) identifiers in distinct columns named 'Sample' and 'Assays' (instead of a single 'Bioassay' column). The assays will be delimited by a '" + TsvUtils.SUB_DELIMITER + "' character." );
        options.addOption( USE_BIO_ASSAY_IDS, "use-bioassay-ids", false, "Use IDs instead of names or short names for bioassays and samples." );
        options.addOption( USE_RAW_COLUMN_NAMES_OPTION, "use-raw-column-names", false, "Use raw names for the columns, otherwise R-friendly names are used. This option is incompatible with " + formatOption( options, STANDARD_LOCATION_OPTION ) + "." );
        options.addOption( USE_PROCESSED_DATA_OPTION, "use-processed-data", false, "Write the experimental design for the assays of the processed data. This option is incompatible with -quantitationType,--quantitation-type." );
        options.addOption( QUANTITATION_TYPE_OPTION, "quantitation-type", true, "Quantitation type identifier to use when writing the experimental design. If not specified, a generic experimental design will be written. This option is incompatible with -useProcessedData/--use-processed-data." );
        addDataFileOptions( options, "experimental design", true );
        addForceOption( options );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        destination = getDataFileOptionValue( commandLine, true );
        useMultipleRowsForAssays = commandLine.hasOption( USE_MULTIPLE_ROWS_FOR_ASSAYS );
        separateSampleFromAssaysIdentifiers = commandLine.hasOption( SEPARATE_SAMPLE_FROM_ASSAYS_IDENTIFIERS_OPTION );
        useBioAssayIds = commandLine.hasOption( USE_BIO_ASSAY_IDS );
        useRawColumnNames = commandLine.hasOption( USE_RAW_COLUMN_NAMES_OPTION );
        useProcessedData = commandLine.hasOption( USE_PROCESSED_DATA_OPTION );
        quantitationTypeIdentifier = commandLine.getOptionValue( QUANTITATION_TYPE_OPTION );
        if ( useProcessedData && quantitationTypeIdentifier != null ) {
            throw new ParseException( "Options -useProcessedData,--use-processed-data and -quantitationType,--quantitation-type are incompatible." );
        }
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) throws IOException {
        ee = eeService.thawLite( ee );
        Path dest;
        if ( destination.isStandardLocation() ) {
            ExpressionExperiment finalEe = ee;
            dest = expressionDataFileService.writeOrLocateDesignFile( ee, useProcessedData, isForce() )
                    .map( LockedPath::closeAndGetPath )
                    .orElseThrow( () -> new IllegalStateException( finalEe + " does not have an experimental design." ) );
        } else {
            String filename;
            if ( quantitationTypeIdentifier != null ) {
                QuantitationType qt = entityLocator.locateQuantitationType( ee, quantitationTypeIdentifier, RawExpressionDataVector.class );
                filename = ExpressionDataFileUtils.getDesignFileName( ee, qt );
            } else {
                filename = ExpressionDataFileUtils.getDesignFileName( ee, useProcessedData );
            }
            dest = destination.getOutputFile( filename );
            PathUtils.createParentDirectories( dest );
            try ( PrintWriter writer = new PrintWriter( dest.toFile(), StandardCharsets.UTF_8.name() ) ) {
                ExperimentalDesignWriter edWriter = new ExperimentalDesignWriter( entityUrlBuilder, buildInfo, true );
                edWriter.setUseMultipleRowsForAssays( useMultipleRowsForAssays );
                edWriter.setSeparateSampleFromAssaysIdentifiers( separateSampleFromAssaysIdentifiers );
                edWriter.setUseBioAssayIds( useBioAssayIds );
                edWriter.setUseRawColumnNames( useRawColumnNames );
                if ( useProcessedData ) {
                    ExpressionExperiment finalEe1 = ee;
                    QuantitationType qt = eeService.getProcessedQuantitationType( ee )
                            .orElseThrow( () -> new IllegalArgumentException( finalEe1 + " does not have processed data vectors." ) );
                    Collection<BioAssayDimension> dimensions = eeService.getProcessedBioAssayDimensionsWithAssays( ee );
                    if ( dimensions.isEmpty() ) {
                        throw new IllegalStateException( qt + " does not have an associated dimension." );
                    }
                    if ( dimensions.size() > 1 ) {
                        log.warn( "Multiple dimensions found for " + qt + ", this is not supposed to happen for processed data." );
                    }
                    Set<BioAssay> assays = dimensions.stream().map( BioAssayDimension::getBioAssays )
                            .flatMap( Collection::stream )
                            .collect( Collectors.toSet() );
                    edWriter.write( ee, qt, ProcessedExpressionDataVector.class, assays, true, writer );
                } else if ( quantitationTypeIdentifier != null ) {
                    QuantitationType qt = entityLocator.locateQuantitationType( ee, quantitationTypeIdentifier, RawExpressionDataVector.class );
                    Collection<BioAssayDimension> dimensions = eeService.getBioAssayDimensionsWithAssays( ee, qt );
                    if ( dimensions.isEmpty() ) {
                        throw new IllegalStateException( qt + " does not have an associated dimension." );
                    }
                    // more than one dimension is possible for raw data
                    // order is irrelevant since the design writer sorts biomaterials
                    Set<BioAssay> assays = dimensions.stream().map( BioAssayDimension::getBioAssays )
                            .flatMap( Collection::stream )
                            .collect( Collectors.toSet() );
                    edWriter.write( ee, qt, RawExpressionDataVector.class, assays, true, writer );
                } else {
                    edWriter.write( ee, writer );
                }
            }
        }
        addSuccessObject( ee, "Wrote experimental design to " + dest + "." );
    }
}
