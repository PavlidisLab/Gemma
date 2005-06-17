package edu.columbia.gemma.loader.expression.arraydesign;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.loader.association.Gene2GOAssociationMappings;
import edu.columbia.gemma.loader.genome.gene.Parser;
import edu.columbia.gemma.loader.loaderutils.BasicLineMapParser;
import edu.columbia.gemma.loader.loaderutils.LoaderTools;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @spring.property name="arrayDesignMappings" ref="arrayDesignMappings"
 */
public class ArrayDesignParserImpl extends BasicLineMapParser implements Parser {
    protected static final Log log = LogFactory.getLog( ArrayDesignParserImpl.class );

    private Map arrayDesignMap;

    private ArrayDesignMappings arrayDesignMappings;

    private String filename;

    Method methodToInvoke = null;

    public Collection createOrGetDependencies( Object[] dependencies, Map objectMap ) {
        // TODO Auto-generated method stub
        return null;
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
        LoaderTools.debugMap( arrayDesignMap );
        return arrayDesignMap;
    }

    public Map parseFile( String filename ) throws IOException {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    public Map parseFromHttp( String url ) throws IOException, ConfigurationException {
        InputStream is = LoaderTools.retrieveByHTTP( url );

        GZIPInputStream gZipInputStream = new GZIPInputStream( is );

        Method lineParseMethod = null;
        try {
            lineParseMethod = LoaderTools.findParseLineMethod( new Gene2GOAssociationMappings(), filename );
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
            Object obj = methodToInvoke.invoke( arrayDesignMappings, new Object[] { line } );
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
        // TODO Auto-generated method stub
        return null;
    }

}
