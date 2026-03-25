MotorPH Payroll System Phase 1 | Computer Programming 1

---

## Team Contributions

**Seth William Maravilla**
- Responsible for most of the program development and initial system setup. Implemented the core program logic including CSV file processing, attendance handling, payroll computation, and overall program structure.

**Kachina Shen Pascual**
- Handled payroll output formatting and organized the code structure. Implemented the login authentication system, prepared the project worksheet, and wrote the code explanations and documentation for the project.
  

---
# MotorPH Payroll System

Manages employee payroll using CSV data. The system calculates work hours per semi-monthly period, determines gross and net salaries, and applies required deductions. Employees can check their payroll information, while payroll staff can generate reports for individual or all employees.

---
## Features

**Login System**
Employees and payroll staff must log in before accessing the system.

**Default credentials:**
- **Employee:** employee
- **Payroll Staff:** payroll_staff

**Default password for both accounts:** 12345

Users have a maximum of three login attempts.

---
## Employee Functions
Employees can enter their Employee Number to view payroll details.

**Displayed information:**
- Employee Number
- Employee Name
- Birthday

**Payroll details per cutoff:** total hours worked, gross salary, net salary, and deductions (second cutoff only)

---

## Payroll Staff Functions
Payroll staff can generate payroll reports:

- **Process One Employee** – generate payroll for a specific employee
- **Process All Employees** – generate payroll for all employees sequentially
- **Exit Program** – leave the payroll system

---

## Attendance Processing

Data is read from **attendance_record.csv**.

**Each attendance record includes:** Employee Number, Date, Log-in Time, Log-out Time.

**Processing steps:**
1. Read attendance records from CSV
2. Parse date and time values
3. Calculate hours worked as the difference between log-out and log-in
4. Assign hours to the correct semi-monthly cutoff
5. Sum hours per employee per cutoff

Only records from **June–December 2024** are included.

---

## Payroll Computation

**Semi-Monthly Cutoffs:**
- **1st Cutoff:** Day 1–15
- **2nd Cutoff:** Day 16–End of Month

**Gross Salary:** retrieved from Employee Details.csv (Gross Semi-Monthly)

**Deductions (applied only in the 2nd cutoff):**
- SSS
- PhilHealth
- Pag-IBIG
- Income Tax

**Net Salary Formula:**
Net Salary = Gross Salary − (SSS + PhilHealth + Pag-IBIG + Income Tax)

For the 1st cutoff, net salary = gross salary.

---

## CSV File Handling

- **Employee Details.csv** - Contains employee information such as employee number, name, birthday, and gross semi monthly salary.
- **attendance_record.csv** - Contains employee information such as employee number, name, birthday, and gross semi monthly salary.

The program parses these files to retrieve employee records and calculate attendance hours per payroll cutoff period. Invalid or incomplete rows are skipped during processing.

---

## Running the Program

**Compile:**
javac SearchEmployeePayroll.java

**Run:**
java SearchEmployeePayroll

**Login:**
- **Employee:** employee / 12345
- **Payroll Staff:** payroll_staff / 12345

---

## Error Handling and Validation

- **Login:** maximum of 3 attempts per user
- **File validation:** checks if CSV files exist and are readable
- **Data validation:** skips invalid employee numbers, dates, or times (including logout before login)
- Menu input validation ensures users enter valid selections

---

## Code Overview

**Main Class**
**SearchEmployeePayroll** is the main class that runs the program. The **main()** method handles login, loads CSV files, and directs users to the correct menu.

**Login System**
User credentials are stored in a HashMap. The system supports two roles: Employee and Payroll Staff. Users are allowed up to three login attempts.

**Employee Menu**
Employees can enter their employee number to view payroll information. The program displays employee details and payroll summaries for each cutoff period.

**Payroll Staff Menu**
Payroll staff can process payroll for one employee or generate payroll reports for all employees in the system.

**Employee Data Loading**
Employee records are read from **Employee Details.csv** and stored in a HashMap using the employee number as the key for quick lookup.

**Attendance Processing**
Attendance data is read from **attendance_record.csv**. The program parses login and logout times, calculates hours worked, and assigns them to the correct semi monthly cutoff.

**Payroll Computation**
The system retrieves the employee’s gross semi monthly salary and displays payroll information including total hours worked, gross salary, deductions, and net salary.

**Employee Class**
The Employee class stores employee information such as employee number, name, birthday, and gross semi monthly salary.

---

## Project Plan

[View Project Plan](https://docs.google.com/spreadsheets/d/12MRNjTV6ithJ07SayGCYP1cRwL1MnBhYmgYSskS6irg/edit?usp=sharing)
