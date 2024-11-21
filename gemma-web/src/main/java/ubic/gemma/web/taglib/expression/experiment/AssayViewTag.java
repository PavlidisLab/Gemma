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

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.web.servlet.tags.HtmlEscapingAwareTag;
import org.springframework.web.servlet.tags.form.TagWriter;
import org.springframework.web.util.HtmlUtils;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.web.util.StaticAssetServer;
import ubic.gemma.web.util.WebEntityUrlBuilder;

import javax.annotation.Nullable;
import javax.servlet.jsp.JspException;
import java.util.*;

/**
 * Used to display table of biomaterials and bioassays. In edit mode this displays allows dragging bioassays around to
 * match up across platforms.
 *
 * @author joseph
 */
@CommonsLog
public class AssayViewTag extends HtmlEscapingAwareTag {

    /**
     * How many 'extra' biomaterials to add to the editing table, so the user can passing assays to new biomaterials.
     */
    private static final int NUM_EXTRA_BIOMATERIALS = 12;

    private StaticAssetServer staticAssetServer;
    private WebEntityUrlBuilder entityUrlBuilder;

    private Collection<BioAssayValueObject> bioAssays;

    @Nullable
    private Long expressionExperimentId;

    private boolean edit = false;

    /* internal state */
    private int currentRow;
    private int emptyAssays;

    public void setBioAssays( Collection<BioAssayValueObject> bioAssays ) {
        this.bioAssays = bioAssays;
    }

    /**
     * An identifier to the expression experiment that owns the biossays.
     */
    public void setExpressionExperimentId( @Nullable Long eeId ) {
        this.expressionExperimentId = eeId;
    }

    public void setEdit( boolean edit ) {
        this.edit = edit;
    }

    @Override
    public int doStartTagInternal() throws Exception {
        if ( staticAssetServer == null ) {
            staticAssetServer = getRequestContext().getWebApplicationContext().getBean( StaticAssetServer.class );
        }
        if ( entityUrlBuilder == null ) {
            entityUrlBuilder = getRequestContext().getWebApplicationContext().getBean( WebEntityUrlBuilder.class );
        }

        currentRow = 0;
        emptyAssays = 0;

        TagWriter writer = new TagWriter( pageContext );

        writer.startTag( "div" );

        // create table
        Map<BioMaterialValueObject, Map<ArrayDesignValueObject, Collection<BioAssayValueObject>>> bioAssayMap = new HashMap<>();
        LinkedHashSet<ArrayDesignValueObject> designs = new LinkedHashSet<>();
        Map<ArrayDesignValueObject, Long> arrayMaterialCount = new HashMap<>();

        // package all of this information into JSON for javascript dynamic retrieval
        JSONObject assayToMaterial = new JSONObject();
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
                arrayMaterialCount.put( design, 1L );
            }

        }
        int materialCount = bioAssayMap.size();
        writer.startTag( "table" );
        writer.writeAttribute( "class", "detail row-separated odd-gray" );
        writer.startTag( "tr" );
        writer.startTag( "th" );
        writer.appendValue( materialCount + " Biomaterials" );
        if ( expressionExperimentId != null ) {
            writer.appendValue( " (" );
            writer.startTag( "a" );
            ExpressionExperiment ee = new ExpressionExperiment();
            ee.setId( expressionExperimentId );
            String eeUri = entityUrlBuilder.fromContextPath().entity( ee ).web().bioMaterials().toUriString();
            writer.writeAttribute( "href", htmlEscape( eeUri ) );
            writer.appendValue( "list" );
            writer.endTag(); // </a>
            writer.appendValue( ")" );
        }
        writer.endTag(); // </th>
        // display arraydesigns
        for ( ArrayDesignValueObject design : designs ) {
            Long count = arrayMaterialCount.get( design );
            writer.startTag( "th" );
            writer.appendValue( count + " Assays " );
            if ( expressionExperimentId != null ) {
                writer.appendValue( " (" );
                writer.startTag( "a" );
                writer.writeAttribute( "target", "_blank" );
                ExpressionExperiment ee = new ExpressionExperiment();
                ee.setId( expressionExperimentId );
                String bioAssaysUri = entityUrlBuilder.fromContextPath().entity( ee ).web().bioAssays().toUriString();
                writer.writeAttribute( "href", htmlEscape( bioAssaysUri ) + "&platform=" + design.getId() );
                writer.appendValue( "list" );
                writer.endTag();
                writer.appendValue( ")" );
            }
            writer.appendValue( " on " );
            writer.startTag( "a" );
            writer.writeAttribute( "target", "_blank" );
            String arrayDesignUri = entityUrlBuilder.fromContextPath().entity( ArrayDesign.class, design.getId() ).toUriString();
            writer.writeAttribute( "href", htmlEscape( arrayDesignUri ) );
            writer.writeAttribute( "title", htmlEscape( design.getName() ) );
            writer.appendValue( htmlEscape( design.getShortName() ) );
            writer.endTag(); // <a>
            writer.endTag(); // </th>
        }
        writer.endTag(); // </tr>

        // display bioMaterials and the corresponding bioAssays

        Iterator<BioMaterialValueObject> iter = bioAssayMap.keySet().iterator();
        List<BioMaterialValueObject> materials = new ArrayList<>();
        while ( iter.hasNext() ) {
            materials.add( iter.next() );
        }
        materials.sort( Comparator.comparing( BioMaterialValueObject::getName ) );
        for ( BioMaterialValueObject material : materials ) {
            writer.startTag( "tr" );
            if ( currentRow % 2 == 0 ) {
                writer.writeAttribute( "class", "even" );
            } else {
                writer.writeAttribute( "class", "odd" );
            }

            writer.startTag( "td" );
            writer.startTag( "a" );
            String bioMaterialUri = entityUrlBuilder.fromContextPath().entity( BioMaterial.class, material.getId() ).toUriString();
            writer.writeAttribute( "href", htmlEscape( bioMaterialUri ) );
            writer.appendValue( htmlEscape( material.getName() ) );
            writer.endTag(); // </a>
            writer.endTag(); // </td>

            Map<ArrayDesignValueObject, Collection<BioAssayValueObject>> assayMap = bioAssayMap.get( material );

            for ( ArrayDesignValueObject design : designs ) {
                if ( assayMap.containsKey( design ) ) {
                    Collection<BioAssayValueObject> assays = assayMap.get( design );
                    for ( BioAssayValueObject assay : assays ) {
                        Long bioAssayId = assay.getId();
                        assayToMaterial.put( bioAssayId.toString(), material.getId().toString() );
                    }

                    if ( assayMap.get( design ).size() > 1 ) {
                        writer.startTag( "td" );
                        boolean first = true;
                        for ( BioAssayValueObject assay : assays ) {
                            if ( !first ) {
                                writer.appendValue( ", " );
                            }
                            first = false;
                            writer.startTag( "a" );
                            writer.writeAttribute( "target", "_blank" );
                            writer.writeAttribute( "title", htmlEscape( StringUtils.abbreviate( assay.getDescription(), 60 ) ) );
                            String bioAssayUri = entityUrlBuilder.fromContextPath().entity( BioAssay.class, assay.getId() ).toUriString();
                            writer.writeAttribute( "href", htmlEscape( bioAssayUri ) );
                            writer.appendValue( htmlEscape( assay.getName() ) );
                            writer.endTag(); // </a>
                        }
                        writer.endTag();
                    } else {

                        /*
                         * Each bioassay has a unique id; the div it sits in is identified by the class 'dragitem'. See
                         * expressionExperiment.edit.jsp.
                         */

                        BioAssayValueObject assay = assayMap.get( design ).iterator().next();
                        writer.startTag( "td" );
                        writer.startTag( "span" );
                        if ( edit && designs.size() > 1 ) {
                            writer.writeAttribute( "class", "dragItem" );
                            writer.writeAttribute( "id", "bioassay." + assay.getId() );
                            writer.writeAttribute( "material", material.getId().toString() );
                            writer.writeAttribute( "assay", assay.getId().toString() );
                            writer.writeAttribute( "arrayDesign", design.getId().toString() );
                            writer.startTag( "img" );
                            writer.writeAttribute( "height", "16" );
                            writer.writeAttribute( "width", "16" );

                            writer.writeAttribute( "src", staticAssetServer.resolveUrl( "/images/icons/arrow_switch.png" ) );
                            writer.endTag();
                            writer.appendValue( "&nbsp;" );
                        }
                        writer.startTag( "a" );
                        writer.writeAttribute( "target", "_blank" );
                        writer.writeAttribute( "title", htmlEscape( StringUtils.abbreviate( assay.getDescription(), 60 ) ) );
                        String bioAssayUri = entityUrlBuilder.fromContextPath().entity( BioAssay.class, assay.getId() ).toUriString();
                        writer.writeAttribute( "href", htmlEscape( bioAssayUri ) );
                        writer.appendValue( htmlEscape( assay.getName() ) );
                        writer.endTag(); // </a>
                        writer.endTag(); // </span>
                        writer.endTag(); // </td>
                    }

                } else {
                    addEmptyBioAssay( assayToMaterial, material, design, writer );
                }
            }

            writer.endTag(); // </tr>
            currentRow++;
        }

        // add a few blanks, but only if we are editing.
        if ( edit && designs.size() > 1 ) {
            addEmptyRows( designs, assayToMaterial, writer );
        }

        writer.endTag(); // </table>

        if ( edit ) {
            // append JSON serialization
            writer.startTag( "input" );
            writer.writeAttribute( "type", "hidden" );
            writer.writeAttribute( "id", "assayToMaterialMap" );
            writer.writeAttribute( "name", "assayToMaterialMap" );
            writer.writeAttribute( "value", htmlEscape( assayToMaterial.toString() ) );
            writer.endTag();
        }

        writer.endTag();

        return SKIP_BODY;
    }

    /**
     * Add places for completely new biomaterials to be added. These are the row labels.
     */
    private void addEmptyRows( LinkedHashSet<ArrayDesignValueObject> designs,
            JSONObject assayToMaterial, TagWriter writer ) throws JspException {
        for ( long i = 1; i <= NUM_EXTRA_BIOMATERIALS; i++ ) {
            writer.startTag( "tr" );
            if ( currentRow % 2 == 0 ) {
                writer.writeAttribute( "class", "even" );
            } else {
                writer.writeAttribute( "class", "odd" );
            }

            // FIXME this is a kludge: use negative ids to distinguish the new biomaterials.
            BioMaterialValueObject material = new BioMaterialValueObject( -i );

            material.setName( "New Biomaterial #" + i );
            writer.startTag( "td" );
            writer.startTag( "i" );
            writer.appendValue( htmlEscape( material.getName() ) );
            writer.endTag(); // </i>
            writer.endTag(); // </td>
            for ( ArrayDesignValueObject design : designs ) {
                addEmptyBioAssay( assayToMaterial, material, design, writer );
                emptyAssays++;
            }
            writer.endTag(); // </tr>
            currentRow++;
        }
    }

    /**
     * Add an 'unused' assay for the given material.
     */
    private void addEmptyBioAssay( JSONObject assayToMaterial, BioMaterialValueObject material, ArrayDesignValueObject design, TagWriter writer ) throws JspException {
        // put empty space in table if the bioMaterial does not
        // use this array design
        writer.startTag( "td" );
        writer.startTag( "span" );
        if ( edit ) {
            writer.writeAttribute( "class", "dragItem" );
            writer.writeAttribute( "id", "bioassay.empty." + ( emptyAssays + 1 ) );
            writer.writeAttribute( "material", material.getId().toString() );
            writer.writeAttribute( "assay", "nullElement" );
            writer.writeAttribute( "arrayDesign", design.getId().toString() );
            writer.startTag( "img" );
            writer.writeAttribute( "height", "10" );
            writer.writeAttribute( "width", "20" );
            writer.writeAttribute( "src", staticAssetServer.resolveUrl( "/images/arrow_out.png" ) );
            writer.endTag(); // </img>
            emptyAssays++;
        } else {
            writer.appendValue( "&nbsp;" );
        }
        writer.endTag(); // </span>
        writer.endTag(); // </td>
        assayToMaterial.put( "nullElement", material.getId().toString() );
    }

    private String htmlEscape( String s ) {
        return isHtmlEscape() ? HtmlUtils.htmlEscape( s ) : s;
    }
}
