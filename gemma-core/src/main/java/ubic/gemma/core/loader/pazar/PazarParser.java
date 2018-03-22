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
package ubic.gemma.core.loader.pazar;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.loader.pazar.model.PazarRecord;
import ubic.gemma.core.loader.util.parser.BasicLineParser;

import java.util.Collection;
import java.util.HashSet;

/**
 * ssTODO Document Me
 *
 * @author paul
 */
public class PazarParser extends BasicLineParser<PazarRecord> {

    private final Collection<PazarRecord> results = new HashSet<>();

    @Override
    public Collection<PazarRecord> getResults() {
        return results;
    }

    @Override
    protected void addResult( PazarRecord obj ) {
        results.add( obj );
    }

    @Override
    public PazarRecord parseOneLine( String line ) {
        if ( line == null || line.isEmpty() )
            return null;

        if ( line.startsWith( "TF_PAZAR_ID" ) )
            return null;

        String[] fields = StringUtils.splitPreserveAllTokens( line, '\t' );
        if ( fields.length < 2 )
            return null;

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
