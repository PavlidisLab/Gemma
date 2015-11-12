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

import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceRemoveEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

/**
 * Remove all associations that this array design has with BioSequences. This is needed for cases where the original
 * import has associated the probes with the wrong sequences. A common case is for GEO data sets where the actual
 * oligonucleotide is not given. Instead the submitter provides Genbank accessions, which are misleading. This method
 * can be used to clear those until the "right" sequences can be identified and filled in. Note that this does not
 * delete the BioSequences, it just nulls the BiologicalCharacteristics of the CompositeSequences.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignBioSequenceDetachCli extends ArrayDesignSequenceManipulatingCli {

    public static void main( String[] args ) {
        ArrayDesignBioSequenceDetachCli p = new ArrayDesignBioSequenceDetachCli();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#getCommandName()
     */
    @Override
    public String getCommandName() {
        return "detachSequences";
    }

    @Override
    public String getShortDesc() {
        return "Remove all associations that a platform has with sequences, for cases where imported data had wrong associations.";
    }

    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( args );
        if ( err != null ) return err;
        for ( ArrayDesign arrayDesign : this.arrayDesignsToProcess ) {
            this.getArrayDesignService().removeBiologicalCharacteristics( arrayDesign );
            audit( arrayDesign, "Removed sequence associations with CLI" );
        }
        return null;
    }

    /**
     * @param arrayDesign
     */
    private void audit( ArrayDesign arrayDesign, String note ) {
        super.getArrayDesignReportService().generateArrayDesignReport( arrayDesign.getId() );
        AuditEventType eventType = ArrayDesignSequenceRemoveEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( arrayDesign, eventType, note );
    }

}
