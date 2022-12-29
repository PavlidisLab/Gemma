package ubic.gemma.persistence.service.expression.arrayDesign;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.persistence.util.Filter;

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
    public void testGetFilter() {
        assertThat( arrayDesignService.getFilter( "id", Filter.Operator.eq, "1" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "ad" )
                .hasFieldOrPropertyWithValue( "propertyName", "id" )
                .hasFieldOrPropertyWithValue( "requiredValue", 1L );
    }

    @Test
    public void testGetFilterWhenPropertyDoesNotExist() {
        assertThatThrownBy( () -> arrayDesignService.getFilter( "foo.bar", Filter.Operator.eq, "joe" ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContainingAll( "taxon", "taxon.ncbiId", "alternativeTo" );
    }

    @Test
    public void testGetFilterWhenPropertyExceedsMaxDepth() {
        assertThatThrownBy( () -> arrayDesignService.getFilter( "primaryTaxon.externalDatabase.databaseSupplier.name", Filter.Operator.eq, "joe" ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "At most 3 levels can be used for filtering." )
                .hasNoCause();
    }

    @Test
    public void testGetFilterTechnologyType() {
        assertThat( arrayDesignService.getFilterablePropertyDescription( "technologyType" ) )
                .contains( TechnologyType.SEQUENCING.name(), TechnologyType.ONECOLOR.name() );
    }

    @Test
    public void testTaxonPropertyResolutionInGetFilter() {
        assertThat( arrayDesignService.getFilter( "taxon", Filter.Operator.eq, "1" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "t" )
                .hasFieldOrPropertyWithValue( "propertyName", "id" )
                .hasFieldOrPropertyWithValue( "requiredValue", 1L );
        assertThat( arrayDesignService.getFilter( "taxon.ncbiId", Filter.Operator.eq, "9606" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "t" )
                .hasFieldOrPropertyWithValue( "propertyName", "ncbiId" )
                .hasFieldOrPropertyWithValue( "requiredValue", 9606 );
        assertThat( arrayDesignService.getFilter( "taxon.commonName", Filter.Operator.eq, "human" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "t" )
                .hasFieldOrPropertyWithValue( "propertyName", "commonName" )
                .hasFieldOrPropertyWithValue( "requiredValue", "human" );
        assertThat( arrayDesignService.getFilter( "needsAttention", Filter.Operator.eq, "true" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "s" )
                .hasFieldOrPropertyWithValue( "propertyName", "needsAttention" )
                .hasFieldOrPropertyWithValue( "requiredValue", true );
        assertThat( arrayDesignService.getFilter( "technologyType", Filter.Operator.eq, "ONECOLOR" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "ad" )
                .hasFieldOrPropertyWithValue( "propertyName", "technologyType" )
                .hasFieldOrPropertyWithValue( "requiredValue", "ONECOLOR" );
    }
}