package ubic.gemma.rest.util;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.internal.Iterables;
import org.springframework.validation.Errors;
import ubic.gemma.core.lang.Nullable;

public class ErrorsAssert extends AbstractAssert<ErrorsAssert, Errors> {

    private final Iterables iterables = Iterables.instance();

    public ErrorsAssert( Errors errors ) {
        super( errors, ErrorsAssert.class );
    }

    /**
     * Asserts that the errors object has a global error.
     */
    public ErrorsAssert hasGlobalError( @Nullable String objectName, String code ) {
        iterables.assertAnySatisfy( info, actual.getGlobalErrors(), fe -> {
            objects.assertEqual( info, fe.getObjectName(), objectName );
            objects.assertEqual( info, fe.getCode(), code );
        } );
        return myself;
    }

    /**
     * Asserts that the errors object has a field error.
     */
    public ErrorsAssert hasFieldError( @Nullable String objectName, String field, String code ) {
        iterables.assertAnySatisfy( info, actual.getFieldErrors(), fe -> {
            objects.assertEqual( info, fe.getObjectName(), objectName );
            objects.assertEqual( info, fe.getField(), field );
            objects.assertEqual( info, fe.getCode(), code );
        } );
        return myself;
    }

    /**
     * Asserts that the errors object has a field error with a given rejected value.
     */
    public ErrorsAssert hasFieldError( @Nullable String objectName, String field, String code, @Nullable Object rejectedValue ) {
        iterables.assertAnySatisfy( info, actual.getFieldErrors(), fe -> {
            objects.assertEqual( info, fe.getObjectName(), objectName );
            objects.assertEqual( info, fe.getField(), field );
            objects.assertEqual( info, fe.getCode(), code );
            objects.assertEqual( info, fe.getRejectedValue(), rejectedValue );
        } );
        return myself;
    }
}
