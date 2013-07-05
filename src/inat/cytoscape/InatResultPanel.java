package inat.cytoscape;

import inat.InatBackend;
import inat.analyser.uppaal.ResultAverager;
import inat.analyser.uppaal.SimpleLevelResult;
import inat.graph.FileUtils;
import inat.graph.Graph;
import inat.graph.GraphScaleListener;
import inat.graph.Scale;
import inat.model.Model;
import inat.util.Quadruple;
import inat.util.XmlConfiguration;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.view.CyNetworkView;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.view.cytopanels.CytoPanelImp;
import cytoscape.view.cytopanels.CytoPanelListener;
import cytoscape.view.cytopanels.CytoPanelState;
import cytoscape.visual.VisualMappingManager;

/**
 * The Inat result panel.
 * 
 * @author Brend Wanders
 */
public class InatResultPanel extends JPanel implements ChangeListener, GraphScaleListener, CytoPanelListener {

	private static final long serialVersionUID = -163756255393221954L;
	private final Model model; //The model from which the results were obtained
	private final SimpleLevelResult result; //Contains the results to be shown in this panel
	private JPanel container; //The panel on which all the components of this resultPanel are layed out (thus, not only the panel itself, but also the slider, buttons etc)
	private int myIndex; //The index at which the panel is placed when added to the CytoPanel
	private JSlider slider; //The slider to allow the user to choose a moment in the simulation time, which will be reflected on the network window as node colors, indicating the corresponding reactant activity level.
	private double scale, //The time scale
	   minValueOnGraph,
	   maxValueOnGraph, //the scale to translate a value of the slider (in the interval [0,1]) to the corresponding position in the graph
	   scaleForConcentration; //the scale to translate a value of the slider (in the interval [0,1]) to the corresponding value resulting from the UPPAAL trace
	private static final String START_DIFFERENCE = "Difference with...", //The three strings here are for one button. Everybody normally shows START_DIFFERENCE. When the user presses on the button (differenceWith takes the value of this for the InatResultPanel where the button was pressed),
								END_DIFFERENCE = "Difference with this", //every other panel shows the END_DIFFERENCE. If the user presses a button with END_DIFFERENCE, a new InatResultPanel is created with the difference between the data in differenceWith and the panel where END_DIFFERENCE was pressed. Then every panel goes back to START_DIFFERENCE.
								CANCEL_DIFFERENCE = "Cancel difference"; //CANCEL_DIFFERENCE is shown as text of the button only on the panel whare START_DIFFERENCE was pressed. If CANCEL_DIFFERENCE is pressed, then no difference is computed and every panel simply goes back to START_DIFFERENCE
	private String title; //The title to show to the user
	private Graph g;

	
	private JButton differenceButton = null;
	private static InatResultPanel differenceWith = null;
	private static Vector<InatResultPanel> allExistingPanels = new Vector<InatResultPanel>();
	private String vizMapName = null;
	private boolean isDifference = false;
	
	
	/**
	 * Load the simulation data from a file instead than getting it from a simulation we have just made
	 * @param model The representation of the current Cytoscape network, to which the simulation data
	 * will be coupled (it is REQUIRED that the simulation was made on the same network, or else the node IDs
	 * will not be the same, and the slider will not work properly)
	 * @param simulationDataFile The file from which to load the data. For the format, see saveSimulationData()
	 */
	public InatResultPanel(File simulationDataFile) {
		this(loadSimulationData(simulationDataFile));
	}
	
	public InatResultPanel(Quadruple<Model, SimpleLevelResult, Double, String> simulationData) {
		this(simulationData.first, simulationData.second, simulationData.third, simulationData.fourth);
	}
	
	public InatResultPanel(Model model, SimpleLevelResult result, double scale) {
		this(model, result, scale, "ANIMO Results");
	}
	
	/**
	 * The panel constructor.
	 * 
	 * @param model the model this panel uses
	 * @param result the results object this panel uses
	 */
	public InatResultPanel(final Model model, final SimpleLevelResult result, double scale, String title) {
		super(new BorderLayout(), true);
		allExistingPanels.add(this);
		this.model = model;
		this.result = result;
		this.scale = scale;
		this.title = title;

		JPanel sliderPanel = new JPanel(new BorderLayout());
		this.slider = new JSlider();
		this.slider.setOrientation(JSlider.HORIZONTAL);
		this.slider.setMinimum(0);
		//int max = (int)Math.round(result.getTimeIndices().get(result.getTimeIndices().size() - 1) * scale);
		if (result.isEmpty()) {
			this.scaleForConcentration = 1;
		} else {
			this.scaleForConcentration = result.getTimeIndices().get(result.getTimeIndices().size() - 1);
		}
		this.minValueOnGraph = 0;
		this.maxValueOnGraph = this.scaleForConcentration * this.scale;
		this.slider.setMaximum(200);
		this.slider.setPaintTicks(true);
		this.slider.setPaintLabels(true);
		slider.setMajorTickSpacing(slider.getMaximum() / 10);
		slider.setMinorTickSpacing(slider.getMaximum() / 100);
		Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
		DecimalFormat formatter = new DecimalFormat("0.###");
		int nLabels = 10;
		for (int i=0;i<=nLabels;i++) {
			double val = 1.0 * i / nLabels * this.maxValueOnGraph;
			String valStr = formatter.format(val);
			labels.put((int)Math.round(1.0 * i / nLabels * slider.getMaximum()), new JLabel(valStr));
		}
		this.slider.setLabelTable(labels);
		
		this.slider.setValue(0);
		this.slider.getModel().addChangeListener(this);
		
		ImageIcon icon = null;
		try {
			icon = new ImageIcon(getClass().getResource("/copy20x20.png"));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		JButton setParameters;
		if (icon != null) {
			setParameters = new JButton(icon);
		} else {
			setParameters = new JButton("Copy");
		}
		setParameters.setToolTipText("Copy the currently shown activity levels as initial activity levels in the model");
		setParameters.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//set the initial activity levels of the reactants in the network as they are in this point of the simulation ("this point" = where the slider is currently)
				double t = getSliderTime();
				CyAttributes nodeAttributes = Cytoscape.getNodeAttributes();
				int nLevels = result.getNumberOfLevels();
				for (String r : result.getReactantIds()) {
					if (model.getReactant(r) == null) continue;
					final String id = model.getReactant(r).get(Model.Properties.REACTANT_NAME).as(String.class);
					//System.err.println("R id = " + id);
					final double level = result.getConcentration(r, t) / nLevels * model.getReactant(r).get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class); //We also rescale the value to the correct number of levels of each node
					nodeAttributes.setAttribute(id, Model.Properties.INITIAL_LEVEL, (int)Math.round(level));
				}
			}
		});
		final XmlConfiguration configuration = InatBackend.get().configuration();
		String areWeTheDeveloperStr = configuration.get(XmlConfiguration.DEVELOPER_KEY);
		boolean areWeTheDeveloper = false;
		if (areWeTheDeveloperStr != null) {
			areWeTheDeveloper = Boolean.parseBoolean(areWeTheDeveloperStr);
		}
		if (areWeTheDeveloper) {
			sliderPanel.add(setParameters, BorderLayout.WEST);
		}
		
		sliderPanel.add(this.slider, BorderLayout.CENTER);
		
		this.add(sliderPanel, BorderLayout.SOUTH);
		
		g = new Graph();
		//We map reactant IDs to their corresponding aliases (canonical names, i.e., the names displayed to the user in the network window), so that
		//we will be able to use graph series names consistent with what the user has chosen.
		Map<String, String> seriesNameMapping = new HashMap<String, String>();
		Vector<String> filteredSeriesNames = new Vector<String>(); //profit from the cycle for the series mapping to create a filter for the series to be actually plotted
		for (String r : result.getReactantIds()) {
			String name = null;
			String stdDevReactantName = null;
			if (model.getReactant(r) != null) { //we can also refer to a name not present in the reactant collection
				name = model.getReactant(r).get(Model.Properties.ALIAS).as(String.class); //if an alias is set, we prefer it
				if (name == null) {
					name = model.getReactant(r).get(Model.Properties.REACTANT_NAME).as(String.class);
				}
			} else if (r.toLowerCase().contains(ResultAverager.STD_DEV.toLowerCase())) {
				stdDevReactantName = r.substring(0, r.lastIndexOf(ResultAverager.STD_DEV));
				if (model.getReactant(stdDevReactantName).get(Model.Properties.ALIAS).as(String.class) != null) {
					name = model.getReactant(stdDevReactantName).get(Model.Properties.ALIAS).as(String.class) + ResultAverager.STD_DEV;
				} else {
					name = r; //in this case, I simply don't know what we are talking about =)
				}
			} else if (r.toLowerCase().contains(ResultAverager.OVERLAY_NAME.toLowerCase())) {
				stdDevReactantName = r.substring(0, r.lastIndexOf(ResultAverager.OVERLAY_NAME));
				if (model.getReactant(stdDevReactantName).get(Model.Properties.ALIAS).as(String.class) != null) {
					name = model.getReactant(stdDevReactantName).get(Model.Properties.ALIAS).as(String.class); // + r.substring(r.lastIndexOf(ResultAverager.OVERLAY_NAME));
					seriesNameMapping.put(stdDevReactantName, name);
				} else {
					name = r; //boh
				}
			}
			if ((!r.toLowerCase().contains(ResultAverager.STD_DEV.toLowerCase()) && !r.toLowerCase().contains(ResultAverager.OVERLAY_NAME.toLowerCase()) 
						&& model.getReactant(r) != null && model.getReactant(r).get(Model.Properties.PLOTTED).as(Boolean.class))
				|| ((r.toLowerCase().contains(ResultAverager.STD_DEV.toLowerCase()) || r.toLowerCase().contains(ResultAverager.OVERLAY_NAME.toLowerCase())) 
						&& model.getReactant(stdDevReactantName) != null && model.getReactant(stdDevReactantName).get(Model.Properties.PLOTTED).as(Boolean.class))) {
				
				filteredSeriesNames.add(r);
			}
			seriesNameMapping.put(r, name);
		}
		g.parseLevelResult(result.filter(filteredSeriesNames), seriesNameMapping, scale); //Add all series to the graph, using the mapping we built here to "translate" the names into the user-defined ones.
		g.setXSeriesName("Time (min)");
		g.setYLabel("Protein activity (a. u.)");
		
		if (!model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).isNull()) { //if we find a maximum value for activity levels, we declare it to the graph, so that other added graphs (such as experimental data) will be automatically rescaled to match us
			int nLevels = model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class);
			g.declareMaxYValue(nLevels);
			double maxTime = scale * result.getTimeIndices().get(result.getTimeIndices().size()-1);
//			int maxTimeInt = (int)maxTime;
//			if (maxTimeInt < maxTime) { //If we get 6.2 as last time on the trace, we show up to 7, so that we let the user see also the end of the trace
//				maxTimeInt++;
//			}
			g.setDrawArea(0, maxTime, 0, nLevels); //This is done because the graph automatically computes the area to be shown based on minimum and maximum values for X and Y, including StdDev. So, if the StdDev of a particular series (which represents an average) in a particular point is larger that the value of that series in that point, the minimum y value would be negative. As this is not very nice to see, I decided that we will recenter the graph to more strict bounds instead.
														//Also, if the maximum value reached during the simulation is not the maximum activity level, the graph does not loook nice
		}
		this.add(g, BorderLayout.CENTER);
		g.addGraphScaleListener(this);
	}

	

	private double getSliderTime() {
		return (1.0 * this.slider.getValue() / this.slider.getMaximum() * (this.maxValueOnGraph - this.minValueOnGraph) + this.minValueOnGraph) / this.scale; //1.0 * this.slider.getValue() / this.slider.getMaximum() * this.scaleForConcentration; //this.slider.getValue() / scale;
	}
	
	/**
	 * When the user moves the time slider, we update the activity ratio (SHOWN_LEVEL) of
	 * all nodes in the network window, so that, thanks to the continuous Visual Mapping
	 * defined when the interface is augmented (see AugmentAction), different colors will
	 * show different activity levels. 
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		final double t = getSliderTime();
		
		double graphWidth = this.maxValueOnGraph - this.minValueOnGraph;
		g.setRedLinePosition(1.0 * this.slider.getValue() / this.slider.getMaximum() * graphWidth);
		
		//g.setRedLinePosition(this.slider.getValue() - this.slider.getMinimum());
		
		CyAttributes nodeAttributes = Cytoscape.getNodeAttributes();
		final int levels = this.model.getProperties().get(Model.Properties.NUMBER_OF_LEVELS).as(Integer.class); //at this point, all levels have already been rescaled to the maximum (= the number of levels of the model), so we use it as a reference for the number of levels to show on the network nodes 
		for (String r : this.result.getReactantIds()) {
			if (this.model.getReactant(r) == null) continue;
			final String id = this.model.getReactant(r).get(Model.Properties.REACTANT_NAME).as(String.class);
			final double level = this.result.getConcentration(r, t);
			nodeAttributes.setAttribute(id, Model.Properties.SHOWN_LEVEL, level / levels);
		}

		Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);
		
		/* Unfortunately, it is not possible to set a background other than simply java.awt.Color... So, for the moment I have no idea on what to do in order to paint in the background image (for example, to add a legend for colors/shapes, or a counter for the current time, to better reflect the scale in the slider)
		BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g1 = img.createGraphics();
		g1.setPaint(java.awt.Color.RED.darker());
		g1.setBackground(java.awt.Color.WHITE);
		//g1.fill(new Rectangle2D.Float(0, 0, 1000, 1000));
		g1.drawString("aaaa", 50, 50);
		Cytoscape.getCurrentNetworkView().setBackgroundPaint(new TexturePaint(img, new Rectangle2D.Float(0, 0, 100, 100)));
		//new TexturePaint(img, new Rectangle2D.Float(0, 0, 1000, 1000))); //java.awt.Color.RED.darker()); 
		 */

		Cytoscape.getCurrentNetworkView().updateView();
	}
	
	/**
	 * When the graph scale changes (due to a zoom or axis range change),
	 * we change the min and max of the JSlider to adapt it to the graph area.
	 */
	@Override
	public void scaleChanged(Scale newScale) {
		this.minValueOnGraph = newScale.getMinX();
		this.maxValueOnGraph = newScale.getMaxX();
		this.scaleForConcentration = this.maxValueOnGraph / this.scale;
		
		Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
		DecimalFormat formatter = new DecimalFormat("0.###");
		int nLabels = 10;
		double graphWidth = this.maxValueOnGraph - this.minValueOnGraph;
		for (int i=0;i<=nLabels;i++) {
			double val = this.minValueOnGraph + 1.0 * i / nLabels * graphWidth;
			String valStr = formatter.format(val);
			labels.put((int)Math.round(1.0 * i / nLabels * slider.getMaximum()), new JLabel(valStr));
		}
		this.slider.setLabelTable(labels);
		stateChanged(null); //Note that we don't use that parameter, so I can also call the function with null
	}
	
	public void saveSimulationData(File outputFile) {
		try {
			FileOutputStream fOut = new FileOutputStream(outputFile);
			ObjectOutputStream out = new ObjectOutputStream(fOut);
			out.writeObject(model);
			out.writeObject(result);
			out.writeDouble(new Double(scale));
			out.writeObject(title);
			out.close();
			fOut.close();
		} catch (Exception ex) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(baos);
			ex.printStackTrace(ps);
			JOptionPane.showMessageDialog(Cytoscape.getDesktop(), baos.toString(), "Error: " + ex, JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public static Quadruple<Model, SimpleLevelResult, Double, String> loadSimulationData(File inputFile) {
		try {
			FileInputStream fIn = new FileInputStream(inputFile);
			ObjectInputStream in = new ObjectInputStream(fIn);
			Model model = (Model)in.readObject();
			SimpleLevelResult result = (SimpleLevelResult)in.readObject();
			Double scale = in.readDouble();
			String title = in.readObject().toString();
			in.close();
			fIn.close();
			return new Quadruple<Model, SimpleLevelResult, Double, String>(model, result, scale, title);
		} catch (Exception ex) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(baos);
			ex.printStackTrace(ps);
			JOptionPane.showMessageDialog(Cytoscape.getDesktop(), baos.toString(), "Error: " + ex, JOptionPane.ERROR_MESSAGE);
		}
		return null;
	}
	
	
	/**
	 * Add the InatResultPanel to the given Cytoscape panel
	 * @param cytoPanel
	 */
	public void addToPanel(final CytoPanel cytoPanel) {
		container = new JPanel(new BorderLayout(2, 2));
		container.add(this, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new FlowLayout()); //new GridLayout(2, 2, 2, 2));
		
		JButton close = new JButton(new AbstractAction("Close") {
			private static final long serialVersionUID = 4327349309742276633L;

			@Override
			public void actionPerformed(ActionEvent e) {
				cytoPanel.remove(container);
				allExistingPanels.remove(this);
			}
		});
		
		JButton save = new JButton(new AbstractAction("Save simulation data...") {
			private static final long serialVersionUID = -2492923184151760584L;

			@Override
			public void actionPerformed(ActionEvent e) {
				String fileName = FileUtils.save(".sim", "ANIMO simulation data", Cytoscape.getDesktop());
				saveSimulationData(new File(fileName));
			}
		});
		
		JButton changeTitle = new JButton(new AbstractAction("Change title") {
			private static final long serialVersionUID = 7093059357198172376L;

			@Override
			public void actionPerformed(ActionEvent e) {
				String newTitle = JOptionPane.showInputDialog(Cytoscape.getDesktop(), "Please give a new title", title);
				if (newTitle == null) {
					return;
				}
				setTitle(newTitle);
			}
		});
		
		differenceButton = new JButton(START_DIFFERENCE);
		differenceButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				differenceButtonPressed(differenceButton.getText());
			}
		});
		
		buttons.add(changeTitle);
		buttons.add(differenceButton);
		if (!isDifference) { //The differences are not saved (for the moment)
			buttons.add(save);
		}
		buttons.add(close);
		container.add(buttons, BorderLayout.NORTH);
		
		
		cytoPanel.setState(CytoPanelState.DOCK);
		fCytoPanel = (CytoPanelImp)cytoPanel;
		cytoPanel.addCytoPanelListener(this);
		
		cytoPanel.add(this.title, container);
		this.myIndex = cytoPanel.getCytoPanelComponentCount() - 1;
		cytoPanel.setSelectedIndex(this.myIndex);
		resetDivider();
	}
	
	public void setTitle(String newTitle) {
		title = newTitle;
		int currentIdx = fCytoPanel.getSelectedIndex();
		fCytoPanel.remove(currentIdx);
		fCytoPanel.add(title, null, container, newTitle, currentIdx);
		fCytoPanel.setSelectedIndex(currentIdx);
		resetDivider();
	}
	
	public String getTitle() {
		return this.title;
	}
	
	private void differenceButtonPressed(String caption) {
		if (caption.equals(START_DIFFERENCE)) {
			differenceButton.setText(CANCEL_DIFFERENCE);
			differenceWith = this;
			for (InatResultPanel others : allExistingPanels) {
				if (others == this) continue;
				others.differenceButton.setText(END_DIFFERENCE);
			}
		} else if (caption.equals(END_DIFFERENCE)) {
			if (differenceWith != null) {
				SimpleLevelResult diff = (SimpleLevelResult)this.result.difference(differenceWith.result);
				if (diff.isEmpty()) {
					JOptionPane.showMessageDialog(Cytoscape.getDesktop(), "Error: empty difference. Please contact the developers and send them the current model,\nwith a reference to which simulations were used for the difference.");
					return;
				}
				InatResultPanel newPanel = new InatResultPanel(differenceWith.model, diff, differenceWith.scale, differenceWith.title + " - " + this.title);
				newPanel.isDifference = true;
				double maxY = Math.max(this.g.getScale().getMaxY(), differenceWith.g.getScale().getMaxY());
				Scale scale = newPanel.g.getScale();
				newPanel.g.setDrawArea((int)scale.getMinX(), (int)scale.getMaxX(), (int)-maxY, (int)maxY); //(int)scale.getMaxY());
				newPanel.vizMapName = InatPlugin.TAB_NAME + "_Diff";
				if (fCytoPanel != null) {
					newPanel.addToPanel(fCytoPanel);
				}
				for (InatResultPanel panel : allExistingPanels) {
					panel.differenceButton.setText(START_DIFFERENCE);
				}
			}
		} else if (caption.equals(CANCEL_DIFFERENCE)) {
			differenceWith = null;
			for (InatResultPanel panel : allExistingPanels) {
				panel.differenceButton.setText(START_DIFFERENCE);
			}
		}
	}
	
	private int lastWidth;
	private void resetDivider() {
		JSplitPane par = (JSplitPane)(fCytoPanel.getParent());
		CyNetworkView p2 = Cytoscape.getCurrentNetworkView();
		int width = 0;
		if (p2 != null) {
			try {
				width += Cytoscape.getDesktop().getNetworkViewManager().getInternalFrame(p2).getWidth(); //There may be null pointers here (e.g. when you have already closed all network windows and are attempting to close a results tab)
			} catch (Exception ex) {
				width += 0;
			}
		}
		if (width == 0) {
			width = lastWidth;
		}
		par.setDividerLocation(width);
		lastWidth = width;
	}
	
	private CytoPanelImp fCytoPanel;
	@Override
	public void onComponentAdded(int arg0) {
		resetDivider();
	}

	@Override
	public void onComponentRemoved(int arg0) {
		resetDivider();
	}

	@Override
	public void onComponentSelected(int arg0) {
		if (arg0 == myIndex) {
			VisualMappingManager vizMap = Cytoscape.getVisualMappingManager();
			if (vizMapName != null) {
				//CalculatorCatalog visualStyleCatalog = vizMap.getCalculatorCatalog();
				vizMap.setVisualStyle(vizMapName);
			} else {
				vizMap.setVisualStyle("default");
			}
			stateChanged(null); //We must do this to make sure that the visual mapping is correctly applied, otherwise Cytoscape will not notice a possibly different scale on the mapping values (e.g. [-1.0, 1.0] instead of [0.0, 1.0])
		}
		resetDivider();
	}

	@Override
	public void onStateChange(CytoPanelState arg0) {
		resetDivider();
	}
}
