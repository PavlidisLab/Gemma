/**
 * This file contains various overrides for ext 3.4 elements
 * 
 * Includes overrides to:
 * - textArea to allow control of word wrapping
 * - Ext.data.Store to handle multiple independent filters
 * - ColumnModel to allow adding and removing columns
 * - vbox to handle collapsible elements
 * - sliders to make them work in IE9
 * - grid to make cell text selectable
 * 
 */




// Override textArea to allow control of word wrapping
// just adds a wordWrap config field to textArea
// from here: http://www.sencha.com/forum/showthread.php?52122-preventing-word-wrap-in-textarea
// needed for download window of diff ex viz
Ext.override(Ext.form.TextArea, {
    initComponent: Ext.form.TextArea.prototype.initComponent.createSequence(function(){
        Ext.applyIf(this, {
            wordWrap: true
        });
    }),
    
    onRender: Ext.form.TextArea.prototype.onRender.createSequence(function(ct, position){
        this.el.setOverflow('auto');
        if (this.wordWrap === false) {
            if (!Ext.isIE) {
                this.el.set({
                    wrap: 'off'
                });
            }
            else {
                this.el.dom.wrap = 'off';
            }
        }
        if (this.preventScrollbars === true) {
            this.el.setStyle('overflow', 'hidden');
        }
    })
});


/**
 * Add methods to Ext.data.Store to handle multiple independent filters
 * should be able to add/remove and activate/deactivate filters by name
 * 
 * filterObjects passed to store will be js objects with 3 fields: 
 * fn : the filtering function. Takes the row's record as param and returns 'true' if the row should be shown, false otherwise
 * name : unique name of the filter so that it can be removed or deactivated
 * active : boolean indicating whether the filter should be applied or not
 * 
 * (I'm not actually overriding anything but Ext.apply doesn't work)
 */
Ext.override(Ext.data.Store, {
    multiFilters:[],
	/**
	 * add a filter to the multi-filter, must have 3 fields: fn, name, active
	 * returns true if successfully added, false if fields are missing
	 */
	addMultiFilter : function(filterObj){
		if(typeof(filterObj) !== undefined &&
			typeof(filterObj.fn) !== undefined && 
			typeof(filterObj.name) !== undefined && 
			typeof(filterObj.active) !== undefined){
			this.multiFilters.push(filterObj);
			return true;
		}
		return false;
	},
	
	removeMultiFilter : function(filterName){
		var index = this.getFilterObjIndexByName(filterName);
		if (index !== null && index > -1) {
			this.multiFilters.splice(index, 1);
		}
	},
	
	activateMultiFilter : function(filterName){
		var fo = this.getFilterObjByName(filterName);
		if(fo !== null){
			fo.active = true;
		}
	},
	
	deactivateMultiFilter : function(filterName){
		var fo = this.getFilterObjByName(filterName);
		if(fo !== null){
			fo.active = false;
		}
	},
	/**
	 * go through each filter, if any filter wants the row hidden, hide it
	 * otherwise show it
	 */
	applyMultiFilters: function(){
		this.filterBy(function(record, id){
			var j;
			for (j = 0; j < this.multiFilters.length; j++) {
				if (this.multiFilters[j].active && !this.multiFilters[j].fn(record)) {
					return false;
				}
			}
			return true;
		}, this);
		
	},
		
	clearMultiFilters : function (){
		this.multiFilters = [];
		
	},
	// could do this more efficiently with a map, but the array will probably always be small ( < 5 elements)
	getFilterObjByName : function (name){
		var i;
		for(i=0;i<this.multiFilters.length;i++){
			if(this.multiFilters[i].name && this.multiFilters[i].name === name){
				return this.multiFilters[i];
			}
		}
		return null;
	},
	// could do this more efficiently with a map, but the array will probably always be small ( < 5 elements)
	getFilterObjIndexByName : function (name){
		var i;
		for(i=0;i<this.multiFilters.length;i++){
			if(this.multiFilters[i].name && this.multiFilters[i].name === name){
				return i;
			}
		}
		return null;
	}
	
});


/* Override ColumnModel to allow adding and removing columns
 * http://www.sencha.com/forum/showthread.php?53009-Adding-removing-fields-and-columns
 * 
 * usage example: 
 * 
  var grid = new Ext.grid.GridPanel({
	store: new Ext.data.SimpleStore({
		fields: ['A', 'B'],
		data: [['ABC', 'DEF'], ['GHI', 'JKL']]
	}),
	columns: [
		{header: 'A', dataIndex: 'A'},
		{header: 'B', dataIndex: 'B'}
	]});
	new Ext.Viewport({
		layout: 'fit',
		items: grid
	});
	grid.addColumn('C');
	grid.addColumn({name: 'D', defaultValue: 'D'}, {header: 'D', dataIndex: 'D'});
	grid.removeColumn('B');
 */ 
Ext.override(Ext.data.Store,{
	addField: function(field){
		field = new Ext.data.Field(field);
		this.recordType.prototype.fields.replace(field);
		if(typeof field.defaultValue != 'undefined'){
			this.each(function(r){
				if(typeof r.data[field.name] == 'undefined'){
					r.data[field.name] = field.defaultValue;
				}
			});
		}
	},
	removeField: function(name){
		this.recordType.prototype.fields.removeKey(name);
		this.each(function(r){
			delete r.data[name];
			if(r.modified){
				delete r.modified[name];
			}
		});
	}
});
Ext.override(Ext.grid.ColumnModel,{
	addColumn: function(column, colIndex){
		if(typeof column == 'string'){
			column = {header: column, dataIndex: column};
		}
		var config = this.config;
		this.config = [];
		if(typeof colIndex == 'number'){
			config.splice(colIndex, 0, column);
		}else{
			colIndex = config.push(column);
		}
		this.setConfig(config);
		return colIndex;
	},
	removeColumn: function(colIndex){
		var config = this.config;
		this.config = [config[colIndex]];
		config.splice(colIndex, 1);
		this.setConfig(config);
	}
});
Ext.override(Ext.grid.GridPanel,{
	addColumn: function(field, column, colIndex){
		if(!column){
			if(field.dataIndex){
				column = field;
				field = field.dataIndex;
			} else{
				column = field.name || field;
			}
		}
		this.store.addField(field);
		return this.colModel.addColumn(column, colIndex);
	},
	removeColumn: function(name, colIndex){
		this.store.removeField(name);
		if(typeof colIndex != 'number'){
			colIndex = this.colModel.findColumnIndex(name);
		}
		if(colIndex >= 0){
			this.colModel.removeColumn(colIndex);
		}
	}
});

// Override the class so that tooltip will be rendered automatically when renderToolTip is set to true.
Ext.override(Ext.grid.Column,{
    renderer : function(value, metaData, record, rowIndex, colIndex, store){
    	if (this.renderToolTip && metaData) {
		    metaData.attr = 'ext:qtip="' + value + '"';
		    return value;
    	}
        return value;
    }
});


/**
 * overrides so that vbox can handle collapsible elements
 * see http://www.sencha.com/forum/showthread.php?98165-vbox-layout-with-two-grids-grid-collapse-does-not-stretch-non-collapsed-grid&p=463266&viewfull=1#post463266
 * Other methods must be called by child components to make collapsing work
 * see expressionExperiments.jsp for an example
 * 
 */
Ext.override(Ext.layout.BoxLayout, {
    getVisibleItems: function(ct) {
        var ct  = ct || this.container,
            t   = ct.getLayoutTarget(),
            cti = ct.items.items,
            len = cti.length,

            i, c, items = [];

        for (i = 0; i < len; i++) {
            if((c = cti[i]).rendered && this.isValidParent(c, t) && c.hidden !== true){
                items.push(c);
            }
        }

        return items;
    }
});
Ext.override(Ext.layout.VBoxLayout, {
    calculateChildBoxes: function(visibleItems, targetSize) {
        var visibleCount = visibleItems.length,

            padding      = this.padding,
            topOffset    = padding.top,
            leftOffset   = padding.left,
            paddingVert  = topOffset  + padding.bottom,
            paddingHoriz = leftOffset + padding.right,

            width        = targetSize.width - this.scrollOffset,
            height       = targetSize.height,
            availWidth   = Math.max(0, width - paddingHoriz),

            isStart      = this.pack == 'start',
            isCenter     = this.pack == 'center',
            isEnd        = this.pack == 'end',

            nonFlexHeight= 0,
            maxWidth     = 0,
            totalFlex    = 0,
            desiredHeight= 0,
            minimumHeight= 0,

            //used to cache the calculated size and position values for each child item
            boxes        = [],
            
            //used in the for loops below, just declared here for brevity
            child, childWidth, childHeight, childSize, childMargins, canLayout, i, calcs, flexedHeight, 
            horizMargins, vertMargins, stretchWidth, length;

        //gather the total flex of all flexed items and the width taken up by fixed width items
        for (i = 0; i < visibleCount; i++) {
            child = visibleItems[i];
            childHeight = child.collapsed ? child.getHeight() : child.height;
            childWidth  = child.width;
            canLayout   = !child.hasLayout && typeof child.doLayout == 'function';

            // Static height (numeric) requires no calcs
            if (typeof childHeight != 'number') {

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
            vertMargins  = childMargins.top + childMargins.bottom;

            nonFlexHeight += vertMargins + (childHeight || 0);
            desiredHeight += vertMargins + (child.flex ? child.minHeight || 0 : childHeight);
            minimumHeight += vertMargins + (child.minHeight || childHeight || 0);

            // Max width for align - force layout of non-layed out subcontainers without a numeric width
            if (typeof childWidth != 'number') {
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
                
        var shortfall = desiredHeight - height,
            tooNarrow = minimumHeight > height;

        //the height available to the flexed items
        var availableHeight = Math.max(0, (height - nonFlexHeight - paddingVert));
        
        if (tooNarrow) {
            for (i = 0, length = visibleCount; i < length; i++) {
                boxes[i].height = visibleItems[i].minHeight || visibleItems[i].height || boxes[i].height;
            }
        } else {
            //all flexed items should be sized to their minimum width, other items should be shrunk down until
            //the shortfall has been accounted for
            if (shortfall > 0) {
                var minHeights = [];

                
/**
                 * When we have a shortfall but are not tooNarrow, we need to shrink the height of each non-flexed item.
                 * Flexed items are immediately reduced to their minHeight and anything already at minHeight is ignored.
                 * The remaining items are collected into the minHeights array, which is later used to distribute the shortfall.
                 */
                for (var index = 0, length = visibleCount; index < length; index++) {
                    var item      = visibleItems[index],
                        minHeight = item.minHeight || 0;

                    //shrink each non-flex tab by an equal amount to make them all fit. Flexed items are all
                    //shrunk to their minHeight because they're flexible and should be the first to lose height
                    if (item.flex) {
                        boxes[index].height = minHeight;
                    } else {
                        minHeights.push({
                            minHeight: minHeight, 
                            available: boxes[index].height - minHeight,
                            index    : index
                        });
                    }
                }

                //sort by descending minHeight value
                minHeights.sort(function(a, b) {
                    return a.available > b.available ? 1 : -1;
                });

                /*
                 * Distribute the shortfall (difference between total desired with of all items and actual height available)
                 * between the non-flexed items. We try to distribute the shortfall evenly, but apply it to items with the
                 * smallest difference between their height and minHeight first, so that if reducing the height by the average
                 * amount would make that item less than its minHeight, we carry the remainder over to the next item.
                 */
                for (var i = 0, length = minHeights.length; i < length; i++) {
                    var itemIndex = minHeights[i].index;

                    if (itemIndex == undefined) {
                        continue;
                    }

                    var item      = visibleItems[itemIndex],
                        box       = boxes[itemIndex],
                        oldHeight  = box.height,
                        minHeight  = item.minHeight,
                        newHeight  = Math.max(minHeight, oldHeight - Math.ceil(shortfall / (length - i))),
                        reduction = oldHeight - newHeight;

                    boxes[itemIndex].height = newHeight;
                    shortfall -= reduction;
                }
            } else {
                //temporary variables used in the flex height calculations below
                var remainingHeight = availableHeight,
                    remainingFlex   = totalFlex;
                
                //calculate the height of each flexed item
                for (i = 0; i < visibleCount; i++) {
                    child = visibleItems[i];
                    calcs = boxes[i];

                    childMargins = child.margins;
                    horizMargins = childMargins.left + childMargins.right;

                    if (isStart && child.flex && !child.collapsed && !child.height) {
                        flexedHeight     = Math.ceil((child.flex / remainingFlex) * remainingHeight);
                        remainingHeight -= flexedHeight;
                        remainingFlex   -= child.flex;

                        calcs.height = flexedHeight;
                        calcs.dirtySize = true;
                    }
                }
            }
        }

        if (isCenter) {
            topOffset += availableHeight / 2;
        } else if (isEnd) {
            topOffset += availableHeight;
        }

        //finally, calculate the left and top position of each item
        for (i = 0; i < visibleCount; i++) {
            child = visibleItems[i];
            calcs = boxes[i];

            childMargins = child.margins;
            topOffset   += childMargins.top;
            horizMargins = childMargins.left + childMargins.right;
            

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
                maxWidth     : maxWidth,
                nonFlexHeight: nonFlexHeight,
                desiredHeight: desiredHeight,
                minimumHeight: minimumHeight,
                shortfall    : desiredHeight - height,
                tooNarrow    : tooNarrow
            }
        };
    }
});
// end of vbox-collasible override


/**
 * override to make sliders work in IE9
 * from http://www.sencha.com/forum/showthread.php?141254-Ext.Slider-not-working-properly-in-IE9
 */
Ext.override(Ext.dd.DragTracker, {
    onMouseMove: function (e, target) {
        var isIE9 = Ext.isIE && (/msie 9/.test(navigator.userAgent.toLowerCase())) && document.documentMode != 6;
        if (this.active && Ext.isIE && !isIE9 && !e.browserEvent.button) {
            e.preventDefault();
            this.onMouseUp(e);
            return;
        }
        e.preventDefault();
        var xy = e.getXY(), s = this.startXY;
        this.lastXY = xy;
        if (!this.active) {
            if (Math.abs(s[0] - xy[0]) > this.tolerance || Math.abs(s[1] - xy[1]) > this.tolerance) {
                this.triggerStart(e);
            } else {
                return;
            }
        }
        this.fireEvent('mousemove', this, e);
        this.onDrag(e);
        this.fireEvent('drag', this, e);
    }
});

