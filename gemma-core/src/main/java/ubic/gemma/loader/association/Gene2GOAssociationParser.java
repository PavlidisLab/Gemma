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
package ubic.gemma.loader.association;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.util.ParserAndLoaderTools;
import ubic.gemma.loader.util.parser.BasicLineParser;
import ubic.gemma.model.association.Gene2GOAssociation;

/**
 * FIXME - document what this is for.
 * 
 * @author keshav
 * @author pavlidis
 * @spring.bean id="gene2GOAssociationParser"
 * @version $Id$
 */
public class Gene2GOAssociationParser extends BasicLineParser {
    protected static final Log log = LogFactory.getLog( Gene2GOAssociationParser.class );

    private Gene2GOAssociationMappings gene2GOAssociationMappings = null;

    private Collection<Gene2GOAssociation> results = new HashSet<Gene2GOAssociation>();

    int i = 0;

    public Gene2GOAssociationParser() {
        try {
            gene2GOAssociationMappings = new Gene2GOAssociationMappings();
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param url
     * @return Map
     * @throws IOException
     * @throws ConfigurationException
     */
    public void parseFromHttp( String url ) throws IOException {

        InputStream is = ParserAndLoaderTools.retrieveByHTTP( url );

        GZIPInputStream gZipInputStream = new GZIPInputStream( is );

        this.parse( gZipInputStream );
    }

    public Object parseOneLine( String line ) {
        assert gene2GOAssociationMappings != null;
        Gene2GOAssociation g2GO = null;

        Object obj = this.gene2GOAssociationMappings.mapFromGene2GO( line );
        if ( obj == null ) {
            return obj;
        }
        g2GO = ( Gene2GOAssociation ) obj;
        return g2GO;

    }

    @Override
    protected void addResult( Object obj ) {
        results.add( ( Gene2GOAssociation ) obj );

    }

    @Override
    public Collection<Gene2GOAssociation> getResults() {
        return results;
    }

}
