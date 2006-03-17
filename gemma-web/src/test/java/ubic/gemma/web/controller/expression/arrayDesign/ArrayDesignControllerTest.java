package ubic.gemma.web.controller.expression.arrayDesign;

import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;

/**
 * @author keshav
 * @version $Id$
 */
public class ArrayDesignControllerTest extends BaseTransactionalSpringContextTest {

    // private MockServletContext mockCtx;
    private MockHttpServletRequest request;
    // private MockHttpServletResponse response;

    ArrayDesignController arrayDesignController;

    ArrayDesign testArrayDesign;

    ArrayDesignService arrayDesignService;

    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        // mockCtx = new MockServletContext();

        request = new MockHttpServletRequest();
        // response = new MockHttpServletResponse();

        // arrayDesignController = ( ArrayDesignController ) ctx.getBean( "arrayDesignController" );

        testArrayDesign = ArrayDesign.Factory.newInstance();

        // arrayDesignService = ( ArrayDesignService ) ctx.getBean( "arrayDesignService" );

    }

    /**
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testShowAllArrayDesigns() throws Exception {
        ArrayDesignController a = ( ArrayDesignController ) getBean( "arrayDesignController" );
        request.setRequestURI( "Gemma/arrayDesign/showAllArrayDesigns.html" );
        ModelAndView mav = a.showAll( request, ( HttpServletResponse ) null );
        Collection<ArrayDesign> c = ( mav.getModel() ).values();
        assertNotNull( c );
        assertEquals( mav.getViewName(), "arrayDesigns" );
    }
}
