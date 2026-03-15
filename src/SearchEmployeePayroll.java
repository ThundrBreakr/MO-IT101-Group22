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

    // FILE PATHS
    
    private static final String EMPLOYEE_CSV_PATH = "Employee Details.csv";
    private static final String ATTENDANCE_CSV_PATH = "attendance_record.csv";

    // pattern to split CSV while ignoring commas inside quotes
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
        
        // LOGIN SYSTEM

        // display login header
        System.out.println("========================================");
        System.out.println("             MOTORPH LOGIN              ");
        System.out.println("========================================");

        // scanner for user input
        Scanner scanner = new Scanner(System.in);
        
        // define valid usernames and passwords
        Map<String, String> validUsers = new HashMap<>();
        validUsers.put("employee", "12345");
        validUsers.put("payroll_staff", "12345");

        String username;
        String password;

        while (true) { // keep asking for login until correct credentials
            System.out.print("Username: ");
            username = scanner.nextLine().trim();

            System.out.print("Password: ");
            password = scanner.nextLine().trim();

        // check credentials
        if (validUsers.containsKey(username) && validUsers.get(username).equals(password)) {
            break; // login successful, exit loop
        } else {
            System.out.println("Incorrect username or password.\nPlease try again.\n");
        }
    }
        
        // LOAD CSV FILES

        File employeeFile = new File(EMPLOYEE_CSV_PATH);
        File attendanceFile = new File(ATTENDANCE_CSV_PATH);

        // verify files exist and are readable
        if (!isReadableFile(employeeFile) || !isReadableFile(attendanceFile)) {
            System.out.println("Error: Required CSV file/s not found or unreadable.");
            return;
        }

        // load employee and attendance data
        Map<String, Employee> employees = loadEmployees(employeeFile);
        Map<String, Map<String, Double>> cutoffHoursByEmployee = loadAttendanceCutoffHours(attendanceFile);

        // exit if no employee records are found
        if (employees.isEmpty()) {
            System.out.println("No employee records found.");
            return;
        }

        // BRANCH MENU BASED ON USERNAME

        if (username.equals("employee")) {
            runEmployeeMenu(scanner, employees); // show only their own info
        } else if (username.equals("payroll_staff")) {
            runMenu(employees, cutoffHoursByEmployee); // full payroll menu
        }
    }
    
    // EMPLOYEE MENU (for reguralr employees)

    private static void runEmployeeMenu(Scanner scanner, Map<String, Employee> employees) {
        while (true) { // keep menu active until user exits
            System.out.println("\n============ EMPLOYEE MENU =============");
            System.out.println("Select an option:");
            System.out.println("  1. Enter Employee Number");
            System.out.println("  2. Exit Program");
            System.out.print("Enter choice: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    System.out.print("Enter Employee Number: ");
                    String empNum = scanner.nextLine().trim();
                    Employee emp = employees.get(empNum);
                    if (emp != null) {
                        printEmployeePayroll(emp, new HashMap<>()); // show payroll info
                    } else {
                        System.out.println("Employee number does not exist.");
                    }
                    break;
                case "2":
                    System.out.println("Exiting program.");
                    return; // exit employee menu
                default:
                    System.out.println("Invalid choice. Please select 1 or 2.");
            }
        }
    }

    // PAYROLL MENU (for payroll staff)

    private static void runMenu(Map<String, Employee> employees, Map<String, Map<String, Double>> cutoffHoursByEmployee) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n=========== PROCESS PAYROLL ============");
            System.out.println("Select an option:");
            System.out.println("  1. One Employee");
            System.out.println("  2. All Employees");
            System.out.println("  3. Exit Program");
            System.out.print("Enter choice: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    processOneEmployee(scanner, employees, cutoffHoursByEmployee);
                    break;
                case "2":
                    processAllEmployees(employees, cutoffHoursByEmployee);
                    break;
                case "3":
                    System.out.println("Exiting program.");
                    return;
                default:
                    System.out.println("Invalid choice. Please select 1, 2, or 3.");
            }
        }
    }

    // PROCESS SINGLE EMPLOYEE

    private static void processOneEmployee(Scanner scanner, Map<String, Employee> employees,
                                           Map<String, Map<String, Double>> cutoffHoursByEmployee) {
        System.out.print("Enter employee number: ");
        String employeeNumber = scanner.nextLine().trim();

        Employee employee = employees.get(employeeNumber);
        if (employee == null) {
            System.out.println("Employee number does not exist.");

            return;
        }

        printEmployeePayroll(employee, cutoffHoursByEmployee.getOrDefault(employeeNumber, new HashMap<>()));
    }

    // PROCESS ALL EMPLOYEES

      private static void processAllEmployees(Map<String, Employee> employees,
                                            Map<String, Map<String, Double>> cutoffHoursByEmployee) {
        for (Employee employee : employees.values()) {
            Map<String, Double> employeeHours = cutoffHoursByEmployee.getOrDefault(employee.employeeNumber, new HashMap<>());
            printEmployeePayroll(employee, employeeHours);
            System.out.println();
        }
    }

    // PRINT PAYROLL INFO

    private static void printEmployeePayroll(Employee employee, Map<String, Double> cutoffHours) {
        List<String> cutoffKeys = buildCutoffKeysFromJuneToDecember2024();

        // employee details
        System.out.println("\n========= EMPLOYEE INFORMATION =========");
        System.out.println("Employee #: " + employee.employeeNumber);
        System.out.println("Employee Name: " + employee.fullName);
        System.out.println("Birthday: " + employee.birthday);
        System.out.println("========================================");

        // loop through all cutoffs for display
        for (String cutoffKey : cutoffKeys) {
            String label = cutoffLabel(cutoffKey);
            double totalHoursWorked = cutoffHours.getOrDefault(cutoffKey, 0.0);
            double grossSalary = employee.grossSemiMonthly;

            System.out.println("\n---- Cutoff Date: " + label + " ----");
            System.out.printf("Total Hours Worked: %.2f%n", totalHoursWorked);
            System.out.printf("Gross Salary: %.2f%n", grossSalary);

            if (isSecondCutoff(cutoffKey)) { // deductions only for 2nd cutoff
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
                System.out.printf("Net Salary: %.2f%n", grossSalary);
            }
        }
    }

    // BUILD CUTOFF KEYS

    private static List<String> buildCutoffKeysFromJuneToDecember2024() {
        List<String> keys = new ArrayList<>();
        for (int month = 6; month <= 12; month++) {
            keys.add(String.format("2024-%02d-1", month)); // 1st cutoff
            keys.add(String.format("2024-%02d-2", month)); // 2nd cutoff
        }
        return keys;
    }

    private static String cutoffLabel(String cutoffKey) {
        String[] parts = cutoffKey.split("-");
        int month = Integer.parseInt(parts[1]);
        int part = Integer.parseInt(parts[2]);
        String monthName = YearMonth.of(2024, month).getMonth().name().substring(0, 1)
                + YearMonth.of(2024, month).getMonth().name().substring(1).toLowerCase();

        if (part == 1) {
            return monthName + " 1 to " + monthName + " 15";
        }

            // skip the first row of the CSV file since it contains column headers
        int endOfMonth = YearMonth.of(2024, month).lengthOfMonth();
        return monthName + " 16 to " + monthName + " " + endOfMonth;
    }

    private static boolean isSecondCutoff(String cutoffKey) {
        return cutoffKey.endsWith("-2");
    }

    // LOAD EMPLOYEES

    private static Map<String, Employee> loadEmployees(File file) {
        Map<String, Employee> employees = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            reader.readLine(); // skip header

            String line;
            while ((line = reader.readLine()) != null) {

                String[] parts = splitCsvLine(line);

                if (parts.length < MIN_EMPLOYEE_COLS) {
                    continue;
                }

                String employeeNumber = clean(parts[COL_EMPLOYEE_NUMBER]);
                String lastName = clean(parts[COL_LAST_NAME]);
                String firstName = clean(parts[COL_FIRST_NAME]);
                String birthday = clean(parts[COL_BIRTHDAY]);
                Double grossSemiMonthly = tryParseMoney(parts[COL_GROSS_SEMI_MONTHLY]);

                if (employeeNumber.isEmpty() || grossSemiMonthly == null) {
                    continue;
                }

                String fullName = firstName + " " + lastName;
                employees.put(employeeNumber, new Employee(employeeNumber, fullName, birthday, grossSemiMonthly));
            }
        } catch (IOException e) {
            System.out.println("Error reading employee file: " + e.getMessage());
        }

        return employees;
    }

    private static Map<String, Map<String, Double>> loadAttendanceCutoffHours(File file) {
        Map<String, Map<String, Double>> hoursByEmployee = new HashMap<>();

        // LOAD ATTENDANCE AND CALCULATE HOURS

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = splitCsvLine(line);
                if (parts.length < MIN_ATTENDANCE_COLS) {
                    continue;
                }

                String employeeNumber = clean(parts[ATTENDANCE_COL_EMPLOYEE_NUMBER]);
                LocalDate date = parseDate(clean(parts[ATTENDANCE_COL_DATE]));
                Double workedHours = computeWorkedHours(clean(parts[ATTENDANCE_COL_LOG_IN]), clean(parts[ATTENDANCE_COL_LOG_OUT]));

                if (employeeNumber.isEmpty() || date == null || workedHours == null) {
                    continue;
                }

                // only process attendance records between June and December 2024
                if (date.getYear() != 2024 || date.getMonthValue() < 6 || date.getMonthValue() > 12) {
                    continue;
                }

                String cutoffKey = cutoffKey(date);
                hoursByEmployee
                        .computeIfAbsent(employeeNumber, k -> new HashMap<>())
                        .merge(cutoffKey, workedHours, Double::sum);

            }

        } catch (IOException e) {
          System.out.println("Error reading attendance file: " + e.getMessage());
        }

        // intentionally not closing the Scanner to avoid closing System.in for the rest of the JVM
        return hoursByEmployee;
    }

    private static String cutoffKey(LocalDate date) {
        int cutoffPart = date.getDayOfMonth() <= 15 ? 1 : 2;
        return String.format("%04d-%02d-%d", date.getYear(), date.getMonthValue(), cutoffPart);

    }

    // PAYROLL CALCULATIONS

    private static Double computeWorkedHours(String logInRaw, String logOutRaw) {
        try {
            LocalTime logIn = LocalTime.parse(logInRaw, TIME_FORMAT);
            LocalTime logOut = LocalTime.parse(logOutRaw, TIME_FORMAT);

            if (logOut.isBefore(logIn)) {
                return null;
            }

            Duration duration = Duration.between(logIn, logOut);
            return duration.toMinutes() / 60.0; // convert to hours
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static LocalDate parseDate(String rawDate) {
        try {
            return LocalDate.parse(rawDate, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }

    }

    private static String[] splitCsvLine(String line) {
        return CSV_SPLIT_PATTERN.split(line, -1);
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace("\"", "").trim();
    }

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
            return null;
        }
    }

    private static boolean isReadableFile(File file) {
        return file.exists() && file.isFile() && file.canRead();

    }

    // DEDUCTIONS

    public static double computeSSS(double salary) {
        // placeholder: simple percentage-based computation
        return salary * 0.045;
    }

    public static double computePhilHealth(double salary) {
        // placeholder: simple percentage-based computation
        return salary * 0.03;
    }

    public static double computePagIBIG(double salary) {
        // placeholder: simple percentage-based computation
        return salary * 0.02;
    }

    public static double computeIncomeTax(double salary) {
        // placeholder: brackets for demo purposes
        if (salary <= 20000) {
            return 0;
        } else if (salary <= 40000) {
            return salary * 0.10;
        }
        return salary * 0.20;
    }

    // EMPLOYEE CLASS

    private static class Employee {
        private final String employeeNumber;
        private final String fullName;
        private final String birthday;
        private final double grossSemiMonthly;

        private Employee(String employeeNumber, String fullName, String birthday, double grossSemiMonthly) {
            this.employeeNumber = employeeNumber;
            this.fullName = fullName;
            this.birthday = birthday;
            this.grossSemiMonthly = grossSemiMonthly;
        }
    }
}