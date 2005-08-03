package interceptor;

import java.util.Collection;
import java.util.Random;

import junit.framework.TestCase;

import org.springframework.beans.factory.BeanFactory;

import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignService;
import edu.columbia.gemma.util.SpringContextUtil;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class PersistAclInterceptorTest extends TestCase {
    ArrayDesign ad = null;

    protected void setUp() throws Exception {
        super.setUp();
        
        ad = ArrayDesign.Factory.newInstance();
        
        ad.setName((new Random()).toString());
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Calling the method saveArrayDesign, which should have the PersistAclInterceptor.invoke called on it before the
     * actual method invocation.
     * 
     * @throws Exception
     */
    public void testGetArrayDesignsWithDao() throws Exception {
        BeanFactory ctx = SpringContextUtil.getApplicationContext();
             
        ( ( ArrayDesignService ) ctx.getBean( "arrayDesignService" ) ).saveArrayDesign(ad);

    }

}
