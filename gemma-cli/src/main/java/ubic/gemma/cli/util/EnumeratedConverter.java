package ubic.gemma.cli.util;

import org.apache.commons.cli.Converter;
import org.springframework.context.MessageSourceResolvable;

import java.util.Map;

/**
 * This interface is implemented by converters that can enumerate their possible values.
 * @author poirigui
 */
public interface EnumeratedConverter<T, E extends Throwable> extends Converter<T, E> {

    /**
     * Obtain a list of possible values that the converter can convert to {@link T}.
     * <p>
     * Implementation should use either a {@link java.util.SortedMap} or {@link java.util.LinkedHashMap} for
     * reproducibility.
     */
    Map<String, MessageSourceResolvable> getPossibleValues();
}
