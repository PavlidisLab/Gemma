package ubic.gemma.core.analysis.report;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.util.ResourceUtils;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static ubic.gemma.core.analysis.service.ExpressionDataFileUtils.getExpressionExperimentMetadataDirname;

/**
 * High-level service for writing {@link ubic.gemma.model.expression.experiment.ExpressionExperiment} reports.
 * <p>
 * This is analogous to {@link ubic.gemma.core.analysis.service.ExpressionDataFileService} and care should be taken to
 * provide consistent APIs.
 */
@Service
@Transactional(propagation = Propagation.NEVER)
@CommonsLog
public class ExpressionExperimentReportFileService {

    @Autowired
    private ExpressionExperimentReportService expressionExperimentReportService;

    @Autowired
    private VelocityEngine velocityEngine;

    @Value("${gemma.appdata.home}/metadata")
    private Path metadataDir;

    public Path writeOrLocateDataProcessingReport( ExpressionExperiment ee ) throws IOException {
        Path reportFile = metadataDir.resolve( getExpressionExperimentMetadataDirname( ee ) ).resolve( "report.txt" );
        // TODO: check if the report generation can be skipped
        // TODO: add support for locking the report file
        try ( Writer writer = Files.newBufferedWriter( reportFile ) ) {
            writeDataProcessingReport( ee, writer );
        }
        return reportFile;
    }

    public void writeDataProcessingReport( ExpressionExperiment ee, Writer writer ) throws IOException {
        ExpressionExperimentDataProcessingReport report = expressionExperimentReportService.generateDataProcessingReport( ee );
        renderReportTemplate( "dataProcessingReport.md", ee, report, writer );
    }

    private void renderReportTemplate( String templateName, ExpressionExperiment ee, Object report, Writer writer ) throws IOException {
        Map<String, Object> contextMap = new HashMap<>();
        contextMap.put( "dataset", ee );
        contextMap.put( "report", report );
        try {
            velocityEngine.mergeTemplate( "ubic/gemma/core/analysis/report/" + templateName + ".vm",
                    RuntimeConstants.ENCODING_DEFAULT,
                    new VelocityContext( contextMap ),
                    writer );
        } catch ( ParseErrorException e ) {
            URL templateResource = getClass().getResource( "/" + e.getTemplateName() );
            throw new RuntimeException( String.format( "Invalid syntax encountered in Velocity template at %s:%d:%d:\n\n\t%s",
                    templateResource != null ? ResourceUtils.getSourceCodeLocation( templateResource ) : e.getTemplateName(),
                    e.getLineNumber(), e.getColumnNumber(), e.getInvalidSyntax() ), e );
        }
        writer.flush();
    }
}
