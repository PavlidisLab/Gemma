package ubic.gemma.core.loader.util.mapper;

import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.Map;

/**
 * @author poirigui
 */
public class MapBasedDesignElementMapper extends MapBasedEntityMapper<CompositeSequence> implements DesignElementMapper {

    public MapBasedDesignElementMapper( String name, Map<String, CompositeSequence> elementsMapping ) {
        super( name, elementsMapping );
    }
}
