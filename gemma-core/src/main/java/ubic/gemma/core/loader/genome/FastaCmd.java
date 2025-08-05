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
package ubic.gemma.core.loader.genome;

import ubic.gemma.model.genome.biosequence.BioSequence;

import java.util.Collection;

/**
 * Interface representing a class that can retrieve sequences from Blast databases. (In later versions of BLAST this is
 * known as Blastdbcmd)
 *
 * @author pavlidis
 */
@SuppressWarnings("unused") // Possible external use
public interface FastaCmd {

    BioSequence getByAccession( String accession, String database );

    BioSequence getByIdentifier( int identifier, String database );

    Collection<BioSequence> getBatchAccessions( Collection<String> accessions, String database );

    Collection<BioSequence> getBatchIdentifiers( Collection<Integer> identifiers, String database );
}
