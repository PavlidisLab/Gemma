package edu.columbia.gemma.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Iterator;

import edu.columbia.gemma.expression.designElement.CompositeSequence;
import edu.columbia.gemma.loader.arraydesign.AffyProbeReader;
import edu.columbia.gemma.genome.biosequence.BioSequence;

/**
 * Given an Affymetrix array design, "collapse" the probes into sequences that include all probe sequence.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AffyProbeCollapser {

    public AffyProbeCollapser() {
    }

    public void collapse( String arrayName, InputStream is, Writer writer ) throws IOException {

        AffyProbeReader apr = new AffyProbeReader();

        apr.parse( is );

        for ( Iterator iter = apr.iterator(); iter.hasNext(); ) {
            String probeSetname = ( String ) iter.next();
            CompositeSequence apset = ( CompositeSequence ) apr.get( probeSetname );

            BioSequence m = SequenceManipulation.collapse( apset );
            writer.write( ">target:" + arrayName + ":" + probeSetname + ";\n" + m.getSequence() + "\n" );
        }
    }

    public static void main( String[] args ) throws IOException {
        String arrayName = args[0];
        String filename = args[1];
        File f = new File( filename );
        if ( !f.canRead() ) throw new IOException();

        String outputFileName = args[2];
        File o = new File( outputFileName );
        // if ( !o.canWrite() ) throw new IOException( "Can't write " + outputFileName );

        AffyProbeCollapser apc = new AffyProbeCollapser();
        apc.collapse( arrayName, new FileInputStream( f ), new BufferedWriter( new FileWriter( o ) ) );

    }
}
