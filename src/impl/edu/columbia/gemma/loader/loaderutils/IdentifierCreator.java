package edu.columbia.gemma.loader.loaderutils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;

import edu.columbia.gemma.common.Identifiable;
import edu.columbia.gemma.util.ReflectionUtil;

/**
 * Class to create object identifiers according to pre-established rules.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class IdentifierCreator {

    private static final String IDENTIFIER_FIELD_SEPARATOR = ":";
    private static final String FINDBYIDENTIFIER_METHOD_NAME = "findByIdentifier";

    protected static final Log log = LogFactory.getLog( IdentifierCreator.class );

    /**
     * @param gemmaObj
     * @return Identifier constructed from object fields.
     */
    public static String create( Identifiable gemmaObj ) {
        return create( gemmaObj, null );
    }

    /**
     * @param gemmaObj
     * @return
     */
    public static String create( Identifiable gemmaObj, BeanFactory ctx ) {

        if ( gemmaObj == null ) return null;

        try {

            // fixme: what if it already has an identifier?

            String candidateIdentifier = constructIdentifier( gemmaObj );

         //   log.debug( "Identifier constructed: " + candidateIdentifier );

            if ( ctx == null ) return candidateIdentifier; // no way to check.

            checkForDuplicate( gemmaObj, ctx, candidateIdentifier.toString() );

            return candidateIdentifier.toString();

        } catch ( Exception e ) {
            log.error( e, e );
        }

        return null;
    }

    /**
     * @param gemmaObj
     * @return
     * @throws IntrospectionException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    private static String constructIdentifier( Identifiable gemmaObj ) throws IntrospectionException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        Class clazz = gemmaObj.getClass();
        BeanInfo beanInfo = Introspector.getBeanInfo( clazz );

        String baseName =  ReflectionUtil.getSimpleName(ReflectionUtil.getBaseForImpl( gemmaObj ) );

        StringBuffer identBuf = new StringBuffer( baseName );

        // fixme: should only use non-nullable fields. 'name' isn't one of them.
        if ( gemmaObj.getName() != null ) identBuf.append( IDENTIFIER_FIELD_SEPARATOR + gemmaObj.getName() );

        // fixme: order isn't predictable --- these are lame rules.

        PropertyDescriptor[] props = beanInfo.getPropertyDescriptors();
        for ( int i = 0; i < props.length; i++ ) {
            PropertyDescriptor pdes = props[i];

            if ( pdes.getDisplayName().equals( "name" ) ) continue;

            Object ob = pdes.getReadMethod().invoke( gemmaObj, new Object[] {} );

            if ( ob == null ) continue;

            if ( ob.getClass().getName().startsWith( "java.lang.String" ) ) {
                identBuf.append( IDENTIFIER_FIELD_SEPARATOR + ob );
            }
        }
        return identBuf.toString();
    }

    /**
     * @param gemmaObj
     * @param ctx
     * @param identBuf
     * @throws BeansException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    private static void checkForDuplicate( Identifiable gemmaObj, BeanFactory ctx, String candidateIdentifier )
            throws BeansException, NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {

        String daoName = ReflectionUtil.constructDaoName( gemmaObj );

        if ( daoName == null ) throw new IllegalArgumentException( "No dao name could be constructed" );

        Object dao = ctx.getBean( daoName );
        Method finder = dao.getClass().getMethod( FINDBYIDENTIFIER_METHOD_NAME, new Class[] { String.class } );
        Identifiable existing = ( Identifiable ) finder.invoke( dao, new Object[] { candidateIdentifier.toString() } );
        if ( existing != null ) {
            log.debug("Exists already");
            throw new IllegalArgumentException( "Identifiable with id " + candidateIdentifier.toString()
                    + " exists already" ); // probably a better exception could be used.
        }
    }

}
