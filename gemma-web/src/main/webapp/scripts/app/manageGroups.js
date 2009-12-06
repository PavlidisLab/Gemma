Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

/**
 * @author keshav
 * @version $Id$
 */
Ext.onReady(function() {

	Ext.QuickTips.init();

	/*
	 * Show the groups the user is a member of, and who the other members are.
	 */
	SecurityController.getAvailableGroups({
				callback : function(data) {
					console.log(data);
				},
				errhandler : function(data) {
					alert(data);
				}
			})

		/*
		 * Show link to add users.
		 */

		/*
		 * Show a form to add a new group.
		 */

	});