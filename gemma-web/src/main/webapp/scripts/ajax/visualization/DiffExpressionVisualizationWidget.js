Ext.namespace('Gemma');

Gemma.ZOOM_PLOT_SIZE = 400;
var m_diffQueryGene;

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

		var template = Gemma.getDiffExpressionTemplate();

		this.dv = new Ext.DataView({
			autoHeight : true,
			emptyText : 'Unable to visualize missing data',
			loadingText : 'Loading data ...',
			store : new Gemma.VisualizationStore({
				readMethod : DEDVController.getDEDVForDiffExVisualization
			}),

			tpl : template,

			setTemplate : function(tpl, refresh) {
				// TODO factor this out and create custom DataView (also in
				// CoexpressionExpressionVisualizationWidget)

				this.tpl = tpl;
				if (refresh) {
					this.refresh();
				} else {
					var sel = this.getSelectedIndexes();
					this.tpl.overwrite(this.el, this.collectData(this.store
							.getRange(), 0));
					this.all.fill(Ext.query(this.itemSelector, this.el.dom));
					this.updateIndexes(0);
					this.select(sel);
				}
			},
			listeners : {
				selectionchange : {
					fn : function(dv, nodes) {
						
						var record = dv.getRecords(nodes)[0];
						if (!record){
							return;
						}
						
						var eevo = record.get("eevo");
						var profiles = record.get("profiles");

						// An attempt to hide the zoom panel and have it expand
						// out nicely... no luck *sigh* problem is: have to click on thumbnail twice to get to work....
						if (!this.zoomPanel.isVisible()) {
							this.setWidth(Gemma.PLOT_SIZE + Gemma.ZOOM_PLOT_SIZE);
							this.zoomPanel.show();
						}
						this.zoomPanel.displayWindow(eevo, profiles);

					}.createDelegate(this)
				}, 
				show : {
					fn : function(dv){
						//FIXME doesn't select anything.  Thinking it is getting called before there is data to select....
						//dv.selectRange(0,1);
						//dv.select(0,false, false);
						//console.log( "where" + this.dv.getNodes());
						var nodes = this.dv.getNodes();
						this.dv.select(nodes[0]);
					}.createDelegate(this)
				}
			},

			singleSelect : true,

			itemSelector : 'div.vizWrap',

			prepareData : function(data) {

				// Need to transform the coordinate data from an object to an
				// array for flotr. Happens every time window is
				// refreshed/resized might be able to add a performance boost by
				// not doing twice...
				// probe, genes
				var flotrData = [];
				var coordinateProfile = data.profiles;
				// var eevo = data.eevo;

				for (var i = 0; i < coordinateProfile.size(); i++) {
					var coordinateObject = coordinateProfile[i].points;

					var probeId = coordinateProfile[i].probe.id;
					var probe = coordinateProfile[i].probe.name;
					var genes = coordinateProfile[i].genes;
					var color = coordinateProfile[i].color;
					var factor = coordinateProfile[i].factor;
					var pvalue = coordinateProfile[i].PValue;

					if (factor < 2) {
						/*
						 * Note that using a 'less greyed' color here because
						 * the greyd lines often are by themselves on a plot.
						 * This makes them a bit more obvious so the plot
						 * doesn't look empty.
						 */
						color = "#FFAAAA";
					}

					var geneNames = genes[0].name;				
					for (var k = 1; k < genes.size(); k++) {
						//Put search gene in begining of list
						if (Gemma.geneContained(genes[k].name, [m_diffQueryGene])) {
							geneNames = genes[k].name + "," + geneNames;							
						}
						else {
							geneNames = geneNames + "," + genes[k].name;
						}
					}
					var oneProfile = [];

					for (var j = 0; j < coordinateObject.size(); j++) {
						var point = [coordinateObject[j].x,
								coordinateObject[j].y];
						oneProfile.push(point);
					}

					pvalueLabel = (pvalue != 1) ? sprintf("%.2e", pvalue) : "n/a";

					var plotConfig = {
						data : oneProfile,
						color : color,
						genes : genes,
						label : " <a  href='/Gemma/compositeSequence/show.html?id="
								+ probeId
								+ "' target='_blank' ext:qtip= '"
								+ probe
								+ " ("
								+ geneNames
								+ ")"
								+ "'> "
								+ Ext.util.Format.ellipsis(pvalueLabel + ": "
										+ geneNames,
										Gemma.MAX_LABEL_LENGTH_CHAR) + "</a>",
						lines : {
							lineWidth : Gemma.LINE_THICKNESS
						},
						labelID : probeId,
						factor : factor,
						// Need to be added so switching views work
						probe : {
							id : probeId,
							name : probe
						},
						points : coordinateObject,
						PValue : pvalue

					};

					flotrData.push(plotConfig);
				}

				data.profiles = flotrData;

				// Sort data so that greyed out lines get drawn 1st (don't
				// overlap signifigant probes)
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
				style : 'width:' + Gemma.PLOT_SIZE + 'px;height:'
						+ Gemma.PLOT_SIZE + 'px; float:left;'
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
					fn : function(component, adjWidth, adjHeight, rawWidth,
							rawHeight) {

						// Change the div so that it is the size of the panel
						// surrounding it.
						zoomPanelDiv = Ext.get('graphzoompanel');
						zoomPanelDiv.setHeight(rawHeight - 27); // magic 27
																// again.
						zoomPanelDiv.setWidth(rawWidth - 1);
						zoomPanelDiv.repaint();

						component.refreshWindow();

					}.createDelegate(this)
				}
			},

			html : {
				id : 'graphzoompanel',
				tag : 'div',
				style : 'width:' + Gemma.ZOOM_PLOT_SIZE + 'px;height:'
						+ Gemma.ZOOM_PLOT_SIZE + 'px; margin:5px 2px 2px 5px;'
			},

			refreshWindow : function(profiles) {
				// Should redraw to fit current window width and hight.

				if (!profiles) {
					var window = this
							.findParentByType(Gemma.VisualizationDifferentialWindow);
					var record = window.dv.getSelectedRecords()[0];
					// This gets called because window gets resized at startup.
					if (record == null)
						return;
					profiles = record.get("profiles");
					if (!profiles)
						return;
				}

				if (Gemma.DIFF_HEATMAP_VIEW) {
					$('graphzoompanel').innerHTML = '';
					// Sort data for heatmap view.
					profiles.sort(Gemma.sortByPValue);
					Heatmap.draw($('graphzoompanel'), profiles,
							Gemma.GRAPH_ZOOM_CONFIG);
				} else {
					profiles.sort(Gemma.graphSort);
					Flotr.draw($('graphzoompanel'), profiles,
							Gemma.GRAPH_ZOOM_CONFIG);

				}

			},

			displayWindow : function(eevo, profiles) {

				this
						.setTitle("<a   target='_blank'  href='/Gemma/expressionExperiment/showExpressionExperiment.html?id="
								+ eevo.id
								+ " '> "
								+ eevo.shortName
								+ "</a>: "
								+ eevo.name);

				if (!this.isVisible()) {
					this.setVisible(true);
					this.show();
				}

				if (Gemma.DIFF_HEATMAP_VIEW) {
					$('graphzoompanel').innerHTML = '';
					profiles.sort(Gemma.sortByPValue);
					Heatmap.draw($('graphzoompanel'), profiles,
							Gemma.GRAPH_ZOOM_CONFIG);
				} else {
					profiles.sort(Gemma.graphSort);
					Flotr.draw($('graphzoompanel'), profiles,
							Gemma.GRAPH_ZOOM_CONFIG);

				}

			}

		});

		Ext.apply(this, {
			items : [this.thumbnailPanel, this.zoomPanel],
			buttons : [{
				text : "Switch View",
				id : "toggleView",
				handler : this.switchView.createDelegate(this, [], false)
			}]
		});

		Gemma.VisualizationDifferentialWindow.superclass.initComponent
				.call(this);

	},

	switchView : function() {

		// TODO: change info on button to relfect curent state of viewing
		toggleButton = Ext.get("toggleView");

		if (Gemma.DIFF_HEATMAP_VIEW) {
			Gemma.DIFF_HEATMAP_VIEW = false;
		} else {
			var zoomLegendDiv = $("zoomLegend");
			if (zoomLegendDiv) {
				zoomLegendDiv.innerHTML = '';
			}

			Gemma.DIFF_HEATMAP_VIEW = true;
		}

		var template = Gemma.getDiffExpressionTemplate();

		this.dv.setTemplate(template, false);

	},

	displayWindow : function(eeIds, gene, threshold, factorMap) {

		this.setTitle("Visualization of gene: <a   target='_blank' ext:qtip=' "
				+ gene.officialSymbol
				+ " ' href='/Gemma/gene/showGene.html?id=" + gene.id + "'> "
				+ gene.officialName + "</a>");

		var downloadDedvLink = String
				.format(
						"<a ext:qtip='Download raw data in a tab delimted format'  target='_blank'  href='/Gemma/dedv/downloadDEDV.html?ee={0} &g={1},{2}' > <img src='/Gemma/images/asc.gif'/></a>",
						eeIds, gene.id);

		this.thumbnailPanel.setTitle("Thumbnails &nbsp; " + downloadDedvLink);

		m_diffQueryGene  = gene.officialSymbol;				
				
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