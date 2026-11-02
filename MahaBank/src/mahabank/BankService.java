package mahabank;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class BankService {
    private final Map<Integer, BankAccount> accounts = new LinkedHashMap<>();
    private final List<Transaction> transactions = new ArrayList<>();
    private final Path dataDir = Paths.get("data");
    private final Path accountsFile = dataDir.resolve("accounts.dat");
    private final Path transactionsFile = dataDir.resolve("transactions.dat");
    private int nextAccountNumber = 1001;

    public BankService() {
        loadData();
        if (accounts.isEmpty()) {
            try {
                createAccount("Junaid Mohiuddin", "SAVINGS", 15000.0);
            } catch (BankException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    public synchronized BankAccount createAccount(String name, String type, double openingBalance)
            throws BankException {
        if (name == null || name.trim().isEmpty()) {
            throw new BankException("Account holder name cannot be blank.");
        }
        if (!Double.isFinite(openingBalance) || openingBalance < 0) {
            throw new InvalidAmountException("Opening balance cannot be negative.");
        }
        String normalizedType = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        int number = nextAccountNumber++;
        BankAccount account;
        if ("SAVINGS".equals(normalizedType)) {
            account = new SavingsAccount(number, name.trim(), openingBalance);
        } else if ("CURRENT".equals(normalizedType)) {
            account = new CurrentAccount(number, name.trim(), openingBalance);
        } else {
            throw new BankException("Choose Savings or Current account.");
        }
        accounts.put(number, account);
        record("OPEN", account, "Account opened", openingBalance);
        saveData();
        return account;
    }

    public synchronized BankAccount findAccount(int number) throws AccountNotFoundException {
        BankAccount account = accounts.get(number);
        if (account == null) {
            throw new AccountNotFoundException("No account found with number " + number + ".");
        }
        return account;
    }

    public synchronized void deposit(int number, double amount) throws BankException {
        BankAccount account = findAccount(number);
        account.deposit(amount);
        record("DEPOSIT", account, "Cash deposit", amount);
        saveData();
    }

    public synchronized void withdraw(int number, double amount) throws BankException {
        BankAccount account = findAccount(number);
        account.withdraw(amount);
        record("WITHDRAW", account, "Cash withdrawal", amount);
        saveData();
    }

    public synchronized void transfer(int fromNumber, int toNumber, double amount) throws BankException {
        if (fromNumber == toNumber) {
            throw new BankException("Source and destination accounts must be different.");
        }
        BankAccount from = findAccount(fromNumber);
        BankAccount to = findAccount(toNumber);
        // Validate before changing either balance so a failed transfer cannot lose money.
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new InvalidAmountException("Transfer amount must be greater than zero.");
        }
        if (amount > from.getBalance()) {
            throw new InsufficientBalanceException(
                String.format("Insufficient balance. Available balance: ₹%.2f", from.getBalance()));
        }
        from.withdraw(amount);
        to.deposit(amount);
        record("TRANSFER OUT", from, "Transfer to A/C " + toNumber, amount);
        record("TRANSFER IN", to, "Transfer from A/C " + fromNumber, amount);
        saveData();
    }

    public synchronized double creditMonthlyInterest(int number) throws BankException {
        BankAccount account = findAccount(number);
        double interest = account.calculateMonthlyInterest();
        if (interest <= 0) {
            throw new BankException("Monthly interest is available for Savings accounts only.");
        }
        account.creditMonthlyInterest();
        record("INTEREST", account, "Monthly savings interest (4% p.a.)", interest);
        saveData();
        return interest;
    }

    public synchronized List<BankAccount> getAccounts() {
        return new ArrayList<>(accounts.values());
    }

    public synchronized List<BankAccount> getAccountsSortedByBalance() {
        return accounts.values().stream()
                .sorted(Comparator.comparingDouble(BankAccount::getBalance).reversed())
                .collect(Collectors.toList());
    }

    public synchronized List<Transaction> getTransactionsFor(int number) {
        return transactions.stream()
                .filter(t -> t.getAccountNumber() == number)
                .collect(Collectors.toList());
    }

    public synchronized List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions);
    }

    private void record(String type, BankAccount account, String details, double amount) {
        transactions.add(new Transaction(type, account.getAccountNumber(), details,
                amount, account.getBalance()));
    }

    private void saveData() {
        try {
            Files.createDirectories(dataDir);
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(accountsFile))) {
                out.writeObject(new ArrayList<>(accounts.values()));
            }
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(transactionsFile))) {
                out.writeObject(new ArrayList<>(transactions));
            }
        } catch (IOException e) {
            System.err.println("Could not save bank data: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        try {
            Files.createDirectories(dataDir);
            if (Files.exists(accountsFile)) {
                try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(accountsFile))) {
                    Object obj = in.readObject();
                    if (obj instanceof List<?>) {
                        for (Object item : (List<?>) obj) {
                            if (item instanceof BankAccount) {
                                BankAccount account = (BankAccount) item;
                                accounts.put(account.getAccountNumber(), account);
                                nextAccountNumber = Math.max(nextAccountNumber, account.getAccountNumber() + 1);
                            }
                        }
                    }
                }
            }
            if (Files.exists(transactionsFile)) {
                try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(transactionsFile))) {
                    Object obj = in.readObject();
                    if (obj instanceof List<?>) {
                        for (Object item : (List<?>) obj) {
                            if (item instanceof Transaction) transactions.add((Transaction) item);
                        }
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Could not load previous data. Starting with available data: " + e.getMessage());
        }
    }
}
