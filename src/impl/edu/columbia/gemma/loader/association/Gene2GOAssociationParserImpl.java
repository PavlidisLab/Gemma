/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package edu.columbia.gemma.loader.association;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.association.Gene2GOAssociation;
import edu.columbia.gemma.common.description.OntologyEntry;
import edu.columbia.gemma.common.description.OntologyEntryDao;
import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.GeneDao;
import edu.columbia.gemma.loader.expression.PersisterHelper;
import edu.columbia.gemma.loader.loaderutils.BasicLineMapParser;
import edu.columbia.gemma.loader.loaderutils.ParserAndLoaderTools;
import edu.columbia.gemma.loader.loaderutils.Persister;

/**
 * FIXME separate parsing from persisting. This class should implement Parser and yield a collection of
 * Gene2GoAssociations.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @author pavlidis
 * @spring.bean id="gene2GOAssociationParser"
 * @spring.property name="ontologyEntryDao" ref="ontologyEntryDao"
 * @spring.property name="geneDao" ref="geneDao"
 * @spring.property name="persisterHelper" ref="persisterHelper"
 * @author keshav
 * @version $Id$
 */
public class Gene2GOAssociationParserImpl extends BasicLineMapParser implements Persister {
    protected static final Log log = LogFactory.getLog( Gene2GOAssociationParserImpl.class );

    Method methodToInvoke = null;

    Map<Object, Gene2GOAssociation> g2GOMap = null;

    private OntologyEntryDao ontologyEntryDao = null;

    private GeneDao geneDao = null;

    private Gene2GOAssociationMappings gene2GOAssociationMappings = null;

    private String filename;

    private PersisterHelper persisterHelper;

    int i = 0;

    public Gene2GOAssociationParserImpl() {
        g2GOMap = new HashMap<Object, Gene2GOAssociation>();
        try {
            gene2GOAssociationMappings = new Gene2GOAssociationMappings();
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param name
     */
    public Gene2GOAssociationParserImpl( String name ) {
        g2GOMap = new HashMap<Object, Gene2GOAssociation>();
        filename = name;
    }

    /**
     * @param is
     * @param lineParseMethod
     * @return Map
     * @throws IOException
     */
    public Map parse( InputStream is, Method lineParseMethod ) throws IOException {
        methodToInvoke = lineParseMethod;
        parse( is );
        ParserAndLoaderTools.debugMap( g2GOMap );
        return g2GOMap;
    }

    /**
     * @param filename
     * @return Map
     * @throws IOException
     */
    public Map parseToMap( String f ) throws IOException {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    /**
     * @param url
     * @return Map
     * @throws IOException
     * @throws ConfigurationException
     */
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

    public Collection<Object> persist( Collection<Object> col ) {
        for ( Object object : col ) {
            persist( object );
        }
        return col;
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Persister#persist(java.lang.Object)
     */
    public Object persist( Object obj ) {
        if ( obj == null ) return null;
        assert obj instanceof Gene2GOAssociation;
        Gene2GOAssociation g2go = ( Gene2GOAssociation ) obj;
        g2go.setGene( ( Gene ) persisterHelper.persist( g2go.getGene() ) );
        g2go
                .setAssociatedOntologyEntry( ( OntologyEntry ) persisterHelper.persist( g2go
                        .getAssociatedOntologyEntry() ) );

        return g2go;

    }

    @Override
    public Object parseOneLine( String line ) {
        assert gene2GOAssociationMappings != null;
        assert g2GOMap != null;
        Gene2GOAssociation g2GO = null;

        try {
            Object obj = methodToInvoke.invoke( getGene2GOAssociationMappings(), new Object[] { line } );
            if ( obj == null ) return obj;
            g2GO = ( Gene2GOAssociation ) obj;
            g2GOMap.put( getKey( g2GO ), g2GO );

            return g2GO;

        } catch ( IllegalArgumentException e ) {
            log.error( e, e );
            return null;
        } catch ( IllegalAccessException e ) {
            log.error( e, e );
            return null;
        } catch ( InvocationTargetException e ) {
            log.error( e, e );
            return null;
        }

    }

    /**
     * @param newItem
     * @return Object
     */
    protected Object getKey( Object newItem ) {
        if ( !( newItem instanceof Gene2GOAssociation ) ) throw new IllegalArgumentException();
        Gene2GOAssociation g2GO = ( Gene2GOAssociation ) newItem;
        return g2GO.getAssociatedGene() + " " + g2GO.getAssociatedOntologyEntry() + " " + g2GO.getEvidenceCode();
    }

    /**
     * @return Returns the ontologyEntryDao.
     */
    public OntologyEntryDao getOntologyEntryDao() {
        return ontologyEntryDao;
    }

    /**
     * @param ontologyEntryDao The ontologyEntryDao to set.
     */
    public void setOntologyEntryDao( OntologyEntryDao ontologyEntryDao ) {
        this.ontologyEntryDao = ontologyEntryDao;
    }

    /**
     * @return Returns the geneDao.
     */
    public GeneDao getGeneDao() {
        return geneDao;
    }

    /**
     * @param geneDao The geneDao to set.
     */
    public void setGeneDao( GeneDao geneDao ) {
        this.geneDao = geneDao;
    }

    /**
     * @return Returns the gene2GOAssociationMappings.
     */
    public Gene2GOAssociationMappings getGene2GOAssociationMappings() {
        return gene2GOAssociationMappings;
    }

    /**
     * @param gene2GOAssociationMappings The gene2GOAssociationMappings to set.
     */
    public void setGene2GOAssociationMappings( Gene2GOAssociationMappings gene2GOAssociationMappings ) {
        this.gene2GOAssociationMappings = gene2GOAssociationMappings;
    }

    /**
     * @param persisterHelper The persisterHelper to set.
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

}
