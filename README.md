# Maha Bank: Bank Account System

A web-based bank account management system built with **Java** (backend) and **HTML, CSS and JavaScript** (frontend). It uses no external libraries, only the JDK's built-in `com.sun.net.httpserver.HttpServer`.

## Features
- Create Savings and Current accounts with unique account numbers (starting at 1001)
- Deposit, withdraw and transfer money
- Monthly interest for Savings accounts (4% annual, credited manually)
- Transaction history for every operation
- Custom exceptions for invalid amounts, insufficient balance and missing accounts
- Data saved to local files, so it survives server restarts

## Java concepts demonstrated
Encapsulation, inheritance, abstraction, polymorphism, custom checked exceptions, collections, streams and Comparator, file serialization, synchronization.

## Project structure
```
MahaBank/
├── src/mahabank/   (Java source files)
├── web/            (index.html, style.css, app.js)
├── data/           (created at runtime)
└── out/            (compiled classes)
```

## How to run
Requires JDK 11 or higher.

```
cd MahaBank
javac -d out src/mahabank/*.java
java -cp out mahabank.BankServer
```

Then open **http://localhost:8080** in your browser. Press `Ctrl+C` to stop the server.

## API routes
| Method | Route | Purpose |
|--------|-------|---------|
| GET | `/` | Dashboard |
| GET | `/api/accounts` | All accounts and monthly interest estimates |
| GET | `/api/transactions` | Transaction history |
| POST | `/` | Create account, deposit, withdraw, transfer, credit interest |

## Author
MD Junaid Mohiuddin, AI & Data Science, Chaitanya Bharathi Institute of Technology

*This is an educational simulation, not a real banking system. It has no authentication or encryption.*
