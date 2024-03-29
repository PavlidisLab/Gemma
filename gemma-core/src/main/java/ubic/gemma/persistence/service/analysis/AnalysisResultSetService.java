package ubic.gemma.persistence.service.analysis;

import ubic.gemma.model.analysis.AnalysisResult;
import ubic.gemma.model.analysis.AnalysisResultSet;
import ubic.gemma.persistence.service.BaseImmutableService;
import ubic.gemma.persistence.service.BaseReadOnlyService;
import ubic.gemma.persistence.service.BaseService;

/**
 * Interface for services providing {@link AnalysisResultSet}.
 *
 * @param <K> the type of analysis result
 * @param <O> the type of result set
 */
public interface AnalysisResultSetService<K extends AnalysisResult, O extends AnalysisResultSet<K>> extends BaseReadOnlyService<O> {

}
