package by.bsuir.kalplat.service;

import by.bsuir.kalplat.domain.model.PaymentRequest;
import by.bsuir.kalplat.domain.model.RequestStatus;
import by.bsuir.kalplat.domain.model.User;
import by.bsuir.kalplat.web.dto.RequestCreateDto;

import java.util.List;

/** Обработка заявок на оплату. */
public interface PaymentRequestService {

    PaymentRequest create(RequestCreateDto dto, User author);

    PaymentRequest submit(Long id, User author);

    PaymentRequest approve(Long id, User user);

    PaymentRequest reject(Long id, User user, String reason);

    PaymentRequest findById(Long id);

    List<PaymentRequest> findAll(RequestStatus status);
}
