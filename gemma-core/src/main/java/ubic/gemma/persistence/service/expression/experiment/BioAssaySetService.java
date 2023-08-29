package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.persistence.service.BaseReadOnlyService;

import java.util.Collection;

/**
 * Generic service for dealing with all subclasses of {@link BioAssaySet}.
 * @author poirigui
 */
public interface BioAssaySetService extends BaseReadOnlyService<BioAssaySet> {

    void remove( Collection<? extends BioAssaySet> entities );

    void remove( BioAssaySet entity );
}
