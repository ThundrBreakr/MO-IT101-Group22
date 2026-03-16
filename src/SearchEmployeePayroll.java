// IMPORTS

import java.io.BufferedReader;  // Efficiently reads text from files
import java.io.File;            // Represents a file or directory path
import java.io.FileReader;      // Opens and reads text from a file
import java.io.IOException;     // Handles errors when reading files
import java.time.Duration;      // Calculates time difference between two times
import java.time.LocalDate;     // Stores a date (year, month, day)
import java.time.LocalTime;     // Stores a time (hour and minute)
import java.time.YearMonth;     // Stores a specific month and year
import java.time.format.DateTimeFormatter;          // Parses and formats dates and times
import java.time.format.DateTimeParseException;     // Handles invalid date or time formats
import java.util.ArrayList;     // Stores dynamic lists of data
import java.util.HashMap;       // Stores data using key value pairs
import java.util.List;          // Interface used for list collections
import java.util.Locale;        // Defines regional settings for parsing
import java.util.Map;           // Interface for key value collections
import java.util.Scanner;       // Reads user input from the console
import java.util.regex.Pattern; // Matches patterns (CSV splitting)

public class SearchEmployeePayroll {

    // FILE PATHS - used by the system to locate the CSV data files
    
    private static final String EMPLOYEE_CSV_PATH = "Employee Details.csv";
    private static final String ATTENDANCE_CSV_PATH = "attendance_record.csv";

    // pattern used to split CSV rows while ignoring commas inside quotation marks
    private static final Pattern CSV_SPLIT_PATTERN =
            Pattern.compile(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
    
    // date and time formats used in CSV files
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm", Locale.ENGLISH);

    // CSV column indexes
    private static final int COL_EMPLOYEE_NUMBER = 0;
    private static final int COL_LAST_NAME = 1;
    private static final int COL_FIRST_NAME = 2;
    private static final int COL_BIRTHDAY = 3;
    private static final int COL_GROSS_SEMI_MONTHLY = 17;

    private static final int MIN_EMPLOYEE_COLS = 18;
    private static final int ATTENDANCE_COL_EMPLOYEE_NUMBER = 0;
    private static final int ATTENDANCE_COL_DATE = 3;
    private static final int ATTENDANCE_COL_LOG_IN = 4;
    private static final int ATTENDANCE_COL_LOG_OUT = 5;
    private static final int MIN_ATTENDANCE_COLS = 6;

    // MAIN METHOD

    public static void main(String[] args) {
        
        // LOGIN SYSTEM - verifies user before accessing the program

        // display login header
        System.out.println("========================================");
        System.out.println("             MOTORPH LOGIN              ");
        System.out.println("========================================");

        // scanner used to read input from the user
        Scanner scanner = new Scanner(System.in);
        
        // list of valid usernames and passwords
        Map<String, String> validUsers = new HashMap<>();
        validUsers.put("employee", "12345");
        validUsers.put("payroll_staff", "12345");

        // variables to store login input
        String username = "";
        String password;

        // track login attempts
        int attempts = 0;
        int maxAttempts = 3;

        // LOGIN LOOP - allows user to try logging in up to 3 times

        while (attempts < maxAttempts) {
            System.out.print("Username: ");
            username = scanner.nextLine().trim();

            System.out.print("Password: ");
            password = scanner.nextLine().trim();

            // check if username and password match stored accounts
            if (validUsers.containsKey(username) && validUsers.get(username).equals(password)) {
                break; // login successful, exit loop
            } else {
                attempts++;
                System.out.println("Incorrect username or password.");
                if (attempts < maxAttempts) {
                    System.out.println("Please try again.\n"); // allow another attempt
                }
            }
        }

        // stop program if login attempts exceed the limit
        if (attempts == maxAttempts) {
            System.out.println("Maximum login attempts reached. Program terminated.");
            return;
        }

        // LOAD CSV FILES - prepares employee and attendance data

        File employeeFile = new File(EMPLOYEE_CSV_PATH);
        File attendanceFile = new File(ATTENDANCE_CSV_PATH);

        // check if the required files exist and can be read
        if (!isReadableFile(employeeFile) || !isReadableFile(attendanceFile)) {
            System.out.println("Error: Required CSV file/s not found or unreadable.");
            return;
        }

        // load employee information from the CSV file
        Map<String, Employee> employees = loadEmployees(employeeFile);

        // load attendance records and calculate hours per cutoff
        Map<String, Map<String, Double>> cutoffHoursByEmployee = loadAttendanceCutoffHours(attendanceFile);

        // stop program if no employee records are found
        if (employees.isEmpty()) {
            System.out.println("No employee records found.");
            return;
        }

        // MENU ACCESS - different users see different menus

        // employee can only view their own data
        if (username.equals("employee")) {
            runEmployeeMenu(scanner, employees);
            
        // payroll staff can access full payroll functions
        } else if (username.equals("payroll_staff")) {
            runMenu(employees, cutoffHoursByEmployee);
        }
    }
    
    // EMPLOYEE MENU - allows regular employees to view their payroll information

    private static void runEmployeeMenu(Scanner scanner, Map<String, Employee> employees) {
        while (true) { // menu loop keeps running until the user chooses to exit
            
            // display employee menu options
            System.out.println("\n============ EMPLOYEE MENU =============");
            System.out.println("Select an option:");
            System.out.println("  1. Enter Employee Number");
            System.out.println("  2. Exit Program");
            System.out.print("Enter choice: ");

            // read user choice
            String choice = scanner.nextLine().trim();

            // handle menu selection
            switch (choice) {

                // option 1 allows employee to search using their employee number
                case "1":
                    System.out.print("Enter Employee Number: ");
                    String empNum = scanner.nextLine().trim();

                    // retrieve employee record from the stored employee list
                    Employee emp = employees.get(empNum);

                    if (emp != null) {
                        // display payroll information for the selected employee
                        printEmployeePayroll(emp, new HashMap<>()); // show payroll info
                    } else {
                        System.out.println("Employee number does not exist.");
                    }
                    break;

                // option 2 exits the employee menu and ends the program
                case "2":
                    System.out.println("Exiting program.");
                    return; // exit employee menu

                // hanlde invalid menu inputs
                default:
                    System.out.println("Invalid choice. Please select 1 or 2.");
            }
        }
    }

    // PAYROLL MENU - allows payroll staff to process payroll for one or all employees

    private static void runMenu(Map<String, Employee> employees, Map<String, Map<String, Double>> cutoffHoursByEmployee) {
        Scanner scanner = new Scanner(System.in);

        // menu loop runs until payroll staff chooses to exit
        while (true) {

            // display payroll menu options
            System.out.println("\n=========== PROCESS PAYROLL ============");
            System.out.println("Select an option:");
            System.out.println("  1. One Employee");
            System.out.println("  2. All Employees");
            System.out.println("  3. Exit Program");
            System.out.print("Enter choice: ");

            // read menu selection from user
            String choice = scanner.nextLine().trim();

            // handle selected menu option
            switch (choice) {
                case "1": // process payroll for one employee
                    processOneEmployee(scanner, employees, cutoffHoursByEmployee);
                    break;
                case "2": // process payroll for all employees
                    processAllEmployees(employees, cutoffHoursByEmployee);
                    break;
                case "3": // exit payroll menu
                    System.out.println("Exiting program.");
                    return;
                default: // handle invalid input
                    System.out.println("Invalid choice. Please select 1, 2, or 3.");
            }
        }
    }

    // PROCESS SINGLE EMPLOYEE - prompts for an employee number and prints their payroll

    private static void processOneEmployee(Scanner scanner, Map<String, Employee> employees,
                                           Map<String, Map<String, Double>> cutoffHoursByEmployee) {
        // ask user for employee number
        System.out.print("Enter employee number: ");
        String employeeNumber = scanner.nextLine().trim();

        // get employee record
        Employee employee = employees.get(employeeNumber);

        // handle case where employee does not exist
        if (employee == null) {
            System.out.println("Employee number does not exist.");
            return;
        }

        // print payroll for this employee using stored attendance hours
        printEmployeePayroll(employee, cutoffHoursByEmployee.getOrDefault(employeeNumber, new HashMap<>()));
    }

    // PROCESS ALL EMPLOYEES - loops through all employees and prints payroll for each

      private static void processAllEmployees(Map<String, Employee> employees,
                                            Map<String, Map<String, Double>> cutoffHoursByEmployee) {
        // iterate over all employee records
        for (Employee employee : employees.values()) {

            // get attendance hours for employee
            Map<String, Double> employeeHours = cutoffHoursByEmployee.getOrDefault(employee.employeeNumber, new HashMap<>());
            // print payroll for current employee
            printEmployeePayroll(employee, employeeHours);
            // add spacing between employee records
            System.out.println();
        }
    }

    // PRINT PAYROLL INFO - displays an employee's details and payroll per cutoff

    private static void printEmployeePayroll(Employee employee, Map<String, Double> cutoffHours) {

        // list of cutoff periods from June to December 2024
        List<String> cutoffKeys = buildCutoffKeysFromJuneToDecember2024();

        // display employee information
        System.out.println("\n========= EMPLOYEE INFORMATION =========");
        System.out.println("Employee #: " + employee.employeeNumber);
        System.out.println("Employee Name: " + employee.fullName);
        System.out.println("Birthday: " + employee.birthday);
        System.out.println("========================================");

        // loop through each cutoff period to show payroll
        for (String cutoffKey : cutoffKeys) {
            
            // display the cutoff label
            String label = cutoffLabel(cutoffKey);
            
            // get total hours worked during this cutoff, default to 0 if not found
            double totalHoursWorked = cutoffHours.getOrDefault(cutoffKey, 0.0);

            // gross semi-monthly salary
            double grossSalary = employee.grossSemiMonthly;

            System.out.println("\n---- Cutoff Date: " + label + " ----");
            System.out.printf("Total Hours Worked: %.2f%n", totalHoursWorked);
            System.out.printf("Gross Salary: %.2f%n", grossSalary);

            // calculate deductions only for the 2nd cutoff of the month
            if (isSecondCutoff(cutoffKey)) {
                double sss = computeSSS(grossSalary);
                double philHealth = computePhilHealth(grossSalary);
                double pagIbig = computePagIBIG(grossSalary);
                double tax = computeIncomeTax(grossSalary);
                double totalDeductions = sss + philHealth + pagIbig + tax;
                double netSalary = grossSalary - totalDeductions;

                // display deductions and net salary
                System.out.println("Deductions");
                System.out.printf("   SSS: %.2f%n", sss);
                System.out.printf("   PhilHealth: %.2f%n", philHealth);
                System.out.printf("   Pag-IBIG: %.2f%n", pagIbig);
                System.out.printf("   Tax: %.2f%n", tax);
                System.out.println("   -----------------------");
                System.out.printf("   Total Deductions: %.2f%n", totalDeductions);
                System.out.printf("Net Salary: %.2f%n", netSalary);
            } else {
                // for first cutoff, net salary equals gross salary
                System.out.printf("Net Salary: %.2f%n", grossSalary);
            }
        }
    }

    // BUILD CUTOFF KEYS - generates cutoff identifiers from June to December 2024

    private static List<String> buildCutoffKeysFromJuneToDecember2024() {
        List<String> keys = new ArrayList<>();
        for (int month = 6; month <= 12; month++) {
            keys.add(String.format("2024-%02d-1", month)); // first cutoff of the month
            keys.add(String.format("2024-%02d-2", month)); // second cutoff of the month
        }
        return keys;
    }

    // GET CUTOFF LABEL - converts a cutoff key to a human-readable date range
    
    private static String cutoffLabel(String cutoffKey) {
        String[] parts = cutoffKey.split("-");
        int month = Integer.parseInt(parts[1]);
        int part = Integer.parseInt(parts[2]);

        // format month name
        String monthName = YearMonth.of(2024, month).getMonth().name().substring(0, 1)
                + YearMonth.of(2024, month).getMonth().name().substring(1).toLowerCase();

        if (part == 1) {
            return monthName + " 1 to " + monthName + " 15";
        }

        // second cutoff runs from 16th to end of month
        int endOfMonth = YearMonth.of(2024, month).lengthOfMonth();
        return monthName + " 16 to " + monthName + " " + endOfMonth;
    }

    // CHECK IF SECOND CUTOFF - returns true if cutoff key ends with "-2"

    private static boolean isSecondCutoff(String cutoffKey) {
        return cutoffKey.endsWith("-2");
    }

    // LOAD EMPLOYEES - reads employee records from CSV and returns by employee number

    private static Map<String, Employee> loadEmployees(File file) {
        Map<String, Employee> employees = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            reader.readLine(); // skip header row

            String line;
            while ((line = reader.readLine()) != null) {

                String[] parts = splitCsvLine(line);

                // skip rows with missing columns
                if (parts.length < MIN_EMPLOYEE_COLS) {
                    continue;
                }

                // extract employee info from CSV columns
                String employeeNumber = clean(parts[COL_EMPLOYEE_NUMBER]);
                String lastName = clean(parts[COL_LAST_NAME]);
                String firstName = clean(parts[COL_FIRST_NAME]);
                String birthday = clean(parts[COL_BIRTHDAY]);
                Double grossSemiMonthly = tryParseMoney(parts[COL_GROSS_SEMI_MONTHLY]);

                // skip incomplete or invalid records
                if (employeeNumber.isEmpty() || grossSemiMonthly == null) {
                    continue;
                }

                // combine first and last name
                String fullName = firstName + " " + lastName;

                // store employee object in map
                employees.put(employeeNumber, new Employee(employeeNumber, fullName, birthday, grossSemiMonthly));
            }
        } catch (IOException e) {
            System.out.println("Error reading employee file: " + e.getMessage());
        }

        return employees;
    }

    // LOAD ATTENDANCE HOURS - prepares a map of hours worked per employee per cutoff

    private static Map<String, Map<String, Double>> loadAttendanceCutoffHours(File file) {
        Map<String, Map<String, Double>> hoursByEmployee = new HashMap<>();

        // LOAD ATTENDANCE AND CALCULATE HOURS - reads attendance CSV and sums hours per cutoff

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            reader.readLine(); // skip header row
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = splitCsvLine(line);

                // skip rows with missing columns
                if (parts.length < MIN_ATTENDANCE_COLS) {
                    continue;
                }

                // extract attendance info from CSV
                String employeeNumber = clean(parts[ATTENDANCE_COL_EMPLOYEE_NUMBER]);
                LocalDate date = parseDate(clean(parts[ATTENDANCE_COL_DATE]));
                Double workedHours = computeWorkedHours(clean(parts[ATTENDANCE_COL_LOG_IN]), clean(parts[ATTENDANCE_COL_LOG_OUT]));

                // skip invalid or incomplete records
                if (employeeNumber.isEmpty() || date == null || workedHours == null) {
                    continue;
                }

                // only include records from June to December 2024
                if (date.getYear() != 2024 || date.getMonthValue() < 6 || date.getMonthValue() > 12) {
                    continue;
                }

                // determine cutoff key for attendance date
                String cutoffKey = cutoffKey(date);

                // sum worked hours for employee and cutoff
                hoursByEmployee
                        .computeIfAbsent(employeeNumber, k -> new HashMap<>())
                        .merge(cutoffKey, workedHours, Double::sum);
            }

        } catch (IOException e) {
          System.out.println("Error reading attendance file: " + e.getMessage());
        }

        // intentionally not closing the Scanner to avoid closing System.in
        return hoursByEmployee;
    }

    // DETERMINE CUTOFF KEY - assigns 1st or 2nd cutoff based on day of month

    private static String cutoffKey(LocalDate date) {
        int cutoffPart = date.getDayOfMonth() <= 15 ? 1 : 2;
        return String.format("%04d-%02d-%d", date.getYear(), date.getMonthValue(), cutoffPart);
    }

    // PAYROLL CALCULATIONS

    // computeWorkedHours - calculates total hours worked from log-in and log-out times
    private static Double computeWorkedHours(String logInRaw, String logOutRaw) {
        try {
            LocalTime logIn = LocalTime.parse(logInRaw, TIME_FORMAT);
            LocalTime logOut = LocalTime.parse(logOutRaw, TIME_FORMAT);

            // ignore invalid times where log-out is before log-in
            if (logOut.isBefore(logIn)) {
                return null; 
            }

            Duration duration = Duration.between(logIn, logOut);
            return duration.toMinutes() / 60.0; // convert minutes to hours
        } catch (DateTimeParseException e) {
            return null; // return null if time format is invalid
        }
    }

    // parseDate - parses a date string into a LocalDate, returns null if invalid
    private static LocalDate parseDate(String rawDate) {
        try {
            return LocalDate.parse(rawDate, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // splitCsvLine - splits a CSV line into columns, handling quoted commas
    private static String[] splitCsvLine(String line) {
        return CSV_SPLIT_PATTERN.split(line, -1);
    }

    // clean - removes quotes and trims whitespace from a string
    private static String clean(String value) {
        return value == null ? "" : value.replace("\"", "").trim();
    }

    // tryParseMoney - converts a string to Double, removing commas and quotes
    private static Double tryParseMoney(String rawCsvValue) {
        if (rawCsvValue == null) {
            return null;
        }

        // remove quotation marks and comma separators before parsing
        String normalized = rawCsvValue.replace("\"", "").replace(",", "").trim();
        if (normalized.isEmpty()) {
            return null;
        }

        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            return null; // return null if parsing fails
        }
    }

    // isReadableFile - checks if a file exists, is a file, and can be read
    private static boolean isReadableFile(File file) {
        return file.exists() && file.isFile() && file.canRead();
    }

    // DEDUCTIONS - calculate payroll deductions

    public static double computeSSS(double salary) {
        // computeSSS - social security deduction (simple placeholder rate)
        return salary * 0.045;
    }

    public static double computePhilHealth(double salary) {
        // computePhilHealth - health insurance deduction (simple placeholder rate)
        return salary * 0.03;
    }

    public static double computePagIBIG(double salary) {
        // computePagIBIG - housing fund deduction (simple placeholder rate)
        return salary * 0.02;
    }

    // computeIncomeTax - calculates income tax
    public static double computeIncomeTax(double salary) {
        if (salary <= 20000) {
            return 0;
        } else if (salary <= 40000) {
            return salary * 0.10;
        }
        return salary * 0.20;
    }

    // EMPLOYEE CLASS - stores employee details used throughout the system

    private static class Employee {
        private final String employeeNumber;    // employee number
        private final String fullName;          // full name of employee
        private final String birthday;          // birth date
        private final double grossSemiMonthly;  // gross semi-monthly salary

        private Employee(String employeeNumber, String fullName, String birthday, double grossSemiMonthly) {
            this.employeeNumber = employeeNumber;
            this.fullName = fullName;
            this.birthday = birthday;
            this.grossSemiMonthly = grossSemiMonthly;
        }
    }
}