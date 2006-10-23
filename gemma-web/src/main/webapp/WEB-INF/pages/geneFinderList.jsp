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
            <fmt:message key="genes.title" />
        </h2>
        <h3>
        	<b>${searchParameter}</b>
        	<br>
        	Genes matched by:
        </h3>

		<b>Official symbol</b>

        <display:table name="genesOfficialSymbol" class="list" requestURI="" id="genesOfficialSymbolList"
            decorator="ubic.gemma.web.taglib.displaytag.gene.GeneWrapper" 
            pagesize="20">	
			<display:column property="name" sortable="true" titleKey="gene.name" maxWords="20"
				href="/Gemma/gene/showGene.html" paramId="id" paramProperty="id" />
			<display:column property="officialSymbol" sortable="true" titleKey="gene.officialSymbol" maxWords="20" />
			<display:column property="taxon" sortable="true" titleKey="taxon.title" maxWords="20" />
			<display:column property="description" sortable="true" titleKey="gene.description" maxWords="100" /> 
            <display:setProperty name="basic.empty.showtable" value="true" />      
        </display:table>
 
 		<b>Alias</b>
 		       
        <display:table name="genesAlias" class="list" requestURI="" id="genesAliasList"
            decorator="ubic.gemma.web.taglib.displaytag.gene.GeneWrapper" 
            pagesize="20">	
			<display:column property="name" sortable="true" titleKey="gene.name" maxWords="20"
				href="/Gemma/gene/showGene.html" paramId="id" paramProperty="id" />
			<display:column property="officialSymbol" sortable="true" titleKey="gene.officialSymbol" maxWords="20" />
			<display:column property="taxon" sortable="true" titleKey="taxon.title" maxWords="20" />
			<display:column property="description" sortable="true" titleKey="gene.description" maxWords="100" /> 
            <display:setProperty name="basic.empty.showtable" value="true" />      
        </display:table>
        
         <b>Gene Product</b>
 		       
        <display:table name="genesGeneProduct" class="list" requestURI="" id="genesGeneProductList"
            decorator="ubic.gemma.web.taglib.displaytag.gene.GeneWrapper" 
            pagesize="20">	
			<display:column property="name" sortable="true" titleKey="gene.name" maxWords="20"
				href="/Gemma/gene/showGene.html" paramId="id" paramProperty="id" />
			<display:column property="officialSymbol" sortable="true" titleKey="gene.officialSymbol" maxWords="20" />
			<display:column property="taxon" sortable="true" titleKey="taxon.title" maxWords="20" />
			<display:column property="description" sortable="true" titleKey="gene.description" maxWords="100" /> 
            <display:setProperty name="basic.empty.showtable" value="true" />      
        </display:table>

    </body>
</html>
