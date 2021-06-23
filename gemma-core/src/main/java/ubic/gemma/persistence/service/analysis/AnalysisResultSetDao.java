package ubic.gemma.persistence.service.analysis;

import ubic.gemma.model.analysis.AnalysisResult;
import ubic.gemma.model.analysis.AnalysisResultSet;
import ubic.gemma.model.analysis.AnalysisResultSetValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledDao;

/**
 * Generic DAO for manipulating {@link AnalysisResultSet}.
 *
 * @param <O>
 * @param <VO>
 */
public interface AnalysisResultSetDao<K extends AnalysisResult, O extends AnalysisResultSet<K>, VO extends AnalysisResultSetValueObject<K, O>> extends BaseVoEnabledDao<O, VO> {
}
