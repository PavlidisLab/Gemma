package ubic.gemma.core.loader.expression.singleCell.transform;

import lombok.Setter;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataType;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filter 10x Chromium Sequencing data to remove low-quality cells.
 * <p>
 * This filter is specific to 10x MEX data, it might not work with other MEX formats.
 * <p>
 * It's not possible to inherit from {@link AbstractPythonScriptBasedTransformation} because Cell Ranger provide its own
 * Anaconda environment with Python pre-installed.
 * @author poirigui
 */
@Setter
public class SingleCell10xMexFilter extends AbstractCellRangerBasedTransformation implements SingleCellInputOutputFileTransformation {

    private Path inputFile;
    private Path outputFile;
    /**
     * Genome identifier to use.
     * <p>
     * This is only used by Cell Ranger to deal with multi-species samples.
     */
    private String genome;
    /**
     * Chemistry to use for filtering data.
     * <p>
     * If null, the defaults parameters will be used.
     */
    @Nullable
    private String chemistry;

    public SingleCell10xMexFilter() {
        super( "filter-10x-mex.py" );
    }

    @Override
    public String getDescription() {
        return "Filter 10x MEX data to remove low-quality cells.";
    }

    @Override
    protected String[] createScriptArgs() {
        Assert.notNull( inputFile, "Input file must be specified." );
        Assert.notNull( outputFile, "Output file must be specified." );
        Assert.notNull( genome, "A genome must be specified." );
        Set<String> supportedChemistryIdentifiers = getChemistryIdentifiers();
        if ( chemistry != null && !supportedChemistryIdentifiers.contains( chemistry ) ) {
            throw new UnsupportedOperationException( String.format( "Unsupported chemistry: %s. Possible values are: %s.",
                    chemistry, supportedChemistryIdentifiers.stream().sorted().collect( Collectors.joining( ", " ) ) ) );
        }
        if ( chemistry != null ) {
            return new String[] {
                    getPythonExecutable().toString(), "-u", "-",
                    inputFile.toString(),
                    outputFile.toString(),
                    genome, chemistry };
        } else {
            return new String[] {
                    getPythonExecutable().toString(), "-u", "-",
                    inputFile.toString(),
                    outputFile.toString(),
                    genome };
        }
    }

    @Override
    public void setInputFile( Path inputFile, SingleCellDataType singleCellDataType ) {
        Assert.isTrue( singleCellDataType == SingleCellDataType.MEX,
                "Only 10x MEX is supported as input for this transformation." );
        this.inputFile = inputFile;
    }

    @Override
    public void setOutputFile( Path outputFile, SingleCellDataType singleCellDataType ) {
        Assert.isTrue( singleCellDataType == SingleCellDataType.MEX,
                "Only MEX is supported as output for this transformation." );
        this.outputFile = outputFile;
    }
}
