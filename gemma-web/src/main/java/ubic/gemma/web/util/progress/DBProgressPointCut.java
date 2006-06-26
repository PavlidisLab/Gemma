package ubic.gemma.web.util.progress;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.support.DynamicMethodMatcherPointcut;

/**
 * 
 *
 * <hr>
 * <p>Copyright (c) 2006 UBC Pavlab
 * @author klc
 * @version $Id$\
 *
 * Point cutter for monintoring the status of DB persist operation
 */
public class DBProgressPointCut extends DynamicMethodMatcherPointcut {

   /* (non-Javadoc)
    * @see org.springframework.aop.MethodMatcher#matches(java.lang.reflect.Method, java.lang.Class)
    */
   
    
    private final transient Log log = LogFactory.getLog(DBProgressPointCut.class );
    
   @SuppressWarnings("unused")
   public boolean matches( Method arg0,  Class arg1 ) {
   
       log.debug("Before. Made it to matches with 2 args" + arg0 + arg1);
      if ( arg0.getName().equals("persist") ) {
        log.debug("After. Made it to matches with 2 args" + arg0 + arg1);
         return true;
      }
      return false;
   }

   
   /* (non-Javadoc)
    * @see org.springframework.aop.MethodMatcher#matches(java.lang.reflect.Method, java.lang.Class, java.lang.Object[])
    */
   @SuppressWarnings("unused")
   public boolean matches( Method arg0, Class arg1, Object[] arg2 ) {
       log.debug("Before: Made it to matches with 3 args");
      if ( arg2 != null ) {
         log.debug("After: Made it to matches with 3 args");
         return true;
      }
      return false;
   }

}
