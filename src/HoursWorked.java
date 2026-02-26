import java.util.Scanner;

public class HoursWorked {

    public static void main(String[] args){
        evaluateHoursWorked();
    }

    public static void evaluateHoursWorked() {

        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter Hours Worked: ");

        try {
            double hoursWorked = scanner.nextDouble();

            if (hoursWorked < 0) {
                System.out.println("Input invalid");
            }
            else if (hoursWorked == 0) {
                System.out.println("Employee didn't work");
            }
            else if (hoursWorked >= 1 && hoursWorked <= 40) {
                System.out.println("You have Worked Regular Hours");
            }
            else {
                System.out.println("Overtime Done");
            }

        } catch (Exception e) {
            System.out.println("Input Invalid put numberic value");
        }

        scanner.close();
    }

}