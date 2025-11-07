package ubic.gemma.apps;

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
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Base class for CLI tools that manipulate expression experiment vectors.
 *
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

    private boolean allQuantitationTypes = false;
    private boolean defaultToPreferredQuantitationType = false;

    @Nullable
    private String qtIdentifier;

    protected ExpressionExperimentVectorsManipulatingCli( Class<T> dataVectorType ) {
        this.dataVectorType = dataVectorType;
    }

    /**
     * Use the preferred QT if no identifier is provided, otherwise process all the QTs.
     * <p>
     * When this is set, an {@code -allQts} option will be added to process all QTs.
     */
    protected void setDefaultToPreferredQuantitationType() {
        Assert.state( !this.defaultToPreferredQuantitationType, "Use preferred quantitation type is already set" );
        this.defaultToPreferredQuantitationType = true;
    }

    @Override
    protected final void buildExperimentOptions( Options options ) {
        options.addOption( Option.builder( "qt" )
                .longOpt( "quantitation-type" )
                .hasArg()
                .required( false )
                .desc( "Identifier of the quantitation type to process"
                        + ( " (defaults to " + ( defaultToPreferredQuantitationType ? "the preferred one" : "all of them" ) + ")" ) )
                .get() );
        if ( defaultToPreferredQuantitationType ) {
            options.addOption( "allQts", "all-quantitation-types", false, "Process all quantitation types (defaults to the preferred one)" );
        }
        buildExperimentVectorsOptions( options );
    }

    protected void buildExperimentVectorsOptions( Options options ) {

    }

    @Override
    protected final void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        qtIdentifier = commandLine.getOptionValue( "qt" );
        allQuantitationTypes = commandLine.hasOption( "allQts" );
        processExperimentVectorsOptions( commandLine );
    }

    protected void processExperimentVectorsOptions( CommandLine commandLine ) throws ParseException {

    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) throws Exception {
        Map<Class<? extends T>, Collection<QuantitationType>> qts;
        if ( qtIdentifier != null ) {
            Map.Entry<Class<? extends T>, QuantitationType> vqt = entityLocator.locateQuantitationType( expressionExperiment, qtIdentifier, quantitationTypeService.getMappedDataVectorType( dataVectorType ) );
            qts = Collections.singletonMap( vqt.getKey(), Collections.singleton( vqt.getValue() ) );
        } else if ( allQuantitationTypes ) {
            qts = quantitationTypeService.getMappedDataVectorType( dataVectorType ).stream()
                    .collect( Collectors.toMap( vt -> vt, vt -> quantitationTypeService.findByExpressionExperiment( expressionExperiment, vt ) ) );
        } else if ( defaultToPreferredQuantitationType ) {
            qts = quantitationTypeService.getMappedDataVectorType( dataVectorType ).stream()
                    .collect( Collectors.toMap( vt -> vt, vt -> Collections.singleton( locatePreferredQuantitationType( expressionExperiment, vt ) ) ) );
        } else {
            qts = quantitationTypeService.getMappedDataVectorType( dataVectorType ).stream()
                    .collect( Collectors.toMap( vt -> vt, vt -> quantitationTypeService.findByExpressionExperiment( expressionExperiment, vt ) ) );
        }
        for ( Map.Entry<Class<? extends T>, Collection<QuantitationType>> e : qts.entrySet() ) {
            Class<? extends T> vectorType = e.getKey();
            for ( QuantitationType qt : e.getValue() ) {
                try {
                    processExpressionExperimentVectors( expressionExperiment, qt, vectorType );
                } catch ( Exception e2 ) {
                    if ( isAbortOnError() ) {
                        throw e2;
                    } else {
                        addErrorObject( expressionExperiment, qt, "Error while processing " + qt, e2 );
                    }
                }
            }
        }
    }

    protected void processExpressionExperimentVectors( ExpressionExperiment ee, QuantitationType qt, Class<? extends T> vectorType ) throws Exception {
        processExpressionExperimentVectors( ee, qt );
    }

    /**
     * Process a set of vectors identified by a {@link QuantitationType}.
     */
    protected void processExpressionExperimentVectors( ExpressionExperiment ee, QuantitationType qt ) throws Exception {
        throw new UnsupportedOperationException( "This command line does support experiment vectors." );
    }

    protected final void addSuccessObject( ExpressionExperiment ee, QuantitationType qt, String message ) {
        addSuccessObject( toBatchObject( ee, qt ), message );
    }

    protected final void addErrorObject( ExpressionExperiment ee, QuantitationType qt, String message ) {
        addErrorObject( toBatchObject( ee, qt ), message );
    }

    protected final void addErrorObject( ExpressionExperiment ee, QuantitationType qt, String message, Throwable throwable ) {
        addErrorObject( toBatchObject( ee, qt ), message, throwable );
    }

    protected final void addErrorObject( ExpressionExperiment ee, QuantitationType qt, Exception exception ) {
        addErrorObject( toBatchObject( ee, qt ), exception );
    }

    private Serializable toBatchObject( ExpressionExperiment ee, QuantitationType qt ) {
        return String.format( "%s (%s)", toBatchObject( ee ), qt.getName() );
    }

    private QuantitationType locatePreferredQuantitationType( ExpressionExperiment expressionExperiment, Class<? extends DataVector> dataVectorType ) {
        if ( RawExpressionDataVector.class.isAssignableFrom( dataVectorType ) ) {
            return eeService.getPreferredQuantitationType( expressionExperiment )
                    .orElseThrow( () -> new IllegalStateException( expressionExperiment + " does not have a preferred set of raw vectors." ) );
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
