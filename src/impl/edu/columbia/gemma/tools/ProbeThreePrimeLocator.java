package edu.columbia.gemma.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.sequenceAnalysis.BlatResultImpl;
import edu.columbia.gemma.loader.genome.BlatResultParser;
import edu.columbia.gemma.tools.GoldenPath.ThreePrimeData;

/**
 * Given a blat result set for an array design, find the 3' locations for all the really good hits.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ProbeThreePrimeLocator {
    protected static final Log log = LogFactory.getLog( ProbeThreePrimeLocator.class );
    private String dbName = "hg17";
    private double scoreThreshold = 0.96;

    public void run( InputStream input, Writer output ) throws IOException, SQLException, InstantiationException,
            IllegalAccessException, ClassNotFoundException {

        GoldenPath bp = new GoldenPath( 3307, dbName, "localhost", "pavlidis", "toast" );

        BlatResultParser brp = new BlatResultParser();
        brp.parse( input );

        int count = 0;
        for ( Iterator iter = brp.iterator(); iter.hasNext(); ) {
            BlatResultImpl blatRes = ( BlatResultImpl ) iter.next(); // fixme, this should not be an impl
            double score = blatRes.score();
            if ( score < scoreThreshold ) continue;

            String qName = blatRes.getQueryName();

            String[] sa = qName.split( ":" );
            if ( sa.length < 2 ) throw new IllegalArgumentException( "Expected query name in format 'xxx:xxx'" );
            String probeName = sa[0];
            String arrayName = sa[1];

            List tpds = bp.getThreePrimeDistances( blatRes.getTargetName(), blatRes.getTargetStart(), blatRes
                    .getTargetEnd() );

            if ( tpds == null ) continue;

            for ( Iterator iterator = tpds.iterator(); iterator.hasNext(); ) {
                ThreePrimeData tpd = ( ThreePrimeData ) iterator.next();
                Gene gene = tpd.getGene();

                assert gene != null : "Null gene";

                String geneName = gene.getOfficialSymbol();

                output.write( probeName + "\t" + arrayName + "\t" + blatRes.getMatches() + "\t"
                        + blatRes.getQuerySize() + "\t" + score + "\t" + geneName + "\t" + gene.getName() + "\t"
                        + tpd.getDistance() + "\n" );

                count++;
                if ( count % 100 == 0 ) log.info( "Three-prime locations computed for " + count + " probes" );

            }

        }
        input.close();
        output.close();
    }

    public static void main( String[] args ) {

        try {
            if ( args.length < 2 ) throw new IllegalArgumentException( "usage: input file name, output filename" );
            String filename = args[0];
            File f = new File( filename );
            if ( !f.canRead() ) throw new IOException();

            String outputFileName = args[1];
            File o = new File( outputFileName );
            // if ( !o.canWrite() ) throw new IOException( "Can't write " + outputFileName );

            ProbeThreePrimeLocator ptpl = new ProbeThreePrimeLocator();
            ptpl.run( new FileInputStream( f ), new BufferedWriter( new FileWriter( o ) ) );

        } catch ( IOException e ) {
            log.error( e, e );
        } catch ( SQLException e ) {
            log.error( e, e );
        } catch ( InstantiationException e ) {
            log.error( e, e );
        } catch ( IllegalAccessException e ) {
            log.error( e, e );
        } catch ( ClassNotFoundException e ) {
            log.error( e, e );
        }
    }

}
