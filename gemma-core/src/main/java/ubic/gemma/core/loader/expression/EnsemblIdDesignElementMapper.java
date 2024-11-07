package ubic.gemma.core.loader.expression;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EnsemblIdDesignElementMapper implements DesignElementMapper {

    private final Map<String, CompositeSequence> elementsMapping;

    public EnsemblIdDesignElementMapper( ArrayDesign platform, Map<CompositeSequence, Set<Gene>> cs2g ) {
        Map<String, CompositeSequence> elementsMapping = new HashMap<>();
        for ( CompositeSequence cs : platform.getCompositeSequences() ) {
            if ( !cs2g.containsKey( cs ) ) {
                continue;
            }
            for ( Gene g : cs2g.get( cs ) ) {
                if ( g.getEnsemblId() != null ) {
                    elementsMapping.putIfAbsent( stripEnsemblIdVersion( g.getEnsemblId() ), cs );
                }
            }
        }
        this.elementsMapping = elementsMapping;
    }

    @Override
    public String getName() {
        return "Ensembl ID";
    }

    @Override
    public boolean contains( String geneIdentifier ) {
        return elementsMapping.containsKey( stripEnsemblIdVersion( geneIdentifier ) );
    }

    @Override
    public boolean containsAny( Collection<String> geneIdentifiers ) {
        return geneIdentifiers.stream().anyMatch( this::contains );
    }

    @Nullable
    @Override
    public CompositeSequence get( String geneIdentifier ) {
        return elementsMapping.get( stripEnsemblIdVersion( geneIdentifier ) );
    }

    @Override
    public MappingStatistics getMappingStatistics( Collection<String> geneIdentifiers ) {
        long overlap = geneIdentifiers.stream()
                .map( this::stripEnsemblIdVersion )
                .filter( elementsMapping::containsKey )
                .count();
        Set<String> strippedGeneIdentifiers = geneIdentifiers.stream()
                .map( this::stripEnsemblIdVersion )
                .collect( Collectors.toSet() );
        long coverage = elementsMapping.keySet().stream()
                .filter( strippedGeneIdentifiers::contains )
                .count();
        return new MappingStatistics( ( double ) overlap / geneIdentifiers.size(), ( double ) coverage / elementsMapping.size() );
    }

    private String stripEnsemblIdVersion( String ensemblId ) {
        int i;
        if ( ( i = ensemblId.lastIndexOf( '.' ) ) != -1 ) {
            return ensemblId.substring( 0, i );
        } else {
            return ensemblId;
        }
    }
}
