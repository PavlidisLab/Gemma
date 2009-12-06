/**
 * Methods to view and edit security on objects
 * 
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.SecurityManager = {};


/**
 * Show the manager for the given entity.
 * 
 * @param {}
 *            clazz full qualified class name of Gemma entity impl, e.g.
 *            ubic.gemma.model.expression.experiment.ExpressionExperimentImpl.
 * @param {}
 *            id of the entity
 * @param {}
 *            elid HTML element that will be used to show the results.
 */
Gemma.SecurityManager.managePermissions = function(clazz, id, elid) {
	/*
	 * Show a panel to 1) make the data set private or public 2) share the data with groups the user is in an which 3)
	 * shows the current permissions. There can be any number of groups. On returning, update the div.
	 */

	/*
	 * Need to get: public status from server; group sharing; available groups.
	 */

	var isPublic;

	SecurityController.getSecurityInfo({
				classDelegatingFor : clazz,
				id : id
			}, function(data) {
				console.log(data);
				isPublic = data.isPublic;
			});

	/*
	 * After done
	 */
	Ext.DomHelper.overwrite(elid, Gemma.SecurityManager.getSecurityLink(clazz, id, isPublic));

};

/**
 * Display an icon representing the security status.
 * 
 * @param {}
 *            clazz full qualified class name of Gemma entity impl, e.g.
 *            ubic.gemma.model.expression.experiment.ExpressionExperimentImpl.
 * @param {}
 *            id of the entity
 * @param {}
 *            isPublic
 * @return {} html for the link
 */
Gemma.SecurityManager.getSecurityLink = function(clazz, id, isPublic) {
	var securityFieldId = Ext.id();

	var icon = isPublic ? '/Gemma/images/icons/lock_open2.png' : '/Gemma/images/icons/lock.png';

	var result = '<a href="#" onClick="return Gemma.SecurityManager.managePermissions(\'' + clazz + '\',' + id + ',\''
			+ securityFieldId + '\')"><span id="' + securityFieldId
			+ '" ><img ext:qtip="Click to edit permissions" src="' + icon + '" "public"/></span></a>';
	return result;
};