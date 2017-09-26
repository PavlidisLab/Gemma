/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.web.view;

import com.sdicons.json.mapper.JSONMapper;
import com.sdicons.json.mapper.MapperException;
import com.sdicons.json.model.JSONValue;

import java.util.List;

/**
 * @author pavlidis
 */
public class JSONTableRenderer {

    public String render( List<Object> tableObjects ) {
        try {
            StringBuilder b = new StringBuilder();
            for ( Object o : tableObjects ) {
                JSONValue v;
                v = JSONMapper.toJSON( o );
                b.append( v.render( true ) );
            }
            return b.toString();
        } catch ( MapperException e ) {
            throw new RuntimeException( e );
        }
    }

}
