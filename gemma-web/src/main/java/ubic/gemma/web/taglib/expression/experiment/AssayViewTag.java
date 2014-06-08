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
package ubic.gemma.web.taglib.expression.experiment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang3.StringUtils;

import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;

import com.sdicons.json.mapper.JSONMapper;
import com.sdicons.json.mapper.MapperException;

/**
 * Used to display table of biomaterials and bioassays. In edit mode this displays allows dragging bioassays around to
 * match up across platforms.
 * 
 * @author joseph
 * @version $Id$
 */
public class AssayViewTag extends TagSupport {
    /**
     * @author pavlidis
     * @version $Id$
     */
    static class BioMaterialComparator implements Comparator<BioMaterialValueObject> {

        @Override
        public int compare( BioMaterialValueObject arg0, BioMaterialValueObject arg1 ) {

            return arg0.getName().compareTo( arg1.getName() );
        }
    }

    /**
     * 
     */
    private static final long serialVersionUID = 8754490187937841260L;

    /**
     * How many 'extra' biomaterials to add to the editing table, so the user can assing bioassays to new biomaterials.
     */
    private static final int NUM_EXTRA_BIOMATERIALS = 12;

    private boolean edit = false;

    private Collection<BioAssayValueObject> bioAssays;

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doEndTag()
     */
    @Override
    public int doEndTag() {
        return EVAL_PAGE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.jsp.tagext.TagSupport#doStartTag()
     */
    @Override
    public int doStartTag() throws JspException {

        StringBuilder buf = new StringBuilder();

        buf.append( "<div>" );

        // create table
        Map<BioMaterialValueObject, Map<ArrayDesignValueObject, Collection<BioAssayValueObject>>> bioAssayMap = new HashMap<>();
        Set<ArrayDesignValueObject> designs = new HashSet<>();
        Map<ArrayDesignValueObject, Long> arrayMaterialCount = new HashMap<>();

        // package all of this information into JSON for javascript dynamic retrieval
        Map<String, String> assayToMaterial = new HashMap<>();
        for ( BioAssayValueObject assay : bioAssays ) {
            // map for bioassays linked to a specific arraydesign
            // map for the bioassays linked to a specific biomaterial
            BioMaterialValueObject material = assay.getSample();
            ArrayDesignValueObject design = assay.getArrayDesign();
            designs.add( design );

            // check if the assay list is initialized yet
            Map<ArrayDesignValueObject, Collection<BioAssayValueObject>> assayMap;
            if ( bioAssayMap.containsKey( material ) ) {
                assayMap = bioAssayMap.get( material );
            } else {
                assayMap = new HashMap<>();
                bioAssayMap.put( material, assayMap );
            }

            if ( assayMap.containsKey( design ) ) {
                assayMap.get( design ).add( assay );
            } else {
                Collection<BioAssayValueObject> assayList = new ArrayList<>();
                assayList.add( assay );
                assayMap.put( design, assayList );
            }

            if ( arrayMaterialCount.containsKey( design ) ) {
                Long count = arrayMaterialCount.get( design );
                count++;
                arrayMaterialCount.put( design, count );
            } else {
                Long count = new Long( 1 );
                arrayMaterialCount.put( design, count );
            }

        }
        int materialCount = bioAssayMap.keySet().size();
        buf.append( "<table class='list'><tr>" );
        buf.append( "<th>" + materialCount + " BioMaterials</th>" );
        // display arraydesigns
        for ( ArrayDesignValueObject design : designs ) {
            Long count = arrayMaterialCount.get( design );
            buf.append( "<th>" + count
                    + " BioAssays on<br /><a target='_blank' href=\"/Gemma/arrays/showArrayDesign.html?id="
                    + design.getId() + "\" title=\"" + design.getName() + "\" >"
                    + ( design.getShortName() == null ? design.getName() : design.getShortName() ) + "</a></th>" );
        }
        buf.append( "</tr>" );

        // display bioMaterials and the corresponding bioAssays
        int count = 1;

        Iterator<BioMaterialValueObject> iter = bioAssayMap.keySet().iterator();
        List<BioMaterialValueObject> materials = new ArrayList<>();
        while ( iter.hasNext() ) {
            materials.add( iter.next() );
        }
        Comparator<BioMaterialValueObject> comparator = new BioMaterialComparator();
        Collections.sort( materials, comparator );
        int elementCount = 1;
        int emptyCount = 0;
        for ( BioMaterialValueObject material : materials ) {
            if ( count % 2 == 0 ) {
                buf.append( "<tr class='even' align=justify>" );
            } else {
                buf.append( "<tr class='odd' align=justify>" );
            }

            String bmLink = "<a href='/Gemma/bioMaterial/showBioMaterial.html?id=" + material.getId() + "'> "
                    + material.getName() + "</a>";
            buf.append( "<td>" + bmLink + "</td>" );

            Map<ArrayDesignValueObject, Collection<BioAssayValueObject>> assayMap = bioAssayMap.get( material );

            String image = "&nbsp;&nbsp;&nbsp;<img height=16 width=16 src='/Gemma/images/icons/arrow_switch.png' />&nbsp;&nbsp;&nbsp;";
            for ( ArrayDesignValueObject design : designs ) {
                if ( assayMap.containsKey( design ) ) {
                    Collection<BioAssayValueObject> assays = assayMap.get( design );
                    Collection<Long> ids = new ArrayList<>();
                    Collection<String> tooltips = new ArrayList<>();
                    for ( BioAssayValueObject assay : assays ) {
                        ids.add( assay.getId() );
                        tooltips.add( StringUtils.abbreviate( assay.getName() + assay.getDescription(), 120 ) );
                        this.addMaterial( assayToMaterial, assay.getId(), material.getId() );
                    }

                    if ( assayMap.get( design ).size() > 1 ) {
                        String link = "<a title='" + StringUtils.join( tooltips.toArray(), "\n" )
                                + "' href='/Gemma/bioAssay/showAllBioAssays.html?id="
                                + StringUtils.join( ids.toArray(), "," ) + "'> (list) </a>";
                        buf.append( "<td>" + assayMap.get( design ).size() + link + "&nbsp;" + elementCount + "</td>\n" );

                    } else {

                        /*
                         * Each bioassay has a unique id; the div it sits in is identified by the class 'dragitem'. See
                         * expressionExperiment.edit.jsp.
                         */

                        BioAssayValueObject assay = ( ( List<BioAssayValueObject> ) assayMap.get( design ) ).get( 0 );
                        String shortDesc = StringUtils.abbreviate( assay.getDescription(), 60 );
                        String link = "<a target=\"_blank\" title='" + shortDesc
                                + "' href='/Gemma/bioAssay/showBioAssay.html?id=" + assay.getId() + "'>"
                                + assay.getName() + "</a>";
                        String editAttributes = " align='left' class='dragItem' id='bioassay." + assay.getId()
                                + "' material='" + material.getId() + "' assay='" + assay.getId() + "' arrayDesign='"
                                + design.getId() + "'";
                        if ( edit && designs.size() > 1 ) {
                            buf.append( "\n<td><span " + editAttributes + ">" + image + link );
                        } else {
                            buf.append( "\n<td ><span>" + link + "&nbsp;" );
                        }
                        buf.append( "</span></td>\n" );
                    }

                } else {
                    emptyCount = addEmpty( buf, assayToMaterial, emptyCount, material, image, design );
                }
            }

            buf.append( "</tr>" );
            count++;
            elementCount++;
        }

        // add a few blanks, but only if we are editing.
        if ( edit ) {
            addNovelBiomaterialSlots( buf, designs, assayToMaterial, count, emptyCount );
        }

        buf.append( "</table>" );

        if ( edit ) {
            // append JSON serialization
            try {
                String jsonSerialization = JSONMapper.toJSON( assayToMaterial ).render( false );
                buf.append( "<input type='hidden' id='assayToMaterialMap' name='assayToMaterialMap' value='"
                        + jsonSerialization + "'/>" );
            } catch ( MapperException e ) {
                // cannot serialize
            }

        }

        buf.append( "</div>" );

        try {
            pageContext.getOut().print( buf.toString() );
        } catch ( Exception ex ) {
            throw new JspException( "assayViewTag: " + ex.getMessage() );
        }
        return SKIP_BODY;
    }

    /**
     * @param edit
     */
    public void setEdit( String edit ) {
        if ( edit.equalsIgnoreCase( "true" ) ) {
            this.edit = true;
        } else {
            this.edit = false;
        }
    }

    /**
     * @param expressionExperiment
     */
    public void setBioAssays( Collection<BioAssayValueObject> bioAssays ) {
        this.bioAssays = bioAssays;
    }

    /**
     * Add a 'unused' biomaterial/bioassay combination to the table.
     * 
     * @param buf
     * @param assayToMaterial
     * @param emptyCount
     * @param material
     * @param image
     * @param design
     * @return revised count of number of empty items.
     */
    private int addEmpty( StringBuilder buf, Map<String, String> assayToMaterial, int emptyCount,
            BioMaterialValueObject material, String image, ArrayDesignValueObject design ) {
        // put empty space in table if the bioMaterial does not
        // use this array design
        emptyCount++;
        String editAttributes = "class='dragItem' id='bioassay.empty." + emptyCount + "' material='" + material.getId()
                + "' assay='nullElement' arrayDesign='" + design.getId() + "'";

        if ( edit ) {
            buf.append( "\n<td><span " + editAttributes + ">" + image );
        } else {
            buf.append( "\n<td><span>&nbsp;" );
        }
        this.addMaterial( assayToMaterial, null, material.getId() );
        buf.append( "</span></td>\n" );
        return emptyCount;
    }

    /**
     * @param assayToMaterial
     * @param bioAssayId
     * @param bioMaterialId
     */
    private void addMaterial( Map<String, String> assayToMaterial, Long bioAssayId, Long bioMaterialId ) {
        String bioAssayStr = "";
        if ( bioAssayId == null ) {
            bioAssayStr = "nullElement";
        } else {
            bioAssayStr = bioAssayId.toString();
        }

        assayToMaterial.put( bioAssayStr, bioMaterialId.toString() );

    }

    /**
     * Add places for completely new biomaterials to be added. These are the row labels.
     * 
     * @param buf
     * @param designs
     * @param assayToMaterial
     * @param count
     * @param emptyCount
     */
    private void addNovelBiomaterialSlots( StringBuilder buf, Set<ArrayDesignValueObject> designs,
            Map<String, String> assayToMaterial, int count, int emptyCount ) {
        if ( designs.size() == 1 ) {
            return;
        }
        for ( int i = 1; i <= NUM_EXTRA_BIOMATERIALS; i++ ) {

            if ( count % 2 == 0 ) {
                buf.append( "<tr class='even' align=justify>" );
            } else {
                buf.append( "<tr class='odd' align=justify>" );
            }
            BioMaterialValueObject material = new BioMaterialValueObject();

            // FIXME this is a kludge: use negative ids to distinguish the new biomaterials.
            material.setId( 0L - i );

            material.setName( "[New biomaterial " + i + "]" );
            buf.append( "<td>" + material.getName() + "</td>" );
            String image = "<img height=10 width=20 src='/Gemma/images/arrow_out.png' />";
            for ( ArrayDesignValueObject design : designs ) {
                emptyCount = addEmpty( buf, assayToMaterial, emptyCount, material, image, design );
            }
            buf.append( "</tr>" );
            count++;
        }
    }
}
