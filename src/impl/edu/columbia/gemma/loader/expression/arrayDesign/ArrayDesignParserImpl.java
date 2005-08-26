package edu.columbia.gemma.loader.expression.arrayDesign;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.common.auditAndSecurity.Contact;
import edu.columbia.gemma.common.auditAndSecurity.ContactDao;
import edu.columbia.gemma.common.description.LocalFile;
import edu.columbia.gemma.common.description.LocalFileDao;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.loader.association.Gene2GOAssociationMappings;
import edu.columbia.gemma.loader.loaderutils.BasicLineMapParser;
import edu.columbia.gemma.loader.loaderutils.ParserByMap;
import edu.columbia.gemma.loader.loaderutils.ParserAndLoaderTools;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @spring.property name="arrayDesignMappings" ref="arrayDesignMappings"
 * @spring.property name="contactDao" ref="contactDao"
 * @spring.property name="localFileDao" ref="localFileDao"
 */
public class ArrayDesignParserImpl extends BasicLineMapParser implements ParserByMap {
    protected static final Log log = LogFactory.getLog( ArrayDesignParserImpl.class );

    private Map arrayDesignMap = null;

    private ArrayDesignMappings arrayDesignMappings = null;

    private String filename = null;

    Method methodToInvoke = null;

    ContactDao contactDao = null;

    LocalFileDao localFileDao = null;

    /**
     * 
     */
    public ArrayDesignParserImpl() {
        super();
        arrayDesignMap = new HashMap();
    }

    public Collection<ArrayDesign> createOrGetDependencies( Object[] dependencies, Map adMap ) {
        Set adKeysSet = null;

        Contact contact = null;

        LocalFile localFile = null;

        Collection<LocalFile> localFiles = new HashSet<LocalFile>();

        for ( Object obj : dependencies ) {
            Class c = obj.getClass();
            if ( c.getName().endsWith( "ContactImpl" ) )
                contact = createOrGetContact( ( Contact ) obj );
            else if ( c.getName().endsWith( "LocalFileImpl" ) ) {
                localFile = createOrGetLocalFile( ( LocalFile ) obj );
                localFiles.add( localFile );
            } else {
                throw new IllegalArgumentException( "Make sure you have specified valid dependencies" );
            }
        }
        log.info( "creating Gemma objects ... " );

        adKeysSet = adMap.keySet();

        Collection<ArrayDesign> arrayDesignCol = new HashSet<ArrayDesign>();

        // create Gemma domain objects
        for ( Object key : adKeysSet ) {
            ArrayDesign ad = ( ArrayDesign ) adMap.get( key );

            ad.setDesignProvider( contact );

            ad.setLocalFiles( localFiles );

            arrayDesignCol.add( ad );

        }
        return arrayDesignCol;
    }

    private Contact createOrGetContact( Contact c ) {
        if ( getContacts().size() == 0 )
            this.getContactDao().create( c );
        else {
            Collection<Contact> contacts = getContacts();

            for ( Contact contact : contacts ) {

                String name = contact.getName();
                if ( name == null ) break;
                if ( contact.getName().equals( c.getName() ) ) {
                    log.info( "manufacturer: " + contact.getName() + " already exists" );
                    return contact;
                }
            }
            this.getContactDao().create( c );
            log.info( "contact: " + c.getName() + " created" );

        }
        return c;
    }

    /**
     * @param lf
     * @return LocalFile
     */
    private LocalFile createOrGetLocalFile( LocalFile lf ) {

        if ( getLocalFileEntries().size() == 0 )
            this.getLocalFileDao().create( lf );
        else {
            Collection<LocalFile> localFiles = getLocalFileEntries();
            for ( LocalFile localFile : localFiles ) {
                if ( localFile.getLocalURI().equals( lf.getLocalURI() ) ) {
                    log.info( "local file already exists" );
                    return localFile;
                }
            }
            this.getLocalFileDao().create( lf );
            log.info( "local file created." );
        }
        return lf;
    }

    /**
     * @return Collection
     */
    @SuppressWarnings("unchecked")
    private Collection<LocalFile> getLocalFileEntries() {
        return this.getLocalFileDao().findAll();
    }

    /**
     * @return Collection
     */
    @SuppressWarnings("unchecked")
    private Collection<Contact> getContacts() {
        return this.getContactDao().findAll();
    }

    /**
     * @return Returns the arrayDesignMappings.
     */
    public ArrayDesignMappings getArrayDesignMappings() {
        return arrayDesignMappings;
    }

    public Map parse( InputStream is, Method lineParseMethod ) throws IOException {
        methodToInvoke = lineParseMethod;
        parse( is );
        ParserAndLoaderTools.debugMap( arrayDesignMap );
        return arrayDesignMap;
    }

    public Map parseToMap( String filename ) throws IOException { 
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public Map parseFromHttp( String url ) throws IOException, ConfigurationException {
        InputStream is = ParserAndLoaderTools.retrieveByHTTP( url );

        GZIPInputStream gZipInputStream = new GZIPInputStream( is );

        Method lineParseMethod = null;
        try {
            lineParseMethod = ParserAndLoaderTools.findParseLineMethod( new Gene2GOAssociationMappings(), filename );
        } catch ( NoSuchMethodException e ) {
            log.error( e, e );
            return null;
        }
        return this.parse( gZipInputStream, lineParseMethod );
    }

    @Override
    public Object parseOneLine( String line ) {
        assert arrayDesignMappings != null;
        assert arrayDesignMap != null;

        ArrayDesign ad = null;

        try {
            Object obj = methodToInvoke.invoke( this.getArrayDesignMappings(), new Object[] { line } );
            if ( obj == null ) return obj;
            ad = ( ArrayDesign ) obj;
            arrayDesignMap.put( ad.getName(), ad );
            return ad;
        } catch ( IllegalArgumentException e ) {
            e.printStackTrace();
            return null;
        } catch ( IllegalAccessException e ) {
            e.printStackTrace();
            return null;
        } catch ( InvocationTargetException e ) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param arrayDesignMappings The arrayDesignMappings to set.
     */
    public void setArrayDesignMappings( ArrayDesignMappings arrayDesignMappings ) {
        this.arrayDesignMappings = arrayDesignMappings;
    }

    @Override
    protected Object getKey( Object newItem ) {
        return ( ( ArrayDesign ) newItem ).getName();
    }

    /**
     * @return Returns the contactDao.
     */
    public ContactDao getContactDao() {
        return contactDao;
    }

    /**
     * @param contactDao The contactDao to set.
     */
    public void setContactDao( ContactDao contactDao ) {
        this.contactDao = contactDao;
    }

    /**
     * @return Returns the localFileDao.
     */
    public LocalFileDao getLocalFileDao() {
        return localFileDao;
    }

    /**
     * @param localFileDao The localFileDao to set.
     */
    public void setLocalFileDao( LocalFileDao localFileDao ) {
        this.localFileDao = localFileDao;
    }

}
