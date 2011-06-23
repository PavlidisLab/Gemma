/*
Author	: 	Anh Nguyen (Totti)
Email	: 	tot2ivn@gmail.com
Blog	: 	http://iamtotti.com

Ext.ux.tot2ivn.AccordionVboxLayout extension 
is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

Ext.ns('Ext.ux.tot2ivn');

/**
 * @class Ext.ux.tot2ivn.AccordionVboxLayout
 * @extends Ext.layout.AccordionLayout
 * <p>This is a layout that manages multiple Panels in an expandable hybrid style between AccordionLayout and VBoxLayout 
 * such that <b>multiple panels can be collapsed or expanded at a time</b> and the sizes of other panels are <b>automatically
 * resized according to the new empty space left</b>.
 * Each Panel has built-in support for expanding and collapsing. And the size of the panels can be flexibly defined as 
 * in the VBoxLayout, with the powerful 'flex' property.</p> 
 * <p>Example usage:</p>
 * <pre><code>
var accordion = new Ext.Panel({
    title: 'Accordion Layout',
    layout:'ux.accordionfit',
    defaults: {
        // applied to each contained panel
        bodyStyle: 'padding:15px',
		flex			: 1			// The sizes of panels are divided according to the flex index
    },
    layoutConfig: {
		align 			: 'stretch',
		pack  			: 'start',
		animate			: true,
		titleCollapse	: true							    
	},
    items: [{
        title: 'Panel 1',
        html: '&lt;p&gt;Panel content!&lt;/p&gt;'
    },{
        title: 'Panel 2',
        html: '&lt;p&gt;Panel content!&lt;/p&gt;'
    },{
        title: 'Panel 3',
        html: '&lt;p&gt;Panel content!&lt;/p&gt;'
    }]
});
</code></pre>
 */

Ext.ux.tot2ivn.AccordionVboxLayout = Ext.extend(Ext.layout.AccordionLayout, {
	defaultMargins : {left:0,top:0,right:0,bottom:0},
    
    padding : '0',
    // documented in subclasses
    pack : 'start',
	
	collapsedOffset : 0,
	
	constructor : function(config){
        Ext.ux.tot2ivn.AccordionVboxLayout.superclass.constructor.call(this, config);

        if (Ext.isString(this.defaultMargins)) {
            this.defaultMargins = this.parseMargins(this.defaultMargins);
        }
    },
	
	/**
     * @private
     * Runs the child box calculations and caches them in childBoxCache. Subclasses can used these cached values
     * when laying out
     */
    onLayout: function(container, target) {
        Ext.ux.tot2ivn.AccordionVboxLayout.superclass.onLayout.call(this, container, target);
		
        var items = this.getVisibleItems(container),
            tSize = this.getLayoutTargetSize();

        /**
         * @private
         * @property layoutTargetLastSize
         * @type Object
         * Private cache of the last measured size of the layout target. This should never be used except by
         * BoxLayout subclasses during their onLayout run.
         */
        this.layoutTargetLastSize = tSize;

        /**
         * @private
         * @property childBoxCache
         * @type Array
         * Array of the last calculated height, width, top and left positions of each visible rendered component
         * within the Box layout.
         */
        this.childBoxCache = this.calculateChildBoxes(items, tSize);

        this.updateInnerCtSize(tSize, this.childBoxCache);
        this.updateChildBoxes(this.childBoxCache.boxes);

        // Putting a box layout into an overflowed container is NOT correct and will make a second layout pass necessary.
        this.handleTargetOverflow(tSize, container, target);
    },
	
    /**
     * Resizes each child component
     * @param {Array} boxes The box measurements
     */
    updateChildBoxes: function(boxes) {
        for (var i = 0, length = boxes.length; i < length; i++) {
            var box  = boxes[i],
                comp = box.component;

            if (box.dirtySize) {
				// On IE, setSize throws an exception when box.width is !undefined
                // comp.setSize(box.width, box.height);
				comp.setSize(undefined, box.height);
            }
        }
    },
	
	/**
     * Resizes each child component
     * @param {Array} boxes The box measurements
     */
    resizeChildComponents: function(boxes, anim) {
		for (var i = 0, l = boxes.length; i < l; i++) {
            var box  = boxes[i],
                comp = box.component;

            if (box.dirtySize) {				
				if (anim) {
					// common config options shown with default values.
					var h = box.height - comp.header.getHeight();						
					comp.body.shift({
						height		: h,
						easing		: 'easeOut',
						duration	: .35							
					});
				}				
				else {
					comp.setSize(undefined, box.height);
				}
            }
        }
    },

    
    /**
     * @private
     * This should be called after onLayout of any BoxLayout subclass. If the target's overflow is not set to 'hidden',
     * we need to lay out a second time because the scrollbars may have modified the height and width of the layout
     * target. Having a Box layout inside such a target is therefore not recommended.
     * @param {Object} previousTargetSize The size and height of the layout target before we just laid out
     * @param {Ext.Container} container The container
     * @param {Ext.Element} target The target element
     */
    handleTargetOverflow: function(previousTargetSize, container, target) {
        var overflow = target.getStyle('overflow');

        if (overflow && overflow != 'hidden' &&!this.adjustmentPass) {
            var newTargetSize = this.getLayoutTargetSize();
            if (newTargetSize.width != previousTargetSize.width || newTargetSize.height != previousTargetSize.height){
                this.adjustmentPass = true;
                this.onLayout(container, target);
            }
        }

        delete this.adjustmentPass;
    },

    // private
    isValidParent : function(c, target){
        return this.innerCt && c.getPositionEl().dom.parentNode == this.innerCt.dom;
    },

    /**
     * @private
     * Returns all items that are both rendered and visible
     * @return {Array} All matching items
     */
    getVisibleItems: function(ct) {
        var ct  = ct || this.container,
            t   = ct.getLayoutTarget(),
            cti = ct.items.items,
            len = cti.length,

            i, c, items = [],
			// total number of collapsed child panels			
			co = 0;

        for (i = 0; i < len; i++) {            
			if((c = cti[i]).rendered && this.isValidParent(c, t) && c.hidden !== true){                
                if (c.collapsed === true) {
					co += c.getHeight();					
				}
				else {
					items.push(c);
				}
            }
        }
		
		/**
         * @private
         * @property collapsedOffset
         * @type Integer
         * Private calculated total number of hidden height in px;
         */
		this.collapsedOffset = co;
		
        return items;
    },

    // private
    renderAll : function(ct, target){
        if(!this.innerCt){
            // the innerCt prevents wrapping and shuffling while
            // the container is resizing
            this.innerCt = target.createChild({cls:this.innerCls});
            this.padding = this.parseMargins(this.padding);
        }
        Ext.layout.BoxLayout.superclass.renderAll.call(this, ct, this.innerCt);
    },

    getLayoutTargetSize : function(){
        var target = this.container.getLayoutTarget(), ret;
        if (target) {
            ret = target.getViewSize();

            // IE in strict mode will return a width of 0 on the 1st pass of getViewSize.
            // Use getStyleSize to verify the 0 width, the adjustment pass will then work properly
            // with getViewSize
            if (Ext.isIE && Ext.isStrict && ret.width == 0){
                ret =  target.getStyleSize();
            }

            ret.width -= target.getPadding('lr');
            ret.height -= target.getPadding('tb');
        }
        return ret;
    },

    // private	
    renderItem : function(c) {
	
		// Accordion layout specific attributes
		if(this.animate === false){
            c.animCollapse = false;
        }
        c.collapsible = true;
        if(this.autoWidth){
            c.autoWidth = true;
        }
        if(this.titleCollapse){
            c.titleCollapse = true;
        }
		if(this.hideCollapseTool){
            c.hideCollapseTool = true;
        }
		if(this.collapseFirst !== undefined){
            c.collapseFirst = this.collapseFirst;
        }
		
		// Box layout specific attributes
        if(Ext.isString(c.margins)){
            c.margins = this.parseMargins(c.margins);
        }else if(!c.margins){
            c.margins = this.defaultMargins;
        }
        Ext.layout.BoxLayout.superclass.renderItem.apply(this, arguments);
		
		c.on('expand', this.adjustSize, this);
		c.on('collapse', this.adjustSize, this);
    },	
    	
	adjustSize : function(p) {
		var items = this.getVisibleItems(),
            tSize = this.getLayoutTargetSize(),
			// Recalculate how much space each needs            
        	childBoxCache = this.calculateChildBoxes(items, tSize);
       
        // Resize the child components accordingly using Ext.Fx
        this.resizeChildComponents(childBoxCache.boxes, true);
	},
	
	onRemove: function(c){
        Ext.ux.tot2ivn.AccordionVboxLayout.superclass.onRemove.call(this, c);
        
        c.un('expand', this.adjustSize, this);
        c.un('collapse', this.adjustSize, this);
    },	
	
	// VBoxLayout calculation	
	align 	: 'left', // left, center, stretch, strechmax
    type	: 'vbox',

    /**
     * @private
     * Called by onRender just before the child components are sized and positioned. This resizes the innerCt
     * to make sure all child items fit within it. We call this before sizing the children because if our child
     * items are larger than the previous innerCt size the browser will insert scrollbars and then remove them
     * again immediately afterwards, giving a performance hit.
     * Subclasses should provide an implementation.
     * @param {Object} currentSize The current height and width of the innerCt
     * @param {Array} calculations The new box calculations of all items to be laid out
     */
    updateInnerCtSize: function(tSize, calcs) {
        var innerCtHeight = tSize.height,
            innerCtWidth  = calcs.meta.maxWidth + this.padding.left + this.padding.right;

        if (this.align == 'stretch') {
            innerCtWidth = tSize.width;
        } else if (this.align == 'center') {
            innerCtWidth = Math.max(tSize.width, innerCtWidth);
        }

        //we set the innerCt size first because if our child items are larger than the previous innerCt size
        //the browser will insert scrollbars and then remove them again immediately afterwards
        this.innerCt.setSize(innerCtWidth || undefined, innerCtHeight || undefined);
    },

    /**
     * @private
     * Calculates the size and positioning of each item in the VBox. This iterates over all of the rendered,
     * visible items and returns a height, width, top and left for each, as well as a reference to each. Also
     * returns meta data such as maxHeight which are useful when resizing layout wrappers such as this.innerCt.
     * @param {Array} visibleItems The array of all rendered, visible items to be calculated for
     * @param {Object} targetSize Object containing target size and height
     * @return {Object} Object containing box measurements for each child, plus meta data
     */
    calculateChildBoxes: function(visibleItems, targetSize) {
        var visibleCount = visibleItems.length,

            padding      = this.padding,
            topOffset    = padding.top,
            leftOffset   = padding.left,
            paddingVert  = topOffset  + padding.bottom,
            paddingHoriz = leftOffset + padding.right,

            width        = targetSize.width - this.scrollOffset,
            height       = targetSize.height - this.collapsedOffset,
            availWidth   = Math.max(0, width - paddingHoriz),

            isStart      = this.pack == 'start',
            isCenter     = this.pack == 'center',
            isEnd        = this.pack == 'end',

            nonFlexHeight= 0,
            maxWidth     = 0,
            totalFlex    = 0,

            //used to cache the calculated size and position values for each child item
            boxes        = [],

            //used in the for loops below, just declared here for brevity
            child, childWidth, childHeight, childSize, childMargins, canLayout, i, calcs, flexedHeight, horizMargins, stretchWidth;

            //gather the total flex of all flexed items and the width taken up by fixed width items
            for (i = 0; i < visibleCount; i++) {
                child = visibleItems[i];
                childHeight = child.height;
                childWidth  = child.width;
                canLayout   = !child.hasLayout && Ext.isFunction(child.doLayout);


                // Static height (numeric) requires no calcs
                if (!Ext.isNumber(childHeight)) {

                    // flex and not 'auto' height
                    if (child.flex && !childHeight) {
                        totalFlex += child.flex;

                    // Not flexed or 'auto' height or undefined height
                    } else {
                        //Render and layout sub-containers without a flex or width defined, as otherwise we
                        //don't know how wide the sub-container should be and cannot calculate flexed widths
                        if (!childHeight && canLayout) {
                            child.doLayout();
                        }

                        childSize = child.getSize();
                        childWidth = childSize.width;
                        childHeight = childSize.height;
                    }
                }

                childMargins = child.margins;

                nonFlexHeight += (childHeight || 0) + childMargins.top + childMargins.bottom;

                // Max width for align - force layout of non-layed out subcontainers without a numeric width
                if (!Ext.isNumber(childWidth)) {
                    if (canLayout) {
                        child.doLayout();
                    }
                    childWidth = child.getWidth();
                }

                maxWidth = Math.max(maxWidth, childWidth + childMargins.left + childMargins.right);

                //cache the size of each child component
                boxes.push({
                    component: child,
                    height   : childHeight || undefined,
                    width    : childWidth || undefined
                });
            }

            //the height available to the flexed items
            var availableHeight = Math.max(0, (height - nonFlexHeight - paddingVert));

            if (isCenter) {
                topOffset += availableHeight / 2;
            } else if (isEnd) {
                topOffset += availableHeight;
            }

            //temporary variables used in the flex height calculations below
            var remainingHeight = availableHeight,
                remainingFlex   = totalFlex;

            //calculate the height of each flexed item, and the left + top positions of every item
            for (i = 0; i < visibleCount; i++) {
                child = visibleItems[i];
                calcs = boxes[i];

                childMargins = child.margins;
                horizMargins = childMargins.left + childMargins.right;

                topOffset   += childMargins.top;

                if (isStart && child.flex && !child.height) {
                    flexedHeight     = Math.ceil((child.flex / remainingFlex) * remainingHeight);
                    remainingHeight -= flexedHeight;
                    remainingFlex   -= child.flex;

                    calcs.height = flexedHeight;
                    calcs.dirtySize = true;
                }

                calcs.left = leftOffset + childMargins.left;
                calcs.top  = topOffset;

                switch (this.align) {
                    case 'stretch':
                        stretchWidth = availWidth - horizMargins;
                        calcs.width  = stretchWidth.constrain(child.minWidth || 0, child.maxWidth || 1000000);
                        calcs.dirtySize = true;
                        break;
                    case 'stretchmax':
                        stretchWidth = maxWidth - horizMargins;
                        calcs.width  = stretchWidth.constrain(child.minWidth || 0, child.maxWidth || 1000000);
                        calcs.dirtySize = true;
                        break;
                    case 'center':
                        var diff = availWidth - calcs.width - horizMargins;
                        if (diff > 0) {
                            calcs.left = leftOffset + horizMargins + (diff / 2);
                        }
                }

                topOffset += calcs.height + childMargins.bottom;
            }

        return {
            boxes: boxes,
            meta : {
                maxWidth: maxWidth
            }
        };
    }
	// end
});

// Declare layout: 'ux.accordionvbox'
Ext.Container.LAYOUTS['ux.accordionvbox'] = Ext.ux.tot2ivn.AccordionVboxLayout;
