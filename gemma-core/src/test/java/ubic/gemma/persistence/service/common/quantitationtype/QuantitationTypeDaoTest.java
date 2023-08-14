package ubic.gemma.persistence.service.common.quantitationtype;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Filter;

@TestExecutionListeners(TransactionalTestExecutionListener.class)
public class QuantitationTypeDaoTest extends BaseSpringContextTest {

    @Autowired
    private QuantitationTypeDao quantitationTypeDao;

    @Test
    @Transactional
    public void testLoadValueObjects() {
        Filters filters = Filters.by( Filter.parse( null, "name", String.class, Filter.Operator.eq, "FPKM", null ) );
        quantitationTypeDao.loadValueObjects( filters, null );
    }
}