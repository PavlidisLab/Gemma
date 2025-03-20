package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataType;
import ubic.gemma.core.loader.expression.singleCell.transform.*;
import ubic.gemma.core.util.AbstractCLI;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Transform various single-cell formats.
 * @author poirigui
 */
public class SingleCellDataTransformCli extends AbstractCLI {

    private static final String PYTHON_OPTION = "python";

    private static final Map<String, Class<? extends SingleCellDataTransformation>> transformationsByName = new LinkedHashMap<>();

    static {
        transformationsByName.put( "transpose", SingleCellDataTranspose.class );
        transformationsByName.put( "pack", SingleCellDataPack.class );
        transformationsByName.put( "sortBySample", SingleCellDataSortBySample.class );
        transformationsByName.put( "sample", SingleCellDataSample.class );
        transformationsByName.put( "rewrite", SingleCellDataRewrite.class );
        transformationsByName.put( "unraw", SingleCellDataUnraw.class );
        transformationsByName.put( "sparsify", SingleCellDataSparsify.class );
    }

    @Value("${python.exe}")
    private Path pythonExecutable;

    @Nullable
    private String operation;
    @Nullable
    private SingleCellDataTransformation transformation;

    public SingleCellDataTransformCli() {
        setAllowPositionalArguments();
    }

    @Override
    protected void buildOptions( Options options ) {
        options.addOption( PYTHON_OPTION, "python", true, "Override the Python executable to use (defaults to " + pythonExecutable + ")" );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.hasOption( PYTHON_OPTION ) ) {
            pythonExecutable = Paths.get( commandLine.getOptionValue( PYTHON_OPTION ) );
        }
        LinkedList<String> positionalArguments = new LinkedList<>( commandLine.getArgList() );
        if ( positionalArguments.isEmpty() ) {
            throw new ParseException( "No operation specified. Possible values are: " + String.join( ", ", transformationsByName.keySet() ) + "." );
        }
        transformation = parseNextTransformation( positionalArguments );
        if ( !positionalArguments.isEmpty() ) {
            throw new ParseException( "Unused positional arguments: " + String.join( " ", positionalArguments ) );
        }
    }

    private SingleCellDataTransformation parseNextTransformation( LinkedList<String> positionalArguments ) throws ParseException {
        operation = positionalArguments.removeFirst();
        Class<? extends SingleCellDataTransformation> transformationClass = transformationsByName.get( operation );
        if ( transformationClass == null ) {
            throw new ParseException( String.format( "Unknown operation: %s. Possible values are: %s", operation,
                    String.join( ", ", transformationsByName.keySet() ) ) );
        }
        transformation = BeanUtils.instantiate( transformationClass );
        if ( transformation instanceof PythonBasedSingleCellDataTransformation ) {
            ( ( PythonBasedSingleCellDataTransformation ) transformation )
                    .setPythonExecutable( pythonExecutable );
        }
        if ( transformation instanceof SingleCellInputOutputFileTransformation ) {
            if ( positionalArguments.size() < 2 ) {
                throw new ParseException( "Two arguments are expected for input and output files." );
            }
            Path inputFile = Paths.get( positionalArguments.removeFirst() );
            ( ( SingleCellInputOutputFileTransformation ) transformation ).setInputFile( inputFile, detectDataType( inputFile ) );
            Path outputFile = Paths.get( positionalArguments.removeFirst() );
            ( ( SingleCellInputOutputFileTransformation ) transformation ).setOutputFile( outputFile, detectDataType( outputFile ) );
        }
        if ( transformation instanceof SingleCellDataSortBySample ) {
            if ( positionalArguments.isEmpty() ) {
                throw new ParseException( "One argument is expected for the sample column name." );
            }
            ( ( SingleCellDataSortBySample ) transformation )
                    .setSampleColumnName( positionalArguments.removeFirst() );
        } else if ( transformation instanceof SingleCellDataSample ) {
            if ( positionalArguments.size() < 2 ) {
                throw new ParseException( "Two arguments are expected for number of cells and genes." );
            }
            ( ( SingleCellDataSample ) transformation )
                    .setNumberOfCells( Integer.parseInt( positionalArguments.removeFirst() ) );
            ( ( SingleCellDataSample ) transformation )
                    .setNumberOfGenes( Integer.parseInt( positionalArguments.removeFirst() ) );
        }
        return transformation;
    }

    @Override
    protected void doWork() throws Exception {
        Assert.notNull( transformation );
        transformation.perform();
    }

    @Nullable
    @Override
    public String getCommandName() {
        return "transformSingleCellData";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Transform single-cell data in various ways";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.EXPERIMENT;
    }

    /**
     * Adjust usage to reflect the transformation-specific options.
     */
    @Override
    protected String getUsage() {
        String commandName = this.getCliContext().getCommandNameOrAliasUsed();
        if ( commandName == null ) {
            commandName = getClass().getName();
        }
        StringBuilder usage = new StringBuilder();
        usage.append( "gemma-cli [options] " )
                .append( commandName )
                .append( " [commandOptions]" );
        if ( operation != null ) {
            usage.append( " " ).append( operation );
        } else {
            usage.append( " <operation>" );
        }
        if ( transformation != null ) {
            if ( transformation instanceof SingleCellInputOutputFileTransformation ) {
                usage.append( " <inputFile> <outputFile>" );
            }
            if ( transformation instanceof SingleCellDataSortBySample ) {
                usage.append( " <sampleColumnName>" );
            } else if ( transformation instanceof SingleCellDataSample ) {
                usage.append( " <numberOfCells> <numberOfGenes>" );
            }
        } else {
            usage.append( " [operationOptions]" );
        }
        return usage.toString();
    }

    @Nullable
    @Override
    protected String getHelpFooter() {
        if ( transformation != null ) {
            return transformation.getDescription();
        } else {
            int len = transformationsByName.keySet().stream().mapToInt( String::length ).max().orElse( 0 );
            return "Operations:\n" + transformationsByName.entrySet().stream()
                    .map( e -> String.format( "%s%s%s", StringUtils.rightPad( e.getKey(), len ), StringUtils.repeat( ' ', HelpFormatter.DEFAULT_DESC_PAD ), BeanUtils.instantiate( e.getValue() ).getDescription() ) )
                    .collect( Collectors.joining( "\n" ) );
        }
    }

    /**
     * TODO: detect data types although we only support AnnData for now.
     */
    private SingleCellDataType detectDataType( Path outputFile ) {
        Assert.notNull( transformation );
        if ( transformation instanceof AbstractPythonScriptBasedAnnDataTransformation ) {
            return SingleCellDataType.ANNDATA;
        } else {
            throw new UnsupportedOperationException( "Detecting data type for " + transformation.getClass().getName() + " is not supported." );
        }
    }
}
