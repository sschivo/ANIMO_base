package inat.cytoscape;

import giny.model.Edge;
import giny.model.Node;
import giny.view.EdgeView;
import giny.view.NodeView;
import inat.model.Model;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cytoscape.CyEdge;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.view.CyNetworkView;
import cytoscape.view.CytoscapeDesktop;
import cytoscape.visual.ArrowShape;
import cytoscape.visual.EdgeAppearanceCalculator;
import cytoscape.visual.GlobalAppearanceCalculator;
import cytoscape.visual.NodeAppearanceCalculator;
import cytoscape.visual.NodeShape;
import cytoscape.visual.VisualMappingManager;
import cytoscape.visual.VisualPropertyType;
import cytoscape.visual.VisualStyle;
import cytoscape.visual.VisualPropertyDependency.Definition;
import cytoscape.visual.calculators.BasicCalculator;
import cytoscape.visual.calculators.Calculator;
import cytoscape.visual.calculators.GenericNodeCustomGraphicCalculator;
import cytoscape.visual.mappings.BoundaryRangeValues;
import cytoscape.visual.mappings.ContinuousMapping;
import cytoscape.visual.mappings.DiscreteMapping;
import cytoscape.visual.mappings.LinearNumberToColorInterpolator;
import cytoscape.visual.mappings.PassThroughMapping;
import cytoscape.visual.mappings.continuous.ContinuousMappingPoint;
import ding.view.EdgeContextMenuListener;
import ding.view.NodeContextMenuListener;

public class INATPropertyChangeListener implements PropertyChangeListener {
	
	private int currentEdgeNumber = -1, currentNodeNumber = -1;
	private Object[] edgesArray = null;
	private ColorsLegend legendColors;
	private ShapesLegend legendShapes;

	public INATPropertyChangeListener(ColorsLegend legendColors, ShapesLegend legendShapes) {
		this.legendColors = legendColors;
		this.legendShapes = legendShapes;
	}

	/**
	 * Add the right-click menus for nodes (reactants) and edges (reactions)
	 * Menus are: Edit, Enable/disable and Plotted/hidden (last one only for nodes)
	 */
	private void addMenus() {
		Cytoscape.getCurrentNetworkView().addNodeContextMenuListener(new NodeContextMenuListener() {
			
			@Override
			public void addNodeContextMenuItems(final NodeView nodeView, JPopupMenu menu) {
				if (menu != null) {
					menu.add(new AbstractAction("[ANIMO] Edit reactant...") {
						private static final long serialVersionUID = 6233177439944893232L;

						@Override
						public void actionPerformed(ActionEvent e) {
							NodeDialog dialog = new NodeDialog(nodeView.getNode());
							dialog.pack();
							dialog.setLocationRelativeTo(Cytoscape.getDesktop());
							dialog.setVisible(true);
						}
					});
					
					menu.add(new AbstractAction("[ANIMO] Enable/disable") {
						private static final long serialVersionUID = 2579035100338148305L;

						@SuppressWarnings("unchecked")
						@Override
						public void actionPerformed(ActionEvent e) {
							//CyNetwork network = Cytoscape.getCurrentNetwork();
							CyNetworkView view = Cytoscape.getCurrentNetworkView();
							CyAttributes nodeAttr = Cytoscape.getNodeAttributes();
							//CyAttributes edgeAttr = Cytoscape.getEdgeAttributes();
							for (Iterator i = view.getSelectedNodes().iterator(); i.hasNext(); ) {
								NodeView nView = (NodeView)i.next();
								CyNode node = (CyNode)nView.getNode();
								boolean status;
								if (!nodeAttr.hasAttribute(node.getIdentifier(), Model.Properties.ENABLED)) {
									status = false;
								} else {
									status = !nodeAttr.getBooleanAttribute(node.getIdentifier(), Model.Properties.ENABLED);
								}
								nodeAttr.setAttribute(node.getIdentifier(), Model.Properties.ENABLED, status);
								/*int [] adjacentEdges = network.getAdjacentEdgeIndicesArray(node.getRootGraphIndex(), true, true, true);
								for (int edgeIdx : adjacentEdges) {
									CyEdge edge = (CyEdge)view.getEdgeView(edgeIdx).getEdge();
									edgeAttr.setAttribute(edge.getIdentifier(), Model.Properties.ENABLED, status);
								}*/
							}
							if (view.getSelectedNodes().isEmpty()) { //if the user wanted to change only one node (i.e. right click on a node without first selecting one), here we go
								Node node = nodeView.getNode();
								boolean status;
								if (!nodeAttr.hasAttribute(node.getIdentifier(), Model.Properties.ENABLED)) {
									status = false;
								} else {
									status = !nodeAttr.getBooleanAttribute(node.getIdentifier(), Model.Properties.ENABLED);
								}
								nodeAttr.setAttribute(node.getIdentifier(), Model.Properties.ENABLED, status);
								/*int [] adjacentEdges = network.getAdjacentEdgeIndicesArray(node.getRootGraphIndex(), true, true, true);
								for (int edgeIdx : adjacentEdges) {
									CyEdge edge = (CyEdge)view.getEdgeView(edgeIdx).getEdge();
									edgeAttr.setAttribute(edge.getIdentifier(), Model.Properties.ENABLED, status);
								}*/
							}
							/*//In order to keep the model consistent, we disable all edges coming from (or going into) disabled nodes
							for (int i : network.getEdgeIndicesArray()) {
								CyEdge edge = (CyEdge)view.getEdgeView(i).getEdge();
								CyNode source = (CyNode)edge.getSource(),
									   target = (CyNode)edge.getTarget();
								if ((nodeAttr.hasAttribute(source.getIdentifier(), Model.Properties.ENABLED) && !nodeAttr.getBooleanAttribute(source.getIdentifier(), Model.Properties.ENABLED))
									|| (nodeAttr.hasAttribute(target.getIdentifier(), Model.Properties.ENABLED) && !nodeAttr.getBooleanAttribute(target.getIdentifier(), Model.Properties.ENABLED))) {
									edgeAttr.setAttribute(edge.getIdentifier(), Model.Properties.ENABLED, false);
								}
							}*/
							Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);
						}
					});
					
					menu.add(new AbstractAction("[ANIMO] Plot/hide") {

						private static final long serialVersionUID = -4264583436246699628L;

						@SuppressWarnings("unchecked")
						@Override
						public void actionPerformed(ActionEvent e) {
							CyNetworkView view = Cytoscape.getCurrentNetworkView();
							CyAttributes nodeAttr = Cytoscape.getNodeAttributes();
							for (Iterator i = view.getSelectedNodes().iterator(); i.hasNext(); ) {
								NodeView nView = (NodeView)i.next();
								CyNode node = (CyNode)nView.getNode();
								boolean status;
								if (!nodeAttr.hasAttribute(node.getIdentifier(), Model.Properties.PLOTTED)) {
									status = false;
								} else {
									status = !nodeAttr.getBooleanAttribute(node.getIdentifier(), Model.Properties.PLOTTED);
								}
								nodeAttr.setAttribute(node.getIdentifier(), Model.Properties.PLOTTED, status);
							}
							if (view.getSelectedNodes().isEmpty()) { //if the user wanted to change only one node (i.e. right click on a node without first selecting one), here we go
								Node node = nodeView.getNode();
								boolean status;
								if (!nodeAttr.hasAttribute(node.getIdentifier(), Model.Properties.PLOTTED)) {
									status = false;
								} else {
									status = !nodeAttr.getBooleanAttribute(node.getIdentifier(), Model.Properties.PLOTTED);
								}
								nodeAttr.setAttribute(node.getIdentifier(), Model.Properties.PLOTTED, status);
							}
							Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);
						}
						
					});
				}
			}
		});
		Cytoscape.getCurrentNetworkView().addEdgeContextMenuListener(new EdgeContextMenuListener() {
			
			@Override
			public void addEdgeContextMenuItems(final EdgeView edgeView, JPopupMenu menu) {
				if (menu != null) {
					menu.add(new AbstractAction("[ANIMO] Edit reaction...") {
						private static final long serialVersionUID = -5725775462053708399L;

						@Override
						public void actionPerformed(ActionEvent e) {
							EdgeDialog dialog = new EdgeDialog(edgeView.getEdge());
							dialog.pack();
							dialog.setLocationRelativeTo(Cytoscape.getDesktop());
							dialog.setVisible(true);
						}
					});
					
					menu.add(new AbstractAction("[ANIMO] Enable/disable") {

						private static final long serialVersionUID = -1078261166495178010L;

						@SuppressWarnings("unchecked")
						@Override
						public void actionPerformed(ActionEvent e) {
							CyNetworkView view = Cytoscape.getCurrentNetworkView();
							CyAttributes edgeAttr = Cytoscape.getEdgeAttributes();
							for (Iterator i = view.getSelectedEdges().iterator(); i.hasNext(); ) {
								EdgeView eView = (EdgeView)i.next();
								CyEdge edge = (CyEdge)eView.getEdge();
								boolean status;
								if (!edgeAttr.hasAttribute(edge.getIdentifier(), Model.Properties.ENABLED)) {
									status = false;
								} else {
									status = !edgeAttr.getBooleanAttribute(edge.getIdentifier(), Model.Properties.ENABLED);
								}
								edgeAttr.setAttribute(edge.getIdentifier(), Model.Properties.ENABLED, status);
							}
							if (view.getSelectedEdges().isEmpty()) { //if the user wanted to change only one edge (i.e. right click on an edge without first selecting one), here we go
								Edge edge = edgeView.getEdge();
								boolean status;
								if (!edgeAttr.hasAttribute(edge.getIdentifier(), Model.Properties.ENABLED)) {
									status = false;
								} else {
									status = !edgeAttr.getBooleanAttribute(edge.getIdentifier(), Model.Properties.ENABLED);
								}
								edgeAttr.setAttribute(edge.getIdentifier(), Model.Properties.ENABLED, status);
							}
							Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);
						}
						
					});
				}
			}
		});
	}
	
	
	/**
	 * Add the visual mappings to visually enhance the user interface (nodes colors, shapes, arrows etc)
	 */
	private void addVisualMappings() {
		VisualMappingManager vizMap = Cytoscape.getVisualMappingManager();
		VisualStyle visualStyle = vizMap.getVisualStyle();
		NodeAppearanceCalculator nac = visualStyle.getNodeAppearanceCalculator();
		EdgeAppearanceCalculator eac = visualStyle.getEdgeAppearanceCalculator();
		GlobalAppearanceCalculator gac = visualStyle.getGlobalAppearanceCalculator();
		visualStyle.getDependency().set(Definition.NODE_SIZE_LOCKED, false);
		
		gac.setDefaultBackgroundColor(Color.WHITE);
		Color selectionColor = new Color(102, 102, 255);
		gac.setDefaultNodeSelectionColor(selectionColor);
		gac.setDefaultEdgeSelectionColor(selectionColor);

		/*DiscreteMapping mapping = new DiscreteMapping(Color.class, Model.Properties.ENABLED);
		mapping.putMapValue(false, Color.BLACK);
		mapping.putMapValue(true, Color.WHITE);
		Calculator calco = new GenericNodeCustomGraphicCalculator("Mapping for enabled and disabled nodes (label color)", mapping, VisualPropertyType.NODE_LABEL_COLOR);
		nac.setCalculator(calco);*/
		
		DiscreteMapping mapping = new DiscreteMapping(Float.class, Model.Properties.ENABLED);
		mapping.putMapValue(false, 50.0f);
		mapping.putMapValue(true, 255.0f);
		Calculator calco = new GenericNodeCustomGraphicCalculator("Mapping for enabled and disabled nodes (node opacity)", mapping, VisualPropertyType.NODE_OPACITY);
		nac.setCalculator(calco);
		
		mapping = new DiscreteMapping(Float.class, Model.Properties.ENABLED);
		mapping.putMapValue(false, 60.0f);
		mapping.putMapValue(true, 255.0f);
		calco = new GenericNodeCustomGraphicCalculator("Mapping for enabled and disabled nodes (node border opacity)", mapping, VisualPropertyType.NODE_BORDER_OPACITY);
		nac.setCalculator(calco);
		
		mapping = new DiscreteMapping(Float.class, Model.Properties.ENABLED);
		mapping.putMapValue(false, 50.0f);
		mapping.putMapValue(true, 255.0f);
		calco = new BasicCalculator("Mapping for enabled and disabled edges (edge opacity)", mapping, VisualPropertyType.EDGE_OPACITY);
		eac.setCalculator(calco);
		
		calco = new BasicCalculator("Mapping for enabled and disabled edges (edge target arrow opacity)", mapping, VisualPropertyType.EDGE_TGTARROW_OPACITY);
		eac.setCalculator(calco);

		mapping = new DiscreteMapping(Float.class, Model.Properties.ENABLED);
		mapping.putMapValue(false, 3.0f);
		mapping.putMapValue(true, 6.0f);
		calco = new GenericNodeCustomGraphicCalculator("Mapping to show thicker borders for enabled nodes", mapping, VisualPropertyType.NODE_LINE_WIDTH);
		nac.setCalculator(calco);
		
		mapping = new DiscreteMapping(Color.class, Model.Properties.PLOTTED);
		mapping.putMapValue(false, Color.DARK_GRAY);
		mapping.putMapValue(true, Color.BLUE);
		calco = new GenericNodeCustomGraphicCalculator("Mapping to highlighting plotted nodes with a blue border", mapping, VisualPropertyType.NODE_BORDER_COLOR);
		nac.setCalculator(calco);
		
		mapping = new DiscreteMapping(ArrowShape.class, Model.Properties.INCREMENT);
		mapping.putMapValue(-1, ArrowShape.T);
		mapping.putMapValue(1, ArrowShape.ARROW);
		calco = new BasicCalculator("Mapping for arrow target shape", mapping, VisualPropertyType.EDGE_TGTARROW_SHAPE);
		eac.setCalculator(calco);
		
		mapping = new DiscreteMapping(NodeShape.class, Model.Properties.MOLECULE_TYPE);
		mapping.putMapValue(Model.Properties.TYPE_CYTOKINE, NodeShape.RECT);
		mapping.putMapValue(Model.Properties.TYPE_RECEPTOR, NodeShape.ELLIPSE);
		mapping.putMapValue(Model.Properties.TYPE_KINASE, NodeShape.ELLIPSE);
		mapping.putMapValue(Model.Properties.TYPE_PHOSPHATASE, NodeShape.DIAMOND);
		mapping.putMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR, NodeShape.ELLIPSE);
		mapping.putMapValue(Model.Properties.TYPE_OTHER, NodeShape.RECT);
		//I purposedly omit TYPE_OTHER, because I want it to stay on the default setting
		calco = new BasicCalculator("Mapping node shape from molecule type", mapping, VisualPropertyType.NODE_SHAPE);
		nac.setCalculator(calco);
		final DiscreteMapping shapesMap = mapping;
		
		/*mapping = new DiscreteMapping(Float.class, Model.Properties.MOLECULE_TYPE);
		mapping.putMapValue(Model.Properties.TYPE_CYTOKINE, 60.0f);
		mapping.putMapValue(Model.Properties.TYPE_RECEPTOR, 50.0f);
		mapping.putMapValue(Model.Properties.TYPE_KINASE, 55.0f);
		mapping.putMapValue(Model.Properties.TYPE_PHOSPHATASE, 55.0f);
		mapping.putMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR, 50.0f);
		calco = new BasicCalculator("Mapping node shape size from molecule type", mapping, VisualPropertyType.NODE_SIZE);
		nac.setCalculator(calco);*/
		
		mapping = new DiscreteMapping(Float.class, Model.Properties.MOLECULE_TYPE);
		mapping.putMapValue(Model.Properties.TYPE_CYTOKINE, 50.0f);
		mapping.putMapValue(Model.Properties.TYPE_RECEPTOR, 45.0f);
		mapping.putMapValue(Model.Properties.TYPE_KINASE, 55.0f);
		mapping.putMapValue(Model.Properties.TYPE_PHOSPHATASE, 55.0f);
		mapping.putMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR, 60.0f);
		mapping.putMapValue(Model.Properties.TYPE_OTHER, 60.0f);
		calco = new BasicCalculator("Mapping node shape width from molecule type", mapping, VisualPropertyType.NODE_WIDTH);
		nac.setCalculator(calco);
		final DiscreteMapping widthsMap = mapping;
		
		mapping = new DiscreteMapping(Float.class, Model.Properties.MOLECULE_TYPE);
		mapping.putMapValue(Model.Properties.TYPE_CYTOKINE, 50.0f);
		mapping.putMapValue(Model.Properties.TYPE_RECEPTOR, 65.0f);
		mapping.putMapValue(Model.Properties.TYPE_KINASE, 55.0f);
		mapping.putMapValue(Model.Properties.TYPE_PHOSPHATASE, 55.0f);
		mapping.putMapValue(Model.Properties.TYPE_TRANSCRIPTION_FACTOR, 40.0f);
		mapping.putMapValue(Model.Properties.TYPE_OTHER, 35.0f);
		calco = new BasicCalculator("Mapping node shape height from molecule type", mapping, VisualPropertyType.NODE_HEIGHT);
		nac.setCalculator(calco);
		final DiscreteMapping heightsMap = mapping;
		shapesMap.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				legendShapes.setParameters(shapesMap, widthsMap, heightsMap);
			}
		});
		widthsMap.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				legendShapes.setParameters(shapesMap, widthsMap, heightsMap);
			}
		});
		heightsMap.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				legendShapes.setParameters(shapesMap, widthsMap, heightsMap);
			}
		});
		
		/*mapping = new DiscreteMapping(Color.class, Model.Properties.MOLECULE_TYPE);
		mapping.putMapValue(Model.Properties.TYPE_RECEPTOR, Color.BLACK);
		calco = new BasicCalculator("Mapping node label color from molecule type", mapping, VisualPropertyType.NODE_LABEL_COLOR);
		nac.setCalculator(calco);*/
		
		PassThroughMapping mp = new PassThroughMapping(String.class, Model.Properties.CANONICAL_NAME);
		calco = new BasicCalculator("Mapping for node label", mp, VisualPropertyType.NODE_LABEL);
		nac.setCalculator(calco);
		
		final ContinuousMapping mc = new ContinuousMapping(Color.class, Model.Properties.SHOWN_LEVEL);
		Color lowerBound = new Color(204, 0, 0), //Color.RED.darker(),
		  middleBound = new Color(255, 204, 0), //Color.YELLOW.darker(),
		  upperBound = new Color(0, 204, 0); //Color.GREEN.darker();
		mc.addPoint(0.0, new BoundaryRangeValues(lowerBound, lowerBound, lowerBound));
		mc.addPoint(0.5, new BoundaryRangeValues(middleBound, middleBound, middleBound));
		mc.addPoint(1.0, new BoundaryRangeValues(upperBound, upperBound, upperBound));
		/*Color lowerBound = Color.BLACK,
			  midLowerBound = new Color(204, 0, 0),
			  midUpperBound = new Color(255, 255, 0),
			  upperBound = Color.WHITE;
		mc.addPoint(0.0, new BoundaryRangeValues(lowerBound, lowerBound, lowerBound));
		mc.addPoint(0.33333333, new BoundaryRangeValues(midLowerBound, midLowerBound, midLowerBound));
		mc.addPoint(0.66666666, new BoundaryRangeValues(midUpperBound, midUpperBound, midUpperBound));
		mc.addPoint(1.0, new BoundaryRangeValues(upperBound, upperBound, upperBound));*/
		/*Color lowerBound = new Color(204, 0, 0),
			  upperBound = new Color(255, 204, 0);
		mc.addPoint(0.0, new BoundaryRangeValues(lowerBound, lowerBound, lowerBound));
		mc.addPoint(1.0, new BoundaryRangeValues(upperBound, upperBound, upperBound));*/
		mc.setInterpolator(new LinearNumberToColorInterpolator());
		calco = new GenericNodeCustomGraphicCalculator("Mapping for the current activity level of nodes", mc, VisualPropertyType.NODE_FILL_COLOR);
		nac.setCalculator(calco);
		mc.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				List<ContinuousMappingPoint> points = mc.getAllPoints();
				float fractions[] = new float[points.size()];
				Color colors[] = new Color[points.size()];
				
				int i = 0;
				for (ContinuousMappingPoint point : points) {
					fractions[fractions.length - 1 - i] = 1 - (float)point.getValue().doubleValue();
					colors[colors.length - 1 - i] = (Color)point.getRange().equalValue;
					i++;
				}
				legendColors.setParameters(fractions, colors);
			}
		});
		
		
		//legendColors.setParameters(new float[]{1 - 1, 1 - 2/3.0f, 1 - 1/3.0f, 1 - 0}, new Color[]{Color.WHITE, new Color(255, 255, 0), new Color(204, 0, 0), Color.BLACK});
		legendColors.setParameters(new float[]{1 - 1, 1 - 0.5f, 1 - 0/*1 - 1, 1 - 0*/}, new Color[]{new Color(0, 204, 0), new Color(255, 204, 0), new Color(204, 0, 0)/*new Color(255, 204, 0), new Color(204, 0, 0)*/});
		
		legendShapes.setParameters(shapesMap, widthsMap, heightsMap);
		
		VisualPropertyType.NODE_BORDER_COLOR.setDefault(visualStyle, Color.BLUE);//Color.DARK_GRAY);
		VisualPropertyType.NODE_FILL_COLOR.setDefault(visualStyle, Color.RED);
		VisualPropertyType.NODE_LABEL_COLOR.setDefault(visualStyle, Color.BLACK); //new Color(102, 102, 255));//Color.WHITE);
		VisualPropertyType.NODE_FONT_SIZE.setDefault(visualStyle, 14);
		VisualPropertyType.NODE_LINE_WIDTH.setDefault(visualStyle, 6.0f);
		VisualPropertyType.NODE_SHAPE.setDefault(visualStyle, NodeShape.RECT);
		VisualPropertyType.NODE_SIZE.setDefault(visualStyle, 50.0f);
		VisualPropertyType.NODE_WIDTH.setDefault(visualStyle, 60.0f);
		VisualPropertyType.NODE_HEIGHT.setDefault(visualStyle, 35.0f);
		VisualPropertyType.EDGE_LINE_WIDTH.setDefault(visualStyle, 4.0f);
		VisualPropertyType.EDGE_COLOR.setDefault(visualStyle, Color.BLACK);
		VisualPropertyType.EDGE_TGTARROW_COLOR.setDefault(visualStyle, Color.BLACK);
		
		//Recompute the activityRatio property for all nodes, to make sure that it exists
		CyNetwork network = Cytoscape.getCurrentNetwork();
		if (network != null) {
			CyAttributes nodeAttr = Cytoscape.getNodeAttributes(),
						 networkAttr = Cytoscape.getNetworkAttributes();
			for (int i : network.getNodeIndicesArray()) {
				Node n = network.getNode(i);
				double level = 0;
				int nLevels = 0;
				if (nodeAttr.hasAttribute(n.getIdentifier(), Model.Properties.INITIAL_LEVEL)) {
					level = nodeAttr.getIntegerAttribute(n.getIdentifier(), Model.Properties.INITIAL_LEVEL);
				} else {
					level = 0;
				}
				if (nodeAttr.hasAttribute(n.getIdentifier(), Model.Properties.NUMBER_OF_LEVELS)) {
					nLevels = nodeAttr.getIntegerAttribute(n.getIdentifier(), Model.Properties.NUMBER_OF_LEVELS);
				} else if (networkAttr.hasAttribute(network.getIdentifier(), Model.Properties.NUMBER_OF_LEVELS)) {
					nLevels = networkAttr.getIntegerAttribute(network.getIdentifier(), Model.Properties.NUMBER_OF_LEVELS);
				} else {
					nLevels = 1;
				}
				nodeAttr.setAttribute(n.getIdentifier(), Model.Properties.SHOWN_LEVEL, level / nLevels);
			}
			
			Cytoscape.firePropertyChange(Cytoscape.ATTRIBUTES_CHANGED, null, null);
		}
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equalsIgnoreCase(CytoscapeDesktop.NETWORK_VIEW_CREATED)) {
			addVisualMappings();
			addMenus();
			CyNetwork network = Cytoscape.getCurrentNetwork();
			currentEdgeNumber = network.getEdgeCount();
			currentNodeNumber = network.getNodeCount();
			edgesArray = network.edgesList().toArray();
		}
		if (evt.getPropertyName().equalsIgnoreCase(Cytoscape.NETWORK_LOADED)) {
			//As there can be edges with intermediate curving points, make those points curved instead of angled (they look nicer)
			List edgeList = Cytoscape.getCurrentNetworkView().getEdgeViewsList();
			for (Iterator i = edgeList.listIterator();i.hasNext();) {
				EdgeView ev = (EdgeView)(i.next());
				ev.setLineType(EdgeView.CURVED_LINES);
			}
		}
		if (evt.getPropertyName().equalsIgnoreCase(Cytoscape.NETWORK_CREATED)) {
			//addVisualMappings();
		}
		if (evt.getPropertyName().equalsIgnoreCase(Cytoscape.NETWORK_MODIFIED)) {
			if (currentEdgeNumber != -1 && currentNodeNumber != -1) {
				CyNetwork network = Cytoscape.getCurrentNetwork();
				int newEdgeNumber = network.getEdgeCount(),
					newNodeNumber = network.getNodeCount();
				if (newEdgeNumber > currentEdgeNumber) {
					//JOptionPane.showMessageDialog(null, "Nuovo arco inserito");
					List oldEdges = new Vector(),
						 newEdges;
					for (Object o : edgesArray) {
						oldEdges.add(o);
					}
					newEdges = network.edgesList();
					CyEdge edge = null;
					for (Object o : newEdges) {
						if (!oldEdges.contains(o)) {
							edge = (CyEdge)o;
							break;
						}
					}
					if (edge != null) {
						Cytoscape.getCurrentNetworkView().getEdgeView(edge).setLineType(EdgeView.CURVED_LINES);
						EdgeDialog dialog = new EdgeDialog(edge);
						dialog.pack();
						dialog.setLocationRelativeTo(Cytoscape.getDesktop());
						dialog.setVisible(true);
					}
					edgesArray = newEdges.toArray();
				} else if (newEdgeNumber < currentEdgeNumber) {
					//JOptionPane.showMessageDialog(null, "Arco rimosso");
				}
				if (newNodeNumber > currentNodeNumber) {
					network.getSelectedNodes();
					//JOptionPane.showMessageDialog(null, "Nuovo nodo inserito");
					Set nodes = network.getSelectedNodes();
					Object[] nodesArray = nodes.toArray();
					for (Object o : nodesArray) {
						CyNode node = (CyNode)o;
						NodeDialog dialog = new NodeDialog(node);
						dialog.pack();
						dialog.setLocationRelativeTo(Cytoscape.getDesktop());
						dialog.setVisible(true);
					}
				} else if(newNodeNumber < currentNodeNumber) {
					//JOptionPane.showMessageDialog(null, "Nodo rimosso");
				}
				currentEdgeNumber = newEdgeNumber;
				currentNodeNumber = newNodeNumber;
			}
		}
		if (evt.getPropertyName().equalsIgnoreCase(Cytoscape.SAVE_VIZMAP_PROPS)) {
			
		}
	}

}
