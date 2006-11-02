<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
    <head>
        <title>
            <fmt:message key="genes.title" />
        </title>
    </head>
    <body>
        <h2>
            Gene search for <b>${searchParameter}</b>
        </h2>

	    <display:table name="genes" class="list" requestURI="" id="genesList"
            decorator="ubic.gemma.web.taglib.displaytag.gene.GeneFinderWrapper" 
            pagesize="100">	
			<display:column property="nameLink" sortable="true" titleKey="gene.name" maxWords="20" />
			<display:column property="officialSymbol" sortable="true" titleKey="gene.officialSymbol" maxWords="20" />
			<display:column property="taxon" sortable="true" titleKey="taxon.title" maxWords="20" />
			<display:column property="officialName" sortable="true" titleKey="gene.officialName" maxWords="20" />
            <display:setProperty name="basic.empty.showtable" value="true" />      
        </display:table>

    </body>
</html>
