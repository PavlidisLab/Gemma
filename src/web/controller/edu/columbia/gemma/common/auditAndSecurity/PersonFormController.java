/*
 * TODO add proper validation. This will be in the
 * action-servlet.xml file and validator Object. TODO before running the savePerson(person), you should check to see if
 * the person already exists.
 * TODO add the delete functionality.
 */

package controller.edu.columbia.gemma.common.auditAndSecurity;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;

import edu.columbia.gemma.common.auditAndSecurity.Person;
import edu.columbia.gemma.common.auditAndSecurity.PersonImpl;
import edu.columbia.gemma.common.auditAndSecurity.PersonService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author keshav
 * @version $Id keshav$
 */

public class PersonFormController extends SimpleFormController {
   protected static final Log log = LogFactory.getLog( PersonFormController.class );
   private PersonService mgr = null;
   //    protected Object formBackingObject(HttpServletRequest request)
   //    throws Exception {
   //        String id = request.getParameter("id");
   //        Person person = null;
   //
   //        if (!StringUtils.isEmpty(id)) {
   //            person = mgr.getPersonDao(id);
   //        } else {
   //            person = new PersonImpl();
   //        }
   //        return person;
   //    }
   /**
    * @return
    * @param request
    */
   protected Object formBackingObject( HttpServletRequest request )
         throws Exception {
      log.info( "formBackingObject" );
      String firstName = request.getParameter( "firstName" );
      String middleName = request.getParameter( "middleName" );
      String lastName = request.getParameter( "lastName" );
      Person person = null;

      if ( !StringUtils.isEmpty( lastName ) ) {
         person = mgr.findByName( firstName, lastName, middleName );
      } else {
         person = new PersonImpl();
      }
      return person;
   }

   /**
    * @return
    * @param request
    * @param response
    * @param command
    * @param errors
    */
   public ModelAndView onSubmit( HttpServletRequest request,
         HttpServletResponse response, Object command, BindException errors )
         throws Exception {
      log.info( "onSubmit" );
      Person person = ( Person ) command;
      //boolean isNew = (person.getId() == null);
      boolean isNew = ( person.getFirstName() == null
            && person.getLastName() == null && person.getMiddleName() == null );
      String success = getSuccessView();
      Locale locale = request.getLocale();

      Object[] args = new Object[] {
         person.getFirstName() + ' ' + person.getLastName()
      };

              if (request.getParameter("delete") != null) {
                  mgr.removePerson(person);
      
//                  saveMessage(request, getText("person.deleted", args, locale));
//              } else {
//                  mgr.savePerson(person);
//      
//                  String key = (isNew) ? "person.added" : "person.updated";
//                  saveMessage(request, getText(key, args, locale));
//      
//                  if (!isNew) {
//                      success = "editPerson.html?id=" + person.getId();
//                  }
              }
      mgr.savePerson( person );
      return new ModelAndView( new RedirectView( success ) );
   }

   /**
    * @return
    * @param request
    * @param response
    * @param command
    * @param errors
    */
   public ModelAndView processFormSubmission( HttpServletRequest request,
         HttpServletResponse response, Object command, BindException errors )
         throws Exception {
      log.info( "processFormSubmission" );
      if ( request.getParameter( "cancel" ) != null ) {
         return new ModelAndView( new RedirectView( getSuccessView() ) );
      }
      return super.processFormSubmission( request, response, command, errors );
   }

   /**
    * @param mgr
    */
   public void setPersonService( PersonService mgr ) {
      this.mgr = mgr;
   }
}