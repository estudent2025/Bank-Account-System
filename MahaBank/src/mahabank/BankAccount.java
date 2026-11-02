package mahabank;

import java.io.Serializable;

public abstract class BankAccount implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int accountNumber;
    private final String holderName;
    private double balance;
    private final String accountType;

    protected BankAccount(int accountNumber, String holderName, double openingBalance,
                          String accountType) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.balance = openingBalance;
        this.accountType = accountType;
    }

    public synchronized void deposit(double amount) throws InvalidAmountException {
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new InvalidAmountException("Enter a deposit amount greater than zero.");
        }
        balance = roundMoney(balance + amount);
    }

    public synchronized void withdraw(double amount)
            throws InvalidAmountException, InsufficientBalanceException {
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new InvalidAmountException("Enter a withdrawal amount greater than zero.");
        }
        if (amount > balance) {
            throw new InsufficientBalanceException(
                String.format("Insufficient balance. Available balance: ₹%.2f", balance));
        }
        balance = roundMoney(balance - amount);
    }

    public abstract double calculateMonthlyInterest();

    public synchronized void creditMonthlyInterest() throws InvalidAmountException {
        double interest = calculateMonthlyInterest();
        if (interest > 0) {
            deposit(interest);
        }
    }

    protected static double roundMoney(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    public int getAccountNumber() { return accountNumber; }
    public String getHolderName() { return holderName; }
    public synchronized double getBalance() { return balance; }
    public String getAccountType() { return accountType; }
}
