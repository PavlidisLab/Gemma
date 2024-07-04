package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ubic.gemma.core.loader.expression.singleCell.*;
import ubic.gemma.core.util.AbstractCLI;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.stream.Collectors;

/**
 * Transform various single-cell formats.
 * @author poirigui
 */
@Component
public class SingleCellDataTransformCli extends AbstractCLI {

    @Value("${python.exe}")
    private String pythonExecutable;

    private SingleCellInputOutputFileTransformation transformation;

    public SingleCellDataTransformCli() {
        setAllowPositionalArguments( true );
    }

    @Override
    protected void buildOptions( Options options ) {
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        LinkedList<String> positionalArguments = new LinkedList<>( commandLine.getArgList() );
        if ( positionalArguments.isEmpty() ) {
            throw new ParseException( "No operation specified. Possible values are: transpose, pack, sortBySample, sample." );
        }
        String operation = positionalArguments.removeFirst();
        if ( positionalArguments.size() < 2 ) {
            throw usageException( operation );
        }
        Path inputFile, outputFile;
        inputFile = Paths.get( positionalArguments.removeFirst() );
        outputFile = Paths.get( positionalArguments.removeFirst() );
        switch ( operation ) {
            case "transpose":
                transformation = new SingleCellDataTranspose();
                break;
            case "pack":
                transformation = new SingleCellDataPack();
                break;
            case "sortBySample":
                if ( commandLine.getArgList().size() != 1 ) {
                    throw usageException( operation, "sampleColumnName" );
                }
                transformation = new SingleCellDataSortBySample();
                ( ( SingleCellDataSortBySample ) transformation )
                        .setSampleColumnName( positionalArguments.removeFirst() );
                break;
            case "sample":
                if ( commandLine.getArgList().size() != 2 ) {
                    throw usageException( operation, "sampleColumnName" );
                }
                transformation = new SingleCellDataSample();
                ( ( SingleCellDataSample ) transformation )
                        .setNumberOfCells( Integer.parseInt( positionalArguments.removeFirst() ) );
                ( ( SingleCellDataSample ) transformation )
                        .setNumberOfGenes( Integer.parseInt( positionalArguments.removeFirst() ) );
            default:
                throw new ParseException( "Unknown operation: " + operation + ". Possible values are: transpose, pack, sortBySample, sample." );
        }
        if ( transformation instanceof AbstractPythonScriptBasedAnnDataTransformation ) {
            ( ( AbstractPythonScriptBasedAnnDataTransformation ) transformation ).setPythonExecutable( pythonExecutable );
        }
        transformation.setInputFile( inputFile );
        transformation.setInputDataType( SingleCellDataType.ANNDATA );
        transformation.setOutputFile( outputFile );
        transformation.setOutputDataType( SingleCellDataType.ANNDATA );
    }

    @Override
    protected void doWork() throws Exception {
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

    @Nullable
    private String operation;
    private String[] args = { "inputFile", "outputFile" };

    @Override
    protected String getUsage() {
        return String.format( "gemma-cli [options] %s [commandOptions] %s %s",
                getCommandName(),
                operation != null ? operation : "<operation>",
                Arrays.stream( args ).map( a -> "<" + a + ">" ).collect( Collectors.joining( " " ) ) );
    }

    private ParseException usageException( String op, String... args ) {
        this.operation = op;
        this.args = ArrayUtils.addAll( this.args, args );
        return new ParseException( "" );
    }
}
