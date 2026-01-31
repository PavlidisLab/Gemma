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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceRemoveEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * Remove all associations that this array design has with BioSequences. This is needed for cases where the original
 * import has associated the probes with the wrong sequences. A common case is for GEO data sets where the actual
 * oligonucleotide is not given. Instead the submitter provides GenBank accessions, which are misleading. This method
 * can be used to clear those until the "right" sequences can be identified and filled in. Note that this does not
 * remove the BioSequences, it just nulls the BiologicalCharacteristics of the CompositeSequences.
 *
 * @author pavlidis
 */
public class ArrayDesignBioSequenceDetachCli extends ArrayDesignSequenceManipulatingCli {

    @Autowired
    private BioSequenceService bioSequenceService;

    private boolean delete;

    @Override
    public String getCommandName() {
        return "detachSequences";
    }

    @Override
    public String getShortDesc() {
        return "Remove all associations that a platform has with sequences, for cases where imported data had wrong associations. "
                + "Also can be used to delete sequences associated with a platform (use very carefully as sequences can be shared by platforms)";
    }

    @Override
    protected void buildArrayDesignOptions( Options options ) {
        options.addOption( Option.builder( "delete" )
                .desc( "Delete sequences instead of detaching them - use with care" )
                .get() );
    }

    @Override
    protected void processArrayDesignOptions( CommandLine commandLine ) throws ParseException {
        this.delete = commandLine.hasOption( "delete" );
    }

    @Override
    protected void processArrayDesigns( Collection<ArrayDesign> arrayDesigns ) {
        if ( arrayDesigns.isEmpty() ) {
            throw new IllegalArgumentException( "You must provide at least one platform to process" );
        }

        for ( ArrayDesign arrayDesign : arrayDesigns ) {
            try {
                if ( this.delete ) {
                    log.info( "Detaching and deleting sequences for " + arrayDesign );
                    Map<CompositeSequence, BioSequence> bioSequences = arrayDesignService.getBioSequences( arrayDesign );
                    arrayDesignService.removeBiologicalCharacteristics( arrayDesign );
                    Collection<BioSequence> seqs = new HashSet<>( bioSequences.values() );
                    while ( seqs.remove( null ) ) {
                        //no-op
                    }
                    bioSequenceService.remove( seqs );
                    this.audit( arrayDesign, "Deleted " + bioSequences.size() + " associated sequences from the system" );
                    this.addSuccessObject( arrayDesign, "Sequences detached and deleted" );

                } else {
                    log.info( "Detaching sequences for " + arrayDesign );

                    arrayDesignService.removeBiologicalCharacteristics( arrayDesign );
                    this.audit( arrayDesign, "Removed sequence associations with CLI" );
                    this.addSuccessObject( arrayDesign, "Sequences detached" );
                }
                arrayDesignReportService.generateArrayDesignReport( arrayDesign.getId() );
            } catch ( Exception e ) {
                log.info( "Failure for " + arrayDesign + " " + e.getMessage() );
                this.addErrorObject( arrayDesign, e.getMessage() );
            }
        }


    }

    private void audit( ArrayDesign arrayDesign, String message ) {
        arrayDesignReportService.generateArrayDesignReport( arrayDesign.getId() );
        auditTrailService.addUpdateEvent( arrayDesign, ArrayDesignSequenceRemoveEvent.class, message );
    }

}
