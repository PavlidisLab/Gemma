/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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

package ubic.gemma.ontology;

import java.util.ArrayList;
import java.util.List;

/**
 * @author klc
 */

/**
 * @author klc
 */
public class OntologyTreeNode {

    private boolean expanded;
    private boolean isTarget;
    private boolean draggable;
    private boolean allowDrag;
    private boolean allowChildren;
    private boolean allowDrop;
    private boolean leaf;
    private String text;
    private String id;
    private List<OntologyTreeNode> children;
    private String uiProvider;

    public OntologyTreeNode() {
        super();
        this.expanded = false;
        this.isTarget = false;
        this.draggable = false;
        this.allowDrag = false;
        this.allowChildren = false;
        this.allowDrop = false;
        this.leaf = false;
        this.text = "ME";
        this.id = "21";
        children = new ArrayList<OntologyTreeNode>();

    }

    /**
     * @param term Doesn't fill in the child assocations
     */
    public OntologyTreeNode( OntologyTerm term ) {
        this();

        this.id = term.getUri();
        this.text = term.getTerm();

    }

    public OntologyTreeNode( String id ) {
        this();
        this.id = id;

    }

    public void appendChild( OntologyTreeNode child ) {
        this.children.add( child );
    }

    public List<OntologyTreeNode> getChildren() {
        return children;
    }

    public String getId() {
        return id;
    }

    /**
     * @return the text
     */
    public String getText() {
        return text;
    }

    public String getUiProvider() {
        return uiProvider;
    }

    /**
     * @return the allowChildren
     */
    public boolean isAllowChildren() {
        return allowChildren;
    }

    /**
     * @return the allowDrag
     */
    public boolean isAllowDrag() {
        return allowDrag;
    }

    /**
     * @return the allowDrop
     */
    public boolean isAllowDrop() {
        return allowDrop;
    }

    /**
     * @return the draggable
     */
    public boolean isDraggable() {
        return draggable;
    }

    /**
     * @return the expanded
     */
    public boolean isExpanded() {
        return expanded;
    }

    /**
     * @return the leaf
     */
    public boolean isLeaf() {
        return leaf;
    }

    /**
     * @return the isTarget
     */
    public boolean isTarget() {
        return isTarget;
    }

    /**
     * @param allowChildren the allowChildren to set
     */
    public void setAllowChildren( boolean allowChildren ) {
        this.allowChildren = allowChildren;
    }

    /**
     * @param allowDrag the allowDrag to set
     */
    public void setAllowDrag( boolean allowDrag ) {
        this.allowDrag = allowDrag;
    }

    /**
     * @param allowDrop the allowDrop to set
     */
    public void setAllowDrop( boolean allowDrop ) {
        this.allowDrop = allowDrop;
    }

    /**
     * @param draggable the draggable to set
     */
    public void setDraggable( boolean draggable ) {
        this.draggable = draggable;
    }

    /**
     * @param expanded the expanded to set
     */
    public void setExpanded( boolean expanded ) {
        this.expanded = expanded;
    }

    public void setId( String id ) {
        this.id = id;
    }

    /**
     * @param leaf the leaf to set
     */
    public void setLeaf( boolean leaf ) {
        this.leaf = leaf;
    }

    /**
     * @param isTarget the isTarget to set
     */
    public void setTarget( boolean isTarget ) {
        this.isTarget = isTarget;
    }

    /**
     * @param text the text to set
     */
    public void setText( String text ) {
        this.text = text;
    }

    public void setUiProvider( String uiProvider ) {
        this.uiProvider = uiProvider;
    }
}
