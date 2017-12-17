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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class Main extends JFrame {
	private JPanel contentPane;
	private JTextPane textPane;
	private JTextField textField;
	private AttributeSet asWhite;
	private AttributeSet asOffWhite;
	private BufferedWriter outputWriterRetta;
	private BufferedWriter outputWriterUs;
	private BufferedWriter logWriter;
	private static HashMap<String, ArrayList<Vehicle>> lotus = new HashMap<>();
	private static HashSet<String> alreadyRequested = new HashSet<>();
	private static HashSet<String> enteredThisSession = new HashSet<>();

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
		String time = new Timestamp(System.currentTimeMillis()).toString().replaceAll(":", "") + ".txt";
		File file1 = new File("Retta" + time);
		File file2 = new File("Us" + time);
		if (file1.createNewFile() && file2.createNewFile()) {
			outputWriterRetta = new BufferedWriter(new FileWriter(file1));
			outputWriterUs = new BufferedWriter(new FileWriter(file2));
		}
		logWriter = new BufferedWriter(new FileWriter("EZBondRelease.log", true));
		loadLotus();
		textPane.setBackground(new Color(43, 43, 43));
		show(String.format("Saving to %s", time), false);
		DefaultCaret caret = (DefaultCaret) textPane.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
	}

	private void process() {
		String input = textField.getText().toUpperCase().trim();
		if (input.length() > 17) Arrays.stream(input.split(" ")).forEach(this::process);
		process(input);
	}

	private void process(String stockNumber) {
		log(stockNumber);
		if (enteredThisSession.contains(stockNumber)) {
			show("Already entered that this session. Use MANUAL to override", true);
			return;
		}
		enteredThisSession.add(stockNumber);
		if (stockNumber.startsWith("MANUAL")) {
			String[] d = stockNumber.split(" ");
			save(String.format("%s,%s,%s", d[1], d[2], d[3]), outputWriterRetta, outputWriterUs);
		} else if (stockNumber.length() >= 8) {
			handle(stockNumber, 8);
		} else if (stockNumber.length() >= 6) {
			handle(stockNumber, 6);
		} else {
			show("Invalid length", true);
		}
	}

	private void handle(String stockNumber, int length) {
		stockNumber = stockNumber.substring(stockNumber.length() - length, stockNumber.length());
		ArrayList<Vehicle> av = lotus.get(stockNumber);
		if (av != null && av.size() == 1) {
			Vehicle v = av.get(0);
			if (v.validUPS()) {
				if (!alreadyRequested.contains(v.getVIN())) {
					save(v.toString(), outputWriterRetta, outputWriterUs);
					textField.setText("");
					return;
				}
				save(v.toString(), outputWriterUs);
				show("We already have that bond release", true);
				return;
			}
			show("Invalid UPS number in Lotus\n" +
					"RECOMMENDED: Enter the information manually with the format \"MANUAL VIN ENTRY UPS\"", true);
			return;
		}
		show("Either none or multiple entries found!\n" +
				"RECOMMENDED: Enter the information manually with the format \"MANUAL VIN ENTRY UPS\"", true);
		if (length == 6) {
			show("Or, try again with the last 8 instead of the last 6", true);
		}
	}

	private static void loadLotus() {
		try {
			Files.lines(Paths.get("U:\\Filing\\2016 bond release.csv")).forEach(line -> {
				String[] data = line.split(",");
				if (data[1].equals("t"))
					alreadyRequested.add(data[0]);
			});
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

	private void show(String msg, boolean isWhite) {
		Document doc = textPane.getDocument();
		try {
			doc.insertString(doc.getLength(), msg + "\n", isWhite ? asWhite : asOffWhite);
			textPane.setCaretPosition(doc.getLength());
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		System.out.println(msg);
	}

	private void save(String msg, BufferedWriter... destinations) {
		show(msg, false);
		try {
			for (BufferedWriter destination : destinations) {
				destination.write(msg + "\r\n");
				destination.flush();
			}
		} catch (IOException e) {
			show("Could not save output!!!!!", true);
		}
	}

	private void log(String msg) {
		try {
			logWriter.write(String.format("%s %s\r\n", new Timestamp(System.currentTimeMillis()).toString(), msg));
			logWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
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
