package ubic.gemma.web.compat;

import org.springframework.validation.BindException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Map;

/**
 * Compatibility shim for Spring 3.2's {@code org.springframework.web.servlet.mvc.SimpleFormController},
 * which was removed in Spring 4 in favour of annotation-driven {@code @Controller} +
 * {@code @RequestMapping} controllers.
 * <p>
 * This class exists only so the legacy {@code ArrayDesignFormController} keeps compiling on the
 * renovations branch during the Spring 3 → 4 → 5 climb. The form workflow itself is not implemented
 * here — the Spring lifecycle will not invoke {@link #onSubmit} / {@link #referenceData} etc. on
 * subclasses through this stub. Subclasses extending this should be migrated to annotation-driven
 * controllers as part of the DWR-to-REST conversion in Phase 1.
 *
 * @see <a href="https://docs.spring.io/spring-framework/docs/3.2.x/spring-framework-reference/html/mvc.html">Spring 3.2 MVC reference</a>
 */
public abstract class SimpleFormController extends AbstractController {

    private Class<?> commandClass;
    private String commandName = "command";
    private String formView;
    private String successView;

    public void setCommandClass( Class<?> commandClass ) {
        this.commandClass = commandClass;
    }

    public Class<?> getCommandClass() {
        return commandClass;
    }

    public void setCommandName( String commandName ) {
        this.commandName = commandName;
    }

    public String getCommandName() {
        return commandName;
    }

    public void setFormView( String formView ) {
        this.formView = formView;
    }

    public String getFormView() {
        return formView;
    }

    public void setSuccessView( String successView ) {
        this.successView = successView;
    }

    public String getSuccessView() {
        return successView;
    }

    @Override
    protected final ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response ) {
        throw new UnsupportedOperationException(
                "ubic.gemma.web.compat.SimpleFormController is a compile-time shim only. "
                        + "Subclass " + getClass().getName() + " must be migrated to annotation-driven "
                        + "@Controller / @RequestMapping before it can serve requests." );
    }

    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        if ( commandClass != null ) {
            return commandClass.getDeclaredConstructor().newInstance();
        }
        return null;
    }

    protected ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {
        return new ModelAndView( successView );
    }

    protected ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {
        return onSubmit( request, response, command, errors );
    }

    protected Map<String, ?> referenceData( HttpServletRequest request ) throws Exception {
        return Collections.emptyMap();
    }

    protected void initBinder( WebDataBinder binder ) {
    }
}
