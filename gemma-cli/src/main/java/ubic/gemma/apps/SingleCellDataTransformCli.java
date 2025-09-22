package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import ubic.gemma.cli.util.AbstractCLI;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataType;
import ubic.gemma.core.loader.expression.singleCell.transform.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Transform various single-cell formats.
 * @author poirigui
 */
public class SingleCellDataTransformCli extends AbstractCLI {

    private static final String
            PYTHON_OPTION = "python",
            INSTALL_PYTHON_DEPENDENCIES_OPTION = "installPythonDeps";

    private static final Map<String, Class<? extends SingleCellDataTransformation>> transformationsByName = new LinkedHashMap<>();

    static {
        transformationsByName.put( "transpose", SingleCellDataTranspose.class );
        transformationsByName.put( "pack", SingleCellDataPack.class );
        transformationsByName.put( "sortBySample", SingleCellDataSortBySample.class );
        transformationsByName.put( "sample", SingleCellDataSample.class );
        transformationsByName.put( "rewrite", SingleCellDataRewrite.class );
        transformationsByName.put( "unraw", SingleCellDataUnraw.class );
        transformationsByName.put( "sparsify", SingleCellDataSparsify.class );
        transformationsByName.put( "filter10x", SingleCell10xMexFilter.class );
    }

    enum Mode {
        INSTALL_PYTHON_DEPENDENCIES,
        TRANSFORM
    }

    @Autowired
    private ApplicationContext ctx;

    @Value("${python.exe}")
    private Path pythonExecutable;

    private Mode mode;

    @Nullable
    private String operation;
    @Nullable
    private SingleCellDataTransformation transformation;

    public SingleCellDataTransformCli() {
        setAllowPositionalArguments();
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

    @Override
    protected void buildOptions( Options options ) {
        options.addOption( PYTHON_OPTION, "python", true, "Override the Python executable to use (defaults to " + pythonExecutable + ")" );
        options.addOption( INSTALL_PYTHON_DEPENDENCIES_OPTION, "install-python-dependencies", false, "Install (or update) Python dependencies." );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.hasOption( PYTHON_OPTION ) ) {
            pythonExecutable = Paths.get( commandLine.getOptionValue( PYTHON_OPTION ) );
        }
        if ( commandLine.hasOption( INSTALL_PYTHON_DEPENDENCIES_OPTION ) ) {
            mode = Mode.INSTALL_PYTHON_DEPENDENCIES;
        } else {
            mode = Mode.TRANSFORM;
            LinkedList<String> positionalArguments = new LinkedList<>( commandLine.getArgList() );
            if ( positionalArguments.isEmpty() ) {
                throw new ParseException( "No operation specified. Possible values are: " + String.join( ", ", transformationsByName.keySet() ) + "." );
            }
            transformation = parseNextTransformation( positionalArguments );
            if ( commandLine.hasOption( PYTHON_OPTION ) ) {
                if ( transformation instanceof PythonBasedSingleCellDataTransformation ) {
                    log.info( "Overriding Python executable of " + transformation.getClass().getName() + " to " + pythonExecutable + "." );
                    ( ( PythonBasedSingleCellDataTransformation ) transformation ).setPythonExecutable( pythonExecutable );
                } else {
                    throw new RuntimeException( transformation.getClass().getName() + " is not Python-based transformation." );
                }
            }
            if ( !positionalArguments.isEmpty() ) {
                throw new ParseException( "Unused positional arguments: " + String.join( " ", positionalArguments ) );
            }
        }
    }

    @Nonnull
    private SingleCellDataTransformation parseNextTransformation( LinkedList<String> positionalArguments ) throws ParseException {
        operation = positionalArguments.removeFirst();
        Class<? extends SingleCellDataTransformation> transformationClass = transformationsByName.get( operation );
        if ( transformationClass == null ) {
            throw new ParseException( String.format( "Unknown operation: %s. Possible values are: %s", operation,
                    String.join( ", ", transformationsByName.keySet() ) ) );
        }
        // TODO: use parameterized getBean() to deal with positional arguments
        transformation = ctx.getBean( transformationClass );
        if ( transformation instanceof SingleCellInputOutputFileTransformation ) {
            if ( positionalArguments.size() < 2 ) {
                throw new ParseException( "Two arguments are expected for input and output files." );
            }
            Path inputFile = Paths.get( positionalArguments.removeFirst() );
            ( ( SingleCellInputOutputFileTransformation ) transformation )
                    .setInputFile( inputFile, detectInputDataType( inputFile ) );
            Path outputFile = Paths.get( positionalArguments.removeFirst() );
            ( ( SingleCellInputOutputFileTransformation ) transformation )
                    .setOutputFile( outputFile, detectOutputDataType( outputFile ) );
        }
        if ( transformation instanceof SingleCell10xMexFilter ) {
            if ( positionalArguments.isEmpty() ) {
                throw new ParseException( "One arguments are expected for the genome." );
            }
            ( ( SingleCell10xMexFilter ) transformation )
                    .setGenome( positionalArguments.removeFirst() );
            if ( !positionalArguments.isEmpty() ) {
                ( ( SingleCell10xMexFilter ) transformation )
                        .setChemistry( positionalArguments.removeFirst() );
            }
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
                    .setNumberOfCellIds( Integer.parseInt( positionalArguments.removeFirst() ) );
            ( ( SingleCellDataSample ) transformation )
                    .setNumberOfGenes( Integer.parseInt( positionalArguments.removeFirst() ) );
        }
        if ( !positionalArguments.isEmpty() ) {
            throw new ParseException( "Unrecognized extraneous arguments: " + String.join( " ", positionalArguments ) );
        }
        return transformation;
    }

    @Override
    protected void doWork() throws Exception {
        if ( mode == Mode.INSTALL_PYTHON_DEPENDENCIES ) {
            installPythonDependencies();
        } else {
            transform();
        }
    }

    private void installPythonDependencies() throws IOException, InterruptedException {
        byte[] requirementsFileContent = IOUtils.toByteArray( requireNonNull( getClass().getResourceAsStream( PythonBasedSingleCellDataTransformation.REQUIREMENTS_FILE ) ) );
        Path requirementsFile = Files.createTempFile( "requirements.txt", null );
        try {
            Files.write( requirementsFile, requirementsFileContent );
            Process proc = new ProcessBuilder( pythonExecutable.toString(), "-m", "pip", "install", "--upgrade", "--upgrade-strategy=eager", "-r", requirementsFile.toString() )
                    .redirectOutput( ProcessBuilder.Redirect.INHERIT )
                    .redirectError( ProcessBuilder.Redirect.PIPE )
                    .start();
            if ( proc.waitFor() != 0 ) {
                throw new RuntimeException( IOUtils.toString( proc.getErrorStream(), StandardCharsets.UTF_8 ) );
            }
        } finally {
            Files.delete( requirementsFile );
        }
    }

    private void transform() throws IOException {
        Assert.notNull( transformation );
        transformation.perform();
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
            if ( transformation instanceof SingleCell10xMexFilter ) {
                usage.append( " <genome> [chemistry]" );
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

    private SingleCellDataType detectInputDataType( Path inputFile ) {
        Assert.notNull( transformation );
        if ( transformation instanceof SingleCell10xMexFilter ) {
            return SingleCellDataType.MEX;
        } else if ( transformation instanceof AbstractPythonScriptBasedAnnDataTransformation ) {
            return SingleCellDataType.ANNDATA;
        } else {
            throw new UnsupportedOperationException( "Detecting data type for input of " + transformation.getClass().getName() + " is not supported." );
        }
    }

    private SingleCellDataType detectOutputDataType( Path outputFile ) {
        Assert.notNull( transformation );
        if ( transformation instanceof SingleCell10xMexFilter ) {
            return SingleCellDataType.MEX;
        } else if ( transformation instanceof AbstractPythonScriptBasedAnnDataTransformation ) {
            return SingleCellDataType.ANNDATA;
        } else {
            throw new UnsupportedOperationException( "Detecting data type for output of " + transformation.getClass().getName() + " is not supported." );
        }
    }
}
