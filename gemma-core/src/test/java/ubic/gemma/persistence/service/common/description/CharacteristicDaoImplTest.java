package ubic.gemma.persistence.service.common.description;

import gemma.gsec.util.SecurityUtil;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.TableMaintenanceUtil;
import ubic.gemma.persistence.service.TableMaintenanceUtilImpl;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.util.MailEngine;
import ubic.gemma.persistence.util.TestComponent;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ContextConfiguration
public class CharacteristicDaoImplTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class CharacteristicDaoImplContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public CharacteristicDao characteristicDao( SessionFactory sessionFactory ) {
            return new CharacteristicDaoImpl( sessionFactory );
        }

        @Bean
        public TableMaintenanceUtil tableMaintenanceUtil() {
            return new TableMaintenanceUtilImpl();
        }

        @Bean
        public AuditEventService auditEventService() {
            return mock( AuditEventService.class );
        }

        @Bean
        public MailEngine mailEngine() {
            return mock( MailEngine.class );
        }

        @Bean
        public ExternalDatabaseService externalDatabaseService() {
            return mock( ExternalDatabaseService.class );
        }
    }

    @Autowired
    private CharacteristicDao characteristicDao;

    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    /* fixtures */
    private Collection<Characteristic> characteristics;

    @Before
    public void setUp() throws Exception {
        // TODO
        characteristics = Arrays.asList(
                createCharacteristic( "http://test/T0001", "male" ),
                createCharacteristic( "http://test/T0001", "male" ),
                createCharacteristic( "http://test/T0002", "male reproductive system" ),
                createCharacteristic( null, "male reproductive system (unknown term)" ),
                createCharacteristic( "http://test/T0003", "female" ),
                createCharacteristic( "http://test/T0004", "untreated" ),
                createCharacteristic( "http://test/T0005", "treated" ),
                createCharacteristic( "http://test/T0006", "treated with sodium chloride" ) );
        characteristics = characteristicDao.create( characteristics );
    }

    @After
    public void tearDown() {
        characteristicDao.remove( characteristics );
    }

    @Test
    public void testCountByValueLike() {
        Map<String, Characteristic> results = characteristicDao.findCharacteristicsByValueUriOrValueLikeGroupedByNormalizedValue( "male%" );
        assertThat( results ).containsKeys( "http://test/T0001".toLowerCase(), "http://test/T0002".toLowerCase(), "male reproductive system (unknown term)" );
    }

    @Test
    public void testCountByValueUriIn() {
        Collection<String> uris = Arrays.asList( "http://test/T0006", "http://test/T0002" );
        Map<String, Long> results = characteristicDao.countCharacteristicsByValueUriGroupedByNormalizedValue( uris );
        assertThat( results ).containsKeys( "http://test/T0006".toLowerCase(), "http://test/T0002".toLowerCase() );
    }

    @Test
    public void testNormalizeByValue() {
        assertThat( characteristicDao.normalizeByValue( createCharacteristic( null, "test" ) ) )
                .isEqualTo( "test" );
        assertThat( characteristicDao.normalizeByValue( createCharacteristic( "", "test" ) ) )
                .isEqualTo( "" );
        assertThat( characteristicDao.normalizeByValue( createCharacteristic( "https://EXAMPLE.COM", "test" ) ) )
                .isEqualTo( "https://example.com" );
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void testFindExperimentsByUris() {
        assertThat( SecurityUtil.isUserAdmin() ).isTrue();
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setAuditTrail( new AuditTrail() );
        Characteristic c = createCharacteristic( "http://example.com", "example" );
        ee.setTaxon( taxon );
        ee.getCharacteristics().add( c );
        sessionFactory.getCurrentSession().persist( ee );
        sessionFactory.getCurrentSession().flush();
        int updated = tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries();
        assertThat( updated ).isEqualTo( 1 );
        sessionFactory.getCurrentSession().flush();
        // ranking by level uses the order by field() which is not supported
        Map<Class<? extends Identifiable>, Map<String, Collection<ExpressionExperiment>>> results = characteristicDao.findExperimentsByUris( Collections.singleton( "http://example.com" ), taxon, 100, false );
        assertThat( results ).containsKey( ExpressionExperiment.class );
    }

    private Characteristic createCharacteristic( @Nullable String valueUri, String value ) {
        Characteristic c = new Characteristic();
        c.setValueUri( valueUri );
        c.setValue( value );
        return c;
    }

}