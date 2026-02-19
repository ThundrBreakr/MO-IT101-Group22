
public class Main {

    public class Employee {

        private String employeeName;
        private long employeeNumber;
        private double hoursWorked;
        private byte clockIn;
        private byte clockOut;
        private double hourlyRate;
        private double grossSalary;
        private double netSalary;
        private byte deduction;

        public Employee (String employeeName, long employeeNumber, double hourlyRate) {
            this.employeeName = employeeName;
            this.employeeNumber = employeeNumber;
            this.hourlyRate = hourlyRate;
        }

        public double getHoursWorked() {
            return
        }

        public double getGrossSalary() {
            return getHoursWorked() * hourlyRate;
        }

        public double getNetSalary() {
            return getGrossSalary() / deduction;
        }

        public String getEmployeeName() {
            return getEmployeeName();
        }

    }

    public static void main(String[] args) {

        String employeeName = "Willy Wonder";
        long employeeNumber = 2_025_105_567;
        int checkIn = 9;
        int checkOut = 17;
        int salary = 100;
        byte PERCENT = 100;
        byte deduction = 8;

        //check in & out times at represented by military time without 0
        //make sure to put checkOut first to prevent making negative numbers for salary
        int workHours = checkOut - checkIn;
        int grossSalary = salary * workHours;

        //deductions of grossSalary
        float netSalary = grossSalary * ((float)deduction / PERCENT);

        IO.println(String.format("Hello and welcome!"));
        System.out.println("Employee Name: " + employeeName);
        System.out.println("Employee No: " + employeeNumber);
        System.out.println("Checks In: " + checkIn);
        System.out.println("Checks out: " + checkOut);
        System.out.println("Worked Hours: " + workHours);
        System.out.println("Salary: " + salary);
        System.out.println("Gross Salary: " + grossSalary);
        System.out.println("Net Salary: " + netSalary);

        }
    }