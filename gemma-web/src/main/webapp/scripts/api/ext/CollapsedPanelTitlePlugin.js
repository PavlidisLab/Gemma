/**
 * Code from http://extjs.com/forum/showthread.php?p=90105, slightly modified. Set 'collapsedTitle' in your
 * configuration of the panel.
 */
Ext.ux.CollapsedPanelTitlePlugin = function() {
	this.init = function(p) {
		if (p.collapsible && p.collapsedTitle) {
			var r = p.region;
			if ((r == 'north') || (r == 'south')) {
				p.on('render', function() {
							var ct = p.ownerCt;
							ct.on('afterlayout', function() {
										if (ct.layout[r].collapsedEl) {
											p.collapsedTitleEl = ct.layout[r].collapsedEl.createChild({
														tag : 'span',
														cls : 'x-panel-collapsed-text front-page-header-text',
														html : p.collapsedTitle
													});
										}
									}, false, {
										single : true
									});
							p.on('collapse', function() {
										if (ct.layout[r].collapsedEl && !p.collapsedTitleEl) {
											p.collapsedTitleEl = ct.layout[r].collapsedEl.createChild({
														tag : 'span',
														cls : 'x-panel-collapsed-text front-page-header-text',
														html : p.collapsedTitle
													});
										}
									}, false, {
										single : true
									});
						});
			}
		}
	};
};