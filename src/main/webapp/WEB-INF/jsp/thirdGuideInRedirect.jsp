<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" session="false"
%><%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><html>
<head>
    <c:if test="${not empty url}"><meta http-equiv="refresh" content="0; url=${url}" /></c:if>
    <title></title>
</head>
<body>
${errorMsg}
</body>
</html>
