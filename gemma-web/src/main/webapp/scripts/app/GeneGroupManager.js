Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

/**
 * Provides functionallity for creating and managing Gene groups inside of Gemma.
 * 
 * @author klc
 * @version $Id$
 */

Ext.onReady(function() {

			Ext.QuickTips.init();
			Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

			new Gemma.GeneGroupManager({
						renderTo : 'genesetCreation-div'
					});

		});
