//hello im the dude who owns this code.
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

public class SearchEmployeePayroll {

    // Default location of the CSV in this repo. If you move the file, update this path, jit
    private static final String EMPLOYEE_CSV_PATH = "Employee Details.csv";
    private static final String ATTENDANCE_CSV_PATH = "attendance_record.csv";

    private static final Pattern CSV_SPLIT_PATTERN =
            Pattern.compile(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm", Locale.ENGLISH);


    // CSV columns used by this program (0-based indexes).
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

    public static void main(String[] args) {

        File employeeFile = new File(EMPLOYEE_CSV_PATH);
        File attendanceFile = new File(ATTENDANCE_CSV_PATH);

        if (!isReadableFile(employeeFile) || !isReadableFile(attendanceFile)) {
            System.out.println("Error: Required CSV file/s not found or unreadable.");
            return;
        }

        Map<String, Employee> employees = loadEmployees(employeeFile);
        Map<String, Map<String, Double>> cutoffHoursByEmployee = loadAttendanceCutoffHours(attendanceFile);


        if (employees.isEmpty()) {
            System.out.println("No employee records found.");

            return;
        }

        runMenu(employees, cutoffHoursByEmployee);
    }

    private static void runMenu(Map<String, Employee> employees, Map<String, Map<String, Double>> cutoffHoursByEmployee) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n1. Process Payroll (Do not include allowances)");
            System.out.println("Display sub-options:");
            System.out.println("1. One employee");
            System.out.println("2. All employees");
            System.out.println("3. Exit the program");
            System.out.print("Enter your choice: ");

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

    private static void processOneEmployee(Scanner scanner, Map<String, Employee> employees,
                                           Map<String, Map<String, Double>> cutoffHoursByEmployee) {
        System.out.print("Enter the employee number: ");
        String employeeNumber = scanner.nextLine().trim();


        Employee employee = employees.get(employeeNumber);
        if (employee == null) {
            System.out.println("Employee number does not exist.");

            return;
        }

        printEmployeePayroll(employee, cutoffHoursByEmployee.getOrDefault(employeeNumber, new HashMap<>()));
    }


      private static void processAllEmployees(Map<String, Employee> employees,
                                            Map<String, Map<String, Double>> cutoffHoursByEmployee) {
        for (Employee employee : employees.values()) {
            Map<String, Double> employeeHours = cutoffHoursByEmployee.getOrDefault(employee.employeeNumber, new HashMap<>());
            printEmployeePayroll(employee, employeeHours);
            System.out.println();
        }
    }

    private static void printEmployeePayroll(Employee employee, Map<String, Double> cutoffHours) {
        List<String> cutoffKeys = buildCutoffKeysFromJuneToDecember2024();

        System.out.println("\n========================================");
        System.out.println("Employee #: " + employee.employeeNumber);
        System.out.println("Employee Name: " + employee.fullName);
        System.out.println("Birthday: " + employee.birthday);

        for (String cutoffKey : cutoffKeys) {
            String label = cutoffLabel(cutoffKey);
            double totalHoursWorked = cutoffHours.getOrDefault(cutoffKey, 0.0);
            double grossSalary = employee.grossSemiMonthly;

            System.out.println("\nCutoff Date: " + label);
            System.out.printf("Total Hours Worked: %.2f%n", totalHoursWorked);
            System.out.printf("Gross Salary: %.2f%n", grossSalary);

            if (isSecondCutoff(cutoffKey)) {
                double sss = computeSSS(grossSalary);
                double philHealth = computePhilHealth(grossSalary);
                double pagIbig = computePagIBIG(grossSalary);
                double tax = computeIncomeTax(grossSalary);
                double totalDeductions = sss + philHealth + pagIbig + tax;
                double netSalary = grossSalary - totalDeductions;

                System.out.printf("SSS: %.2f%n", sss);
                System.out.printf("PhilHealth: %.2f%n", philHealth);
                System.out.printf("Pag-IBIG: %.2f%n", pagIbig);
                System.out.printf("Tax: %.2f%n", tax);
                System.out.printf("Total Deductions: %.2f%n", totalDeductions);
                System.out.printf("Net Salary: %.2f%n", netSalary);
            } else {
                System.out.printf("Net Salary: %.2f%n", grossSalary);
            }
        }
    }

    private static List<String> buildCutoffKeysFromJuneToDecember2024() {
        List<String> keys = new ArrayList<>();
        for (int month = 6; month <= 12; month++) {
            keys.add(String.format("2024-%02d-1", month));
            keys.add(String.format("2024-%02d-2", month));
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


            // Skip the header row (assumes the first line contains column names)     ... jit.
        int endOfMonth = YearMonth.of(2024, month).lengthOfMonth();
        return monthName + " 16 to " + monthName + " " + endOfMonth + " (Second payout includes all deductions)";
    }

    private static boolean isSecondCutoff(String cutoffKey) {
        return cutoffKey.endsWith("-2");
    }

    
    private static Map<String, Employee> loadEmployees(File file) {
        Map<String, Employee> employees = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            reader.readLine(); // header


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


        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            reader.readLine(); // header
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

        // Intentionally not closing the Scanner to avoid closing System.in for the rest of the JVM.
        return hoursByEmployee;
    }

    private static String cutoffKey(LocalDate date) {
        int cutoffPart = date.getDayOfMonth() <= 15 ? 1 : 2;
        return String.format("%04d-%02d-%d", date.getYear(), date.getMonthValue(), cutoffPart);

    }

    // --- Payroll Methods ---
    private static Double computeWorkedHours(String logInRaw, String logOutRaw) {
        try {
            LocalTime logIn = LocalTime.parse(logInRaw, TIME_FORMAT);
            LocalTime logOut = LocalTime.parse(logOutRaw, TIME_FORMAT);


            if (logOut.isBefore(logIn)) {
                return null;
            }

            Duration duration = Duration.between(logIn, logOut);
            return duration.toMinutes() / 60.0;
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

        // Remove surrounding quotes and thousands separators before parsing.
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

    public static double computeSSS(double salary) {
        // Placeholder: simple percentage-based computation. jit
        return salary * 0.045;
    }

    public static double computePhilHealth(double salary) {
        // Placeholder: simple percentage-based computation. uh jit
        return salary * 0.03;
    }

    public static double computePagIBIG(double salary) {
        // Placeholder: simple percentage-based computation. uh huh jit
        return salary * 0.02;
    }

    public static double computeIncomeTax(double salary) {
        // Placeholder brackets for demo purposes. yeah okay jit
        if (salary <= 20000) {
            return 0;
        } else if (salary <= 40000) {
            return salary * 0.10;
        }
        return salary * 0.20;
    }

1
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