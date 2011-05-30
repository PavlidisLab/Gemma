Ext.namespace('Gemma');

Gemma.MetaHeatmapResizablePanelBase = Ext.extend(Ext.Panel, {	

	initComponent: function() {
		Ext.apply(this, {
			border: false,
			bodyBorder: false,
			layout: 'hbox',
			layoutConfig: {
				defaultMargins: {top:0, right: 0, bottom: 0, left:0}
			},
						
			_hidden: false,		
			_columnsHidden : 0,
			applicationRoot: this.applicationRoot,		// root of metaheatmap app. Usefull to access various components.
			
			//
			changePanelWidthBy: function ( delta ) { // - value : shrink, + value : expand				
				this.setWidth( this.getWidth() + delta );				
				//propagate call until parent is not MetaHeatmapResizablePanelBase  TODO: (good candidate for event!)
				var parent = this.ownerCt;				
				if ( parent instanceof Gemma.MetaHeatmapResizablePanelBase ) {
					parent.changePanelWidthBy(delta);
				}
			},
			// 
			sortByProperty: function ( property ) {
			    return function ( a, b ) {
			        if (typeof a[property] == "number") {
			            return (a[property] - b[property]);
			        } else {
			            return ((a[property] < b[property]) ? -1 : ((a[property] > b[property]) ? 1 : 0));
			        }
			    };
			}			
		});
		Gemma.MetaHeatmapResizablePanelBase.superclass.initComponent.apply(this, arguments);		
	},
	
	onRender: function() {				
		Gemma.MetaHeatmapResizablePanelBase.superclass.onRender.apply(this, arguments);
	},
	
	
	filterColumns: function( filteringFn ) {
		var newWidth = 0;	
		var numberHidden = 0;			
		this.items.each( function() {
			numberHidden = numberHidden + this.filterColumns( filteringFn );		  	
			newWidth = newWidth + this.getWidth(); 
		} );
		this._columnsHidden = numberHidden; // this is stored at the experiment group level
		this.setWidth( newWidth );
		if ( newWidth === 0 ) {this._hidden = true;}
		else {this._hidden = false;}
		return numberHidden;
	},						

	refresh: function() {
		this.items.each(function() {if ( this._hidden !== true ) {this.refresh();}});		
	}
	
});


// Analysis Column Group
// There could be multiple analyses associated with each experiment.
//
Gemma.MetaHeatmapAnalysisColumnGroup = Ext.extend ( Gemma.MetaHeatmapResizablePanelBase, {	


	initComponent: function() {
		Ext.apply(this, {
			dataColumns : this.dataColumns,
						
			datasetGroupIndex : this.datasetGroupIndex,
			columnGroupIndex: this.columnGroupIndex,
			columnGroupName: this.datasetName,
			analysisId: this.analysisId,
			
			overallDifferentialExpressionScore: null,
			specificityScore: null,		
			_columnsHidden : 0									
		});

		Gemma.MetaHeatmapAnalysisColumnGroup.superclass.initComponent.apply(this, arguments);
				
		for (var i = 0; i < this.dataColumns.length; i++) {
			var lastColumnInGroup = false;
			this.add ( new Gemma.MetaHeatmapExpandableColumn(
								{ applicationRoot: this.applicationRoot,
								  height: this.height,
								  dataColumn : this.dataColumns[i],
								  columnIndex: i,
								  columnGroupIndex: this.columnGroupIndex,
								  datasetGroupIndex: this.datasetGroupIndex,
								  lastColumnInGroup: lastColumnInGroup }) );
		}
		
		var initialWidth = this.dataColumns.length * (Gemma.MetaVisualizationConfig.cellWidth + Gemma.MetaVisualizationConfig.columnSeparatorWidth);		
		this.setSize ( initialWidth, this.height );
	},
	
	onRender: function() {
		Gemma.MetaHeatmapAnalysisColumnGroup.superclass.onRender.apply(this, arguments);				
	}, 
	
	
	filterColumns: function ( filteringFn ) {
		
		var myNumberHidden = 0;
		this.items.each( function() { 
			if(this.getWidth() === 0){
					myNumberHidden++; // if a column is already hidden
				}
			if(filteringFn( this ) === null){ // not affected
				
			}else{
				if ( filteringFn( this ) ) { // hide
					if(this.getWidth() !== 0){// if not already hidden
						this.setWidth(0);
						this.hide();
						this._columnHidden = true;
						this.updateParentsScores();
						myNumberHidden++;
					}
				}else{ // show
					if(this.getWidth() === 0){// if not already shown
						this.setWidth(Gemma.MetaVisualizationConfig.cellWidth + Gemma.MetaVisualizationConfig.columnSeparatorWidth);
						this.show();
						this._columnHidden = false;
						this.updateParentsScores();
						myNumberHidden--;
					}
				}
			}
			
		});
		//	TODO: any better way to get newWidth??
		var newWidth = (this.dataColumns.length - myNumberHidden) * (Gemma.MetaVisualizationConfig.cellWidth + Gemma.MetaVisualizationConfig.columnSeparatorWidth);		
		this.setWidth( newWidth );
		
		this._numberOfColumnsHidden111 = myNumberHidden;
		if (myNumberHidden == this.dataColumns.length) {
			this.setWidth(0);
			this.hide();
			this._hidden = true;
		}else{
			this.show();
			this._hidden = false;			
		}
		return myNumberHidden;
	}							
});


// We group by dataset right now. We might want to make any other groupings available.
//
Gemma.MetaHeatmapDatasetColumnGroup = Ext.extend ( Gemma.MetaHeatmapResizablePanelBase, {	
	initComponent: function() {
		Ext.apply(this, {
			
			dataColumns : this.dataColumns,		
			_columnsHidden : 0,
			
			datasetGroupIndex : this.datasetGroupIndex,
			
			datasetColumnGroupIndex: this.datasetColumnGroupIndex,			
			datasetName: this.datasetName,			
			
			overallDifferentialExpressionScore: null,
			specificityScore: null												
		});

		Gemma.MetaHeatmapDatasetColumnGroup.superclass.initComponent.apply(this, arguments);

		var initialWidth = 0;
		
		// sort columns by analysis id
		this.dataColumns.sort(this.sortByProperty("analysisId"));
		// put each analysis in a separate column group
		iGroupIndex = 0;
		var tempColumns = [];
		var columnGroup;
		var analysisId = this.dataColumns[0].analysisId;
		tempColumns.push(this.dataColumns[0]);
		for (var i = 1; i < this.dataColumns.length; i++) {			
			if ( analysisId == this.dataColumns[i].analysisId ) {
				tempColumns.push ( this.dataColumns[i] );
			} else {
				columnGroup = new Gemma.MetaHeatmapAnalysisColumnGroup({applicationRoot: this.applicationRoot,
											height: this.height,
											analysisId: analysisId,
											dataColumns: tempColumns,
											columnGroupIndex: iGroupIndex,
											datasetGroupIndex: this.datasetGroupIndex});				
				this.add(columnGroup);
				iGroupIndex++;

				initialWidth += columnGroup.width;
				datasetName = this.dataColumns[i].analysisId;
				tempColumns = [];
				tempColumns.push(this.dataColumns[i]);
			}			
		}
		columnGroup = new Gemma.MetaHeatmapAnalysisColumnGroup({	applicationRoot: this.applicationRoot,
						  				height: this.height,
										analysisId: analysisId,
										dataColumns: tempColumns,
										columnGroupIndex: iGroupIndex,															
										datasetGroupIndex: this.datasetGroupIndex});				
		this.add(columnGroup);				
		initialWidth += columnGroup.width;

		this.setSize ( initialWidth, this.height );
	},
	
	onRender: function() {
		Gemma.MetaHeatmapDatasetColumnGroup.superclass.onRender.apply(this, arguments);		
	}
	
});

//
//
//
Gemma.MetaHeatmapDatasetGroupPanel = Ext.extend(Gemma.MetaHeatmapResizablePanelBase, {	
	initComponent: function() {
		Ext.apply(this, {
			datasetGroupIndex: this.datasetGroupIndex,
			dataColumns: this.dataFactorColumns			
		});

		Gemma.MetaHeatmapDatasetGroupPanel.superclass.initComponent.apply(this, arguments);
		
		var initialWidth = 0;		
		// Sort columns by dataset name
		this.dataColumns.sort( this.sortByProperty("datasetName") );
		// Put each dataset in a separate column group
		// This code is used twice. How to reuse it better?
		iGroupIndex = 0;
		var tempColumns = [];
		var columnGroup;
		var datasetName = this.dataColumns[0].datasetName;
		tempColumns.push(this.dataColumns[0]);
		for (var i = 1; i < this.dataColumns.length; i++) {			
			if (datasetName == this.dataColumns[i].datasetName) {
				tempColumns.push(this.dataColumns[i]);
			} else {
				columnGroup = new Gemma.MetaHeatmapDatasetColumnGroup(
											{ applicationRoot: this.applicationRoot,
											  height: this.height,
											  datasetName: datasetName,
											  dataColumns: tempColumns,
											  datasetColumnGroupIndex: iGroupIndex,
											  datasetGroupIndex: this.datasetGroupIndex } );				
				this.add(columnGroup);
				iGroupIndex++;

				initialWidth += columnGroup.width;
				datasetName = this.dataColumns[i].datasetName;
				tempColumns = [];
				tempColumns.push( this.dataColumns[i] );
			}			
		}
		columnGroup = new Gemma.MetaHeatmapDatasetColumnGroup(
											{ applicationRoot: this.applicationRoot,
											  height: this.height,
											  datasetName: datasetName,
											  dataColumns: tempColumns,
											  columnGroupIndex: iGroupIndex,															
											  datasetGroupIndex: this._datasetGroupIndex } );				
		this.add( columnGroup );				
		initialWidth += columnGroup.width;

		this.setWidth( initialWidth );
	},
	
	onRender: function() {				
		Gemma.MetaHeatmapDatasetGroupPanel.superclass.onRender.apply(this, arguments);
	}
	
});