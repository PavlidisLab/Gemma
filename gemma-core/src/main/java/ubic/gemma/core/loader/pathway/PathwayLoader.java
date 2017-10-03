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
package ubic.gemma.core.loader.pathway;

import java.io.*;

/**
 * FIXME not implemented.
 *
 * @author kelsey
 */

public class PathwayLoader {

    // private Model loadedModel = null;

    PathwayLoader() {
        super();
    }

    PathwayLoader( String fileName ) throws IOException {
        this();
        try (InputStream inputStream = new FileInputStream( new File( fileName ) );) {
            load( inputStream );
        }

    }

    /**
     * The input stream should be the location of an owl file in biopax format
     *
     * @param inputStream inpu stream
     * @throws FileNotFoundException file not found
     */
    public void load( InputStream inputStream ) throws FileNotFoundException {
        // JenaIOHandler jenaIOHandler = new JenaIOHandler(BioPAXLevel.L2);
        // loadedModel = jenaIOHandler.convertFromOWL( inputStream );

    }

    /**
     * Loads the desired pathway from pathway commons using their custom identifiers.
     *
     * @param cPathId path id
     * @throws IOException io problems
     */
    public void load( String cPathId ) throws IOException {

        // FIXME Doubt passing in null is the correct thing to do here.
        // Need to look at source as API documentation is not helpful.
        // PathwayCommonsIOHandler pcIOHandler = new PathwayCommonsIOHandler(null);
        // loadedModel = pcIOHandler.retrieveByID(cPathId);

    }

    // public Set<BioPAXElement> getAllObjectsInModel(){
    //
    // return this.loadedModel.getObjects();
    // }

    // public Set<Interaction> getAllInteractions(){
    //
    // return this.loadedModel.getObjects( Interaction.class );
    // }

}
