package ubic.gemma.persistence.service.expression.biomaterial;

import org.springframework.util.Assert;
import ubic.gemma.model.common.description.Category;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicUtils;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

import java.util.*;
import java.util.function.Consumer;

public class BioMaterialUtils {

    /**
     * Visit all the biomaterials in the hierarchy.
     * <p>
     * Circular biomaterial are not allowed, but if they happen we must detect them to prevent stack overflows.
     * @throws IllegalStateException if a circular biomaterial is detected
     */
    public static void visitBioMaterials( BioMaterial bioMaterial, Consumer<BioMaterial> visitor ) {
        Set<BioMaterial> visited = Collections.newSetFromMap( new IdentityHashMap<>() );
        for ( BioMaterial bm = bioMaterial; bm != null; bm = bm.getSourceBioMaterial() ) {
            Assert.state( visited.add( bm ), "Circular biomaterial detected" );
            visitor.accept( bm );
        }
    }

    /**
     * Create a mapping of characteristics
     */
    public static Map<Category, Map<BioMaterial, Collection<Characteristic>>> createCharacteristicMap( Collection<BioMaterial> samples ) {
        Map<Category, Map<BioMaterial, Collection<Characteristic>>> map = new HashMap<>();
        for ( BioMaterial sample : samples ) {
            for ( Characteristic characteristic : sample.getCharacteristics() ) {
                map.computeIfAbsent( CharacteristicUtils.getCategory( characteristic ), k -> new HashMap<>() )
                        .computeIfAbsent( sample, k -> new HashSet<>( samples.size() ) )
                        .add( characteristic );
            }
        }
        return map;
    }
}
