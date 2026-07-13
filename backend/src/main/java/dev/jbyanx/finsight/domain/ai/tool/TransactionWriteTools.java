package dev.jbyanx.finsight.domain.ai.tool;

import dev.jbyanx.finsight.domain.ai.memory.Conversation;
import dev.jbyanx.finsight.domain.ai.memory.ConversationService;
import dev.jbyanx.finsight.domain.ai.pending.PendingAction;
import dev.jbyanx.finsight.domain.ai.pending.PendingActionExecutor;
import dev.jbyanx.finsight.domain.card.dto.CreditCardResponse;
import dev.jbyanx.finsight.domain.card.service.CreditCardService;
import dev.jbyanx.finsight.domain.cash.dto.CashResponse;
import dev.jbyanx.finsight.domain.cash.service.CashService;
import dev.jbyanx.finsight.domain.savings.dto.PocketResponse;
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
 * NO ejecuta: resuelve el origen del dinero, calcula la consecuencia real y deja
 * una {@link PendingAction}. El usuario confirma con BOTONES y el CÓDIGO ejecuta
 * (PendingActionExecutor). userId y chatId llegan por ToolContext, nunca del modelo.
 *
 * DINERO DE TERCEROS:
 * El parámetro moneyOwner permite pagar con la plata de otra persona alojada en
 * una cuenta del usuario ("paga la factura con la plata de mi papá"). Busca un
 * bolsillo COMPARTIDO cuyo dueño coincida, valida contra el saldo de ESE bolsillo
 * y avisa explícitamente en el preview que no es dinero del usuario. Esa
 * advertencia explícita es la garantía de que nadie gasta plata ajena sin querer.
 */
@Component
@RequiredArgsConstructor
public class TransactionWriteTools {

    private final CreditCardService creditCardService;
    private final SavingsAccountService savingsAccountService;
    private final CashService cashService;
    private final ConversationService conversationService;

    @Tool(description = "Prepara el registro de una transacción o gasto "
            + "(ej. 'gasté 20 mil en un helado con la RappiCard'). Parámetros: "
            + "description (qué se gastó), amount (monto), origin (de dónde sale el "
            + "dinero: 'efectivo', o el nombre de una tarjeta o cuenta), category "
            + "(opcional), installments (opcional, número de cuotas), moneyOwner "
            + "(OPCIONAL: nombre del dueño del dinero si el usuario dice que paga con "
            + "la plata de otra persona, ej. 'papá', 'mi tía'. Dejar vacío si es dinero "
            + "propio). NO ejecuta: calcula la consecuencia y el usuario confirma con un botón.")
    public String previewRegisterTransaction(
            String description,
            BigDecimal amount,
            String origin,
            String category,
            Integer installments,
            String moneyOwner,
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
        boolean useThirdPartyMoney = moneyOwner != null && !moneyOwner.isBlank();
        String q = origin.toLowerCase();

        // Dinero de terceros solo existe dentro de bolsillos de cuentas.
        if (useThirdPartyMoney && (q.contains("efectivo") || q.contains("cash"))) {
            return "El dinero de otra persona debe estar en un bolsillo de una cuenta, "
                    + "no en efectivo. Indica de qué cuenta sale.";
        }

        // 1) Efectivo
        if (q.contains("efectivo") || q.contains("cash")) {
            CashResponse cash = cashService.getByUser(userId);
            if (amount.compareTo(cash.getBalance()) > 0) {
                return ("El gasto de %s supera el efectivo disponible (%s). "
                        + "Revisa el monto o el origen.")
                        .formatted(Money.format(amount), Money.format(cash.getBalance()));
            }
            String preview = "Registrar \"%s\" por %s en efectivo dejaría tu efectivo en %s. ¿Confirmas?"
                    .formatted(description, Money.format(amount),
                            Money.format(cash.getBalance().subtract(amount)));
            stash(chatId, userId, description, amount, category,
                    OriginType.CASH, null, null, null, inst, preview);
            return instruction(preview);
        }

        // 2) Tarjeta de crédito (no aplica dinero de terceros)
        List<CreditCardResponse> cards = creditCardService.getAllByUser(userId);
        CreditCardResponse card = cards.stream()
                .filter(c -> matches(c.getNickname(), c.getInstitution(), q))
                .findFirst().orElse(null);
        if (card != null && !useThirdPartyMoney) {
            BigDecimal newUsed = card.getUsedAmount().add(amount);
            if (newUsed.compareTo(card.getCreditLimit()) > 0) {
                return ("El gasto de %s no cabe en el cupo de %s: usado %s de %s. "
                        + "Sugiere otra tarjeta o un monto menor.")
                        .formatted(Money.format(amount), cardName(card),
                                Money.format(card.getUsedAmount()),
                                Money.format(card.getCreditLimit()));
            }
            BigDecimal util = newUsed.multiply(BigDecimal.valueOf(100))
                    .divide(card.getCreditLimit(), 2, RoundingMode.HALF_UP);
            String instTxt = inst > 1 ? " a %d cuotas".formatted(inst) : "";
            String preview = ("Registrar \"%s\" por %s%s con %s subiría tu utilización a %s%% "
                    + "(usado %s de %s). ¿Confirmas?")
                    .formatted(description, Money.format(amount), instTxt, cardName(card),
                            util, Money.format(newUsed), Money.format(card.getCreditLimit()));
            stash(chatId, userId, description, amount, category,
                    OriginType.CREDIT_CARD, card.getId(), null, null, inst, preview);
            return instruction(preview);
        }
        if (card != null) {
            return "No se puede pagar con dinero de otra persona usando una tarjeta de "
                    + "crédito. El dinero de terceros vive en bolsillos de cuentas.";
        }

        // 3) Cuenta de ahorro
        List<SavingsAccountResponse> accounts = savingsAccountService.getAllByUser(userId);
        SavingsAccountResponse acc = accounts.stream()
                .filter(a -> matches(a.getNickname(), a.getInstitution(), q))
                .findFirst().orElse(null);

        if (acc == null) {
            return ("No encontré un origen que coincida con \"" + origin + "\". "
                    + "El usuario puede pagar con: efectivo, "
                    + cards.stream().map(this::cardName).toList()
                    + " o " + accounts.stream().map(this::accName).toList() + ".");
        }

        // 3a) Pago con dinero de un TERCERO (bolsillo compartido)
        if (useThirdPartyMoney) {
            String owner = moneyOwner.toLowerCase();
            PocketResponse pocket = acc.getPockets().stream()
                    .filter(PocketResponse::isShared)
                    .filter(p -> p.getSharedOwnerName() != null
                            && (p.getSharedOwnerName().toLowerCase().contains(owner)
                            || owner.contains(p.getSharedOwnerName().toLowerCase())))
                    .findFirst().orElse(null);

            if (pocket == null) {
                List<String> owners = acc.getPockets().stream()
                        .filter(PocketResponse::isShared)
                        .map(PocketResponse::getSharedOwnerName)
                        .toList();
                return ("En %s no hay un bolsillo con dinero de \"%s\". Dueños disponibles: %s.")
                        .formatted(accName(acc), moneyOwner,
                                owners.isEmpty() ? "ninguno" : owners);
            }

            if (amount.compareTo(pocket.getBalance()) > 0) {
                return ("El gasto de %s supera el dinero de %s en %s (%s).")
                        .formatted(Money.format(amount), pocket.getSharedOwnerName(),
                                accName(acc), Money.format(pocket.getBalance()));
            }

            String preview = ("Registrar \"%s\" por %s con el dinero de %s (bolsillo \"%s\" en %s). "
                    + "OJO: esta plata NO es tuya; el bolsillo quedaría en %s y tu patrimonio "
                    + "no cambia. ¿Confirmas?")
                    .formatted(description, Money.format(amount), pocket.getSharedOwnerName(),
                            pocket.getName(), accName(acc),
                            Money.format(pocket.getBalance().subtract(amount)));
            stash(chatId, userId, description, amount, category,
                    OriginType.SAVINGS, null, acc.getId(), pocket.getId(), inst, preview);
            return instruction(preview);
        }

        // 3b) Gasto propio: se valida contra el dinero LIBRE, no el saldo físico.
        BigDecimal available = acc.getAvailableBalance();
        if (amount.compareTo(available) > 0) {
            BigDecimal reserved = acc.getBalance().subtract(available);
            return ("El gasto de %s supera tu dinero libre en %s (%s). El saldo total es %s, "
                    + "pero %s está apartado en bolsillos (metas propias o dinero de otras "
                    + "personas). Si la plata debe salir de un bolsillo, pregúntale al usuario "
                    + "de cuál.")
                    .formatted(Money.format(amount), accName(acc), Money.format(available),
                            Money.format(acc.getBalance()), Money.format(reserved));
        }

        String preview = "Registrar \"%s\" por %s desde %s dejaría tu dinero libre en %s. ¿Confirmas?"
                .formatted(description, Money.format(amount), accName(acc),
                        Money.format(available.subtract(amount)));
        stash(chatId, userId, description, amount, category,
                OriginType.SAVINGS, null, acc.getId(), null, inst, preview);
        return instruction(preview);
    }

    private boolean matches(String nickname, String institution, String q) {
        return (nickname != null && nickname.toLowerCase().contains(q))
                || (institution != null && institution.toLowerCase().contains(q));
    }

    private String cardName(CreditCardResponse c) {
        return (c.getNickname() != null && !c.getNickname().isBlank())
                ? c.getNickname() : c.getInstitution();
    }

    private String accName(SavingsAccountResponse a) {
        return (a.getNickname() != null && !a.getNickname().isBlank())
                ? a.getNickname() : a.getInstitution();
    }

    private void stash(String chatId, String userId, String description, BigDecimal amount,
                       String category, OriginType originType, String cardId, String accId,
                       String pocketId, int inst, String previewText) {
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
        if (pocketId != null) action.with("pocketId", pocketId);
        conversationService.setPending(conv, action);
    }

    private String instruction(String previewText) {
        return ("PROPUESTA preparada. Comunícale al usuario esta consecuencia de forma "
                + "clara y breve: " + previewText + " Si la plata es de otra persona, dilo "
                + "explícitamente. Aparecerán botones para confirmar o cancelar; no le pidas "
                + "que escriba nada.");
    }
}