/*
 * The Gemma project
 * 
 * Copyright (c) 2007 Columbia University
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
package ubic.gemma.ontology;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.util.ConfigUtils;

import com.hp.hpl.jena.db.DBConnection;
import com.hp.hpl.jena.db.IDBConnection;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RDFVisitor;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * Service to load an OWL-formatted ontology into the system. The first time the ontology is accessed it is persisted
 * into the local system (can be slow) and this version is used for future accesses.
 * 
 * @author paul
 * @version $Id$
 */
public class OntologyLoader {

    private static Log log = LogFactory.getLog( OntologyLoader.class.getName() );

    /**
     * Ontology must be in the persistent store for this to work.
     * 
     * @param url
     * @return
     */
    protected static ExternalDatabase ontologyAsExternalDatabase( String url ) {
        ExternalDatabase ontology = ExternalDatabase.Factory.newInstance();
        ontology.setType( DatabaseType.ONTOLOGY );
        ontology.setWebUri( url );
        // Ontology ont = getOntology( url );

        // if ( ont != null ) {
        // log.info( "Getting information about " + ont );
        // // jenaOntToExternalDatabase( ontology, ont );
        // }

        return ontology;
    }

    protected static void jenaOntToExternalDatabase( ExternalDatabase ontology, Ontology ont ) {
        StmtIterator iterator = ont.listProperties();
        ontology.setType( DatabaseType.ONTOLOGY );

        while ( iterator.hasNext() ) {
            Statement statement = iterator.nextStatement();
            Property predicate = statement.getPredicate();
            RDFNode object = statement.getObject();
            if ( predicate.getLocalName().equals( "title" ) ) {
                ontology.setName( asString( object ) );
            } else if ( predicate.getLocalName().equals( "description" ) ) {
                ontology.setDescription( asString( object ) );
            } else if ( predicate.getLocalName().equals( "definition" ) ) {
                ontology.setDescription( asString( object ) );
            }
        }
    }

    /**
     * @param url
     * @return
     */
    protected static Ontology getOntology( String url ) {
        OntModel model = getRDBModel( url );
        Ontology ont = null;
        Map m = model.getNsPrefixMap();
        for ( Object o : m.keySet() ) {
            if ( StringUtils.isBlank( ( String ) o ) ) {
                String prefix = model.getNsPrefixURI( ( String ) o );
                if ( prefix == null ) {
                    continue;
                }
                ont = model.getOntology( prefix.replace( "#", "" ) );
            }
        }
        return ont;
    }

    /**
     * Use to pretty-print a RDFNode
     * 
     * @param object
     * @return
     */
    public static String asString( RDFNode object ) {
        return ( String ) object.visitWith( new RDFVisitor() {

            @SuppressWarnings("unused")
            public Object visitBlank( Resource r, AnonId id ) {
                return r.getLocalName();
            }

            public Object visitLiteral( Literal l ) {
                return l.toString().replaceAll( "\\^\\^.+", "" );
            }

            @SuppressWarnings("unused")
            public Object visitURI( Resource r, String uri ) {
                return r.getLocalName();
            }
        } );
    }

    /**
     * @param po
     * @return
     */
    protected static ModelMaker getRDBMaker() {
        PersistentOntology po = new PersistentOntology();
        String dbUrl = ConfigUtils.getString( "gemma.jena.db.url" );
        String user = ConfigUtils.getString( "gemma.jena.db.user" );
        String pwd = ConfigUtils.getString( "gemma.jena.db.password" );
        String type = ConfigUtils.getString( "gemma.jena.db.type" );
        String driver = ConfigUtils.getString( "gemma.jena.db.driver" );

        try {
            Class.forName( driver );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

        ModelMaker maker = po.getRDBMaker( dbUrl, user, pwd, type, false );
        return maker;
    }

    /**
     * Deletes all cached ontologies from the system. Use with care!
     */
    protected static void wipePersistentStore() {
        // PersistentOntology po = new PersistentOntology();
        String dbUrl = ConfigUtils.getString( "gemma.jena.db.url" );
        String user = ConfigUtils.getString( "gemma.jena.db.user" );
        String pwd = ConfigUtils.getString( "gemma.jena.db.password" );
        String type = ConfigUtils.getString( "gemma.jena.db.type" );
        String driver = ConfigUtils.getString( "gemma.jena.db.driver" );

        try {
            Class.forName( driver );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
        IDBConnection conn = new DBConnection( dbUrl, user, pwd, type );

        try {
            conn.cleanDB();
            conn.close();
        } catch ( SQLException e ) {
            throw new RuntimeException();
        }

    }

    /**
     * Load an ontology into memory. Use this type of model when fast access is critical and memory is available.
     * 
     * @param url
     * @return
     * @throws IOException
     */
    public static OntModel loadMemoryModel( String url, OntModelSpec spec ) {
        OntModel model = getMemoryModel( url, spec );
        model.read( url );
        return model;
    }

    /**
     * This is primarily here for testing purposes.
     * 
     * @param is
     * @param name
     * @return
     * @throws IOException
     */
    public static OntModel loadMemoryModel( InputStream is, String name, OntModelSpec spec ) {
        OntModel model = getMemoryModel( name, spec );
        model.read( is, null );
        return model;
    }

    /**
     * Load a model backed by a persistent store. This type of model is much slower than memory models but uses much
     * less memory.
     * 
     * @param url
     * @param force model to be loaded into the database, even if it already exists there.
     * @return
     * @throws IOException
     */
    public static OntModel loadPersistentModel( String url, boolean force ) {
        return persistModelIfNecessary( url, force );
    }

    /**
     * @param url
     * @param model
     * @return
     */
    public static Collection<OntologyResource> initialize( String url, OntModel model ) {
        ExternalDatabase database = ontologyAsExternalDatabase( url );
        Collection<OntologyResource> result = new HashSet<OntologyResource>();

        ExtendedIterator iterator = model.listClasses();
        int count = 0;
        log.info( "Reading classes..." );
        while ( iterator.hasNext() ) {
            OntClass element = ( OntClass ) iterator.next();
            if ( element.isAnon() ) continue;
            OntologyTerm ontologyTerm = new OntologyTermImpl( element, database );
            if ( ontologyTerm == null ) continue; // couldn't be converted for some reason.
            result.add( ontologyTerm );
            if ( ++count % 1000 == 0 ) {
                log.debug( "Loaded " + count + " terms, last was " + ontologyTerm );
            }
        }

        log.info( "Loaded " + count + " terms" );

        iterator = model.listObjectProperties();
        count = 0;
        log.info( "Reading object properties..." );
        while ( iterator.hasNext() ) {
            OntProperty element = ( OntProperty ) iterator.next();
            if ( element.isAnon() ) continue;
            OntologyProperty ontologyTerm = PropertyFactory.asProperty( element, database );
            if ( ontologyTerm == null ) continue; // couldn't be converted for some reason.
            result.add( ontologyTerm );
            if ( ++count % 1000 == 0 ) {
                log.debug( "Loaded " + count + " object properties, last was " + ontologyTerm );
            }
        }

        iterator = model.listDatatypeProperties();
        log.info( "Reading datatype properties..." );
        while ( iterator.hasNext() ) {
            OntProperty element = ( OntProperty ) iterator.next();
            if ( element.isAnon() ) continue;
            OntologyProperty ontologyTerm = PropertyFactory.asProperty( element, database );
            if ( ontologyTerm == null ) continue; // couldn't be converted for some reason.
            result.add( ontologyTerm );
            if ( ++count % 1000 == 0 ) {
                log.debug( "Loaded " + count + " datatype properties, last was " + ontologyTerm );
            }
        }

        log.info( "Loaded " + count + " properties" );

        iterator = model.listIndividuals();
        count = 0;
        log.info( "Reading individuals..." );
        while ( iterator.hasNext() ) {
            Individual element = ( Individual ) iterator.next();
            if ( element.isAnon() ) continue;
            OntologyIndividual ontologyTerm = new OntologyIndividualImpl( element, database );
            if ( ontologyTerm == null ) continue; // couldn't be converted for some reason.
            result.add( ontologyTerm );
            if ( ++count % 1000 == 0 ) {
                log.debug( "Loaded " + count + " individuals, last was " + ontologyTerm );
            }
        }
        log.info( "Loaded " + count + " individuals" );
        return result;
    }

    /**
     * @param url
     * @param force
     * @return
     */
    private static OntModel persistModelIfNecessary( String url, boolean force ) {
        log.info( "Getting model ..." );
        OntModel model = getRDBModel( url );
        if ( model.isEmpty() ) {
            log.info( url + ": New ontology, loading..." );
            model.read( url );
        } else if ( force ) {
            log.info( url + ": Reloading..." );
            model.read( url );
        } else {
            log.info( url + ": Ontology already exists in persistent store" );
        }
        return model;
    }

    public static OntModel load( String url ) {
        return loadPersistentModel( url, false );
    }

    /**
     * Get model that is entirely in memory with default OntModelSpec.OWL_MEM_RDFS_INF.
     * 
     * @param url
     * @return
     */
    static OntModel getMemoryModel( String url ) {
        return getMemoryModel( url, OntModelSpec.OWL_MEM_RDFS_INF );
    }

    /**
     * Get model that is entirely in memory.
     * 
     * @param url
     * @param specification
     * @return
     */
    static OntModel getMemoryModel( String url, OntModelSpec specification ) {
        OntModelSpec spec = new OntModelSpec( specification );
        ModelMaker maker = ModelFactory.createMemModelMaker();
        Model base = maker.createModel( url, false );
        spec.setImportModelMaker( maker );

        return ModelFactory.createOntologyModel( spec, base );
    }

    /**
     * Get model backed by persistent store. Slower.
     * 
     * @param url
     * @return
     */
    private static OntModel getRDBModel( String url ) {

        OntModelSpec spec = new OntModelSpec( OntModelSpec.OWL_DL_MEM_RDFS_INF );
        ModelMaker maker = getRDBMaker();
        spec.setImportModelMaker( maker );
        Model base;
        if ( url == null ) {
            base = maker.createDefaultModel();
        } else {
            base = maker.createModel( url, false );
        }

        return ModelFactory.createOntologyModel( spec, base );

    }

    /**
     * Added to allow loading of files
     */
    public static OntModel loadFromFile( File file, String base ) throws IOException {
        OntModel model = getRDBModel( base );
        model.read( new FileInputStream( file ), base );
        return model;
    }

}
