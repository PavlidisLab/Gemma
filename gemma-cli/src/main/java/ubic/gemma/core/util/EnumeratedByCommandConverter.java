package ubic.gemma.core.util;

import org.apache.commons.cli.Converter;

/**
 * This interface is implemented by {@link Converter} that can use an external command to get possible values.
 * @author poirigui
 */
public interface EnumeratedByCommandConverter<T, E extends Throwable> extends Converter<T, E> {

    /**
     * Command that can be used to get possible values.
     */
    String[] getPossibleValuesCommand();
}
