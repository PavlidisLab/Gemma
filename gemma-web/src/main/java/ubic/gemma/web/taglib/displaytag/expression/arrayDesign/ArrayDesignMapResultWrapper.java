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
@Deprecated
public class ArrayDesignMapResultWrapper extends TableDecorator {

    public String getCompositeSequenceNameLink() {
        if ( getCurrentRowObject() == null ) return "";
        CompositeSequenceMapValueObject object = ( CompositeSequenceMapValueObject ) getCurrentRowObject();
        String name = object.getCompositeSequenceName();
        String id = object.getCompositeSequenceId();
        // call ajax, then call onload function to initialize scrolltable
        String nameLink = "<a href='/Gemma/compositeSequence/showCompositeSequence.html?id=" + id
                + "' onclick=\"ajaxAnywhere.getAJAX('/Gemma/compositeSequence/showAjaxCompositeSequence.html?id=" + id
                + "');showblatres(" + id + ");return false;\"> " + name + " </a>";
        return nameLink;
    }

    public String getGeneList() {
        CompositeSequenceMapValueObject object = ( CompositeSequenceMapValueObject ) getCurrentRowObject();
        Collection<GeneValueObject> gVos = object.getGenes().values();
        Collection<GeneProductValueObject> gpVos = object.getGeneProducts().values();

        // get unique ID - compositeSequenceId
        // String compositeSequenceId = "cs" + object.getCompositeSequenceId();

        // associate genes with geneProducts
        HashMap<Long, Collection<GeneProductValueObject>> geneProducts = new HashMap<Long, Collection<GeneProductValueObject>>();

        for ( Object o2 : gpVos ) {

            GeneProductValueObject gpVo = ( GeneProductValueObject ) o2;

            if ( !geneProducts.containsKey( gpVo.getGeneId() ) ) {
                Collection<GeneProductValueObject> geneProductVos = new ArrayList<GeneProductValueObject>();
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

        if ( geneList.length > 0 ) {
            String fullGeneList = StringUtils.join( geneList, "," );
            String shortGeneList = StringUtils.abbreviate( fullGeneList, 20 );
            retVal.append( shortGeneList );
            if ( geneList.length > 1 ) {
                retVal.append( "&nbsp;(" + geneList.length + ")" );
            }
        } else {
            retVal.append( "&nbsp;-&nbsp;" );
        }
        return retVal.toString();

    }
}
