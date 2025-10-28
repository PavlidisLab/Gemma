package ubic.gemma.apps;

import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportFileService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * Write the data processing report.
 */
public class ExpressionExperimentDataProcessingReportWriterCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private ExpressionExperimentReportFileService expressionExperimentReportFileService;

    @Override
    public String getCommandName() {
        return "generateDataProcessingReport";
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) throws Exception {
        expressionExperimentReportFileService.writeDataProcessingReport( expressionExperiment,
                new OutputStreamWriter( getCliContext().getOutputStream(), StandardCharsets.UTF_8 ) );
    }
}
