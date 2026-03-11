//hello im the dude who owns this code.
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.util.Scanner;
import java.util.regex.Pattern;

public class SearchEmployeePayroll {

    // Default location of the CSV in this repo. If you move the file, update this path, jit
    private static final String DEFAULT_CSV_PATH =
            "D:\\Seth\\Modelsheets\\LHVengence\\Hello-World-MotorPh\\Employee Details.csv";

    // CSV columns used by this program (0-based indexes).
    private static final int COL_EMPLOYEE_NUMBER = 0;
    private static final int COL_LAST_NAME = 1;
    private static final int COL_FIRST_NAME = 2;
    private static final int COL_GROSS_SALARY = 13;

    // Expected minimum number of columns in a valid row, jit
    private static final int MIN_COLUMNS = 14;

    /*
     * Split CSV by commas that are NOT inside double quotes, jit.
     * This keeps values like "50,000" together as one column, jit.
     */
    private static final Pattern CSV_SPLIT_PATTERN =
            Pattern.compile(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter Employee Number: ");
        String inputEmployeeNumber = scanner.nextLine().trim();

        if (inputEmployeeNumber.isEmpty()) {
            System.out.println("Employee number is required.");
            return;
        }

        File file = new File(DEFAULT_CSV_PATH);

        if (!isReadableFile(file)) {
            System.out.println("Error: CSV file not found or unreadable.");
            return;
        }

        boolean employeeFound = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            // Skip the header row (assumes the first line contains column names)     ... jit.
            reader.readLine();

            String line;
            while ((line = reader.readLine()) != null) {

                String[] parts = splitCsvLine(line);

                // Guard against malformed rows.
                if (parts.length < MIN_COLUMNS) {
                    continue;
                }

                String employeeNumber = parts[COL_EMPLOYEE_NUMBER].trim();

                if (employeeNumber.equals(inputEmployeeNumber)) {

                    employeeFound = true;

                    String lastName = parts[COL_LAST_NAME].trim();
                    String firstName = parts[COL_FIRST_NAME].trim();
                    String fullName = firstName + " " + lastName;

                    // Salary may be quoted and may contain commas (e.g., "50,000").
                    Double grossSalary = tryParseMoney(parts[COL_GROSS_SALARY]);
                    if (grossSalary == null) {
                        System.out.println("Invalid salary value for employee: " + employeeNumber);
                        break;
                    }

                    double sss = computeSSS(grossSalary);
                    double philHealth = computePhilHealth(grossSalary);
                    double pagIbig = computePagIBIG(grossSalary);
                    double incomeTax = computeIncomeTax(grossSalary);

                    double totalDeductions = sss + philHealth + pagIbig + incomeTax;
                    double netPay = grossSalary - totalDeductions;

                    printPayrollSummary(
                            employeeNumber,
                            fullName,
                            grossSalary,
                            sss,
                            philHealth,
                            pagIbig,
                            incomeTax,
                            netPay
                    );

                    break; // stop searching jit after finding the employeesjit
                }
            }

            if (!employeeFound) {
                System.out.println("Employee number not found.");
            }

        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }

        // Intentionally not closing the Scanner to avoid closing System.in for the rest of the JVM.
    }

    // --- Payroll Methods ---

    private static boolean isReadableFile(File file) {
        return file.exists() && file.isFile() && file.canRead();
    }

    private static String[] splitCsvLine(String line) {
        return CSV_SPLIT_PATTERN.split(line, -1);
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

    private static void printPayrollSummary(
            String employeeNumber,
            String fullName,
            double grossSalary,
            double sss,
            double philHealth,
            double pagIbig,
            double incomeTax,
            double netPay
    ) {
        System.out.println();
        System.out.println("===== Payroll Summary =====");
        System.out.println("Employee Number: " + employeeNumber);
        System.out.println("Employee Name: " + fullName);
        System.out.printf("Gross Salary: %.2f%n", grossSalary);
        System.out.printf("SSS: %.2f%n", sss);
        System.out.printf("PhilHealth: %.2f%n", philHealth);
        System.out.printf("Pag-IBIG: %.2f%n", pagIbig);
        System.out.printf("Income Tax: %.2f%n", incomeTax);
        System.out.printf("Net Pay: %.2f%n", netPay);
        System.out.println("===========================");
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
}
