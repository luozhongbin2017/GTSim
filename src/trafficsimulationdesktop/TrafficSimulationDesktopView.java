/*
 * TrafficSimulationDesktopView.java
 */

package trafficsimulationdesktop;

import java.awt.*;
//import java.util.logging.Level;
//import java.util.logging.Logger;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.WaypointPainter;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.Painter;
import org.jgrapht.graph.DirectedWeightedMultigraph;

//--------------------------------------created outside the class------
/**
 * The application's main frame.
 */
public class TrafficSimulationDesktopView extends FrameView implements Runnable {
    
    private WaypointPainter wpOverlay;
    private Painter textOverlay;
    private Painter lOverlay;
    private CompoundPainter compPainter;
    private boolean addIntersectionsFlag;
    private boolean addConnectionsFlag;
    private boolean navigateFlag;
    private boolean eraseFlag;
    private boolean isChanged;
    private String fileName = null;
    private File currDir = new File(System.getProperty("user.home"));

// Fields for Adding and Deleting Connections
    private boolean waypointSelectedFlag;
    private WaypointIntersection selectedWaypoint;
    
// The Maps for Map to Graph Mapping
    private Map<WaypointIntersection, Intersection> intMap;
    private Map<WaypointConnection, Connection> conMap;
    public BufferedWriter csv;

    DirectedWeightedMultigraph<Intersection,Connection> graph;                  // The Graph Object
    
// Selected Waypoint/Connection for update
    WaypointIntersection toBeUpdatedWaypoint;
    WaypointConnection toBeUpdatedConnection;
    
    String grpExtn = ".gph";                                                    // The Graph extension
    String odmExtn = ".odm";                                                    // The Origin-Destination matrix extension
    
// Redo and Undo Options
    ArrayList<ActionPerformed> actions;
    int actionIndex;
    
// Origin-destination
    ArrayList<Intersection> originList;
    ArrayList<Intersection> destinationList;
    double origDestMatrix[][];
    double defaultMatVal = 1200.00; // prev 200.00 was 3600.0;
    boolean inputObtained;

// Thread related variables
    private Thread thread = null;   
    private boolean running = false;
    public static int updateInterval = 50;
    public static double t;                                                     //Simulation Time
    public static boolean jrRelax = true;
    public static boolean jrGapacc1;
    public static boolean jrGapacc2;
    public static boolean noLaneChange = false;
    public static double qA = 2300;                     // Default qA value
    public static double qR = 900;                      // Default qR value
    public static double HOTbegin=0.5;                     //location on the segment or connection where HOT lane begins
//    public static double HOTend=11.5;                      //location on the segment or connection where HOT lane
    public static double HOTexit=10;
    public static double HOTexitend= 10.5;
    public static double LDbegin=9;                     //location on the segment or connection where HOT lane begins
    public static double LDend=9.5;                      //location on the segment or connection where HOT lane ends
//    public static double LD2begin=12;

    public static double HOTexitratio = 1;

    public static double TravelTime=0;                                 //total network travel time
    public static double RampTT=0;
    public static final int Seed = 123456789;
    public static Random r = new Random(Seed);
    public static final int vehGenSeed = 123456789;
    public static Random vehGenR = new Random(vehGenSeed);
    public static final int vehGenSeed1 = 123456789;
    public static Random vehGenR1 = new Random(vehGenSeed1);
    public static final int vehGenSeed2 = 123456789;
    public static Random vehGenR2 = new Random(vehGenSeed2);
    public static final int iniVehSeed = 123456789;
    public static Random iniVehR = new Random(iniVehSeed);
    public static final int vehAccSeed = 123456789;
    public static Random vehAccR = new Random(vehAccSeed);
    public static final int relaxRseed = 12345678;
    public static Random relaxR = new Random(relaxRseed);
    public static final int ExRelaxRseed = 123456789;
    public static Random ExRelaxR = new Random(ExRelaxRseed);
    public static final int GapAcc1Rseed = 123456;
    public static Random GapAcc1R = new Random(GapAcc1Rseed);
    public static final int GapAcc2Rseed = 12345;
    public static Random GapAcc2R = new Random(GapAcc2Rseed);
    
    public static final int HOTrSeed = 12345;
    public static Random HOTr = new Random(HOTrSeed);

//    public static String dbName = Integer.toString((int)r.nextDouble()*100000);
    public static int mergeRampCount=0;                                        //this is the number of static cars in the system---used to make sure the system is empty
    public static boolean fwyInflowFlag=true;                                  //to check if fwy inflow is 0 or not
    public static long vehsExited;                                             //number of vehicles entered the system is given by carIDs and this variable gives number of vehicles exited the system till noe
    public static int errorVehicleCount= 0;

    // tunable parameters
    public static double maxpoints = 12;                              //# of data points for data smoothing
    public static double DenSampSize= 42;                           //# of data points for data forecasting
    public static double ForecastLeadTime = 30;                      //# of data points forecasting into future
    public static double UpRate = 300;
    public static double DownRate = 60;
    public static double InterSectionPropagationFactor = 0.4;
    public static double MinVol = 4;
    public static double MaxVol = 10;
    public static double MinSpeed = 26;
    public static double MaxSpeed = 40;
    public static double MinDen = 18;
    public static double MaxDen = 37;
    public static double q = 0.22;
    public static double SatDen = 36;
    public static int KR = 120;

    public static double alpha = 0.90;
    public static double beta = 0.6;
    public static double gamma = 1.22;

    //---VSL_RM

    public static double vsl_l_1 = 0.5;
    public static double vsl_l_2 = 0.7;

    public static int flushUpOcc = 55;
    public static int flushDownOcc = 30;
    


    public static String meteringRate;// = "Params-"+(int)maxpoints+"-"+(int)DenSampSize+"-"+(int)ForecastLeadTime+"-"+(int)UpRate+"-"+(int)DownRate+"-"+(int)(InterSectionPropagationFactor*100)+"-"+(int)MinVol+"-"+(int)MaxVol+"-"+(int)MinSpeed+"-"+(int)MaxSpeed+"-"+(int)MinDen+"-"+(int)MaxDen+"-"+(int)q+"-"+(int)SatDen;
    public static double HOTratio = 0;
// Origin-Destination Matrix Display
    javax.swing.JFrame jFrameOrDest;
    JTextField mat[][];
    
// Simulation Controls
    boolean simulationHaltedFlag;
    boolean initializeParam;
    int simFrameWid = 1400;
    int simFrameHei = 200;
    public static long carIdToPrint = 0;
    public static int BNConnId;
// The Intersection to view
    Intersection intToView;
    boolean intToViewChanged = true;

 
    private WaypointIntersection getNearestWaypoint(WaypointIntersection wp, double accRadius) {
        Point2D p = jXMapKit1.getMainMap().convertGeoPositionToPoint(wp.getPosition());
        WaypointIntersection wpn = null;
        
        double minDis = 100000000;
        double dis = 0;
        
        for(WaypointIntersection wpt : intMap.keySet()){
            Point2D pt = jXMapKit1.getMainMap().convertGeoPositionToPoint(wpt.getPosition());
            dis = Math.sqrt(Math.pow(pt.getX() - p.getX(), 2) + Math.pow(pt.getY() - p.getY(), 2));
            
            if((dis < minDis) && (dis < accRadius)){
                minDis = dis;
                wpn = wpt;
            }
        }
        return wpn;
    }

    private WaypointConnection getNearestWaypointConnection(WaypointIntersection wp, double perpDis) {
        Point2D p = jXMapKit1.getMainMap().convertGeoPositionToPoint(wp.getPosition());
        WaypointConnection wpnc = null;
        
        double a,b,c;
        double denom;
        double disTemp;
        double minPerpDis = 10000000;
        
        for(WaypointConnection wptc : conMap.keySet()){
            Point2D pt1 = jXMapKit1.getMainMap().convertGeoPositionToPoint(wptc.wp1.getPosition());
            Point2D pt2 = jXMapKit1.getMainMap().convertGeoPositionToPoint(wptc.wp2.getPosition());
            
            a = pt2.getY() - pt1.getY();
            b = pt1.getX() - pt2.getX();
            c = pt1.getY()*(pt2.getX() - pt1.getX()) - pt1.getX()*(pt2.getY() - pt1.getY());
            denom = Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
            
            if(
            ((p.getX() >= pt1.getX()) && (p.getY() >= pt1.getY()) && (p.getX() <= pt2.getX()) && (p.getY() <= pt2.getY())) ||
            ((p.getX() >= pt2.getX()) && (p.getY() >= pt2.getY()) && (p.getX() <= pt1.getX()) && (p.getY() <= pt1.getY())) ||
            ((p.getX() <= pt1.getX()) && (p.getY() >= pt1.getY()) && (p.getX() >= pt2.getX()) && (p.getY() <= pt2.getY())) ||
            ((p.getX() <= pt2.getX()) && (p.getY() >= pt2.getY()) && (p.getX() >= pt1.getX()) && (p.getY() <= pt1.getY())) 
            ){
                disTemp = Math.abs((a*p.getX() + b*p.getY() + c)/denom);
                
                if((disTemp <= perpDis) && (disTemp < minPerpDis)){
                    wpnc = wptc;
                    minPerpDis = disTemp;
                }                
            }
        }
        return wpnc;
    }

    private void saveGraph(File f) {
    	try{
            	FileOutputStream fos = new FileOutputStream(f);
    		ObjectOutputStream oos = new ObjectOutputStream(fos);   
    		oos.writeObject(graph);
    		oos.close();
    		fos.close();
                FileInputStream fis = new FileInputStream(f);
                ObjectInputStream ois = new ObjectInputStream(fis);
                graph = (DirectedWeightedMultigraph<Intersection,Connection>) ois.readObject();          
                ois.close();
                fis.close();
            }
    	catch(Exception e){
    		System.out.println("Error "+e.toString());
        }
    }

    private void openGraph(File f) {
    	try{
                System.out.println("In openGraph()");
                String extension = null; 
                fileName = null;

    		fileName = f.getName().trim();
                int dotPos = fileName.lastIndexOf(".");
                extension = fileName.substring(dotPos);
                
                if(extension.equalsIgnoreCase(".txt"))
                {
                    Logic.convertAsciiToGraph(fileName);
                    graph = jXMapKitDrawGraph();
                } 
                else // Create the graph object
                {
                    FileInputStream fis = new FileInputStream(f);
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    graph = (DirectedWeightedMultigraph<Intersection,Connection>) ois.readObject();          
                    ois.close();
                    fis.close();
                }
                intMap = new HashMap<WaypointIntersection, Intersection>() ;
                conMap = new HashMap<WaypointConnection, Connection>() ;
                
                // Create the two maps - intMap and conMap
                System.out.println("Before for con: " + graph.edgeSet().size());
                for(Connection con : graph.edgeSet()){
                    Intersection src = graph.getEdgeSource(con);
                    Intersection tgt = graph.getEdgeTarget(con);
                    WaypointIntersection srcWp = new WaypointIntersection(src.lat, src.lon);
                    WaypointIntersection tgtWp = new WaypointIntersection(tgt.lat, tgt.lon);
                
                    if(! intMap.containsKey(srcWp))
                        intMap.put(srcWp, src);
                    
                    if(! intMap.containsKey(tgtWp))
                        intMap.put(tgtWp, tgt);
                    
                    conMap.put(new WaypointConnection(srcWp, tgtWp), con);
                }
                
                for(Intersection intr : graph.vertexSet()){
                    WaypointIntersection wp = new WaypointIntersection(intr.lat, intr.lon);
                    
                    if(! intMap.containsKey(wp))
                        intMap.put(wp, intr);
                }

                // Initialize the wpOverlay
                wpOverlay.setWaypoints(intMap.keySet());
                jXMapKit1.repaint();
                
    	}catch(Exception e){
    		System.out.println("Error "+e.toString());
    	}
    }

    private DirectedWeightedMultigraph<Intersection, Connection> jXMapKitDrawGraph() throws Exception{                                       
        int intCount, conCount, onRampCount, offRampCount,
                node[], node1[], node2[], laneCount[], isOrigin[], isDest[], onRamp[], offRamp[];
        double linkLength[], X[], Y[];
        
        System.out.println("In jXMapKitDrawGraph()");
        String URL = "jdbc:microsoft:sqlserver://localhost:1433;DatabaseName=network"; 
        Class.forName("com.microsoft.jdbc.sqlserver.SQLServerDriver"); 
        java.sql.Connection con1 = DriverManager.getConnection(URL,"sa","");
        
        Statement st1 = con1.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_UPDATABLE);
        st1.execute("use network");         
        ResultSet rs1;
        
        rs1 = st1.executeQuery("select count(*) from nodeInfo");
        rs1.first();
        intCount = rs1.getInt(1);
        X = new double[intCount];
        Y = new double[intCount];
        node = new int[intCount];
        isOrigin = new int[intCount];
        isDest = new int[intCount];
        
        rs1 = st1.executeQuery("select node, X, Y, isOrigin, isDest from nodeInfo");
        rs1.first();
        int i=0;
        do
        {
            node[i] = rs1.getInt(1);
            X[i] = rs1.getInt(2) * 0.3048;
            Y[i] = rs1.getInt(3) * 0.3048;
            isOrigin[i] = rs1.getInt(4);
            isDest[i] = rs1.getInt(5);
            i++;
        }while(rs1.next());
        
        rs1 = st1.executeQuery("select count(*) from linkInfo where isOnRamp=1");
        rs1.first();
        onRampCount = rs1.getInt(1);
        onRamp = new int[onRampCount];
        
        rs1 = st1.executeQuery("select downNode from linkInfo where isOnRamp=1");
        rs1.first();
        for(i=0;i<onRampCount;i++)
        {
            onRamp[i] = rs1.getInt(1);
            rs1.next();
        }
        
        rs1 = st1.executeQuery("select count(*) from linkInfo where isOffRamp=1");
        rs1.first();
        offRampCount = rs1.getInt(1);
        offRamp = new int[offRampCount];
        
        rs1 = st1.executeQuery("select upNode from linkInfo where isOffRamp=1");
        rs1.first();
        for(i=0;i<offRampCount;i++)
        {
            offRamp[i] = rs1.getInt(1);
            rs1.next();
        }
                
        // Add intersections
        System.out.println("Adding intersections");
        for(i=0;i<intCount;i++)
        {
            WaypointIntersection wp = new WaypointIntersection(
                    jXMapKit1.getMainMap().convertPointToGeoPosition(new Point2D.Double(X[i]+500,Y[i]+500)));
            Intersection intrscn = new Intersection(wp.getPosition().getLatitude(),wp.getPosition().getLongitude(),
             "["+roundDouble(wp.getPosition().getLatitude(),4)+","+roundDouble(wp.getPosition().getLongitude(),4)+"]");
            if(isOrigin[i] == 1)
                intrscn.type = Intersection.ORIGIN;
            else if(isDest[i] == 1) 
                intrscn.type = Intersection.DESTINATION;
            
            for(int j=0;j<onRampCount;j++) 
                if(node[i] == onRamp[j])
                    intrscn.type = Intersection.RAMP;
            
            for(int j=0;j<offRampCount;j++) 
                if(node[i] == offRamp[j])
                    intrscn.type = Intersection.EXIT;
            
            // Adding to Graph
            graph.addVertex(intrscn);
            
            // Adding to Map
            intMap.put(wp, intrscn);

            // Adding to the list of actions
            if(actionIndex == actions.size())
                actions.add(actionIndex, new ActionPerformed(0,wp,null,intrscn,null,null,actionIndex));
            else
                actions.set(actionIndex, new ActionPerformed(0,wp,null,intrscn,null,null,actionIndex));
            actionIndex++;

            System.out.println("Added, Group = "+(actionIndex-1));
            wpOverlay.setWaypoints(intMap.keySet());
            jXMapKit1.repaint();
        }
                
        rs1 = st1.executeQuery("select count(*) from linkInfo");
        rs1.first();
        conCount = rs1.getInt(1);
        node1 = new int[conCount];
        node2 = new int[conCount];
        laneCount = new int[conCount];
        onRamp = new int[conCount];
        offRamp = new int[conCount];
        linkLength = new double[conCount];

        rs1 = st1.executeQuery("select upNode, downNode, linkLength, noOfLanes from linkInfo");
        
        rs1.first();
        for(i=0;i<conCount;i++)
        {
            node1[i] = rs1.getInt(1);
            node2[i] = rs1.getInt(2);
            linkLength[i] = rs1.getDouble(3);
            if(linkLength[i] == 0.0)
                linkLength[i] = 300.0;
            laneCount[i] = rs1.getInt(4);
            rs1.next();
        }
        
        // Add connections
        System.out.println("Adding Connections");
        int j;
        for(i=0;i<conCount;i++)
        { 
            for(j=0;j<intCount;j++)
                if(node1[i] == node[j])
                    break;
            WaypointIntersection wpt = new WaypointIntersection(
                    jXMapKit1.getMainMap().convertPointToGeoPosition(new Point2D.Double(X[j],Y[j])));
            
            for(j=0;j<intCount;j++)
                if(node2[i] == node[j])
                    break;
            System.out.println("j: " + j);
            if(j==541)
                System.out.println("541: " + node2[i]);
            WaypointIntersection wpn = new WaypointIntersection(
                    jXMapKit1.getMainMap().convertPointToGeoPosition(new Point2D.Double(X[j],Y[j])));
            
            waypointSelectedFlag = false;
            Connection connctn = new Connection(laneCount[i], linkLength[i]);
            
            // Adding to Graph
            graph.addEdge(intMap.get(wpt), intMap.get(wpn), connctn);
            graph.setEdgeWeight(connctn,connctn.length);                        

            // Adding to Map
            WaypointConnection wpcn = new WaypointConnection(wpt, wpn);
            conMap.put(wpcn,connctn);

            // Adding to the list of actions
            if(actionIndex == actions.size())
                actions.add(actionIndex, new ActionPerformed(1,null,wpcn,intMap.get(wpt),connctn, intMap.get(wpn),actionIndex));
            else
                actions.set(actionIndex, new ActionPerformed(1,null,wpcn,intMap.get(wpt),connctn, intMap.get(wpn),actionIndex));
            actionIndex++;
            System.out.println("Added, Group = "+(actionIndex-1));
        }

        System.out.println("out of loop");
        jXMapKit1.repaint();
        System.out.println("After repaint");
        System.out.println(graph.toString());
        
        return graph;
    }

    private double roundDouble(double la, int i) {
         return Math.round(la * Math.pow(10, (double) i)) / Math.pow(10,(double) i);
    }

    public TrafficSimulationDesktopView(SingleFrameApplication app) {
        super(app);

        initComponents();

        // Make all the to be updated fields as null
        toBeUpdatedWaypoint = null;
        toBeUpdatedConnection = null;
        
        // Add the Radio Buttons to the group
        buttonGroup1.add(RB_Navigate);
        buttonGroup1.add(RB_AddInt);
        buttonGroup1.add(RB_AddCon);
        buttonGroup1.add(RB_Erase);
        
        // Add the Radio Buttons to the group - 2
        buttonGroup2.add(RB_StopY_UnSignal_IntPropWindow);
        buttonGroup2.add(RB_StopN_UnSignal_IntPropWindow);
        
        // Set all the flags
        navigateFlag = true;
        addIntersectionsFlag = false;
        addConnectionsFlag = false;
        eraseFlag = false;
        isChanged = false;
        
        // Simulation Control
        simulationHaltedFlag = true;
        initializeParam = true;
        
        // Simulation Frame Properties
        F_SimWindow.setBounds(new Rectangle(10,10,simFrameWid, simFrameHei+75));
        F_SimWindow.setTitle("Simulation Window");
//        F_SimWindow.setAlwaysOnTop(true);
        F_SimWindow.setVisible(false);
        F_SimWindow.setResizable(false);
        F_ControlWindow.setBounds(new Rectangle(10,simFrameHei+10,275, 200)); 
        F_ControlWindow.setTitle("Controls");
//        F_ControlWindow.setAlwaysOnTop(true);
        F_ControlWindow.setVisible(false);
        F_ControlWindow.setResizable(false);
                
        // Initialize the Graph and the Maps
        graph = new DirectedWeightedMultigraph<Intersection, Connection>(Connection.class);
        intMap = new HashMap<WaypointIntersection, Intersection>() ;
        conMap = new HashMap<WaypointConnection, Connection>() ;
        
        // Initialize Undo and Redo Options
        actions = new ArrayList<ActionPerformed>();
        actionIndex = 0;

        // Add the Compound Painter
        compPainter = new CompoundPainter();
        compPainter.setCacheable(false);
        jXMapKit1.getMainMap().setOverlayPainter(compPainter);
        
        // Add the Mouse Listener Functions
        addMouseListenerFunctions();
        
        // Waypoint Overlay - Based on Geo Coordinates
        wpOverlay = new WaypointPainter();
                
        // The Text Overlay - Based on Map Coordinates
        textOverlay = new Painter<JXMapViewer>() {
            public void paint(Graphics2D g, JXMapViewer map, int w, int h) {
                g = (Graphics2D) g.create();
                
                // The constant text
                g.setPaint(new Color(165,42,42,170));
                g.fillRoundRect(25, 10, 182 , 30, 10, 10);
                g.setPaint(Color.WHITE);
                g.drawString("Traffic Simulator - Scenario Builder", 25+10, 10+20);
                          
                // The variable text
                Rectangle rect = map.getViewportBounds();
                g.translate(-rect.x, -rect.y);

                for(WaypointIntersection wp : intMap.keySet()) {
                   Intersection intn = intMap.get(wp);
                   Point2D pt = map.getTileFactory().geoToPixel(new GeoPosition(intn.lat, intn.lon), map.getZoom());
                   
                   if((waypointSelectedFlag) && (selectedWaypoint != null) && (intn.lat == selectedWaypoint.getPosition().getLatitude()) && (intn.lon == selectedWaypoint.getPosition().getLongitude())){
                       g.setPaint(Color.ORANGE);
                   }
                   //g.drawString(intn.intersectionName, (int)pt.getX() + 10, (int)pt.getY() + 9);
                   
                   if(intn.type == Intersection.ORIGIN){
                        g.setPaint(new Color(0,0,0,255));
                        g.setStroke(new BasicStroke(2.0f));
                        g.drawRect((int)pt.getX()-5, (int)pt.getY()-29, 9, 9);
                        g.setPaint(new Color(0,255,0,200));
                        g.fillRect((int)pt.getX()-4, (int)pt.getY()-28, 8, 8);
                   }
                   if(intn.type == Intersection.DESTINATION){
                        g.setPaint(new Color(0,0,0,255));
                        g.setStroke(new BasicStroke(2.0f));
                        g.drawRect((int)pt.getX()-5, (int)pt.getY()-29, 9, 9);
                        g.setPaint(new Color(255,0,0,200));
                        g.fillRect((int)pt.getX()-4, (int)pt.getY()-28, 8, 8);                   
                   }
                   if(intn.type == Intersection.ORIGIN_AND_DEST){
                        g.setPaint(new Color(0,0,0,255));
                        g.setStroke(new BasicStroke(2.0f));
                        g.drawRect((int)pt.getX()-5, (int)pt.getY()-29, 10, 10);
                        g.setPaint(new Color(0,255,0,200));
                        g.fillRect((int)pt.getX()-4, (int)pt.getY()-28, 5, 9);                   
                        g.setPaint(new Color(0,0,0,255));
                        g.setStroke(new BasicStroke(2.0f));
                        g.drawRect((int)pt.getX()+1, (int)pt.getY()-29, 5, 10);
                        g.setPaint(new Color(255,0,0,200));
                        g.fillRect((int)pt.getX()+2, (int)pt.getY()-28, 5, 9);                   
                   }                   
                   if(intn.type == Intersection.RAMP){
                        g.setPaint(new Color(0,0,0,255));
                        g.setStroke(new BasicStroke(2.0f));
                        g.drawOval((int)pt.getX()-5, (int)pt.getY()-29, 9, 9);
                        g.setPaint(new Color(0,255,0,200));
                        g.fillOval((int)pt.getX()-4, (int)pt.getY()-28, 8, 8);
                   }
                   if(intn.type == Intersection.RAMP2){
                        g.setPaint(new Color(0,0,0,255));
                        g.setStroke(new BasicStroke(2.0f));
                        g.drawOval((int)pt.getX()-5, (int)pt.getY()-29, 9, 9);
                        g.setPaint(new Color(0,255,0,200));
                        g.fillOval((int)pt.getX()-4, (int)pt.getY()-28, 8, 8);
                        g.setPaint(Color.BLACK);
                        g.setFont(new Font("Arial", Font.BOLD, 9));
                        g.drawString("2", (int)pt.getX()-2, (int)pt.getY()-21);
                   }                   
                   if(intn.type == Intersection.EXIT){
                        g.setPaint(new Color(0,0,0,255));
                        g.setStroke(new BasicStroke(2.0f));
                        g.drawOval((int)pt.getX()-5, (int)pt.getY()-29, 9, 9);
                        g.setPaint(new Color(255,0,0,200));
                        g.fillOval((int)pt.getX()-4, (int)pt.getY()-28, 8, 8);                   
                   }
                   if(intn.type == Intersection.EXIT2){
                        g.setPaint(new Color(0,0,0,255));
                        g.setStroke(new BasicStroke(2.0f));
                        g.drawOval((int)pt.getX()-5, (int)pt.getY()-29, 9, 9);
                        g.setPaint(new Color(255,0,0,200));
                        g.fillOval((int)pt.getX()-4, (int)pt.getY()-28, 8, 8);
                        g.setPaint(Color.YELLOW);
                        g.setFont(new Font("Arial", Font.BOLD, 9));
                        g.drawString("2", (int)pt.getX()-2, (int)pt.getY()-21);
                   }
                   if(intn.type == Intersection.EXIT3){
                        g.setPaint(new Color(0,0,0,255));
                        g.setStroke(new BasicStroke(2.0f));
                        g.drawOval((int)pt.getX()-5, (int)pt.getY()-29, 9, 9);
                        g.setPaint(new Color(255,0,0,200));
                        g.fillOval((int)pt.getX()-4, (int)pt.getY()-28, 8, 8);
                        g.setPaint(Color.YELLOW);
                        g.setFont(new Font("Arial", Font.PLAIN, 9));
                        g.drawString("3", (int)pt.getX()-2, (int)pt.getY()-21);
                   }
                   if(intn == intToView){
                       g.setPaint(Color.BLUE);
                       g.setFont(new Font("Arial", Font.BOLD, 16));
                       g.drawString(intn.intersectionName, (int)pt.getX()-5, (int)pt.getY()-35); 
                   }
                   else if(intn.type!=Intersection.GENERIC)
                   {
                   g.setPaint(Color.BLUE);
                   g.setFont(new Font("Arial", Font.BOLD, 10));
                   g.drawString(intn.intersectionName, (int)pt.getX()+15, (int)pt.getY()-35); 
                   }
                }
                
                for(WaypointConnection wp : conMap.keySet()) {
                   Connection conn = conMap.get(wp);
                   double lat = (graph.getEdgeSource(conn).lat+graph.getEdgeTarget(conn).lat)/2;
                   double lon = (graph.getEdgeSource(conn).lon+graph.getEdgeTarget(conn).lon)/2;
                   Point2D pt = map.getTileFactory().geoToPixel(new GeoPosition(lat, lon), map.getZoom());

                   int ij= 0;
                   for(Connection p : graph.edgeSet())   //to print the connId on the network because conn.connId is currently 0 for all connections
                   {
                       if(conn!=p)
                           ij++;
                       else
                           break;
                   }
                   
                   g.setPaint(Color.BLACK);
                   g.setFont(new Font("Arial", Font.BOLD, 10));
                   if(conn.isOnRamp==1)
                   {
                       g.setPaint(Color.GREEN);
                       g.drawString("On-Ramp ( "+ij+" )", (int)pt.getX(), (int)pt.getY()-30); 
                   }
                   else if (conn.isOffRamp==1)
                   {
                       g.setPaint(Color.RED);
                       g.drawString("Off-Ramp ( "+ij+" )", (int)pt.getX(), (int)pt.getY()-30);
                   }
                   else
                       g.drawString("Freeway ( "+ij+" )", (int)pt.getX(), (int)pt.getY()-30); 
                   
                   g.drawString(conn.nLanes+" Lanes", (int)pt.getX(), (int)pt.getY()-20); 
                   g.drawString(conn.length+" Km", (int)pt.getX(), (int)pt.getY()-10); 
                   
                   if(conn.hasAccLane==1)
                   {
                       g.setPaint(Color.GREEN);
                       g.drawString("Acc. lane length: "+conn.AccLaneLength+" Km", (int)pt.getX(), (int)pt.getY()-2);
                   }
                   if(conn.hasDecLane==1)
                   {
                       g.setPaint(Color.RED);
                       g.drawString("Decel. lane length: "+conn.DecLaneLength+" Km", (int)pt.getX(), (int)pt.getY()-2);
                   }
                }                
                g.dispose();
           }
       };

       // The Line Overlay - Based on Geo Coordinates
       lOverlay = new Painter<JXMapViewer>() {
           public void paint(Graphics2D g, JXMapViewer map, int w, int h) {
               g = (Graphics2D) g.create();
               Rectangle rect = map.getViewportBounds();
               g.translate(-rect.x, -rect.y);
               
               g.setColor(new Color(139,69,13,200));
               
               for(WaypointConnection wpc : conMap.keySet()) {
                   Point2D pt1 = map.getTileFactory().geoToPixel(wpc.wp1.getPosition(), map.getZoom());
                   Point2D pt2 = map.getTileFactory().geoToPixel(wpc.wp2.getPosition(), map.getZoom());
                   g.setStroke(new BasicStroke(6.0f));
                   g.drawLine((int)pt1.getX(),(int)pt1.getY(), (int)pt2.getX(), (int)pt2.getY());
               }
               g.dispose();
           }
       };

       // Add the Overlays
        compPainter.setPainters(lOverlay,wpOverlay,textOverlay);

//--------------------------code below is automatically generated---------------------------
        
        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String)(evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer)(evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });

    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = TrafficSimulationDesktopApp.getApplication().getMainFrame();
            aboutBox = new TrafficSimulationDesktopAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        TrafficSimulationDesktopApp.getApplication().show(aboutBox);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jXMapKit1 = new org.jdesktop.swingx.JXMapKit();
        jPanel2 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        RB_Navigate = new javax.swing.JRadioButton();
        RB_AddInt = new javax.swing.JRadioButton();
        RB_AddCon = new javax.swing.JRadioButton();
        RB_Erase = new javax.swing.JRadioButton();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        MI_New = new javax.swing.JMenuItem();
        MI_Open = new javax.swing.JMenuItem();
        MI_Save = new javax.swing.JMenuItem();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        MI_SetNtwrkParams = new javax.swing.JMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        buttonGroup1 = new javax.swing.ButtonGroup();
        F_SimWindow = new javax.swing.JFrame();
        simulation1 = new trafficsimulationdesktop.Simulation();
        F_IntProperties = new javax.swing.JFrame();
        P_IntWindow = new javax.swing.JPanel();
        L_Name_IntWindow = new javax.swing.JLabel();
        TF_Name_IntWindow = new javax.swing.JTextField();
        L_Type_IntWindow = new javax.swing.JLabel();
        CB_Type_IntWindow = new javax.swing.JComboBox();
        B_Properties_IntWindow = new javax.swing.JButton();
        P_OKCancel_IntWindow = new javax.swing.JPanel();
        B_OK_IntWindow = new javax.swing.JButton();
        B_Cancel_IntWindow = new javax.swing.JButton();
        F_IntPropWindow = new javax.swing.JFrame();
        TP_IntPropWindow = new javax.swing.JTabbedPane();
        P_Ramp_IntPropWindow = new javax.swing.JPanel();
        SB_Gore_Ramp_IntPropWindow = new trafficsimulationdesktop.SlideBar();
        SB_Sd_Ramp_IntPropWindow = new trafficsimulationdesktop.SlideBar();
        P_Exit_IntPropWindow = new javax.swing.JPanel();
        SB_Gore_Exit_IntPropWindow = new trafficsimulationdesktop.SlideBar();
        P_Signal_IntPropWindow = new javax.swing.JPanel();
        SB_Red_Signal_IntPropWindow = new trafficsimulationdesktop.SlideBar();
        SB_Green_Signal_IntPropWindow = new trafficsimulationdesktop.SlideBar();
        SB_Offset_Signal_IntPropWindow = new trafficsimulationdesktop.SlideBar();
        P_Unsignal_IntPropWindow = new javax.swing.JPanel();
        P_Stop_UnSignal_IntPropWindow = new javax.swing.JPanel();
        RB_StopY_UnSignal_IntPropWindow = new javax.swing.JRadioButton();
        RB_StopN_UnSignal_IntPropWindow = new javax.swing.JRadioButton();
        P_OKCancel_IntPropWindow = new javax.swing.JPanel();
        B_OK_IntPropWindow = new javax.swing.JButton();
        B_Cancel_IntPropWindow = new javax.swing.JButton();
        buttonGroup2 = new javax.swing.ButtonGroup();
        F_ConPropWindow = new javax.swing.JFrame();
        TP_ConPropWindow = new javax.swing.JTabbedPane();
        P_RdPrms_ConPropWindow = new javax.swing.JPanel();
        L_Lanes_RdPrms_ConPropWindow = new javax.swing.JLabel();
        CB_Lanes_RdPrms_ConPropWindow = new javax.swing.JComboBox();
        L_LLength_RdPrms_ConPropWindow = new javax.swing.JLabel();
        TF_LLength_RdPrms_ConPropWindow = new javax.swing.JTextField();
        L_OnRamp_RdPrms_ConPropWindow = new javax.swing.JLabel();
        CB_OnRamp_RdPrms_ConPropWindow = new javax.swing.JCheckBox();
        L_OffRamp_RdPrms_ConPropWindow = new javax.swing.JLabel();
        CB_OffRamp_RdPrms_ConPropWindow = new javax.swing.JCheckBox();
        CB_AccLane_RdPrms_ConPropWindow = new javax.swing.JCheckBox();
        L_AccLane_RdPrms_ConPropWindow = new javax.swing.JLabel();
        TF_AccLane_RdPrms_ConPropWindow = new javax.swing.JTextField();
        CB_DecLane_RdPrms_ConPropWindow = new javax.swing.JCheckBox();
        L_DecLane_RdPrms_ConPropWindow = new javax.swing.JLabel();
        TF_DecLane_RdPrms_ConPropWindow = new javax.swing.JTextField();
        SB_Grade_RdPrms_ConPropWindow = new trafficsimulationdesktop.SlideBar();
        P_RM_ConPropWindow = new javax.swing.JPanel();
        SB_MR_Ramp_ConPropWindow = new trafficsimulationdesktop.SlideBar();
        jLabel1 = new javax.swing.JLabel();
        TF_MaxMR = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        TF_MinMR = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        TF_FlushDown = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        TF_FlushUp = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        TF_Storage = new javax.swing.JTextField();
        R_Alinea = new javax.swing.JRadioButton();
        R_SWARM1 = new javax.swing.JRadioButton();
        R_SWARM12 = new javax.swing.JRadioButton();
        jLabel6 = new javax.swing.JLabel();
        CB_Flush = new javax.swing.JCheckBox();
        jLabel7 = new javax.swing.JLabel();
        TF_BN = new javax.swing.JTextField();
        P_CF_ConPropWindow = new javax.swing.JPanel();
        SB_MaxSpd_CF_ConPropWindow = new trafficsimulationdesktop.SlideBar();
        SB_JamDen_CF_ConPropWindow = new trafficsimulationdesktop.SlideBar();
        SB_WaveSpd_CF_ConPropWindow = new trafficsimulationdesktop.SlideBar();
        P_LC_ConPropWindow = new javax.swing.JPanel();
        SB_Epsilon_LC_ConPropWindow = new trafficsimulationdesktop.SlideBar();
        SB_Tau_LC_ConPropWindow = new trafficsimulationdesktop.SlideBar();
        SB_Sd_LC_ConPropWindow = new trafficsimulationdesktop.SlideBar();
        P_OkCancel_ConPropWindow = new javax.swing.JPanel();
        B_Ok_ConPropWindow = new javax.swing.JButton();
        B_Cancel_ConPropWindow = new javax.swing.JButton();
        F_SetNtwrkParams = new javax.swing.JFrame();
        P_SetNtwrkParams = new javax.swing.JPanel();
        L_ID_SetNtwrkParams = new javax.swing.JLabel();
        F_ID_SetNtwrkParams = new javax.swing.JTextField();
        L_TP_SetNtwrkParams = new javax.swing.JLabel();
        F_TP_SetNtwrkParams = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        RB_Relax = new javax.swing.JRadioButton();
        RB_GapAcc1 = new javax.swing.JRadioButton();
        RB_GapAcc2 = new javax.swing.JRadioButton();
        L_MinSpdDiff_SetNtwrkParams = new javax.swing.JLabel();
        F_MinSpdDiff_SetNtwrkParams = new javax.swing.JTextField();
        P_OKCancel_SetNtwrkParams = new javax.swing.JPanel();
        B_OK_SetNtwrkParams = new javax.swing.JButton();
        B_Cancel_SetNtwrkParams = new javax.swing.JButton();
        F_ControlWindow = new javax.swing.JFrame();
        SB_SimSpeed_ControlWindow = new trafficsimulationdesktop.SlideBar();
        SB_MR_ControlWindow = new trafficsimulationdesktop.SlideBar();
        buttonGroup3 = new javax.swing.ButtonGroup();
        buttonGroup4 = new javax.swing.ButtonGroup();
        progressBar = new javax.swing.JProgressBar();

        mainPanel.setName("GTSim"); // NOI18N

        jPanel1.setName("jPanel1"); // NOI18N

        jXMapKit1.setDefaultProvider(org.jdesktop.swingx.JXMapKit.DefaultProviders.OpenStreetMaps);
        jXMapKit1.setName("jXMapKit1"); // NOI18N
        jXMapKit1.setCenterPosition(new GeoPosition(33.91,-84.30));
        jXMapKit1.setZoom(3);
        jXMapKit1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jXMapKit1MousePressed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jXMapKit1, javax.swing.GroupLayout.DEFAULT_SIZE, 1724, Short.MAX_VALUE)
                .addGap(83, 83, 83))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jXMapKit1, javax.swing.GroupLayout.DEFAULT_SIZE, 569, Short.MAX_VALUE)
                .addGap(101, 101, 101))
        );

        jPanel2.setName("jPanel2"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(trafficsimulationdesktop.TrafficSimulationDesktopApp.class).getContext().getActionMap(TrafficSimulationDesktopView.class, this);
        jButton1.setAction(actionMap.get("startSimulation")); // NOI18N
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(trafficsimulationdesktop.TrafficSimulationDesktopApp.class).getContext().getResourceMap(TrafficSimulationDesktopView.class);
        jButton1.setIcon(resourceMap.getIcon("jButton1.icon")); // NOI18N
        jButton1.setText(resourceMap.getString("jButton1.text")); // NOI18N
        jButton1.setName("jButton1"); // NOI18N

        jButton2.setAction(actionMap.get("pauseSimulation")); // NOI18N
        jButton2.setIcon(resourceMap.getIcon("jButton2.icon")); // NOI18N
        jButton2.setText(resourceMap.getString("jButton2.text")); // NOI18N
        jButton2.setToolTipText(resourceMap.getString("jButton2.toolTipText")); // NOI18N
        jButton2.setName("jButton2"); // NOI18N

        jButton3.setAction(actionMap.get("stopSimulation")); // NOI18N
        jButton3.setIcon(resourceMap.getIcon("jButton3.icon")); // NOI18N
        jButton3.setText(resourceMap.getString("jButton3.text")); // NOI18N
        jButton3.setName("jButton3"); // NOI18N

        jButton4.setAction(actionMap.get("undoActionProcedure")); // NOI18N
        jButton4.setIcon(resourceMap.getIcon("jButton4.icon")); // NOI18N
        jButton4.setText(resourceMap.getString("jButton4.text")); // NOI18N
        jButton4.setName("jButton4"); // NOI18N

        jButton5.setAction(actionMap.get("redoActionProcedure")); // NOI18N
        jButton5.setIcon(resourceMap.getIcon("jButton5.icon")); // NOI18N
        jButton5.setText(resourceMap.getString("jButton5.text")); // NOI18N
        jButton5.setName("jButton5"); // NOI18N

        jButton6.setAction(actionMap.get("centerAtAtlanta")); // NOI18N
        jButton6.setText(resourceMap.getString("jButton6.text")); // NOI18N
        jButton6.setName("jButton6"); // NOI18N

        RB_Navigate.setAction(actionMap.get("setNavigateFlag")); // NOI18N
        buttonGroup1.add(RB_Navigate);
        RB_Navigate.setSelected(true);
        RB_Navigate.setText(resourceMap.getString("RB_Navigate.text")); // NOI18N
        RB_Navigate.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        RB_Navigate.setName("RB_Navigate"); // NOI18N
        RB_Navigate.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);

        RB_AddInt.setAction(actionMap.get("setAddIntersectionFlag")); // NOI18N
        buttonGroup1.add(RB_AddInt);
        RB_AddInt.setText(resourceMap.getString("RB_AddInt.text")); // NOI18N
        RB_AddInt.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        RB_AddInt.setName("RB_AddInt"); // NOI18N
        RB_AddInt.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);

        RB_AddCon.setAction(actionMap.get("setAddConnectionsFlag")); // NOI18N
        buttonGroup1.add(RB_AddCon);
        RB_AddCon.setText(resourceMap.getString("RB_AddCon.text")); // NOI18N
        RB_AddCon.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        RB_AddCon.setName("RB_AddCon"); // NOI18N
        RB_AddCon.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);

        RB_Erase.setAction(actionMap.get("eraseComponents")); // NOI18N
        buttonGroup1.add(RB_Erase);
        RB_Erase.setText(resourceMap.getString("RB_Erase.text")); // NOI18N
        RB_Erase.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        RB_Erase.setName("RB_Erase"); // NOI18N
        RB_Erase.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addComponent(RB_Navigate)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(RB_AddInt)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(RB_AddCon)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(RB_Erase)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 933, Short.MAX_VALUE)
                .addComponent(jButton1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton3)
                .addGap(54, 54, 54)
                .addComponent(jButton4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton5)
                .addGap(50, 50, 50)
                .addComponent(jButton6)
                .addGap(19, 19, 19))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap(11, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jButton1)
                        .addComponent(jButton2)
                        .addComponent(jButton3)
                        .addComponent(jButton4)
                        .addComponent(jButton5))
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(RB_Erase)
                        .addComponent(RB_AddInt)
                        .addComponent(RB_AddCon)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton6)
                            .addComponent(RB_Navigate)))))
        );

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(10, 10, 10))
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        menuBar.setName("menuBar"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        MI_New.setAction(actionMap.get("newGraph")); // NOI18N
        MI_New.setText(resourceMap.getString("MI_New.text")); // NOI18N
        MI_New.setName("MI_New"); // NOI18N
        fileMenu.add(MI_New);

        MI_Open.setAction(actionMap.get("openGraph")); // NOI18N
        MI_Open.setText(resourceMap.getString("MI_Open.text")); // NOI18N
        MI_Open.setName("MI_Open"); // NOI18N
        fileMenu.add(MI_Open);

        MI_Save.setAction(actionMap.get("saveGraph")); // NOI18N
        MI_Save.setText(resourceMap.getString("MI_Save.text")); // NOI18N
        MI_Save.setName("MI_Save"); // NOI18N
        fileMenu.add(MI_Save);

        exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
        exitMenuItem.setName("exitMenuItem"); // NOI18N
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setText(resourceMap.getString("editMenu.text")); // NOI18N
        editMenu.setName("editMenu"); // NOI18N

        MI_SetNtwrkParams.setAction(actionMap.get("showNtwrkParamsWindow")); // NOI18N
        MI_SetNtwrkParams.setText(resourceMap.getString("MI_SetNtwrkParams.text")); // NOI18N
        MI_SetNtwrkParams.setName("MI_SetNtwrkParams"); // NOI18N
        editMenu.add(MI_SetNtwrkParams);

        menuBar.add(editMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
        aboutMenuItem.setName("aboutMenuItem"); // NOI18N
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 1744, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 1724, Short.MAX_VALUE)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel))
                .addGap(3, 3, 3))
        );

        F_SimWindow.setTitle(resourceMap.getString("F_SimWindow.title")); // NOI18N
        F_SimWindow.setAlwaysOnTop(true);
        F_SimWindow.setName("F_SimWindow"); // NOI18N

        simulation1.setName("simulation1"); // NOI18N

        javax.swing.GroupLayout simulation1Layout = new javax.swing.GroupLayout(simulation1);
        simulation1.setLayout(simulation1Layout);
        simulation1Layout.setHorizontalGroup(
            simulation1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 783, Short.MAX_VALUE)
        );
        simulation1Layout.setVerticalGroup(
            simulation1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 191, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout F_SimWindowLayout = new javax.swing.GroupLayout(F_SimWindow.getContentPane());
        F_SimWindow.getContentPane().setLayout(F_SimWindowLayout);
        F_SimWindowLayout.setHorizontalGroup(
            F_SimWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(F_SimWindowLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(simulation1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        F_SimWindowLayout.setVerticalGroup(
            F_SimWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(F_SimWindowLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(simulation1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        F_IntProperties.setTitle(resourceMap.getString("F_IntProperties.title")); // NOI18N
        F_IntProperties.setName("F_IntProperties"); // NOI18N

        P_IntWindow.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        P_IntWindow.setName("P_IntWindow"); // NOI18N

        L_Name_IntWindow.setText(resourceMap.getString("L_Name_IntWindow.text")); // NOI18N
        L_Name_IntWindow.setName("L_Name_IntWindow"); // NOI18N

        TF_Name_IntWindow.setText(resourceMap.getString("TF_Name_IntWindow.text")); // NOI18N
        TF_Name_IntWindow.setName("TF_Name_IntWindow"); // NOI18N

        L_Type_IntWindow.setText(resourceMap.getString("L_Type_IntWindow.text")); // NOI18N
        L_Type_IntWindow.setName("L_Type_IntWindow"); // NOI18N

        CB_Type_IntWindow.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "On Ramp", "Off Ramp", "Signalized", "Unsignalized", "Origin", "Destination", "Origin & Dest", "2-way", "Generic", "On Ramp_LaneAddition", "Off Ramp_LaneDrop", "Off Ramp_TwoLaneDrop", "" }));
        CB_Type_IntWindow.setSelectedIndex(8);
        CB_Type_IntWindow.setName("CB_Type_IntWindow"); // NOI18N
        CB_Type_IntWindow.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                CB_Type_IntWindowItemStateChanged(evt);
            }
        });

        B_Properties_IntWindow.setAction(actionMap.get("changeIntersectionSpecProps")); // NOI18N
        B_Properties_IntWindow.setText(resourceMap.getString("B_Properties_IntWindow.text")); // NOI18N
        B_Properties_IntWindow.setName("B_Properties_IntWindow"); // NOI18N

        javax.swing.GroupLayout P_IntWindowLayout = new javax.swing.GroupLayout(P_IntWindow);
        P_IntWindow.setLayout(P_IntWindowLayout);
        P_IntWindowLayout.setHorizontalGroup(
            P_IntWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_IntWindowLayout.createSequentialGroup()
                .addGroup(P_IntWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(P_IntWindowLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(P_IntWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(L_Name_IntWindow)
                            .addComponent(L_Type_IntWindow))
                        .addGap(31, 31, 31)
                        .addGroup(P_IntWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(TF_Name_IntWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(CB_Type_IntWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(P_IntWindowLayout.createSequentialGroup()
                        .addGap(111, 111, 111)
                        .addComponent(B_Properties_IntWindow)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        P_IntWindowLayout.setVerticalGroup(
            P_IntWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_IntWindowLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(P_IntWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(L_Name_IntWindow)
                    .addComponent(TF_Name_IntWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(50, 50, 50)
                .addGroup(P_IntWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(L_Type_IntWindow)
                    .addComponent(CB_Type_IntWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 27, Short.MAX_VALUE)
                .addComponent(B_Properties_IntWindow)
                .addContainerGap())
        );

        P_OKCancel_IntWindow.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        P_OKCancel_IntWindow.setName("P_OKCancel_IntWindow"); // NOI18N

        B_OK_IntWindow.setAction(actionMap.get("updateIntersectionProperties")); // NOI18N
        B_OK_IntWindow.setText(resourceMap.getString("B_OK_IntWindow.text")); // NOI18N
        B_OK_IntWindow.setName("B_OK_IntWindow"); // NOI18N

        B_Cancel_IntWindow.setAction(actionMap.get("closeIntersectionPropWindow")); // NOI18N
        B_Cancel_IntWindow.setText(resourceMap.getString("B_Cancel_IntWindow.text")); // NOI18N
        B_Cancel_IntWindow.setName("B_Cancel_IntWindow"); // NOI18N

        javax.swing.GroupLayout P_OKCancel_IntWindowLayout = new javax.swing.GroupLayout(P_OKCancel_IntWindow);
        P_OKCancel_IntWindow.setLayout(P_OKCancel_IntWindowLayout);
        P_OKCancel_IntWindowLayout.setHorizontalGroup(
            P_OKCancel_IntWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_OKCancel_IntWindowLayout.createSequentialGroup()
                .addGap(33, 33, 33)
                .addComponent(B_OK_IntWindow)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 99, Short.MAX_VALUE)
                .addComponent(B_Cancel_IntWindow)
                .addGap(70, 70, 70))
        );
        P_OKCancel_IntWindowLayout.setVerticalGroup(
            P_OKCancel_IntWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_OKCancel_IntWindowLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(P_OKCancel_IntWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(B_OK_IntWindow)
                    .addComponent(B_Cancel_IntWindow))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout F_IntPropertiesLayout = new javax.swing.GroupLayout(F_IntProperties.getContentPane());
        F_IntProperties.getContentPane().setLayout(F_IntPropertiesLayout);
        F_IntPropertiesLayout.setHorizontalGroup(
            F_IntPropertiesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(F_IntPropertiesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(F_IntPropertiesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(P_OKCancel_IntWindow, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(P_IntWindow, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        F_IntPropertiesLayout.setVerticalGroup(
            F_IntPropertiesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, F_IntPropertiesLayout.createSequentialGroup()
                .addGap(7, 7, 7)
                .addComponent(P_IntWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(P_OKCancel_IntWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        F_IntPropWindow.setName("F_IntPropWindow"); // NOI18N

        TP_IntPropWindow.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        TP_IntPropWindow.setName("TP_IntPropWindow"); // NOI18N

        P_Ramp_IntPropWindow.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        P_Ramp_IntPropWindow.setName("P_Ramp_IntPropWindow"); // NOI18N

        SB_Gore_Ramp_IntPropWindow.setName("SB_Gore_Ramp_IntPropWindow"); // NOI18N
        SB_Gore_Ramp_IntPropWindow.setTxt1(resourceMap.getString("SB_Gore_Ramp_IntPropWindow.txt1")); // NOI18N
        SB_Gore_Ramp_IntPropWindow.setTxt2(resourceMap.getString("SB_Gore_Ramp_IntPropWindow.txt2")); // NOI18N
        SB_Gore_Ramp_IntPropWindow.setValue(70);

        SB_Sd_Ramp_IntPropWindow.setSMax(1000);
        SB_Sd_Ramp_IntPropWindow.setName("SB_Sd_Ramp_IntPropWindow"); // NOI18N
        SB_Sd_Ramp_IntPropWindow.setTxt1(resourceMap.getString("SB_Sd_Ramp_IntPropWindow.txt1")); // NOI18N
        SB_Sd_Ramp_IntPropWindow.setTxt2(resourceMap.getString("SB_Sd_Ramp_IntPropWindow.txt2")); // NOI18N
        SB_Sd_Ramp_IntPropWindow.setValue(200);

        javax.swing.GroupLayout P_Ramp_IntPropWindowLayout = new javax.swing.GroupLayout(P_Ramp_IntPropWindow);
        P_Ramp_IntPropWindow.setLayout(P_Ramp_IntPropWindowLayout);
        P_Ramp_IntPropWindowLayout.setHorizontalGroup(
            P_Ramp_IntPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_Ramp_IntPropWindowLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(P_Ramp_IntPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(SB_Gore_Ramp_IntPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(SB_Sd_Ramp_IntPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(18, Short.MAX_VALUE))
        );
        P_Ramp_IntPropWindowLayout.setVerticalGroup(
            P_Ramp_IntPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_Ramp_IntPropWindowLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(SB_Gore_Ramp_IntPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(SB_Sd_Ramp_IntPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(76, Short.MAX_VALUE))
        );

        TP_IntPropWindow.addTab(resourceMap.getString("P_Ramp_IntPropWindow.TabConstraints.tabTitle"), P_Ramp_IntPropWindow); // NOI18N

        P_Exit_IntPropWindow.setName("P_Exit_IntPropWindow"); // NOI18N

        SB_Gore_Exit_IntPropWindow.setName("SB_Gore_Exit_IntPropWindow"); // NOI18N
        SB_Gore_Exit_IntPropWindow.setTxt1(resourceMap.getString("SB_Gore_Exit_IntPropWindow.txt1")); // NOI18N
        SB_Gore_Exit_IntPropWindow.setTxt2(resourceMap.getString("SB_Gore_Exit_IntPropWindow.txt2")); // NOI18N
        SB_Gore_Exit_IntPropWindow.setValue(70);

        javax.swing.GroupLayout P_Exit_IntPropWindowLayout = new javax.swing.GroupLayout(P_Exit_IntPropWindow);
        P_Exit_IntPropWindow.setLayout(P_Exit_IntPropWindowLayout);
        P_Exit_IntPropWindowLayout.setHorizontalGroup(
            P_Exit_IntPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_Exit_IntPropWindowLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(SB_Gore_Exit_IntPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(24, Short.MAX_VALUE))
        );
        P_Exit_IntPropWindowLayout.setVerticalGroup(
            P_Exit_IntPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_Exit_IntPropWindowLayout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(SB_Gore_Exit_IntPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(126, Short.MAX_VALUE))
        );

        TP_IntPropWindow.addTab(resourceMap.getString("P_Exit_IntPropWindow.TabConstraints.tabTitle"), P_Exit_IntPropWindow); // NOI18N

        P_Signal_IntPropWindow.setName("P_Signal_IntPropWindow"); // NOI18N

        SB_Red_Signal_IntPropWindow.setSMax(90);
        SB_Red_Signal_IntPropWindow.setName("SB_Red_Signal_IntPropWindow"); // NOI18N
        SB_Red_Signal_IntPropWindow.setTxt1(resourceMap.getString("SB_Red_Signal_IntPropWindow.txt1")); // NOI18N
        SB_Red_Signal_IntPropWindow.setTxt2(resourceMap.getString("SB_Red_Signal_IntPropWindow.txt2")); // NOI18N
        SB_Red_Signal_IntPropWindow.setValue(30);

        SB_Green_Signal_IntPropWindow.setSMax(90);
        SB_Green_Signal_IntPropWindow.setName("SB_Green_Signal_IntPropWindow"); // NOI18N
        SB_Green_Signal_IntPropWindow.setTxt1(resourceMap.getString("SB_Green_Signal_IntPropWindow.txt1")); // NOI18N
        SB_Green_Signal_IntPropWindow.setTxt2(resourceMap.getString("SB_Green_Signal_IntPropWindow.txt2")); // NOI18N
        SB_Green_Signal_IntPropWindow.setValue(30);

        SB_Offset_Signal_IntPropWindow.setName("SB_Offset_Signal_IntPropWindow"); // NOI18N
        SB_Offset_Signal_IntPropWindow.setTxt1(resourceMap.getString("SB_Offset_Signal_IntPropWindow.txt1")); // NOI18N
        SB_Offset_Signal_IntPropWindow.setTxt2(resourceMap.getString("SB_Offset_Signal_IntPropWindow.txt2")); // NOI18N
        SB_Offset_Signal_IntPropWindow.setValue(30);

        javax.swing.GroupLayout P_Signal_IntPropWindowLayout = new javax.swing.GroupLayout(P_Signal_IntPropWindow);
        P_Signal_IntPropWindow.setLayout(P_Signal_IntPropWindowLayout);
        P_Signal_IntPropWindowLayout.setHorizontalGroup(
            P_Signal_IntPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_Signal_IntPropWindowLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(P_Signal_IntPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(SB_Red_Signal_IntPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(SB_Green_Signal_IntPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(SB_Offset_Signal_IntPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(19, Short.MAX_VALUE))
        );
        P_Signal_IntPropWindowLayout.setVerticalGroup(
            P_Signal_IntPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_Signal_IntPropWindowLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(SB_Red_Signal_IntPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(SB_Green_Signal_IntPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(SB_Offset_Signal_IntPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(12, Short.MAX_VALUE))
        );

        TP_IntPropWindow.addTab(resourceMap.getString("P_Signal_IntPropWindow.TabConstraints.tabTitle"), P_Signal_IntPropWindow); // NOI18N

        P_Unsignal_IntPropWindow.setName("P_Unsignal_IntPropWindow"); // NOI18N

        P_Stop_UnSignal_IntPropWindow.setBorder(javax.swing.BorderFactory.createTitledBorder(new javax.swing.border.MatteBorder(null), resourceMap.getString("Has Stop Sign?.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, null, resourceMap.getColor("Has Stop Sign?.border.titleColor"))); // NOI18N
        P_Stop_UnSignal_IntPropWindow.setToolTipText(resourceMap.getString("Has Stop Sign?.toolTipText")); // NOI18N
        P_Stop_UnSignal_IntPropWindow.setName("Has Stop Sign?"); // NOI18N

        buttonGroup2.add(RB_StopY_UnSignal_IntPropWindow);
        RB_StopY_UnSignal_IntPropWindow.setText(resourceMap.getString("RB_StopY_UnSignal_IntPropWindow.text")); // NOI18N
        RB_StopY_UnSignal_IntPropWindow.setName("RB_StopY_UnSignal_IntPropWindow"); // NOI18N

        buttonGroup2.add(RB_StopN_UnSignal_IntPropWindow);
        RB_StopN_UnSignal_IntPropWindow.setText(resourceMap.getString("RB_StopN_UnSignal_IntPropWindow.text")); // NOI18N
        RB_StopN_UnSignal_IntPropWindow.setName("RB_StopN_UnSignal_IntPropWindow"); // NOI18N

        javax.swing.GroupLayout P_Stop_UnSignal_IntPropWindowLayout = new javax.swing.GroupLayout(P_Stop_UnSignal_IntPropWindow);
        P_Stop_UnSignal_IntPropWindow.setLayout(P_Stop_UnSignal_IntPropWindowLayout);
        P_Stop_UnSignal_IntPropWindowLayout.setHorizontalGroup(
            P_Stop_UnSignal_IntPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, P_Stop_UnSignal_IntPropWindowLayout.createSequentialGroup()
                .addContainerGap(45, Short.MAX_VALUE)
                .addComponent(RB_StopY_UnSignal_IntPropWindow)
                .addGap(40, 40, 40)
                .addComponent(RB_StopN_UnSignal_IntPropWindow)
                .addGap(65, 65, 65))
        );
        P_Stop_UnSignal_IntPropWindowLayout.setVerticalGroup(
            P_Stop_UnSignal_IntPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_Stop_UnSignal_IntPropWindowLayout.createSequentialGroup()
                .addContainerGap(22, Short.MAX_VALUE)
                .addGroup(P_Stop_UnSignal_IntPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(RB_StopY_UnSignal_IntPropWindow)
                    .addComponent(RB_StopN_UnSignal_IntPropWindow))
                .addGap(17, 17, 17))
        );

        javax.swing.GroupLayout P_Unsignal_IntPropWindowLayout = new javax.swing.GroupLayout(P_Unsignal_IntPropWindow);
        P_Unsignal_IntPropWindow.setLayout(P_Unsignal_IntPropWindowLayout);
        P_Unsignal_IntPropWindowLayout.setHorizontalGroup(
            P_Unsignal_IntPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_Unsignal_IntPropWindowLayout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(P_Stop_UnSignal_IntPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        P_Unsignal_IntPropWindowLayout.setVerticalGroup(
            P_Unsignal_IntPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_Unsignal_IntPropWindowLayout.createSequentialGroup()
                .addGap(61, 61, 61)
                .addComponent(P_Stop_UnSignal_IntPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(59, Short.MAX_VALUE))
        );

        P_Stop_UnSignal_IntPropWindow.getAccessibleContext().setAccessibleName(resourceMap.getString("jPanel6.AccessibleContext.accessibleName")); // NOI18N

        TP_IntPropWindow.addTab(resourceMap.getString("P_Unsignal_IntPropWindow.TabConstraints.tabTitle"), P_Unsignal_IntPropWindow); // NOI18N

        P_OKCancel_IntPropWindow.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        P_OKCancel_IntPropWindow.setName("P_OKCancel_IntPropWindow"); // NOI18N

        B_OK_IntPropWindow.setAction(actionMap.get("setIntersectionSpecProps")); // NOI18N
        B_OK_IntPropWindow.setText(resourceMap.getString("B_OK_IntPropWindow.text")); // NOI18N
        B_OK_IntPropWindow.setName("B_OK_IntPropWindow"); // NOI18N

        B_Cancel_IntPropWindow.setAction(actionMap.get("cancelIntersectionSpecProps")); // NOI18N
        B_Cancel_IntPropWindow.setText(resourceMap.getString("B_Cancel_IntPropWindow.text")); // NOI18N
        B_Cancel_IntPropWindow.setName("B_Cancel_IntPropWindow"); // NOI18N

        javax.swing.GroupLayout P_OKCancel_IntPropWindowLayout = new javax.swing.GroupLayout(P_OKCancel_IntPropWindow);
        P_OKCancel_IntPropWindow.setLayout(P_OKCancel_IntPropWindowLayout);
        P_OKCancel_IntPropWindowLayout.setHorizontalGroup(
            P_OKCancel_IntPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_OKCancel_IntPropWindowLayout.createSequentialGroup()
                .addGap(65, 65, 65)
                .addComponent(B_OK_IntPropWindow)
                .addGap(38, 38, 38)
                .addComponent(B_Cancel_IntPropWindow)
                .addContainerGap(64, Short.MAX_VALUE))
        );
        P_OKCancel_IntPropWindowLayout.setVerticalGroup(
            P_OKCancel_IntPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_OKCancel_IntPropWindowLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(P_OKCancel_IntPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(B_OK_IntPropWindow)
                    .addComponent(B_Cancel_IntPropWindow))
                .addContainerGap(14, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout F_IntPropWindowLayout = new javax.swing.GroupLayout(F_IntPropWindow.getContentPane());
        F_IntPropWindow.getContentPane().setLayout(F_IntPropWindowLayout);
        F_IntPropWindowLayout.setHorizontalGroup(
            F_IntPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, F_IntPropWindowLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(F_IntPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(P_OKCancel_IntPropWindow, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(TP_IntPropWindow, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 285, Short.MAX_VALUE))
                .addContainerGap())
        );
        F_IntPropWindowLayout.setVerticalGroup(
            F_IntPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(F_IntPropWindowLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(TP_IntPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, 237, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(P_OKCancel_IntPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        F_ConPropWindow.setTitle(resourceMap.getString("F_ConPropWindow.title")); // NOI18N
        F_ConPropWindow.setName("F_ConPropWindow"); // NOI18N
        F_ConPropWindow.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                F_ConPropWindowWindowOpened(evt);
            }
        });

        TP_ConPropWindow.setName("TP_ConPropWindow"); // NOI18N

        P_RdPrms_ConPropWindow.setName("P_RdPrms_ConPropWindow"); // NOI18N

        L_Lanes_RdPrms_ConPropWindow.setText(resourceMap.getString("L_Lanes_RdPrms_ConPropWindow.text")); // NOI18N
        L_Lanes_RdPrms_ConPropWindow.setName("L_Lanes_RdPrms_ConPropWindow"); // NOI18N

        CB_Lanes_RdPrms_ConPropWindow.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6" }));
        CB_Lanes_RdPrms_ConPropWindow.setSelectedIndex(2);
        CB_Lanes_RdPrms_ConPropWindow.setName("CB_Lanes_RdPrms_ConPropWindow"); // NOI18N

        L_LLength_RdPrms_ConPropWindow.setText(resourceMap.getString("L_LLength_RdPrms_ConPropWindow.text")); // NOI18N
        L_LLength_RdPrms_ConPropWindow.setName("L_LLength_RdPrms_ConPropWindow"); // NOI18N

        TF_LLength_RdPrms_ConPropWindow.setText(resourceMap.getString("TF_LLength_RdPrms_ConPropWindow.text")); // NOI18N
        TF_LLength_RdPrms_ConPropWindow.setName("TF_LLength_RdPrms_ConPropWindow"); // NOI18N

        L_OnRamp_RdPrms_ConPropWindow.setText(resourceMap.getString("L_OnRamp_RdPrms_ConPropWindow.text")); // NOI18N
        L_OnRamp_RdPrms_ConPropWindow.setName("L_OnRamp_RdPrms_ConPropWindow"); // NOI18N

        CB_OnRamp_RdPrms_ConPropWindow.setText(resourceMap.getString("CB_OnRamp_RdPrms_ConPropWindow.text")); // NOI18N
        CB_OnRamp_RdPrms_ConPropWindow.setName("CB_OnRamp_RdPrms_ConPropWindow"); // NOI18N
        CB_OnRamp_RdPrms_ConPropWindow.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                CB_OnRamp_RdPrms_ConPropWindowStateChanged(evt);
            }
        });

        L_OffRamp_RdPrms_ConPropWindow.setText(resourceMap.getString("L_OffRamp_RdPrms_ConPropWindow.text")); // NOI18N
        L_OffRamp_RdPrms_ConPropWindow.setName("L_OffRamp_RdPrms_ConPropWindow"); // NOI18N

        CB_OffRamp_RdPrms_ConPropWindow.setText(resourceMap.getString("CB_OffRamp_RdPrms_ConPropWindow.text")); // NOI18N
        CB_OffRamp_RdPrms_ConPropWindow.setName("CB_OffRamp_RdPrms_ConPropWindow"); // NOI18N

        CB_AccLane_RdPrms_ConPropWindow.setText(resourceMap.getString("CB_AccLane_RdPrms_ConPropWindow.text")); // NOI18N
        CB_AccLane_RdPrms_ConPropWindow.setName("CB_AccLane_RdPrms_ConPropWindow"); // NOI18N

        L_AccLane_RdPrms_ConPropWindow.setText(resourceMap.getString("L_AccLane_RdPrms_ConPropWindow.text")); // NOI18N
        L_AccLane_RdPrms_ConPropWindow.setName("L_AccLane_RdPrms_ConPropWindow"); // NOI18N

        TF_AccLane_RdPrms_ConPropWindow.setText(resourceMap.getString("TF_AccLane_RdPrms_ConPropWindow.text")); // NOI18N
        TF_AccLane_RdPrms_ConPropWindow.setName("TF_AccLane_RdPrms_ConPropWindow"); // NOI18N

        CB_DecLane_RdPrms_ConPropWindow.setText(resourceMap.getString("CB_DecLane_RdPrms_ConPropWindow.text")); // NOI18N
        CB_DecLane_RdPrms_ConPropWindow.setName("CB_DecLane_RdPrms_ConPropWindow"); // NOI18N

        L_DecLane_RdPrms_ConPropWindow.setText(resourceMap.getString("L_DecLane_RdPrms_ConPropWindow.text")); // NOI18N
        L_DecLane_RdPrms_ConPropWindow.setName("L_DecLane_RdPrms_ConPropWindow"); // NOI18N

        TF_DecLane_RdPrms_ConPropWindow.setText(resourceMap.getString("TF_DecLane_RdPrms_ConPropWindow.text")); // NOI18N
        TF_DecLane_RdPrms_ConPropWindow.setName("TF_DecLane_RdPrms_ConPropWindow"); // NOI18N

        SB_Grade_RdPrms_ConPropWindow.setName("SB_Grade_RdPrms_ConPropWindow"); // NOI18N
        SB_Grade_RdPrms_ConPropWindow.setTxt1(resourceMap.getString("SB_Grade_RdPrms_ConPropWindow.txt1")); // NOI18N
        SB_Grade_RdPrms_ConPropWindow.setTxt2(resourceMap.getString("SB_Grade_RdPrms_ConPropWindow.txt2")); // NOI18N

        javax.swing.GroupLayout P_RdPrms_ConPropWindowLayout = new javax.swing.GroupLayout(P_RdPrms_ConPropWindow);
        P_RdPrms_ConPropWindow.setLayout(P_RdPrms_ConPropWindowLayout);
        P_RdPrms_ConPropWindowLayout.setHorizontalGroup(
            P_RdPrms_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_RdPrms_ConPropWindowLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(P_RdPrms_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(P_RdPrms_ConPropWindowLayout.createSequentialGroup()
                        .addGroup(P_RdPrms_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(P_RdPrms_ConPropWindowLayout.createSequentialGroup()
                                .addGroup(P_RdPrms_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(L_Lanes_RdPrms_ConPropWindow)
                                    .addComponent(L_OnRamp_RdPrms_ConPropWindow))
                                .addGap(46, 46, 46)
                                .addGroup(P_RdPrms_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(CB_Lanes_RdPrms_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(CB_OnRamp_RdPrms_ConPropWindow)))
                            .addGroup(P_RdPrms_ConPropWindowLayout.createSequentialGroup()
                                .addComponent(CB_AccLane_RdPrms_ConPropWindow)
                                .addGap(12, 12, 12)
                                .addComponent(L_AccLane_RdPrms_ConPropWindow)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(TF_AccLane_RdPrms_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(10, 10, 10)
                        .addGroup(P_RdPrms_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(P_RdPrms_ConPropWindowLayout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(CB_DecLane_RdPrms_ConPropWindow)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(L_DecLane_RdPrms_ConPropWindow)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(TF_DecLane_RdPrms_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(P_RdPrms_ConPropWindowLayout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addGroup(P_RdPrms_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(L_LLength_RdPrms_ConPropWindow)
                                    .addComponent(L_OffRamp_RdPrms_ConPropWindow))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 26, Short.MAX_VALUE)
                                .addGroup(P_RdPrms_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(TF_LLength_RdPrms_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(CB_OffRamp_RdPrms_ConPropWindow))
                                .addGap(76, 76, 76))))
                    .addComponent(SB_Grade_RdPrms_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        P_RdPrms_ConPropWindowLayout.setVerticalGroup(
            P_RdPrms_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_RdPrms_ConPropWindowLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(P_RdPrms_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(L_Lanes_RdPrms_ConPropWindow)
                    .addComponent(CB_Lanes_RdPrms_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(L_LLength_RdPrms_ConPropWindow)
                    .addComponent(TF_LLength_RdPrms_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(30, 30, 30)
                .addGroup(P_RdPrms_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(L_OnRamp_RdPrms_ConPropWindow)
                    .addComponent(CB_OnRamp_RdPrms_ConPropWindow)
                    .addGroup(P_RdPrms_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(CB_OffRamp_RdPrms_ConPropWindow)
                        .addComponent(L_OffRamp_RdPrms_ConPropWindow)))
                .addGap(21, 21, 21)
                .addGroup(P_RdPrms_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(P_RdPrms_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(CB_AccLane_RdPrms_ConPropWindow)
                        .addGroup(P_RdPrms_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(P_RdPrms_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(TF_AccLane_RdPrms_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(L_AccLane_RdPrms_ConPropWindow))
                            .addGroup(P_RdPrms_ConPropWindowLayout.createSequentialGroup()
                                .addGap(1, 1, 1)
                                .addGroup(P_RdPrms_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(L_DecLane_RdPrms_ConPropWindow)
                                    .addComponent(TF_DecLane_RdPrms_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addComponent(CB_DecLane_RdPrms_ConPropWindow))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 18, Short.MAX_VALUE)
                .addComponent(SB_Grade_RdPrms_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(26, 26, 26))
        );

        TP_ConPropWindow.addTab(resourceMap.getString("P_RdPrms_ConPropWindow.TabConstraints.tabTitle"), P_RdPrms_ConPropWindow); // NOI18N

        P_RM_ConPropWindow.setName("P_RM_ConPropWindow"); // NOI18N

        SB_MR_Ramp_ConPropWindow.setSMax(1800);
        SB_MR_Ramp_ConPropWindow.setName("SB_MR_Ramp_ConPropWindow"); // NOI18N
        SB_MR_Ramp_ConPropWindow.setTxt1(resourceMap.getString("SB_MR_Ramp_ConPropWindow.txt1")); // NOI18N
        SB_MR_Ramp_ConPropWindow.setTxt2(resourceMap.getString("SB_MR_Ramp_ConPropWindow.txt2")); // NOI18N
        SB_MR_Ramp_ConPropWindow.setValue(1200);

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        TF_MaxMR.setText(resourceMap.getString("TF_MaxMR.text")); // NOI18N
        TF_MaxMR.setName("TF_MaxMR"); // NOI18N

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        TF_MinMR.setText(resourceMap.getString("TF_MinMR.text")); // NOI18N
        TF_MinMR.setName("TF_MinMR"); // NOI18N

        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N

        TF_FlushDown.setText(resourceMap.getString("TF_FlushDown.text")); // NOI18N
        TF_FlushDown.setName("TF_FlushDown"); // NOI18N

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N

        TF_FlushUp.setText(resourceMap.getString("TF_FlushUp.text")); // NOI18N
        TF_FlushUp.setName("TF_FlushUp"); // NOI18N

        jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N

        TF_Storage.setText(resourceMap.getString("TF_Storage.text")); // NOI18N
        TF_Storage.setName("TF_Storage"); // NOI18N

        buttonGroup3.add(R_Alinea);
        R_Alinea.setSelected(true);
        R_Alinea.setText(resourceMap.getString("R_Alinea.text")); // NOI18N
        R_Alinea.setName("R_Alinea"); // NOI18N
        R_Alinea.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                R_AlineaStateChanged(evt);
            }
        });

        buttonGroup3.add(R_SWARM1);
        R_SWARM1.setText(resourceMap.getString("R_SWARM1.text")); // NOI18N
        R_SWARM1.setName("R_SWARM1"); // NOI18N

        buttonGroup3.add(R_SWARM12);
        R_SWARM12.setText(resourceMap.getString("R_SWARM12.text")); // NOI18N
        R_SWARM12.setName("R_SWARM12"); // NOI18N

        jLabel6.setText(resourceMap.getString("jLabel6.text")); // NOI18N
        jLabel6.setName("jLabel6"); // NOI18N

        CB_Flush.setSelected(true);
        CB_Flush.setText(resourceMap.getString("CB_Flush.text")); // NOI18N
        CB_Flush.setName("CB_Flush"); // NOI18N

        jLabel7.setText(resourceMap.getString("jLabel7.text")); // NOI18N
        jLabel7.setName("jLabel7"); // NOI18N

        TF_BN.setText(resourceMap.getString("TF_BN.text")); // NOI18N
        TF_BN.setName("TF_BN"); // NOI18N

        javax.swing.GroupLayout P_RM_ConPropWindowLayout = new javax.swing.GroupLayout(P_RM_ConPropWindow);
        P_RM_ConPropWindow.setLayout(P_RM_ConPropWindowLayout);
        P_RM_ConPropWindowLayout.setHorizontalGroup(
            P_RM_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_RM_ConPropWindowLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(P_RM_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(SB_MR_Ramp_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(P_RM_ConPropWindowLayout.createSequentialGroup()
                        .addGroup(P_RM_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 20, Short.MAX_VALUE)
                        .addGroup(P_RM_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(TF_Storage)
                            .addComponent(TF_MaxMR)
                            .addComponent(CB_Flush))
                        .addGap(18, 18, 18)
                        .addGroup(P_RM_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(P_RM_ConPropWindowLayout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 53, Short.MAX_VALUE)
                                .addComponent(TF_MinMR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, P_RM_ConPropWindowLayout.createSequentialGroup()
                                .addGroup(P_RM_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel4)
                                    .addComponent(jLabel6))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 28, Short.MAX_VALUE)
                                .addGroup(P_RM_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(TF_FlushDown)
                                    .addComponent(TF_FlushUp)))))
                    .addGroup(P_RM_ConPropWindowLayout.createSequentialGroup()
                        .addComponent(R_Alinea)
                        .addGap(10, 10, 10)
                        .addComponent(R_SWARM1)
                        .addGap(10, 10, 10)
                        .addComponent(R_SWARM12)
                        .addGap(12, 12, 12)
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(TF_BN, javax.swing.GroupLayout.DEFAULT_SIZE, 20, Short.MAX_VALUE)))
                .addContainerGap())
        );
        P_RM_ConPropWindowLayout.setVerticalGroup(
            P_RM_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_RM_ConPropWindowLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(SB_MR_Ramp_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(P_RM_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addGroup(P_RM_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel2)
                        .addComponent(TF_MinMR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(TF_MaxMR, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(P_RM_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addGroup(P_RM_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel4)
                        .addComponent(TF_FlushUp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(CB_Flush))
                .addGroup(P_RM_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(P_RM_ConPropWindowLayout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(P_RM_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel5)
                            .addComponent(TF_Storage, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6)))
                    .addGroup(P_RM_ConPropWindowLayout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(TF_FlushDown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(P_RM_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(R_Alinea)
                    .addComponent(R_SWARM12)
                    .addComponent(TF_BN, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7)
                    .addComponent(R_SWARM1))
                .addContainerGap(13, Short.MAX_VALUE))
        );

        TP_ConPropWindow.addTab(resourceMap.getString("P_RM_ConPropWindow.TabConstraints.tabTitle"), P_RM_ConPropWindow); // NOI18N

        P_CF_ConPropWindow.setName("P_CF_ConPropWindow"); // NOI18N

        SB_MaxSpd_CF_ConPropWindow.setName("SB_MaxSpd_CF_ConPropWindow"); // NOI18N
        SB_MaxSpd_CF_ConPropWindow.setTxt1(resourceMap.getString("SB_MaxSpd_CF_ConPropWindow.txt1")); // NOI18N
        SB_MaxSpd_CF_ConPropWindow.setTxt2(resourceMap.getString("SB_MaxSpd_CF_ConPropWindow.txt2")); // NOI18N
        SB_MaxSpd_CF_ConPropWindow.setValue(100);

        SB_JamDen_CF_ConPropWindow.setSMax(200);
        SB_JamDen_CF_ConPropWindow.setName("SB_JamDen_CF_ConPropWindow"); // NOI18N
        SB_JamDen_CF_ConPropWindow.setTxt1(resourceMap.getString("SB_JamDen_CF_ConPropWindow.txt1")); // NOI18N
        SB_JamDen_CF_ConPropWindow.setTxt2(resourceMap.getString("SB_JamDen_CF_ConPropWindow.txt2")); // NOI18N
        SB_JamDen_CF_ConPropWindow.setValue(150);

        SB_WaveSpd_CF_ConPropWindow.setSMax(40);
        SB_WaveSpd_CF_ConPropWindow.setName("SB_WaveSpd_CF_ConPropWindow"); // NOI18N
        SB_WaveSpd_CF_ConPropWindow.setTxt1(resourceMap.getString("SB_WaveSpd_CF_ConPropWindow.txt1")); // NOI18N
        SB_WaveSpd_CF_ConPropWindow.setTxt2(resourceMap.getString("SB_WaveSpd_CF_ConPropWindow.txt2")); // NOI18N
        SB_WaveSpd_CF_ConPropWindow.setValue(20);

        javax.swing.GroupLayout P_CF_ConPropWindowLayout = new javax.swing.GroupLayout(P_CF_ConPropWindow);
        P_CF_ConPropWindow.setLayout(P_CF_ConPropWindowLayout);
        P_CF_ConPropWindowLayout.setHorizontalGroup(
            P_CF_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_CF_ConPropWindowLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(P_CF_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(SB_MaxSpd_CF_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(SB_JamDen_CF_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(SB_WaveSpd_CF_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(107, Short.MAX_VALUE))
        );
        P_CF_ConPropWindowLayout.setVerticalGroup(
            P_CF_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_CF_ConPropWindowLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(SB_MaxSpd_CF_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(SB_JamDen_CF_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(SB_WaveSpd_CF_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(31, Short.MAX_VALUE))
        );

        TP_ConPropWindow.addTab(resourceMap.getString("P_CF_ConPropWindow.TabConstraints.tabTitle"), P_CF_ConPropWindow); // NOI18N

        P_LC_ConPropWindow.setName("P_LC_ConPropWindow"); // NOI18N

        SB_Epsilon_LC_ConPropWindow.setSMax(10);
        SB_Epsilon_LC_ConPropWindow.setName("SB_Epsilon_LC_ConPropWindow"); // NOI18N
        SB_Epsilon_LC_ConPropWindow.setTxt1(resourceMap.getString("SB_Epsilon_LC_ConPropWindow.txt1")); // NOI18N
        SB_Epsilon_LC_ConPropWindow.setTxt2(resourceMap.getString("SB_Epsilon_LC_ConPropWindow.txt2")); // NOI18N
        SB_Epsilon_LC_ConPropWindow.setValue(2);

        SB_Tau_LC_ConPropWindow.setSMax(10);
        SB_Tau_LC_ConPropWindow.setName("SB_Tau_LC_ConPropWindow"); // NOI18N
        SB_Tau_LC_ConPropWindow.setTxt1(resourceMap.getString("SB_Tau_LC_ConPropWindow.txt1")); // NOI18N
        SB_Tau_LC_ConPropWindow.setTxt2(resourceMap.getString("SB_Tau_LC_ConPropWindow.txt2")); // NOI18N
        SB_Tau_LC_ConPropWindow.setValue(4);

        SB_Sd_LC_ConPropWindow.setSMax(1000);
        SB_Sd_LC_ConPropWindow.setName("SB_Sd_LC_ConPropWindow"); // NOI18N
        SB_Sd_LC_ConPropWindow.setTxt1(resourceMap.getString("SB_Sd_LC_ConPropWindow.txt1")); // NOI18N
        SB_Sd_LC_ConPropWindow.setTxt2(resourceMap.getString("SB_Sd_LC_ConPropWindow.txt2")); // NOI18N
        SB_Sd_LC_ConPropWindow.setValue(200);

        javax.swing.GroupLayout P_LC_ConPropWindowLayout = new javax.swing.GroupLayout(P_LC_ConPropWindow);
        P_LC_ConPropWindow.setLayout(P_LC_ConPropWindowLayout);
        P_LC_ConPropWindowLayout.setHorizontalGroup(
            P_LC_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_LC_ConPropWindowLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(P_LC_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(SB_Sd_LC_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, P_LC_ConPropWindowLayout.createSequentialGroup()
                        .addGroup(P_LC_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(SB_Tau_LC_ConPropWindow, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(SB_Epsilon_LC_ConPropWindow, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(139, 139, 139)))
                .addContainerGap())
        );
        P_LC_ConPropWindowLayout.setVerticalGroup(
            P_LC_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_LC_ConPropWindowLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(SB_Epsilon_LC_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(SB_Tau_LC_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(SB_Sd_LC_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(31, Short.MAX_VALUE))
        );

        TP_ConPropWindow.addTab(resourceMap.getString("P_LC_ConPropWindow.TabConstraints.tabTitle"), P_LC_ConPropWindow); // NOI18N

        P_OkCancel_ConPropWindow.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        P_OkCancel_ConPropWindow.setName("P_OkCancel_ConPropWindow"); // NOI18N

        B_Ok_ConPropWindow.setAction(actionMap.get("updateConnectionProperties")); // NOI18N
        B_Ok_ConPropWindow.setText(resourceMap.getString("B_Ok_ConPropWindow.text")); // NOI18N
        B_Ok_ConPropWindow.setName("B_Ok_ConPropWindow"); // NOI18N

        B_Cancel_ConPropWindow.setAction(actionMap.get("cancelConnectionPropWindow")); // NOI18N
        B_Cancel_ConPropWindow.setText(resourceMap.getString("B_Cancel_ConPropWindow.text")); // NOI18N
        B_Cancel_ConPropWindow.setName("B_Cancel_ConPropWindow"); // NOI18N

        javax.swing.GroupLayout P_OkCancel_ConPropWindowLayout = new javax.swing.GroupLayout(P_OkCancel_ConPropWindow);
        P_OkCancel_ConPropWindow.setLayout(P_OkCancel_ConPropWindowLayout);
        P_OkCancel_ConPropWindowLayout.setHorizontalGroup(
            P_OkCancel_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, P_OkCancel_ConPropWindowLayout.createSequentialGroup()
                .addContainerGap(65, Short.MAX_VALUE)
                .addComponent(B_Ok_ConPropWindow)
                .addGap(53, 53, 53)
                .addComponent(B_Cancel_ConPropWindow)
                .addGap(53, 53, 53))
        );
        P_OkCancel_ConPropWindowLayout.setVerticalGroup(
            P_OkCancel_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_OkCancel_ConPropWindowLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(P_OkCancel_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(B_Ok_ConPropWindow)
                    .addComponent(B_Cancel_ConPropWindow))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout F_ConPropWindowLayout = new javax.swing.GroupLayout(F_ConPropWindow.getContentPane());
        F_ConPropWindow.getContentPane().setLayout(F_ConPropWindowLayout);
        F_ConPropWindowLayout.setHorizontalGroup(
            F_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(F_ConPropWindowLayout.createSequentialGroup()
                .addGroup(F_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(F_ConPropWindowLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(TP_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, 363, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(F_ConPropWindowLayout.createSequentialGroup()
                        .addGap(40, 40, 40)
                        .addComponent(P_OkCancel_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        F_ConPropWindowLayout.setVerticalGroup(
            F_ConPropWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(F_ConPropWindowLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(TP_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, 248, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(P_OkCancel_ConPropWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        F_SetNtwrkParams.setTitle(resourceMap.getString("F_SetNtwrkParams.title")); // NOI18N
        F_SetNtwrkParams.setName("F_SetNtwrkParams"); // NOI18N

        P_SetNtwrkParams.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        P_SetNtwrkParams.setName("P_SetNtwrkParams"); // NOI18N

        L_ID_SetNtwrkParams.setText(resourceMap.getString("L_ID_SetNtwrkParams.text")); // NOI18N
        L_ID_SetNtwrkParams.setName("L_ID_SetNtwrkParams"); // NOI18N

        F_ID_SetNtwrkParams.setText(resourceMap.getString("F_ID_SetNtwrkParams.text")); // NOI18N
        F_ID_SetNtwrkParams.setToolTipText(resourceMap.getString("F_ID_SetNtwrkParams.toolTipText")); // NOI18N
        F_ID_SetNtwrkParams.setName("F_ID_SetNtwrkParams"); // NOI18N

        L_TP_SetNtwrkParams.setText(resourceMap.getString("L_TP_SetNtwrkParams.text")); // NOI18N
        L_TP_SetNtwrkParams.setName("L_TP_SetNtwrkParams"); // NOI18N

        F_TP_SetNtwrkParams.setText(resourceMap.getString("F_TP_SetNtwrkParams.text")); // NOI18N
        F_TP_SetNtwrkParams.setName("F_TP_SetNtwrkParams"); // NOI18N

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(null, resourceMap.getString("jPanel3.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, resourceMap.getFont("jPanel3.border.titleFont"), resourceMap.getColor("jPanel3.border.titleColor"))); // NOI18N
        jPanel3.setName("jPanel3"); // NOI18N

        buttonGroup4.add(RB_Relax);
        RB_Relax.setSelected(true);
        RB_Relax.setText(resourceMap.getString("RB_Relax.text")); // NOI18N
        RB_Relax.setName("RB_Relax"); // NOI18N

        buttonGroup4.add(RB_GapAcc1);
        RB_GapAcc1.setText(resourceMap.getString("RB_GapAcc1.text")); // NOI18N
        RB_GapAcc1.setName("RB_GapAcc1"); // NOI18N

        buttonGroup4.add(RB_GapAcc2);
        RB_GapAcc2.setText(resourceMap.getString("RB_GapAcc2.text")); // NOI18N
        RB_GapAcc2.setName("RB_GapAcc2"); // NOI18N

        L_MinSpdDiff_SetNtwrkParams.setText(resourceMap.getString("L_MinSpdDiff_SetNtwrkParams.text")); // NOI18N
        L_MinSpdDiff_SetNtwrkParams.setName("L_MinSpdDiff_SetNtwrkParams"); // NOI18N

        F_MinSpdDiff_SetNtwrkParams.setText(resourceMap.getString("F_MinSpdDiff_SetNtwrkParams.text")); // NOI18N
        F_MinSpdDiff_SetNtwrkParams.setName("F_MinSpdDiff_SetNtwrkParams"); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(RB_Relax)
                        .addGap(18, 18, 18)
                        .addComponent(RB_GapAcc1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 3, Short.MAX_VALUE)
                        .addComponent(RB_GapAcc2))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(L_MinSpdDiff_SetNtwrkParams)
                        .addGap(29, 29, 29)
                        .addComponent(F_MinSpdDiff_SetNtwrkParams, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(L_MinSpdDiff_SetNtwrkParams)
                    .addComponent(F_MinSpdDiff_SetNtwrkParams, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(RB_Relax)
                    .addComponent(RB_GapAcc2)
                    .addComponent(RB_GapAcc1)))
        );

        javax.swing.GroupLayout P_SetNtwrkParamsLayout = new javax.swing.GroupLayout(P_SetNtwrkParams);
        P_SetNtwrkParams.setLayout(P_SetNtwrkParamsLayout);
        P_SetNtwrkParamsLayout.setHorizontalGroup(
            P_SetNtwrkParamsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_SetNtwrkParamsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(P_SetNtwrkParamsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(P_SetNtwrkParamsLayout.createSequentialGroup()
                        .addGroup(P_SetNtwrkParamsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(L_ID_SetNtwrkParams)
                            .addComponent(L_TP_SetNtwrkParams))
                        .addGap(109, 109, 109)
                        .addGroup(P_SetNtwrkParamsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(F_TP_SetNtwrkParams, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(F_ID_SetNtwrkParams, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        P_SetNtwrkParamsLayout.setVerticalGroup(
            P_SetNtwrkParamsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(P_SetNtwrkParamsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(P_SetNtwrkParamsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(L_ID_SetNtwrkParams)
                    .addComponent(F_ID_SetNtwrkParams, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(P_SetNtwrkParamsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(L_TP_SetNtwrkParams)
                    .addComponent(F_TP_SetNtwrkParams, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        P_OKCancel_SetNtwrkParams.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        P_OKCancel_SetNtwrkParams.setName("P_OKCancel_SetNtwrkParams"); // NOI18N

        B_OK_SetNtwrkParams.setAction(actionMap.get("setNtwrkParams")); // NOI18N
        B_OK_SetNtwrkParams.setText(resourceMap.getString("B_OK_SetNtwrkParams.text")); // NOI18N
        B_OK_SetNtwrkParams.setName("B_OK_SetNtwrkParams"); // NOI18N

        B_Cancel_SetNtwrkParams.setAction(actionMap.get("cancelNtwrkParams")); // NOI18N
        B_Cancel_SetNtwrkParams.setText(resourceMap.getString("B_Cancel_SetNtwrkParams.text")); // NOI18N
        B_Cancel_SetNtwrkParams.setName("B_Cancel_SetNtwrkParams"); // NOI18N

        javax.swing.GroupLayout P_OKCancel_SetNtwrkParamsLayout = new javax.swing.GroupLayout(P_OKCancel_SetNtwrkParams);
        P_OKCancel_SetNtwrkParams.setLayout(P_OKCancel_SetNtwrkParamsLayout);
        P_OKCancel_SetNtwrkParamsLayout.setHorizontalGroup(
            P_OKCancel_SetNtwrkParamsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, P_OKCancel_SetNtwrkParamsLayout.createSequentialGroup()
                .addContainerGap(56, Short.MAX_VALUE)
                .addComponent(B_OK_SetNtwrkParams)
                .addGap(34, 34, 34)
                .addComponent(B_Cancel_SetNtwrkParams)
                .addGap(66, 66, 66))
        );
        P_OKCancel_SetNtwrkParamsLayout.setVerticalGroup(
            P_OKCancel_SetNtwrkParamsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, P_OKCancel_SetNtwrkParamsLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(P_OKCancel_SetNtwrkParamsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(B_Cancel_SetNtwrkParams)
                    .addComponent(B_OK_SetNtwrkParams))
                .addContainerGap())
        );

        javax.swing.GroupLayout F_SetNtwrkParamsLayout = new javax.swing.GroupLayout(F_SetNtwrkParams.getContentPane());
        F_SetNtwrkParams.getContentPane().setLayout(F_SetNtwrkParamsLayout);
        F_SetNtwrkParamsLayout.setHorizontalGroup(
            F_SetNtwrkParamsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(F_SetNtwrkParamsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(F_SetNtwrkParamsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(P_SetNtwrkParams, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(P_OKCancel_SetNtwrkParams, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        F_SetNtwrkParamsLayout.setVerticalGroup(
            F_SetNtwrkParamsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(F_SetNtwrkParamsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(P_SetNtwrkParams, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(P_OKCancel_SetNtwrkParams, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        F_ControlWindow.setTitle(resourceMap.getString("F_ControlWindow.title")); // NOI18N
        F_ControlWindow.setAlwaysOnTop(true);
        F_ControlWindow.setName("F_ControlWindow"); // NOI18N

        SB_SimSpeed_ControlWindow.setName("SB_SimSpeed_ControlWindow"); // NOI18N
        SB_SimSpeed_ControlWindow.setTxt1(resourceMap.getString("SB_SimSpeed_ControlWindow.txt1")); // NOI18N
        SB_SimSpeed_ControlWindow.setTxt2(resourceMap.getString("SB_SimSpeed_ControlWindow.txt2")); // NOI18N
        SB_SimSpeed_ControlWindow.setValue(50);
        SB_SimSpeed_ControlWindow.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                SB_SimSpeed_ControlWindowPropertyChange(evt);
            }
        });

        SB_MR_ControlWindow.setSMax(1800);
        SB_MR_ControlWindow.setName("SB_MR_ControlWindow"); // NOI18N
        SB_MR_ControlWindow.setTxt1(resourceMap.getString("SB_MR_ControlWindow.txt1")); // NOI18N
        SB_MR_ControlWindow.setTxt2(resourceMap.getString("SB_MR_ControlWindow.txt2")); // NOI18N
        SB_MR_ControlWindow.setValue(1200);
        SB_MR_ControlWindow.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                SB_MR_ControlWindowPropertyChange(evt);
            }
        });

        javax.swing.GroupLayout F_ControlWindowLayout = new javax.swing.GroupLayout(F_ControlWindow.getContentPane());
        F_ControlWindow.getContentPane().setLayout(F_ControlWindowLayout);
        F_ControlWindowLayout.setHorizontalGroup(
            F_ControlWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(F_ControlWindowLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(F_ControlWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(SB_SimSpeed_ControlWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(SB_MR_ControlWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        F_ControlWindowLayout.setVerticalGroup(
            F_ControlWindowLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(F_ControlWindowLayout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addComponent(SB_SimSpeed_ControlWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(SB_MR_ControlWindow, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        progressBar.setName("progressBar"); // NOI18N

        setComponent(mainPanel);
        setMenuBar(menuBar);
        setStatusBar(statusPanel);
    }// </editor-fold>//GEN-END:initComponents

    private void jXMapKit1MousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jXMapKit1MousePressed
// Right Click
        if(evt.getButton() == MouseEvent.BUTTON3){
            WaypointIntersection wp = new WaypointIntersection(jXMapKit1.getMainMap().convertPointToGeoPosition(new Point2D.Double(evt.getX(),evt.getY())));
            WaypointIntersection wpN = getNearestWaypoint(wp,15);
            WaypointConnection wpNC = getNearestWaypointConnection(wp,15);
            
            // Edit an intersection
            if(wpN != null){
                // Edit the properties of an Intersection
                
                Intersection intrscn = intMap.get(wpN);
                toBeUpdatedWaypoint = wpN;
                
                F_IntProperties.setBounds(evt.getX()+35, evt.getY()+45, 350, 280);
                F_IntProperties.setResizable(false);
                F_IntProperties.setTitle("Intersection");
                
                CB_Type_IntWindow.setSelectedIndex(intrscn.type);
                TF_Name_IntWindow.setText(intrscn.intersectionName);
                
                F_IntProperties.setVisible(true);
            }
            // Edit a connection
            else if(wpNC != null){
                // Edit the properties of a Connection
                
                Connection con = conMap.get(wpNC);
                toBeUpdatedConnection = wpNC;
                
                F_ConPropWindow.setBounds(evt.getX()+35, evt.getY()+45, 390, 350);
                F_ConPropWindow.setResizable(false);
                F_ConPropWindow.setTitle("Connection");
                
                CB_Lanes_RdPrms_ConPropWindow.setSelectedIndex((int)con.nLanes - 1);
                TF_LLength_RdPrms_ConPropWindow.setText("" + (con.length*1000.0));
                F_MinSpdDiff_SetNtwrkParams.setText(""+Connection.minSpeedDiff);
                SB_MR_Ramp_ConPropWindow.setValue((int)con.RM.meterRate);
                TF_MaxMR.setText("" + con.RM.AbsMaxRate);
                TF_MinMR.setText("" + con.RM.AbsMinRate);
                TF_FlushDown.setText("" + con.RM.flushUpOcc);
                TF_FlushUp.setText("" + con.RM.flushDownOcc);            
                TF_Storage.setText("" + (con.length*1000-200-con.RM.StorageBegin*1000)); //aaccleration lane is assumed to be 200 meters
                R_Alinea.setSelected(ModeManager.ALINEA);
                R_SWARM1.setSelected(ModeManager.SWARM1);
                R_SWARM12.setSelected(ModeManager.SWARM1AND2); 
                TF_AccLane_RdPrms_ConPropWindow.setText("" + (con.AccLaneLength*1000.0));        //added by Rama for Acceleration lane length
                TF_DecLane_RdPrms_ConPropWindow.setText("" + (con.DecLaneLength*1000.0));        //added by Rama for Deceleration lane length
                SB_Grade_RdPrms_ConPropWindow.setValue((int)(con.G*100.0));
                RB_Relax.setSelected(TrafficSimulationDesktopView.jrRelax);
                RB_GapAcc1.setSelected(TrafficSimulationDesktopView.jrGapacc1);
                RB_GapAcc2.setSelected(TrafficSimulationDesktopView.jrGapacc2);
                
                CB_OnRamp_RdPrms_ConPropWindow.setSelected(con.isOnRamp == 1);
                CB_OffRamp_RdPrms_ConPropWindow.setSelected(con.isOffRamp == 1);
                CB_AccLane_RdPrms_ConPropWindow.setSelected(con.hasAccLane == 1);
                CB_DecLane_RdPrms_ConPropWindow.setSelected(con.hasDecLane == 1);
                
                SB_MaxSpd_CF_ConPropWindow.setValue((int)con.u);
                SB_JamDen_CF_ConPropWindow.setValue((int)con.kj);
                SB_WaveSpd_CF_ConPropWindow.setValue((int)con.w);
                SB_Epsilon_LC_ConPropWindow.setValue((int)con.epsilon);
                SB_Tau_LC_ConPropWindow.setValue((int)(con.tau*3600.0));  // prev 100.0 was 3600.0
                SB_Sd_LC_ConPropWindow.setValue((int)(con.sd*1000.0));
                
                F_ConPropWindow.setVisible(true);
            }
        }
        // Normal click
        else{
            // Add intersection
            if(addIntersectionsFlag){ 
                WaypointIntersection wp = new WaypointIntersection(jXMapKit1.getMainMap().convertPointToGeoPosition(new Point2D.Double(evt.getX(),evt.getY())));
                Intersection intrscn = new Intersection(wp.getPosition().getLatitude(),wp.getPosition().getLongitude(),"["+roundDouble(wp.getPosition().getLatitude(),4)+","+roundDouble(wp.getPosition().getLongitude(),4)+"]");

                // Check if the point already exists
                if(getNearestWaypoint(wp,0.001) == null){
                    // Adding to Graph
                    graph.addVertex(intrscn);
                
                    // Adding to Map
                    intMap.put(wp, intrscn);
                
                    // Adding to the list of actions
                    if(actionIndex == actions.size())
                        actions.add(actionIndex, new ActionPerformed(0,wp,null,intrscn,null,null,actionIndex));
                    else
                        actions.set(actionIndex, new ActionPerformed(0,wp,null,intrscn,null,null,actionIndex));
                    actionIndex++;
                    
                    System.out.println("Added, Group = "+(actionIndex-1));
                    wpOverlay.setWaypoints(intMap.keySet());
                    
                    jXMapKit1.repaint();
                }
            }
            // Add connection
            else if(addConnectionsFlag){ 
                WaypointIntersection wpt = new WaypointIntersection(jXMapKit1.getMainMap().convertPointToGeoPosition(new Point2D.Double(evt.getX(),evt.getY())));
                WaypointIntersection wpn = getNearestWaypoint(wpt,25);
                
                if(wpn != null){    
                    if((waypointSelectedFlag) && (wpn != selectedWaypoint)){
                        waypointSelectedFlag = false;
                        Connection connctn = new Connection();
                        
                        // Check if an edge already exists between the two vertices
                        if(!graph.containsEdge(intMap.get(selectedWaypoint), intMap.get(wpn))){
                            // Adding to Graph
                            graph.addEdge(intMap.get(selectedWaypoint), intMap.get(wpn), connctn);
                            graph.setEdgeWeight(connctn,connctn.length);                        
                        
                            // Adding to Map
                            WaypointConnection wpcn = new WaypointConnection(selectedWaypoint, wpn);
                            conMap.put(wpcn,connctn);
                            
                            // Adding to the list of actions
                            if(actionIndex == actions.size())
                                actions.add(actionIndex, new ActionPerformed(1,null,wpcn,intMap.get(selectedWaypoint),connctn, intMap.get(wpn),actionIndex));
                            else
                                actions.set(actionIndex, new ActionPerformed(1,null,wpcn,intMap.get(selectedWaypoint),connctn, intMap.get(wpn),actionIndex));
                            actionIndex++;
                            
                            System.out.println("Added, Group = "+(actionIndex-1));
                        }
                    }
                    // Deselect an intersection
                    else if((waypointSelectedFlag) && (wpn == selectedWaypoint)){        
                        waypointSelectedFlag = false;
                    }
                    // Select an intersection for adding a connection
                    else{
                        selectedWaypoint = wpn;
                        waypointSelectedFlag = true;
                    }
                    
                    jXMapKit1.repaint();
                }
            }
            // Erase functionality
            else if(eraseFlag){
                WaypointIntersection wpt = new WaypointIntersection(jXMapKit1.getMainMap().convertPointToGeoPosition(new Point2D.Double(evt.getX(),evt.getY())));
                WaypointIntersection wpn = getNearestWaypoint(wpt,15);
                WaypointConnection wpntcc = getNearestWaypointConnection(wpt,15);
                
                //  Erase a Conncetion
                if(wpntcc != null){
                   // System.out.println("["+wpntcc.wp1+","+wpntcc.wp2+"]");
                   Connection conntntemp = conMap.get(wpntcc);
                           
                    // Adding to the list of actions
                   if(actionIndex == actions.size())
                        actions.add(actionIndex, new ActionPerformed(3,null,wpntcc,graph.getEdgeSource(conntntemp),conntntemp,graph.getEdgeTarget(conntntemp),actionIndex));
                   else
                        actions.set(actionIndex, new ActionPerformed(3,null,wpntcc,graph.getEdgeSource(conntntemp),conntntemp,graph.getEdgeTarget(conntntemp),actionIndex));
                    actionIndex++;
                    
                    System.out.println("Added, Group = "+(actionIndex-1));
                  
                   // Removing Edges from Graph
                   graph.removeEdge(conMap.get(wpntcc));
                    
                   // Removing the WaypointConnection-Connection Mapping        
                   conMap.remove(wpntcc);

                   jXMapKit1.repaint();
                }
                // Erase an Intersection
                else if(wpn != null){
                    Set<WaypointConnection> wpcSet = new HashSet<WaypointConnection>();
                    int tempActionIndex = actionIndex;
                     
                    for(WaypointConnection wpc : conMap.keySet()){
                        if((wpc.wp1 == wpn) || (wpc.wp2 == wpn)){
                            wpcSet.add(wpc);
                        }
                    }
                     
                    for(WaypointConnection wpc : wpcSet){
                         Connection conntntemp = conMap.get(wpc);
                        
                         // Adding to the list of actions
                         if(actionIndex == actions.size())
                            actions.add(actionIndex, new ActionPerformed(3,null,wpc,graph.getEdgeSource(conntntemp),conntntemp,graph.getEdgeTarget(conntntemp),tempActionIndex));
                         else
                             actions.set(actionIndex, new ActionPerformed(3,null,wpc,graph.getEdgeSource(conntntemp),conntntemp,graph.getEdgeTarget(conntntemp),tempActionIndex));
                         actionIndex++;
                         
                         // Removing Edges from Graph
                         graph.removeEdge(conMap.get(wpc));
                         
                         // Removing the WaypointConnection-Connection Mapping        
                         conMap.remove(wpc);
                         
                         System.out.println("While, Group = "+tempActionIndex);
                     }
                    
                    // Adding to the list of actions
                    if(actionIndex == actions.size())
                        actions.add(actionIndex, new ActionPerformed(2,wpn,null,intMap.get(wpn),null,null,tempActionIndex));
                    else
                        actions.set(actionIndex, new ActionPerformed(2,wpn,null,intMap.get(wpn),null,null,tempActionIndex));
                    actionIndex++;
                    System.out.println("Outside, Group = "+tempActionIndex);
                    
                    // Removing the Intersection(Vertex) from the Graph
                    graph.removeVertex(intMap.get(wpn));
                    
                    // Removing the Waypoint-Intersection Mapping
                    intMap.remove(wpn);
                     
                    wpOverlay.setWaypoints(intMap.keySet());
                    jXMapKit1.repaint();
                  }
            }
            // Select the intersection to see in the simulation - Visualization
            else{
                System.out.println("Simulation - Visualization");
                WaypointIntersection wpt = new WaypointIntersection(jXMapKit1.getMainMap().convertPointToGeoPosition(new Point2D.Double(evt.getX(),evt.getY())));
                WaypointIntersection wpn = getNearestWaypoint(wpt,25);
                
                if(wpn != null){
                    intToView = this.intMap.get(wpn);
                    //if(intToView.type == Intersection.AuxLane_RAMP)
                        F_ControlWindow.setVisible(true);
                    //else 
                      //  jFrame5.setVisible(false);
                    intToViewChanged = true;
                    for(Connection conTemp :graph.edgesOf(intToView))
                        if(conTemp.type == Connection.ORIGIN)
                            SB_MR_ControlWindow.setValue((int)conTemp.RM.meterRate);
                    System.out.println("Intersection to View changed to = "+intToView);
                    jXMapKit1.repaint();
                }
            }
        }

    }//GEN-LAST:event_jXMapKit1MousePressed

    private void CB_Type_IntWindowItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_CB_Type_IntWindowItemStateChanged
        if(CB_Type_IntWindow.getSelectedIndex() >= 4 && CB_Type_IntWindow.getSelectedIndex()<=8)
            B_Properties_IntWindow.setEnabled(false);
        else
            B_Properties_IntWindow.setEnabled(true);
    }//GEN-LAST:event_CB_Type_IntWindowItemStateChanged

    private void SB_SimSpeed_ControlWindowPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_SB_SimSpeed_ControlWindowPropertyChange
        updateInterval = SB_SimSpeed_ControlWindow.getSMax() - SB_SimSpeed_ControlWindow.getValue();
    }//GEN-LAST:event_SB_SimSpeed_ControlWindowPropertyChange

    private void SB_MR_ControlWindowPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_SB_MR_ControlWindowPropertyChange
        if(intToView != null){
//            intToView.meterRate = SB_MR_ControlWindow.getValue();
            for(Connection conTemp :graph.edgesOf(intToView)){
                if(conTemp.type == Connection.ORIGIN){
                    conTemp.RM.meterRate = SB_MR_ControlWindow.getValue();
//                    for(int ii = 0; ii < conTemp.nLanes; ii++){
//                        Lane l = conTemp.lanes[ii];
//                        l.meter = SB_MR_ControlWindow.getValue();
//                    }
                }
            }
        }
    }//GEN-LAST:event_SB_MR_ControlWindowPropertyChange

    private void CB_OnRamp_RdPrms_ConPropWindowStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_CB_OnRamp_RdPrms_ConPropWindowStateChanged
         TP_ConPropWindow.setEnabledAt(1, ((JCheckBox)(evt.getSource())).isSelected());
    }//GEN-LAST:event_CB_OnRamp_RdPrms_ConPropWindowStateChanged

    private void R_AlineaStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_R_AlineaStateChanged
       TF_BN.setEnabled(!((JRadioButton)(evt.getSource())).isSelected());
       jLabel7.setEnabled(!((JRadioButton)(evt.getSource())).isSelected());
    }//GEN-LAST:event_R_AlineaStateChanged

    private void F_ConPropWindowWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_F_ConPropWindowWindowOpened
        TF_BN.setEnabled(!R_Alinea.isSelected());
        jLabel7.setEnabled(!R_Alinea.isSelected());
        TP_ConPropWindow.setEnabledAt(1, CB_OnRamp_RdPrms_ConPropWindow.isSelected());
    }//GEN-LAST:event_F_ConPropWindowWindowOpened
     
    /* 
     * Adding Mouse Listener Functions
     */    
    private void addMouseListenerFunctions(){
        jXMapKit1.getMainMap().addMouseListener(new MouseListener(){
            public void mouseClicked(MouseEvent e) {isChanged = true;}
            public void mousePressed(MouseEvent e) {jXMapKit1MousePressed(e);}            
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}            
        });
    }
    
    @Action
    public void setNavigateFlag() {
        this.navigateFlag = true;
        this.addIntersectionsFlag = false;
        this.addConnectionsFlag = false;
        this.eraseFlag = false;
    }

    @Action
    public void setAddIntersectionFlag() {
        this.addIntersectionsFlag = true;
        this.addConnectionsFlag = false;
        this.navigateFlag = false;
        this.eraseFlag = false;
    }

    @Action
    public void setAddConnectionsFlag() {
        this.addConnectionsFlag = true;
        this.addIntersectionsFlag = false;
        this.navigateFlag = false;
        this.eraseFlag = false;
    }

    @Action
    public void eraseComponents() {
        this.eraseFlag = true;
        this.navigateFlag = false;
        this.addIntersectionsFlag = false;
        this.addConnectionsFlag = false;
    }

    @Action
    public void centerAtAtlanta() {
        jXMapKit1.setCenterPosition(new GeoPosition(33.76,-84.39));
//        jXMapKit1.setCenterPosition(new GeoPosition(33.91,-84.30));
        jXMapKit1.setZoom(2);
        
        System.out.println("Address Location = "+jXMapKit1.getAddressLocation());
        System.out.println("Center Location = "+jXMapKit1.getCenterPosition());
    }

    @Action
    public void redoActionProcedure() {
        if(actionIndex < actions.size()){
            int group = actions.get(actionIndex).group;
            int tempGroup = group;
            
            do{
                System.out.println("Redo, Group = "+group+", Temp Group = "+tempGroup);
                ActionPerformed ap = actions.get(actionIndex);
                WaypointIntersection wpi = ap.wpint;
                WaypointConnection wpc = ap.wpcon;
                Intersection intscn = ap.intscn;
                Connection connctn = ap.conctn;
                Intersection intscn1 = ap.intscn1;
                tempGroup = ap.group;
                
                if(tempGroup != group)
                    break;
                
                if(ap.type == 0){
                    // Adding to Graph
                    graph.addVertex(intscn);

                    // Adding to Map
                    intMap.put(wpi, intscn);

                }
                else if(ap.type == 1){
                    // Adding to Graph
                    graph.addEdge(intscn, intscn1, connctn);
                    graph.setEdgeWeight(connctn,connctn.length);                        

                    // Adding to Map
                    conMap.put(wpc,connctn);
                }
                else if(ap.type == 2){
                    // Removing the Intersection(Vertex) from the Graph
                    graph.removeVertex(intMap.get(wpi));

                    // Removing the Waypoint-Intersection Mapping
                    intMap.remove(wpi);
                }
                else if(ap.type == 3){
                    // Removing Edges from Graph
                    graph.removeEdge(conMap.get(wpc));

                    // Removing the WaypointConnection-Connection Mapping        
                    conMap.remove(wpc);
                }

                actionIndex++;
            }while((tempGroup == group) && (actionIndex < actions.size()));
        }
        else{
            System.out.println("Nothing to REDO");
        }
        
        jXMapKit1.repaint();
    }

    @Action
    public void undoActionProcedure() {
        if(actionIndex >0){
            int group = actions.get(actionIndex-1).group;
            int tempGroup = group;
            
            do{
                System.out.println("Undo, Group = "+group+", Temp Group = "+tempGroup);
                ActionPerformed ap = actions.get(actionIndex-1);
                WaypointIntersection wpi = ap.wpint;
                WaypointConnection wpc = ap.wpcon;
                Intersection intscn = ap.intscn;
                Connection connctn = ap.conctn;
                Intersection intscn1 = ap.intscn1;
                tempGroup = ap.group;
                
                if(tempGroup != group)
                    break;
        
                if(ap.type == 0){
                    // Removing the Intersection(Vertex) from the Graph
                    graph.removeVertex(intMap.get(wpi));
                    
                    // Removing the Waypoint-Intersection Mapping
                    intMap.remove(wpi);
                }
                else if(ap.type == 1){
                    // Removing Edges from Graph
                    graph.removeEdge(conMap.get(wpc));
                    
                    // Removing the WaypointConnection-Connection Mapping        
                    conMap.remove(wpc);
                }
                else if(ap.type == 2){
                    // Adding to Graph
                    graph.addVertex(intscn);
                
                    // Adding to Map
                    intMap.put(wpi, intscn);
                }
                else if(ap.type == 3){
                    // Adding to Graph
                    graph.addEdge(intscn, intscn1, connctn);
                    graph.setEdgeWeight(connctn,connctn.length);                        
                        
                    // Adding to Map
                    conMap.put(wpc,connctn);
                }
                
                actionIndex--;
                
            }while((tempGroup == group) && (actionIndex > 0));
        }
        else{
             System.out.println("Nothing to UNDO");
        }
        
        jXMapKit1.repaint();
    }
   public void run() {
        System.out.println("Inside run ... ");

        // Parameters are initialized only when we restart similation.
        if(initializeParam){

            // Set the graph
            System.out.println("\tSetting graph :"+graph);
            simulation1.setGraph(graph);

            // Create the Origin and Destination lists
            createOrigDestLists();

            // Set the Origin and Destination lists
            System.out.println("\tSetting Origin List : "+originList);
            simulation1.setOriginList(originList);
            System.out.println("\tSetting Destination List : "+destinationList);
            simulation1.setDestinationList(destinationList);

            // Show the input screen for inputting the origin-destination matrix
            inputObtained = false;
            showOrigDestInputWindow();

            //------push OK?

            while(!inputObtained){
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    System.out.println( "inside run"+ ex.getMessage());
                    //Logger.getLogger(TrafficSimulationDesktopView.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

            // Set the Origin-Destination Matrix. Each entry is veh/hr
            System.out.println("\t(Continuing from Setting Destination List)Setting the Origin-Destination Matrix");
            for(int i = 0; i < originList.size(); i++){
                System.out.print("[");
                System.out.print("{"+origDestMatrix[i][0]+"}");
                for(int j = 1; j < destinationList.size(); j++)
                    System.out.print(",{"+origDestMatrix[i][j]+"}");
                System.out.print("] ");
            }
            simulation1.setOrigDestMatrix(origDestMatrix);

            // Initialze Simulation
            System.out.println("\n\tInitializing Simulation");
            try{
            simulation1.initializeSimulation();
            } catch (Exception e) {
                System.out.println(
                        "inside run"+ e.getMessage());
            }

            F_SimWindow.setVisible(true);
            F_ControlWindow.setVisible(false);
        }

        // Run the simulation
        System.out.println("Running Simulation");

        while (running) {
            try{
            simulation1.runSimulation();                                        //1
            } catch (Exception e) {System.out.println("error in runSimulation"+e.getMessage());            }

            if(intToViewChanged){
                simulation1.init(simFrameWid, simFrameHei);
                simulation1.setIntersection(intToView);
                intToViewChanged = false;
            }

            if(!F_SimWindow.isVisible())
                F_SimWindow.setVisible(true);

            simulation1.paintAll();                                             //2
            t += simulation1.dt;
            try {
                Thread.sleep(updateInterval);
            }
            catch ( InterruptedException e ) {
		System.out.println("interrupted ... ");
                return;
            }
        }
    }

    @Action
    public void startSimulation() {

        File ntwrkFile = new File("C:\\Users\\hcho95\\rafegh.gph");  // Build it on your own
        System.out.println("File: "+ntwrkFile);


        navigateFlag = true;
        addIntersectionsFlag = false;
        addConnectionsFlag = false;
        eraseFlag = false;
        RB_Navigate.setSelected(true);

        fileName = ntwrkFile.getAbsolutePath();
        openGraph(ntwrkFile);

        //Initialize Undo and Redo Options
        actions = new ArrayList<ActionPerformed>();
        actionIndex = 0;

        navigateFlag = true;
        addIntersectionsFlag = false;
        addConnectionsFlag = false;
        eraseFlag = false;
        RB_Navigate.setSelected(true);
        RB_AddInt.setEnabled(false);
        RB_AddCon.setEnabled(false);
        RB_Erase.setEnabled(false);

//        if(!running){
//
//            if(initializeParam)
//                System.out.println("Starting simulation ... ");
//            else
//                System.out.println("Resuming simulation ... ");
//
//            running = true;
//            thread = new Thread(this);
//            thread.start();
//        }
//
//        // Set all the flags
        navigateFlag = true;
        addIntersectionsFlag = false;
        addConnectionsFlag = false;
        eraseFlag = false;
        RB_Navigate.setSelected(true);
        RB_AddInt.setEnabled(false);
        RB_AddCon.setEnabled(false);
        RB_Erase.setEnabled(false);

        if(!running){

            if(initializeParam)
                System.out.println("Starting simulation ... ");
            else
                System.out.println("Resuming simulation ... ");

            running = true;
            thread = new Thread(this);
            thread.start();
        }
    }

    @Action
    public void stopSimulation() {
        if(running){
            F_SimWindow.setVisible(false);
            F_ControlWindow.setVisible(false);
            running = false;
            thread.interrupt();
            initializeParam = true;
            System.out.println("Stopped simulation ... ");
        }
        
        // Set all the flags
        navigateFlag = true;
        addIntersectionsFlag = false;
        addConnectionsFlag = false;
        eraseFlag = false;
        RB_Navigate.setSelected(true);
        RB_AddInt.setEnabled(true);
        RB_AddCon.setEnabled(true);
        RB_Erase.setEnabled(true);
    }

    @Action
    public void pauseSimulation() {
         if(running){
            running = false;
            thread.interrupt();
            initializeParam = false;
            System.out.println("Paused simulation ... ");
        }
    }

    @Action
    public void changeIntersectionSpecProps() {        
        
        Intersection intscn = intMap.get(toBeUpdatedWaypoint);
        if(intscn != null){
            SB_Gore_Ramp_IntPropWindow.setValue((int)(intscn.gore*1000.0));
            SB_Sd_Ramp_IntPropWindow.setValue((int)intscn.sigDist);
            SB_Gore_Exit_IntPropWindow.setValue((int)(intscn.gore*1000.0));
            SB_Green_Signal_IntPropWindow.setValue((int)intscn.greenTime);
            SB_Red_Signal_IntPropWindow.setValue((int)intscn.redTime);
            SB_Offset_Signal_IntPropWindow.setValue((int)intscn.offset);
            if(intscn.hasStopSign){
                RB_StopY_UnSignal_IntPropWindow.setSelected(true);
            }
            else{
                RB_StopN_UnSignal_IntPropWindow.setSelected(true);
            }
        }
        else
            return;
        
        if(CB_Type_IntWindow.getSelectedIndex() == 0 ||CB_Type_IntWindow.getSelectedIndex() == 9){
            TP_IntPropWindow.setEnabledAt(0, true);
            TP_IntPropWindow.setEnabledAt(1, false);
            TP_IntPropWindow.setEnabledAt(2, false);
            TP_IntPropWindow.setEnabledAt(3, false);
            TP_IntPropWindow.setSelectedIndex(0);
        }
        else if(CB_Type_IntWindow.getSelectedIndex() == 1||CB_Type_IntWindow.getSelectedIndex() > 9){
            TP_IntPropWindow.setEnabledAt(0, false);
            TP_IntPropWindow.setEnabledAt(1, true);
            TP_IntPropWindow.setEnabledAt(2, false);
            TP_IntPropWindow.setEnabledAt(3, false);
            TP_IntPropWindow.setSelectedIndex(1);
        }
        else if(CB_Type_IntWindow.getSelectedIndex() == 2){
            TP_IntPropWindow.setEnabledAt(0, false);
            TP_IntPropWindow.setEnabledAt(1, false);
            TP_IntPropWindow.setEnabledAt(2, true);
            TP_IntPropWindow.setEnabledAt(3, false);
            TP_IntPropWindow.setSelectedIndex(2);
        }
        else if(CB_Type_IntWindow.getSelectedIndex() == 3){
            TP_IntPropWindow.setEnabledAt(0, false);
            TP_IntPropWindow.setEnabledAt(1, false);
            TP_IntPropWindow.setEnabledAt(2, false);
            TP_IntPropWindow.setEnabledAt(3, true);
            TP_IntPropWindow.setSelectedIndex(3);
        }
        
        F_IntPropWindow.setBounds(F_IntProperties.getX()+242, F_IntProperties.getY(), 320, 380);
        F_IntPropWindow.setResizable(false);
        F_IntPropWindow.setTitle("Props");        
        F_IntPropWindow.setVisible(true);
    }

    @Action
    public void updateIntersectionProperties() {
        Intersection intscn = intMap.get(toBeUpdatedWaypoint);
        if(intscn != null){
            intscn.intersectionName = TF_Name_IntWindow.getText().toLowerCase();
            intscn.type = CB_Type_IntWindow.getSelectedIndex();
        }
        
        F_IntProperties.setVisible(false);
        F_IntPropWindow.setVisible(false);
        jXMapKit1.repaint();
    }

    @Action
    public void closeIntersectionPropWindow() {
        F_IntProperties.setVisible(false);
        F_IntPropWindow.setVisible(false);
    }

    @Action
    public void setIntersectionSpecProps() {
        Intersection intscn = intMap.get(toBeUpdatedWaypoint);
        if(intscn != null){
            if(CB_Type_IntWindow.getSelectedIndex() == 0||CB_Type_IntWindow.getSelectedIndex() == 9){
                intscn.setOnRampProps(SB_Gore_Ramp_IntPropWindow.getValue()/1000.0, SB_MR_Ramp_ConPropWindow.getValue(), SB_Sd_Ramp_IntPropWindow.getValue());
            }
            else if(CB_Type_IntWindow.getSelectedIndex() == 1||CB_Type_IntWindow.getSelectedIndex()> 9){
                intscn.setOffRampProps(SB_Gore_Exit_IntPropWindow.getValue()/1000.0);
            }
            else if(CB_Type_IntWindow.getSelectedIndex() == 2){
                intscn.setSignalizedProps(SB_Green_Signal_IntPropWindow.getValue(), SB_Red_Signal_IntPropWindow.getValue(), SB_Offset_Signal_IntPropWindow.getValue());
            }
            else if(CB_Type_IntWindow.getSelectedIndex() == 3){
                intscn.setUnsignalizedProps(RB_StopY_UnSignal_IntPropWindow.isSelected());
            }
        }
        F_IntPropWindow.setVisible(false);
    }

    @Action
    public void cancelIntersectionSpecProps() {
        F_IntPropWindow.setVisible(false);
    }

    @Action
    public void updateConnectionProperties() {
       Connection con = conMap.get(toBeUpdatedConnection);
        
       if(con != null){
            con.nLanes = CB_Lanes_RdPrms_ConPropWindow.getSelectedIndex() + 1;                      //index starts from 0 but the # of lanes start from 1 so +1
            con.length = Double.parseDouble(TF_LLength_RdPrms_ConPropWindow.getText())/1000.0;
                        
            con.RM.AbsMaxRate = Double.parseDouble(TF_MaxMR.getText());
            con.RM.AbsMinRate = Double.parseDouble(TF_MinMR.getText());
            con.RM.flushUpOcc = Double.parseDouble(TF_FlushDown.getText());
            con.RM.flushDownOcc = Double.parseDouble(TF_FlushUp.getText());
            con.RM.StorageBegin = (con.length*1000-(Double.parseDouble(TF_Storage.getText())+200))/1000;   //200 meters of
//            con.RM.StorageBegin = (con.length)/2;   //200 meters of
            con.RM.meterRate = SB_MR_Ramp_ConPropWindow.getValue();
            
            ModeManager.ALINEA = R_Alinea.isSelected();
            ModeManager.SWARM1 = R_SWARM1.isSelected(); 
            ModeManager.SWARM1AND2 = R_SWARM12.isSelected();             
            ModeManager.QueueFlush = CB_Flush.isSelected();
            TrafficSimulationDesktopView.BNConnId = Integer.parseInt(TF_BN.getText());
           
            
            con.AccLaneLength = Double.parseDouble(TF_AccLane_RdPrms_ConPropWindow.getText())/1000.0;        //added by Rama for Acceleration lane length
            con.DecLaneLength = Double.parseDouble(TF_DecLane_RdPrms_ConPropWindow.getText())/1000.0;        //added by Rama for Deceleration lane length
            con.G = SB_Grade_RdPrms_ConPropWindow.getValue()/100.0;
            con.isOnRamp = CB_OnRamp_RdPrms_ConPropWindow.isSelected() ? 1:0;
            con.isOffRamp = CB_OffRamp_RdPrms_ConPropWindow.isSelected() ? 1:0;
            con.hasAccLane = CB_AccLane_RdPrms_ConPropWindow.isSelected() ? 1:0;                              //added by Rama for Acceleration lane length
            con.hasDecLane = CB_DecLane_RdPrms_ConPropWindow.isSelected() ? 1:0;                              //added by Rama for Acceleration lane length
            con.u = SB_MaxSpd_CF_ConPropWindow.getValue();
            con.kj = SB_JamDen_CF_ConPropWindow.getValue();
            con.w = SB_WaveSpd_CF_ConPropWindow.getValue();
            con.epsilon = SB_Epsilon_LC_ConPropWindow.getValue();
            con.tau = SB_Tau_LC_ConPropWindow.getValue()/3600.0; // prev 100.0 was 3600.0;
            con.sd = SB_Sd_LC_ConPropWindow.getValue()/1000.0;


            con.dt = 1/(con.kj*con.w);
            
            graph.setEdgeWeight(con,con.length); 
        }
        
        F_ConPropWindow.setVisible(false);
        jXMapKit1.repaint();
    }

    @Action
    public void cancelConnectionPropWindow() {
        F_ConPropWindow.setVisible(false);
    }

    @Action
    public void newGraph() {
        if(fileName != null)
            fileName = null;
            
        navigateFlag = true;
        addIntersectionsFlag = false;
        addConnectionsFlag = false;
        eraseFlag = false;
        RB_Navigate.setSelected(true);
        
        graph = new DirectedWeightedMultigraph<Intersection, Connection>(Connection.class);
        intMap = new HashMap<WaypointIntersection, Intersection>() ;
        conMap = new HashMap<WaypointConnection, Connection>() ;
        
        wpOverlay.setWaypoints(intMap.keySet());
        jXMapKit1.repaint();
        
        //Initialize Undo and Redo Options
        actions = new ArrayList<ActionPerformed>();
        actionIndex = 0;
    }

    @Action
    public void openGraph() {
         JFileChooser chooser = new javax.swing.JFileChooser();
         
         chooser.setFileFilter(new JFileFilter(grpExtn+" files",grpExtn));
         chooser.setFileHidingEnabled(true);
         chooser.setCurrentDirectory(currDir);
         int action = chooser.showOpenDialog(mainPanel);
         
         if(action == JFileChooser.APPROVE_OPTION)
         {
            File selFile = chooser.getSelectedFile();
            System.out.println("File: "+selFile);
         
            navigateFlag = true;
            addIntersectionsFlag = false;
            addConnectionsFlag = false;
            eraseFlag = false;
            RB_Navigate.setSelected(true);
            
            fileName = selFile.getAbsolutePath();
            openGraph(selFile);
            
            //Initialize Undo and Redo Options
            actions = new ArrayList<ActionPerformed>();
            actionIndex = 0;
         }
         currDir = chooser.getCurrentDirectory();
    }

    @Action
    public void saveGraph() {
         JFileChooser chooser = new JFileChooser();
         chooser.setCurrentDirectory(currDir);
         chooser.setFileFilter(new JFileFilter(grpExtn+" files",grpExtn));
         chooser.setFileHidingEnabled(true);
             int action = chooser.showSaveDialog(mainPanel);
             if(action == JFileChooser.APPROVE_OPTION)
             {
                File savFile = chooser.getSelectedFile();
                if(!savFile.getName().endsWith(grpExtn))
                   savFile = new File(savFile.getAbsolutePath().concat(grpExtn));

                fileName = savFile.getAbsolutePath();
                saveGraph(savFile);

                //Initialize Undo and Redo Options
                actions = new ArrayList<ActionPerformed>();
                actionIndex = 0;
             }
         currDir = chooser.getCurrentDirectory();
    }

    @Action
    public void setNtwrkParams() {
        Connection.minSpeedDiff = Double.parseDouble(F_MinSpdDiff_SetNtwrkParams.getText());
        TrafficSimulationDesktopView.jrRelax = RB_Relax.isSelected();
        TrafficSimulationDesktopView.jrGapacc1 = RB_GapAcc1.isSelected();
        TrafficSimulationDesktopView.jrGapacc2 = RB_GapAcc2.isSelected();        
        
        F_SetNtwrkParams.setVisible(false);
    }

    @Action
    public void cancelNtwrkParams() {
        F_SetNtwrkParams.setVisible(false);
    }
    
   /* 
    * Show the input window to fill in the origin-destination parameters
    */
    public void showOrigDestInputWindow(){
        int maxwid = 1320;
        int maxhei = 408;
                
        jFrameOrDest = new javax.swing.JFrame();
        jFrameOrDest.setTitle("Input Origin-Destination Matrix");
        jFrameOrDest.setResizable(false);
        jFrameOrDest.setSize((((destinationList.size()+1)*120) > maxwid)?maxwid:((destinationList.size()+1)*120), (((this.originList.size()+3)*34) > maxhei)?maxhei:((this.originList.size()+3)*34));        
        jFrameOrDest.setLocation(10, 10);
        
        JButton saveButton = new JButton("Ok");
        JButton cancelButton = new JButton("Cancel");
        
        Container contentPane = jFrameOrDest.getContentPane();
       
        //contentPane.setLayout(new GridLayout(0, destinationList.size()+1,5,5));
        contentPane.setLayout(new GridLayout(0, destinationList.size()+1,5,5));
        
        // New code
        javax.swing.JMenuBar menuBarOrigDest = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenuOrigDest = new javax.swing.JMenu();
        javax.swing.JMenuItem jMenuItemOrigDest1 = new javax.swing.JMenuItem();
        javax.swing.JMenuItem jMenuItemOrigDest2 = new javax.swing.JMenuItem();
        fileMenuOrigDest.setText("File");
        jMenuItemOrigDest1.setText("Open");
        addMenuItemListener(jMenuItemOrigDest1,"Open");
        jMenuItemOrigDest2.setText("Save");
        addMenuItemListener(jMenuItemOrigDest2,"Save");
        fileMenuOrigDest.add(jMenuItemOrigDest1);
        fileMenuOrigDest.add(jMenuItemOrigDest2);
        menuBarOrigDest.add(fileMenuOrigDest);
        contentPane.add(menuBarOrigDest);
        for(int j = 0; j < this.destinationList.size(); j++)
            contentPane.add(new javax.swing.JMenuBar());
        // End new code
        
        mat = new JTextField[this.originList.size()][this.destinationList.size()];
        
        if(this.origDestMatrix == null)
            createDefaultOrigDestMatrix();
        
        // Add the Column Name labels
        for(int j = -1; j < this.destinationList.size(); j++){
            if(j == -1)
                contentPane.add(new JLabel("Origin    \\   Destination"));
            else
                contentPane.add(new JLabel(destinationList.get(j).toString()));
        }
        
        // Add all the text boxes to the content pane
        for(int i = 0; i < this.originList.size(); i++){
            for(int j = -1; j < this.destinationList.size(); j++){
                    if(j == -1){
                        contentPane.add(new JLabel(originList.get(i).toString()));
                    }
                    else{
                        mat[i][j] = new JTextField();
                        mat[i][j].setText(""+this.origDestMatrix[i][j]+"");
                        contentPane.add(mat[i][j]);
                    }
            }
        }
        
        // Adding Listeners - Save Button
        saveButton.addMouseListener(new MouseListener(){
            public void mouseClicked(MouseEvent e) {
                updateOrigDestMatrix();
                System.out.println("Save Clicked!!!!!!!!!");
            }
            public void mousePressed(MouseEvent e) {}            
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}            
        });
        
        // Adding Listeners - Cancel Button
        cancelButton.addMouseListener(new MouseListener(){
            public void mouseClicked(MouseEvent e) {
                closeOrigDestInputWindow();
                System.out.println("Cancel Clicked!!!!!!!!!");
            }
            public void mousePressed(MouseEvent e) {}            
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}            
        });
        
        contentPane.add(saveButton);
        contentPane.add(cancelButton);
        
        jFrameOrDest.setVisible(true);
    }    
        public void openOrigDestMatrix(File f) {
    	try{
            FileInputStream fis = new FileInputStream(f);
            ObjectInputStream ois = new ObjectInputStream(fis);
            double tempOrigDestMatrix[][] = (double[][]) ois.readObject();
            int oriCount = tempOrigDestMatrix.length;
            int desCount = tempOrigDestMatrix[0].length;
            
            if(oriCount != originList.size() || desCount != destinationList.size()){
                JOptionPane.showMessageDialog(this.F_IntProperties, "The matrix input is invalid!","Invalid Input" , 0);
            }
            else{
                origDestMatrix = tempOrigDestMatrix;
                
                // Add all the text boxes to the content pane
                for(int i = 0; i < this.originList.size(); i++){
                    for(int j = 0; j < this.destinationList.size(); j++){
                            mat[i][j].setText(""+this.origDestMatrix[i][j]+"");
                     }
                }
            }
            
            ois.close();
            fis.close();
    	}catch(Exception e){
    		System.out.println("Error "+e.toString());
    	}
    }
        
    // Open Origin Destination Matrix Frame
    private void openOrigDestMatrixFrame(){
         JFileChooser chooser = new javax.swing.JFileChooser();
         chooser.setFileFilter(new JFileFilter(odmExtn+" files",odmExtn));
         chooser.setFileHidingEnabled(true);
         int action = chooser.showOpenDialog(mainPanel);
        
         if(action == JFileChooser.APPROVE_OPTION){
            File selFile = chooser.getSelectedFile();
         
            openOrigDestMatrix(selFile);
         }
    }
    
    public void saveOrigDestMatrix(File f) {
    	try{
    		FileOutputStream fos = new FileOutputStream(f);
    		ObjectOutputStream oos = new ObjectOutputStream(fos);
                
                for(int i = 0; i < this.originList.size(); i++){
                    for(int j = 0; j < this.destinationList.size(); j++){
                            origDestMatrix[i][j] = Double.parseDouble(mat[i][j].getText().trim());
                     }
                }
                
    		oos.writeObject(origDestMatrix);
    		oos.close();
    		fos.close();
    	}
    	catch(Exception e){
    		System.out.println("Error "+e.toString());
        }
    }
    
    // Save Origin Destination Matrix Frame
    private void saveOrigDestMatrixFrame(){
         JFileChooser chooser = new javax.swing.JFileChooser();
         chooser.setFileFilter(new JFileFilter(odmExtn+" files",odmExtn));
         chooser.setFileHidingEnabled(true);
         int action = chooser.showSaveDialog(mainPanel);
        
         if(action == JFileChooser.APPROVE_OPTION){
            File savFile = chooser.getSelectedFile();
            
            if(!savFile.getName().endsWith(odmExtn))
                   savFile = new File(savFile.getAbsolutePath().concat(odmExtn));
               
            saveOrigDestMatrix(savFile);
         }
    }
    
    // Exit Origin Destination Matrix Frame
    private void exitOrigDestMatrixFrame(){
        jFrameOrDest.setVisible(false);
    }
    
    private void addMenuItemListener(javax.swing.JMenuItem menuItem,final String menuItemType){
        if(menuItemType.equals("Open")){
            menuItem.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    openOrigDestMatrixFrame();
                }
            });
        }
        else if(menuItemType.equals("Save")){
            menuItem.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    saveOrigDestMatrixFrame();
                }
            });
        }
        else{
               menuItem.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    exitOrigDestMatrixFrame();
                }
            });
        }
    }
    
    /*
     * Creates the Origin and Destination Lists
     */
    public void createOrigDestLists(){
        // Create the Origin and Destination lists
        originList = new ArrayList<Intersection>();
        destinationList = new ArrayList<Intersection>();
        for(Intersection tempInt: graph.vertexSet()){
            if(tempInt.type == Intersection.ORIGIN || tempInt.type == Intersection.ORIGIN_AND_DEST)
                originList.add(tempInt);
            if (tempInt.type == Intersection.DESTINATION || tempInt.type == Intersection.ORIGIN_AND_DEST)
                destinationList.add(tempInt);
            }
    }
    
    /*
     * Creates Default Origin-Destination Matrix
     */
    public void createDefaultOrigDestMatrix(){
        // Create Origin Destination Matrix
        origDestMatrix = new double[originList.size()][destinationList.size()];
        for(int i = 0; i < originList.size(); i++)
            for(int j = 0; j < destinationList.size(); j++){
                if(originList.get(i) == destinationList.get(j))
                    origDestMatrix[i][j] = 0;
                else
                    origDestMatrix[i][j] = defaultMatVal;
            }
        // Set the Orign-Destination Matrix. Each entry is veh/hr
        System.out.print("(Inside createDefaultOrigDestMatrix() class)Setting Origin Dest Matrix : ");
        for(int i = 0; i < originList.size(); i++){
            System.out.print("[");
            System.out.print("{"+origDestMatrix[i][0]+"}");
            for(int j = 1; j < destinationList.size(); j++)
                System.out.print(",{"+origDestMatrix[i][j]+"}");
            System.out.print("] ");
        }
    }
    
    /*
     * Updates the Orig-Destination Matrix
     */
    public void updateOrigDestMatrix(){
        for(int i = 0; i < this.originList.size(); i++){
            for(int j = 0; j < this.destinationList.size(); j++){
                double val;
                try{
                    val = java.lang.Double.parseDouble(this.mat[i][j].getText());
                }
                catch(NumberFormatException nfe){
                    val = this.origDestMatrix[i][j];
                }
                
                this.origDestMatrix[i][j] = val;
            }
        }
        
        closeOrigDestInputWindow();
    }
    
    /*
    * Closes the Orig-Destination Matrix window
    */
    public void closeOrigDestInputWindow(){
        this.inputObtained = true;
        jFrameOrDest.setVisible(false);
    }
        
//--------end of custom GUI code for displaying OD matrix    

    @Action
    public void showNtwrkParamsWindow() {
        F_SetNtwrkParams.setBounds(35, 45, 310, 300); 
        F_SetNtwrkParams.setTitle("Network Parameters");
        F_SetNtwrkParams.setResizable(false);
        F_SetNtwrkParams.setVisible(true);

    }
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton B_Cancel_ConPropWindow;
    private javax.swing.JButton B_Cancel_IntPropWindow;
    private javax.swing.JButton B_Cancel_IntWindow;
    private javax.swing.JButton B_Cancel_SetNtwrkParams;
    private javax.swing.JButton B_OK_IntPropWindow;
    private javax.swing.JButton B_OK_IntWindow;
    private javax.swing.JButton B_OK_SetNtwrkParams;
    private javax.swing.JButton B_Ok_ConPropWindow;
    private javax.swing.JButton B_Properties_IntWindow;
    private javax.swing.JCheckBox CB_AccLane_RdPrms_ConPropWindow;
    private javax.swing.JCheckBox CB_DecLane_RdPrms_ConPropWindow;
    private javax.swing.JCheckBox CB_Flush;
    private javax.swing.JComboBox CB_Lanes_RdPrms_ConPropWindow;
    private javax.swing.JCheckBox CB_OffRamp_RdPrms_ConPropWindow;
    private javax.swing.JCheckBox CB_OnRamp_RdPrms_ConPropWindow;
    private javax.swing.JComboBox CB_Type_IntWindow;
    private javax.swing.JFrame F_ConPropWindow;
    private javax.swing.JFrame F_ControlWindow;
    private javax.swing.JTextField F_ID_SetNtwrkParams;
    private javax.swing.JFrame F_IntPropWindow;
    private javax.swing.JFrame F_IntProperties;
    private javax.swing.JTextField F_MinSpdDiff_SetNtwrkParams;
    private javax.swing.JFrame F_SetNtwrkParams;
    private javax.swing.JFrame F_SimWindow;
    private javax.swing.JTextField F_TP_SetNtwrkParams;
    private javax.swing.JLabel L_AccLane_RdPrms_ConPropWindow;
    private javax.swing.JLabel L_DecLane_RdPrms_ConPropWindow;
    private javax.swing.JLabel L_ID_SetNtwrkParams;
    private javax.swing.JLabel L_LLength_RdPrms_ConPropWindow;
    private javax.swing.JLabel L_Lanes_RdPrms_ConPropWindow;
    private javax.swing.JLabel L_MinSpdDiff_SetNtwrkParams;
    private javax.swing.JLabel L_Name_IntWindow;
    private javax.swing.JLabel L_OffRamp_RdPrms_ConPropWindow;
    private javax.swing.JLabel L_OnRamp_RdPrms_ConPropWindow;
    private javax.swing.JLabel L_TP_SetNtwrkParams;
    private javax.swing.JLabel L_Type_IntWindow;
    private javax.swing.JMenuItem MI_New;
    private javax.swing.JMenuItem MI_Open;
    private javax.swing.JMenuItem MI_Save;
    private javax.swing.JMenuItem MI_SetNtwrkParams;
    private javax.swing.JPanel P_CF_ConPropWindow;
    private javax.swing.JPanel P_Exit_IntPropWindow;
    private javax.swing.JPanel P_IntWindow;
    private javax.swing.JPanel P_LC_ConPropWindow;
    private javax.swing.JPanel P_OKCancel_IntPropWindow;
    private javax.swing.JPanel P_OKCancel_IntWindow;
    private javax.swing.JPanel P_OKCancel_SetNtwrkParams;
    private javax.swing.JPanel P_OkCancel_ConPropWindow;
    private javax.swing.JPanel P_RM_ConPropWindow;
    private javax.swing.JPanel P_Ramp_IntPropWindow;
    private javax.swing.JPanel P_RdPrms_ConPropWindow;
    private javax.swing.JPanel P_SetNtwrkParams;
    private javax.swing.JPanel P_Signal_IntPropWindow;
    private javax.swing.JPanel P_Stop_UnSignal_IntPropWindow;
    private javax.swing.JPanel P_Unsignal_IntPropWindow;
    private javax.swing.JRadioButton RB_AddCon;
    private javax.swing.JRadioButton RB_AddInt;
    private javax.swing.JRadioButton RB_Erase;
    private javax.swing.JRadioButton RB_GapAcc1;
    private javax.swing.JRadioButton RB_GapAcc2;
    private javax.swing.JRadioButton RB_Navigate;
    private javax.swing.JRadioButton RB_Relax;
    private javax.swing.JRadioButton RB_StopN_UnSignal_IntPropWindow;
    private javax.swing.JRadioButton RB_StopY_UnSignal_IntPropWindow;
    private javax.swing.JRadioButton R_Alinea;
    private javax.swing.JRadioButton R_SWARM1;
    private javax.swing.JRadioButton R_SWARM12;
    private trafficsimulationdesktop.SlideBar SB_Epsilon_LC_ConPropWindow;
    private trafficsimulationdesktop.SlideBar SB_Gore_Exit_IntPropWindow;
    private trafficsimulationdesktop.SlideBar SB_Gore_Ramp_IntPropWindow;
    private trafficsimulationdesktop.SlideBar SB_Grade_RdPrms_ConPropWindow;
    private trafficsimulationdesktop.SlideBar SB_Green_Signal_IntPropWindow;
    private trafficsimulationdesktop.SlideBar SB_JamDen_CF_ConPropWindow;
    private trafficsimulationdesktop.SlideBar SB_MR_ControlWindow;
    private trafficsimulationdesktop.SlideBar SB_MR_Ramp_ConPropWindow;
    private trafficsimulationdesktop.SlideBar SB_MaxSpd_CF_ConPropWindow;
    private trafficsimulationdesktop.SlideBar SB_Offset_Signal_IntPropWindow;
    private trafficsimulationdesktop.SlideBar SB_Red_Signal_IntPropWindow;
    private trafficsimulationdesktop.SlideBar SB_Sd_LC_ConPropWindow;
    private trafficsimulationdesktop.SlideBar SB_Sd_Ramp_IntPropWindow;
    private trafficsimulationdesktop.SlideBar SB_SimSpeed_ControlWindow;
    private trafficsimulationdesktop.SlideBar SB_Tau_LC_ConPropWindow;
    private trafficsimulationdesktop.SlideBar SB_WaveSpd_CF_ConPropWindow;
    private javax.swing.JTextField TF_AccLane_RdPrms_ConPropWindow;
    private javax.swing.JTextField TF_BN;
    private javax.swing.JTextField TF_DecLane_RdPrms_ConPropWindow;
    private javax.swing.JTextField TF_FlushDown;
    private javax.swing.JTextField TF_FlushUp;
    private javax.swing.JTextField TF_LLength_RdPrms_ConPropWindow;
    private javax.swing.JTextField TF_MaxMR;
    private javax.swing.JTextField TF_MinMR;
    private javax.swing.JTextField TF_Name_IntWindow;
    private javax.swing.JTextField TF_Storage;
    private javax.swing.JTabbedPane TP_ConPropWindow;
    private javax.swing.JTabbedPane TP_IntPropWindow;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.ButtonGroup buttonGroup4;
    private javax.swing.JMenu editMenu;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private org.jdesktop.swingx.JXMapKit jXMapKit1;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JProgressBar progressBar;
    private trafficsimulationdesktop.Simulation simulation1;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    // End of variables declaration//GEN-END:variables

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private JDialog aboutBox;
}
class ActionPerformed{
    int type;                       // 0: Added Waypoint, 1: Added Waypoint Connection, 2: Deleted Waypoint 3: Deleted Waypoint Connection
    WaypointIntersection wpint;
    WaypointConnection wpcon;
    Intersection intscn;
    Connection conctn;
    Intersection intscn1;
    int group;
    
    // Constructor
    ActionPerformed(int type, WaypointIntersection wpint, WaypointConnection wpcon, Intersection intscn, Connection conctn, Intersection intscn1, int group){
        this.type = type;
        this.wpint = wpint;
        this.wpcon = wpcon;
        this.intscn = intscn;
        this.conctn = conctn;
        this.intscn1 = intscn1;
        this.group = group;
    }
}
