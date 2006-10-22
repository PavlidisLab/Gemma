package ubic.gemma.web.controller.coexpressionSearch;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author joseph
 * @version $Id$
 */
public class CoexpressionSearchControllerTest extends BaseSpringContextTest {
    private static Log log = LogFactory.getLog( CoexpressionSearchController.class.getName() );

    /**
     * @throws Exception
     */
    @Override
    @SuppressWarnings("unchecked")
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

    }

    /**
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testOnSubmit() throws Exception {
        Gene g = this.getTestPeristentGene();

        CoexpressionSearchController searchController = ( CoexpressionSearchController ) this
                .getBean( "coexpressionSearchController" );

        HttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        CoexpressionSearchCommand command = new CoexpressionSearchCommand();
        command.setSearchCriteria( "gene symbol" );
        command.setSearchString( g.getOfficialName() );

        log.debug( "gene id id " + g.getOfficialName() );

        // setComplete();// leave data in database

        BindException errors = new BindException( command, "CoexpressionSearchCommand" );
        searchController.processFormSubmission( request, response, command, errors );
        ModelAndView mav = searchController.onSubmit( request, response, command, errors );
        assertEquals( "showCoexpressionSearchResults", mav.getViewName() );

    }
}
