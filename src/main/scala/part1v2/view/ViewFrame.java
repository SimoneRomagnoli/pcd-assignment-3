package part1v2.view;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import part1v2.controller.InputListener;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * GUI component of the view.
 *
 */
public class ViewFrame extends JFrame implements ActionListener {

	private static final int WIDTH = (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth()/1.4);
	private static final int HEIGHT = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight()/2;

	private static final Object[] OCCURRENCES_TABLE_COLUMNS = {"Word", "Occurrences"};

	//DIRECTORY
	private JLabel dirLabel;
	private JTextField pdfDirectory;
	private JButton pdfDirectoryChooser;

	//EXCLUDED WORDS
	private JLabel excLabel;
	private JTextField excludeWords;
	private JButton excludeWordsFileChooser;

	//LIMIT WORDS
	private JLabel limitLabel;
	private JTextField limitOfWords;

	//RESULTS
	private JLabel resLabel;
	private JLabel wordsLabel;
	private JTextField elaboratedWords;
	private JPanel chartPanel;
	private DefaultCategoryDataset dataset =new DefaultCategoryDataset();
	private JTable occurrencesTable;
	private JScrollPane occurrencesTableContainer;

	//BUTTONS
	private JButton startButton;
	private JButton stopButton;

	private ArrayList<InputListener> listeners;

	public ViewFrame(){
		super("View");
		setSize(600,400);
		listeners = new ArrayList<>();

		this.createDirectoryInput();
		this.createExcludedInput();
		this.createLimitWordsInput();
		this.createElaboratedWordsOutput();
		this.createStartButton();
		this.createStopButton();
		this.createChartPanel();
		this.createTable();

		this.setSize(WIDTH, HEIGHT);
		setResizable(false);
		this.setLayout(new BorderLayout());
		this.setVisible(true);
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

	public void addListener(InputListener l){
		listeners.add(l);
	}
	
	public void actionPerformed(ActionEvent ev){
		Object src = ev.getSource();
		if (this.pdfDirectoryChooser.equals(src)) {
			final JFileChooser startDirectoryChooser = new JFileChooser();
			startDirectoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			startDirectoryChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
		    int returnVal = startDirectoryChooser.showOpenDialog(this);
		    if(returnVal == JFileChooser.APPROVE_OPTION) {
		        pdfDirectory.setText(startDirectoryChooser.getSelectedFile().getAbsolutePath());
		     }
		} else if (this.excludeWordsFileChooser.equals(src)) {
			JFileChooser wordsToDiscardFileChooser = new JFileChooser();
			wordsToDiscardFileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
		    int returnVal = wordsToDiscardFileChooser.showOpenDialog(this);
		    if(returnVal == JFileChooser.APPROVE_OPTION) {
		        excludeWords.setText(wordsToDiscardFileChooser.getSelectedFile().getAbsolutePath());
		     }
		} else if (src == startButton) {
			File dir = new File(pdfDirectory.getText());
			File configFile = new File(excludeWords.getText());
			int limitWords = Integer.parseInt(limitOfWords.getText());
			this.notifyStarted(dir, configFile, limitWords);

			DefaultTableModel model = (DefaultTableModel) this.occurrencesTable.getModel();
			IntStream.generate(() -> 0).limit(model.getRowCount()).forEach(model::removeRow);
			dataset.clear();
			this.elaboratedWords.setText("0");

			this.startButton.setEnabled(false);
			this.stopButton.setEnabled(true);
			pdfDirectoryChooser.setEnabled(false);
			excludeWordsFileChooser.setEnabled(false);

		} else if (src == stopButton) {
			this.notifyStopped();

			this.startButton.setEnabled(true);
			this.stopButton.setEnabled(false);
			pdfDirectoryChooser.setEnabled(true);
			excludeWordsFileChooser.setEnabled(true);
		}

	}

	private void notifyStarted(File dir, File wordsFile, int limitWords){
		for (InputListener l: listeners){
			l.started(dir, wordsFile, limitWords);
		}
	}
	
	private void notifyStopped(){
		for (InputListener l: listeners){
			l.stopped();
		}
	}
	
	public void update(final int words, final Map<String, Integer> occurrences) {
		SwingUtilities.invokeLater(() -> {
			this.elaboratedWords.setText(""+words);
		});
		if(!occurrences.isEmpty()) {

			SwingUtilities.invokeLater(() -> {
				DefaultTableModel model = (DefaultTableModel) this.occurrencesTable.getModel();
				IntStream.generate(() -> 0).limit(model.getRowCount()).forEach(model::removeRow);
				dataset.clear();
				for (String word : occurrences.keySet().stream().sorted((a, b) -> occurrences.get(b) - occurrences.get(a)).collect(Collectors.toList())) {
					this.dataset.addValue(occurrences.get(word), "row", word);
					model.addRow(new String[] {
							word, String.valueOf(occurrences.get(word))
					});
				}
			});
		}
	}
	
	public void done() {
		SwingUtilities.invokeLater(() -> {
			this.startButton.setEnabled(true);
			this.stopButton.setEnabled(false);
			this.pdfDirectoryChooser.setEnabled(true);
			this.excludeWordsFileChooser.setEnabled(true);
		});

	}

	private void createDirectoryInput() {
		this.dirLabel = new JLabel("Directory with PDFs:");
		this.dirLabel.setBounds((int)(HEIGHT*0.05), (int)(HEIGHT*0.025), (int)(WIDTH*0.2), (int)(HEIGHT*0.1));
		this.pdfDirectory = new JTextField(10);
		this.pdfDirectory.setBounds((int)(HEIGHT*0.05), (int)(HEIGHT*0.1), (int)(WIDTH*0.2), (int)(HEIGHT*0.1));
		this.pdfDirectoryChooser = new JButton("Find directory");
		this.pdfDirectoryChooser.setBounds((int)(HEIGHT*0.05), (int)(HEIGHT*0.225), (int)(HEIGHT*0.4), (int)(HEIGHT*0.05));
		this.pdfDirectoryChooser.addActionListener(this);
		this.add(this.dirLabel);
		this.add(this.pdfDirectory);
		this.add(this.pdfDirectoryChooser);
	}

	private void createExcludedInput() {
		this.excLabel = new JLabel("File with excluded words:");
		this.excLabel.setBounds((int)(HEIGHT*0.05), (int)(HEIGHT*0.325), (int)(WIDTH*0.2), (int)(HEIGHT*0.1));
		this.excludeWords = new JTextField(10);
		this.excludeWords.setBounds((int)(HEIGHT*0.05), (int)(HEIGHT*0.4), (int)(WIDTH*0.2), (int)(HEIGHT*0.1));
		this.excludeWordsFileChooser = new JButton("Find file");
		this.excludeWordsFileChooser.setBounds((int)(HEIGHT*0.05), (int)(HEIGHT*0.525), (int)(HEIGHT*0.4), (int)(HEIGHT*0.05));
		this.excludeWordsFileChooser.addActionListener(this);
		this.add(this.excLabel);
		this.add(this.excludeWords);
		this.add(this.excludeWordsFileChooser);
	}

	private void createLimitWordsInput() {
		this.limitLabel = new JLabel("Select a number of words:");
		this.limitLabel.setBounds((int)(HEIGHT*0.05), (int)(HEIGHT*0.625), (int)(WIDTH*0.2), (int)(HEIGHT*0.1));
		this.limitOfWords = new JTextField(10);
		this.limitOfWords.setBounds((int)(HEIGHT*0.05), (int)(HEIGHT*0.7), (int)(WIDTH*0.2), (int)(HEIGHT*0.1));
		this.limitOfWords.setText("5");
		this.add(this.limitLabel);
		this.add(this.limitOfWords);
	}

	private void createTable() {
		this.resLabel = new JLabel("Results:");
		this.resLabel.setBounds((int)(WIDTH*0.25), (int)(HEIGHT*0.025), (int)(WIDTH*0.2), (int)(HEIGHT*0.1));
		this.occurrencesTable = new JTable(new DefaultTableModel(OCCURRENCES_TABLE_COLUMNS, 0));
		this.occurrencesTableContainer = new JScrollPane(this.occurrencesTable);
		this.occurrencesTableContainer.setBounds((int)(WIDTH*0.25), (int)(HEIGHT*0.1), (int)(WIDTH*0.2), (int)(HEIGHT*0.5));
		this.add(this.occurrencesTableContainer);
	}

	private void createElaboratedWordsOutput() {
		this.wordsLabel = new JLabel("Elaborated words:");
		this.wordsLabel.setBounds((int)(WIDTH*0.25), (int)(HEIGHT*0.625), (int)(WIDTH*0.2), (int)(HEIGHT*0.1));
		this.elaboratedWords = new JTextField(10);
		this.elaboratedWords.setBounds((int)(WIDTH*0.25), (int)(HEIGHT*0.7), (int)(WIDTH*0.2), (int)(HEIGHT*0.1));
		this.elaboratedWords.setText("0");
		this.add(this.wordsLabel);
		this.add(this.elaboratedWords);
	}

	private void createStartButton() {
		this.startButton = new JButton("Start");
		this.startButton.setBounds((int)(WIDTH*0.5), (int)(HEIGHT*0.7), (int)(WIDTH*0.15), (int)(HEIGHT*0.1));
		this.startButton.addActionListener(this);
		this.add(this.startButton);
	}

	private void createStopButton() {
		this.stopButton = new JButton("Stop");
		this.stopButton.setBounds((int)(WIDTH*0.7), (int)(HEIGHT*0.7), (int)(WIDTH*0.15), (int)(HEIGHT*0.1));
		this.stopButton.addActionListener(this);
		this.stopButton.setEnabled(false);
		this.add(this.stopButton);
	}

	private void createChartPanel() {
		JFreeChart barChart = ChartFactory.createBarChart(
				"",
				"",
				"Occurrences",
				dataset,
				PlotOrientation.HORIZONTAL,
				false, true, false);

		this.chartPanel = new ChartPanel(barChart);
		this.chartPanel.setBounds((int)(WIDTH*0.5), (int)(HEIGHT*0.1), (int)(WIDTH*0.45), (int)(HEIGHT*0.5));
		this.add(chartPanel);
	}

}
	
