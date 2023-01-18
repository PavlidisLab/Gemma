package ubic.gemma.rest.util.args;

import java.util.List;

/**
 * Class representing an API argument that should be an array.
 * <p>
 * If you use this alongside a {@link javax.ws.rs.QueryParam}, make sure that you include a {@link io.swagger.v3.oas.annotations.Parameter}
 * with the 'explode' attribute set to {@link io.swagger.v3.oas.annotations.enums.Explode#FALSE}, otherwise the
 * serialization will not be correct.
 *
 * @author tesarst
 */
public abstract class AbstractArrayArg<T> extends AbstractArg<List<T>> {

    protected static final String ERROR_MSG = "Value '%s' can not converted to an array of ";

    protected AbstractArrayArg( List<T> values ) {
        super( values );
    }
}
