package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

public class ValueObjectUtils {

    /**
     * Remap a given mapping where the keys are {@link Identifiable} to a mapping where the keys are {@link IdentifiableValueObject}.
     */
    public static <O extends Identifiable, VO extends IdentifiableValueObject<O>, V> Map<VO, V> remap( Map<O, V> map, Collection<VO> vos ) {
        Map<Long, V> byId = map.entrySet().stream()
                .collect( Collectors.toMap( e -> e.getKey().getId(), Map.Entry::getValue ) );
        return vos.stream()
                .filter( c -> byId.containsKey( c.getId() ) )
                .distinct() // otherwise keys could become duplicated
                .collect( Collectors.toMap( identity(), c -> byId.get( c.getId() ) ) );
    }
}
