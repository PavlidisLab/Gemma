package edu.columbia.gemma.loader.expression.arrayDesign;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.expression.designElement.DesignElement;

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

    private final int MGU74A_COMPOSITESEQUENCE_NAME = conf.getInt( "mgu74a.compositesequence.name" );
    private final int MGU74A_COMPOSITESEQUENCE_DESCRIPTION = conf.getInt( "mgu74a.compositesequence.description" );

    Map<String, ArrayDesign> arrayDesignMap = new HashMap<String, ArrayDesign>();
    Map<String, Contact> designProvidersMap = null;
    Collection<DesignElement> designElements = new HashSet<DesignElement>();

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

        arrayDesign.setNumberOfFeatures( Integer.parseInt( values[ARRAY_NUM_OF_REPORTERS] ) );

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

        if ( line.startsWith( "Probe" ) ) return null;

        if ( values.length == 0 ) return null;

        ArrayDesign arrayDesign = checkAndGetExistingArrayDesign( "MG-U74A" );

        CompositeSequence cs = CompositeSequence.Factory.newInstance();
        cs.setName( values[MGU74A_COMPOSITESEQUENCE_NAME] );
        cs.setDescription( values[MGU74A_COMPOSITESEQUENCE_DESCRIPTION] );

        // NOTE: if you use cs.setArrayDesign(arrayDesign), 
        // make sure you set inverse="true" in ArrayDesign.hbm.xml under the <set> designElements.  This tells 
        // hibernate to only propagate changes to the database that are made from the arrayDesign end.  That is,
        // arrayDesign.setDesignElements(designElements).  If you do not set inverse="true", then you must leave
        // this first line out (that is: cs.setArrayDesign(arrayDesign)).
        cs.setArrayDesign( arrayDesign );
        designElements.add( cs );
        arrayDesign.setDesignElements( designElements );

        return arrayDesign;
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
