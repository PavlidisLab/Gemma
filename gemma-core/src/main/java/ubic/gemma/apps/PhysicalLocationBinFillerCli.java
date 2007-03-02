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
package ubic.gemma.apps;

import java.util.Collection;

import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.PhysicalLocationDao;
import ubic.gemma.util.AbstractSpringAwareCLI;
import ubic.gemma.util.SequenceBinUtils;

/**
 * This is a one-off.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class PhysicalLocationBinFillerCli extends AbstractSpringAwareCLI {

    @Override
    protected void buildOptions() {
        //
    }

    public static void main( String[] args ) {
        PhysicalLocationBinFillerCli p = new PhysicalLocationBinFillerCli();
        p.doWork( args );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        processCommandLine( "physicalLocationBinFiller", args );

        PhysicalLocationDao pld = ( PhysicalLocationDao ) this.getBean( "physicalLocationDao" );

        Collection<PhysicalLocation> lcs = pld.loadAll();
        int count = 0;
        for ( PhysicalLocation location : lcs ) {
            if ( location.getNucleotide() == null || location.getNucleotideLength() == null ) continue;
            int bin = SequenceBinUtils.binFromRange( location.getNucleotide().intValue(), location.getNucleotide()
                    .intValue()
                    + location.getNucleotideLength().intValue() );
            location.setBin( bin );
            pld.update( location );
            if ( ++count % 10000 == 0 ) {
                log.info( "Processed " + count );
            }
        }

        return null;
    }
}
