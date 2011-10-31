/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.loader.pazar;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.loader.pazar.model.PazarRecord;
import ubic.gemma.loader.util.parser.BasicLineParser;

/**
 * ssTODO Document Me
 * 
 * @author paul
 * @version $Id$
 */
public class PazarParser extends BasicLineParser<PazarRecord> {

    Collection<PazarRecord> results = new HashSet<PazarRecord>();

    @Override
    protected void addResult( PazarRecord obj ) {
        results.add( obj );
    }

    @Override
    public Collection<PazarRecord> getResults() {
        return results;
    }

    @Override
    public PazarRecord parseOneLine( String line ) {
        if ( line == null || line.isEmpty() ) return null;

        if ( line.startsWith( "TF_PAZAR_ID" ) ) return null;

        String[] fields = StringUtils.splitPreserveAllTokens( line, '\t' );
        if ( fields.length < 2 ) return null;

        PazarRecord r = new PazarRecord();

        r.setPazarTfId( StringUtils.strip( fields[0] ) );
        r.setTfAcc( fields[1] );
        r.setSpecies( fields[2] );
        r.setPazarTargetGeneId( fields[3] );
        r.setTargetGeneAcc( fields[4] );
        r.setProject( fields[6] );
        r.setPubMedId( fields[7] );
        // r.setMethod(fields[8);

        return r;

    }

}
