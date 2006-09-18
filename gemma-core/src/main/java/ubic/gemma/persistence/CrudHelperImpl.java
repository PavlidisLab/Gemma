package ubic.gemma.persistence;

import java.beans.PropertyDescriptor;

import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.dao.DataAccessException;

/**
 * @author pavlidis
 * @version $Id$
 */
public class CrudHelperImpl implements CrudHelper, ApplicationContextAware {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.CrudHelper#cascadeCreateOrUpdate(java.lang.Object)
     */
    public Object cascadeCreateOrUpdate( Object entity ) throws DataAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.CrudHelper#cascadeUpdate(java.lang.Object)
     */
    public void cascadeUpdate( Object entity ) throws DataAccessException {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.CrudHelper#createOrUpdate(java.lang.Object)
     */
    public Object createOrUpdate( Object entity ) throws DataAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.CrudHelper#delete(java.lang.Object)
     */
    public void delete( Object entity ) throws IllegalArgumentException {
        if ( CrudUtils.isTransient( entity ) ) throw new IllegalArgumentException( "entity was transient" );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.CrudHelper#find(java.lang.Object)
     */
    public Object find( Object entity ) throws DataAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.CrudHelper#findOrCreate(java.lang.Object)
     */
    public Object findOrCreate( Object entity ) throws DataAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.CrudHelper#strictCreate(java.lang.Object)
     */
    public Object strictCreate( Object entity ) throws IllegalArgumentException {
        if ( !CrudUtils.isTransient( entity ) ) throw new IllegalArgumentException( "entity was transient" );

        PropertyDescriptor[] props = PropertyUtils.getPropertyDescriptors( entity );

        // can be done recursively; however, if the mode for an association is cascade create or all, don't call create
        // on it.

        // for ( int i = 0; i < props.length; i++ ) {
        // PropertyDescriptor descriptor = props[i];
        // Method setter = descriptor.getWriteMethod();
        // if ( setter == null ) continue;
        // Method getter = descriptor.getReadMethod();
        // try {
        // Object association = getter.invoke( sourceObject, new Object[] {} );
        // if ( sourceValue == null ) continue;
        // Object persistedValue = getter.invoke( targetObject, new Object[] {} );
        // if ( persistedValue == null || update ) {
        // setter.invoke( targetObject, new Object[] { sourceValue } );
        // }
        // } catch ( IllegalArgumentException e ) {
        // throw new RuntimeException( e );
        // } catch ( IllegalAccessException e ) {
        // throw new RuntimeException( e );
        // } catch ( InvocationTargetException e ) {
        // throw new RuntimeException( e );
        // }
        // }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.CrudHelper#strictUpdate(java.lang.Object)
     */
    public void strictUpdate( Object entity ) throws DataAccessException {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    public void setApplicationContext( ApplicationContext applicationContext ) throws BeansException {
        // TODO Auto-generated method stub

    }

}
