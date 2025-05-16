package ubic.gemma.core.loader.util.mapper;

import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A simple design element mapper that simply uses the name as identifier.
 * @author poirigui
 */
public class SimpleDesignElementMapper extends MapBasedDesignElementMapper {

    public SimpleDesignElementMapper( Collection<CompositeSequence> de ) {
        super( "simple", de.stream().collect( Collectors.toMap( CompositeSequence::getName, Function.identity() ) ) );
    }
}
