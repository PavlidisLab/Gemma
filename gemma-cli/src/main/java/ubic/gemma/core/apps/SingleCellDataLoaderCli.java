package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.loader.expression.singleCell.AnnDataSingleCellDataLoaderConfig;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoaderConfig;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoaderService;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class SingleCellDataLoaderCli extends ExpressionExperimentManipulatingCLI {

    private static final String
            PLATFORM_OPTION = "a",
            DATA_TYPE_OPTION = "dataType",
            QT_NAME_OPTION = "quantitationTypeName",
            CELL_TYPE_ASSIGNMENT_FILE_OPTION = "cellTypeAssignmentFile";


    private static final String ANNDATA_OPTION_PREFIX = "annData";
    private static final String
            ANNDATA_SAMPLE_FACTOR_NAME_OPTION = ANNDATA_OPTION_PREFIX + "SampleFactorName",
            ANNDATA_CELL_TYPE_FACTOR_NAME_OPTION = ANNDATA_OPTION_PREFIX + "CellTypeFactorName",
            ANNDATA_UNKNOWN_CELL_TYPE_INDICATOR_OPTION = ANNDATA_OPTION_PREFIX + "UnknownCellTypeIndicator";

    @Autowired
    private SingleCellDataLoaderService singleCellDataLoaderService;
    @Autowired
    private ArrayDesignService arrayDesignService;

    private ArrayDesign platform;
    @Nullable
    private SingleCellDataType dataType;
    @Nullable
    private String qtName;
    @Nullable
    private Path cta;

    // AnnData
    private String annDataSampleFactorName;
    private String annDataCellTypeFactorName;
    private String annDataUnknownCellTypeIndicator;

    @Nullable
    @Override
    public String getCommandName() {
        return "loadSingleCellData";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Load single-cell data from either AnnData or 10x MEX format.";
    }

    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );
        options.addRequiredOption( PLATFORM_OPTION, "platform", true, "Target platform (must already exist in the system)" );
        options.addOption( DATA_TYPE_OPTION, "data-type", true, "Data type to import. Must be one of " + Arrays.stream( SingleCellDataType.values() ).map( Enum::name ).collect( Collectors.joining( ", " ) ) + "." );
        options.addOption( QT_NAME_OPTION, "quantitation-type-name", false, "Quantitation type to import (optional, use if more than one is present in data)" );
        options.addOption( CELL_TYPE_ASSIGNMENT_FILE_OPTION, "cell-type-assignment-file", false, "Path to a cell type assignment file. If missing, cell type importing will be delegated to a specific loader. For AnnData, you must supply the -" + ANNDATA_CELL_TYPE_FACTOR_NAME_OPTION + " option." );
        // for AnnData
        options.addOption( ANNDATA_SAMPLE_FACTOR_NAME_OPTION, "anndata-sample-factor-name", true, "Name of the factor used for the sample name." );
        options.addOption( ANNDATA_CELL_TYPE_FACTOR_NAME_OPTION, "anndata-cell-type-factor-name", true, "Name of the factor used for the cell type, incompatible with -" + CELL_TYPE_ASSIGNMENT_FILE_OPTION + "." );
        options.addOption( ANNDATA_UNKNOWN_CELL_TYPE_INDICATOR_OPTION, "anndata-unknown-cell-type-indicator", true, "Indicator used for missing cell type. Defaults to using the standard -1 categorical code." );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        super.processOptions( commandLine );
        String platformName = commandLine.getOptionValue( PLATFORM_OPTION );
        try {
            platform = arrayDesignService.loadAndThaw( Long.parseLong( platformName ) );
        } catch ( NumberFormatException e ) {
            platform = arrayDesignService.findByShortName( platformName );
            if ( platform != null ) {
                platform = arrayDesignService.thaw( platform );
            }
        }
        if ( platform == null ) {
            throw new IllegalArgumentException( "No platform with identifier or short name " + platformName + "." );
        }
        if ( commandLine.hasOption( DATA_TYPE_OPTION ) ) {
            dataType = SingleCellDataType.valueOf( commandLine.getOptionValue( DATA_TYPE_OPTION ) );
        }
        qtName = commandLine.getOptionValue( QT_NAME_OPTION );
        cta = commandLine.getParsedOptionValue( CELL_TYPE_ASSIGNMENT_FILE_OPTION );
        if ( dataType == SingleCellDataType.ANNDATA ) {
            annDataSampleFactorName = commandLine.getOptionValue( ANNDATA_SAMPLE_FACTOR_NAME_OPTION );
            annDataCellTypeFactorName = commandLine.getOptionValue( ANNDATA_CELL_TYPE_FACTOR_NAME_OPTION );
            annDataUnknownCellTypeIndicator = commandLine.getOptionValue( ANNDATA_UNKNOWN_CELL_TYPE_INDICATOR_OPTION );
            if ( cta != null && annDataCellTypeFactorName != null ) {
                throw new IllegalArgumentException( String.format( "The -%s option would override the value of -%s.",
                        CELL_TYPE_ASSIGNMENT_FILE_OPTION, ANNDATA_CELL_TYPE_FACTOR_NAME_OPTION ) );
            }
        } else {
            for ( Option o : commandLine.getOptions() ) {
                if ( o.getOpt().startsWith( ANNDATA_OPTION_PREFIX ) ) {
                    throw new IllegalArgumentException( String.format( "Options starting with -%s require -%s to be set to %s.",
                            ANNDATA_OPTION_PREFIX, DATA_TYPE_OPTION, SingleCellDataType.ANNDATA ) );
                }
            }
        }
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        if ( dataType != null ) {
            singleCellDataLoaderService.load( ee, platform, dataType, getConfigForDataType( dataType ) );
        } else {
            singleCellDataLoaderService.load( ee, platform, getConfigForDataType( null ) );
        }
    }

    private SingleCellDataLoaderConfig getConfigForDataType( @Nullable SingleCellDataType dataType ) {
        if ( dataType == SingleCellDataType.ANNDATA ) {
            return AnnDataSingleCellDataLoaderConfig.builder()
                    .quantitationTypeName( qtName )
                    .cellTypeAssignmentPath( cta )
                    .sampleFactorName( annDataSampleFactorName )
                    .cellTypeFactorName( annDataCellTypeFactorName )
                    .unknownCellTypeIndicator( annDataUnknownCellTypeIndicator )
                    .build();
        } else {
            return SingleCellDataLoaderConfig.builder()
                    .quantitationTypeName( qtName )
                    .cellTypeAssignmentPath( cta )
                    .build();
        }
    }
}
