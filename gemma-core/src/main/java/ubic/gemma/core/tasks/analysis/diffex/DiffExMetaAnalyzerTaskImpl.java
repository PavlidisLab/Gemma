package ubic.gemma.core.tasks.analysis.diffex;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.core.analysis.expression.diff.DiffExMetaAnalyzerService;
import ubic.gemma.core.analysis.expression.diff.GeneDiffExMetaAnalysisHelperService;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.job.AbstractTask;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysis;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisDetailValueObject;

/**
 * A differential expression meta-analysis space task
 * 
 * @author frances
 *
 */
@Component
@Scope("prototype")
public class DiffExMetaAnalyzerTaskImpl extends AbstractTask<DiffExMetaAnalyzerTaskCommand> implements
        DiffExMetaAnalyzerTask {

    @Autowired
    private DiffExMetaAnalyzerService diffExMetaAnalyzerService;

    @Autowired
    private GeneDiffExMetaAnalysisHelperService geneDiffExMetaAnalysisHelperService;

    @Override
    public TaskResult call() {
        GeneDifferentialExpressionMetaAnalysis metaAnalysis = this.diffExMetaAnalyzerService.analyze( taskCommand
                .getAnalysisResultSetIds() );

        if ( metaAnalysis != null ) {
            metaAnalysis.setName( taskCommand.getName() );
            metaAnalysis.setDescription( taskCommand.getDescription() );

            if ( taskCommand.isPersist() ) {
                metaAnalysis = this.diffExMetaAnalyzerService.persist( metaAnalysis );
            }
        }

        GeneDifferentialExpressionMetaAnalysisDetailValueObject metaAnalysisVO = ( metaAnalysis == null ? null
                : this.geneDiffExMetaAnalysisHelperService.convertToValueObject( metaAnalysis ) );

        return new TaskResult( taskCommand, metaAnalysisVO );
    }
}
