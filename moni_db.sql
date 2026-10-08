-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Sep 25, 2026 at 02:54 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `moni_db`
--

-- --------------------------------------------------------

--
-- Table structure for table `accounts`
--

CREATE TABLE `accounts` (
  `account_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `wallet_balance` decimal(10,2) NOT NULL DEFAULT 0.00,
  `savings_balance` decimal(10,2) NOT NULL DEFAULT 0.00
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `accounts`
--

INSERT INTO `accounts` (`account_id`, `user_id`, `wallet_balance`, `savings_balance`) VALUES
(1, 1, 850.00, 0.00),
(2, 2, 0.00, 0.00),
(3, 3, 500.00, 0.00);

-- --------------------------------------------------------

--
-- Table structure for table `budgets`
--

CREATE TABLE `budgets` (
  `budget_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `category` varchar(50) NOT NULL,
  `budget_limit` decimal(10,2) NOT NULL DEFAULT 0.00,
  `amount_spent` decimal(10,2) NOT NULL DEFAULT 0.00
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `budgets`
--

INSERT INTO `budgets` (`budget_id`, `user_id`, `category`, `budget_limit`, `amount_spent`) VALUES
(1, 1, 'Food', 200.00, 0.00),
(2, 1, 'Transportation', 150.00, 0.00),
(3, 1, 'School', 200.00, 0.00),
(4, 1, 'Entertainment', 300.00, 0.00),
(5, 1, 'Other', 500.00, 0.00),
(6, 2, 'Food', 800.00, 0.00),
(7, 2, 'Transportation', 400.00, 0.00),
(8, 2, 'School', 400.00, 0.00),
(9, 2, 'Entertainment', 200.00, 0.00),
(10, 2, 'Other', 200.00, 0.00),
(11, 3, 'Food', 1120.00, 0.00),
(12, 3, 'Transportation', 560.00, 0.00),
(13, 3, 'School', 560.00, 0.00),
(14, 3, 'Entertainment', 280.00, 0.00),
(15, 3, 'Other', 280.00, 0.00);

-- --------------------------------------------------------

--
-- Table structure for table `transactions`
--

CREATE TABLE `transactions` (
  `transaction_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `transaction_date` date NOT NULL,
  `flow_type` varchar(20) NOT NULL,
  `category_source` varchar(50) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `amount` decimal(10,2) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `transactions`
--

INSERT INTO `transactions` (`transaction_id`, `user_id`, `transaction_date`, `flow_type`, `category_source`, `description`, `amount`, `created_at`) VALUES
(1, 1, '2026-09-24', 'Money In', 'Allowance', 'Added Funds to Wallet', 600.00, '2026-09-24 06:31:47'),
(2, 1, '2026-09-25', 'Money Out', 'Food', 'CHICKCHICKIN', -150.00, '2026-09-24 16:35:43'),
(3, 1, '2026-09-25', 'Money In', 'Allowance', 'Added Allowance', 400.00, '2026-09-24 16:36:25'),
(4, 3, '2026-09-24', 'Money In', 'Allowance', 'Added Allowance', 500.00, '2026-09-25 12:07:39');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `user_id` int(11) NOT NULL,
  `student_number` varchar(30) NOT NULL,
  `full_name` varchar(100) NOT NULL,
  `email` varchar(150) NOT NULL,
  `password` varchar(255) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`user_id`, `student_number`, `full_name`, `email`, `password`, `created_at`) VALUES
(1, '2026-1001', 'Demo', 'd@mail.com', 'pass123', '2026-09-24 06:27:58'),
(2, '2025105526', 'Erin Gail Dames', 'egdames@mymail.mapua.edu.ph', 'pass123', '2026-09-24 16:42:30'),
(3, '2025105527', 'Erin Gail Dames', 'yerindames@gmail.com', 'pass123', '2026-09-25 12:06:23');

-- --------------------------------------------------------

--
-- Table structure for table `user_settings`
--

CREATE TABLE `user_settings` (
  `settings_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `daily_spending_limit` decimal(10,2) NOT NULL DEFAULT 0.00,
  `weekly_spending_limit` decimal(10,2) NOT NULL DEFAULT 0.00,
  `show_wallet` tinyint(1) NOT NULL DEFAULT 1,
  `show_savings` tinyint(1) NOT NULL DEFAULT 1,
  `show_daily` tinyint(1) NOT NULL DEFAULT 1,
  `show_weekly` tinyint(1) NOT NULL DEFAULT 1,
  `show_transactions` tinyint(1) NOT NULL DEFAULT 1,
  `show_budget` tinyint(1) NOT NULL DEFAULT 1,
  `show_quick_actions` tinyint(1) NOT NULL DEFAULT 1,
  `setup_completed` tinyint(1) NOT NULL DEFAULT 0,
  `allowance_amount` decimal(12,2) NOT NULL DEFAULT 0.00,
  `allowance_frequency` varchar(30) NOT NULL DEFAULT 'Daily',
  `daily_allowance` decimal(12,2) NOT NULL DEFAULT 0.00
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `user_settings`
--

INSERT INTO `user_settings` (`settings_id`, `user_id`, `daily_spending_limit`, `weekly_spending_limit`, `show_wallet`, `show_savings`, `show_daily`, `show_weekly`, `show_transactions`, `show_budget`, `show_quick_actions`, `setup_completed`, `allowance_amount`, `allowance_frequency`, `daily_allowance`) VALUES
(1, 1, 500.00, 3500.00, 1, 1, 1, 1, 1, 1, 1, 1, 0.00, 'Daily', 0.00),
(2, 2, 285.71, 2000.00, 1, 1, 1, 1, 1, 1, 1, 1, 2500.00, 'Weekly', 357.14),
(3, 3, 400.00, 2800.00, 1, 1, 1, 1, 1, 1, 1, 1, 500.00, 'Daily', 500.00);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `accounts`
--
ALTER TABLE `accounts`
  ADD PRIMARY KEY (`account_id`),
  ADD UNIQUE KEY `user_id` (`user_id`);

--
-- Indexes for table `budgets`
--
ALTER TABLE `budgets`
  ADD PRIMARY KEY (`budget_id`),
  ADD UNIQUE KEY `user_id` (`user_id`,`category`);

--
-- Indexes for table `transactions`
--
ALTER TABLE `transactions`
  ADD PRIMARY KEY (`transaction_id`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `student_number` (`student_number`),
  ADD UNIQUE KEY `email` (`email`);

--
-- Indexes for table `user_settings`
--
ALTER TABLE `user_settings`
  ADD PRIMARY KEY (`settings_id`),
  ADD UNIQUE KEY `user_id` (`user_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `accounts`
--
ALTER TABLE `accounts`
  MODIFY `account_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `budgets`
--
ALTER TABLE `budgets`
  MODIFY `budget_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- AUTO_INCREMENT for table `transactions`
--
ALTER TABLE `transactions`
  MODIFY `transaction_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `user_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `user_settings`
--
ALTER TABLE `user_settings`
  MODIFY `settings_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `accounts`
--
ALTER TABLE `accounts`
  ADD CONSTRAINT `accounts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `budgets`
--
ALTER TABLE `budgets`
  ADD CONSTRAINT `budgets_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `transactions`
--
ALTER TABLE `transactions`
  ADD CONSTRAINT `transactions_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `user_settings`
--
ALTER TABLE `user_settings`
  ADD CONSTRAINT `user_settings_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
