package dev.jbyanx.finsight.domain.debt.service.impl;

import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.UserRepository;
import dev.jbyanx.finsight.domain.debt.dto.*;
import dev.jbyanx.finsight.domain.debt.entity.FixedDebt;
import dev.jbyanx.finsight.domain.debt.mapper.FixedDebtMapper;
import dev.jbyanx.finsight.domain.debt.repository.FixedDebtRepository;
import dev.jbyanx.finsight.domain.debt.service.FixedDebtService;
import dev.jbyanx.finsight.domain.transaction.dto.TransactionRequest;
import dev.jbyanx.finsight.domain.transaction.entity.Transaction.OriginType;
import dev.jbyanx.finsight.domain.transaction.service.TransactionService;
import dev.jbyanx.finsight.shared.exception.DebtAlreadyPaidOffException;
import dev.jbyanx.finsight.shared.exception.PaymentExceedsBalanceException;
import dev.jbyanx.finsight.shared.exception.ResourceNotFoundException;
import dev.jbyanx.finsight.shared.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FixedDebtServiceImpl implements FixedDebtService {

    private final FixedDebtRepository fixedDebtRepository;
    private final UserRepository userRepository;
    private final FixedDebtMapper fixedDebtMapper;
    private final TransactionService transactionService;

    @Override
    @Transactional
    public FixedDebtResponse create(String userId, FixedDebtRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        FixedDebt debt = FixedDebt.builder()
                .user(user)
                .institution(request.getInstitution())
                .description(request.getDescription())
                .totalAmount(request.getTotalAmount())
                .monthlyPayment(request.getMonthlyPayment())
                .paymentDueDay(request.getPaymentDueDay())
                .remainingBalance(request.getRemainingBalance())
                .build();

        return fixedDebtMapper.toResponse(fixedDebtRepository.save(debt));
    }

    @Override
    public List<FixedDebtResponse> getAllByUser(String userId) {
        return fixedDebtMapper.toResponseList(
                fixedDebtRepository.findAllByUserIdAndActiveTrue(userId));
    }

    @Override
    public FixedDebtResponse getById(String userId, String debtId) {
        return fixedDebtMapper.toResponse(findDebtBelongingToUser(userId, debtId));
    }

    @Override
    @Transactional
    public FixedDebtResponse patch(String userId, String debtId,
                                   FixedDebtPatchRequest request) {
        FixedDebt debt = findDebtBelongingToUser(userId, debtId);

        if (request.getDescription() != null)
            debt.setDescription(request.getDescription());
        if (request.getMonthlyPayment() != null)
            debt.setMonthlyPayment(request.getMonthlyPayment());
        if (request.getPaymentDueDay() != null)
            debt.setPaymentDueDay(request.getPaymentDueDay());

        return fixedDebtMapper.toResponse(fixedDebtRepository.save(debt));
    }

    @Override
    @Transactional
    public void delete(String userId, String debtId) {
        FixedDebt debt = findDebtBelongingToUser(userId, debtId);
        debt.setActive(false);
        fixedDebtRepository.save(debt);
    }

    @Override
    @Transactional
    public FixedDebtResponse applyPayment(String userId, String debtId,
                                          DebtPaymentRequest request) {
        FixedDebt debt = findDebtBelongingToUser(userId, debtId);

        // Verifica que la deuda no está ya saldada
        if (debt.isPaidOff()) {
            throw new DebtAlreadyPaidOffException(debt.getInstitution());
        }

        // Verifica que el pago no excede el saldo restante
        if (request.getAmount().compareTo(debt.getRemainingBalance()) > 0) {
            throw new PaymentExceedsBalanceException(
                    debt.getRemainingBalance().toString());
        }

        // El descuento de la fuente va PRIMERO: create() valida el saldo disponible y
        // lanza si no alcanza, así la deuda no se toca. Todo bajo la misma @Transactional.
        if (request.getOriginType() != null) {
            discountFromSource(userId, debt, request);
        }

        // applyPayment en la entidad actualiza el balance
        // y desactiva automáticamente si queda en cero
        debt.applyPayment(request.getAmount());

        return fixedDebtMapper.toResponse(fixedDebtRepository.save(debt));
    }

    /**
     * Delega en TransactionService: ahí ya vive la validación de saldo, el manejo de
     * bolsillos y dinero de terceros, y el descuento. Además deja el movimiento
     * trazable en el historial en vez de que la plata desaparezca sin rastro.
     */
    private void discountFromSource(String userId, FixedDebt debt,
                                    DebtPaymentRequest request) {
        OriginType originType = request.getOriginType();

        if (originType == OriginType.CREDIT_CARD) {
            throw new UnauthorizedException(
                    "A debt cannot be paid with a credit card: that moves the debt "
                            + "instead of settling it. Use CASH or SAVINGS.");
        }

        if (originType == OriginType.SAVINGS && request.getSavingsAccountId() == null) {
            throw new UnauthorizedException(
                    "savingsAccountId is required when originType is SAVINGS");
        }

        transactionService.create(userId, TransactionRequest.builder()
                .description("Pago deuda " + debt.getInstitution())
                .category("Deuda")
                .amount(request.getAmount())
                .originType(originType)
                .savingsAccountId(request.getSavingsAccountId())
                .pocketId(request.getPocketId())
                .build());
    }

    private FixedDebt findDebtBelongingToUser(String userId, String debtId) {
        return fixedDebtRepository.findByIdAndUserId(debtId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("FixedDebt", debtId));
    }
}