package mahabank;

public class SavingsAccount extends BankAccount {
    private static final double ANNUAL_INTEREST_RATE = 4.0;

    public SavingsAccount(int number, String name, double openingBalance) {
        super(number, name, openingBalance, "SAVINGS");
    }

    @Override
    public double calculateMonthlyInterest() {
        return Math.round(getBalance() * ANNUAL_INTEREST_RATE / 100.0 / 12.0 * 100.0) / 100.0;
    }
}
