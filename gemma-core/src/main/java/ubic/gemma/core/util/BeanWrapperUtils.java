package ubic.gemma.core.util;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyValue;

import javax.annotation.Nullable;
import java.util.Objects;

public class BeanWrapperUtils {

    /**
     * Set a property value on a {@link BeanWrapper} and return whether the value was changed.
     * @return if the value was changed as per {@link Objects#equals(Object, Object)}
     * @see BeanWrapper#setPropertyValue(PropertyValue)
     */
    public static <T> boolean setPropertyValue( BeanWrapper bw, String property, @Nullable T newVal ) {
        if ( !Objects.equals( bw.getPropertyValue( property ), newVal ) ) {
            bw.setPropertyValue( property, newVal );
            return true;
        }
        return false;
    }
}
