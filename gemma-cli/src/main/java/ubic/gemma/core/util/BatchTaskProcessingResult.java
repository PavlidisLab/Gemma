package ubic.gemma.core.util;

import lombok.Value;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nullable;

/**
 * Represents an individual result in a batch processing.
 */
@Value
class BatchTaskProcessingResult {

    ResultType resultType;
    @Nullable
    Object source;
    @Nullable
    String message;
    @Nullable
    Throwable throwable;

    BatchTaskProcessingResult( ResultType resultType, @Nullable Object source, @Nullable String message, @Nullable Throwable throwable ) {
        this.resultType = resultType;
        this.source = source;
        this.message = message;
        this.throwable = throwable;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append( source != null ? source : "Unknown object" );
        if ( message != null ) {
            buf.append( "\t" )
                    .append( message.replace( "\n", "\n\t" ) ); // FIXME We don't want newlines here at all, but I'm not sure what condition this is meant to fix exactly.
        }
        if ( throwable != null ) {
            buf.append( "\t" )
                    .append( "Reason: " )
                    .append( ExceptionUtils.getRootCauseMessage( throwable ) );
        }
        return buf.toString();
    }

    enum ResultType {
        SUCCESS,
        WARNING,
        ERROR
    }
}
