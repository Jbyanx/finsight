package dev.jbyanx.finsight.domain.savings.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * El caso Nequi: una cuenta PROPIA que aloja dinero de terceros.
 * Antes del fix, marcarla shared borraba TODO su saldo de los cálculos.
 */
class SavingsAccountTest {

    private Pocket pocket(String name, String amount, boolean shared, String owner) {
        return Pocket.builder()
                .name(name)
                .balance(new BigDecimal(amount))
                .shared(shared)
                .sharedOwnerName(owner)
                .active(true)
                .build();
    }

    private SavingsAccount nequi(List<Pocket> pockets) {
        return SavingsAccount.builder()
                .institution("Nequi")
                .balance(new BigDecimal("1000000"))
                .shared(false)
                .pockets(pockets)
                .build();
    }

    @Test
    void sharedTotal_SumsOnlySharedPockets() {
        SavingsAccount acc = nequi(List.of(
                pocket("Papá", "300000", true, "Papá"),
                pocket("Tía", "100000", true, "Tía"),
                pocket("Viaje", "200000", false, null)));

        assertEquals(0, new BigDecimal("400000").compareTo(acc.getSharedTotal()));
    }

    @Test
    void ownBalance_ExcludesThirdPartyMoneyButKeepsOwnGoals() {
        SavingsAccount acc = nequi(List.of(
                pocket("Papá", "300000", true, "Papá"),
                pocket("Viaje", "200000", false, null)));

        // 1.000.000 - 300.000 (de Papá) = 700.000. El bolsillo Viaje SIGUE siendo mío.
        assertEquals(0, new BigDecimal("700000").compareTo(acc.getOwnBalance()));
    }

    @Test
    void availableBalance_ExcludesAllActivePockets() {
        SavingsAccount acc = nequi(List.of(
                pocket("Papá", "300000", true, "Papá"),
                pocket("Viaje", "200000", false, null)));

        // Libre = 1.000.000 - 300.000 - 200.000 = 500.000
        assertEquals(0, new BigDecimal("500000").compareTo(acc.getAvailableBalance()));
    }

    @Test
    void inactivePockets_AreIgnored() {
        Pocket closed = pocket("Viejo", "500000", true, "Ex");
        closed.setActive(false);

        SavingsAccount acc = nequi(List.of(closed));

        assertEquals(0, BigDecimal.ZERO.compareTo(acc.getSharedTotal()));
        assertEquals(0, new BigDecimal("1000000").compareTo(acc.getOwnBalance()));
    }

    @Test
    void fullySharedAccount_IsEntirelyThirdPartyMoney() {
        SavingsAccount acc = SavingsAccount.builder()
                .institution("Cuenta conjunta")
                .balance(new BigDecimal("500000"))
                .shared(true)
                .pockets(List.of())
                .build();

        assertEquals(0, new BigDecimal("500000").compareTo(acc.getSharedTotal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(acc.getOwnBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(acc.getAvailableBalance()));
    }

    @Test
    void noPockets_OwnBalanceEqualsFullBalance() {
        SavingsAccount acc = nequi(List.of());

        assertEquals(0, new BigDecimal("1000000").compareTo(acc.getOwnBalance()));
        assertEquals(0, new BigDecimal("1000000").compareTo(acc.getAvailableBalance()));
    }
}