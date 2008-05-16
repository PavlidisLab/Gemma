/* Ext.Gemma.CoexpressionGridRowExpander constructor...
 */
Ext.Gemma.CoexpressionGridRowExpander = function ( config ) {
	
	this.expandedElements = [];

	this.grid = config.grid;

	var superConfig = {
	};
	
	for ( property in config ) {
		superConfig[property] = config[property];
	}
	Ext.Gemma.CoexpressionGridRowExpander.superclass.constructor.call( this, superConfig );
	
};

/* instance methods...
 */
Ext.extend( Ext.Gemma.CoexpressionGridRowExpander, Ext.grid.RowExpander, {
	
	beforeExpand : function (record, body, rowIndex) {
		if(this.fireEvent('beforeexpand', this, record, body, rowIndex) !== false){
		
			// don't do it twice.
			if ( this.expandedElements[ rowIndex ] ) {
				return true;
			}
			
			this.expandedElements[ rowIndex ] = [];
			
			var bodyEl = new Ext.Element( body );
			 
			bodyEl.addClass('coexpressionGridRowExpanded'); // layout.css
			
	 		
			// Tab: supporting data sets. x-hide-display hides the div until we need to show it.
			var supportingDsGridEl = bodyEl.createChild( { } );
	 		supportingDsGridEl.addClass("x-hide-display");
			// Tab: differential expression  
			
			var diffExGridEl = bodyEl.createChild( {  } );  
			diffExGridEl.addClass("x-hide-display");
			
			 var tabPanel = new Ext.TabPanel({
			 	renderTo: bodyEl,
    			activeTab: 0,
    			items: [{
        			title: "Supporting datasets",
        			contentEl: supportingDsGridEl
    			},{
        			title:  "Differential expression of " + record.data.foundGene.officialSymbol,
        			contentEl: diffExGridEl
    			}]
			 });
			
			this.expandedElements[ rowIndex ].push(tabPanel);
				
			var supporting = this.getSupportingDatasetRecords( record );
			
			var dsGrid = new Ext.Gemma.ExpressionExperimentGrid( {
				records : supporting,
				pageSize : 10,
    			width : 800,
    			renderTo : supportingDsGridEl
			} );
			 
		 	dsGrid.getStore().load( { params : { start : 0, limit : 10 } });
			
			var diffExGrid = new Ext.Gemma.DifferentialExpressionGrid( {
    			geneId : record.data.foundGene.id,
    			threshold : 0.01,
    			renderTo : diffExGridEl,
    			pageSize : 10,
    			width : 800
    		} );
    		
			var loadMask = new Ext.LoadMask( diffExGridEl, {
				removeMask : true,
				store : diffExGrid.getStore()
			} );
			loadMask.show();
			
			// Keep mouse events from propogating to the parent grid. See ExtJS forums topic "nested grids problem" (242878).
			dsGrid.getEl().swallowEvent(['mouseover','mousedown','click','dblclick']);
			diffExGrid.getEl().swallowEvent(['mouseover','mousedown','click','dblclick']);
			
            return true;
         }
         return false;
    },
    
 	getSupportingDatasetRecords : function( record  ) {
		var ids = record.data.supportingExperiments;
		var supporting = [];
		var ind = 0;
		// this is quite inefficient, but probably doesn't matter.
		for ( var i=0; i<ids.length; ++i ) {
			var id = ids[i];
			for (var j = 0; j < this.grid.datasets.length; j++) {
				var index = this.grid.datasets[j].id;
				if ( index === id ) {
					supporting.push( this.grid.datasets[ j ] );
					break;  
				}
			}
		}
		return supporting;
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
