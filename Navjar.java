import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class Navjar extends JFrame {
    private JTabbedPane tabbedPane;

    public Navjar() {
        super("Navjar Browser");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 650);

        tabbedPane = new JTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);

        // Add initial web tab
        addWebTab("https://tinyurl.com/itjsearch");

        // Menu for opening files and running scripts/code
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem openFile = new JMenuItem("Open File...");
        openFile.addActionListener(e -> openFileTab());

        JMenuItem runPython = new JMenuItem("Run Python Script...");
        runPython.addActionListener(e -> runScriptTab("python"));

        JMenuItem runNode = new JMenuItem("Run Node.js Script...");
        runNode.addActionListener(e -> runScriptTab("node"));

        JMenuItem runPythonInput = new JMenuItem("Run Python Code...");
        runPythonInput.addActionListener(e -> runCodeInputTab("python"));

        JMenuItem runNodeInput = new JMenuItem("Run Node.js Code...");
        runNodeInput.addActionListener(e -> runCodeInputTab("node"));

        fileMenu.add(openFile);
        fileMenu.add(runPython);
        fileMenu.add(runNode);
        fileMenu.addSeparator();
        fileMenu.add(runPythonInput);
        fileMenu.add(runNodeInput);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private void addWebTab(String url) {
        JPanel panel = new JPanel(new BorderLayout());
        JTextField addressField = new JTextField(url, 40);
        JButton goButton = new JButton("Go");
        JEditorPane webPane = new JEditorPane();
        webPane.setEditable(false);

        ActionListener loadPage = evt -> {
            try {
                webPane.setPage(addressField.getText());
            } catch (IOException ex) {
                webPane.setText("Failed to load: " + ex.getMessage());
            }
        };

        goButton.addActionListener(loadPage);
        addressField.addActionListener(loadPage);

        webPane.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                addressField.setText(e.getURL().toString());
                try {
                    webPane.setPage(e.getURL());
                } catch (IOException ex) {
                    webPane.setText("Failed to load: " + ex.getMessage());
                }
            }
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(addressField, BorderLayout.CENTER);
        topPanel.add(goButton, BorderLayout.EAST);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(webPane), BorderLayout.CENTER);

        tabbedPane.addTab("Web", panel);
        loadPage.actionPerformed(null);
    }

    private void openFileTab() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            JTextArea textArea = new JTextArea();
            textArea.setEditable(false);
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                textArea.read(reader, null);
            } catch (IOException ex) {
                textArea.setText("Failed to load: " + ex.getMessage());
            }
            tabbedPane.addTab(file.getName(), new JScrollPane(textArea));
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        }
    }

    // Upload and execute a Python or Node.js script, displaying output in a new tab
    private void runScriptTab(String interpreter) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select " + interpreter + " script to run");
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File script = chooser.getSelectedFile();
            JTextArea outputArea = new JTextArea();
            outputArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(outputArea);
            String tabTitle = (interpreter.equals("python") ? "Python: " : "Node: ") + script.getName();
            tabbedPane.addTab(tabTitle, scrollPane);
            tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);

            // Run the script in a background thread
            new Thread(() -> {
                try {
                    ProcessBuilder pb = new ProcessBuilder(interpreter, script.getAbsolutePath());
                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        SwingUtilities.invokeLater(() -> outputArea.append(line + "\n"));
                    }
                    int exitCode = process.waitFor();
                    SwingUtilities.invokeLater(() -> outputArea.append("\nProcess exited with code: " + exitCode));
                } catch (IOException | InterruptedException ex) {
                    SwingUtilities.invokeLater(() -> outputArea.append("Execution failed: " + ex.getMessage()));
                }
            }).start();
        }
    }

    // Direct code input and execution for Python or Node.js
    private void runCodeInputTab(String interpreter) {
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea codeArea = new JTextArea(18, 60);
        JTextArea outputArea = new JTextArea(10, 60);
        outputArea.setEditable(false);
        JButton runButton = new JButton("Run");

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(new JLabel("Enter your " + interpreter + " code below:"), BorderLayout.NORTH);
        topPanel.add(new JScrollPane(codeArea), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(runButton, BorderLayout.NORTH);
        bottomPanel.add(new JLabel("Output:"), BorderLayout.CENTER);
        bottomPanel.add(new JScrollPane(outputArea), BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(bottomPanel, BorderLayout.CENTER);

        String tabTitle = interpreter.equals("python") ? "Python Input" : "Node.js Input";
        tabbedPane.addTab(tabTitle, panel);
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);

        runButton.addActionListener(e -> {
            outputArea.setText(""); // Clear previous output
            try {
                // Write code to a temporary file
                File tempFile = File.createTempFile("navjar_code_", interpreter.equals("python") ? ".py" : ".js");
                try (FileWriter writer = new FileWriter(tempFile)) {
                    writer.write(codeArea.getText());
                }
                // Run the code
                new Thread(() -> {
                    try {
                        ProcessBuilder pb = new ProcessBuilder(interpreter, tempFile.getAbsolutePath());
                        pb.redirectErrorStream(true);
                        Process process = pb.start();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            SwingUtilities.invokeLater(() -> outputArea.append(line + "\n"));
                        }
                        int exitCode = process.waitFor();
                        SwingUtilities.invokeLater(() -> outputArea.append("\nProcess exited with code: " + exitCode));
                    } catch (IOException | InterruptedException ex) {
                        SwingUtilities.invokeLater(() -> outputArea.append("Execution failed: " + ex.getMessage()));
                    }
                }).start();
            } catch (IOException ex) {
                outputArea.setText("Could not create temp file: " + ex.getMessage());
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Navjar().setVisible(true));
    }
}
