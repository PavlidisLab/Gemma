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
package ubic.gemma.loader.genome;

import java.util.Collection;

import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * Interface representing a class that can retrieve sequences from Blast databases. (In later versions of BLAST this is
 * known as Blastdbcmd)
 * 
 * @author pavlidis
 * @version $Id$
 */
public interface FastaCmd {

    public BioSequence getByAccession( String accession, String database );

    public BioSequence getByIdentifier( int identifier, String database );

    public Collection<BioSequence> getBatchAccessions( Collection<String> accessions, String database );

    public Collection<BioSequence> getBatchIdentifiers( Collection<Integer> identifiers, String database );

    public BioSequence getByAccession( String accession, String database, String blastHome );

    public BioSequence getByIdentifier( int identifier, String database, String blastHome );

    public Collection<BioSequence> getBatchAccessions( Collection<String> accessions, String database, String blastHome );

    public Collection<BioSequence> getBatchIdentifiers( Collection<Integer> identifiers, String database,
            String blastHome );

}
