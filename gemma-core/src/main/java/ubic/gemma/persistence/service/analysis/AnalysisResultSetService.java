package ubic.gemma.persistence.service.analysis;

import ubic.gemma.model.analysis.AnalysisResult;
import ubic.gemma.model.analysis.AnalysisResultSet;
import ubic.gemma.persistence.service.BaseService;

/**
 * Interface for services providing {@link AnalysisResultSet}.
 *
 * @param <O> the type of result set
 * @param <V> a value object type to expose result sets
 */
public interface AnalysisResultSetService<K extends AnalysisResult, O extends AnalysisResultSet<K>> extends BaseService<O> {

}
