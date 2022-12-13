package ubic.gemma.persistence.service.expression.arrayDesign;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.persistence.util.ObjectFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ArrayDesignServiceTest extends BaseSpringContextTest {

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Test
    public void testGetFilterableProperties() {
        assertThat( arrayDesignService.getFilterableProperties() )
                .contains( "taxon", "taxon.ncbiId" );
    }

    @Test
    public void testGetObjectFilter() {
        assertThat( arrayDesignService.getObjectFilter( "id", ObjectFilter.Operator.eq, "1" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "ad" )
                .hasFieldOrPropertyWithValue( "propertyName", "id" )
                .hasFieldOrPropertyWithValue( "requiredValue", 1L );
    }

    @Test
    public void testGetObjectFilterWhenPropertyDoesNotExist() {
        assertThatThrownBy( () -> arrayDesignService.getObjectFilter( "foo.bar", ObjectFilter.Operator.eq, "joe" ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContainingAll( "taxon", "taxon.ncbiId", "alternativeTo" );
    }

    @Test
    public void testGetObjectFilterWhenPropertyExceedsMaxDepth() {
        assertThatThrownBy( () -> arrayDesignService.getObjectFilter( "primaryTaxon.externalDatabase.contact.name", ObjectFilter.Operator.eq, "joe" ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "At most 3 levels can be used for filtering." )
                .hasNoCause();
    }

    @Test
    public void testTaxonPropertyResolutionInGetObjectFilter() {
        assertThat( arrayDesignService.getObjectFilter( "taxon", ObjectFilter.Operator.eq, "1" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "t" )
                .hasFieldOrPropertyWithValue( "propertyName", "id" )
                .hasFieldOrPropertyWithValue( "requiredValue", 1L );
        assertThat( arrayDesignService.getObjectFilter( "taxon.ncbiId", ObjectFilter.Operator.eq, "9606" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "t" )
                .hasFieldOrPropertyWithValue( "propertyName", "ncbiId" )
                .hasFieldOrPropertyWithValue( "requiredValue", 9606 );
        assertThat( arrayDesignService.getObjectFilter( "taxon.commonName", ObjectFilter.Operator.eq, "human" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "t" )
                .hasFieldOrPropertyWithValue( "propertyName", "commonName" )
                .hasFieldOrPropertyWithValue( "requiredValue", "human" );
        assertThat( arrayDesignService.getObjectFilter( "needsAttention", ObjectFilter.Operator.eq, "true" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "s" )
                .hasFieldOrPropertyWithValue( "propertyName", "needsAttention" )
                .hasFieldOrPropertyWithValue( "requiredValue", true );
    }

}