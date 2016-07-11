
package fr.emn.premode.center;

/**
 * @author Guillaume Le Louët [guillaume.lelouet@gmail.com] 2015
 *
 */
public class PowerMode {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PowerMode.class);

	public int power;

	public int profit;

	public PowerMode() {
		this(0, 0);
	}

	public PowerMode(int power, int profit) {
		this.power = power;
		this.profit = profit;
	}

	public static PowerMode mode(int power, int profit) {
		return new PowerMode(power, profit);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj.getClass() == PowerMode.class) {
			PowerMode pm = (PowerMode) obj;
			return pm.power == power && pm.profit == profit;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return power + profit;
	}

	@Override
	public String toString() {
		return "powermode[" + power + ";" + profit + "]";
	}

}
