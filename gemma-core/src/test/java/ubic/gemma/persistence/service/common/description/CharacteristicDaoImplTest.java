package ubic.gemma.persistence.service.common.description;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.common.description.Characteristic;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CharacteristicDaoImplTest extends BaseSpringContextTest {

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
        Map<String, CharacteristicDao.CharacteristicByValueUriOrValueCount> results = characteristicDao.countCharacteristicValueLikeByNormalizedValue( "male%" );
        assertThat( results ).containsKeys( "http://test/T0001".toLowerCase(), "http://test/T0002".toLowerCase(), "male reproductive system (unknown term)" );
        assertThat( results.get( "http://test/t0001" ).getCount() ).isEqualTo( 2L );
        assertThat( results.get( "http://test/t0002" ).getCount() ).isEqualTo( 1L );
    }

    @Test
    public void testCountByValueUriIn() {
        Collection<String> uris = Arrays.asList( "http://test/T0006", "http://test/T0002" );
        Map<String, CharacteristicDao.CharacteristicByValueUriOrValueCount> results = characteristicDao.countCharacteristicValueUriInByNormalizedValue( uris );
        assertThat( results ).containsKeys( "http://test/T0006".toLowerCase(), "http://test/T0002".toLowerCase() );
    }

    private Characteristic createCharacteristic( String valueUri, String value ) {
        Characteristic c = new Characteristic();
        c.setValueUri( valueUri );
        c.setValue( value );
        return c;
    }

}