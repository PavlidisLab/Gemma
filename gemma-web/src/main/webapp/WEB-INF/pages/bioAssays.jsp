<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
    <head>
        <title>
            <fmt:message key="bioAssays.title" />
        </title>
    </head>
    <body>
        <h2>
            <fmt:message key="bioAssays.title" />
        </h2>

        <display:table name="bioAssays" class="list" requestURI="" id="bioAssayList"
             pagesize="20" decorator="ubic.gemma.web.taglib.displaytag.expression.bioAssay.BioAssayWrapper">
		
			<display:column property="nameLink" sortable="true" titleKey="bioAssay.name" maxWords="20" />
			<display:column property="description" sortable="true" titleKey="bioAssay.description" maxWords="100" />
			
  
            <display:setProperty name="basic.empty.showtable" value="true" />      
        </display:table>

    </body>
</html>
