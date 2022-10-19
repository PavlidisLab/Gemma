package ubic.gemma.persistence.service.common.quantitationtype;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.ObjectFilter;

public class QuantitationTypeDaoTest extends BaseSpringContextTest {

    @Autowired
    private QuantitationTypeDao quantitationTypeDao;

    @Test
    public void testLoadValueObjectsPreFilter() {
        Filters filters = Filters.singleFilter( ObjectFilter.parseObjectFilter( null, "name", String.class, ObjectFilter.Operator.eq, "FPKM" ) );
        quantitationTypeDao.loadValueObjectsPreFilter( filters, null );
    }
}