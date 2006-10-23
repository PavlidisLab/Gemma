<%@ include file="/common/taglibs.jsp"%>

<title><fmt:message key="userList.title"/></title>
<content tag="heading"><fmt:message key="userList.heading"/></content>

<c:set var="buttons">
    <button type="button" style="margin-right: 5px"
        onclick="location.href='<c:url value="/editUser.html"/>?method=Add&from=list'">
        <fmt:message key="button.add"/>
    </button>
    
    <button type="button" onclick="location.href='<c:url value="/mainMenu.html" />'">
        <fmt:message key="button.cancel"/>
    </button>
</c:set>

<%-- <display:table name="${userList}" cellspacing="0" cellpadding="0" --%>
<display:table name="userList" cellspacing="0" cellpadding="0"
    requestURI="" defaultsort="1" id="users"
    pagesize="25" styleClass="list userList" >
  
    <%-- Table columns --%>
    <display:column property="userName" sort="true" 
    	headerClass="sortable" width="17%"
        url="/editUser.html?from=list" 
        paramId="userName" paramProperty="userName"
        titleKey="user.userName"/>
    <display:column property="name" sort="true" 
    	headerClass="sortable" width="20%"
        titleKey="user.name" />
    <display:column property="lastName" sort="true" 
    	headerClass="sortable" width="13%"
        titleKey="user.lastName"/>
    <display:column property="email" sort="true" headerClass="sortable" 
    	width="26%" autolink="true"
        titleKey="user.email" />
    
    <display:setProperty name="paging.banner.item_name" value="user"/>
    <display:setProperty name="paging.banner.items_name" value="users"/>

    <display:setProperty name="export.excel.filename" value="User List.xls"/>
    <display:setProperty name="export.csv.filename" value="User List.csv"/>
</display:table>

<c:out value="${buttons}" escapeXml="false" />
            
<script type="text/javascript">
<!--
highlightTableRows("users");
//-->
</script>
