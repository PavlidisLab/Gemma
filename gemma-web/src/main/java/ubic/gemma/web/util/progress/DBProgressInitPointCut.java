package ubic.gemma.web.util.progress;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.support.StaticMethodMatcherPointcut;

/**
 * 
 *
 * <hr>
 * <p>Copyright (c) 2006 UBC Pavlab
 * @author klc
 * @version $Id$
 */
public class DBProgressInitPointCut extends StaticMethodMatcherPointcut {

   /* (non-Javadoc)
    * @see org.springframework.aop.MethodMatcher#matches(java.lang.reflect.Method, java.lang.Class)
    * 
    * Called just once on inilization.  to set up the duration of the progress bar. Called on a collection commit.
    */
   
    private final transient Log log = LogFactory.getLog(DBProgressInitPointCut.class );
    
    
   @SuppressWarnings("unused")
   public boolean matches( Method arg0, Class arg1 ) {
       log.debug("DBINIT Made it to matches with 2 args: arg0=" + arg0 +  "  arg1="+ arg1);
       
      if (arg0.getName().equals("persist"))
            return true;
      return false;
   }

}
