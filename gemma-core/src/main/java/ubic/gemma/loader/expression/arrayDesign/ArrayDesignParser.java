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
package ubic.gemma.loader.expression.arrayDesign;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.loader.util.parser.BasicLineParser;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;

/**
 * Parse ArrayDesigns from a flat file. This is used to seed the system from our legacy data. (probably not used)
 * <p>
 * Format:
 * <ol>
 * <li>Murine Genome U74A Array --- platform name
 * <li>Affymetrix -- Manufacturer name
 * <li>MG-U74A --- short name
 * <li>MOUSE --- taxon
 * <li>10044 --- advertised number of design elements
 * <li>(Masked) Affymetrix GeneChip expression probe array... --- Description
 * </ol>
 * 
 * @author keshav
 * @version $Id$
 * @deprecated
 */
@Deprecated
public class ArrayDesignParser extends BasicLineParser<ArrayDesign> {

    private Collection<ArrayDesign> results = new HashSet<ArrayDesign>();

    public ArrayDesign parseOneLine( String line ) {
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        String[] fields = StringUtils.splitPreserveAllTokens( line, '\t' );
        ad.setName( fields[0] );
        ad.setDescription( fields[5] );

        Taxon t = Taxon.Factory.newInstance();
        t.setCommonName( fields[4].toLowerCase() );
        t.setIsSpecies( true ); // assumption.
        t.setIsGenesUsable( true ); // assumption
        ad.setPrimaryTaxon( t );

        Contact manufacturer = Contact.Factory.newInstance();
        manufacturer.setName( fields[1] );
        ad.setDesignProvider( manufacturer );

        ad.setAdvertisedNumberOfDesignElements( Integer.parseInt( fields[4] ) );
        return ad;
    }

    @Override
    protected void addResult( ArrayDesign obj ) {
        results.add( obj );

    }

    @Override
    public Collection<ArrayDesign> getResults() {
        return results;
    }

}
