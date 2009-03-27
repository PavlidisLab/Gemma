Ext.namespace('Gemma');

Gemma.ZOOM_PLOT_SIZE = 400;

Gemma.VisualizationDifferentialWindow = Ext.extend(Ext.Window, {
	id : 'VisualizationDifferentialWindow',
	closeAction : 'destroy',
	bodyStyle : "background:white",
	layout : 'border',
	constrainHeader : true,
	title : "Visualization",
	height : Gemma.ZOOM_PLOT_SIZE,
	width : 600,
	stateful : false,
	
	initComponent : function() {
		this.dv = new Ext.DataView({
			autoHeight : true,
			emptyText : 'Unable to visualize missing data',
			loadingText : 'Loading data ...',
			store : new Gemma.VisualizationStore({
						readMethod : DEDVController.getDEDVForDiffExVisualization
					}),

			tpl : new Gemma.ProfileTemplate(
					'<tpl for="."><tpl for="eevo">',
					'<div class="vizWrap" id ="{shortName}_vizwrap" style="float:left; padding: 10px"> <b> {shortName}</b>: <small> {[sprintf("%.35s",values.name)]} </small> <i> {[(values.minPvalue < 1) ? sprintf("%.3e", values.minPvalue) : "-"]}  </i></div>',
					'</tpl></tpl>'),

			listeners : {
				selectionchange : {
					fn : function(dv, nodes) {

						var record = dv.getRecords(nodes)[0];
						var eevo = record.get("eevo");
						var profiles = record.get("profiles");

						// An attempt to hide the zoom panel and have it expand out nicely... no luck *sigh*
						if (!this.zoomPanel.isVisible()) {
							this.setWidth(Gemma.PLOT_SIZE + Gemma.ZOOM_PLOT_SIZE);
							this.zoomPanel.show();
						}
						this.zoomPanel.displayWindow(eevo, profiles);

					}.createDelegate(this)
				}
			},

			singleSelect : true,

			itemSelector : 'div.vizWrap',

			prepareData : function(data) {

				// Need to transform the coordinate data from an object to an
				// array for flotr
				// probe, genes
				var flotrData = [];
				var coordinateProfile = data.profiles;

				for (var i = 0; i < coordinateProfile.size(); i++) {
					var coordinateObject = coordinateProfile[i].points;

					var probeId = coordinateProfile[i].probe.id;
					var probe = coordinateProfile[i].probe.name;
					var genes = coordinateProfile[i].genes;
					var color = coordinateProfile[i].color;
					var factor = coordinateProfile[i].factor;
					if (factor < 2) {
						/*
						 * Note that using a 'less greyed' color here because the greyd lines often are by themselves on
						 * a plot. This makes them a bit more obvious so the plot doesn't look empty.
						 */
						color = "#FFAAAA";
					}

					var geneNames = genes[0].name;
					for (var k = 1; k < genes.size(); k++) {
						geneNames = geneNames + "," + genes[k].name;
					}

					var oneProfile = [];

					for (var j = 0; j < coordinateObject.size(); j++) {
						var point = [coordinateObject[j].x, coordinateObject[j].y];
						oneProfile.push(point);
					}
					var plotConfig = {
						data : oneProfile,
						color : color,
						genes : genes,
						label : probe + " (" + geneNames + ")",
						lines : {
							lineWidth : Gemma.LINE_THICKNESS
						},
						labelID : probeId,
						factor : factor
					};

					flotrData.push(plotConfig);
				}

				data.profiles = flotrData;

				// Sort data so that greyed out lines get drawn 1st (don't overlap signifigant probes)
				data.profiles.sort(Gemma.graphSort);

				return data;
			}
		});

		this.thumbnailPanel = new Ext.Panel({
					title : 'query gene (red) with coexpressed gene (black)',
					region : 'west',
					split : true,
					width : Gemma.PLOT_SIZE + 50,
					collapsible : true,
					stateful : false,
					margins : '3 0 3 3',
					cmargins : '3 3 3 3',
					items : this.dv,
					autoScroll : true,
					html : {
						id : 'zoomLegend',
						tag : 'div',
						style : 'width:' + Gemma.PLOT_SIZE + 'px;height:' + Gemma.PLOT_SIZE + 'px; float:left;'
					}

				});

		this.zoomPanel = new Ext.Panel({

					region : 'center',
					split : true,
					width : Gemma.ZOOM_PLOT_SIZE,
					height : Gemma.ZOOM_PLOT_SIZE,
					stateful : false,
					id : 'visualization-zoom-window',
					closeAction : 'destroy',
					bodyStyle : "background:white",
					constrainHeader : true,
					layout : 'fit',
					title : "Click thumbnail to zoom in",

					listeners : {
						resize : {
							fn : function(component, adjWidth, adjHeight, rawWidth, rawHeight) {

								// Change the div so that it is the size of the panel surrounding it.
								zoomPanelDiv = Ext.get('graphzoompanel');
								zoomPanelDiv.setHeight(rawHeight - 27); // magic 27 again.
								zoomPanelDiv.setWidth(rawWidth - 1);
								zoomPanelDiv.repaint();

								component.refreshWindow();

							}.createDelegate(this)
						}
					},

					html : {
						id : 'graphzoompanel',
						tag : 'div',
						style : 'width:' + Gemma.ZOOM_PLOT_SIZE + 'px;height:' + Gemma.ZOOM_PLOT_SIZE + 'px;'
					},

					refreshWindow : function(profiles) {
						// Should redraw to fit current window width and hight.

						if (profiles == null) {
							var window = this.findParentByType(Gemma.VisualizationDifferentialWindow)
							var record = window.dv.getSelectedRecords()[0];
							// This gets called because window gets resized at startup.
							if (record == null)
								return;
							profiles = record.get("profiles");
						}

						Flotr.draw($('graphzoompanel'), profiles, Gemma.GRAPH_ZOOM_CONFIG);

					},

					displayWindow : function(eevo, profiles) {

						this.setTitle("<a   target='_blank'  href='/Gemma/expressionExperiment/showExpressionExperiment.html?id=" +eevo.id+ " '> " + eevo.shortName + "</a>: " + eevo.name);

						if (!this.isVisible()) {
							this.setVisible(true);
							this.show();
						}

						Flotr.draw($('graphzoompanel'), profiles, Gemma.GRAPH_ZOOM_CONFIG);

					}

				});

		Ext.apply(this, {
					items : [this.thumbnailPanel, this.zoomPanel]
				});

		Gemma.VisualizationDifferentialWindow.superclass.initComponent.call(this);

	},

	displayWindow : function(eeIds, gene, threshold, factorMap) {

		this.setTitle("Visualization of gene: <a   target='_blank' ext:qtip=' "+ gene.officialSymbol+ " ' href='/Gemma/gene/showGene.html?id=" + gene.id + "'> " + gene.officialName + "</a>");

		var downloadDedvLink =  String.format("<a ext:qtip='Download raw data in a tab delimted format'  target='_blank'  href='/Gemma/dedv/downloadDEDV.html?ee={0} &g={1},{2}' > [download raw data]</a>",
				eeIds,gene.id);
		
		this.thumbnailPanel.setTitle(gene.officialSymbol + "<br>" + downloadDedvLink);

		var params = [];
		params.push(eeIds);
		params.push([gene.id]);
		params.push(threshold);
		params.push(factorMap);

		this.show();
		this.dv.store.load({
					params : params
				});

	}

});