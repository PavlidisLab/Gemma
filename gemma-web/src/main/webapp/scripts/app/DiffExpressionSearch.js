/**
 * @author keshav
 * @version $Id$
 * @deprecated
 */
Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
Ext.onReady(function() {
	Ext.QuickTips.init();
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

	if (Ext.isIE && Ext.isIE7) {
		Ext.DomHelper.append('diffExpression-form', {
			tag : 'p',
			cls : 'trouble',
			html : Gemma.HelpText.CommonWarnings.BrowserWarnings.ie7
		});
	}

	var searchPanel = new Gemma.DiffExpressionSearchForm({});
	searchPanel.render("diffExpression-form");

	var diffExGrid = new Gemma.DiffExpressionGrid({
				renderTo : "diffExpression-results",
				title : "Differentially expressed genes",
				searchPanel : searchPanel
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