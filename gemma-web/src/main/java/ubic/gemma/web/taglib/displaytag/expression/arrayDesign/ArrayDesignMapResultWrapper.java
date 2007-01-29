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
package ubic.gemma.web.taglib.displaytag.expression.arrayDesign;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.displaytag.decorator.TableDecorator;

import ubic.gemma.analysis.sequence.CompositeSequenceMapValueObject;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;

/**
 * Used to generate hyperlinks in displaytag tables.
 * <p>
 * See http://displaytag.sourceforge.net/10/tut_decorators.html and http://displaytag.sourceforge.net/10/tut_links.html
 * for explanation of how this works.
 * 
 * @author joseph
 */
public class ArrayDesignMapResultWrapper extends TableDecorator {

    Log log = LogFactory.getLog( this.getClass() );

    public String getGeneList() {
        CompositeSequenceMapValueObject object = ( CompositeSequenceMapValueObject ) getCurrentRowObject();
        Collection gVos = object.getGenes().values();
        Collection gpVos = object.getGeneProducts().values();

        // get unique ID - compositeSequenceId
        // String compositeSequenceId = "cs" + object.getCompositeSequenceId();

        // associate genes with geneProducts
        HashMap<Long, Collection> geneProducts = new HashMap<Long, Collection>();

        for ( Object o2 : gpVos ) {

            GeneProductValueObject gpVo = ( GeneProductValueObject ) o2;

            if ( !geneProducts.containsKey( gpVo.getGeneId() ) ) {
                Collection geneProductVos = new ArrayList<GeneProductValueObject>();
                // geneProductVos.add( gpVo );
                geneProducts.put( gpVo.getGeneId(), geneProductVos );
            } else {
                // Collection geneProductVos = geneProducts.get( gpVo.getGeneId() );
                // geneProductVos.add( gpVo );
            }
        }

        StringBuffer retVal = new StringBuffer();
        String[] geneList = new String[gVos.size()];
        int geneCount = 0;
        for ( Object o2 : gVos ) {
            GeneValueObject gVo = ( GeneValueObject ) o2;

            String fullName = gVo.getOfficialSymbol();
            geneList[geneCount] = fullName;
            geneCount++;

        }
        String fullGeneList = StringUtils.join( geneList, "," );
        String shortGeneList = StringUtils.abbreviate( fullGeneList, 20 );
        retVal.append( shortGeneList );
        retVal.append( "(" + geneList.length + ")" );
        return retVal.toString();

    }

    // private String generateGeneProductLink( GeneProductValueObject gpVo ) {
    // StringBuffer gpStr = new StringBuffer();
    // String fullName = gpVo.getName();
    // String shortName = StringUtils.abbreviate( fullName, 20 );
    //
    // String ncbiLink = "";
    // if ( gpVo.getType().equalsIgnoreCase( "RNA" ) ) {
    // ncbiLink = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=Nucleotide&cmd=search&term=";
    // } else {
    // // assume protein
    // ncbiLink = "http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=Protein&cmd=search&term=";
    //
    // }
    // if ( gpVo.getNcbiId() != null ) {
    // gpStr.append( "&nbsp;&nbsp;<span title='" + fullName + "'>" + shortName
    // + "</span><a target='_blank' href='" + ncbiLink + gpVo.getNcbiId()
    // + "'><img height=10 width=10 src='/Gemma/images/logo/ncbi.gif' /></a><BR>" );
    // } else {
    // gpStr.append( "&nbsp;&nbsp;<span title='" + fullName + "'>" + shortName + "</span><BR>" );
    // }
    // return gpStr.toString();
    // }

    public String getCompositeSequenceNameLink() {
        if ( getCurrentRowObject() == null ) return "";
        CompositeSequenceMapValueObject object = ( CompositeSequenceMapValueObject ) getCurrentRowObject();
        String name = object.getCompositeSequenceName();
        String id = object.getCompositeSequenceId();
        // call ajax, then call onload function to initialize scrolltable
        String nameLink = "<a href='#' onclick=\"ajaxAnywhere.getAJAX('/Gemma/compositeSequence/showAjaxCompositeSequence.html?id="
                + id + "');return false;\"> " + name + " </a>";
        return nameLink;
    }
}
