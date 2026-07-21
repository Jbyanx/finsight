package dev.jbyanx.finsight.domain.card.service.impl;

import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.UserRepository;
import dev.jbyanx.finsight.domain.card.dto.CreditCardPatchRequest;
import dev.jbyanx.finsight.domain.card.dto.CreditCardRequest;
import dev.jbyanx.finsight.domain.card.dto.CreditCardResponse;
import dev.jbyanx.finsight.domain.card.entity.CreditCard;
import dev.jbyanx.finsight.domain.card.mapper.CreditCardMapper;
import dev.jbyanx.finsight.domain.card.repository.CreditCardRepository;
import dev.jbyanx.finsight.shared.exception.ResourceNotFoundException;
import dev.jbyanx.finsight.shared.exception.InvalidCreditLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditCardServiceImplTest {

    @Mock
    private CreditCardRepository creditCardRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CreditCardMapper creditCardMapper;

    @InjectMocks
    private CreditCardServiceImpl creditCardService;

    private User mockUser;
    private CreditCard mockCard;
    private CreditCardRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id("user-1").build();

        mockCard = CreditCard.builder()
                .id("card-1")
                .user(mockUser)
                .institution("Nu")
                .creditLimit(new BigDecimal("400000"))
                .usedAmount(new BigDecimal("100000"))
                .active(true)
                .build();

        mockRequest = CreditCardRequest.builder()
                .institution("Nu")
                .nickname("Mi Tarjeta")
                .creditLimit(new BigDecimal("400000"))
                .cutOffDay(3)
                .paymentDueDay(23)
                .build();
    }

    @Test
    void create_Success() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(mockUser));
        when(creditCardRepository.save(any(CreditCard.class))).thenReturn(mockCard);

        CreditCardResponse mockResponse = CreditCardResponse.builder().id("card-1").build();
        when(creditCardMapper.toResponse(mockCard)).thenReturn(mockResponse);

        CreditCardResponse response = creditCardService.create("user-1", mockRequest);

        assertNotNull(response);
        assertEquals("card-1", response.getId());
        verify(creditCardRepository).save(any(CreditCard.class));
    }

    @Test
    void create_UserNotFound_ThrowsException() {
        when(userRepository.findById("user-bad")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> creditCardService.create("user-bad", mockRequest));

        verify(creditCardRepository, never()).save(any());
    }

    @Test
    void patch_Success() {
        CreditCardPatchRequest patchRequest = CreditCardPatchRequest.builder()
                .nickname("Nu Actualizada")
                .creditLimit(new BigDecimal("500000")) // Aumentando el cupo
                .build();

        when(creditCardRepository.findByIdAndUserId("card-1", "user-1"))
                .thenReturn(Optional.of(mockCard));
        when(creditCardRepository.save(any(CreditCard.class))).thenReturn(mockCard);

        creditCardService.patch("user-1", "card-1", patchRequest);

        assertEquals("Nu Actualizada", mockCard.getNickname());
        assertEquals(new BigDecimal("500000"), mockCard.getCreditLimit());
        verify(creditCardRepository).save(mockCard);
    }

    @Test
    void patch_LimitLessThanUsedAmount_ThrowsException() {
        CreditCardPatchRequest patchRequest = CreditCardPatchRequest.builder()
                .creditLimit(new BigDecimal("50000")) // Menor que los 100000 usados
                .build();

        when(creditCardRepository.findByIdAndUserId("card-1", "user-1"))
                .thenReturn(Optional.of(mockCard));

        assertThrows(InvalidCreditLimitException.class,
                () -> creditCardService.patch("user-1", "card-1", patchRequest));

        verify(creditCardRepository, never()).save(any());
    }

    @Test
    void update_Success() {
        // PUT que sube el cupo por encima de lo usado
        when(creditCardRepository.findByIdAndUserId("card-1", "user-1"))
                .thenReturn(Optional.of(mockCard));
        when(creditCardRepository.save(any(CreditCard.class))).thenReturn(mockCard);
        when(creditCardMapper.toResponse(mockCard))
                .thenReturn(CreditCardResponse.builder().id("card-1").build());

        mockRequest = CreditCardRequest.builder()
                .institution("Nu")
                .nickname("Mi Tarjeta")
                .creditLimit(new BigDecimal("500000")) // > usado 100000
                .cutOffDay(3)
                .paymentDueDay(23)
                .build();

        creditCardService.update("user-1", "card-1", mockRequest);

        assertEquals(new BigDecimal("500000"), mockCard.getCreditLimit());
        verify(creditCardRepository).save(mockCard);
    }

    @Test
    void update_LimitLessThanUsedAmount_ThrowsException_CardUntouched() {
        CreditCardRequest badRequest = CreditCardRequest.builder()
                .institution("Nubank")             // valores nuevos que NO deben aplicarse
                .nickname("Cambiada")
                .creditLimit(new BigDecimal("50000")) // < usado 100000
                .cutOffDay(9)
                .paymentDueDay(29)
                .build();

        when(creditCardRepository.findByIdAndUserId("card-1", "user-1"))
                .thenReturn(Optional.of(mockCard));

        assertThrows(InvalidCreditLimitException.class,
                () -> creditCardService.update("user-1", "card-1", badRequest));

        // La tarjeta queda intacta: ningun setter corrio antes de la validacion
        assertEquals("Nu", mockCard.getInstitution());
        assertEquals(new BigDecimal("400000"), mockCard.getCreditLimit());
        verify(creditCardRepository, never()).save(any());
    }

    @Test
    void getById_NotFound_ThrowsException() {
        when(creditCardRepository.findByIdAndUserId("card-bad", "user-1"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> creditCardService.getById("user-1", "card-bad"));
    }

    @Test
    void delete_LogicalDelete_Success() {
        when(creditCardRepository.findByIdAndUserId("card-1", "user-1"))
                .thenReturn(Optional.of(mockCard));

        creditCardService.delete("user-1", "card-1");

        assertFalse(mockCard.isActive());
        verify(creditCardRepository).save(mockCard);
    }

    @Test
    void getOrderedByUtilization_Success() {
        when(creditCardRepository.findAllByUserIdOrderByUtilizationAsc("user-1"))
                .thenReturn(List.of(mockCard));

        creditCardService.getOrderedByUtilization("user-1");

        verify(creditCardRepository).findAllByUserIdOrderByUtilizationAsc("user-1");
        verify(creditCardMapper).toResponseList(anyList());
    }
}