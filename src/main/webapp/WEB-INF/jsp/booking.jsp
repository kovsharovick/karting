<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Оформление записи</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-gray-50 min-h-screen flex flex-col">
    <header class="bg-white shadow-sm py-3 px-4 flex items-center">
        <a href="${pageContext.request.contextPath}/slots/${slot.id}" class="text-blue-600 hover:underline mr-4">← Назад</a>
        <h1 class="text-xl font-bold text-blue-600">Оформление записи</h1>
    </header>

    <main class="flex-1 container mx-auto px-4 py-6 max-w-3xl w-full">
        <c:choose>
            <c:when test="${not empty slot}">
                <div class="bg-white rounded-lg shadow p-6">
                    <div class="bg-gray-50 p-3 rounded mb-4">
                        <p>${slot.trackConfig.name} · ${slot.startAtFormatted}</p>
                        <p>Маршал: ${slot.marshal.name}</p>
                    </div>

                    <form:form action="${pageContext.request.contextPath}/booking/${slot.id}" method="post" modelAttribute="bookingForm">
                        <div class="mb-4">
                            <label class="block font-medium">Количество мест</label>
                            <div class="flex items-center gap-3 mt-1">
                                <button type="button" onclick="changeSeats(-1)" class="px-3 py-1 border rounded hover:bg-gray-100">−</button>
                                <span id="seatsDisplay" class="text-xl font-bold w-8 text-center">1</span>
                                <button type="button" onclick="changeSeats(1)" class="px-3 py-1 border rounded hover:bg-gray-100">+</button>
                                <span class="text-sm text-gray-500 ml-2">(макс. ${slot.freeKarts})</span>
                            </div>
                            <form:hidden path="seatsCount" id="seatsInput"/>
                            <form:errors path="seatsCount" cssClass="text-red-600 text-sm mt-1"/>
                        </div>

                        <div class="mb-4">
                            <label class="block font-medium">Прокат экипировки</label>
                            <div class="flex items-center gap-3 mt-1">
                                <button type="button" onclick="changeRental(-1)" class="px-3 py-1 border rounded hover:bg-gray-100">−</button>
                                <span id="rentalDisplay" class="text-xl font-bold w-8 text-center">0</span>
                                <button type="button" onclick="changeRental(1)" class="px-3 py-1 border rounded hover:bg-gray-100">+</button>
                                <span class="text-sm text-gray-500 ml-2">(до ${slot.freeRentalGear})</span>
                            </div>
                            <form:hidden path="rentalGearCount" id="rentalInput"/>
                            <form:errors path="rentalGearCount" cssClass="text-red-600 text-sm mt-1"/>
                        </div>

                        <div class="text-lg font-semibold mt-4">
                            Итого: <span id="totalPrice">${slot.priceKart} ₽</span>
                        </div>
                        <p class="text-sm text-gray-500 mt-1">Оплата на месте: наличные или перевод на карту.</p>

                        <c:if test="${not empty error}">
                            <div class="text-red-600 text-sm mt-2">${error}</div>
                        </c:if>

                        <button type="submit" class="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 mt-4">
                            Подтвердить запись
                        </button>
                    </form:form>
                </div>
            </c:when>
            <c:otherwise>
                <div class="text-center py-10 text-gray-500">
                    <p>Слот не найден</p>
                    <a href="${pageContext.request.contextPath}/slots" class="text-blue-600 hover:underline">Вернуться</a>
                </div>
            </c:otherwise>
        </c:choose>
    </main>

    <script>
        let seats = 1;
        let rental = 0;
        const maxSeats = parseInt('${slot.freeKarts}') || 14;
        const maxRental = parseInt('${slot.freeRentalGear}') || 0;
        const priceKart = parseInt('${slot.priceKart}') || 0;
        const priceGear = parseInt('${slot.priceGearRental}') || 0;

        function updateUI() {
            document.getElementById('seatsDisplay').textContent = seats;
            document.getElementById('seatsInput').value = seats;
            document.getElementById('rentalDisplay').textContent = rental;
            document.getElementById('rentalInput').value = rental;
            const total = seats * priceKart + rental * priceGear;
            document.getElementById('totalPrice').textContent = total + ' ₽';
        }

        function changeSeats(delta) {
            const newVal = seats + delta;
            if (newVal >= 1 && newVal <= maxSeats) {
                seats = newVal;
                if (rental > seats) rental = seats;
                updateUI();
            }
        }

        function changeRental(delta) {
            const newVal = rental + delta;
            if (newVal >= 0 && newVal <= Math.min(seats, maxRental)) {
                rental = newVal;
                updateUI();
            }
        }

        updateUI();
    </script>
</body>
</html>