package trafficsimulationdesktop;

import java.awt.*;
import javax.swing.*;
import java.io.*;
import java.io.FileInputStream;
import java.awt.image.*;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.jgrapht.alg.BellmanFordShortestPath;
import java.util.*;
import java.util.List;
import java.util.Scanner;


/*
 * Simulates the current scenario
 */
public class Simulation extends JComponent implements Serializable {

   private ArrayList<Intersection> originList;                                 // List of origin nodes
   private ArrayList<Intersection> destinationList;                            // List of destination nodes
   private double origDestMatrix[][];                                          // Number of vehicles/sec produced between the Origin and Destination
   private DirectedWeightedMultigraph<Intersection,Connection> graph;          // The graph of the network
   BellmanFordShortestPath sp;

   public static LinkedList<RampMeter> allRMs;                                 // All Ramp Meters in the Network
   public static LinkedList<VariableSpeedLimit> allVSLs;                                 // All Variable Speed Limits in the Network
   public static LinkedList<TrafficLight> allTLs;                                // All Traffic Lights in the Network

   public long lastQueue=0;
   private LinkedList<Connection> ec ;
   private LinkedList<Connection> ec_tl ;

   private Intersection dispInscn;                                             // The intersection to displayed
   private int xsize;
   private int ysize;

   private int counter = 0;
   private Graphics2D bg = null;
   private Image buffer;
//   private String dir = "simulation/";
//   AnimatedGifEncoder e = new AnimatedGifEncoder();
   private  int dotsize = 2;
   ModeManager mManager; 
   double dt = 0;
   public static double currTime=0;                                                              //time from the start of the simulation (if currTime = 0.001, then it is 36 seconds into the simulation or 24 time steps)
   boolean fwyEmptyFlag = false;                                                    //OD change period in hours
   java.util.Date date = new java.util.Date();

   public Lane lane;
   public double HOTratio;
   public double HOTexitratio;
   public static int vdsId;

   public static DetectorStation[] VSL_VDS;


   public Simulation () {
       allRMs = new LinkedList<RampMeter>();
       allVSLs = new LinkedList<VariableSpeedLimit>();
       allTLs = new LinkedList<TrafficLight>();
   }

  /*
   * Initialize display parameters for simuation
   */
   public void init(int xisize, int yisize){
       xsize = xisize;
       ysize = yisize;
       buffer = createImage(xsize,ysize + 15);
       bg = (Graphics2D)buffer.getGraphics();
       bg.setColor (Color.GREEN);
       bg.fillRect (0, 0, xsize, ysize + 15);
       bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
   }

  /*
   * Initialize display parameters for simuation
   */
   public void init(){
       init(getSize().width, getSize().height);
   }

  /*
   * Initialize the Sections/Lanes/Car Prototype etc for simulaltion
   */
   public void initializeSimulation() throws Exception{       
        System.out.println("Initialize Simulation: "+Calendar.getInstance().getTime()); 

        // Reset the car Ids
       Lane.carIds = 0;

       for(Connection contn : graph.edgeSet())
           dt = contn.dt;       
       
       // 1. Initialize Connections
       System.out.println("Initialize Connections");
       for(Connection tempCon : graph.edgeSet()){
          if (graph.getEdgeSource(tempCon).type == Intersection.ORIGIN){
              if(graph.getEdgeTarget(tempCon).type == Intersection.RAMP||graph.getEdgeTarget(tempCon).type == Intersection.RAMP2){
                  System.out.println("Found Origin Ramp");
                  tempCon.iniN(TrafficSimulationDesktopView.qR,TrafficSimulationDesktopView.qR, Connection.ORIGIN, graph.getEdgeTarget(tempCon).gore);
              }
              else if(graph.getEdgeTarget(tempCon).type == Intersection.EXIT)
                  tempCon.iniE(TrafficSimulationDesktopView.qA, TrafficSimulationDesktopView.qR, Connection.ORIGIN,graph.getEdgeTarget(tempCon).gore);
              else if(graph.getEdgeTarget(tempCon).type == Intersection.EXIT2)
                  tempCon.iniE2(TrafficSimulationDesktopView.qA, TrafficSimulationDesktopView.qR, Connection.ORIGIN,graph.getEdgeTarget(tempCon).gore);
              else if(graph.getEdgeTarget(tempCon).type == Intersection.EXIT3)
                  tempCon.iniE3(TrafficSimulationDesktopView.qA, TrafficSimulationDesktopView.qR, Connection.ORIGIN,graph.getEdgeTarget(tempCon).gore);
              else{
                  System.out.println("Found Origin");
                  tempCon.iniN(TrafficSimulationDesktopView.qA, TrafficSimulationDesktopView.qA, Connection.ORIGIN,0.0);
              }
          }
          else if(graph.getEdgeTarget(tempCon).type == Intersection.DESTINATION){
              if(graph.getEdgeSource(tempCon).type == Intersection.EXIT||graph.getEdgeSource(tempCon).type == Intersection.EXIT2||graph.getEdgeSource(tempCon).type == Intersection.EXIT3){
                  System.out.println("Found Destination Exit");
                  tempCon.iniN(TrafficSimulationDesktopView.qR,TrafficSimulationDesktopView.qR, Connection.DESTINATION, graph.getEdgeSource(tempCon).gore);
              }
              else if(graph.getEdgeSource(tempCon).type == Intersection.RAMP)
                  tempCon.iniR(TrafficSimulationDesktopView.qA, TrafficSimulationDesktopView.qR, Connection.DESTINATION,graph.getEdgeSource(tempCon).gore);
              else if(graph.getEdgeSource(tempCon).type == Intersection.RAMP2)
                  tempCon.iniR2(TrafficSimulationDesktopView.qA, TrafficSimulationDesktopView.qR, Connection.DESTINATION,graph.getEdgeSource(tempCon).gore);
              else{
                  System.out.println("Found Destination");
                  tempCon.iniN(TrafficSimulationDesktopView.qA, TrafficSimulationDesktopView.qA, Connection.DESTINATION, 0.0);
              }
          }
          else if(graph.getEdgeSource(tempCon).type == Intersection.ORIGIN_AND_DEST && 
                  graph.getEdgeTarget(tempCon).type == Intersection.TWO_WAY){
              System.out.println("Found OD to 2-way");
              tempCon.iniN(TrafficSimulationDesktopView.qA, TrafficSimulationDesktopView.qA, Connection.ORIGIN, 0.0);
          }
          else if(graph.getEdgeSource(tempCon).type == Intersection.TWO_WAY && 
                  graph.getEdgeTarget(tempCon).type == Intersection.TWO_WAY){
              System.out.println("Found 2-way to 2-way");
              tempCon.iniN(TrafficSimulationDesktopView.qA, TrafficSimulationDesktopView.qA, Connection.NORMAL, 0.0);
          }
          else if(graph.getEdgeSource(tempCon).type == Intersection.TWO_WAY && 
                  graph.getEdgeTarget(tempCon).type == Intersection.ORIGIN_AND_DEST){
              System.out.println("Found 2-way to OD");
              tempCon.iniN(TrafficSimulationDesktopView.qA, TrafficSimulationDesktopView.qA, Connection.DESTINATION, 0.0);
          }
          else if(graph.getEdgeSource(tempCon).type == Intersection.ORIGIN_AND_DEST && 
                  graph.getEdgeTarget(tempCon).type == Intersection.ORIGIN_AND_DEST){
              System.out.println("Found OD to OD");
              tempCon.iniN(TrafficSimulationDesktopView.qA, TrafficSimulationDesktopView.qA, Connection.ORIGIN, 0.0);
          }
          else if(graph.getEdgeSource(tempCon).type == Intersection.ORIGIN_AND_DEST && 
                  graph.getEdgeTarget(tempCon).type == Intersection.SIGNALIZED){
              System.out.println("Found OD to Signal");
              tempCon.iniS(TrafficSimulationDesktopView.qA, TrafficSimulationDesktopView.qA, Connection.ORIGIN, 0.0);
          }
          else if(graph.getEdgeSource(tempCon).type == Intersection.SIGNALIZED && 
                  graph.getEdgeTarget(tempCon).type == Intersection.ORIGIN_AND_DEST){
              System.out.println("Found Signal to OD");
              tempCon.iniS(TrafficSimulationDesktopView.qA, TrafficSimulationDesktopView.qA, Connection.DESTINATION, 0.0);
          }
          else if(graph.getEdgeSource(tempCon).type == Intersection.SIGNALIZED && 
                  graph.getEdgeTarget(tempCon).type == Intersection.GENERIC){
              System.out.println("Found Signal to Connector");
              tempCon.iniN(TrafficSimulationDesktopView.qA, TrafficSimulationDesktopView.qA, Connection.NORMAL, 0.0);
          }
          else if(graph.getEdgeSource(tempCon).type == Intersection.GENERIC && 
                  graph.getEdgeTarget(tempCon).type == Intersection.SIGNALIZED){
              System.out.println("Found Connector to Signal");
              tempCon.iniE2(TrafficSimulationDesktopView.qA, TrafficSimulationDesktopView.qR, Connection.NORMAL,graph.getEdgeSource(tempCon).gore);
//              tempCon.iniS(TrafficSimulationDesktopView.qA, TrafficSimulationDesktopView.qA, Connection.NORMAL, 0.0);
          }
          else if(graph.getEdgeSource(tempCon).type == Intersection.SIGNALIZED &&
                  graph.getEdgeTarget(tempCon).type == Intersection.SIGNALIZED){
              System.out.println("Found Connector to Signal");
              tempCon.iniS(TrafficSimulationDesktopView.qA, TrafficSimulationDesktopView.qA, Connection.NORMAL, 0.0);
          }
          else if(graph.getEdgeSource(tempCon).type == Intersection.ORIGIN &&
                  graph.getEdgeTarget(tempCon).type == Intersection.SIGNALIZED){
              System.out.println("Found Origin to Signal");
              tempCon.iniS(TrafficSimulationDesktopView.qA, TrafficSimulationDesktopView.qA, Connection.NORMAL, 0.0);
          }
          else if(graph.getEdgeSource(tempCon).type == Intersection.SIGNALIZED &&
                  graph.getEdgeTarget(tempCon).type == Intersection.DESTINATION){
              System.out.println("Found Signal to Destination");
              tempCon.iniN(TrafficSimulationDesktopView.qA, TrafficSimulationDesktopView.qA, Connection.NORMAL, 0.0);
          }

          else{
              if((graph.getEdgeSource(tempCon).type == Intersection.RAMP) &&
                 (graph.getEdgeTarget(tempCon).type == Intersection.EXIT)){
                  System.out.println("Found Both: Ramp Exit");
//                  tempCon.iniB(TrafficSimulationDesktopView.qA,TrafficSimulationDesktopView.qR, Connection.NORMAL,graph.getEdgeSource(tempCon).gore, graph.getEdgeSource(tempCon).meterRate);
              }
              else if (graph.getEdgeSource(tempCon).type == Intersection.RAMP){
                  System.out.println("Found section with Ramp merging lane");
                  tempCon.iniR(TrafficSimulationDesktopView.qA, TrafficSimulationDesktopView.qR, Connection.NORMAL,graph.getEdgeSource(tempCon).gore);
              }
              else if (graph.getEdgeSource(tempCon).type == Intersection.RAMP2){
                  System.out.println("Found section with Ramp2 merging lane");
                  tempCon.iniR2(TrafficSimulationDesktopView.qA, TrafficSimulationDesktopView.qR, Connection.NORMAL,graph.getEdgeSource(tempCon).gore);
              }
              else if (graph.getEdgeTarget(tempCon).type == Intersection.EXIT){
                  System.out.println("Found section with Exit lane");
                  tempCon.iniE(TrafficSimulationDesktopView.qA, TrafficSimulationDesktopView.qR, Connection.NORMAL,graph.getEdgeSource(tempCon).gore);
              }
              else if (graph.getEdgeTarget(tempCon).type == Intersection.EXIT2){
                  System.out.println("Found section with Exit2 lane");
                  tempCon.iniE2(TrafficSimulationDesktopView.qA, TrafficSimulationDesktopView.qR, Connection.NORMAL,graph.getEdgeSource(tempCon).gore);
              }
              else if (graph.getEdgeTarget(tempCon).type == Intersection.EXIT3){
                  System.out.println("Found section with Exit3 lane");
                  tempCon.iniE3(TrafficSimulationDesktopView.qA, TrafficSimulationDesktopView.qR, Connection.NORMAL,graph.getEdgeSource(tempCon).gore);
              }
              else{
                  System.out.println("Found normal section");
                  tempCon.iniN(TrafficSimulationDesktopView.qA, TrafficSimulationDesktopView.qA, Connection.NORMAL, 0.0);
             }
           } 
//          System.out.println(tempCon+"\nsource: "+tempCon.Source+"\ntarget: "+tempCon.Target+"\nLane0type: "+tempCon.lane0Type+"\ntype: "+tempCon.type);
       }

       // 2. Initialize Lanes -> Done in the Connection Initialization only

       // 3. initialize DetectorStations, SEARM_bottleneck, LaneDetectors, and Ramp Meters , VDS, VSL
       Intersection s;
       Intersection s1;

       ec = new LinkedList<Connection>();
       
       allRMs = new LinkedList<RampMeter>();
       allVSLs = new LinkedList<VariableSpeedLimit>();
       
       ec_tl = new LinkedList<Connection>();
       allTLs = new LinkedList<TrafficLight>();

       for(Connection tempCon1: graph.edgeSet())
       {
//---------VDS Definition--------VSLRM

//--------------------DetectorStation definition----------------RM!!!!!
           if(tempCon1.isOnRamp==0&&tempCon1.isOffRamp==0){
               int nStations =(int)(tempCon1.length/0.25) ;
               tempCon1.stations = new DetectorStation[nStations];
               for(int i=0; i<nStations; i++)
                   tempCon1.stations[i]=new DetectorStation(tempCon1, (i+1)*0.25);  //stations are located at 1 km, 1,5 km...not starting at 0 km.

           }
           else if(tempCon1.isOnRamp==1){

               tempCon1.stations = new DetectorStation[5];
               tempCon1.stations[0]=new DetectorStation(tempCon1, 0.1);
               tempCon1.stations[1]=new DetectorStation(tempCon1, tempCon1.length-1.5);
               tempCon1.stations[2]=new DetectorStation(tempCon1, tempCon1.length-1.3);
               tempCon1.stations[3]=new DetectorStation(tempCon1, tempCon1.length-1.0);
               tempCon1.stations[4]=new DetectorStation(tempCon1, tempCon1.length-0.05);


           }
//           else{
           else if(tempCon1.isOffRamp==1){
               tempCon1.stations = new DetectorStation[1];
               tempCon1.stations[0]=new DetectorStation(tempCon1, 0.1);
           }



//------------------count the number of ramp merges--------------used to see how many static cars are there in the system---used for making sure the system is empty
           if(tempCon1.lane0Type==Lane.AuxLane_RAMP)
                       TrafficSimulationDesktopView.mergeRampCount++;
//--------------------RampMeter definition----------------
//
           if((tempCon1.lane0Type == Lane.AuxLane_RAMP ||(tempCon1.lane0Type==Lane.NORMAL && tempCon1.Source==Intersection.RAMP2))){ //identifying On Ramps
               s = graph.getEdgeSource(tempCon1);
               s1 = graph.getEdgeTarget(tempCon1);
               for(Connection tempCon2: graph.edgeSet()){
                  if(graph.getEdgeTarget(tempCon2) == s)
                      ec.add(tempCon2);
                  else if(graph.getEdgeSource(tempCon2) == s1)
                      ec.add(tempCon2);
               }
              for(int i=0;i<ec.size();i++){
                  if(ec.get(i).isOnRamp==1){
                      ec.get(i).RM.ConnUS = ec.get(i-2);
                      if(tempCon1.t==1){
                        ec.get(i).RM.ConnDS = tempCon1;
                      }else
                        ec.get(i).RM.ConnDS = ec.get(i-1);
                      ec.get(i).RM.ConnRM= ec.get(i);
                      ec.get(i).RM.nRampLanes = ec.get(i).lanes.length;

                      ec.get(i).RM.flushUpOcc = TrafficSimulationDesktopView.flushUpOcc;
                      ec.get(i).RM.flushDownOcc = TrafficSimulationDesktopView.flushDownOcc;

                      allRMs.add(ec.get(i).RM); //all Ramp Meters in graph are stored here
                  }

               }
//
////               for(int i=0;i<ec.size();i++){
////                  if(ec.get(i).isOnRamp==1){
////                      ec.get(i).RM.ConnUS = ec.get(ec.size()-i-1);
////                      ec.get(i).RM.ConnDS = tempCon1;
////                      ec.get(i).RM.ConnRM= ec.get(i);
////                      ec.get(i).RM.nRampLanes = ec.get(i).lanes.length;
////                      allRMs.add(ec.get(i).RM); //all Ramp Metrs in graph are stored here
////                  }
////               }
//
           }
           ec.clear();
//
//           -------------RMs
//
           for(int i = allRMs.size()-1; i>0;i--){
               for(long j=allRMs.get(i-1).ConnDS.ConnId; j<allRMs.get(i).ConnDS.ConnId;j++){
                   for(Connection tempCon: graph.edgeSet()){
                       if(tempCon.ConnId==j){
                           allRMs.get(i-1).distanceToNextRamp+=tempCon.length;
                           break;
                       }
                   }
               }
           }

       //--------------------Traffic Lights definition----------------
           if(graph.getEdgeTarget(tempCon1).type == Intersection.SIGNALIZED ){
               ec_tl.add(tempCon1);
               tempCon1.TL = new TrafficLight(tempCon1, 60, 60, 2);
               tempCon1.isSignalized = 1;
               allTLs.add(ec_tl.get(0).TL);
//               s = graph.getEdgeSource(tempCon1);
//               s1 = graph.getEdgeTarget(tempCon1);
//               for(Connection tempCon2: graph.edgeSet()){
//                  if(graph.getEdgeTarget(tempCon2) == s)
//                      ec_tl.add(tempCon2);
//                  if(graph.getEdgeSource(tempCon2) == s1)
//                      ec_tl.add(tempCon2);
//               }
//              for(int i=0;i<ec_tl.size();i++){
//                  if(ec_tl.get(i).isSignalized==1){
//                      ec_tl.get(i).TL.ConnSig = ec_tl.get(i-2);
//                      if(tempCon1.t==1){
//                        ec_tl.get(i).TL.ConnLeft = tempCon1;
//                      }else
//                        ec_tl.get(i).TL.ConnRight = ec_tl.get(i-1);
//                      ec_tl.get(i).TL.ConnSig= ec_tl.get(i);
////                      ec_tl.get(i).TL.ConnSig.nRampLanes = ec_tl.get(i).lanes.length;
//
//                      allTLs.add(ec_tl.get(i).TL); //all Ramp Meters in graph are stored here
//                  }
//
//               }

           }
           ec_tl.clear();


       //----------------VSL Definition

//
//           double l1 = TrafficSimulationDesktopView.vsl_l_1;
//           double l2 = TrafficSimulationDesktopView.vsl_l_2;
//
//
//           if(tempCon1.ConnId == 1){
//                tempCon1.vsls = new VariableSpeedLimit[2];
//                tempCon1.vsls[0] = new VariableSpeedLimit(tempCon1, 1, 0.0, 13, 17, 25,26,12);
//                tempCon1.vsls[1] = new VariableSpeedLimit(tempCon1, 2, 0.1, 13, 17, 25,26,12);
//           }
////           if(tempCon1.ConnId == 0){
////                tempCon1.vsls = new VariableSpeedLimit[1];
////                tempCon1.vsls[0] = new VariableSpeedLimit(tempCon1, 1, 2.0, 13, 17, 25,26,27);
////           }



       }


 //--------------
              
       // 4. Create cars, calculate shortest path and insert into the lanes - We basically just insert the templates
       System.out.println("Creating car prototypes");
       int cnt = (this.destinationList.size()-1);
       java.util.LinkedList<Connection>[] spList1;
       spList1 = (java.util.LinkedList<Connection>[])new java.util.LinkedList[cnt];
       Intersection signal = null;

       for(Connection tempCon: graph.edgeSet())
           if(graph.getEdgeSource(tempCon).type == Intersection.SIGNALIZED)
               signal = graph.getEdgeSource(tempCon);
       for(int i = 0; i < this.originList.size(); i++){
           Intersection srcInt = this.originList.get(i);
           if((srcInt.type == Intersection.ORIGIN_AND_DEST) || (srcInt.type == Intersection.SIGNALIZED))//srcInt.type == Intersection.TWO_WAY)// || 
           {
               cnt=0;
               for(int m=0;m<this.destinationList.size();m++){
                   if(this.destinationList.get(m) != (srcInt))
                   {
                       spList1[cnt] = new java.util.LinkedList<Connection>();
                       spList1[cnt].add(graph.getEdge(srcInt, signal));
                       spList1[cnt].add(graph.getEdge(signal, this.destinationList.get(m)));
                       double q = this.origDestMatrix[i][m];                                                        // Vehicles/hr
                       Intersection destInt = this.destinationList.get(m);                                          // Destination Intersection
                       Connection srcCon = spList1[cnt].get(0);                                                     // The current segment
                       for(int ln = 0; ln < srcCon.laneCount; ln++)
                           srcCon.lanes[ln].addPrototype(srcInt, destInt, spList1[cnt], q/srcCon.laneCount);
                       cnt++;
                   }
               }                       
           }
           else
           {    
               sp = new BellmanFordShortestPath(graph, srcInt);
               for(int j = 0; j < this.destinationList.size(); j++)
               {
                   double q = this.origDestMatrix[i][j];                                                        // Vehicles/hr
                   Intersection destInt = this.destinationList.get(j);                                          // Destination Intersection
                   if(sp.getPathEdgeList(destInt)!=null)
                   {                          
                       java.util.List<Connection> spList = (java.util.List<Connection>)sp.getPathEdgeList(destInt); // The shortest path
                       Connection srcCon = spList.get(0);                                                           // The current segment
                       for(int ln = 0; ln < srcCon.laneCount; ln++)
                           if(srcCon.lane0Type!=0)
                                srcCon.lanes[ln].addPrototype(srcInt, destInt, spList, q/(srcCon.laneCount-1));
                           else
                                srcCon.lanes[ln].addPrototype(srcInt, destInt, spList, q/srcCon.laneCount);

                   }
               }               
           }
       }             
   }

  /*
   * Run the simulation
   */
   public synchronized void runSimulation() throws Exception{                              
       try{                         
           if(currTime==0){

//               this.loadNextODMFile(0);

//               TrafficSimulationDesktopView.meteringRate = (int)TrafficSimulationDesktopView.maxpoints+"-"+(int)TrafficSimulationDesktopView.DenSampSize+"-"+(int)TrafficSimulationDesktopView.ForecastLeadTime+"-"+(int)TrafficSimulationDesktopView.UpRate+"-"+(int)TrafficSimulationDesktopView.DownRate+"-"+TrafficSimulationDesktopView.InterSectionPropagationFactor+"-"+(int)TrafficSimulationDesktopView.MinVol+"-"+(int)TrafficSimulationDesktopView.MaxVol+"-"+(int)TrafficSimulationDesktopView.MinSpeed+"-"+(int)TrafficSimulationDesktopView.MaxSpeed+"-"+(int)TrafficSimulationDesktopView.MinDen+"-"+(int)TrafficSimulationDesktopView.MaxDen+"-"+TrafficSimulationDesktopView.q+"-"+(int)TrafficSimulationDesktopView.SatDen;
               for(Connection contn : graph.edgeSet()){
                   if (contn.type == Connection.ORIGIN ){
                       for(int k =0; k<contn.laneCount;k++)
                           contn.lanes[k].setvehPrototypeList(contn.lanes[k].getvehPrototypeList());                                      
                   }
                   else if (contn.type != Connection.ORIGIN && contn.isOnRamp==0 && contn.isOffRamp==0) {  // set vehprototype for ALL fwylanes only  starting from connId 1                 
                       check: {
                            for(Connection contn1 : graph.edgeSet()){
                                if(contn1.isOnRamp==0 && contn1.isOffRamp==0 && contn1.ConnId == contn.ConnId-1){  // the vehprototypelist of connId 0 will be used on all the links on the fwy but removing the d/s exit destination                                
                                    if(contn.laneCount<contn1.laneCount){
                                        int k0 = (contn1.Target==Intersection.EXIT3)? 2:1;
                                        for(int k =0; k<contn.laneCount;k++)
                                           contn.lanes[k].setvehPrototypeList(contn1.lanes[k+k0].getvehPrototypeList());  
                                    }
                                    else if(contn.laneCount>contn1.laneCount){  //since there can only be 1 extra lane (because of SOURCE == AuxLane_RAMP or AuxLane_RAMP2 )
                                            contn.lanes[0].setvehPrototypeList(contn1.lanes[0].getvehPrototypeList());  
                                        for(int k =1; k<contn.laneCount;k++)
                                           contn.lanes[k].setvehPrototypeList(contn1.lanes[k-1].getvehPrototypeList());  
                                    }
                                    else{
                                        for(int k =0; k<contn.laneCount;k++)
                                           contn.lanes[k].setvehPrototypeList(contn1.lanes[k].getvehPrototypeList()); 
                                    }
                                    break check;
                                }
                            }
                        }
// The closest exit destination is removed from the prototypelist so that vehicles on the segment feeding the exit will not exit immediately
// and therefore the initial vehicles can be placed anywhere on this segment. moreover, if the inserted vehicles were to exit immediately d/s, 
//then if they are inserted close to the section d/s boundary, they can not exit and will give error.                         
                        if(contn.Target==Intersection.EXIT||contn.Target==Intersection.EXIT2||contn.Target==Intersection.EXIT3)  //if connid 21 is created, connid 22 will be taken care of  
                        {
                            for(int k =0; k<contn.laneCount;k++){
                               ArrayList<VehiclePrototype> tempvehPrototypeList= new ArrayList<VehiclePrototype>();
                               for(VehiclePrototype props :contn.lanes[k].getvehPrototypeList())
                                   tempvehPrototypeList.add(props);
                               contn.lanes[k].setvehPrototypeList(tempvehPrototypeList);                               
                            }
                        }
                   }
               }               
            }
       }       catch(Exception k){
           System.out.println("error in runSimulation: "+k.getMessage());
       }


//       if(currTime> 0.5){
//
//               long carsInSys= Lane.carIds-TrafficSimulationDesktopView.vehsExited;
//               if((Math.round(currTime*3600))% 2400==0){
//
//                    System.out.println(TrafficSimulationDesktopView.TravelTime+"\t"+TrafficSimulationDesktopView.RampTT+"\t"+(TrafficSimulationDesktopView.TravelTime-TrafficSimulationDesktopView.RampTT)+"\t"+Lane.carIds+"\t");//appends the string to the file
//                    System.exit(0);
//               }
//
//        }

//       if((Math.round(currTime*3600))% 300==0 && currTime > 0 && (Math.round(currTime*3600)) < 1600 ){
//               System.out.println("OD changed: "+(Math.round(currTime*3600)));
//              loadNextODMFile((int)(Math.round(currTime*3600)));
//       }
//       if((Math.round(currTime*3600))== 500 ){
//               System.out.println("OD changed: "+(Math.round(currTime*3600)));
//              loadNextODMFile((int)(Math.round(currTime*3600)));
//       }
//       if((Math.round(currTime*3600))== 800 ){
//               System.out.println("OD changed: "+(Math.round(currTime*3600)));
//              loadNextODMFile((int)(Math.round(currTime*3600)));
//       }
//       if((Math.round(currTime*3600))== 1100 ){
//               System.out.println("OD changed: "+(Math.round(currTime*3600)));
//              loadNextODMFile((int)(Math.round(currTime*3600)));
//       }
//       if((Math.round(currTime*3600))== 1500 ){
//               System.out.println("OD changed: "+(Math.round(currTime*3600)));
//              loadNextODMFile((int)(Math.round(currTime*3600)));
//       }


       if((Math.round(currTime*3600))%DetectorStation.AggregationTime ==0 && currTime>0){

           for(Connection contn : graph.edgeSet()){   // VDS Calculation

               int vdscount = 0;
               if(contn.t==1 && contn.isOnRamp==0 && contn.isOffRamp==0){
                       for(vdscount = 0; vdscount < contn.stations.length ; vdscount++ ){
                           contn.stations[vdscount].insertIntoDB();
                       }
               }
               if(contn.isOnRamp==1){
                   contn.stations[0].insertIntoDB();
                   contn.stations[1].insertIntoDB();
                   contn.stations[2].insertIntoDB();
                   contn.stations[3].insertIntoDB();
                   contn.stations[4].insertIntoDB();
               }
           }

       }

//           for(int i=0; i<allRMs.size();i++){
////               allRMs.get(i).ConnRM.stations[0].insertIntoDB();
//               allRMs.get(i).ConnRM.stations[1].insertIntoDB();
//           }


           // Queue Backup Check

//           for(Connection contn : graph.edgeSet()){
//               int AvgIntervalsFlush = 6;
//
//               if(contn.isOnRamp==1 || contn.ConnId==0 ){
//                    int vol=0;
//                    double vel=0;
//                    for(int j=0;j<Math.min(contn.stations[0].stndata.size(),AvgIntervalsFlush);j++){
//                        vol+= contn.stations[0].stndata.get(j)[0];
//                        vel+= contn.stations[0].stndata.get(j)[0]*contn.stations[0].stndata.get(j)[1];  // space mean speed
//                    }
//                    vel = vol>0? vel/vol: contn.u;
//
//                    if(vel < 30){
//                         TrafficSimulationDesktopView.TravelTime = 10000;
//                    }
//               }
//           }

//           if(ModeManager.ALINEA)
//                ModeManager.ALINEA();
//
////
////--------------check if queue flush is needed--------------
//
//        try{
//
//                int AvgIntervalsFlush = 6;
//                double multiplier = 3600/(DetectorStation.AggregationTime*AvgIntervalsFlush);
//
//
//                RampMeter RM;
//                for(int i=0; i<allRMs.size(); i++){
//                    int vol=0;
//                    double vel=0;
//                    double den=0;
//                    RM = Simulation.allRMs.get(i);
//
//
//                    for(int j=0;j<Math.min(RM.ConnRM.stations[1].stndata.size(),AvgIntervalsFlush);j++){
//                        vol+= RM.ConnRM.stations[2].stndata.get(j)[0];
//                        vel+= RM.ConnRM.stations[2].stndata.get(j)[0]*RM.ConnRM.stations[2].stndata.get(j)[1];  // space mean speed
//                    }
//                    vel = vol>0? vel/vol:RM.ConnRM.u;
//
//                    if(vel<RM.ConnRM.u*0.99)
//                        den=RM.ConnRM.kj-(vol*multiplier/RM.ConnRM.w);
//                    else
//                        den = vol*multiplier/vel;
//
//                    if(den>RM.flushUpOcc && !RM.QFlushFlag){  //averaging over a 2 minute period
//                        RM.QFlushFlag=true;
//                        RM.flushCount++;
//                        RM.flushStartTime = TrafficSimulationDesktopView.t;
////                gw.write(RM+"\t"+TrafficSimulationDesktopView.t+"\t");//appends the string to the file
//                    }
//                    if(den<RM.flushDownOcc && RM.QFlushFlag){
//                        RM.QFlushFlag=false;
////                gw.write(RM+"\t End Time: \t"+TrafficSimulationDesktopView.t+"\t Duration: \t"+(TrafficSimulationDesktopView.t-RM.flushStartTime));//appends the string to the file
//                    }
//
//                    //Queue Flush VSL
//
//
//                    if(RM.QFlushFlag==true){
//                           for(Connection contn : graph.edgeSet()){
//                               Connection tempconvds0 = null ;
//                               int tempconvdsnum0 = 0 ;
//                               Connection tempconvds1 = null ;
//                               int tempconvdsnum1 = 0 ;
//                               Connection tempconvds2 = null ;
//                               int tempconvdsnum2 = 0 ;
//                               Connection tempconvds3 = null ;
//                               int tempconvdsnum3 = 0 ;
//                               Connection tempconvds4 = null ;
//                               int tempconvdsnum4 = 0 ;
//
//
//                               if(contn.vslnumber > 0 ){       // If Conn has VSLs..
//                                      for(int vdn = 0; vdn < contn.vsls[0].numberofvdsofvsl; vdn++){    // Linking VDSs..
//                                           for(Connection contn2 : graph.edgeSet()){
//                                               if(contn2.stations!=null ){
//                                                   for(int sn = 0; sn < contn2.stations.length; sn++){
//                                                        if(contn.vsls[0].connectedvdsid[vdn]==contn2.stations[sn].stationid){
//                                                            if(vdn==0){
//                                                                tempconvds0 = contn2;
//                                                                tempconvdsnum0 = sn;
//                                                                tempconvds0.stations[tempconvdsnum0] = contn2.stations[sn];
//                                                            }
//                                                            else if(vdn==1){
//                                                                tempconvds1 = contn2;
//                                                                tempconvdsnum1 = sn;
//                                                                tempconvds1.stations[tempconvdsnum1] = contn2.stations[sn];
//                                                            }
//                                                            else if(vdn==2){
//                                                                tempconvds2 = contn2;
//                                                                tempconvdsnum2 = sn;
//                                                                tempconvds2.stations[tempconvdsnum2] = contn2.stations[sn];
//                                                            }
//                                                            else if(vdn==3){
//                                                                tempconvds3 = contn2;
//                                                                tempconvdsnum3 = sn;
//                                                                tempconvds3.stations[tempconvdsnum3] = contn2.stations[sn];
//                                                            }
//                                                            else if(vdn==4){
//                                                                tempconvds4 = contn2;
//                                                                tempconvdsnum4 = sn;
//                                                                tempconvds4.stations[tempconvdsnum4] = contn2.stations[sn];
//                                                            }
//                                                        }
//                                                   }
//                                               }
//                                           }
//                                       }
//
//                                       contn.vsls[0].calculateVSL(Math.round(currTime*3600), tempconvds0.stations[tempconvdsnum0], tempconvds1.stations[tempconvdsnum1],tempconvds2.stations[tempconvdsnum2],tempconvds3.stations[tempconvdsnum3],tempconvds4.stations[tempconvdsnum4]);
//
//                                }
//
//                           }
//
//                    }else{
//                        for(Connection contn : graph.edgeSet())
//                            if(contn.ConnId==0){
//                                 contn.vsls[0].setVSL = 100;
//                            }
//
//
//                    }
//
//
//                    //-- Queue FLush VSL
//               }
//
//              } catch (Exception e){
//                  System.out.println("Error in writing FlushTiming file: "+e.getMessage());
//              }
//

////-----------------------Queue Flush

        if((Math.round(currTime*3600))%(DetectorStation.AggregationTime*6) ==0 && currTime>0){
             if(ModeManager.ALINEA)
                ModeManager.ALINEA();
        }
//
////
////--------------check if queue flush is needed--------------
//
//            try{
//
//                int AvgIntervalsFlush = 6;
//                double multiplier = 3600/(DetectorStation.AggregationTime*AvgIntervalsFlush);
//
//
//                RampMeter RM;
//                for(int i=0; i<allRMs.size(); i++){
//                    int vol=0;
//                    double vel=0;
//                    double den=0;
//                    RM = Simulation.allRMs.get(i);
//
//
//                    for(int j=0;j<Math.min(RM.ConnRM.stations[2].stndata.size(),AvgIntervalsFlush);j++){
//                        vol+= RM.ConnRM.stations[2].stndata.get(j)[0];
//                        vel+= RM.ConnRM.stations[2].stndata.get(j)[0]*RM.ConnRM.stations[2].stndata.get(j)[1];  // space mean speed
//                    }
//                    vel = vol>0? vel/vol:RM.ConnRM.u;
//
//                    if(vel<RM.ConnRM.u*0.99)
//                        den=RM.ConnRM.kj-(vol*multiplier/RM.ConnRM.w);
//                    else
//                        den = vol*multiplier/vel;
//
//                    if(den>RM.flushUpOcc && !RM.QFlushFlag){  //averaging over a 2 minute period
//                        RM.QFlushFlag=true;
//                        RM.flushCount++;
//                        RM.flushStartTime = TrafficSimulationDesktopView.t;
////                gw.write(RM+"\t"+TrafficSimulationDesktopView.t+"\t");//appends the string to the file
//                    }
//                    if(den<RM.flushDownOcc && RM.QFlushFlag){
//                        RM.QFlushFlag=false;
////                gw.write(RM+"\t End Time: \t"+TrafficSimulationDesktopView.t+"\t Duration: \t"+(TrafficSimulationDesktopView.t-RM.flushStartTime));//appends the string to the file
//                    }
//
//                    //Queue Flush VSL
//
//
//                    if(RM.QFlushFlag==true){
//                           for(Connection contn : graph.edgeSet()){
//                               Connection tempconvds0 = null ;
//                               int tempconvdsnum0 = 0 ;
//                               Connection tempconvds1 = null ;
//                               int tempconvdsnum1 = 0 ;
//                               Connection tempconvds2 = null ;
//                               int tempconvdsnum2 = 0 ;
//                               Connection tempconvds3 = null ;
//                               int tempconvdsnum3 = 0 ;
//                               Connection tempconvds4 = null ;
//                               int tempconvdsnum4 = 0 ;
//
//
//                               if(contn.vslnumber > 0 ){       // If Conn has VSLs..
//                                      for(int vdn = 0; vdn < contn.vsls[0].numberofvdsofvsl; vdn++){    // Linking VDSs..
//                                           for(Connection contn2 : graph.edgeSet()){
//                                               if(contn2.stations!=null ){
//                                                   for(int sn = 0; sn < contn2.stations.length; sn++){
//                                                        if(contn.vsls[0].connectedvdsid[vdn]==contn2.stations[sn].stationid){
//                                                            if(vdn==0){
//                                                                tempconvds0 = contn2;
//                                                                tempconvdsnum0 = sn;
//                                                                tempconvds0.stations[tempconvdsnum0] = contn2.stations[sn];
//                                                            }
//                                                            else if(vdn==1){
//                                                                tempconvds1 = contn2;
//                                                                tempconvdsnum1 = sn;
//                                                                tempconvds1.stations[tempconvdsnum1] = contn2.stations[sn];
//                                                            }
//                                                            else if(vdn==2){
//                                                                tempconvds2 = contn2;
//                                                                tempconvdsnum2 = sn;
//                                                                tempconvds2.stations[tempconvdsnum2] = contn2.stations[sn];
//                                                            }
//                                                            else if(vdn==3){
//                                                                tempconvds3 = contn2;
//                                                                tempconvdsnum3 = sn;
//                                                                tempconvds3.stations[tempconvdsnum3] = contn2.stations[sn];
//                                                            }
//                                                            else if(vdn==4){
//                                                                tempconvds4 = contn2;
//                                                                tempconvdsnum4 = sn;
//                                                                tempconvds4.stations[tempconvdsnum4] = contn2.stations[sn];
//                                                            }
//                                                        }
//                                                   }
//                                               }
//                                           }
//                                       }
//
//                                       contn.vsls[0].calculateVSL(Math.round(currTime*3600), tempconvds0.stations[tempconvdsnum0], tempconvds1.stations[tempconvdsnum1],tempconvds2.stations[tempconvdsnum2],tempconvds3.stations[tempconvdsnum3],tempconvds4.stations[tempconvdsnum4]);
//
//                                }
//
//                           }
//
//                    }else{
//                        for(Connection contn : graph.edgeSet())
//                            if(contn.ConnId==1){
//                                 contn.vsls[0].setVSL = 100;
//                            }
//                    }
//
// //-------------------- Queue FLush VSL
//                   }
//
//              } catch (Exception e){
//                  System.out.println("Error in writing FlushTiming file: "+e.getMessage());
//              }
//
//
// //--------------------- Queue Flush 2
//
//           try{
//                int AvgIntervalsFlush = 6;
//                double multiplier = 3600/(DetectorStation.AggregationTime*AvgIntervalsFlush);
//
//                RampMeter RM;
//                for(int i=0; i<allRMs.size(); i++){
//                    int vol=0;
//                    double vel=0;
//                    double den=0;
//                    RM = Simulation.allRMs.get(i);
//
//                    for(int j=0;j<Math.min(RM.ConnRM.stations[1].stndata.size(),AvgIntervalsFlush);j++){
//                        vol+= RM.ConnRM.stations[1].stndata.get(j)[0];
//                        vel+= RM.ConnRM.stations[1].stndata.get(j)[0]*RM.ConnRM.stations[1].stndata.get(j)[1];  // space mean speed
//                    }
//                    vel = vol>0? vel/vol:RM.ConnRM.u;
//
//                    if(vel<RM.ConnRM.u*0.99)
//                        den=RM.ConnRM.kj-(vol*multiplier/RM.ConnRM.w);
//                    else
//                        den = vol*multiplier/vel;
//
//                    if(den>RM.flushUpOcc && !RM.QFlushFlag_2){  //averaging over a 2 minute period
//                        RM.QFlushFlag_2=true;
//                    }
//                    if(den<RM.flushDownOcc && RM.QFlushFlag_2){
//                        RM.QFlushFlag_2=false;
//                  }
//
//
//                //--------VSL2
//
//                    if(RM.QFlushFlag_2==true){
//                           for(Connection contn : graph.edgeSet()){
//                               Connection tempconvds0 = null ;
//                               int tempconvdsnum0 = 0 ;
//                               Connection tempconvds1 = null ;
//                               int tempconvdsnum1 = 0 ;
//                               Connection tempconvds2 = null ;
//                               int tempconvdsnum2 = 0 ;
//                               Connection tempconvds3 = null ;
//                               int tempconvdsnum3 = 0 ;
//                               Connection tempconvds4 = null ;
//                               int tempconvdsnum4 = 0 ;
//
//
//                               if(contn.vslnumber > 0 ){       // If Conn has VSLs..
//                                      for(int vdn = 0; vdn < contn.vsls[0].numberofvdsofvsl; vdn++){    // Linking VDSs..
//                                           for(Connection contn2 : graph.edgeSet()){
//                                               if(contn2.stations!=null ){
//                                                   for(int sn = 0; sn < contn2.stations.length; sn++){
//                                                        if(contn.vsls[0].connectedvdsid[vdn]==contn2.stations[sn].stationid){
//                                                            if(vdn==0){
//                                                                tempconvds0 = contn2;
//                                                                tempconvdsnum0 = sn;
//                                                                tempconvds0.stations[tempconvdsnum0] = contn2.stations[sn];
//                                                            }
//                                                            else if(vdn==1){
//                                                                tempconvds1 = contn2;
//                                                                tempconvdsnum1 = sn;
//                                                                tempconvds1.stations[tempconvdsnum1] = contn2.stations[sn];
//                                                            }
//                                                            else if(vdn==2){
//                                                                tempconvds2 = contn2;
//                                                                tempconvdsnum2 = sn;
//                                                                tempconvds2.stations[tempconvdsnum2] = contn2.stations[sn];
//                                                            }
//                                                            else if(vdn==3){
//                                                                tempconvds3 = contn2;
//                                                                tempconvdsnum3 = sn;
//                                                                tempconvds3.stations[tempconvdsnum3] = contn2.stations[sn];
//                                                            }
//                                                            else if(vdn==4){
//                                                                tempconvds4 = contn2;
//                                                                tempconvdsnum4 = sn;
//                                                                tempconvds4.stations[tempconvdsnum4] = contn2.stations[sn];
//                                                            }
//                                                        }
//                                                   }
//                                               }
//                                           }
//                                       }
//
//                                       contn.vsls[0].calculateVSL2(Math.round(currTime*3600), tempconvds0.stations[tempconvdsnum0], tempconvds1.stations[tempconvdsnum1],tempconvds2.stations[tempconvdsnum2],tempconvds3.stations[tempconvdsnum3],tempconvds4.stations[tempconvdsnum4]);
//
//                                }
//
//                           }
//
//                    }else{
//                        for(Connection contn : graph.edgeSet())
//                            if(contn.ConnId==1){
//                                 contn.vsls[0].setVSL = 100;
//                            }
//                    }
//
//                //-----------------
//               }
//
//
//              } catch (Exception e){
//                  System.out.println("Error in writing FlushTiming file: "+e.getMessage());
//              }
//
//
//
//
//
//
//
//
////-----------------------Queue Flush--------------------------------
      
       
//    update Signal

        for(int i=0; i< Simulation.allTLs.size(); i++){
            TrafficLight TL = Simulation.allTLs.get(i);
            TL.setRedLight(TL, Math.round(currTime*3600));
        }


       //       System.out.println("***b4 update LC*****");
       for(Connection contn : graph.edgeSet())
           contn.updateLC();
//       System.out.println("***b4 updateLid****");
       for(Connection contn : graph.edgeSet())       //leaders are recalculated so that the leaders did not change due to lanechanges
           contn.updateLid();
//       System.out.println("***b4 updatev****");
       for(Connection contn : graph.edgeSet())
           contn.updateV();
//       System.out.println("***b4 updateX*****");       
       for(Connection contn : graph.edgeSet())
           contn.updateX();
//       System.out.println("***b4 update transfer*****");       
       for(Connection contn : graph.edgeSet())       //transfers vehicles to next section - only the vehicles whose x is > lane.length
           contn.updateTransfer();
       //Update NexttoStop
//       for(Connection contn : graph.edgeSet())       
//           contn.updateStop();


         currTime = currTime + dt;
        
   }

   public void loadNextODMFile(int p) throws Exception
   {            
        double flowTotal = 0;

        FileInputStream fstream = new FileInputStream("C:\\Users\\hcho95\\Desktop\\GTsimLab\\OD\\OD"+p+".txt");

        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));        
        for(int i = 0; i < this.originList.size(); i++)   //change the flow value for each OD pair from here (for each lane)
        {
            int numMissedDest=0;
            Intersection tempsrcInt = this.originList.get(i);
            sp = new BellmanFordShortestPath(graph, tempsrcInt);

            for(int j = 0; j < this.destinationList.size(); j++)
            {
                   this.origDestMatrix[i][j] = Integer.parseInt(br.readLine());
                   double q = this.origDestMatrix[i][j];                                 // Vehicles/hr 
                   flowTotal+=q;
                   Intersection tempdestInt = this.destinationList.get(j);               // Destination Intersection
                   if (sp.getPathEdgeList(tempdestInt)!=null) {
                       java.util.List<Connection> spList = (java.util.List<Connection>)sp.getPathEdgeList(tempdestInt); // The shortest path                       
                       Connection srcCon = spList.get(0);                                                           // The current segment
//                       for(int ln = 0; ln < srcCon.laneCount; ln++)                     // ln = 0 -> 1 by Hyun
//                             srcCon.lanes[ln].changePrototypeQ(j-numMissedDest, q/(srcCon.laneCount));

                       //--- Lane Flow Distribution
                       if(srcCon.ConnId==0){

                           double median_flow = myMath.min(2500,q/3 );
                           double center_flow = myMath.min(2500,q/3 );
                           double shoulder_flow = myMath.min(2500,q-median_flow-center_flow);

                           srcCon.lanes[0].changePrototypeQ(j-numMissedDest, shoulder_flow);
                           srcCon.lanes[1].changePrototypeQ(j-numMissedDest, center_flow);
                           srcCon.lanes[2].changePrototypeQ(j-numMissedDest, median_flow);
                           
                       }else{
                           for(int ln = 0; ln < srcCon.laneCount; ln++)                     // ln = 0 -> 1 by Hyun
                               srcCon.lanes[ln].changePrototypeQ(j-numMissedDest, q/(srcCon.laneCount));
                       }
                       //----
                   }
                   else
                       numMissedDest += 1; 
            }
       }                                                                        //change the flow value for each OD pair till here^^^^
        if(flowTotal==0) 
            TrafficSimulationDesktopView.fwyInflowFlag = false;  // if all the ODs are 0, then inflow into the fwy is 0. so no more vehicles generated.
        for(int i = 0; i < this.originList.size(); i++)                         //change the cumulativeCumulativeProtoProbTable values for each origin (for each lane) from here
        {            
            Intersection tempsrcInt = this.originList.get(i);
            sp = new BellmanFordShortestPath(graph, tempsrcInt);

            Intersection tempdestInt = this.destinationList.get(0);                                          // Destination Intersection; we use 0 because all origins go to this destination - it is the "d/s fwy" destination
            if (sp.getPathEdgeList(tempdestInt)!=null)
            {
                java.util.List<Connection> spList = (java.util.List<Connection>)sp.getPathEdgeList(tempdestInt); // The shortest path                       
                Connection srcCon = spList.get(0);                                                           // The current segment
                for(int ln = 0; ln < srcCon.laneCount; ln++)  
                    srcCon.lanes[ln].changeCumulativeProtoProbTable();         
            }
       }                                                                        //change the cumulativeCumulativeProtoProbTable values for each origin (for each lane) till here ^^^^^
       fstream.close();
       in.close();
       br.close();
    }



/*
   * display
   */
   public synchronized void paintAll(){
       if(dispInscn != null){
           paintComponents(bg);
           repaint();
//           if (counter == 300) e.finish();
       }
       counter++;
   }

  /*
   * Overriden paint function
   */
   @Override
   public void paint(Graphics g) {
       Graphics2D g2 = (Graphics2D)g;
       if (buffer != null)
           g2.drawImage(buffer, 0, 0, null);
   }

  /*
   * Actulally paints the simulaltion window
   */
   private void  paintComponents(Graphics2D g){

       int xm = 0, ym = 0, d=25, xsizetemp;

       // Types of scenarios:
       //       1__________|_________________________              // the equations for these lines are numbered below
       // Con1  7__________|_     __________________5  Con3
       // Con2  4__________|_____/6
      //                 0 |
       //       ____________________________________
       // Con1  8______________    _______________10  Con3
       //                     11\___________________9  Con2

       //       1___________________________________              // the equations for these lines are numbered below
       //       2_ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ 
       // Con1  12__________________________________  Con3

       //       ________________
       // Con1  ________________

       //                       ___________________
       //                       ___________________  Con3
              
       Connection con1 = null;                                          // First part of the connection  (Inflow/null)
       Connection con2 = null;                                          // Second part of the connection (Ramp/Exit/null)
       Connection con3 = null;                                          // Third part of the connection  (OutFlow/null)
       
       // Assign con1,con2 and con3
       if(dispInscn.type == Intersection.RAMP||dispInscn.type == Intersection.RAMP2){
           for(Connection cc : graph.edgeSet()){
               if((graph.getEdgeTarget(cc)) == this.dispInscn){
                    if(cc.isOnRamp == 1)
                       con2 = cc;                        
                    else
                        con1 = cc;                                            
               } 
               else if((graph.getEdgeSource(cc)) == this.dispInscn)
                   con3 = cc;               
           }
       }
       else if(dispInscn.type == Intersection.EXIT||dispInscn.type == Intersection.EXIT2||dispInscn.type == Intersection.EXIT3){
           for(Connection cc : graph.edgeSet()){
               if((graph.getEdgeTarget(cc)) == this.dispInscn)
                   con1 = cc;               
               else if((graph.getEdgeSource((Connection)cc)) == this.dispInscn){                   
                    if(cc.isOffRamp == 1)
                       con2 = cc;
                    else
                       con3 = cc;
               }
           }
       }
       else{
           for(Connection cc : graph.edgeSet()){
               if((graph.getEdgeTarget(cc)) == this.dispInscn)
                   con1 = cc;               
               else if((graph.getEdgeSource((Connection)cc)) == this.dispInscn) 
                   con3 = cc;               
           }
       }
       if((con1 == null) && (con2 == null) && (con3 == null))// && (con4 == null) && (con5 == null)&& (con6 == null) && (con7 == null))
           return;

       // Draw Con 1
       if(con1 != null)
       {           
           if(con3!=null)
               xsizetemp = (int)(xsize*con1.length/(con1.length+con3.length));  //x-coordinate of the right-edge of con1
           else
               xsizetemp = xsize;
           
           drawSegment(g, con1, xm, ym, xsizetemp, d, graph.getEdgeSource(con1), graph.getEdgeTarget(con1));

           g.setColor(Color.BLACK);                                            // line differentiating the adjasent segments
           g.fillRect(xsizetemp, ym, 2, con1.laneCount*d);                                //-------0--------
       }

       // Draw Con 3
       if(con3 != null)
       {
           if(con1!=null)
               xm = (int) (xsize*con1.length/(con1.length+con3.length)); 
           ym = 0;
           xsizetemp = xsize;
           drawSegment(g, con3, xm, ym, xsizetemp, d, graph.getEdgeSource(con3), graph.getEdgeTarget(con3));
       }

       // Draw Con 2 
       if(con2 != null){

           if(dispInscn.type == Intersection.RAMP ||dispInscn.type == Intersection.RAMP2){
               xm = 0;
               ym = con1.laneCount*d;
               xsizetemp = (int)(xsize*con1.length/(con1.length+con3.length));
           }
           else{
               xm = (int)(xsize*con1.length/(con1.length+con3.length));
               ym = con3.laneCount*d;
               xsizetemp = xsize;
           }
           drawRampExit(g, con2, xm, ym, xsizetemp, d);
       }       
   }
   
   private void drawSegment(Graphics2D a, Connection cn, int beginX, int beginY, int endX, int laneWidth, Intersection xx, Intersection yy){
       int d = laneWidth;                                                             // Lane width
       int ds;
       Graphics2D g = a;
       Connection con = cn;
       int dd = 80;
       int xm = beginX;
       int ym = beginY;       
       int xsizetemp = endX;
       Intersection source = xx;
       Intersection target = yy;
       
       int laneCount = con.laneCount;
       double f = (xsizetemp - xm)/((double)(con.length*1000));

       // PAINT GREY RECTANGE (ALL ROADS)
       g.setColor(new Color((int)(255*0.97),(int)(255*1),(int)(255*0.91) )) ;
       g.fillRect(xm, ym, xsizetemp-xm, d*laneCount + 4);

       // PAINT TOP LINE
       g.setColor(Color.BLACK);
       g.fillRect(xm, ym, xsizetemp-xm, 2);            //-------1--------

       // PAINT DOTTED LANE LINES
       ym += d;
       for (int i=0; i < laneCount-1; i++){     // note that it is lancount-1 so that there will be a boundary line that is solid and thick
           g.setColor(Color.GRAY);

           int x = xm;
           while (x < xsizetemp){
               g.fillRect(x, ym, 10, 1);               //-------2--------
               x += 20;
           }
           ym += d;
       }

       if(source.type == Intersection.RAMP){
           g.setColor(Color.BLACK);
           int Acc = (int)(f*con.AccLaneLength*1000.0);               

           int L0 = (int)(f * dispInscn.gore*1000.0);
           g.fillRect(xm, ym, L0+Acc, 2);                            //-------4--------

           ym -= d;

           int L1 = xsizetemp - xm - L0 - dd;               
           g.fillRect(xm + L0 + dd+Acc, ym, L1-Acc, 2);                  //-------5--------
           g.drawLine(xm + L0 + dd+Acc, ym, xm + L0+Acc, ym+d);             //-------6--------
           g.drawLine(xm + L0 + dd+Acc, ym+1, xm + L0+Acc, ym+d+1);        //-------6--------
           g.fillRect(xm, ym, L0+Acc, 2);                                  //-------7-------- same equation as  ------4----- but ym is different
       }
       else if(target.type == Intersection.EXIT){
           g.setColor(Color.BLACK);
           int Dcc = (int)(f*con.DecLaneLength*1000.0);               //move the diverge location according to the deceleration lane length
           int L3 = (int)(f * dispInscn.gore*1000.0);

           ym -= d;

           int L1 = xsizetemp - xm - L3 - dd;
           g.fillRect(xm, ym, L1-Dcc, 2);                             //-------8--------
           g.fillRect(xm + L1 + dd-Dcc, ym+d, L3+Dcc, 2);             //-------9--------
           g.fillRect(xm + L1 + dd-Dcc, ym, L3+Dcc, 2);               //-------10-------
           g.drawLine(xm + L1 + dd-Dcc, ym + d, xm + L1-Dcc, ym);         //-------11-------- 
           g.drawLine(xm + L1 + dd-Dcc, ym + d + 1, xm + L1-Dcc, ym + 1); //-------11--------
       }
       else{
           g.setColor(Color.BLACK);
           g.fillRect(xm, ym , xsizetemp - xm, 2);                     //-------12--------
       }

       // PAINT LOOP DETECTORS                       //turnoff loops
//       g.setColor(Color.BLACK);
//       for(int i=0;i< (int)(3 * con.length); i++)
//       {
//           g.fillRect((int)(xm + i * (xsizetemp-xm)/3), ym + 36, 2, 20);
//           g.drawOval((int)(xm + i * (xsizetemp-xm)/3)-2, ym + 30, 5, 5);
//       }

       //---Signal


       if(cn.isSignalized==1){
           // STOP LINE
           int r = 5;
           int L0 = (int)(f * cn.length * 1000.0);
//           int x0 = xm + (int)(L0-(200*f))+ 2*r;
           int x0 = xm + (int)(L0-(900*f))+ 2*r;
//           g.setColor(Color.BLACK);
//           g.fillRect(x0 , ym + 2 , 2, d -4);

//           // Write "STOP"
//           x0 += 6;
//           g.translate(x0, ym + 2);
//           g.rotate(Math.PI/2.0);
//           g.setFont(new Font("sansserif", Font.BOLD, 8));
//           g.drawString("STOP",0,0);
//           g.rotate(-Math.PI/2.0);
//           g.translate(-x0, -ym - 2);
//           g.setFont(new Font("sansserif", Font.BOLD, 12));

           // Light
//               ym += laneCount+0.5*d;
           ym += 1.2*d;
//           x0 = xm+(int)(L0-(200*f));
           x0 = xm+(int)(L0-(30*f));
           g.setColor(Color.DARK_GRAY);
           g.fillRect(x0, ym, 2*r + 4, 4*r + 6);
//           g.setColor(Color.BLACK);
           g.fillOval(x0+2, ym+2, 2*r, 2*r);
           g.fillOval(x0+2, ym+2 + 2*r + 2, 2*r, 2*r);
//           g.drawString("Rate: ",x0+7+2*r, ym+2+r + 2*r + 2);

           g.setColor(Color.RED);
           g.setBackground(Color.BLACK);
//           bg.setColor (Color.GREEN);
//           g.clearRect(x0+36+2*r, ym+4, 40, 20);
//           g.setColor(Color.BLACK);

           if(!cn.TL.RedLightFlag)
              g.setColor(Color.GREEN);
           else
              g.setColor(Color.RED);
           g.fillOval(x0+2, ym+2, 2*r, 2*r);

//               ym -= laneCount+0.5*d;
           ym -= 1.5*d;
       }







       // PAINT CARS
       ym = -d; 
       for (Lane L : con.lanes){
           for (Vehicle c : L.cars){
               //if ((L.idx != 0) || (c != L.cars.getFirst())) {//not plotting first onramp car
                   int y = ym + d * (laneCount - L.idx) + d/2;
                   g.setColor(c.clr);
                   ds = 3*dotsize;

                   if (c.getvMB() != 999){
                       g.setColor(Color.MAGENTA);
                       ds = 3*dotsize;
                   } 
//                   else if (c.changingUntil > TrafficSimulationDesktopView.t){
//                       y = ym + d *(2*laneCount + 1 - c.omd)/2;
//                   }
                   g.fillRect(xm + (int)(c.getX() * f * 1000.0), y, 2*ds, ds); 
           }
       }       
   } 
   
   private void drawRampExit(Graphics2D a, Connection cn, int beginX, int beginY, int endX, int laneWidth){
       int d = laneWidth;                                                             // Lane width
       int ds;
       Graphics2D g = a;
       Connection con2 = cn;
       int xm = beginX;
       int ym = beginY;       
       int xsizetemp = endX;           
       int laneCount = con2.laneCount;

       double f = (xsizetemp - xm)/((double)(con2.length*1000));

       // PAINT GREY RECTANGE (ALL ROADS)
       g.setColor(new Color((int)(255*0.97),(int)(255*1),(int)(255*0.91) )) ;
       g.fillRect(xm, ym, xsizetemp-xm, d*laneCount + 4);

       // PAINT TOP LINE
       g.setColor(Color.BLACK);
       g.fillRect(xm, ym, xsizetemp-xm, 2);                                 //------7 or 10-----

       // PAINT DOTTED LANE LINES
       ym += d;
       for (int i=0; i < laneCount-1; i++){
           g.setColor(Color.GRAY);

           int x = xm;
           while (x < xsizetemp){
               g.fillRect(x, ym, 10, 1);                                    //------2-----
               x += 20;
           }
           ym += d;
       }

       if(con2.lane0Type == Lane.NORMAL){
           g.setColor(Color.BLACK);
           g.fillRect(xm, ym , xsizetemp - xm, 2);                          //------4 or 9-----
       }

       ym -= d;

       if(con2.isOnRamp==1){
           // STOP LINE
           int r = 5;
           int L0 = (int)(f * con2.length * 1000.0);
//           int x0 = xm + (int)(L0-(200*f))+ 2*r;
           int x0 = xm + (int)(L0-(900*f))+ 2*r;
           g.setColor(Color.BLACK);
           g.fillRect(x0 , ym + 2 , 2, d -4);

           // Write "STOP"
           x0 += 6;
           g.translate(x0, ym + 2);
           g.rotate(Math.PI/2.0);
           g.setFont(new Font("sansserif", Font.BOLD, 8));
           g.drawString("STOP",0,0);
           g.rotate(-Math.PI/2.0);
           g.translate(-x0, -ym - 2);
           g.setFont(new Font("sansserif", Font.BOLD, 12));

           // Light
//               ym += laneCount+0.5*d;
           ym += 1.5*d;
//           x0 = xm+(int)(L0-(200*f));
           x0 = xm+(int)(L0-(600*f));
           g.setColor(Color.DARK_GRAY);
           g.fillRect(x0, ym, 2*r + 4, 4*r + 6);
           g.setColor(Color.BLACK);
           g.fillOval(x0+2, ym+2, 2*r, 2*r);
           g.fillOval(x0+2, ym+2 + 2*r + 2, 2*r, 2*r);

           g.drawString("Rate: ",x0+7+2*r, ym+2+r + 2*r + 2);
           g.setColor(Color.RED);
           g.setBackground(Color.BLACK);
//           bg.setColor (Color.GREEN);
           g.clearRect(x0+36+2*r, ym+4, 40, 20);
           if(!con2.RM.QFlushFlag)
               g.drawString(""+con2.RM.meterRate,x0+40+2*r, ym+2+r + 2*r + 2);
           else
               g.drawString("FLUSH",x0+40+2*r, ym+2+r + 2*r + 2);
           g.setColor(Color.BLACK);
           if (con2.lanes[0].nextToStop != null) {
               if (con2.lanes[0].nextToStop.getV()>0) {
                   if(!con2.RM.QFlushFlag)
                       g.setColor(Color.GREEN);
                   else
                       g.setColor(Color.DARK_GRAY);
                   g.fillOval(x0+2, ym+2 + 2*r + 2, 2*r, 2*r);
               } else {
                   if(!con2.RM.QFlushFlag)
                       g.setColor(Color.RED);
                   else
                       g.setColor(Color.DARK_GRAY);
                   g.fillOval(x0+2, ym+2, 2*r, 2*r);
               }
           }
//               ym -= laneCount+0.5*d;
           ym -= 1.5*d;
       }

       // PAINT LOOP DETECTORS
       g.setColor(Color.BLACK);
//       for(int i=0;i< (int)(3 * con2.length); i++)
       if(con2.isOffRamp==1){
           g.fillRect((int)(xm + (xsizetemp-xm)/10), ym + 36, 2, 20);
           g.drawOval((int)(xm + (xsizetemp-xm)/10)-2, ym + 30, 5, 5);
       }
       else{
           g.fillRect((int)(xm + (xsizetemp-xm)/(con2.length*20)), ym + 36, 2, 20);       //station 0 - upstream most station on ramp located at 0.05 km
           g.drawOval((int)(xm + (xsizetemp-xm)/(con2.length*20))-2, ym + 30, 5, 5); 
           
//           g.fillRect((int)(xm + (xsizetemp-xm)*con2.RM.StorageBegin/con2.length), ym + 36, 2, 20);  //station 1 - u/s boundary of ramp storage
//           g.drawOval((int)(xm + (xsizetemp-xm)*con2.RM.StorageBegin/con2.length)-2, ym + 30, 5, 5);
//
           g.fillRect((int)(xm + (xsizetemp-xm)*(con2.length-1.2)/con2.length), ym + 36, 2, 20);  //station 1 - u/s boundary of ramp storage
           g.drawOval((int)(xm + (xsizetemp-xm)*(con2.length-1.2)/con2.length)-2, ym + 30, 5, 5);

           g.fillRect((int)(xm + (xsizetemp-xm)*(con2.length-0.9)/con2.length), ym + 36, 2, 20);  //station 1 - u/s boundary of ramp storage
           g.drawOval((int)(xm + (xsizetemp-xm)*(con2.length-0.9)/con2.length)-2, ym + 30, 5, 5);


           g.fillRect((int)(xm + (xsizetemp-xm)*(con2.length-0.05)/con2.length), ym + 36, 2, 20);       //station 3 - 0.05 km upstream of merge
           g.drawOval((int)(xm + (xsizetemp-xm)*(con2.length-0.05)/con2.length)-2, ym + 30, 5, 5);            
       }

       // PAINT CARS
       ym += - laneCount * d;
       for (Lane L : con2.lanes){
           for (Vehicle c : L.cars){
               //if ((L.idx != 0) || (c != L.cars.getFirst())) {//not plotting first onramp car
                   int y = ym + d * (laneCount - L.idx) + d/2;
                   g.setColor(c.clr);
                   ds = 3*dotsize;

                   if (c.getvMB() != 999){
                       g.setColor(Color.MAGENTA);
                       ds = 3*dotsize;
                   } 
//                   else if (c.changingUntil > TrafficSimulationDesktopView.t){
//                       y = ym + d *(2*laneCount + 1 - c.omd)/2;
//                   }

                   g.fillRect(xm + (int)(c.getX() * f * 1000.0), y, 2*ds, ds);
           }
       }
   }
   public BufferedImage convert(Image im){
       BufferedImage bi = new BufferedImage(im.getWidth(null),im.getHeight(null),BufferedImage.TYPE_INT_RGB);
       Graphics bgl = bi.getGraphics();
       bgl.drawImage(im, 0, 0, null);
       bgl.dispose();
       return bi;
   }

   public void setIntersection(Intersection interscn){
       this.dispInscn = interscn;
   }

   public void setOrigDestMatrix(double matrix[][]){
       this.origDestMatrix = matrix;
   }

   public double[][] getOrigDestMatrix(){
       return this.origDestMatrix;
   }

   public void setGraph(DirectedWeightedMultigraph graph){
       this.graph = graph;
   }

   public DirectedWeightedMultigraph getGraph(){
        return this.graph;
   }

   public void setOriginList(ArrayList<Intersection> orLst){
       this.originList = orLst;
   }

   public void setDestinationList(ArrayList<Intersection> desLst){
       this.destinationList = desLst;
   }

   public ArrayList<Intersection> getDestinationList(){
       return this.destinationList;
   }
}
