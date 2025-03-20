package ubic.gemma.core.loader.util.mapper;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;

import java.util.*;

import static org.junit.Assert.*;

public class EnsemblIdDesignElementMapperTest {

    @Test
    public void test() {
        ArrayDesign platform = new ArrayDesign();
        CompositeSequence cs = new CompositeSequence();
        Gene g = new Gene();
        g.setEnsemblId( "ENSG00000139618" );
        Map<CompositeSequence, Set<Gene>> cs2g = new HashMap<>();
        platform.getCompositeSequences().add( cs );
        cs2g.put( cs, Collections.singleton( g ) );
        EntityMapper.StatefulEntityMapper<CompositeSequence> mapper = new EnsemblIdDesignElementMapper( cs2g )
                .forCandidates( platform );
        assertTrue( mapper.contains( "ENSG00000139618.13" ) );
        assertTrue( mapper.contains( "ENSG00000139618" ) );
        assertFalse( mapper.contains( "ENSG00000139619" ) );
        assertFalse( mapper.contains( "ENSG00000139619.2" ) );
        Assertions.assertThat( mapper.matchOne( "ENSG00000139618" ) )
                .hasValue( cs );
        Assertions.assertThat( mapper.matchOne( Collections.singleton( "ENSG00000139618" ) ) )
                .containsEntry( "ENSG00000139618", cs );
        DesignElementMapper.MappingStatistics stats = mapper.getMappingStatistics( Arrays.asList( "ENSG00000139618.13", "ENSG00000139618", "ENSG00000139619" ) );
        assertEquals( 0.66, stats.getOverlap(), 0.01 );
        assertEquals( 1.0, stats.getCoverage(), 0 );
    }
}