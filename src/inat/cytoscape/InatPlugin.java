package inat.cytoscape;

import fitting.ParameterFitter;
import inat.InatBackend;
import inat.cytoscape.INATPropertyChangeListener.ColorsListener;
import inat.cytoscape.INATPropertyChangeListener.ShapesListener;
import inat.exceptions.InatException;
import inat.graph.FileUtils;
import inat.util.XmlConfiguration;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.text.DecimalFormat;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.CytoscapeInit;
import cytoscape.data.attr.MultiHashMapListener;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.view.CytoscapeDesktop;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.visual.VisualMappingManager;

/**
 * The ANIMO Cytoscape plugin main class.
 * 
 * @author Brend Wanders
 */
public class InatPlugin extends CytoscapePlugin {
	private final File configurationFile;
	private static final String SECONDS_PER_POINT = "seconds per point";
	private ShapesLegend legendShapes;
	private ColorsLegend legendColors;
	public static final String TAB_NAME = "ANIMO";
	private ColorsListener colorsListener = null;
	private ShapesListener shapesListener = null;

	/**
	 * Mandatory constructor.
	 */
	public InatPlugin() {
		this.configurationFile = CytoscapeInit.getConfigFile("ANIMO-configuration.xml");

		try {
			InatBackend.initialise(this.configurationFile);
			System.setProperty("java.security.policy", CytoscapeInit.getConfigFile("ANIMO-security.policy").getAbsolutePath());

			CytoPanel p = Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST);
			p.add(TAB_NAME, this.setupPanel(this));
			
			INATPropertyChangeListener pcl = new INATPropertyChangeListener(legendColors, legendShapes);
			Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(Cytoscape.NETWORK_CREATED, pcl); //Add all visual mappings
			Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(Cytoscape.NETWORK_LOADED, pcl); //Make arrows "smooth"
			Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(CytoscapeDesktop.NETWORK_VIEW_CREATED, pcl); //Add right-click menus
			Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(Cytoscape.NETWORK_MODIFIED, pcl); //Add/remove nodes

			colorsListener = pcl.getColorsListener();
			shapesListener = pcl.getShapesListener();

			final VisualMappingManager vizMap = Cytoscape.getVisualMappingManager();
			ChangeListener vizMapChangeListener = new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					//String nome = "(boh)";
					if (Cytoscape.getCurrentNetworkView() != null) {
						//nome = Cytoscape.getCurrentNetworkView().getIdentifier();
						vizMap.removeChangeListener(colorsListener);
						vizMap.addChangeListener(colorsListener);
						vizMap.removeChangeListener(shapesListener);
						vizMap.addChangeListener(shapesListener);
					}
					//System.err.println("La rete " + nome + " ha cambiato stile in " + vizMap.getVisualStyle());
				}
			};
			vizMap.addChangeListener(vizMapChangeListener);
			
		} catch (InatException e) {
			// show error panel
			CytoPanel p = Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST);
			JPanel errorPanel = new JPanel(new BorderLayout());
			JTextArea message = new JTextArea(e.getMessage());
			message.setEditable(false);
			JScrollPane viewport = new JScrollPane(message);
			errorPanel.add(viewport, BorderLayout.CENTER);
			p.add(TAB_NAME, errorPanel);
		}
	}
	
	
	/**
	 * Constructs the panel
	 * 
	 * @param plugin the plugin for which the control panel is constructed.
	 * @return
	 */
	private JPanel setupPanel(InatPlugin plugin) {
		final XmlConfiguration configuration = InatBackend.get().configuration();
		String areWeTheDeveloperStr = configuration.get(XmlConfiguration.DEVELOPER_KEY);
		boolean areWeTheDeveloper = false;
		if (areWeTheDeveloperStr != null) {
			areWeTheDeveloper = Boolean.parseBoolean(areWeTheDeveloperStr);
		}
		
		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); //BorderLayout(2, 2));

		// the button container
		JPanel buttons = new JPanel();// GridLayout(5, 1, 2, 2));
		buttons.setLayout(new GridBagLayout()); //new BoxLayout(buttons, BoxLayout.Y_AXIS));
		int yPositionCounter = 0; //The Y position of the various elements to be put in the GridBagLayout. TODO: remember to increment it each time you add an element to the buttons JPanel, and to use it as index for the y position
		
		//This part allows the user to choose whether to perform the computations on the local machine or on a remote machine.
		Box uppaalBox = new Box(BoxLayout.Y_AXIS);
		final JCheckBox remoteUppaal = new JCheckBox("Remote");
		final Box serverBox = new Box(BoxLayout.Y_AXIS);
		final JTextField serverName = new JTextField("my.server.com"),
				   serverPort = new JFormattedTextField("1234");
		remoteUppaal.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				boolean sel = remoteUppaal.isSelected();
				serverName.setEnabled(sel);
				serverPort.setEnabled(sel);
			}
		});
		remoteUppaal.setSelected(false);
		serverName.setEnabled(false);
		serverPort.setEnabled(false);
		//serverBox.add(serverName);
		//serverBox.add(serverPort);
		Box sBox = new Box(BoxLayout.X_AXIS);
		sBox.add(new JLabel("Server"));
		sBox.add(serverName);
		serverBox.add(Box.createVerticalStrut(10));
		serverBox.add(sBox);
		Box pBox = new Box(BoxLayout.X_AXIS);
		pBox.add(new JLabel("Port"));
		pBox.add(serverPort);
		serverBox.add(Box.createVerticalStrut(10));
		serverBox.add(pBox);
		serverBox.add(Box.createVerticalStrut(10));
		
		/*Box localUppaalBox = new Box(BoxLayout.X_AXIS);
		localUppaalBox.add(Box.createHorizontalStrut(5));
		localUppaalBox.add(localUppaal);
		localUppaalBox.add(Box.createGlue());
		uppaalBox.add(localUppaalBox);*/
		//uppaalBox.add(remoteUppaal);
		remoteUppaal.setOpaque(true);
		final ComponentTitledBorder border = new ComponentTitledBorder(remoteUppaal, serverBox, BorderFactory.createEtchedBorder());
		/*localUppaal.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				serverBox.repaint();
			}
		});*/
		serverBox.setBorder(border);
		uppaalBox.add(serverBox);
		//buttons.add(uppaalBox);
		if (areWeTheDeveloper) {
			buttons.add(serverBox, new GridBagConstraints(0, yPositionCounter++, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		}
		
		//This part allows the user to choose between simulation run(s) and Statistical Model Checking
		final JRadioButton normalUppaal = new JRadioButton("Simulation"),
						   smcUppaal = new JRadioButton("SMC");
		ButtonGroup modelCheckingGroup = new ButtonGroup();
		modelCheckingGroup.add(normalUppaal);
		modelCheckingGroup.add(smcUppaal);
		
		final JCheckBox multipleRuns = new JCheckBox("Compute");
		final JRadioButton overlayPlot = new JRadioButton("Overlay plot"),
						   computeAvgStdDev = new JRadioButton("Average and std deviation"); //"Show standard deviation as error bars");
		ButtonGroup multipleRunsGroup = new ButtonGroup();
		multipleRunsGroup.add(overlayPlot);
		multipleRunsGroup.add(computeAvgStdDev);
		computeAvgStdDev.setSelected(true);
		computeAvgStdDev.setToolTipText(computeAvgStdDev.getText());
		overlayPlot.setToolTipText("Plot all run results one above the other");
		final JFormattedTextField timeTo = new JFormattedTextField(240);
		final JFormattedTextField nSimulationRuns = new JFormattedTextField(10);
		final JTextField smcFormula = new JTextField("Pr[<=50](<> MK2 > 50)");
		timeTo.setToolTipText("Plot activity levels up to this time point (real-life MINUTES).");
		nSimulationRuns.setToolTipText("Number of simulations of which to show the average. NO statistical guarantees!");
		smcFormula.setToolTipText("Give an answer to this probabilistic query (times in real-life MINUTES).");
		normalUppaal.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (normalUppaal.isSelected()) {
					timeTo.setEnabled(true);
					multipleRuns.setEnabled(true);
					nSimulationRuns.setEnabled(multipleRuns.isSelected());
					computeAvgStdDev.setEnabled(multipleRuns.isSelected());
					overlayPlot.setEnabled(multipleRuns.isSelected());
					smcFormula.setEnabled(false);
				} else {
					timeTo.setEnabled(false);
					multipleRuns.setEnabled(false);
					nSimulationRuns.setEnabled(false);
					computeAvgStdDev.setEnabled(false);
					overlayPlot.setEnabled(false);
					smcFormula.setEnabled(true);
				}
			}
		});
		multipleRuns.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (multipleRuns.isSelected() && normalUppaal.isSelected()) {
					nSimulationRuns.setEnabled(true);
					computeAvgStdDev.setEnabled(true);
					overlayPlot.setEnabled(true);
				} else {
					nSimulationRuns.setEnabled(false);
					computeAvgStdDev.setEnabled(false);
					overlayPlot.setEnabled(false);
				}
			}
		});
		normalUppaal.setSelected(true);
		smcUppaal.setSelected(false);
		timeTo.setEnabled(true);
		multipleRuns.setEnabled(true);
		multipleRuns.setSelected(false);
		computeAvgStdDev.setEnabled(false);
		computeAvgStdDev.setSelected(true);
		overlayPlot.setEnabled(false);
		nSimulationRuns.setEnabled(false);
		smcFormula.setEnabled(false);
		Box modelCheckingBox = new Box(BoxLayout.Y_AXIS);
		final Box normalBox = new Box(BoxLayout.Y_AXIS);
		Box smcBox = new Box(BoxLayout.X_AXIS);
		//Box normalUppaalBox = new Box(BoxLayout.X_AXIS);
		//normalUppaalBox.add(normalUppaal);
		//normalUppaalBox.add(Box.createGlue());
		//normalBox.add(normalUppaalBox);
		Box timeToBox = new Box(BoxLayout.X_AXIS);
		timeToBox.add(new JLabel("until"));
		timeToBox.add(timeTo);
		timeToBox.add(new JLabel("minutes"));
		normalBox.add(timeToBox);
		Box averageBox = new Box(BoxLayout.X_AXIS);
		averageBox.add(multipleRuns);
		averageBox.add(nSimulationRuns);
		averageBox.add(new JLabel("runs"));
		normalBox.add(averageBox);
		Box stdDevBox = new Box(BoxLayout.X_AXIS);
		stdDevBox.add(computeAvgStdDev);
		stdDevBox.add(Box.createGlue());
		normalBox.add(stdDevBox);
		Box overlayPlotBox = new Box(BoxLayout.X_AXIS);
		overlayPlotBox.add(overlayPlot);
		overlayPlotBox.add(Box.createGlue());
		normalBox.add(overlayPlotBox);
		smcBox.add(smcUppaal);
		smcBox.add(smcFormula);
		normalUppaal.setOpaque(true);
		//final ComponentTitledBorder border2 = new ComponentTitledBorder(normalUppaal, normalBox, BorderFactory.createEtchedBorder());
		//smcUppaal.addChangeListener(new ChangeListener() {
		//	@Override
		//	public void stateChanged(ChangeEvent e) {
		//		normalBox.repaint();
		//	}
		//});
		//normalBox.setBorder(border2);
		//modelCheckingBox.add(normalBox);
		//modelCheckingBox.add(smcBox);
		
		

		JButton loadSimulationDataButton = new JButton(new AbstractAction("Load simulation data...") {
			private static final long serialVersionUID = -998176729911500957L;

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					CyNetwork net = Cytoscape.getCurrentNetwork();
					if (net != null) {
						//Model model = Model.generateModelFromCurrentNetwork(null, -1);
						
						String inputFileName = FileUtils.open(".sim", "ANIMO simulation", Cytoscape.getDesktop());
						if (inputFileName == null) return;
						final InatResultPanel resultViewer = new InatResultPanel(new File(inputFileName));
						resultViewer.addToPanel(Cytoscape.getDesktop().getCytoPanel(SwingConstants.EAST));
						
					} else {
						JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "There is no current network to which to associate the simulation data.\nPlease load a network first.", "No current network", JOptionPane.ERROR_MESSAGE);
					}
				} catch (Exception ex) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					PrintStream ps = new PrintStream(baos);
					ex.printStackTrace(ps);
					JOptionPane.showMessageDialog(Cytoscape.getDesktop(), baos.toString(), "Error: " + ex, JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		Box loadSimulationBox = new Box(BoxLayout.X_AXIS);
		loadSimulationBox.add(Box.createGlue());
		loadSimulationBox.add(loadSimulationDataButton);
		loadSimulationBox.add(Box.createGlue());
		//buttonsBox.add(loadSimulationBox);
		modelCheckingBox.add(loadSimulationBox);
		
		
		modelCheckingBox.add(new LabelledField("Simulation", normalBox));
		buttons.add(modelCheckingBox, new GridBagConstraints(0, yPositionCounter++, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		
		Box buttonsBox = new Box(BoxLayout.Y_AXIS);
		
		final JButton changeSecondsPerPointbutton = new JButton();
		changeSecondsPerPointbutton.setToolTipText("Click here to change the number of seconds to which a single simulation step corresponds");
		new ChangeSecondsAction(plugin, changeSecondsPerPointbutton); //This manages the button for changing the number of seconds per UPPAAL time unit
		//buttons.add(changeSecondsPerPointbutton);
		Box changeSecondsPerPointbuttonBox = new Box(BoxLayout.X_AXIS);
		changeSecondsPerPointbuttonBox.add(Box.createGlue());
		changeSecondsPerPointbuttonBox.add(changeSecondsPerPointbutton);
		changeSecondsPerPointbuttonBox.add(Box.createGlue());
		buttonsBox.add(changeSecondsPerPointbuttonBox);
		Cytoscape.getNetworkAttributes().getMultiHashMap().addDataListener(new MultiHashMapListener() {

			@Override
			public void allAttributeValuesRemoved(String arg0, String arg1) {
				
			}

			@Override
			public void attributeValueAssigned(String objectKey, String attributeName,
					Object[] keyIntoValue, Object oldAttributeValue, Object newAttributeValue) {
				
				if (attributeName.equals(SECONDS_PER_POINT)) {
					if (newAttributeValue != null) {
						double value = 1;
						try {
							value = Double.parseDouble(newAttributeValue.toString());
						} catch (Exception e) {
							value = 1;
						}
						DecimalFormat df = new DecimalFormat("#.########");
						changeSecondsPerPointbutton.setText(df.format(value) + " seconds/step");
					} else {
						changeSecondsPerPointbutton.setText("Choose seconds/step");
					}
				}
			}

			@Override
			public void attributeValueRemoved(String arg0, String arg1,
					Object[] arg2, Object arg3) {
				
			}
		});
		
		
		//The "Parameter Fitter"
		if (areWeTheDeveloper) {
			JButton parameterFit = new JButton("Parameter fitter...");
			parameterFit.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					ParameterFitter fitter = new ParameterFitter();
					fitter.showWindow(false, Integer.parseInt(timeTo.getValue().toString()));
				}
			});
			Box parameterFitBox = new Box(BoxLayout.X_AXIS);
			parameterFitBox.add(Box.createGlue());
			parameterFitBox.add(parameterFit);
			parameterFitBox.add(Box.createGlue());
			buttonsBox.add(parameterFitBox);
		}
		

		//The "Analyse network" button: perform the requested analysis on the current network with the given parameters
		JButton runButton = new JButton(new RunAction(plugin, remoteUppaal, serverName, serverPort, smcUppaal, timeTo, nSimulationRuns, computeAvgStdDev, overlayPlot, smcFormula));
		//buttons.add(runButton);
		Box runButtonBox = new Box(BoxLayout.X_AXIS);
		runButtonBox.add(Box.createGlue());
		runButtonBox.add(runButton);
		runButtonBox.add(Box.createGlue());
		buttonsBox.add(runButtonBox);
		
		
		/*JButton augmentButton = new JButton(new AugmentAction(plugin));
		//buttons.add(augmentButton);
		Box augmentButtonBox = new Box(BoxLayout.X_AXIS);
		augmentButtonBox.add(Box.createGlue());
		augmentButtonBox.add(augmentButton);
		augmentButtonBox.add(Box.createGlue());
		buttonsBox.add(augmentButtonBox);*/
		
		
		buttons.add(buttonsBox, new GridBagConstraints(0, yPositionCounter++, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		

		legendColors = new ColorsLegend();
		legendShapes = new ShapesLegend();
		JPanel panelLegends = new JPanel();
		panelLegends.setBackground(Color.WHITE);
		panelLegends.setLayout(new GridBagLayout());
		panelLegends.add(legendColors, new GridBagConstraints(0, 0, 1, 1, 1, 0.5, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		panelLegends.add(legendShapes, new GridBagConstraints(0, 1, 1, 1, 1, 0.5, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		panelLegends.setPreferredSize(new Dimension(200, 400));
		buttons.add(new LabelledField("Legend", new JScrollPane(panelLegends, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)), new GridBagConstraints(0, yPositionCounter++, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(30, 5, 5, 5), 0, 0));
		
		
		panel.add(buttons);
		

		return panel;
	}

}
