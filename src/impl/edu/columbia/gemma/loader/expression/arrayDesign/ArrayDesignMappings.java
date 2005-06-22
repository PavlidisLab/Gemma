package edu.columbia.gemma.loader.expression.arrayDesign;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.common.auditAndSecurity.Contact;
import edu.columbia.gemma.common.auditAndSecurity.ContactDao;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignDao;

/**
 * The mappings based on different files.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean name="arrayDesignMappings"
 */
public class ArrayDesignMappings {
    protected static final Log log = LogFactory.getLog( ArrayDesignMappings.class );
    Configuration conf = new PropertiesConfiguration( "Gemma.properties" );

    private final int ARRAY_ARRAYDESIGN_NAME = conf.getInt( "array.name" );
    private final int ARRAY_DESCRIPTION = conf.getInt( "array.description" );
    private final int ARRAY_NUM_OF_REPORTERS = conf.getInt( "array.numofreporters" );

    Map<String, ArrayDesign> arrayDesignMap = new HashMap<String, ArrayDesign>();
    Map<String, Contact> designProvidersMap = null;

    private ContactDao contactDao = null;
    private ArrayDesignDao arrayDesignDao = null;

    /**
     * 
     */
    public ArrayDesignMappings() throws ConfigurationException {
        super();
    }

    /**
     * map from file array.txt
     * 
     * @param line
     * @return Object
     */
    public Object mapFromArray( String line ) {
        String[] values = StringUtils.split( line, "\t" );

        ArrayDesign arrayDesign = checkAndGetExistingArrayDesign( values[ARRAY_ARRAYDESIGN_NAME] );

        arrayDesign.setDescription( values[ARRAY_DESCRIPTION] );

        arrayDesign.setNumberOfCompositeSequences( ARRAY_NUM_OF_REPORTERS );

        return arrayDesign;
    }

    /**
     * map from file MG-U74A.txt
     * 
     * @param line
     * @return Object
     */
    public Object mapFromMGU74A( String line ) {
        String[] values = StringUtils.split( line, "\t" );

        if ( line.startsWith( "Probe" ) || line.startsWith( " " ) ) return null;

        return null;
    }

    /**
     * You need this method if you are creating your objects from multiple files.
     * 
     * @param name
     * @return ArrayDesign
     */
    private ArrayDesign checkAndGetExistingArrayDesign( String name ) {
        if ( arrayDesignMap.containsKey( name ) ) return arrayDesignMap.get( name );

        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( name );
        arrayDesignMap.put( name, ad );
        return ad;
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
     * @return Returns the arrayDesignDao.
     */
    public ArrayDesignDao getArrayDesignDao() {
        return arrayDesignDao;
    }

    /**
     * @param arrayDesignDao The arrayDesignDao to set.
     */
    public void setArrayDesignDao( ArrayDesignDao arrayDesignDao ) {
        this.arrayDesignDao = arrayDesignDao;
    }

}
