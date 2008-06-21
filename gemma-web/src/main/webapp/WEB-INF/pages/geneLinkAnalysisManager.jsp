<%@ include file="/common/taglibs.jsp"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<head>
	<title>Gene link analysis manager</title>

	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/app/GeneLinkAnalysisManager.js' />

</head>

<h1>
	Gene link analysis manager
</h1>

<authz:authorize ifAnyGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="true" />
</authz:authorize>
<authz:authorize ifNotGranted="admin">
	<input type="hidden" name="hasAdmin" id="hasAdmin" value="" />
</authz:authorize>
<div id='createAnalysisDialog' class="x-hidden"></div>
<div id='messages' style='width: 600px; height: 1.6em; margin: 0.2em; padding-bottom: 0.4em;'></div>
<div id='genelinkanalysis-analysisgrid' style='width: 910px; margin-bottom: 1em;'></div>
<div style='width: 930px; height: 600px;'>
	<div id='genelinkanalysis-datasetgrid' style='width: 450px; position: absolute;'></div>
	<div id='genelinkanalysis-newanalysis' style='width: 450px; position: absolute; left: 470px;'></div>

</div>
