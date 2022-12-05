package ubic.gemma.persistence.service.common.quantitationtype;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.ObjectFilter;

@TestExecutionListeners(TransactionalTestExecutionListener.class)
public class QuantitationTypeDaoTest extends BaseSpringContextTest {

    @Autowired
    private QuantitationTypeDao quantitationTypeDao;

    @Test
    @Transactional
    public void testLoadValueObjectsPreFilter() {
        Filters filters = Filters.singleFilter( ObjectFilter.parseObjectFilter( null, "name", String.class, ObjectFilter.Operator.eq, "FPKM" ) );
        quantitationTypeDao.loadValueObjectsPreFilter( filters, null );
    }
}