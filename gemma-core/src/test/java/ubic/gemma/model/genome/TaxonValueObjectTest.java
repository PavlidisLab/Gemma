package ubic.gemma.model.genome;

import org.junit.Test;

import static org.junit.Assert.*;

public class TaxonValueObjectTest {

    @Test
    public void testDefaultNoArgConstructor() throws InstantiationException, IllegalAccessException {
        TaxonValueObject tvo = TaxonValueObject.class.newInstance();
    }
}