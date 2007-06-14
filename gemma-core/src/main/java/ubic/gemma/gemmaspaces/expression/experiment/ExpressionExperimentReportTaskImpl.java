package ubic.gemma.gemmaspaces.expression.experiment;

import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.gemmaspaces.GemmaSpacesResult;

public class ExpressionExperimentReportTaskImpl implements ExpressionExperimentLoadTask {
    
    private ExpressionExperimentReportService expressionExperimentReportService = null;
    
    public GemmaSpacesResult execute( GemmaSpacesExpressionExperimentLoadCommand javaSpacesExpressionExperimentLoadCommand ) {
        
        expressionExperimentReportService.generateSummaryObjects();
        
        return null;
    }
    
    public void setExpressionExperimentReportService( ExpressionExperimentReportService expressionExperimentReportService ) {
        this.expressionExperimentReportService = expressionExperimentReportService;
    }

}
