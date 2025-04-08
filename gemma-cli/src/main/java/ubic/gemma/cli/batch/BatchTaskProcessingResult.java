package ubic.gemma.cli.batch;

import lombok.Value;

import javax.annotation.Nullable;

/**
 * Represents an individual result in a batch processing.
 */
@Value
public class BatchTaskProcessingResult {

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

    enum ResultType {
        SUCCESS,
        WARNING,
        ERROR
    }
}
