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
package ubic.gemma.core.apps;

import org.apache.commons.cli.Option;
import org.apache.commons.lang3.StringUtils;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignSequenceProcessingService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceUpdateEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.InputStream;

/**
 * Attach sequences to array design, fetching from BLAST database if requested.
 *
 * @author pavlidis
 */
public class ArrayDesignSequenceAssociationCli extends ArrayDesignSequenceManipulatingCli {

    private ArrayDesignSequenceProcessingService arrayDesignSequenceProcessingService;
    private boolean force = false;
    private String idFile = null;
    private String sequenceFile;
    private String sequenceId = null;
    private String sequenceType;
    private String taxonName = null;
    private TaxonService taxonService;

    public static void main( String[] args ) {
        ArrayDesignSequenceAssociationCli p = new ArrayDesignSequenceAssociationCli();
        executeCommand( p, args );
    }

    @Override
    public String getCommandName() {
        return "addPlatformSequences";
    }

    @Override
    protected void doWork() throws Exception {
        // this is kind of an oddball function of this tool.
        if ( this.hasOption( 's' ) ) {
            BioSequence updated = arrayDesignSequenceProcessingService.processSingleAccession( this.sequenceId,
                    new String[]{"nt", "est_others", "est_human", "est_mouse"}, null, force );
            if ( updated != null ) {
                AbstractCLI.log.info( "Updated or created " + updated );
            }
            return;
        }

        if ( getArrayDesignsToProcess().size() > 1 ) {
            throw new IllegalStateException( "Only one platform can be processed by this CLI" );
        }

        ArrayDesign arrayDesign = this.getArrayDesignsToProcess().iterator().next();

        arrayDesign = this.thaw( arrayDesign );

        SequenceType sequenceTypeEn = SequenceType.fromString( sequenceType );

        if ( sequenceTypeEn == null ) {
            throw new IllegalArgumentException( "No sequenceType " + sequenceType + " found" );

        }

        Taxon taxon = null;
        if ( this.hasOption( 't' ) ) {
            assert StringUtils.isNotBlank( this.taxonName );
            taxon = taxonService.findByCommonName( this.taxonName );
            if ( taxon == null ) {
                throw new IllegalArgumentException( "No taxon named " + taxonName );
            }
        }

        if ( this.hasOption( 'f' ) ) {
            try ( InputStream sequenceFileIs = FileTools
                    .getInputStreamFromPlainOrCompressedFile( sequenceFile ) ) {

                if ( sequenceFileIs == null ) {
                    throw new IllegalArgumentException( "No file " + sequenceFile + " was readable" );
                }

                AbstractCLI.log.info( "Processing ArrayDesign..." );

                arrayDesignSequenceProcessingService
                        .processArrayDesign( arrayDesign, sequenceFileIs, sequenceTypeEn, taxon );

                this.audit( arrayDesign, "Sequences read from file: " + sequenceFile );
            }
        } else if ( this.hasOption( 'i' ) ) {
            try ( InputStream idFileIs = FileTools.getInputStreamFromPlainOrCompressedFile( idFile ) ) {

                if ( idFileIs == null ) {
                    throw new IllegalArgumentException( "No file " + idFile + " was readable" );
                }

                AbstractCLI.log.info( "Processing ArrayDesign..." );

                arrayDesignSequenceProcessingService.processArrayDesign( arrayDesign, idFileIs,
                        new String[]{"nt", "est_others", "est_human", "est_mouse"}, null, taxon, force );

                this.audit( arrayDesign, "Sequences identifiers from file: " + idFile );
            }
        } else {
            AbstractCLI.log.info( "Retrieving sequences from BLAST databases" );
            arrayDesignSequenceProcessingService.processArrayDesign( arrayDesign,
                    new String[]{"nt", "est_others", "est_human", "est_mouse"}, null, force );
            this.audit( arrayDesign, "Sequence looked up from BLAST databases" );
        }
    }

    @Override
    public String getShortDesc() {
        return "Attach sequences to array design, from a file or fetching from BLAST database.";
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();

        Option fileOption = Option.builder( "f" ).argName( "Input sequence file" ).hasArg()
                .desc( "Path to file (FASTA)" ).longOpt( "file" ).build();

        this.addOption( fileOption );

        Option sequenceIdentifierOption = Option.builder( "i" ).argName( "Input identifier file" ).hasArg()
                .desc( "Path to file (two columns with probe ids and sequence accessions)" )
                .longOpt( "ids" ).build();

        this.addOption( sequenceIdentifierOption );

        StringBuilder buf = new StringBuilder();

        for ( String lit : SequenceType.literals() ) {
            buf.append( lit ).append( "\n" );
        }

        String seqtypes = buf.toString();
        seqtypes = StringUtils.chop( seqtypes );

        Option sequenceTypeOption = Option.builder( "y" ).required().argName( "Sequence type" ).hasArg()
                .desc( seqtypes ).longOpt( "type" ).build();

        this.addOption( sequenceTypeOption );

        this.addOption(
                Option.builder( "s" ).hasArg().argName( "accession" ).desc( "A single accession to update" )
                        .longOpt( "sequence" ).build() );

        Option forceOption = Option.builder( "force" )
                .desc(
                        "Force overwriting of existing sequences; If biosequences are encountered that already have sequences filled in, "
                                + "they will be overwritten; default is to leave them." )
                .build();

        this.addOption( forceOption );

        Option taxonOption = Option.builder( "t" ).hasArg().argName( "taxon" ).desc(
                "Taxon common name (e.g., human) for sequences (only required if array design is 'naive')" )
                .build();

        this.addOption( taxonOption );

    }

    @Override
    protected void processOptions() {
        super.processOptions();
        arrayDesignSequenceProcessingService = this.getBean( ArrayDesignSequenceProcessingService.class );
        this.taxonService = this.getBean( TaxonService.class );

        if ( this.hasOption( 'y' ) ) {
            sequenceType = this.getOptionValue( 'y' );
        }

        if ( this.hasOption( 'f' ) ) {
            this.sequenceFile = this.getOptionValue( 'f' );
        }

        if ( this.hasOption( 's' ) ) {
            this.sequenceId = this.getOptionValue( 's' );
        }

        if ( this.hasOption( 't' ) ) {
            this.taxonName = this.getOptionValue( 't' );
            if ( StringUtils.isBlank( this.taxonName ) ) {
                throw new IllegalArgumentException( "You must provide a taxon name when using the -t option" );
            }
        }

        if ( this.hasOption( 'i' ) ) {
            this.idFile = this.getOptionValue( 'i' );
        }

        if ( this.hasOption( "force" ) ) {
            this.force = true;
        }

    }

    private void audit( ArrayDesign arrayDesign, String note ) {
        // minor : don't add audit event if no sequences were changed, or --force.
        this.getArrayDesignReportService().generateArrayDesignReport( arrayDesign.getId() );
        AuditEventType eventType = ArrayDesignSequenceUpdateEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( arrayDesign, eventType, note );
    }

}
