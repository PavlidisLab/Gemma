package ubic.gemma.persistence.service.expression.biomaterial;

import org.springframework.util.Assert;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
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
}
