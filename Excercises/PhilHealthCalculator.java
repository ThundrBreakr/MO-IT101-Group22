package Excercises;
import java.util.Scanner;

public class PhilHealthCalculator {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter your monthly salary: ");
        if (!scanner.hasNextDouble()) {
            /*prevents crash if the input text */
            System.out.println("Invalid input. Please enter a numeric value for the monthly salary.");
            scanner.close();
            return;
        }

        double monthlySalary = scanner.nextDouble();

        if (monthlySalary < 0) {
            System.out.println("Monthly salary cannot be negative. Please enter a valid amount.");
            scanner.close();
            return;
        }

        double premiumRate = 0.03; //3%
        double monthlyPremium = monthlySalary * premiumRate;
        double employeeShare = monthlyPremium * 0.5; // Employer pays 50% of the premium

        System.out.println("\n--- PhilHealth Contribution Details ---");
        System.out.println("Monthly Salary: " + monthlySalary);
        System.out.println("Premium Rate: 3%");
        System.out.println("Monthly Premium: " + monthlyPremium);
        System.out.println("Employee Share (50%): " + employeeShare);

        scanner.close();
    }

    public static double calculatePhilHealthContribution(double monthlySalary) {
        double contributionRate = 0.035; // 3.5% of the monthly salary
        return monthlySalary * contributionRate;
    }
}