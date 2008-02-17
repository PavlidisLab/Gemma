/**
 * 
 */
package ubic.gemma.ontology;

/***********************************************************************************************************************
 * Originally based on: Source code information ----------------------- Original author Ian Dickinson, HP Labs Bristol
 * Author email ian.dickinson@hp.com Package Jena 2 Web http://sourceforge.net/projects/jena/ Created 25-Jul-2003
 * Filename $RCSfile$ Revision $Revision$ Release status $State$ Last modified
 * on $Date$ by $Author$ (c) Copyright 2002, 2003, 2004, 2005, 2006, 2007 Hewlett-Packard
 * Development Company, LP (see footer for full conditions)
 **********************************************************************************************************************/

import org.apache.commons.lang.StringUtils;

import com.hp.hpl.jena.db.DBConnection;
import com.hp.hpl.jena.db.IDBConnection;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;

/**
 * @author Ian Dickinson, HP Labs (<a href="mailto:Ian.Dickinson@hp.com" >email</a>)
 * @version CVS $Id$
 */
public class PersistentOntology {

    /**
     * @param maker
     * @param source
     */
    public OntModel loadDB( ModelMaker maker, String source ) {
        // use the model maker to get the base model as a persistent model
        // strict=false, so we get an existing model by that name if it exists
        // or create a new one
        Model base = maker.createModel( source, false );

        // now we plug that base model into an ontology model that also uses
        // the given model maker to create storage for imported models
        OntModel m = ModelFactory.createOntologyModel( getModelSpec( maker ), base );

        // now load the source document, which will also load any imports
        m.read( source );

        return m;
    }

    /**
     * @param dbURL
     * @param dbUser
     * @param dbPw
     * @param dbType e.g. 'mysql'
     * @param cleanDB
     * @return
     */
    public ModelMaker getRDBMaker( String dbURL, String dbUser, String dbPw, String dbType, boolean clean ) {
        if ( StringUtils.isBlank( dbURL ) ) throw new IllegalArgumentException( "Database URL must be provided" );
        try {
            // Create database connection
            IDBConnection conn = new DBConnection( dbURL, dbUser, dbPw, dbType );

            if ( clean ) {
                conn.cleanDB();
            }

            // Create a model maker object
            return ModelFactory.createModelRDBMaker( conn );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * @param maker
     * @return
     */
    public OntModelSpec getModelSpec( ModelMaker maker ) {
        OntModelSpec spec = new OntModelSpec( OntModelSpec.OWL_MEM_RDFS_INF );
        spec.setImportModelMaker( maker );
        return spec;
    }

}
