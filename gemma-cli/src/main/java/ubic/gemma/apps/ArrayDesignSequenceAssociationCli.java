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
package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignSequenceProcessingService;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceUpdateEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.InputStream;

import static ubic.gemma.cli.util.EntityOptionsUtils.addTaxonOption;

/**
 * Attach sequences to array design, fetching from BLAST database if requested.
 *
 * @author pavlidis
 */
public class ArrayDesignSequenceAssociationCli extends ArrayDesignSequenceManipulatingCli {

    @Autowired
    private ArrayDesignSequenceProcessingService arrayDesignSequenceProcessingService;
    @Autowired
    private TaxonService taxonService;

    private boolean force = false;
    private String idFile = null;
    private String sequenceFile;
    private String sequenceId = null;
    private String sequenceType;
    private String taxonName = null;

    @Override
    public String getCommandName() {
        return "addPlatformSequences";
    }

    @Override
    public String getShortDesc() {
        return "Attach sequences to array design, from a file or fetching from BLAST database.";
    }

    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );

        Option fileOption = Option.builder( "f" ).argName( "Input sequence file" ).hasArg()
                .desc( "Path to file (FASTA for cDNA or three-column format for OLIGO). If the FASTA file doesn't have " +
                        "probe identifiers included, provide identifiers via the -i option." ).longOpt( "file" ).build();

        options.addOption( fileOption );

        Option sequenceIdentifierOption = Option.builder( "i" ).argName( "Input identifier file" ).hasArg()
                .desc( "Path to file (two columns with probe ids and sequence accessions); can use in combination with -file" )
                .longOpt( "ids" ).build();

        options.addOption( sequenceIdentifierOption );

        StringBuilder buf = new StringBuilder();

        for ( SequenceType lit : SequenceType.values() ) {
            buf.append( lit.name() ).append( "\n" );
        }

        String seqtypes = buf.toString();
        seqtypes = StringUtils.chop( seqtypes );

        Option sequenceTypeOption = Option.builder( "y" ).required().argName( "Sequence type" ).hasArg()
                .desc( seqtypes ).longOpt( "type" ).build();

        options.addOption( sequenceTypeOption );

        options.addOption( Option.builder( "s" ).hasArg().argName( "accession" ).desc( "A single accession to update" )
                .longOpt( "sequence" ).build() );

        addForceOption( options, "Force overwriting of existing sequences; If biosequences are encountered that already have sequences filled in, they will be overwritten; default is to leave them." );

        addTaxonOption( options, "t", "taxon", "Taxon identifier (e.g., human) for sequences (only required if array design is 'naive')" );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        super.processOptions( commandLine );

        if ( commandLine.hasOption( 'y' ) ) {
            sequenceType = commandLine.getOptionValue( 'y' );
        }

        if ( commandLine.hasOption( 'f' ) ) {
            this.sequenceFile = commandLine.getOptionValue( 'f' );
        }

        if ( commandLine.hasOption( 's' ) ) {
            this.sequenceId = commandLine.getOptionValue( 's' );
        }

        if ( commandLine.hasOption( 't' ) ) {
            this.taxonName = commandLine.getOptionValue( 't' );
            if ( StringUtils.isBlank( this.taxonName ) ) {
                throw new IllegalArgumentException( "You must provide a taxon name when using the -t option" );
            }
        }

        if ( commandLine.hasOption( 'i' ) ) {
            this.idFile = commandLine.getOptionValue( 'i' );
        }

        if ( commandLine.hasOption( "force" ) ) {
            this.force = true;
        }

    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        // this is kind of an oddball function of this tool.
        if ( this.sequenceId != null ) {
            BioSequence updated = arrayDesignSequenceProcessingService.processSingleAccession( this.sequenceId,
                    new String[] { "nt", "est_others", "est_human", "est_mouse" }, force );
            if ( updated != null ) {
                log.info( "Updated or created " + updated );
            }
            return;
        }

        if ( getArrayDesignsToProcess().size() > 1 ) {
            throw new IllegalStateException( "Only one platform can be processed by this CLI" );
        }

        ArrayDesign arrayDesign = this.getArrayDesignsToProcess().iterator().next();

        arrayDesign = getArrayDesignService().thaw( arrayDesign );

        SequenceType sequenceTypeEn = SequenceType.valueOf( sequenceType );

        Taxon taxon;
        if ( this.taxonName != null ) {
            taxon = entityLocator.locateTaxon( this.taxonName );
        } else {
            taxon = this.getArrayDesignService().getTaxon( arrayDesign.getId() );
            // could still be null
        }

        if ( this.sequenceFile != null ) {
            try ( InputStream sequenceFileIs = FileTools
                    .getInputStreamFromPlainOrCompressedFile( sequenceFile ) ) {

                if ( sequenceFileIs == null ) {
                    throw new IllegalArgumentException( "No file " + sequenceFile + " was readable" );
                }

                log.info( "Processing ArrayDesign..." );

                if ( idFile != null ) {
                    try ( InputStream idFileIs = FileTools
                            .getInputStreamFromPlainOrCompressedFile( idFile ) ) {

                        arrayDesignSequenceProcessingService
                                .processArrayDesign( arrayDesign, sequenceFileIs, idFileIs, sequenceTypeEn, taxon );


                    }
                } else {
                    // sequence file has to have a way to identify the probes they go with
                    arrayDesignSequenceProcessingService
                            .processArrayDesign( arrayDesign, sequenceFileIs, sequenceTypeEn, taxon );
                }

                this.audit( arrayDesign, "Sequences read from file: " + sequenceFile );
            }
        } else if ( this.idFile != null ) {
            try ( InputStream idFileIs = FileTools.getInputStreamFromPlainOrCompressedFile( idFile ) ) {

                if ( idFileIs == null ) {
                    throw new IllegalArgumentException( "No file " + idFile + " was readable" );
                }

                log.info( "Processing ArrayDesign..." );

                String[] databases = chooseBLASTdbs( taxon );

                arrayDesignSequenceProcessingService.processArrayDesign( arrayDesign, idFileIs,
                        databases, taxon, force );

                this.audit( arrayDesign, "Sequences identifiers from file: " + idFile );
            }
        } else {
            log.info( "Retrieving sequences from BLAST databases" );

            String[] databases = chooseBLASTdbs( taxon );

            arrayDesignSequenceProcessingService.processArrayDesign( arrayDesign,
                    databases, force );
            this.audit( arrayDesign, "Sequence looked up from BLAST databases" );
        }
    }

    /**
     */
    private String[] chooseBLASTdbs( Taxon taxon ) {
        String[] databases;

        if ( taxon != null && taxon.getCommonName().equals( "mouse" ) ) {
            databases = new String[] { "est_mouse", "nt" };
        } else if ( taxon != null && taxon.getCommonName().equals( "human" ) ) {
            databases = new String[] { "est_human", "nt" };
        } else {
            databases = new String[] { "nt", "est_others", "est_human", "est_mouse" };
        }
        return databases;
    }

    private void audit( ArrayDesign arrayDesign, String note ) {
        // minor : don't add audit event if no sequences were changed, or --force.
        this.getArrayDesignReportService().generateArrayDesignReport( arrayDesign.getId() );
        auditTrailService.addUpdateEvent( arrayDesign, ArrayDesignSequenceUpdateEvent.class, note );
    }
}
