package edu.columbia.gemma.loader.loaderutils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;

import edu.columbia.gemma.util.ReflectionUtil;

/**
 * Given a collection of Gemma objects, persist them.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="creatorService"
 */
public class CreatorService {

    private static final String CREATE_METHOD_NAME = "create";

    protected static final Log log = LogFactory.getLog( CreatorService.class );

    /**
     * @param gemmaObjs
     */
    public void bulkCreate( Collection gemmaObjs, BeanFactory ctx ) {
        for ( Iterator iter = gemmaObjs.iterator(); iter.hasNext(); ) {
            Object gemmaObj = iter.next();
            create( ctx, gemmaObj );
        }
    }

    /**
     * @param ctx
     * @param gemmaObj
     */
    private void create( BeanFactory ctx, Object gemmaObj ) {
        try {
            Class clazz = gemmaObj.getClass();
            Object dao = ctx.getBean( ReflectionUtil.constructDaoName( gemmaObj ) );
            Method create = dao.getClass().getMethod( CREATE_METHOD_NAME, new Class[] { clazz } );
            // this will not work if cascade is "none"
            // does not check if object already exists.
            create.invoke( dao, new Object[] { gemmaObj } );
        } catch ( Exception e ) {
            log.error( e, e );
        }
    }

}
