package ubic.gemma.persistence.service.expression.arrayDesign;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventDao;
import ubic.gemma.persistence.service.expression.experiment.BlacklistedEntityService;
import ubic.gemma.persistence.util.ObjectFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.array;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ArrayDesignServiceTest {

    @Configuration
    static class ArrayDesignServiceTestContextConfiguration {

        @Bean
        public ArrayDesignDao arrayDesignDao() {
            ArrayDesignDao arrayDesignDao = mock( ArrayDesignDao.class );
            when( arrayDesignDao.getElementClass() ).thenAnswer( a -> ArrayDesign.class );
            when( arrayDesignDao.getObjectAlias() ).thenReturn( "ad" );
            return arrayDesignDao;
        }

        @Bean
        public BlacklistedEntityService blacklistedEntityService() {
            return mock( BlacklistedEntityService.class );
        }

        @Bean
        public ArrayDesignService arrayDesignService( ArrayDesignDao arrayDesignDao ) {
            return new ArrayDesignServiceImpl( arrayDesignDao, mock( AuditEventDao.class ) );
        }
    }

    @Autowired
    private ArrayDesignDao arrayDesignDao;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Test
    public void testGetObjectFilter() {
        assertThat( arrayDesignService.getObjectFilter( "id", ObjectFilter.Operator.eq, "1" ) )
                .hasFieldOrPropertyWithValue( "objectAlias", "ad" )
                .hasFieldOrPropertyWithValue( "propertyName", "id" )
                .hasFieldOrPropertyWithValue( "requiredValue", 1L );
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