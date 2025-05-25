import java.util.*;

public class Main {
    public static double[] calculateCoordinates(double x1, double y1, double theta1,
                                                double x2, double y2, double theta2) {
        // Calculate xk
        double tan_theta1 = Math.tan(Math.toRadians(theta1));
        double tan_theta2 = Math.tan(Math.toRadians(theta2));
        double xk = ((y2 - y1) - (x2 * tan_theta2 - x1 * tan_theta1)) / (tan_theta1 - tan_theta2);

        // Calculate yk
        double cot_theta1 = 1.0 / Math.tan(Math.toRadians(theta1));
        double cot_theta2 = 1.0 / Math.tan(Math.toRadians(theta2));
        double yk = ((x2 - x1) - (y2 * cot_theta2 - y1 * cot_theta1)) / (cot_theta1 - cot_theta2);

        return new double[]{xk, yk};
    }

    public static void main(String[] args) {
        // Example coordinates and angles
        double x1 = 0, y1 = 0, theta1 = 45; // (x1, y1) and theta1 in degrees
        double x2 = 10, y2 = 10, theta2 = 135; // (x2, y2) and theta2 in degrees

        double[] result = calculateCoordinates(x1, y1, theta1, x2, y2, theta2);
        double xk = result[0];
        double yk = result[1];

        System.out.println("Coordinates of point k: (" + xk + ", " + yk + ")");
    }
}