// IMPORTS

import java.io.BufferedReader;  // Used to efficiently read large CSV files line-by-line for payroll data processing
import java.io.File;            // Represents external CSV files containing employee and attendance records
import java.io.FileReader;      // Enables reading of CSV file contents into the system
import java.io.IOException;     // Handles file access errors to prevent system crashes during data loading
import java.time.Duration;      // Calculates total working hours from log-in and log-out times
import java.time.LocalDate;     // Represents attendance dates for payroll cutoff classification
import java.time.LocalTime;     // Represents employee log-in and log-out times
import java.time.YearMonth;     // Determines month and year boundaries
import java.time.format.DateTimeFormatter;       // Ensures consistent parsing of date/time formats from CSV input
import java.time.format.DateTimeParseException;  // Handles invalid date/time inputs to maintain data integrity
import java.util.ArrayList;     // Stores dynamic lists such as generated payroll cutoff periods
import java.util.HashMap;       // Stores employee and payroll data for fast lookup by employee number
import java.util.List;          // Provides abstraction for handling collections of payroll-related data
import java.util.Locale;        // Ensures consistent parsing of time formats regardless of system settings
import java.util.Map;           // Defines structured key-value storage for employees and computed payroll data
import java.util.Scanner;       // Captures user input for login and menu navigation
import java.util.regex.Pattern; // Splits CSV rows correctly, handling quoted values to avoid data corruption

public class SearchEmployeePayroll {

    // Defines the data sources required for payroll processing (employee details and attendance records)
    private static String EMPLOYEE_CSV_PATH;
    private static String ATTENDANCE_CSV_PATH;

    // Ensures accurate CSV parsing to prevent data misalignment caused by commas inside quoted values
    private static final Pattern CSV_SPLIT_PATTERN =
            Pattern.compile(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
    
    // Standardizes date and time parsing to maintain consistency with input data formats
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm", Locale.ENGLISH);

    // Maps CSV columns to employee attributes to ensure correct payroll data extraction
    private static final int COL_EMPLOYEE_NUMBER = 0;
    private static final int COL_LAST_NAME = 1;
    private static final int COL_FIRST_NAME = 2;
    private static final int COL_BIRTHDAY = 3;
    private static final int COL_GROSS_SEMI_MONTHLY = 17;

    // Validates employee records to prevent incomplete data from affecting payroll computation
    private static final int MIN_EMPLOYEE_COLS = 18; // Employee CSV must have columns 0–17 (number, name, birthday, gross semi-monthly)
    
    // Maps attendance data fields required for calculating worked hours
    private static final int ATTENDANCE_COL_EMPLOYEE_NUMBER = 0;
    private static final int ATTENDANCE_COL_DATE = 3;
    private static final int ATTENDANCE_COL_LOG_IN = 4;
    private static final int ATTENDANCE_COL_LOG_OUT = 5;
    
    // Ensures attendance records are complete before processing work hours
    private static final int MIN_ATTENDANCE_COLS = 6; // Attendance CSV must have columns 0–5 (employee number, date, log-in/out)

    /**
     * MAIN METHOD
     * Entry point of the payroll system
     * 
     * Handles user authentication, loads required data sources,
     * and routes users to the appropriate system functions based on role.
     * 
     * @param args command-line arguments (not used in this application)
     */
    public static void main(String[] args) {

        EMPLOYEE_CSV_PATH = args.length > 0 ? args[0] : "Employee Details.csv";
        ATTENDANCE_CSV_PATH = args.length > 1 ? args[1] : "attendance_record.csv";
        
        // LOGIN SYSTEM - enforces authentication and access control for payroll operations
        System.out.println("========================================");
        System.out.println("             MOTORPH LOGIN              ");
        System.out.println("========================================");

        // Captures user input for authentication and menu navigation
        Scanner scanner = new Scanner(System.in);
        
        // Establishes user roles to control access to employee vs payroll functionalities
        Map<String, String> validUsers = new HashMap<>();
        validUsers.put("employee", "12345");
        validUsers.put("payroll_staff", "12345");

        // Holds username and password entered during login for validation
        String username = "";
        String password;

        // Limits login attempts to prevent unauthorized access
        int attempts = 0;
        int maxAttempts = 3;

        // LOGIN LOOP - limits authentication attempts to 3 to prevent unauthorized access
        while (attempts < maxAttempts) {
            System.out.print("Username: ");
            username = scanner.nextLine().trim();

            System.out.print("Password: ");
            password = scanner.nextLine().trim();

            // Validates credentials to grant role-based system access
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

        // Terminates the program after exceeding the maximum of 3 allowed login attempts
        if (attempts == maxAttempts) {
            System.out.println("Maximum login attempts reached. Program terminated.");
            return;
        }

        // LOAD CSV FILES - initializes employee and attendance data from external sources for payroll processing
        File employeeFile = new File(EMPLOYEE_CSV_PATH);
        File attendanceFile = new File(ATTENDANCE_CSV_PATH);

        // Prevents execution if required payroll data files are missing or inaccessible
        if (!isReadableFile(employeeFile) || !isReadableFile(attendanceFile)) {
            System.out.println("Error: Required CSV file/s not found or unreadable.");
            return;
        }

        // Loads employee records used as the foundation for payroll computation
        Map<String, Employee> employees = loadEmployees(employeeFile);

        // Aggregates attendance data into cutoff-based working hours
        Map<String, Map<String, Double>> cutoffHoursByEmployee = loadAttendanceCutoffHours(attendanceFile);

        // Prevents execution if no valid employee data is available
        if (employees.isEmpty()) {
            System.out.println("No employee records found.");
            return;
        }

        // MENU ACCESS - enforces role-based access to restrict features based on user permissions
        
        // Employees are limited to viewing their own data to protect sensitive payroll information
        if (username.equals("employee")) {
            runEmployeeMenu(scanner, employees, cutoffHoursByEmployee);
            
        // Payroll staff are granted full access to perform payroll processing tasks
        } else if (username.equals("payroll_staff")) {
            runMenu(employees, cutoffHoursByEmployee);
        }
    }
    
    /**
     * EMPLOYEE MENU
     * Displays and manages the employee self-service menu.
     * 
     * Allows employees to access and view their payroll details
     * using their employee number.
     * 
     * @param scanner   handles user input
     * @param employees map of employee records indexed by employee number
    */
    private static void runEmployeeMenu(Scanner scanner, Map<String, Employee> employees, Map<String, Map<String, Double>> cutoffHoursByEmployee) {
        while (true) { // Keeps menu active until user chooses to exit
            
            // Presents available actions for employees to access and view their payroll information
            System.out.println("\n============ EMPLOYEE MENU =============");
            System.out.println("Select an option:");
            System.out.println("  1. Enter Employee Number");
            System.out.println("  2. Exit Program");
            System.out.print("Enter choice: ");

            // Captures user input to determine the selected menu action
            String choice = scanner.nextLine().trim();

            // Routes execution based on the selected menu option
            switch (choice) {

                case "1":
                    // Allows employee to access their personal information
                    System.out.print("Enter Employee Number: ");
                    String empNum = scanner.nextLine().trim();

                    // Fetches employee data using the provided ID to validate existence
                    Employee emp = employees.get(empNum);

                    if (emp != null) {
                        // Displays the employee's payroll details based on retrieved records
                        printEmployeePayroll(emp, cutoffHoursByEmployee.getOrDefault(empNum, new HashMap<>())); // Show payroll information
                    } else {
                        System.out.println("Employee number does not exist.");
                    }
                    break;

                case "2":
                    // Provides a controlled exit from the employee interface
                    System.out.println("Exiting program.");
                    return; // Exits the menu loop, stopping further user interaction

                // Prevents invalid input from disrupting menu flow
                default:
                    System.out.println("Invalid choice. Please select 1 or 2.");
            }
        }
    }

    /**
     * PAYROLL MENU
     * Displays and manages the payroll staff menu.
     * 
     * Allows payroll staff to process payroll for individual employees
     * or generate payroll for all employees based on attendance data.
     * 
     * @param employees               map of employee records
     * @param cutoffHoursByEmployee   computed work hours per cutoff period
     */
    private static void runMenu(Map<String, Employee> employees, Map<String, Map<String, Double>> cutoffHoursByEmployee) {
        Scanner scanner = new Scanner(System.in);

        while (true) { // Keeps payroll menu active until user exits

            // Presents payroll processing options to allow staff to select how payroll will be handled
            System.out.println("\n=========== PROCESS PAYROLL ============");
            System.out.println("Select an option:");
            System.out.println("  1. One Employee");
            System.out.println("  2. All Employees");
            System.out.println("  3. Exit Program");
            System.out.print("Enter choice: ");

            // Captures user input to determine the selected payroll operation
            String choice = scanner.nextLine().trim();

            // Routes execution based on the selected payroll processing option
            switch (choice) {
                case "1": 
                    // Processes payroll for a selected employee
                    processOneEmployee(scanner, employees, cutoffHoursByEmployee);
                    break;
                    
                case "2": 
                    // Generates payroll for all employees based on computed attendance data
                    processAllEmployees(employees, cutoffHoursByEmployee);
                    break;
                    
                case "3": 
                    // Provides controlled exit from payroll interface
                    System.out.println("Exiting program.");
                    return;
                    
                default: 
                    // Prevents invalid input from disrupting menu flow
                    System.out.println("Invalid choice. Please select 1, 2, or 3.");
            }
        }
    }

    /**
     * PROCESS SINGLE EMPLOYEE
     * Processes payroll for a single employee.
     * 
     * Retrieves the employee record and generates payroll output
     * based on computed attendance hours per cutoff period.
     * 
     * @param scanner                  handles user input
     * @param employees                map of employee records indexed by employee number
     * @param cutoffHoursByEmployee    computed work hours per cutoff period
    */
    private static void processOneEmployee(Scanner scanner, Map<String, Employee> employees,
                                           Map<String, Map<String, Double>> cutoffHoursByEmployee) {
        
        // Captures employee number to locate payroll data
        System.out.print("Enter employee number: ");
        String employeeNumber = scanner.nextLine().trim();

        // Retrieves the employee record using the provided employee number for payroll processing
        Employee employee = employees.get(employeeNumber);

        // Prevents processing for non-existent employee records
        if (employee == null) {
            System.out.println("Employee number does not exist.");
            return;
        }

        // Generates payroll using available attendance data
        printEmployeePayroll(employee, cutoffHoursByEmployee.getOrDefault(employeeNumber, new HashMap<>()));
    }

    /**
     * PROCESS ALL EMPLOYEES
     * Processes payroll for all employees.
     * 
     * Iterates through all employee records and generates payroll output
     * using their corresponding attendance data per cutoff period.
     * 
     * @param employees               map of employee records
     * @param cutoffHoursByEmployee   computed work hours per cutoff period
     */
      private static void processAllEmployees(Map<String, Employee> employees,
                                            Map<String, Map<String, Double>> cutoffHoursByEmployee) {
          
        // Loops through all employees to process payroll records
        for (Employee employee : employees.values()) {

            // Retrieves attendance data required for payroll calculation
            Map<String, Double> employeeHours = cutoffHoursByEmployee.getOrDefault(employee.employeeNumber, new HashMap<>());
            
            // Generates payroll output based on employee data and computed attendance hours
            printEmployeePayroll(employee, employeeHours);
            
            // Adds spacing to improve readability between employee payroll outputs
            System.out.println();
        }
    }

    /**
     * PRINT PAYROLL INFO
     * Generates and displays payroll details for a given employee.
     * 
     * Uses attendance-based cutoff periods to compute gross pay,
     * apply deductions, and determine net salary.
     * 
     * @param employee     employee record containing personal and salary data
     * @param cutoffHours  map of worked hours per cutoff period
     */
    private static void printEmployeePayroll(Employee employee, Map<String, Double> cutoffHours) {

        // Defines the payroll cutoff periods from June to December 2024
        List<String> cutoffKeys = buildCutoffKeysFromJuneToDecember2024();

        // Displays employee information before showing payroll breakdown
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