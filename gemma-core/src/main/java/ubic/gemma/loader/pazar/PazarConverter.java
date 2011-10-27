/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.loader.pazar;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.loader.pazar.model.PazarRecord;
import ubic.gemma.loader.util.converter.Converter;
import ubic.gemma.model.association.PazarAssociation;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;

/**
 * @author paul
 * @version $Id$
 */
@Service
public class PazarConverter implements Converter<PazarRecord, PazarAssociation> {

    private static Log log = LogFactory.getLog( PazarConverter.class.getName() );

    @Autowired
    private GeneService geneService;

    @Override
    public Collection<PazarAssociation> convert( Collection<PazarRecord> sourceDomainObjects ) {
        Collection<PazarAssociation> result = new HashSet<PazarAssociation>();

        for ( PazarRecord r : sourceDomainObjects ) {
            result.add( convert( r ) );
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.loader.util.converter.Converter#convert(java.lang.Object)
     * 
     * Note that not everything maps nicely. Some TFs are dimers, so they have two genes. Others don't map to NCBI.
     */
    @Override
    public PazarAssociation convert( PazarRecord sourceDomainObject ) {
        PazarAssociation a = PazarAssociation.Factory.newInstance();

        String targetAcc = sourceDomainObject.getTargetGeneAcc();
        Gene targetGene = null;
        try {
            targetGene = geneService.findByNCBIId( Integer.parseInt( targetAcc ) );
        } catch ( NumberFormatException e ) {
            // ok.
        }

        String tfAcc = sourceDomainObject.getTfAcc();
        Gene tfGene = null;
        try {
            tfGene = geneService.findByNCBIId( Integer.parseInt( tfAcc ) );
        } catch ( NumberFormatException e ) {
            // ok.
        }

        if ( targetGene == null ) {
            log.warn( "Failed to map a gene:" + targetAcc );
            return null;
        }
        if ( tfGene == null ) {
            log.warn( "Failed to map a gene: " + tfAcc );
            return null;
        }
        a.setPazarTargetGeneId( sourceDomainObject.getPazarTargetGeneId() );
        a.setPazarTfId( sourceDomainObject.getPazarTfId() );
        a.setFirstGene( tfGene );
        a.setSecondGene( targetGene );

        return a;
    }
}
