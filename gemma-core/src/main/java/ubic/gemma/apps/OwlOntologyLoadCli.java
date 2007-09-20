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
package ubic.gemma.apps;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.ontology.OntologyLoader;
import ubic.gemma.util.AbstractCLI;

/**
 * Load an Ontology into the persistent store. Simple tool to initialize OWL ontologies locally.
 * 
 * @author Paul
 * @version $Id$
 */
public class OwlOntologyLoadCli extends AbstractCLI {

    public static void main( String[] args ) {
        OwlOntologyLoadCli p = new OwlOntologyLoadCli();
        Exception e = p.doWork( args );
        if ( e != null ) log.error( e, e );
    }

    private String url;
    private File file;
    private boolean force;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        addOption( OptionBuilder.isRequired().hasArg().withArgName( "url" ).withDescription( "Base URL for the OWL file" )
                .withLongOpt( "url" ).create( "o" ) );

        addOption( OptionBuilder.withDescription( "Force reloading of Ontology in Database" ).withLongOpt( "force" )
                .create( "f" ) );

        addOption( OptionBuilder.withDescription( "Load from file" ).withLongOpt( "file" ).create( "l" ) );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception exception = processCommandLine( "Load OWL ontology", args );

        if ( exception != null ) return exception;

        try {
            if ( file != null ) {
                OntologyLoader.loadFromFile( file, url );
            } else {
                OntologyLoader.loadPersistentModel( url, force );
            }

            // log.info( "Loaded " + values.size() + " terms" );

            // if ( log.isDebugEnabled() ) {
            // for ( OntologyResource term : values ) {
            // log.debug( term );
            //
            // if ( !( term instanceof OntologyTerm ) ) continue;
            //
            // Collection<OntologyTerm> superClasses = ( ( OntologyTerm ) term ).getParents( true );
            // for ( OntologyTerm o : superClasses ) {
            // log.debug( " isa: " + o );
            // }
            //
            // Collection<AnnotationProperty> annotations = ( ( OntologyTerm ) term ).getAnnotations();
            //
            // for ( AnnotationProperty o : annotations ) {
            // log.debug( " Annot: " + o );
            // }
            //
            // }
            // }
        } catch ( IOException e ) {
            return e;
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#processOptions()
     */
    @Override
    protected void processOptions() {
        this.url = this.getOptionValue( 'o' );
        this.force = this.hasOption( 'f' );
        
        if (this.getOptionValue( 'l' ) != null)
            this.file = new File( this.getOptionValue( 'l' ) );
    }

}
