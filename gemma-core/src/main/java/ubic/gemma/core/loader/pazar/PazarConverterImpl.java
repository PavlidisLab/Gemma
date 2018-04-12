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
package ubic.gemma.core.loader.pazar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.loader.pazar.model.PazarRecord;
import ubic.gemma.model.association.PazarAssociation;
import ubic.gemma.model.genome.Gene;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author paul
 */
@Component
public class PazarConverterImpl implements PazarConverter {

    private static final Log log = LogFactory.getLog( PazarConverterImpl.class.getName() );

    @Autowired
    private GeneService geneService;

    @Override
    public Collection<PazarAssociation> convert( Collection<? extends PazarRecord> sourceDomainObjects ) {
        Collection<PazarAssociation> result = new HashSet<>();

        for ( PazarRecord r : sourceDomainObjects ) {
            result.add( this.convert( r ) );
        }

        return result;
    }

    @Override
    public PazarAssociation convert( PazarRecord sourceDomainObject ) {

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
            PazarConverterImpl.log.warn( "Failed to map a gene:" + targetAcc );
            return null;
        }
        if ( tfGene == null ) {
            PazarConverterImpl.log.warn( "Failed to map a gene: " + tfAcc );
            return null;
        }

        return PazarAssociation.Factory.newInstance( null, targetGene, tfGene, null, sourceDomainObject.getPazarTfId(),
                sourceDomainObject.getPazarTargetGeneId() );
    }
}