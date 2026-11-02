package mahabank;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String time;
    private final String type;
    private final int accountNumber;
    private final String details;
    private final double amount;
    private final double balanceAfter;

    public Transaction(String type, int accountNumber, String details,
                       double amount, double balanceAfter) {
        this.time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.type = type;
        this.accountNumber = accountNumber;
        this.details = details;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }

    public String getTime() { return time; }
    public String getType() { return type; }
    public int getAccountNumber() { return accountNumber; }
    public String getDetails() { return details; }
    public double getAmount() { return amount; }
    public double getBalanceAfter() { return balanceAfter; }
}
