<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Вход — Апекс</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-50 min-h-screen flex items-center justify-center p-4">
    <div class="bg-white p-8 rounded-lg shadow-md w-full max-w-md">
        <h1 class="text-3xl font-bold text-blue-600 text-center mb-6">Апекс</h1>
        <p class="text-center text-gray-600 mb-4">Войдите, чтобы записаться</p>

        <form:form action="${pageContext.request.contextPath}/request-code" method="post" modelAttribute="loginForm">
            <div class="mb-4">
                <label class="block text-sm font-medium text-gray-700">Телефон</label>
                <form:input path="phone" type="tel" placeholder="+7 999 123-45-67"
                            class="w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500
                                   ${pageContext.request.getParameter('error') != null ? 'border-red-500' : 'border-gray-300'}"/>
                <form:errors path="phone" cssClass="text-red-600 text-sm mt-1"/>
                <c:if test="${not empty error}">
                    <p class="text-red-600 text-sm mt-1">${error}</p>
                </c:if>
            </div>
            <button type="submit"
                    class="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 transition">
                Получить код
            </button>
            <p class="text-xs text-gray-400 mt-3 text-center">Без пароля — вход по номеру</p>
        </form:form>
    </div>
</body>
</html>