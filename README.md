# moni-finance

Moni is a student money manager built with Java Swing. It answers one question:
how much can I spend today and still have allowance left for the rest of the week?

## Features

- Wallet and savings balances
- Add expenses, add money (allowance or other funds) and move money to savings
- Daily and weekly spending limits, plus a weekly budget for each category
- "Left to spend today": what's left of the weekly limit, shared across the days left in the week
- Searchable, filterable transaction history
- Weekly reports: money in and out, spending per day and per category
- Notifications when you get close to or go over a limit
- Light and dark mode in an olive green and beige cream palette

## Running it

1. Start MySQL or MariaDB (for example with XAMPP) and create a database called `moni_db`.
   You can import `moni_db.sql`, or leave it empty: Moni creates any missing tables itself.
2. Add the MySQL Connector/J `.jar` to the project's libraries.
3. Run `app.MainApp`.

The connection uses the XAMPP defaults (`root`, no password). To use something else, set the
`MONI_DB_URL`, `MONI_DB_USER` and `MONI_DB_PASS` environment variables.

## Code overview

| File | What it does |
| --- | --- |
| `MainApp.java` | Main window: sidebar, top bar and every page |
| `AuthDialog.java`, `AuthManager.java` | Sign in and create account |
| `OnboardingDialog.java` | Four-step setup for allowance, limits, categories and dashboard |
| `FormDialog.java` | Small form used by Add expense, Add money and Move to savings |
| `TransactionsPanel.java` | Transaction table with search, filters, sorting and pages |
| `DailyChart.java` | Bar chart of this week's spending |
| `Theme.java` | Colours (light and dark), fonts and the custom-painted components |
| `Icons.java` | Icons drawn with Java2D |
| `MoniDatabase.java`, `DatabaseConnection.java` | Database access |
| `User.java`, `UserSettings.java`, `TransactionRecord.java` | Data classes |

## Technologies

- Java and Java Swing
- MySQL / MariaDB with JDBC
