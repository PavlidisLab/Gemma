package edu.columbia.gemma.loader.association;

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

import edu.columbia.gemma.association.Gene2GOAssociation;
import edu.columbia.gemma.common.description.OntologyEntry;
import edu.columbia.gemma.common.description.OntologyEntryDao;
import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.GeneDao;
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
 * @spring.bean id="gene2GOAssociationParser"
 * @spring.property name="ontologyEntryDao" ref="ontologyEntryDao"
 * @spring.property name="geneDao" ref="geneDao"
 * @spring.property name="gene2GOAssociationMappings" ref="gene2GOAssociationMappings"
 */

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class Gene2GOAssociationParserImpl extends BasicLineMapParser implements Parser {
    protected static final Log log = LogFactory.getLog( Gene2GOAssociationParserImpl.class );

    Method methodToInvoke = null;

    Map<String, Gene2GOAssociation> g2GOMap = null;

    private OntologyEntryDao ontologyEntryDao = null;

    private GeneDao geneDao = null;

    private Gene2GOAssociationMappings gene2GOAssociationMappings = null;

    private String filename;

    int i = 0;

    public Gene2GOAssociationParserImpl() {
        g2GOMap = new HashMap();
    }

    /**
     * @param name
     */
    public Gene2GOAssociationParserImpl( String name ) {
        g2GOMap = new HashMap();
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
        LoaderTools.debugMap( g2GOMap );
        return g2GOMap;
    }

    /**
     * @param filename
     * @return Map
     * @throws IOException
     */
    public Map parseFile( String filename ) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param url
     * @param dependencies
     * @return Collection
     * @throws IOException
     * @throws ConfigurationException
     */
    public Collection parseFromHttp( String url ) throws IOException, ConfigurationException {

        InputStream is = LoaderTools.retrieveByHTTP( url );

        GZIPInputStream gZipInputStream = new GZIPInputStream( is );

        Method lineParseMethod = null;
        try {
            lineParseMethod = LoaderTools.findParseLineMethod( new Gene2GOAssociationMappings(), filename );
        } catch ( NoSuchMethodException e ) {
            log.error( e, e );
            return null;
        }
        return this.parse( gZipInputStream, lineParseMethod ).values();

        // return createDependencies( dependencies, g2GOMap );
    }

    /**
     * @param dependencies
     * @param gene2GOAssMap
     * @return Collection
     * @see Parser
     */
    public Collection createOrGetDependencies( Object[] dependencies, Map gene2GOAssMap ) {
        Set gene2GOKeysSet = null;

        Gene gene = null;

        OntologyEntry ontologyEntry = null;

        for ( Object obj : dependencies ) {
            Class c = obj.getClass();
            if ( c.getName().endsWith( "GeneImpl" ) )
                gene = createOrGetGene( ( Gene ) obj );
            else if ( c.getName().endsWith( "OntologyEntryImpl" ) ) {
                ontologyEntry = createOrGetOntologyEntry( ( OntologyEntry ) obj );
            } else {
                throw new IllegalArgumentException( "Make sure you have specified valid dependencies" );
            }
        }
        log.info( "creating Gemma objects ... " );

        gene2GOKeysSet = gene2GOAssMap.keySet();

        Collection<Gene2GOAssociation> gene2GOAssCol = new HashSet<Gene2GOAssociation>();

        // create Gemma domain objects
        for ( Object key : gene2GOKeysSet ) {
            Gene2GOAssociation g2GO = Gene2GOAssociation.Factory.newInstance();

            g2GO.setGene( gene );

            g2GO.setAssociatedGene( gene );

            g2GO.setAssociatedOntologyEntry( ontologyEntry );

            gene2GOAssCol.add( g2GO );

        }
        return gene2GOAssCol;

    }

    /**
     * @param oe
     * @return OntologyEntry
     */
    private OntologyEntry createOrGetOntologyEntry( OntologyEntry oe ) {

        if ( getOntologyEntries().size() == 0 )
            this.getOntologyEntryDao().create( oe );
        else {
            Collection<OntologyEntry> ontologyEntries = getOntologyEntries();

            for ( OntologyEntry ontologyEntry : ontologyEntries ) {
                if ( ontologyEntry.getAccession().equals( oe.getAccession() )
                        && ontologyEntry.getExternalDatabase().getName().equalsIgnoreCase(
                                oe.getExternalDatabase().getName() ) ) {
                    log.info( "ontology entry: " + ontologyEntry.getExternalDatabase().getName() + "Accession: "
                            + ontologyEntry.getAccession() + " already exists" );
                    return ontologyEntry;
                }
            }
            this.getOntologyEntryDao().create( oe );
            log.info( "ontology entry: " + oe.getExternalDatabase().getName() + "Accession: " + oe.getAccession()
                    + "created" );

        }
        return oe;
    }

    /**
     * @return Collection
     */
    private Collection getOntologyEntries() {
        return this.getOntologyEntryDao().findAllOntologyEntries();
    }

    /**
     * @param g
     * @return Gene
     */
    private Gene createOrGetGene( Gene g ) {
        if ( getGenes().size() == 0 )
            this.getGeneDao().create( g );

        else {
            Collection<Gene> genes = getGenes();

            for ( Gene gene : genes ) {
                if ( gene.getNcbiId().equals( g.getNcbiId() ) ) {

                    log.info( "gene with ncbi id " + gene.getNcbiId() + " already exists" );
                    return gene;
                }
            }
            this.getGeneDao().create( g );
            log.info( "gene with ncbi id " + g.getNcbiId() + " created" );
        }
        return g;
    }

    /**
     * @return Collection<Gene>
     */
    private Collection<Gene> getGenes() {
        return this.getGeneDao().findAllGenes();
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
            g2GOMap.put( g2GO.getAssociatedGene() + " " + g2GO.getAssociatedOntologyEntry() + " "
                    + g2GO.getEvidenceCode(), g2GO );

            return g2GO;

        } catch ( IllegalArgumentException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch ( IllegalAccessException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch ( InvocationTargetException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }

    }

    /**
     * @param newItem
     * @return Object
     */
    protected Object getKey( Object newItem ) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @param string
     * @return Method
     * @throws NoSuchMethodException
     */
    public Method findParseLineMethod( String string ) throws NoSuchMethodException {
        // TODO Auto-generated method stub
        return null;
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

}
