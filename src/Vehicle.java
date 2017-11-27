import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Vehicle {
	private String VIN, entryDate, ups;
	private static final Pattern threeComma = Pattern.compile("(^[^,]*,[^,]*,[^,]*)");
	private static final Pattern dataPattern = Pattern.compile("(^[^,]+)?,([^,]*)?,([^,]*)?$");
	private static final Pattern UPS = Pattern.compile("^(\\w{3}|\\d{3})-\\d{7}-\\d$");
	private static final int FIELDS = 3;

	Vehicle(String data) {
		data = simplify(data);
		String[] datum = data.split(",");
		if (datum[0].equals("VIN NUMBER")) return;
		if (datum.length != FIELDS) {
			//The formatting of the file is broken, but we can't risk losing any data
			Matcher m = dataPattern.matcher(data);
			datum = new String[FIELDS];
			if (m.find()) {
				for (int i = 0; i < m.groupCount(); i++) {
					datum[i] = m.group(i + 1);
				}
			}
		}
		this.VIN = clean(datum[0]);
		this.entryDate = clean(datum[1]);
		this.ups = clean(datum[2]);
	}

	boolean validUPS() {
		return UPS.matcher(ups).find();
	}

	private static String clean(String s) {
		return fixEmpty(s).toUpperCase().trim();
	}

	private static String fixEmpty(String s) {
		return s == null || s.isEmpty() ? "???" : s;
	}

	@Override
	public String toString() {
		return String.format("%s,%s,%s", VIN, entryDate, ups);
	}

	@Override
	public int hashCode() {
		return this.VIN.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Vehicle) {
			Vehicle v = (Vehicle) obj;
			if (VIN.equals(v.VIN) &&
					equalsIgnoreUnknown(entryDate, v.entryDate) &&
					equalsIgnoreUnknown(ups, v.ups)) {
				//Merge wildcard data
				if (!entryDate.equals("???") ^ !v.entryDate.equals("???")) {
					entryDate = entryDate.equals("???") ? v.entryDate : entryDate;
					v.entryDate = v.entryDate.equals("???") ? entryDate : v.entryDate;
				}
				if (!ups.equals("???") ^ !v.ups.equals("???")) {
					ups = ups.equals("???") ? v.ups : ups;
					v.ups = v.ups.equals("???") ? ups : v.ups;
				}
				return true;
			}
		}
		return false;
	}

	private static boolean equalsIgnoreUnknown(String s1, String s2) {
		//They are the same if either one is a wildcard
		return s1.equals("???") || s2.equals("???") || s1.equals(s2);
	}

	private static String simplify(String s) {
		s = s.toUpperCase()
				.replace("\"", "")
				.replaceAll("\t", "")
				.trim();
		Matcher m = threeComma.matcher(s);
		if (m.find()) {
			s = m.group(1);
		}
		return s;
	}

	public String getVIN() {
		return VIN;
	}
}