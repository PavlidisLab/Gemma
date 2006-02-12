<%@ include file="/common/taglibs.jsp"%>
<title>
    <fmt:message key="mainMenu.title" />
</title>
<content tag="heading">
<fmt:message key="mainMenu.heading" />
</content>

<fmt:message key="mainMenu.message" />

<div class="separator"></div>

<ul class="glassList">

    <li>
        <!--    Paul, uncomment to use webflow implementation    
		<a href="<c:url value="/flowController.html?_flowId=arrayDesign.Search"/>"><fmt:message key="menu.flow.ArrayDesignSearch"/></a>
-->
        <a href="<c:url value="/arrayDesign/showAllArrayDesigns.html"/>">
            <fmt:message key="menu.flow.ArrayDesignSearch" />
        </a>
    </li>
    <li>
        <!-- <a href="<c:url value="/ExperimentList.html"/>">Show Expression Experiments</a>-->
        <a href="<c:url value="/expressionExperiments.html"/>">
            Show Expression Experiments
        </a>
    </li>
    <li>
        <a href="<c:url value="/candidateGeneList.html"/>">
            <fmt:message key="menu.CandidateGeneList" />
    </li>
    <li>
        <a href="<c:url value="/flowController.html?_flowId=pubMed.Search"/>">
            <fmt:message key="menu.flow.PubMedSearch" />
        </a>
    </li>
    <li>
        <a href="<c:url value="/bibRef/searchBibRef.html"/>">
            Search Gemma for PubMed Id
        </a>
    </li>
    <li>
        <a href="<c:url value="/editProfile.html"/>">
            <fmt:message key="menu.user" />
        </a>
    </li>
    <li>
        <a href="<c:url value="/flowController.htm?_flowId=fileUploader"/>">
            <fmt:message key="menu.selectFile" />
        </a>
    </li>
</ul>

