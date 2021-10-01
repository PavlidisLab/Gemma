package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import ubic.gemma.model.common.auditAndSecurity.curation.AbstractCuratableValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.persistence.service.BaseVoEnabledDao;
import ubic.gemma.persistence.service.FilteringVoEnabledDao;

import java.util.Collection;

/**
 * Created by tesarst on 13/03/17.
 * DAO wrapper for all curatable DAOs.
 */
public interface CuratableDao<C extends Curatable, VO extends AbstractCuratableValueObject<C>>
        extends FilteringVoEnabledDao<C, VO> {

    Collection<C> findByName( String name );

    C findByShortName( String name );
}
