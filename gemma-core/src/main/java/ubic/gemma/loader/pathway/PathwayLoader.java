/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.loader.pathway;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

//import org.biopax.paxtools.io.jena.JenaIOHandler;
//import org.biopax.paxtools.io.pathwayCommons.PathwayCommonsIOHandler;
//import org.biopax.paxtools.model.BioPAXElement;
//import org.biopax.paxtools.model.BioPAXLevel;
//import org.biopax.paxtools.model.Model;
//import org.biopax.paxtools.model.level3.Interaction;

/**
 * FIXME not implemented.
 * 
 * @author kelsey
 * @version $Id$
 */

public class PathwayLoader {

    // private Model loadedModel = null;

    PathwayLoader() {
        super();
    }

    /**
     * @param fileName location of biopax owl file to be loaded
     * @throws FileNotFoundException
     */
    PathwayLoader( String fileName ) throws FileNotFoundException {
        this();
        InputStream inputStream = new FileInputStream( new File( fileName ) );
        load( inputStream );

    }

    /**
     * The input stream should be the location of an owl file in biopax format
     * 
     * @param inputStream
     * @throws FileNotFoundException
     */
    public void load( InputStream inputStream ) throws FileNotFoundException {
        // JenaIOHandler jenaIOHandler = new JenaIOHandler(BioPAXLevel.L2);
        // loadedModel = jenaIOHandler.convertFromOWL( inputStream );

    }

    /**
     * Loads the desired pathway from pathway commons using their custom identifiers.
     * 
     * @param cPathId
     * @throws IOException
     */
    public void load( String cPathId ) throws IOException {

        // FIXME Doubt passing in null is the correct thing to do here.
        // Need to look at source as API documentation is not helpful.
        // PathwayCommonsIOHandler pcIOHandler = new PathwayCommonsIOHandler(null);
        // loadedModel = pcIOHandler.retrieveByID(cPathId);

    }

    /**
     * @return
     */
    // public Set<BioPAXElement> getAllObjectsInModel(){
    //        
    // return this.loadedModel.getObjects();
    // }

    /**
     * @return
     */
    // public Set<Interaction> getAllInteractions(){
    // 
    // return this.loadedModel.getObjects( Interaction.class );
    // }

}
