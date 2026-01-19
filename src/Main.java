
public class Main {
    public static void main(String[] args) {

        String employeeName = "Willy Wonder";
        long employeeNumber = 2_025_105_567;
        int checkIn = 9;
        int checkOut = 17;
        int salary = 100;
        int deduction = 80;

        int workHours = checkOut - checkIn;
        int grossSalary = salary * workHours;



        IO.println(String.format("Hello and welcome!"));
        System.out.println("Employee Name: " + employeeName);
        System.out.println("Employee No: " + employeeNumber);
        System.out.println("Checks In: " + checkIn);
        System.out.println("Checks out: " + checkOut);
        System.out.println("Worked Hours: " + workHours);
        System.out.println("Salary: " + salary);
        System.out.println("Gross Salary: " + grossSalary);

        }
    }
