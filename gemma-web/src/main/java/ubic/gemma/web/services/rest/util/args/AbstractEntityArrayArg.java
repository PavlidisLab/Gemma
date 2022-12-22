package ubic.gemma.web.services.rest.util.args;

import com.google.common.base.Strings;
import org.apache.commons.lang3.NotImplementedException;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.FilteringVoEnabledService;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.web.services.rest.util.MalformedArgException;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AbstractEntityArrayArg<A, O extends Identifiable, S extends FilteringVoEnabledService<O, ?>> extends AbstractArrayArg<A> implements EntityArrayArg<A, O, S> {

    private final Class<? extends AbstractEntityArg> entityArgClass;
    private String argValueName = null;

    protected AbstractEntityArrayArg( Class<? extends AbstractEntityArg> entityArgClass, List<A> values ) {
        super( values );
        this.entityArgClass = entityArgClass;
    }

    @Override
    public Filters getFilters( S service ) throws BadRequestException {
        try {
            return Filters.by( service.getFilter( this.getPropertyName( service ), Filter.Operator.in, getFilterRequiredValues() ) );
        } catch ( IllegalArgumentException e ) {
            throw new MalformedArgException( e );
        }
    }

    @Override
    public List<O> getEntities( S service ) throws NotFoundException, BadRequestException {
        List<O> objects = new ArrayList<>( this.getValue().size() );
        for ( A s : this.getValue() ) {
            AbstractEntityArg<?, O, S> arg;
            if ( s instanceof String ) {
                arg = this.entityArgValueOf( ( String ) s );
            } else {
                throw new NotImplementedException( "Obtaining entities is only supported for string values." );
            }
            objects.add( arg.checkEntity( service, arg.getEntity( service ) ) );
        }
        return objects;
    }

    /**
     * Convert a given value to string so that it can be passed to {@link FilteringVoEnabledService#getFilter(String, Filter.Operator, String)}
     */
    protected List<String> getFilterRequiredValues() {
        return this.getValue().stream().map( String::valueOf ).collect( Collectors.toList() );
    }

    /**
     * Reads the given MutableArgs property name and checks whether it is null or empty.
     *
     * @param arg     the MutableArg to retrieve the property name from.
     * @param value   one of the values of the property that has been passed into this array arg.
     * @param service service that may be used to retrieve the property from the MutableArg.
     * @param <T>     type of the given MutableArg.
     * @return the name of the property that the values in this arrayArg refer to.
     */
    private <T extends AbstractEntityArg<?, O, S>> String checkPropertyNameString( T arg, A value, S service ) throws BadRequestException {
        String identifier = arg.getPropertyName( service );
        if ( Strings.isNullOrEmpty( identifier ) ) {
            throw new BadRequestException( "Identifier " + value + " not recognized." );
        }
        return identifier;
    }

    /**
     * Guess the property name for this array of entities by testing the valueOf of the first entity.
     *
     * If no entity is specified, defaults on 'id'.
     *
     * This routine only works if the type of this array is {@link String}.
     *
     * @param service the service used to guess the type and name of the property that this arrayEntityArg represents.
     * @return the name of the property that the values in this array represent.
     */
    protected String getPropertyName( S service ) throws BadRequestException {
        if ( this.argValueName == null ) {
            Optional<A> value = this.getValue().stream().findFirst();
            if ( value.isPresent() ) {
                AbstractEntityArg<?, O, S> arg;
                if ( value.get() instanceof String ) {
                    arg = this.entityArgValueOf( ( String ) value.get() );
                } else {
                    throw new NotImplementedException( "Inferring the property name is only supported for string values." );
                }
                this.argValueName = this.checkPropertyNameString( arg, value.get(), service );
            } else {
                /* assumed since {@link O} is identifiable */
                this.argValueName = service.getIdentifierPropertyName();
            }
        }
        return this.argValueName;
    }

    /**
     * Call the valueOf method of the entity arg that consititute the elements of this array.
     */
    private AbstractEntityArg<?, O, S> entityArgValueOf( String s ) throws NotFoundException, BadRequestException {
        try {
            // noinspection unchecked // Could not avoid using reflection, because java does not allow abstract static methods.
            return ( AbstractEntityArg<?, O, S> ) entityArgClass.getMethod( "valueOf", String.class ).invoke( null, s );
        } catch ( IllegalAccessException | InvocationTargetException | NoSuchMethodException e ) {
            throw new NotImplementedException( "Could not call 'valueOf' for " + entityArgClass.getName(), e );
        }
    }

}
