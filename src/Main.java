import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;

public class Main extends JFrame {
	private JPanel contentPane;
	private JTextPane textPane;
	private JTextField textField;
	private Document doc;
	private AttributeSet asWhite;
	private AttributeSet asOffWhite;
	private BufferedWriter outputWriter;
	private BufferedWriter logWriter;
	private static HashMap<String, ArrayList<Vehicle>> lotus = new HashMap<>();

	private Main() throws IOException {
		setContentPane(contentPane);

		// call onCancel() when cross is clicked
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				onCancel();
			}
		});

		// call onCancel() on ESCAPE
		contentPane.registerKeyboardAction(e -> onCancel(),
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

		textField.addActionListener(e -> process());
		doc = textPane.getDocument();
		textPane.setEditable(false);
		StyleContext sc = StyleContext.getDefaultStyleContext();
		//Color 1 - White
		asWhite = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, new Color(255, 255, 255));
		asWhite = sc.addAttribute(asWhite, StyleConstants.FontFamily, "Lucida Console");
		asWhite = sc.addAttribute(asWhite, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);
		sc = StyleContext.getDefaultStyleContext();
		//Color 2 - Off White
		asOffWhite = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, new Color(200, 200, 200));
		asOffWhite = sc.addAttribute(asOffWhite, StyleConstants.FontFamily, "Lucida Console");
		asOffWhite = sc.addAttribute(asOffWhite, StyleConstants.Alignment, StyleConstants.ALIGN_JUSTIFIED);
		//File file = new File(new Timestamp(System.currentTimeMillis()).toString() + ".txt");
		String time = new Timestamp(System.currentTimeMillis()).toString() + ".txt";
		File file = new File(time.replaceAll(":", ""));
		if (file.createNewFile())
			outputWriter = new BufferedWriter(new FileWriter(file));
		logWriter = new BufferedWriter(new FileWriter("EZBondRelease.log", true));
		loadLotus();
		textPane.setBackground(new Color(43, 43, 43));
		log(String.format("Saving to %s", time), false);
		DefaultCaret caret = (DefaultCaret) textPane.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
	}

	private void process() {
		String stockNumber = textField.getText().toUpperCase().trim();
		log(stockNumber);
		if (stockNumber.startsWith("MANUAL")) {
			String[] d = stockNumber.split(" ");
			save(String.format("%s,%s,%s", d[1], d[2], d[3]));
		} else if (stockNumber.length() >= 8) {
			handle(stockNumber, 8);
		} else if (stockNumber.length() >= 6) {
			handle(stockNumber, 6);
		} else {
			log("Invalid length", true);
		}
	}

	private void log(String s) {
		try {
			logWriter.write(s);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handle(String stockNumber, int length) {
		stockNumber = stockNumber.substring(stockNumber.length() - length, stockNumber.length());
		ArrayList<Vehicle> av = lotus.get(stockNumber);
		if (av != null && av.size() == 1) {
			Vehicle v = av.get(0);
			if (v.validUPS()) {
				save(v.toString());
				textField.setText("");
				return;
			}
			log("Invalid UPS number in Lotus\n" +
					"RECOMMENDED: Enter the information manually with the format \"MANUAL VIN ENTRY UPS\"", true);
			return;
		}
		log("Either none or multiple entries found!\n" +
				"RECOMMENDED: Enter the information manually with the format \"MANUAL VIN ENTRY UPS\"", true);
		if (length == 6) {
			log("Or, try again with the last 8 instead of the last 6", true);
		}
	}

	private static void loadLotus() {
		try {
			Files.lines(Paths.get("U:\\Filing\\Bond Release Database.txt")).forEach(data -> {
				String[] datum = data.split(",");
				String vin = datum[0].trim();
				Vehicle vehicle = new Vehicle(data);
				if (vin.length() > 6) {
					if (vin.length() > 8) {
						addToLotus(vehicle, vin.substring(vin.length() - 8, vin.length()));
					}
					addToLotus(vehicle, vin.substring(vin.length() - 6, vin.length()));
				} else {
					addToLotus(vehicle, vin);
				}

			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void addToLotus(Vehicle vehicle, String stock) {
		lotus.putIfAbsent(stock, new ArrayList<>());
		lotus.get(stock).add(vehicle);
	}

	private void log(String msg, boolean isWhite) {
		int len = textPane.getDocument().getLength();
		try {
			textPane.getDocument().insertString(len, msg + "\n", isWhite ? asWhite : asOffWhite);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		System.out.println(msg);
	}

	private void save(String msg) {
		log(msg, false);
		try {
			outputWriter.write(msg + "\r\n");
			outputWriter.flush();
		} catch (IOException e) {
			log("Could not save output!!!!!", true);
		}
	}

	private void onCancel() {
		// add your code here if necessary
		dispose();
	}

	public static void main(String[] args) {
		try {
			Main window = new Main();
			window.setSize(720, 480);
			window.setVisible(true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
