<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
    <head>
        <title>
            <fmt:message key="compositeSequences.title" />
        </title>
    </head>
    <body>
        <h2>
            <fmt:message key="compositeSequences.title" />
        </h2>

        <display:table name="compositeSequences" class="list" requestURI="" id="compositeSequenceList"
            export="true" pagesize="20">
			<display:column property="name" sortable="true" titleKey="compositeSequence.name" maxWords="20" />
			<display:column property="description" sortable="true" titleKey="compositeSequence.description" maxWords="100" />			 
            <display:setProperty name="basic.empty.showtable" value="true" />      
        </display:table>

    </body>
</html>
