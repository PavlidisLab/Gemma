package ubic.gemma.core.loader.expression.singleCell;

import org.junit.Test;
import ubic.gemma.core.loader.expression.DesignElementMapper;
import ubic.gemma.core.loader.expression.EnsemblIdDesignElementMapper;
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
        EnsemblIdDesignElementMapper mapper = new EnsemblIdDesignElementMapper( platform, cs2g );
        assertTrue( mapper.contains( "ENSG00000139618.13" ) );
        assertTrue( mapper.contains( "ENSG00000139618" ) );
        assertFalse( mapper.contains( "ENSG00000139619" ) );
        assertFalse( mapper.contains( "ENSG00000139619.2" ) );
        DesignElementMapper.MappingStatistics stats = mapper.getMappingStatistics( Arrays.asList( "ENSG00000139618.13", "ENSG00000139618", "ENSG00000139619" ) );
        assertEquals( 0.66, stats.getOverlap(), 0.01 );
        assertEquals( 1.0, stats.getCoverage(), 0 );
    }
}