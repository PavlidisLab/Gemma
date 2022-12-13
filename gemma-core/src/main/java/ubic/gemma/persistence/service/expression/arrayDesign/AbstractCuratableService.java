package ubic.gemma.persistence.service.expression.arrayDesign;

import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledService;
import ubic.gemma.persistence.service.FilteringVoEnabledDao;

import java.util.Date;

/**
 * Base implementation for services dealing with {@link ubic.gemma.model.common.auditAndSecurity.curation.Curatable}
 * entities.
 * @param <O>
 * @param <VO>
 * @author poirigui
 * @see ubic.gemma.model.common.auditAndSecurity.curation.Curatable
 */
public abstract class AbstractCuratableService<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends AbstractFilteringVoEnabledService<O, VO> {

    protected AbstractCuratableService( FilteringVoEnabledDao<O, VO> voDao ) {
        super( voDao );
    }

    /**
     * {@inheritDoc}
     * <p>
     * Include filtering by {@code lastUpdated}, {@code troubled} and {@code needsAttention} from the associated
     * curation details.
     */
    @Override
    protected ObjectFilterPropertyMeta getObjectFilterPropertyMeta( String propertyName ) throws IllegalArgumentException {
        if ( propertyName.equals( "lastUpdated" ) ) {
            return new ObjectFilterPropertyMeta( "s", "lastUpdated", Date.class );
        }

        if ( propertyName.equals( "troubled" ) ) {
            return new ObjectFilterPropertyMeta( "s", "troubled", Boolean.class );
        }

        if ( propertyName.equals( "needsAttention" ) ) {
            return new ObjectFilterPropertyMeta( "s", "needsAttention", Boolean.class );
        }

        return super.getObjectFilterPropertyMeta( propertyName );
    }
}
