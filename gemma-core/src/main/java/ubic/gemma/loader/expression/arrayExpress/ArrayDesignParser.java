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
package ubic.gemma.loader.expression.arrayExpress;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.loader.util.parser.BasicLineParser;
import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * Parses the flat files from ArrayExpress.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignParser extends BasicLineParser {

    Collection<CompositeSequence> results = new HashSet<CompositeSequence>();

    @Override
    protected void addResult( Object obj ) {
        results.add( ( CompositeSequence ) obj );
    }

    @Override
    public Collection<CompositeSequence> getResults() {
        return results;
    }

    public Object parseOneLine( String line ) {
        String[] fields = StringUtils.splitPreserveAllTokens( line, '\t' );
        if ( fields.length < 2 ) return null;
        CompositeSequence cs = CompositeSequence.Factory.newInstance();
        cs.setName( fields[1] );
        cs.setDescription( fields[0] );
        return cs;
    }

}
