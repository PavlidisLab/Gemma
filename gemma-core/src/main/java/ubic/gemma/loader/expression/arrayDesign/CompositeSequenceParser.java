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
import ubic.gemma.loader.util.parser.ExternalDatabaseUtils;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * Parse the "old" array description format. This has three columns, with probe id, a genbank id, and a description.
 * <p>
 * Note that this does not set the ArrayDesign for the CompositeSequences, this must be set by the caller.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class CompositeSequenceParser extends BasicLineParser<CompositeSequence> {

    private Collection<CompositeSequence> results = new HashSet<CompositeSequence>();

    private Taxon taxon;

    @Override
    protected void addResult( CompositeSequence obj ) {
        results.add( obj );
    }

    @Override
    public Collection<CompositeSequence> getResults() {
        return results;
    }

    @Override
    public CompositeSequence parseOneLine( String line ) {
        String[] tokens = StringUtils.splitPreserveAllTokens( line, '\t' );

        if ( tokens.length != 3 ) {
            return null;
        }

        String probeid = tokens[0];
        String genbankAcc = tokens[1];
        String description = tokens[2];

        CompositeSequence result = CompositeSequence.Factory.newInstance();
        result.setName( probeid );
        result.setDescription( description );

        DatabaseEntry dbEntry = ExternalDatabaseUtils.getGenbankAccession( genbankAcc );

        BioSequence biologicalCharacteristic = BioSequence.Factory.newInstance();
        biologicalCharacteristic.setName( genbankAcc ); // this will be changed later, typically.
        biologicalCharacteristic.setTaxon( taxon );

        // this will be changed later, typically.
        biologicalCharacteristic.setDescription( description + " (From platform source)" );

        biologicalCharacteristic.setSequenceDatabaseEntry( dbEntry );

        result.setBiologicalCharacteristic( biologicalCharacteristic );

        return result;

    }

    public Taxon getTaxon() {
        return taxon;
    }

    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

}
