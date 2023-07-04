package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

/**
 * Service that simplifies operation with curatable entities of unknown types.
 * <p>
 * The main motivation of having this helper is to avoid having to check the type of the curatable entity before calling
 * a specific service, generally either {@link ArrayDesignService} or {@link ExpressionExperimentService}.
 * @author poirigui
 */
public interface GenericCuratableDao extends CuratableDao<Curatable> {
}
