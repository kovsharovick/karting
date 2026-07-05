<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Детали брони</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <script src="https://cdn.jsdelivr.net/npm/axios/dist/axios.min.js"></script>
</head>
<body class="bg-gray-50 min-h-screen flex flex-col">
    <header class="bg-white shadow-sm py-3 px-4 flex items-center">
        <a href="${pageContext.request.contextPath}/bookings" class="text-blue-600 hover:underline mr-4">← Назад</a>
        <h1 class="text-xl font-bold text-blue-600">Детали брони</h1>
    </header>

    <main class="flex-1 container mx-auto px-4 py-6 max-w-3xl w-full">
        <c:choose>
            <c:when test="${not empty booking}">
                <div class="bg-white rounded-lg shadow p-6 space-y-4">
                    <div>
                        <span class="text-sm px-2 py-1 rounded
                            <c:choose>
                                <c:when test="${booking.status == 'active'}">bg-green-100 text-green-800</c:when>
                                <c:when test="${booking.status == 'cancelled'}">bg-gray-200 text-gray-600</c:when>
                                <c:when test="${booking.status == 'late_cancel'}">bg-yellow-100 text-yellow-800</c:when>
                                <c:when test="${booking.status == 'center_cancelled'}">bg-red-100 text-red-800</c:when>
                            </c:choose>">
                            <c:choose>
                                <c:when test="${booking.status == 'active'}">Активна</c:when>
                                <c:when test="${booking.status == 'cancelled'}">Отменена</c:when>
                                <c:when test="${booking.status == 'late_cancel'}">Поздняя отмена</c:when>
                                <c:when test="${booking.status == 'center_cancelled'}">Отменён центром</c:when>
                            </c:choose>
                        </span>
                        <c:if test="${booking.status == 'center_cancelled' && not empty booking.cancellationReason}">
                            <p class="text-sm text-red-600 mt-1">Причина: ${booking.cancellationReason}</p>
                        </c:if>
                    </div>

                    <p class="text-2xl font-bold">${booking.slot.startAtFormatted}</p>
                    <p>${booking.slot.trackConfig.name} · ${booking.slot.marshal.name}</p>
                    <p>${booking.seatsCount} места
                        <c:if test="${booking.rentalGearCount > 0}">, прокат: ${booking.rentalGearCount}</c:if>
                    </p>
                    <p class="text-xl font-bold">${booking.priceTotal} ₽</p>
                    <p class="text-sm text-gray-500">Оплата на месте: наличные или перевод на карту.</p>

                    <c:if test="${not empty booking.slot.meetingPoint}">
                        <div class="text-sm text-gray-600">📍 ${booking.slot.meetingPoint}</div>
                    </c:if>
                    <c:if test="${not empty booking.slot.address}">
                        <div class="text-sm text-gray-600">Адрес: ${booking.slot.address}</div>
                    </c:if>

                    <!-- Действия -->
                    <c:if test="${booking.status == 'active'}">
                        <!-- Отмена, если не начался -->
                        <c:choose>
                            <c:when test="${booking.slot.startAt.isAfter(now)}">
                                <button onclick="showCancelModal()" class="w-full bg-red-600 text-white py-2 rounded-lg hover:bg-red-700">
                                    Отменить
                                </button>
                            </c:when>
                            <c:otherwise>
                                <span class="block w-full bg-gray-300 text-gray-500 text-center py-2 rounded-lg cursor-not-allowed">
                                    Заезд уже начался
                                </span>
                            </c:otherwise>
                        </c:choose>

                        <!-- Оценка, если завершён -->
                        <c:if test="${booking.slot.startAt.plusMinutes(booking.slot.durationMinutes).isBefore(now) and empty booking.rating}">
                            <button onclick="showRatingModal()" class="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 mt-2">
                                Оценить маршала
                            </button>
                        </c:if>
                        <c:if test="${not empty booking.rating}">
                            <div class="bg-gray-50 p-3 rounded mt-2">
                                <p class="font-medium">Ваша оценка</p>
                                <p class="text-yellow-500 text-xl">${'★'.repeat(booking.rating.value)}${'☆'.repeat(5 - booking.rating.value)}</p>
                                <c:if test="${not empty booking.rating.comment}">
                                    <p class="text-sm text-gray-600">${booking.rating.comment}</p>
                                </c:if>
                            </div>
                        </c:if>
                    </c:if>
                </div>
            </c:when>
            <c:otherwise>
                <div class="text-center py-10 text-gray-500">
                    <p>Бронь не найдена</p>
                    <a href="${pageContext.request.contextPath}/bookings" class="text-blue-600 hover:underline">Вернуться</a>
                </div>
            </c:otherwise>
        </c:choose>
    </main>

    <!-- Модалка отмены -->
    <div id="cancelModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center hidden">
        <div class="bg-white rounded-lg p-6 w-full max-w-md mx-4">
            <h2 class="text-xl font-bold mb-4">Отменить запись?</h2>
            <p class="text-gray-700 mb-4">Вы уверены, что хотите отменить бронь?</p>
            <div class="flex gap-2">
                <button onclick="confirmCancel()" class="flex-1 bg-red-600 text-white py-2 rounded hover:bg-red-700">Подтвердить</button>
                <button onclick="closeModal('cancelModal')" class="flex-1 bg-gray-200 text-gray-800 py-2 rounded hover:bg-gray-300">Не отменять</button>
            </div>
        </div>
    </div>

    <!-- Модалка оценки -->
    <div id="ratingModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center hidden">
        <div class="bg-white rounded-lg p-6 w-full max-w-md mx-4">
            <h2 class="text-xl font-bold mb-4">Оцените маршала ${booking.slot.marshal.name}</h2>
            <div class="flex gap-1 text-3xl justify-center" id="ratingStars">
                <span class="cursor-pointer" onclick="setRating(1)">☆</span>
                <span class="cursor-pointer" onclick="setRating(2)">☆</span>
                <span class="cursor-pointer" onclick="setRating(3)">☆</span>
                <span class="cursor-pointer" onclick="setRating(4)">☆</span>
                <span class="cursor-pointer" onclick="setRating(5)">☆</span>
            </div>
            <textarea id="ratingComment" placeholder="Комментарий (необязательно)" class="w-full border rounded p-2 mt-4" maxlength="500"></textarea>
            <div class="flex gap-2 mt-4">
                <button onclick="submitRating()" id="submitRatingBtn" class="flex-1 bg-blue-600 text-white py-2 rounded hover:bg-blue-700 disabled:opacity-50" disabled>Отправить</button>
                <button onclick="closeModal('ratingModal')" class="flex-1 bg-gray-200 text-gray-800 py-2 rounded hover:bg-gray-300">Пропустить</button>
            </div>
        </div>
    </div>

    <script>
        let selectedRating = 0;
        let currentBookingId = '${booking.id}';
        let currentMarshalId = '${booking.slot.marshal.id}';

        function showCancelModal() {
            document.getElementById('cancelModal').classList.remove('hidden');
        }

        function confirmCancel() {
            axios.post('${pageContext.request.contextPath}/web/bookings/' + currentBookingId + '/cancel?confirm=true')
                .then(() => {
                    location.reload();
                })
                .catch(err => {
                    alert('Ошибка отмены: ' + (err.response?.data || err.message));
                });
        }

        function showRatingModal() {
            document.getElementById('ratingModal').classList.remove('hidden');
            resetRating();
        }

        function setRating(val) {
            selectedRating = val;
            const stars = document.getElementById('ratingStars').children;
            for (let i = 0; i < stars.length; i++) {
                stars[i].textContent = i < val ? '★' : '☆';
            }
            document.getElementById('submitRatingBtn').disabled = false;
        }

        function resetRating() {
            selectedRating = 0;
            const stars = document.getElementById('ratingStars').children;
            for (let i = 0; i < stars.length; i++) {
                stars[i].textContent = '☆';
            }
            document.getElementById('submitRatingBtn').disabled = true;
            document.getElementById('ratingComment').value = '';
        }

        function submitRating() {
            if (selectedRating === 0) return;
            const comment = document.getElementById('ratingComment').value;
            axios.post('${pageContext.request.contextPath}/web/ratings', {
                marshalId: currentMarshalId,
                bookingId: currentBookingId,
                value: selectedRating,
                comment: comment || null
            }, {
                headers: { 'Content-Type': 'application/json' }
            })
            .then(() => {
                location.reload();
            })
            .catch(err => {
                alert('Ошибка отправки оценки: ' + (err.response?.data || err.message));
            });
        }

        function closeModal(id) {
            document.getElementById(id).classList.add('hidden');
            if (id === 'ratingModal') resetRating();
        }

        // Закрытие модалки по клику на фон
        document.querySelectorAll('.fixed.inset-0').forEach(el => {
            el.addEventListener('click', function(e) {
                if (e.target === this) {
                    this.classList.add('hidden');
                    if (this.id === 'ratingModal') resetRating();
                }
            });
        });
    </script>
</body>
</html>