package auxiliary;

public class Utils {
    public static double gamma(int n, int d) {
	if (n == 0) return 1;
	double numerator = Math.pow(n, d);
	double denominator = 0;
	for (int i = 1; i <= n; i++) {
		denominator += Math.pow(i, d);
	}
	return numerator / denominator;
    }
}
