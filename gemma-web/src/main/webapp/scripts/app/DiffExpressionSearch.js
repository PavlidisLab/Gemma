/**
 * @author keshav
 * @version $Id$
 */
Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
Ext.onReady(function() {
	Ext.QuickTips.init();
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

	var admin = dwr.util.getValue("hasAdmin");

	if (Ext.isIE && !Ext.isIE7) {
		Ext.DomHelper.append('diffExpression-form', {
			tag : 'p',
			cls : 'trouble',
			html : 'This page may display improperly in older versions of Internet Explorer. Please upgrade to Internet Explorer 7 or newer.'
		});
	}

	var searchPanel = new Gemma.DiffExpressionSearchForm({});
	searchPanel.render("diffExpression-form");

	var diffExGrid = new Gemma.DiffExpressionGrid({
				renderTo : "diffExpression-results",
				title : "Differentially expressed genes",
				pageSize : 25
			});

	var visWindow;
	var geneRowClickHandler = function(grid, rowIndex, columnIndex, e) {
		if (this.getSelectionModel().hasSelection()) {

			var record = this.getStore().getAt(rowIndex);
			var fieldName = this.getColumnModel().getDataIndex(columnIndex);
			var gene = record.data.gene;

			if (fieldName == 'visualize') {

				var activeExperiments = record.data.activeExperiments;
				var activeExperimentIds = [];

				for (var i = 0; i < activeExperiments.size(); i++) {
					activeExperimentIds.push(activeExperiments[i].id);
				}

				// destroy if already open
				if (visWindow) {
					visWindow.close();
				}

				visWindow = new Gemma.VisualizationDifferentialWindow({
							admin : admin
						});
				visWindow.displayWindow(activeExperimentIds, gene);
			} else if (fieldName == 'details') {

				var diffExGrid = new Gemma.ProbeLevelDiffExGrid({
							width : 750,
							height : 300
						});

				var w = new Ext.Window({
							modal : true,
							layout : 'fit',
							title : 'Details for ' + gene.officialSymbol,
							closeAction : 'close',
							items : [diffExGrid]
						});

				w.show();

				var supporting = record.data.probeResults;
				diffExGrid.getStore().loadData(supporting);

			}
		}
	};

	diffExGrid.on("cellclick", geneRowClickHandler, diffExGrid);

	searchPanel.geneChooserPanel.toolbar.taxonCombo.on('ready', function() {
				Ext.get('loading').remove();
				Ext.get('loading-mask').fadeOut({
							duration : 0.5,
							remove : true
						});
			});

	searchPanel.on("aftersearch", function(panel, result) {
		var link = panel.getBookmarkableLink();
		diffExGrid
				.setTitle(String
						.format(
								"Differentially expressed genes <a href='{0}'>(bookmarkable link)</a> <a href='{0}&export'>(export as text)</a>",
								link));

		diffExGrid.loadData(result);

	});

});