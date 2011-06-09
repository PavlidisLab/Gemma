Ext.namespace('Gemma');

Gemma.MetaHeatmapControlWindow = Ext.extend(Ext.Window, {

			hidden : true,
			shadow : false,
			initComponent : function() {
				Ext.apply(this, {
							title : 'Visualization settings',
							height : 400,
							width : 300,
							layout : 'accordion',
							layoutConfig : {
								titleCollapse : false,
								animate : true,
								activeOnTop : true
							},
							items : [{
										title : 'Data selection',
										xtype : 'metaVizDataSelection',
										ref : 'selectionPanel'
									}, {
										title : 'Filter/Sort',
										xtype : 'metaVizSortFilter',
										ref : 'sortPanel'
									}]
						});
				Gemma.MetaHeatmapControlWindow.superclass.initComponent.apply(this, arguments);
			},
			onRender : function() {
				Gemma.MetaHeatmapControlWindow.superclass.onRender.apply(this, arguments);
			}
		});

Gemma.MetaHeatmapSortFilter = Ext.extend(Ext.Panel, {
			initComponent : function() {
				Ext.apply(this, {});
				Gemma.MetaHeatmapSortFilter.superclass.initComponent.apply(this, arguments);
			},
			onRender : function() {
				Gemma.MetaHeatmapSortFilter.superclass.onRender.apply(this, arguments);
			}
		});

Ext.reg('metaVizSortFilter', Gemma.MetaHeatmapSortFilter);