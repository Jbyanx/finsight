package dev.jbyanx.finsight.domain.ai.tool;

import dev.jbyanx.finsight.domain.ai.memory.Conversation;
import dev.jbyanx.finsight.domain.ai.memory.ConversationService;
import dev.jbyanx.finsight.domain.ai.pending.PendingAction;
import dev.jbyanx.finsight.domain.ai.pending.PendingActionExecutor;
import dev.jbyanx.finsight.domain.card.dto.CreditCardResponse;
import dev.jbyanx.finsight.domain.card.service.CreditCardService;
import dev.jbyanx.finsight.domain.cash.dto.CashResponse;
import dev.jbyanx.finsight.domain.cash.service.CashService;
import dev.jbyanx.finsight.domain.savings.dto.SavingsAccountResponse;
import dev.jbyanx.finsight.domain.savings.service.SavingsAccountService;
import dev.jbyanx.finsight.domain.transaction.entity.Transaction.OriginType;
import dev.jbyanx.finsight.shared.util.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Tool de ESCRITURA para registrar una transacción/gasto, en modo PREVIEW.
 *
 * Mismo patrón de seguridad y confirmación que {@link DebtWriteTools}: NO ejecuta.
 * Resuelve el origen del dinero (efectivo, una tarjeta o una cuenta) a partir de
 * texto natural, calcula la consecuencia real (utilización o saldo resultante) y
 * deja una {@link PendingAction} en la conversación. La confirmación se hace con
 * BOTONES inline; el toque del usuario dispara la ejecución en CÓDIGO
 * (PendingActionExecutor). El modelo solo propone.
 *
 * userId y chatId llegan por ToolContext, nunca del modelo.
 *
 * Resolución del origen (en orden): efectivo -> tarjeta por nombre/institución ->
 * cuenta de ahorro por nombre/institución. Si un nombre coincide con una tarjeta
 * y una cuenta, gana la tarjeta (el gasto por tarjeta es el caso más común).
 */
@Component
@RequiredArgsConstructor
public class TransactionWriteTools {

    private final CreditCardService creditCardService;
    private final SavingsAccountService savingsAccountService;
    private final CashService cashService;
    private final ConversationService conversationService;

    @Tool(description = "Prepara el registro de una transacción o gasto del usuario "
            + "(ej. 'pagué 130 mil en el mercado con la Nu', 'gasté 20 mil en efectivo'). "
            + "Parámetros: description (qué se gastó), amount (monto), origin (de dónde "
            + "sale el dinero: 'efectivo', o el nombre/institución de una tarjeta o cuenta), "
            + "category (opcional, ej. 'Comida'), installments (opcional, número de cuotas "
            + "si es compra a crédito). NO ejecuta el gasto: calcula la consecuencia real "
            + "(utilización de la tarjeta o saldo resultante) y el usuario confirma con un botón.")
    public String previewRegisterTransaction(
            String description,
            BigDecimal amount,
            String origin,
            String category,
            Integer installments,
            ToolContext toolContext) {

        String userId = (String) toolContext.getContext().get(ToolContextKeys.USER_ID);
        String chatId = (String) toolContext.getContext().get(ToolContextKeys.CHAT_ID);
        if (userId == null || chatId == null) {
            return "No se pudo identificar al usuario.";
        }
        if (description == null || description.isBlank()) {
            return "Necesito una descripción para registrar la transacción.";
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return "El monto del gasto debe ser mayor a cero.";
        }
        if (origin == null || origin.isBlank()) {
            return "Necesito saber de dónde sale el dinero: efectivo, una tarjeta o una cuenta.";
        }

        int inst = (installments != null && installments > 0) ? installments : 1;
        String q = origin.toLowerCase();

        // 1) Efectivo
        if (q.contains("efectivo") || q.contains("cash")) {
            CashResponse cash = cashService.getByUser(userId);
            if (amount.compareTo(cash.getBalance()) > 0) {
                return ("El gasto de %s supera el efectivo disponible (%s). "
                        + "Revisa el monto o el origen.")
                        .formatted(Money.format(amount), Money.format(cash.getBalance()));
            }
            BigDecimal newBalance = cash.getBalance().subtract(amount);
            String preview = "Registrar \"%s\" por %s en efectivo dejaría tu efectivo en %s. ¿Confirmas?"
                    .formatted(description, Money.format(amount), Money.format(newBalance));
            stashPending(chatId, userId, description, amount, category,
                    OriginType.CASH, null, null, inst, preview);
            return instruction(preview);
        }

        // 2) Tarjeta de crédito
        List<CreditCardResponse> cards = creditCardService.getAllByUser(userId);
        CreditCardResponse card = cards.stream()
                .filter(c -> nameMatches(c.getNickname(), c.getInstitution(), q))
                .findFirst()
                .orElse(null);
        if (card != null) {
            BigDecimal newUsed = card.getUsedAmount().add(amount);
            if (newUsed.compareTo(card.getCreditLimit()) > 0) {
                return ("El gasto de %s no cabe en el cupo de %s: usado %s de %s. "
                        + "Sugiere otra tarjeta o un monto menor.")
                        .formatted(Money.format(amount), cardName(card),
                                Money.format(card.getUsedAmount()),
                                Money.format(card.getCreditLimit()));
            }
            BigDecimal newUtil = newUsed.multiply(BigDecimal.valueOf(100))
                    .divide(card.getCreditLimit(), 2, RoundingMode.HALF_UP);
            String instTxt = inst > 1 ? " a %d cuotas".formatted(inst) : "";
            String preview = ("Registrar \"%s\" por %s%s con %s subiría tu utilización a %s%% "
                    + "(usado %s de %s). ¿Confirmas?")
                    .formatted(description, Money.format(amount), instTxt, cardName(card),
                            newUtil, Money.format(newUsed), Money.format(card.getCreditLimit()));
            stashPending(chatId, userId, description, amount, category,
                    OriginType.CREDIT_CARD, card.getId(), null, inst, preview);
            return instruction(preview);
        }

        // 3) Cuenta de ahorro
        List<SavingsAccountResponse> accounts = savingsAccountService.getAllByUser(userId);
        SavingsAccountResponse acc = accounts.stream()
                .filter(a -> nameMatches(a.getNickname(), a.getInstitution(), q))
                .findFirst()
                .orElse(null);
        if (acc != null) {
            if (amount.compareTo(acc.getBalance()) > 0) {
                return ("El gasto de %s supera el saldo de %s (%s). "
                        + "Revisa el monto o el origen.")
                        .formatted(Money.format(amount), accName(acc),
                                Money.format(acc.getBalance()));
            }
            BigDecimal newBalance = acc.getBalance().subtract(amount);
            String preview = "Registrar \"%s\" por %s desde %s dejaría el saldo en %s. ¿Confirmas?"
                    .formatted(description, Money.format(amount), accName(acc),
                            Money.format(newBalance));
            stashPending(chatId, userId, description, amount, category,
                    OriginType.SAVINGS, null, acc.getId(), inst, preview);
            return instruction(preview);
        }

        // No se pudo resolver el origen — no dejamos nada pendiente.
        return ("No encontré un origen que coincida con \"" + origin + "\". "
                + "El usuario puede pagar con: efectivo, "
                + cards.stream().map(this::cardName).toList()
                + " o " + accounts.stream().map(this::accName).toList() + ".");
    }

    private boolean nameMatches(String nickname, String institution, String q) {
        boolean inNickname = nickname != null && nickname.toLowerCase().contains(q);
        boolean inInstitution = institution != null && institution.toLowerCase().contains(q);
        return inNickname || inInstitution;
    }

    private String cardName(CreditCardResponse c) {
        return (c.getNickname() != null && !c.getNickname().isBlank())
                ? c.getNickname() : c.getInstitution();
    }

    private String accName(SavingsAccountResponse a) {
        return (a.getNickname() != null && !a.getNickname().isBlank())
                ? a.getNickname() : a.getInstitution();
    }

    private void stashPending(String chatId, String userId, String description, BigDecimal amount,
                              String category, OriginType originType, String cardId, String accId,
                              int inst, String previewText) {
        Conversation conv = conversationService.getOrCreate(chatId, userId);
        PendingAction action = PendingAction
                .of(PendingActionExecutor.REGISTER_TRANSACTION, previewText)
                .with("description", description)
                .with("amount", amount.toPlainString())
                .with("originType", originType.name())
                .with("installments", String.valueOf(inst));
        if (category != null && !category.isBlank()) action.with("category", category);
        if (cardId != null) action.with("creditCardId", cardId);
        if (accId != null) action.with("savingsAccountId", accId);
        conversationService.setPending(conv, action);
    }

    private String instruction(String previewText) {
        return ("PROPUESTA preparada. Comunícale al usuario esta consecuencia de forma "
                + "clara y breve: " + previewText + " Aparecerán botones para confirmar "
                + "o cancelar; no le pidas que escriba nada.");
    }
}