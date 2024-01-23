/*
 * The gemma-core project
 *
 * Copyright (c) 2018 University of British Columbia
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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import ubic.gemma.core.analysis.sequence.SequenceManipulation;
import ubic.gemma.core.loader.expression.arrayDesign.AffyProbeReader;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.biosequence.BioSequence;

import java.io.IOException;
import java.util.Collection;

/**
 * Purely a testing tool, to turn Affy individual probes (by probeset) into collapsed sequences. This is what happens
 * internally when we add sequences to a platform, but this makes it possible to see (and check) the sequences first
 * before committing to them.
 * <p>
 * Should probably be in GemmaAnalysis but that is badly broken.
 * <p>
 * You just run this like
 * <p>
 * $GEMMACMD affyCollapse [filename]
 * <p>
 * It doesn't handle the regular argument setup, wasn't worth the trouble. Generates FASTA format but easy to change.
 *
 * @author paul
 */
public class AffyProbeCollapseCli extends ArrayDesignSequenceManipulatingCli {

    private String affyProbeFileName;

    /*
     * (non-Javadoc)
     *
     * @see ubic.gemma.core.util.AbstractCLI#getCommandName()
     */
    @Override
    public String getCommandName() {
        return "affyCollapse";
    }

    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );
        options.addOption( Option.builder( "affyProbeFile" )
                .hasArg()
                .desc( "Affymetrix probe file to use as input" )
                .required().build() );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        super.processOptions( commandLine );
        affyProbeFileName = commandLine.getOptionValue( "affyProbeFile" );
    }

    /*
     * (non-Javadoc)
     *
     * @see ubic.gemma.core.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected void doWork() throws IOException {

        // parse
        AffyProbeReader apr = new AffyProbeReader();
        apr.parse( affyProbeFileName );
        Collection<CompositeSequence> compositeSequencesFromProbes = apr.getKeySet();

        for ( CompositeSequence newCompositeSequence : compositeSequencesFromProbes ) {

            BioSequence collapsed = SequenceManipulation.collapse( apr.get( newCompositeSequence ) );
            String sequenceName = newCompositeSequence.getName() + "_collapsed";
            System.out.println( ">" + newCompositeSequence.getName() + "\t" + sequenceName + "\n" + collapsed.getSequence() + "\n" );
        }
    }

}
