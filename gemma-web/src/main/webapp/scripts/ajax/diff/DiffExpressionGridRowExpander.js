/*
 * @author: keshav
 * @version: $Id$
 */


/* Ext.Gemma.DiffExpressionGridRowExpander constructor...
 */
Ext.Gemma.DiffExpressionGridRowExpander = function ( config ) {
	
	this.expandedElements = [];

	this.grid = config.grid;

	var superConfig = {
	};
	
	for ( property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.DiffExpressionGridRowExpander.superclass.constructor.call( this, superConfig );
	
};

/* instance methods...
 */
Ext.extend( Ext.Gemma.DiffExpressionGridRowExpander, Ext.grid.RowExpander, {
	
	beforeExpand : function (record, body, rowIndex) {
		if(this.fireEvent('beforeexpand', this, record, body, rowIndex) !== false){
		
			// don't do it twice.
			if ( this.expandedElements[ rowIndex ] ) {
				return true;
			}
			
			this.expandedElements[ rowIndex ] = [];
			
			var bodyEl = new Ext.Element( body );
			 
			bodyEl.addClass('diffExpressionGridRowExpanded'); // layout.css
			
	 		
			// Tab: experiment details. x-hide-display hides the div until we need to show it.
			var experimentDetails = bodyEl.createChild( { } );
	 		experimentDetails.addClass("x-hide-display");
			
			// Tab: design details
			var designDetails = bodyEl.createChild( {  } );  
			designDetails.addClass("x-hide-display");
			
			 var tabPanel = new Ext.TabPanel({
			 	renderTo: bodyEl,
    			activeTab: 0,
    			items: [{
        			title: "Experiment details",
        			contentEl: experimentDetails
    			},{
        			title:  "Design details",
        			contentEl: designDetails
    			}]
			 });
			
			this.expandedElements[ rowIndex ].push(tabPanel);
				
			//var experiment = record.data.
			
            return true;
         }
         return false;
    },
    
    clearCache : function () {
    	for ( var i=0; i<this.expandedElements.length; ++i ) {
			if ( this.expandedElements[i] ) {
				for ( var j=0; j<this.expandedElements[i].length; ++j ) {
					// grid.destroy() seems to be broken...
					try {
						this.expandedElements[i][j].destroy();
					} catch (e) {}
				}
				this.expandedElements[i] = null;
			}
    	}
    }
	
} );
