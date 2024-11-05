package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Base class for CLI tools that manipulate expression experiment vectors.
 * @author poirigui
 */
public abstract class ExpressionExperimentVectorsManipulatingCli<T extends DataVector> extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private QuantitationTypeService quantitationTypeService;

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    /**
     * The data vector type this CLI is working with.
     */
    private final Class<T> dataVectorType;

    private boolean quantitationTypeIdentifierRequired = false;
    private boolean usePreferredQuantitationType = false;

    @Nullable
    private String qtIdentifier;

    protected ExpressionExperimentVectorsManipulatingCli( Class<T> dataVectorType ) {
        this.dataVectorType = dataVectorType;
    }

    /**
     * Makes it so that the quantitation type identifier is required.
     * <p>
     * This is incompatible with {@link #setUsePreferredQuantitationType()}.
     */
    public void setQuantitationTypeIdentifierRequired() {
        Assert.state( !this.quantitationTypeIdentifierRequired, "Quantitation type identifier is already required" );
        Assert.state( !this.usePreferredQuantitationType, "Preferred quantitation type is enabled, cannot require an identifier." );
        this.quantitationTypeIdentifierRequired = true;
    }

    /**
     * Use the preferred QT if no identifier is provided, otherwise process all the QTs.
     * <p>
     * This is incompatible with {@link #setQuantitationTypeIdentifierRequired()}.
     */
    public void setUsePreferredQuantitationType() {
        Assert.state( !this.usePreferredQuantitationType, "Use preferred quantitation type is already set" );
        Assert.state( !this.quantitationTypeIdentifierRequired, "Quantitation type identifier is required, cannot default the the preferred one." );
        this.usePreferredQuantitationType = true;
    }

    @Override
    protected final void buildExperimentOptions( Options options ) {
        options.addOption( Option.builder( "qt" )
                .longOpt( "quantitation-type" )
                .hasArg()
                .required( quantitationTypeIdentifierRequired )
                .desc( "Identifier of the quantitation type to use"
                        + ( quantitationTypeIdentifierRequired ? ""
                        : " (defaults to " + ( usePreferredQuantitationType ? "the preferred one" : "all of them" ) + ")" ) )
                .build() );
        buildExperimentVectorsOptions( options );
    }

    protected void buildExperimentVectorsOptions( Options options ) {

    }

    @Override
    protected final void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        qtIdentifier = commandLine.getOptionValue( "qt" );
        processExperimentVectorsOptions( commandLine );
    }

    protected void processExperimentVectorsOptions( CommandLine commandLine ) throws ParseException {

    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        Collection<QuantitationType> qts;
        if ( qtIdentifier != null ) {
            qts = Collections.singleton( entityLocator.locateQuantitationType( expressionExperiment, qtIdentifier, quantitationTypeService.getMappedDataVectorType( dataVectorType ) ) );
        } else if ( usePreferredQuantitationType ) {
            qts = quantitationTypeService.getMappedDataVectorType( dataVectorType ).stream()
                    .map( vt -> locatePreferredQuantitationType( expressionExperiment, vt ) )
                    .collect( Collectors.toSet() );
        } else {
            qts = quantitationTypeService.getMappedDataVectorType( dataVectorType ).stream()
                    .flatMap( vt -> quantitationTypeService.findByExpressionExperiment( expressionExperiment, vt ).stream() )
                    .collect( Collectors.toSet() );
        }
        for ( QuantitationType qt : qts ) {
            try {
                processExpressionExperimentVectors( expressionExperiment, qt );
            } catch ( Exception e ) {
                if ( isAbortOnError() ) {
                    throw e;
                } else {
                    addErrorObject( expressionExperiment, "Error while processing " + qt, e );
                }
            }
        }
    }

    /**
     * Process a set of vectors identified by a {@link QuantitationType}.
     */
    protected abstract void processExpressionExperimentVectors( ExpressionExperiment ee, QuantitationType qt );

    private QuantitationType locatePreferredQuantitationType( ExpressionExperiment expressionExperiment, Class<? extends DataVector> dataVectorType ) {
        if ( RawExpressionDataVector.class.isAssignableFrom( dataVectorType ) ) {
            return eeService.getPreferredQuantitationType( expressionExperiment );
        } else if ( ProcessedExpressionDataVector.class.isAssignableFrom( dataVectorType ) ) {
            Collection<QuantitationType> results = quantitationTypeService.findByExpressionExperiment( expressionExperiment, dataVectorType );
            if ( results.isEmpty() ) {
                return null;
            } else if ( results.size() > 1 ) {
                throw new IllegalStateException( expressionExperiment + " has more than one set of processed vectors." );
            } else {
                return results.iterator().next();
            }
        } else if ( SingleCellExpressionDataVector.class.isAssignableFrom( dataVectorType ) ) {
            return singleCellExpressionExperimentService.getPreferredSingleCellQuantitationType( expressionExperiment )
                    .orElseThrow( () -> new IllegalStateException( expressionExperiment + " does not have a preferred set of single-cell vectors." ) );
        } else {
            throw new IllegalArgumentException( "Unsupported data vector type: " + dataVectorType );
        }
    }
}
