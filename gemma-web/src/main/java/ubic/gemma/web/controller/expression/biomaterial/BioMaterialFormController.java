package ubic.gemma.web.controller.expression.biomaterial;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.web.controller.BaseFormController;

/**
 * @author paul
 * @version $Id$
 * @spring.bean id="bioMaterialFormController"
 * @spring.property name = "commandName" value="bioMaterial"
 * @spring.property name = "formView" value="bioMaterial.edit"
 * @spring.property name = "successView" value="redirect:/expressionExperiment/showAllExpressionExperiments.html"
 * @spring.property name = "bioMaterialService" ref="bioMaterialService"
 * @spring.property name = "externalDatabaseService" ref="externalDatabaseService"
 * @spring.property name = "validator" ref="bioMaterialValidator"
 */
public class BioMaterialFormController extends BaseFormController {

    private Log log = LogFactory.getLog( this.getClass() );

    BioMaterialService bioMaterialService = null;

    ExternalDatabaseService externalDatabaseService = null;

    /**
     * 
     *
     */
    public BioMaterialFormController() {
        /* if true, reuses the same command object across the edit-submit-process (get-post-process). */
        setSessionForm( true );
        setCommandClass( BioMaterial.class );
    }

    /**
     * @param request
     * @return Object
     * @throws ServletException
     * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
     */
    @Override
    protected Object formBackingObject( HttpServletRequest request ) {
        BioMaterial ba = null;

        log.debug( "entering formBackingObject" );

        String id_param = ServletRequestUtils.getStringParameter( request, "id", "" );

        Long id = Long.parseLong( id_param );

        if ( !id.equals( null ) )
            ba = bioMaterialService.load( id );
        else
            ba = BioMaterial.Factory.newInstance();

        return ba;
    }

    /**
     * @param request
     * @return Map
     */
    @Override
    @SuppressWarnings( { "unchecked", "unused" })
    protected Map<String, Collection<ExternalDatabase>> referenceData( HttpServletRequest request ) {
        Collection<ExternalDatabase> edCol = externalDatabaseService.loadAll();
        Map<String, Collection<ExternalDatabase>> edMap = new HashMap<String, Collection<ExternalDatabase>>();
        edMap.put( "externalDatabases", edCol );
        return edMap;
    }

    /**
     * @param request
     * @param response
     * @param command
     * @param errors
     * @return ModelAndView
     * @throws Exception
     * @see org.springframework.web.servlet.mvc.AbstractFormController#processFormSubmission(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse, java.lang.Object, org.springframework.validation.BindException)
     */
    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {

        log.debug( "entering processFormSubmission" );

        // String accession = request.getParameter( "bioMaterialImpl.accession.accession" );

        // if ( accession == null ) {
        // // do nothing
        // } else {
        // if ( request.getParameter( "cancel" ) != null ) {
        // /*
        // * return new ModelAndView( new RedirectView( "http://" + request.getServerName() + ":" +
        // * request.getServerPort() + request.getContextPath() + "/bioAssay/showBioAssay.html?id=" +
        // * id.toString() ) );
        // */
        // return new ModelAndView( "bioMaterial.detail" ).addObject( "bioMaterial", command );
        // }
        //
        // /* database entry */
        // ( ( BioMaterial ) command ).getExternalAccession().setAccession( accession );
        //
        // /* external database */
        // ExternalDatabase ed = ( ( ( BioMaterial ) command ).getExternalAccession().getExternalDatabase() );
        // ed = externalDatabaseService.findOrCreate( ed );
        // ( ( BioMaterial ) command ).getExternalAccession().setExternalDatabase( ed );
        // }
        return super.processFormSubmission( request, response, command, errors );
    }

    /**
     * @param request
     * @param response
     * @param command
     * @param errors
     * @return ModelAndView
     * @throws Exception
     */
    @Override
    @SuppressWarnings("unused")
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        log.debug( "entering onSubmit" );

        BioMaterial ba = ( BioMaterial ) command;
        bioMaterialService.update( ba );

        saveMessage( request, "object.saved", new Object[] { ba.getClass().getSimpleName(), ba.getId() }, "Saved" );

        return new ModelAndView( getSuccessView() );
    }

    /**
     * @param bioMaterialService
     */
    public void setBioMaterialService( BioMaterialService bioMaterialService ) {
        this.bioMaterialService = bioMaterialService;
    }

    /**
     * @param externalDatabaseDao
     */
    public void setExternalDatabaseService( ExternalDatabaseService externalDatabaseService ) {
        this.externalDatabaseService = externalDatabaseService;
    }

}
