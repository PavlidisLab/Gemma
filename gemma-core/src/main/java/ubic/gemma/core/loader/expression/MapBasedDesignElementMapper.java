package ubic.gemma.core.loader.expression;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MapBasedDesignElementMapper implements DesignElementMapper {

    private final String name;
    private final Map<String, CompositeSequence> elementsMapping;

    public MapBasedDesignElementMapper( String name, Map<String, CompositeSequence> elementsMapping ) {
        this.name = name;
        this.elementsMapping = elementsMapping;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean contains( String geneIdentifier ) {
        return elementsMapping.containsKey( geneIdentifier );
    }

    @Override
    public boolean containsAny( Collection<String> genesSet ) {
        return CollectionUtils.containsAny( elementsMapping.keySet(), genesSet );
    }

    @Nullable
    @Override
    public CompositeSequence get( String geneIdentifier ) {
        return elementsMapping.get( geneIdentifier );
    }

    @Override
    public MappingStatistics getMappingStatistics( Collection<String> geneIdentifiers ) {
        int overlap;
        int coverage;
        if ( geneIdentifiers instanceof Set ) {
            int commonElements = SetUtils.intersection( elementsMapping.keySet(), ( Set<String> ) geneIdentifiers ).size();
            overlap = commonElements;
            coverage = commonElements;
        } else {
            // gene identifiers might contain duplicated elements
            overlap = ( int ) geneIdentifiers.stream().filter( elementsMapping::containsKey ).count();
            coverage = SetUtils.intersection( elementsMapping.keySet(), new HashSet<>( geneIdentifiers ) ).size();
        }
        return new MappingStatistics( ( double ) overlap / geneIdentifiers.size(), ( double ) coverage / elementsMapping.size() );
    }
}
