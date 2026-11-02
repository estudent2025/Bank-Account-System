package mahabank;

public class CurrentAccount extends BankAccount {
    public CurrentAccount(int number, String name, double openingBalance) {
        super(number, name, openingBalance, "CURRENT");
    }

    @Override
    public double calculateMonthlyInterest() {
        return 0.0;
    }
}
