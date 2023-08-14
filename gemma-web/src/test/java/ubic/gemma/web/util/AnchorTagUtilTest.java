package ubic.gemma.web.util;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockServletContext;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.servlet.ServletContext;

import static org.junit.Assert.assertEquals;
import static ubic.gemma.web.util.AnchorTagUtil.getBioMaterialLink;
import static ubic.gemma.web.util.AnchorTagUtil.getExpressionExperimentLink;

public class AnchorTagUtilTest {

    private ServletContext servletContext;

    @Before
    public void setUpMocks() {
        servletContext = new MockServletContext();
    }

    @Test
    public void testGetExpressionExperimentLink() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        assertEquals( "<a href=\"/expressionExperiment/showExpressionExperiment.html?id=1\">Dataset #1</a>", getExpressionExperimentLink( ee, "", servletContext ) );
        assertEquals( "<a href=\"/expressionExperiment/showExpressionExperiment.html?id=1\">Foo</a>", getExpressionExperimentLink( ee, "Foo", servletContext ) );
        ee.setShortName( "GSE00001" );
        assertEquals( "<a href=\"/expressionExperiment/showExpressionExperiment.html?id=1\">GSE00001</a>", getExpressionExperimentLink( ee, "", servletContext ) );
    }


    @Test
    public void testGetBioMaterialLink() {
        BioMaterial bm = new BioMaterial();
        bm.setId( 1L );
        assertEquals( "<a href=\"/bioMaterial/showBioMaterial.html?id=1\">Sample #1</a>", getBioMaterialLink( bm, "", servletContext ) );
        assertEquals( "<a href=\"/bioMaterial/showBioMaterial.html?id=1\">Foo</a>", getBioMaterialLink( bm, "Foo", servletContext ) );
        bm.setName( "GSM000001" );
        assertEquals( "<a href=\"/bioMaterial/showBioMaterial.html?id=1\">GSM000001</a>", getBioMaterialLink( bm, "", servletContext ) );
    }
}