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
			 
			bodyEl.addClass('DiffExpressionGridRowExpanded'); // layout.css
			
	 		
			// Tab: supporting data sets. x-hide-display hides the div until we need to show it.
			var supportingDsGridEl = bodyEl.createChild( { } );
	 		supportingDsGridEl.addClass("x-hide-display");
			// Tab: differential expression  
			
			var diffExGridEl = bodyEl.createChild( {  } );  
			diffExGridEl.addClass("x-hide-display");
		
			var supporting = this.getSupportingDatasetRecords( record );

			var dsGrid = new Ext.Gemma.DiffExpressionExperimentGrid( {
				records : supporting,
    			width : 800,
    			renderTo : supportingDsGridEl
			} );
			
			this.expandedElements[ rowIndex ].push(dsGrid);
				

			
	
		 	dsGrid.getStore().load( );
    		
			
			// Keep mouse events from propogating to the parent grid. See ExtJS forums topic "nested grids problem" (242878).
			dsGrid.getEl().swallowEvent(['mouseover','mousedown','click','dblclick']);
			
            return true;
         }
         return false;
    },
    
 	getSupportingDatasetRecords : function( record  ) {
		var ids = record.data.activeExperiments;
		return ids;
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
