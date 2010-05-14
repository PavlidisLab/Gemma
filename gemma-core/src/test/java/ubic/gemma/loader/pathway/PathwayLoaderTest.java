/*
 * The Gemma project Copyright (c) 2009 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */


package ubic.gemma.loader.pathway;

import java.io.FileNotFoundException;
import java.io.InputStream;

import junit.framework.TestCase;

import org.jfree.util.Log;
import org.junit.Test;

/**Tests for testing the pathway loader. 
 * @author kelsey
 * @version $ID
 */

public class PathwayLoaderTest extends TestCase{

    
    @Test
    public void testLoad(){
        PathwayLoader loader = new PathwayLoader();
        InputStream is = this.getClass().getResourceAsStream( "/data/loader/pathway/mammalia.owl" );
        assert is != null;
        
        try {
            loader.load( is );
        }catch(FileNotFoundException fnf){
            Log.error( fnf );
            
        }
        
//        Collection<BioPAXElement> objs = loader.getAllObjectsInModel();
//        
//        Collection<Interaction> interactions = loader.getAllInteractions();
        
        
    }
}
