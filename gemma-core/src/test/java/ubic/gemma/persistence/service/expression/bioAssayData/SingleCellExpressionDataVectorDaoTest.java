package ubic.gemma.persistence.service.expression.bioAssayData;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.persistence.util.TestComponent;

import java.util.Arrays;

@ContextConfiguration
public class SingleCellExpressionDataVectorDaoTest extends BaseDatabaseTest {

    private static final ByteArrayConverter byteArrayConverter = new ByteArrayConverter();

    @Configuration
    @TestComponent
    static class SingleCellDataVectorDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

    }

    @Autowired
    private SessionFactory sessionFactory;

    @Test
    public void test() {
        BioAssay b1 = new BioAssay();
        Characteristic t1, t2;
        t1 = new Characteristic();
        t2 = new Characteristic();

        SingleCellDimension dimension = new SingleCellDimension();
        dimension.setCellIds( Arrays.asList( "cell1", "cell2", "cell3" ) );
        dimension.setBioAssays( Arrays.asList( b1 ) );
        dimension.setBioAssaysOffset( new int[] { 0 } );
        dimension.setCellTypes( Arrays.asList( t1, t2 ) );
        dimension.setCellTypesOffset( new int[] { 0, 2 } );

        SingleCellExpressionDataVector vector = new SingleCellExpressionDataVector();
        vector.setSingleCellDimension( dimension );
        vector.setData( byteArrayConverter.doubleArrayToBytes( new double[] { 1.0f, 2.0f, 3.0f } ) );
        vector.setDataIndices( new int[] { 1, 5, 8 } );
        sessionFactory.getCurrentSession().persist( vector );
    }
}