/*
 NOTE:
 filter genes
 recalculate scores (orthogonal ones)
 filter columns
 recalculate scores
 until nothing changes (converges)
*/

Ext.namespace('Gemma.Metaheatmap');

/**
 *  TreeNode
 *  
 *   + children
 *   + parent
 *   + firstChild
 *   + lastChild
 *   + display.
 *   	pxlStart
 *   	pxlEnd
 *
 *
 *  + computeCoordinates
 *  + findByCoordinate
 *  
 */	
Gemma.Metaheatmap.TreeNode = function (config) {
	this.display = {};
	this.display.pxlStart = Number.MAX_VALUE;
	this.display.pxlEnd   = Number.MIN_VALUE;			
			
	this.parent = null;
	this.children = [];
	this.level = null;		
	
	this.computeCoordinates = function (currentCoordinate, newItemSize, root) {
		
		for (var i = 0; i < this.children.length; i++) {
			var child = this.children[i];  
			currentCoordinate = child.computeCoordinates (currentCoordinate, newItemSize, root);
		}		
		this.display.pxlStart = this.firstChild.display.pxlStart;
		this.display.pxlEnd   = this.lastChild.display.pxlEnd;
		this.display.pxlSize  = this.display.pxlEnd - this.display.pxlStart; 
		
		if (this.parent === null) {
			// we are at root. done.
			return currentCoordinate;
		}
		
		// we're first node at this level.
		if (root.display.levelToY.length == this.level) {
			root.display.levelToY.push(0); 
		}
		
		var textSize = CanvasTextFunctions.measure (null, 9, this.groupName);			
		
		var possibleY;
		// 	if the text doesn't fit, rotate label 90 degrees
		if (this.display.pxlSize > textSize) {
			possibleY = root.display.levelToY [this.level - 1] + 15;
			this.display.textOrientation = 'normal';
		} else {
			possibleY = root.display.levelToY [this.level - 1] + 80;
			this.display.textOrientation = 'side';
		}
			
		root.display.levelToY[this.level] = Math.max (root.display.levelToY[this.level], possibleY);
		//currentCoordinate += 1; // padding				
		
		return currentCoordinate;
	};

	this.findItemByCoordinate = function (coordinate)
	{
		if (this.display.pxlStart > coordinate) {
			return null;
		}
		
		if (this.display.pxlEnd < coordinate) {
			return null;
		}

		var child = this.binarySearchFn_ (coordinate);
		
		if (child === null) {
			return null;
		} else {
			return child.findItemByCoordinate (coordinate);						
		}
	};

	this.binarySearchFn_ = function (coordinate)
	{	
		var high = this.children.length - 1;
		var low = 0;

		while (low <= high) {
			mid = parseInt ((low + high) / 2, 10);
			if (this.children[mid].display.pxlStart <= coordinate && this.children[mid].display.pxlEnd >= coordinate) {
				// hit!
				return this.children[mid];
			}
			if (coordinate < this.children[mid].display.pxlStart) {
				// miss! element is probably on the left
				high = mid - 1;
			}
			if (coordinate > this.children[mid].display.pxlEnd) {
				// miss! element is probably on the right
				low = mid + 1;
			}
		}
		return null;
	};	
};		

/**
 * TreeLeafNode 
 * + display.itemSize
 * + items
 * 
 * + computeCoordinates
 * + findByCoordinate
 * + 
 * 
 */
Gemma.Metaheatmap.TreeLeafNode = function (items) {
	this.display = {};
	this.display.pxlStart = Number.MAX_VALUE;
	this.display.pxlEnd   = Number.MIN_VALUE;			
			
	this.level  = 1;	
	this.items  = items;
	this.parent = null;
	
	this.computeCoordinates = function (currentCoordinate, newItemSize, root) {
		this.display.itemSize = newItemSize;
		var item;			
		for (var i = 0; i < this.items.length; i++) {
			item = this.items[i];
			item.display = {};
			item.display.pxlStart = currentCoordinate;						
			currentCoordinate += newItemSize;					
			item.display.pxlEnd  = currentCoordinate;
			item.display.pxlSize = item.display.pxlEnd - item.display.pxlStart; 
		}			
		this.display.pxlStart = this.items[0].display.pxlStart;
		this.display.pxlEnd   = currentCoordinate;		
		this.display.pxlSize  = this.display.pxlEnd - this.display.pxlStart;

		if (this.parent === null) {
			return currentCoordinate;
		}

		if (root.display.levelToY.length === this.level) {
			root.display.levelToY.push(0);
		}

		var textSize = CanvasTextFunctions.measure (null, 9, this.groupName);		
		
		if ((this.display.pxlSize) > textSize) {
			this.display.textOrientation = 'normal';
			root.display.levelToY[this.level] = Math.max (root.display.levelToY[this.level], 135);
		} else {
			this.display.textOrientation = 'side';
			root.display.levelToY[this.level] = Math.max (root.display.levelToY[this.level], 200);
		}

		//currentCoordinate += 1; // padding				
		return currentCoordinate;
	};
	
	this.findItemByCoordinate = function (coordinate)
	{
		if (this.display.pxlStart > coordinate) {
			return null;
		}
		if (this.display.pxlEnd < coordinate) {
			return null;
		}

		var index = Math.floor ((coordinate - this.display.pxlStart) / this.display.itemSize);
		return this.items[index];		
	};
		
};		

/**
 * 
 *  * SortedFilteredTree
 *  Constructed from genes/conditions array and provided filter/sort/group by settings of the form:
 *  [{'filterFn': }, {'filterFn': }, {'filterFn': }] and [{'sortFn': , 'groupBy': }, {'sortFn': , 'groupBy': }, {'sortFn': }]
 *  
 * Lifecycle:
 *  . Zoom function can be applied multiple times (affects .display.ready properties)
 *  . Can be drawn multiple times (affects .display.drawn) (orientation can be changed)
 *  
 *   
 *  ? (if we add something else apart from applyZoom, consider: render () --? recalculate all sizes/coordinates then can be drawn
 *  + applyZoom (newItemSize)
 *  + findItemByCoordinate (coordinate)
 *  
 *  
 *  + items  -- array of items in the display order
 *  + root   -- root node
 *  + rendered -- true/false whether coordinates have been computed
 *  + height
 *  
 */
Gemma.Metaheatmap.SortedFilteredTree = function (items, sortSettings, filterSettings) {
	// Display properties for gene/condition labels.
	this.display = {};
	this.display.size = {};	
	this.display.size.width = 0;
	this.display.size.height = 0;
	
	// Setting that were used to construct the tree.
	this.settings = {};
	this.settings.sort   = sortSettings;
	this.settings.filter = filterSettings;

	// All visible items (ordered so that's it's trivial to iterate over them to draw the heatmap).
	this.items = []; 
		
	this.applyZoom = function (newItemSize)
	{	
		if (this.isEmpty) {
			return;
		}
		
		this.root.display.levelToY = [];
		this.root.display.levelToY.push(120);

		this.display.size.width  = this.root.computeCoordinates (0, newItemSize, this.root);// - this.settings.sort.length;
		this.display.size.height = this.root.display.levelToY [this.root.level-1];
	};
				
	this.findItemByCoordinate = function (coordinate)
	{
		if (this.isEmpty) {
			return null;
		}
		return this.root.findItemByCoordinate (coordinate);
	};
		
	this.filter_ = function (items)
	{
		if (this.settings.filter.length === 0) { return items; }
		var filteredItems = [];
		for (var i = 0; i < items.length; i++) {
			var keep = true;
			for (var f = 0; f < this.settings.filter.length; f++) {
				var filterFn = this.settings.filter[f].filterFn;
				if ( filterFn (items[i]) ) {
					keep = false;
					break;
				}
			}
			if (keep) {
				filteredItems.push (items[i]);
			}
		}			
		return filteredItems;
	};
	
	// [{'sortFn': , 'groupBy': }, {'sortFn': , 'groupBy': }, {'sortFn': ,'groupBy': null }] <- has to end like this
	this.constructTree_ = function (items, treeDepth)
	{
		var node, child; 
				
		var sortFn  		= this.settings.sort [treeDepth].sortFn;
		var groupByProperty = this.settings.sort [treeDepth].groupBy;
		
		treeDepth++;

		items.sort (sortFn);
		
		if (groupByProperty !== null) {
			node = new Gemma.Metaheatmap.TreeNode();
					
			var index;
			var groupStartIndex = 0;
			for (index = 1; index < items.length; index++) {
				if ( items[index][groupByProperty] !== items[index-1][groupByProperty] ) {
					child = this.constructTree_ (items.slice (groupStartIndex, index), treeDepth);				
					child.parent = node;
					child.groupName = items[index-1][groupByProperty];
					if (child.groupName === null) {child.groupName = 'null';}
					node.children.push (child);
					groupStartIndex = index;
				}
			}		
			child = this.constructTree_ (items.slice (groupStartIndex, index), treeDepth);
			child.parent = node;
			child.groupName = items[index-1][groupByProperty];
			if (child.groupName === null) {child.groupName = 'null';}
			node.children.push (child);
			node.firstChild = node.children[0];
			node.lastChild = child;
			node.level = child.level + 1;
		} else { // We are at TreeLeafNode level
			node = new Gemma.Metaheatmap.TreeLeafNode (items);						
			this.items = this.items.concat (items);
		}
				
		return node;
	};
  	
	// Construct tree from items. 	
	var filteredItems = this.filter_ (items);
	this.numFiltered = items.length - filteredItems.length;
	if (filteredItems.length == 0) {
		this.isEmpty = true;
	} else {
		this.root = this.constructTree_  (filteredItems, 0);
		this.isEmpty = false;
	}
};