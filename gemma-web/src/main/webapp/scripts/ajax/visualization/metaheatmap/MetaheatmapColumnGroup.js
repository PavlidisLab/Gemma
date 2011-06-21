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
			isFiltered: false,
			_columnsHidden : 0,
			applicationRoot: this.applicationRoot,	// root of metaheatmap app. Usefull to access various components.
			
			getGroupFromX : function(x) {
				var groupIndex = 0;
				var xEnd = 0;
				var xStart = 0;
				while (groupIndex < this.items.getCount()) {
					xEnd = xEnd + this.items.get(groupIndex).getWidth();
					if (x > xStart && x < xEnd ) {return this.items.get(groupIndex);}
					xStart = xEnd;
					datasetGroupIndex++;
				}
				return null;				
			},
			
			changePanelWidthBy: function ( delta ) { // - value : shrink, + value : expand				
				this.setWidth( this.getWidth() + delta );				
				//propagate call until parent is not MetaHeatmapResizablePanelBase  TODO: (good candidate for event?)
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
		
		for (var i = 0; i < this.items.getCount(); i++ ) {
			this.items.get(i).filterColumns( filteringFn );		  	
			newWidth = newWidth + this.items.get(i).getWidth();			
		}
		
		this.setWidth( newWidth );
		this.isFiltered = ( newWidth === 0 )? true : false;
	},						
	
	refresh : function() {
		this.items.each(function() { this.refresh(); });
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
			this.add ( new Gemma.MetaHeatmapExpandableColumn(
								{ applicationRoot: this.applicationRoot,
								  height: this.height,
								  dataColumn : this.dataColumns[i],
								  columnIndex: i,
								  columnGroupIndex: this.columnGroupIndex,
								  datasetGroupIndex: this.datasetGroupIndex
								}) );
		}
		
		var initialWidth = this.dataColumns.length * (Gemma.MetaVisualizationConfig.cellWidth + Gemma.MetaVisualizationConfig.columnSeparatorWidth);		
		this.setSize ( initialWidth, this.height );
	},
	
	onRender: function() {
		Gemma.MetaHeatmapAnalysisColumnGroup.superclass.onRender.apply(this, arguments);				
	}, 
	
	
	filterColumns: function ( filteringFn ) {
		var newWidth = 0;
		
		for (var i = 0; i < this.items.getCount(); i++ ) {
			if ( filteringFn( this.items.get(i) ) === null ) { // not affected
				//hack until applying multiple filters at once is implemented				
			} else {
				if ( filteringFn( this.items.get(i) ) ) { // hide
					this.items.get(i).filterHide();
				} else {
					this.items.get(i).filterShow();
				}
				this.items.get(i).updateParentsScores();
			}
			newWidth = newWidth + this.items.get(i).getWidth();			
		}
				
		this.setWidth( newWidth );
		this.isFiltered = (newWidth === 0)? true:false;						
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
			datasetShortName : this.datasetShortName,
			
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
			margins : {
			top : 0,
			right : Gemma.MetaVisualizationConfig.groupSeparatorWidth,
			bottom : 0,
			left : 0
		},
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
		var datasetShortName = this.dataColumns[0].datasetShortName;
		tempColumns.push(this.dataColumns[0]);
		for (var i = 1; i < this.dataColumns.length; i++) {			
			if (datasetName == this.dataColumns[i].datasetName) {
				tempColumns.push(this.dataColumns[i]);
			} else {
				columnGroup = new Gemma.MetaHeatmapDatasetColumnGroup(
											{ applicationRoot: this.applicationRoot,
											  height: this.height,
											  datasetName: datasetName,
											  datasetShortName : datasetShortName,
											  dataColumns: tempColumns,
											  datasetColumnGroupIndex: iGroupIndex,
											  datasetGroupIndex: this.datasetGroupIndex } );				
				this.add(columnGroup);
				iGroupIndex++;

				initialWidth += columnGroup.width;
				datasetName = this.dataColumns[i].datasetName;
				datasetShortName = this.dataColumns[i].datasetShortName;
				tempColumns = [];
				tempColumns.push( this.dataColumns[i] );
			}			
		}
		columnGroup = new Gemma.MetaHeatmapDatasetColumnGroup(
											{ applicationRoot: this.applicationRoot,
											  height: this.height,
											  datasetName: datasetName,
											  datasetShortName: datasetShortName,
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

Gemma.MetaHeatmapScrollableArea = Ext.extend(Gemma.MetaHeatmapResizablePanelBase, {
	initComponent : function() {
		Ext.apply(this, {
					border : false,
					bodyBorder : false,
					layout : 'hbox',
					layoutConfig : {
						defaultMargins : {
							top : 0,
							right : Gemma.MetaVisualizationConfig.groupSeparatorWidth,
							bottom : 0,
							left : 0
						}
					},
					dataDatasetGroups : this.dataDatasetGroups,
					geneNames : this.geneNames,
					geneIds : this.geneIds,

					applicationRoot : this.applicationRoot,
												
					_setTopLabelsBox : function(l) {
						this._topLabelsBox = l;
					},

					columnFilters : {},
								
					addFilterFunction : function (filterId, filterFunction) {
						columnFilters[filterId] = filterFunction;
					},
					
					removeFilterFunction : function (filterId) {
						delete columnFilters[filterId];
					},
					
					applyFilters : function () {
						for (var filter in columnFilters) {
							this.items.each(function() {this.filterColumns(filteringFn);});							
						}												
					},
					
					filterColumns : function(filteringFn) {
						var count = 0;
						this.items.each(function() {
									count = count + this.filterColumns(filteringFn);
								});
						return count;
					},

					_filterRows : function(filteringFn) {

					},

					_sortColumns : function(asc_desc, sortingFn) {
						this.items.each(function() {
									this.items.sort(asc_desc, sortingFn);
								});
					}
				});

		Gemma.MetaHeatmapScrollableArea.superclass.initComponent.apply(this, arguments);

		var i;
		var initialWidth = 0;
		for (i = 0; i < this.dataDatasetGroups.length; i++) {
			if (this.dataDatasetGroups[i].length > 0) {
				var dsGroupPanel = new Gemma.MetaHeatmapDatasetGroupPanel({
					applicationRoot : this.applicationRoot,
					height : this.height,
					dataFactorColumns : this.dataDatasetGroups[i],
					datasetGroupIndex : i,
					geneNames : this.geneNames[i],
					geneIds : this.geneIds[i]
				});				
				this.add(dsGroupPanel);
				initialWidth += dsGroupPanel.width + Gemma.MetaVisualizationConfig.groupSeparatorWidth;
			}			
		}
		this.setWidth(initialWidth);
		
	},

	onRender : function() {
		Gemma.MetaHeatmapScrollableArea.superclass.onRender.apply(this, arguments);
	}

});
Ext.reg('metaVizScrollableArea', Gemma.MetaHeatmapScrollableArea);


