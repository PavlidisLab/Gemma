package ubic.gemma.persistence.service;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

public abstract class AbstractFilteringService<O extends Identifiable> extends AbstractService<O> implements FilteringService<O> {

    private final FilteringDao<O> mainDao;

    protected AbstractFilteringService( FilteringDao<O> mainDao ) {
        super( mainDao );
        this.mainDao = mainDao;
    }

    @Override
    public Set<String> getFilterableProperties() {
        return mainDao.getFilterableProperties();
    }

    @Override
    public Class<?> getFilterablePropertyType( String property ) {
        return mainDao.getFilterablePropertyType( property );
    }

    @Nullable
    @Override
    public String getFilterablePropertyDescription( String property ) {
        return mainDao.getFilterablePropertyDescription( property );
    }

    @Override
    public Filter getFilter( String property, Filter.Operator operator, String value ) throws IllegalArgumentException {
        return mainDao.getFilter( property, operator, value );
    }

    @Override
    public Filter getFilter( String property, Filter.Operator operator, Collection<String> values ) throws IllegalArgumentException {
        return mainDao.getFilter( property, operator, values );
    }

    @Override
    public Sort getSort( String property, @Nullable Sort.Direction direction ) throws IllegalArgumentException {
        return mainDao.getSort( property, direction );
    }
}
