package ubic.gemma.persistence.util;

import org.junit.Test;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import static org.junit.Assert.assertEquals;

public class EntityUrlTest {

    @Test
    public void test() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 12L );
        assertEquals( "/rest/v2/datasets/12", EntityUrl.of( "" ).entity( ee ).rest().toUriString() );
        ArrayDesign ad = new ArrayDesign();
        ad.setId( 3L );
        assertEquals( "/arrays/showArrayDesign.html?id=3", EntityUrl.of( "" ).entity( ad ).web().toUriString() );
        assertEquals( "/rest/v2/platforms/3", EntityUrl.of( "" ).entity( ad ).rest().toUriString() );
    }
}