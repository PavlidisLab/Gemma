<%@ include file="/common/taglibs.jsp"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<head>
	<title>Expression Experiment Set Manager</title>

	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/ajax/entities/DatasetChooserPanel.js' />

</head>

<h1>
	Expression Experiment Set Manager
</h1>

<security:authorize ifAnyGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="true" />
</security:authorize>
<security:authorize ifNotGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="" />
</security:authorize>


<security:authorize ifAnyGranted="admin">
	<script type="text/javascript">
	Ext.namespace('Gemma');
	Ext.onReady(function() {
	Ext.QuickTips.init();
	
	
	 var eeSetChooserPanel = new Gemma.ExpressionExperimentSetPanel({
			isAdmin : true,
			store : new Gemma.ExpressionExperimentSetStore()
	});
	
	eeSetChooserPanel.render('EESetManager');
	eeSetChooserPanel.expand();
	
});
</script>
</security:authorize>


<div id='EESetManager'></div>
<div id='messages' style='width: 600px; height: 1.6em; margin: 0.2em; padding-bottom: 0.4em;'></div>

</div>
