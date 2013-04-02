
<%-- $Id$
@deprecated I don't think this is used any more.

--%>
<%@ include file="/common/taglibs.jsp"%>
<head>
<title><fmt:message key="searchCoexpression.title" /></title>

<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
<jwr:script src='/scripts/app/CoexpressionSearch.js' />

<content tag="heading"> <fmt:message key="searchCoexpression.heading" /> </content>

</head>

<div id='coexpression-messages' style='width: 100%; height: 2.2em; margin: 5px'></div>
<div id='coexpression-experiments' class="x-hidden"></div>
<div id='coexpression-genes' class="x-hidden"></div>
<div id="coexpression-wrap">
	<div id='coexpression-all'></div>
</div>