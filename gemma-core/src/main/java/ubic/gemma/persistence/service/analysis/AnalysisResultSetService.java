package ubic.gemma.persistence.service.analysis;

import ubic.gemma.model.analysis.AnalysisResult;
import ubic.gemma.model.analysis.AnalysisResultSet;
import ubic.gemma.model.analysis.AnalysisResultSetValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledService;

/**
 * Interface for services providing {@link AnalysisResultSet}.
 *
 * @param <O> the type of result set
 * @param <V> a value object type to expose result sets
 */
public interface AnalysisResultSetService<K extends AnalysisResult, O extends AnalysisResultSet<K>, V extends AnalysisResultSetValueObject<K, O>> extends BaseVoEnabledService<O, V> {

}
