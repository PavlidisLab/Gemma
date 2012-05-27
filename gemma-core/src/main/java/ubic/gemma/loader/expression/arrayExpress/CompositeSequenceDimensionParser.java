/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.biomage.DesignElement.CompositeSequence;

import ubic.gemma.loader.util.parser.BasicOrderedLineParser;

/**
 * Parse composite sequence from ArrayExpress. These are line-based records.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class CompositeSequenceDimensionParser extends BasicOrderedLineParser<CompositeSequence> {

    List<CompositeSequence> results = new ArrayList<CompositeSequence>();

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.parser.BasicLineParser#addResult(java.lang.Object)
     */
    @Override
    protected void addResult( CompositeSequence obj ) {
        results.add( obj );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.parser.BasicLineParser#getResults()
     */
     @Override
    public Collection<CompositeSequence> getResults() {
        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.parser.LineParser#parseOneLine(java.lang.String)
     */
    @Override
    public CompositeSequence parseOneLine( String line ) {

        String[] toks = StringUtils.splitPreserveAllTokens( line );

        if ( toks.length != 2 ) {
            throw new IllegalArgumentException();
        }

        CompositeSequence cs = new CompositeSequence();

        cs.setIdentifier( toks[0] );
        cs.setName( toks[1] );

        return cs;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.parser.BasicOrderedLineParser#getOrderedResults()
     */
    @Override
    public List<CompositeSequence> getOrderedResults() {
        return results;
    }

}
