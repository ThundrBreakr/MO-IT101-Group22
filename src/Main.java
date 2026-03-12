
public class Main {

    public static void main(String[] arg) {
        int workedHours = 45;
        int hoursPerDay = 8;

        int totalDaysWorked = workedHours / hoursPerDay;
        int remainingHours = workedHours % hoursPerDay;

        System.out.println(totalDaysWorked + " Full Days Worked");
        System.out.println(remainingHours + " Remaining Hours");

    }

}