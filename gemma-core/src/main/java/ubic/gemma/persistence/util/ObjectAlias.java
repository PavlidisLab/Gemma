package ubic.gemma.persistence.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ObjectAlias {
    private final String objectAlias;
    private final String sqlAlias;
    private final Class<?> cls;
}
