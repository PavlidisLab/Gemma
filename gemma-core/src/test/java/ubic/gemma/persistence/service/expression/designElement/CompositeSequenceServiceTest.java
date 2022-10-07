package ubic.gemma.persistence.service.expression.designElement;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDao;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.persistence.util.Slice;

import static org.assertj.core.api.Assertions.assertThat;

public class CompositeSequenceServiceTest extends BaseSpringContextTest {

    @Autowired
    private CompositeSequenceService compositeSequenceService;

    /* fixtures */
    private ArrayDesign arrayDesign;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        arrayDesign = getTestPersistentArrayDesign( 10, true );
    }

    @Test
    public void testLoadValueObjetsPreFilterByArrayDesign() {
        Filters filters = Filters.singleFilter( new ObjectFilter( ArrayDesignDao.OBJECT_ALIAS, "id", Long.class, ObjectFilter.Operator.eq, arrayDesign.getId() ) );
        Slice<CompositeSequenceValueObject> results = compositeSequenceService.loadValueObjectsPreFilter( filters, null, 0, 10 );
        assertThat( results ).hasSize( 10 )
                .extracting( "arrayDesign" )
                .extracting( "id" )
                .containsOnly( arrayDesign.getId() );
    }
}