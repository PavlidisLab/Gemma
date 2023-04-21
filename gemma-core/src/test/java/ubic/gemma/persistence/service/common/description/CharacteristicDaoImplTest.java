package ubic.gemma.persistence.service.common.description;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.persistence.util.TestComponent;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration
public class CharacteristicDaoImplTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class CharacteristicDaoImplContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public CharacteristicDao characteristicDao( SessionFactory sessionFactory ) {
            return new CharacteristicDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private CharacteristicDao characteristicDao;

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

    private Characteristic createCharacteristic( @Nullable String valueUri, String value ) {
        Characteristic c = new Characteristic();
        c.setValueUri( valueUri );
        c.setValue( value );
        return c;
    }

}