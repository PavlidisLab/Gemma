package ubic.gemma.persistence.service.expression.designElement;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDao;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Slice;

import static org.assertj.core.api.Assertions.assertThat;

public class CompositeSequenceServiceTest extends BaseSpringContextTest {

    @Autowired
    private CompositeSequenceService compositeSequenceService;

    /* fixtures */
    private ArrayDesign arrayDesign;

    @Before
    public void setUp() throws Exception {
        arrayDesign = getTestPersistentArrayDesign( 10, true );
    }

    @Test
    public void testLoadValueObjetsByArrayDesign() {
        Filters filters = Filters.by( ArrayDesignDao.OBJECT_ALIAS, "id", Long.class, Filter.Operator.eq, arrayDesign.getId() );
        Slice<CompositeSequenceValueObject> results = compositeSequenceService.loadValueObjects( filters, null, 0, 10 );
        assertThat( results ).hasSize( 10 )
                .extracting( "arrayDesign" )
                .extracting( "id" )
                .containsOnly( arrayDesign.getId() );
    }
}