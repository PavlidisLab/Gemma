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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import ubic.basecode.util.FileTools;
import ubic.gemma.core.analysis.sequence.SequenceManipulation;
import ubic.gemma.core.loader.expression.arrayDesign.AffyChipTypeExtractor;
import ubic.gemma.core.loader.expression.arrayDesign.AffyProbeReader;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * Purely a testing tool to parse CEL files to extract the 'chip name', so we can gather them.
 * 
 * Should probably be in GemmaAnalysis but that is badly broken.
 * 
 * You just run this like
 * 
 * $GEMMACMD <filename>
 * 
 * It doesn't handle the regular argument setup, wasn't worth the trouble. Generates FASTA format but easy to change.
 * 
 * @author paul
 */
public class AffyChipNameExtract extends ArrayDesignSequenceManipulatingCli {
    public static void main( String[] args ) {
        AffyChipNameExtract d = new AffyChipNameExtract();
        Exception e = d.doWork( args );
        if ( e != null ) {
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.AbstractCLI#getCommandName()
     */
    @Override
    public String getCommandName() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {

        // parse
        AffyChipTypeExtractor apr = new AffyChipTypeExtractor();

        for ( int i = 0; i < args.length; i++ ) {
            String f = args[i];
            try (InputStream is = FileTools.getInputStreamFromPlainOrCompressedFile( f )) {

                String type = apr.extract( is );
                System.err.println( type + "\t" + f );
            } catch ( IOException e ) {

                e.printStackTrace();
            }

        }

        return null;
    }

}
