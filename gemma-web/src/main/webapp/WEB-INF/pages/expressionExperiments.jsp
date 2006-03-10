<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
    <head>
        <title>
            <fmt:message key="expressionExperimentsList.title" />
        </title>
    </head>
    <body>
        <h2>
            <fmt:message key="search.results" />
        </h2>

        <display:table name="expressionExperiments" class="list" requestURI="" id="expressionExperimentList"
            export="true" decorator="ubic.gemma.web.taglib.displaytag.ExpressionExperimentWrapper">

            <display:column property="accession.externalDatabase.name" sort="true"
                titleKey="expressionExperiment.databaseName" />

            <display:column property="detailsLink" sort="true" titleKey="expressionExperiment.id" />

            <display:column property="nameLink" sort="true" titleKey="expressionExperiment.name" />

            <display:column property="designsLink" sort="true" titleKey="expressionExperiment.designs" />

            <display:column property="assaysLink" sort="true" titleKey="expressionExperiment.assays" />

            <display:setProperty name="basic.empty.showtable" value="true" />
        </display:table>

    </body>
</html>
