package ubic.gemma.web.controller;

import org.junit.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import ubic.gemma.web.util.BaseSpringWebTest;
import ubic.gemma.web.util.EntityNotFoundException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ErrorPagesTest extends BaseSpringWebTest {

    @Controller
    static class ErrorController {

        @RequestMapping("/test/error/400")
        public String error400FromException() {
            throw new IllegalArgumentException();
        }

        @RequestMapping("/test/error/403/from_access_denied")
        public String error403FromException() {
            throw new AccessDeniedException( "" );
        }

        @RequestMapping("/test/error/404")
        public String error404FromException() {
            throw new EntityNotFoundException( "" );
        }

        @RequestMapping("/test/error/500")
        public String error500FromException() {
            throw new RuntimeException();
        }

        @RequestMapping("/test/error/500/from_data_access_failure")
        public String error500FromDataAccessFailure() {
            throw new CannotAcquireLockException( "" );
        }
    }

    @Test
    public void test() throws Exception {
        mvc.perform( get( "/test/error/400" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "error/400" ) )
                .andExpect( model().attribute( "exception", instanceOf( IllegalArgumentException.class ) ) );
        mvc.perform( get( "/test/error/403/from_access_denied" ) )
                .andExpect( status().isForbidden() )
                .andExpect( view().name( "accessDeniedFailure" ) )
                .andExpect( model().attribute( "exception", instanceOf( AccessDeniedException.class ) ) );
        mvc.perform( get( "/test/error/404" ) )
                .andExpect( status().isNotFound() )
                .andExpect( view().name( "error/404" ) )
                .andExpect( model().attribute( "exception", instanceOf( EntityNotFoundException.class ) ) );
        mvc.perform( get( "/test/error/500" ) )
                .andExpect( status().isInternalServerError() )
                .andExpect( view().name( "error/500" ) )
                .andExpect( model().attribute( "exception", instanceOf( RuntimeException.class ) ) );
        mvc.perform( get( "/test/error/500/from_data_access_failure" ) )
                .andExpect( status().isInternalServerError() )
                .andExpect( view().name( "dataAccessFailure" ) )
                .andExpect( model().attribute( "exception", instanceOf( DataAccessException.class ) ) );
    }
}
