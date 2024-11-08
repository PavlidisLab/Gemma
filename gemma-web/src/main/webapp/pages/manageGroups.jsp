<%@ include file="/common/taglibs.jsp" %>

<head>
<title>Manage groups</title>
<jwr:script src='/scripts/app/manageGroups.js' useRandomParam='false' />
</head>

<input type="hidden" id="reloadOnLogout" value="true">
<input type="hidden" id="reloadOnLogin" value="true" />
<h2>Manage groups</h2>

<p>
    A 'Group' is a set of Gemma users who have a common set of permissions. This page allows you to see what Groups you
    belong to, create groups, and change who is in groups you control.
</p>

<p>
    For additional controls on which groups can view or edit your data sets, visit the
    <a href="<c:url value="/expressionExperiment/showAllExpressionExperimentLinkSummaries.html" />">Data Manager</a>. If
    you want to manage Gene Groups go to the
    <a href="<c:url value="/geneGroupManager.html" />">Gene Group Manager</a>. To manage Expression Dataset Groups go to
    the
    <a href="<c:url value="/expressionExperimentSetManager.html" />">Dataset Group Manager</a>.
</p>

<div id='manageGroups-div'></div>
<div id='errorMessage' style='width: 500px; margin-bottom: 1em;'></div>