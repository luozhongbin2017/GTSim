package trafficsimulationdesktop;
import java.awt.*;
import java.util.*;
import java.text.DecimalFormat;
import java.io.*;
//import java.io.FileInputStream;


public class Lane {

   //General Lane attributes
   public int idx;                                                                // Lane index in fw

   public double length;
   public static double cap;                                                             // Calculated
   public static double u;                                                                 // static -> X 1201 Hyun
   public double kj;
   public double w;
   public double tau;
   public double dt;
   public double epsilon;
   public double sd;                                                               // Sight Distance
   public double qA;
   public double zeta = 0.5;                                                        //used for on-ramp merging
   public double frictionfactor = 30;                                              // used for controlling ff speed on lanes when adjacent lanes are congested

   //public double frictionfactor = 20;                                              // used for controlling ff speed on lanes when adjacent lanes are congested
   public double HOTfrictionfactor = 100;
   public boolean exitLane = false;                                                             // Length in km
//   public double velRedfactor = 10;                                              //velocity is reduced based on the number of missed LC chances
   public double velRedfactor = 10;                                              //velocity is reduced based on the number of missed LC chances
//   public double HOTratio;
   // Attributes related to Ramp/Exit
   public double gore;                                                             // Length in km
//   public double meter;                                                            // Meter rate vph
   public Vehicle nextToStop=null;                                                 //used for ramp metering
   public double nextDep;

   // Attributes defined/modified by Bharat
   public static int NORMAL = 0;
   public static int AuxLane_RAMP = 1;
   public static int AuxLane_EXIT = 2;
   public int type;

   public LinkedList<Vehicle> cars;                                                // The linked list of cars currently traveling the lane
   public Connection section;                                                      // The section of the road grid this lane belongs to 
   
   private ArrayList<VehiclePrototype> vehPrototypeList;                           // The list of all vehicle prototypes
   private ArrayList<Double> cumulativeProtoProbTable;                             // Stores the cumulative prototype-probability values
   //Added by Shalini
   public ArrayList<LaneDetector> LDlist;                                          // list of loop detectors on this lane
   public ArrayList<LaneDetector> VSLlist;                                          // list of VSLs on this lane
   
   public static long carIds = 0;                                                   // Static count of the cars added so far - used for determining VehicleId
   long nCars = 0;                                                                //cars so far added on this lane
   
   DecimalFormat df = new DecimalFormat("#.###");
   
   // Constructor
   public Lane(int idx, double length, double u, double kj, double w, double tau, double dt, double epsilon, double sd, double qA, Connection con, int type) {
       this.idx = idx;
       this.length = length;
       this.u = u;
       this.kj = kj;
       this.w = w;
       this.tau = tau;
       this.dt = dt;
       this.epsilon = epsilon;
       this.sd = sd;
       this.zeta = 0.5;

       if(con.isOffRamp==0 && con.isOnRamp==0)
           this.cap = 1*(w*u/(w+u)*kj);  // Capacity of ramp 0.5 ->1
       else
           this.cap = w*u/(w+u)*kj;
       
       this.qA = qA;                                                               // Note: It is updated later

       this.nextDep = 0;                                                           // Initialized to 0

       this.section = con;
       this.type = type;

       this.cars = new LinkedList<Vehicle>();
       this.vehPrototypeList = new ArrayList<VehiclePrototype>();
       this.cumulativeProtoProbTable = new ArrayList<Double>();
       
       //Added by Shalini
       this.LDlist = new ArrayList<LaneDetector>();
       this.VSLlist = new ArrayList<LaneDetector>();

   }
   
   // Constructor
   public Lane(int idx, double length, double u, double kj, double w, double tau, double dt, double epsilon, double sd, double qA, Connection con) {
       this(idx, length, u, kj, w, tau, dt, epsilon, sd, qA, con, Lane.NORMAL);
   }

   // Adds a new prototype vehicle
   public void addPrototype(Intersection origin, Intersection destination, java.util.List<Connection> path, double q){
       VehiclePrototype proto = new VehiclePrototype(origin, destination, path, q);
       this.vehPrototypeList.add(proto);
   }

   public  ArrayList<VehiclePrototype> getvehPrototypeList()
   {
           return this.vehPrototypeList;
   }
   public  void setvehPrototypeList(ArrayList<VehiclePrototype> p)
   {       
       if(this.section.type != Connection.ORIGIN && this.section.isOnRamp==0 && this.section.isOffRamp==0){
           this.vehPrototypeList.clear();       
       for (VehiclePrototype props: p)
       this.vehPrototypeList.add(props);
       }
       double qTot = 0.0;
       double qCumulative = 0.0;                                                 // Note: qCumulative is updated here       
       
       for(VehiclePrototype props: vehPrototypeList)
           qTot += props.q;
       
       qA = qTot;                                                                // Note: qA is updated here
       
       for(int i=0; i < vehPrototypeList.size(); i++)
       {
           VehiclePrototype prop = vehPrototypeList.get(i);
           qCumulative += prop.q;
           cumulativeProtoProbTable.add(i, qCumulative/qTot);
       }
   }
           
   public void changePrototypeQ(int j, double q){       
       vehPrototypeList.get(j).q = q;
   }
   
   public void changeCumulativeProtoProbTable()
   {       
       double qTot = 0.0;
       double qCumulative = 0.0;                                                 // Note: qCumulative is updated here
       cumulativeProtoProbTable.clear();
       
       for(VehiclePrototype props :vehPrototypeList)
           qTot += props.q;
       
       qA = qTot;                                                                // Note: qA is updated here
       
       
       for(int i=0; i < vehPrototypeList.size(); i++)
       {
           VehiclePrototype prop = vehPrototypeList.get(i);
           qCumulative += prop.q;
           cumulativeProtoProbTable.add(i, qCumulative/qTot);
       }
   }

    public ArrayList<Double> getCumulativeProtoProbTable() {
        return cumulativeProtoProbTable;
    }
         
   private void addInitialVehicles(){               
       Vehicle veh;
       if(this.section.lane0Type==Lane.AuxLane_RAMP && this.idx==0) // a vehicle is added at the end of every onramp so that vehicles do not continue on lane0
       {
           double x = this.gore+Connection.opening+this.section.AccLaneLength;
           veh = new Vehicle(x, this);
           veh.setV(0);
           veh.setCurrSegment(this.section);
           veh.setVehId(Lane.carIds);
//           veh.clr = new java.awt.Color(221, 236, 251);
           veh.clr = Color.RED;
           Lane.carIds++;
           cars.add(veh);            // Create nCars number of cars and add them to the linked list after setting their starting positions
       }
       try{
        for (int i = 0; i<this.section.ConnId;i++)
            nCars= 0;//Integer.parseInt(br.readLine());
        
       double p = this.length-0.04;
       double k=nCars/(p);                              //to ensure the vehicles are not back to back on either side of section interface 
               if(this.type == Lane.NORMAL){    
                   for(int j=0; j<nCars; j++){               
                         int nextAddIndex = getNextPrototypeIndex(TrafficSimulationDesktopView.iniVehR.nextDouble());                         
                         veh = new Vehicle(p - j/k, this, vehPrototypeList.get(nextAddIndex).origin, vehPrototypeList.get(nextAddIndex).destination, vehPrototypeList.get(nextAddIndex).path);
                         veh.setV(myMath.Veq(1/k, veh.getLane().w,veh.getLane().kj, veh.getLane().u));
                         veh.setCurrSegment(this.section);
                         veh.setCurrSegmentIndx(veh.getPath().indexOf(this.section));
                         if(type==Lane.AuxLane_RAMP && veh.getX()>gore)
                               veh.setEnteringVehicle();
                         if(this.section.ConnId!=0){
                         veh.vehDistanceFromExit = 0;                           //this is needed since the setCurrsegment is performed after the vehicle constructor is called, but the vehicle construction calls the
// method to calculate distancefromExit and NearestExit for the vehicle that uses the currIndx. For insert vehicle class this is not a problem because all the origins have currsegmentindx = 0 (the default value) 
                         veh.nearestExit = 0;
                         veh.CalcExitDistNearestExit();
                         }
//                         if(veh.vehDistanceFromExit<=veh.distanceFromExitThreshold && veh.getPath().get(veh.getPath().size()-1).isOffRamp==1)
                         if(veh.vehDistanceFromExit<=veh.distanceFromExitThreshold )
                               veh.setExitingVehicle();           
                         
                         if(this.cars.size()>0)
                             veh.ldr = this.cars.getLast();               
                         veh.setVehId(Lane.carIds);
                         Lane.carIds++;
                         cars.add(veh);            // Create nCars number of cars and add them to the linked list after setting their starting positions               
                   }
             }
               //1st car generated in this lane is assigned to the first LD of the lane
//       if((this.section.lane0Type==Lane.AuxLane_RAMP||this.section.lane0Type==Lane.AuxLane_RAMP) && idx!=0){
        if((this.section.lane0Type==Lane.AuxLane_RAMP||this.section.lane0Type==Lane.AuxLane_RAMP) ){  // idx != 0 is deleted to use detector on AuxLane 030216 Hyun
               if(this.LDlist.get(0).lastCar == null && cars.size()>0)
                   this.LDlist.get(0).lastCar = cars.get(0);
               }
       }
      catch (Exception e){//Catch exception if any
//      System.err.println("Error in Addinitial Vehicles: " + e.getMessage());
      }
      
   }
        
   // Returns the index of the next prototype to be created based on the input probability value
   int getNextPrototypeIndex(double prob){
       int indx;
//       for(indx = 0; (indx < cumulativeProtoProbTable.size()) && (prob > (cumulativeProtoProbTable.get(indx)-TrafficSimulationDesktopView.HOTratio)/(1-TrafficSimulationDesktopView.HOTratio)); indx++){}
         for(indx = 0; (indx < cumulativeProtoProbTable.size()) && (prob > (cumulativeProtoProbTable.get(indx))); indx++){}

       return indx;
   }

   // Adds a new car to the lane
   private void insertVehicle(){
//       double qTot=0;
//       for(VehiclePrototype props :vehPrototypeList)
//           qTot += props.q;
//       double[] tempdata = new double[3];

       Vehicle veh=null;
       try{
       double rand = TrafficSimulationDesktopView.vehGenR.nextDouble();


       int nextAddIndex = getNextPrototypeIndex(rand);

       if (vehPrototypeList.size()>0){
//           double x0 =0;    
           if(cars.size() > 0){
               if (cars.getLast().getX () > 0){
//                   if(cars.getLast().getX()> 0.05){ // 10/5 1-> 0.1 change by hyun
//                        veh = new Vehicle(0, this,   vehPrototypeList.get(nextAddIndex).origin, vehPrototypeList.get(nextAddIndex).destination, vehPrototypeList.get(nextAddIndex).path);
//
//                   }else{
                        double x0 = cars.getLast().getX() - u/qA;
                        veh = new Vehicle(x0, this,   vehPrototypeList.get(nextAddIndex).origin, vehPrototypeList.get(nextAddIndex).destination, vehPrototypeList.get(nextAddIndex).path);

//                   }
               }
           }
           else{
               veh = new Vehicle(0, this,   vehPrototypeList.get(nextAddIndex).origin, vehPrototypeList.get(nextAddIndex).destination, vehPrototypeList.get(nextAddIndex).path);
           }
           if(veh!=null){
              veh.setVehId(Lane.carIds);
               if(this.cars.size()>0)
                   veh.ldr = this.cars.getLast();

               Lane.carIds++;
               
               cars.add(veh);            // Create nCars number of cars and add them to the linked list after setting their starting positions
//               nCars = nCars+1; 
               
               if(this.section.isOnRamp==1 && nextToStop==null)   //&& nextToStop != null  removed
                   nextToStop = cars.getLast();      
           }
       }
      }  catch (Exception e){
          System.err.println("Error in Insert Vehicle: " + e.getMessage());
      }
   }

 
   public void updateAllV()
   {
       
       
           if(this.section.isOnRamp==0 && this.section.isOffRamp==0 && TrafficSimulationDesktopView.t==0)   //add vehicles only on the fwy in the beginning
              addInitialVehicles();
           else if(TrafficSimulationDesktopView.fwyInflowFlag && TrafficSimulationDesktopView.t>0 && this.section.type == Connection.ORIGIN && this.type != Lane.AuxLane_EXIT )        // need to change to accomodate exit2 and exit3
              insertVehicle();      
           
           
           
       int i0 = (this.section.lane0Type==Lane.AuxLane_RAMP && this.idx==0)? 1 : 0;          // vehicle 0 on every onramp is no updated. vehicle 0 was inserted such that vehicles behind it does not continue on lane0 
       for (int i = i0; i < cars.size(); i++) 
       {
           Vehicle c = this.cars.get(i);
           c.setV(Math.max(0,c.getV()-c.missedLCchances*velRedfactor*c.fi));
           c.missedLCchances=0;
           double vel= updateV(c);
           double vel_FF = speedFriction(c);



           c.setV(myMath.min(vel, vel_FF+frictionfactor));        // VSL off

//           c.setV(myMath.min(vel, vel_FF+frictionfactor, c.vsltag));  //VSL on
           


           c.clr = new Color(255-(int)(c.getV()*255/110),(int)(c.getV()*255/110),0);// color code according to speed
       }
   }

     private double updateVcfl (Vehicle c){
       double v = u;
       double s = 999;

       // if car c has a leader
       if (c.ldr != null){
          if(c.ldr.getLane().section.ConnId!=c.getLane().section.ConnId)
               s = c.ldr.getX()+c.getCurrSegment().length- c.getX();
           else
               s = c.ldr.getX()- c.getX();
       }
       v = c.getV();

       v = myMath.min(v + c.getAcc()*dt, this.u , myMath.Veq(s, c.getLane().w,c.getLane().kj, c.getLane().u), c.getvMB());
       
//       v = myMath.min(v + c.getAcc()*dt, this.u,  myMath.Veq(s, c.getLane().w,c.getLane().kj, c.getLane().u), c.getvMB(),c.vsltag);


//       *** Traffic Light ***

       if(this.section.isSignalized ==1 ){
           if (c.getX() >= this.section.length-0.01 ){   // The ramp meter is located at "this.section.length/2"
              if(this.section.TL.RedLightFlag )        // if Red LIght flag is True
                  v = 0;
           }
       }


//       *** Ramp Meter Light ***


       if(this.section.isOnRamp==1&&(nextToStop != null)){

           if (c == nextToStop)
           {
               if (c.getX() >= this.section.length- 1.0)   // The ramp meter is located at "this.section.length/2"
               {

                   double q ;

                   if(this.section.RM.ConnUS.stations[0].stndata.get(0)[0]==0){
                        q = this.cap/1.2;
                   }

                   else if(!this.section.RM.QFlushFlag)
                   {        // if flush flag is false
                       q = this.section.RM.meterRate; // ALINEA
                   }
//                   //----Queue Flush
////                   else
////                       q = this.cap/1.2;
////
//                   //----Queue Flush
//
                   else if(this.section.RM.QFlushFlag && !this.section.RM.QFlushFlag_2)   // if flush 1 True, flush flag 2 is false yet
                   {
                     q = TrafficSimulationDesktopView.beta * this.section.stations[0].calculateCF();
                   }

                   else if(this.section.stations[0].calculateCF()> this.section.RM.meterRate )   // if flush 1 True, flush flag 2 is false yet
                   {
                      q =  this.section.stations[0].calculateCF();
                   }else
                       q = this.section.RM.meterRate; // ALINEA




                   double rand1 = TrafficSimulationDesktopView.vehGenR.nextDouble();
                   if(rand1 < (q*1.2  )/this.cap){
                           int j = cars.indexOf(c);
                           nextToStop = (j != cars.size()-1)? cars.get(j+1) : null;

                   }else{
                         v=0;

                   }

               }
           }
         }



       //----------RM
       return v;
   }



  private double speedFriction(Vehicle c)
 {
      int i= c.getLane().idx;
      double v = u;
      Lane L = c.getLane();

          for (int j = i - 1; j<=i+1 && j < c.getCurrSegment().laneCount; j+=2) 
          {
             if(j>=0 ){
                 L = c.getCurrSegment().lanes[j];
                 if(L.type==Lane.NORMAL)
                     v = Math.min(v, L.getLaneV(c.getX(),c.getTargetLeader(L)));
             }
           
          }


      return v;
  }
  
   public void updateAllX() throws IOException
   {


       int i0 = (this.section.lane0Type==Lane.AuxLane_RAMP && this.idx==0)? 1 : 0;          // vehicle 0 on every onramp is no updated. vehicle 0 was inserted such that vehicles behind it does not continue on lane0
       for(int i = i0; i<cars.size(); i++) 
       {
           Vehicle c = cars.get(i);
           double x0 = c.getX();
           c.setX(updateX(c));
//           if(this.section.ConnId==0 && c.getX()>TrafficSimulationDesktopView.HOTend)
//               c.isHOTveh=false;
           
           // Checks if the vehicle is making an exit - Set AuxLane_EXIT flag for Vehicles
           
           c.vehDistanceFromExit = c.vehDistanceFromExit-(c.getX()-x0);
           if(c.nearestExit >0)
               c.nearestExit = c.nearestExit-(c.getX()-x0);
           else if(c.getCurrSegmentIndx()<c.getPath().size()-2){
               c.nearestExit =0;
               int counter = c.getCurrSegmentIndx()+1;
               int flag = 0;
               while(counter<c.getPath().size()-1 && flag==0)
               {
                   if(!(c.getPath().get(counter).Target==Intersection.EXIT2||
                       c.getPath().get(counter).Target==Intersection.EXIT3)){
                       c.nearestExit = c.nearestExit+c.getPath().get(counter).length;
                   }
                   else{
                       c.nearestExit = c.nearestExit+c.getPath().get(counter).length;           
                       flag=1;
                   }
                   counter=counter+1;
               }
               if(!(this.section.Target==Intersection.EXIT2||this.section.Target==Intersection.EXIT3))
                   c.nearestExit = c.nearestExit+c.getLane().length-c.getX(); 
               else{                   
                   c.nearestExit = c.getLane().length-c.getX();
               }
           }                                    
           if(c.vehDistanceFromExit<=c.distanceFromExitThreshold && c.isExiting()==false && c.getPath().get(c.getPath().size()-1).isOffRamp==1)           
                   c.setExitingVehicle();
//           if(c.vehDistanceFromExit<=c.distanceFromExitThreshold && c.isExiting()==false && c.getCurrSegment().isSignalized==1&& c.getPath().get(3).isOnRamp==1)  // Need to be corrected
//                   c.setExitingVehicle();
//           if(c.nearestExit<=c.distanceFromExitThreshold && c.isExiting()==false && c.getCurrSegment().isSignalized==1&& c.getPath().get(3).isOnRamp==1)  // Need to be corrected
//                   c.setExitingVehicle();
           if( type==Lane.AuxLane_RAMP && c.getX()>gore)
                c.setEnteringVehicle();

//           if(this.section.ConnId==2){
//
//            FileWriter fw = new FileWriter("C:\\Users\\hcho95\\Desktop\\VSLsim\\Results\\ConnID" + this.section.ConnId +".txt",true); //the true will append the new data
//            fw.write(c.getVehId() +"\t"+   Simulation.currTime*3600+"\t"+ c.getX()+"\t"+c.getV()+"\t"+c.vsltag);
//            fw.write("\n");
//            fw.close();
//           }

       }





   }
      
   public void updateAllTransfer()
   {
       // Remove Vehicles that have reached their Destination or Transfer to the next Section
       if(this.section.type == Connection.DESTINATION)
           removeExitingCars();
       else
           transferToNextSection();
   }   
   
   
//   public void updateAllStop(){
//       
//   }
   
   

   private double updateX(Vehicle c){
       
       double x = 0;
       LaneDetector LD = null;
       LaneDetector LDvsl = null;
       x = c.getX() + c.getV()*dt;


       try{

          if(this.section.isOnRamp==0 && this.section.isOffRamp==0 && this.type==Lane.NORMAL && this.section.t==1 ){
//            if(this.section.ConnId>1 &&this.section.isOnRamp==0 && this.section.isOffRamp==0 && this.type==Lane.NORMAL && this.section.t==1 ){


              ///--------ALINEA Detector One Lane Only--------

//              if(this.section.ConnId==2 && x>0.25 && x<0.5){
//                  if(this.idx==1)
//                      LD=this.LDlist.get(0);
//              }else if (x > 0.25 )
//                       LD=this.LDlist.get(((int)(x/0.25))-1);

              //---------------------

              if (x > 0.25 )
                       LD=this.LDlist.get(((int)(x/0.25))-1);


           }

//           else if (this.idx == 0 && this.section.isOnRamp == 0 && this.section.isOffRamp == 0 && this.type == Lane.NORMAL && this.section.t == 1) {
//
//
//              ///--------ALINEA Detector One Lane Only--------
//
////              if(this.section.ConnId==2 && x>0.25 && x<0.5){
////                  if(this.idx==1)
////                      LD=this.LDlist.get(0);
////              }else if (x > 0.25 )
////                       LD=this.LDlist.get(((int)(x/0.25))-1);
//
//              //---------------------
//
//              if (x > 0.25 )
//                       LD=this.LDlist.get(((int)(x/0.25))-1);
//
//
//           }

           else if(this.section.isOnRamp==1){
                    if(x > this.section.stations[0].location && x < this.section.stations[1].location)
                        LD =this.LDlist.get(0);
                    if(x > this.section.stations[1].location && x < this.section.stations[2].location)
                        LD =this.LDlist.get(1);
                    if(x > this.section.stations[2].location && x < this.section.stations[3].location)
                        LD =this.LDlist.get(2);
                    if(x > this.section.stations[3].location && x < this.section.stations[4].location)
                        LD =this.LDlist.get(3);
                    if(x > this.section.stations[4].location )
                        LD =this.LDlist.get(4);
           }
           else if(this.section.isOffRamp==1){
               if(x>0.1){
                       LD = this.LDlist.get(0);
               }
           }


           if((LD!=null) && (LD.lastCar!=c) && (c.CountedAt!=(int)LD.station.stationid) ){     //countedAt is used to make sure lane changed vehile is not double counted at the u/s detector

               LD.station.N++;
               LD.station.cumVel+=c.getV()==0 ? 0:1/c.getV(); // spacemean speed
               LD.lastCar = c;
               c.CountedAt = (int)LD.station.stationid;

           }


//--------RM 011817 Use this for VSL

//          if( this.section.vslnumber>0 ){
//              if(this.section.vsls.length==1 ){
//                    if(x > this.section.vsls[0].location)
//                        LDvsl = this.VSLlist.get(0);
//              }else if(this.section.vsls.length==2){
//                    if(x > this.section.vsls[0].location && x < this.section.vsls[1].location)
//                        LDvsl =this.VSLlist.get(0);
//                    if(x > this.section.vsls[1].location)
//                        LDvsl =this.VSLlist.get(1);
//              }
//           }
//          if(LDvsl==null){
//               c.vsltag = this.u;
//          }
//          else if((LDvsl != null) && (LDvsl.lastCar != c) && (c.CountedVSL != (int) LDvsl.vsl.variablespeedlimitId)) {     //countedAt is used to make sure lane changed vehile is not double counted at the u/s detector
//
//               LDvsl.lastCar = c;
////               c.vsltag = LDvsl.vsl.setVSL;
//               if(LDvsl ==this.VSLlist.get(0)){
//
//
////                   c.vsltag = LDvsl.vsl.setVSL;
//
//
//                   //--- Shoulder VSL
//
//                   if(this.idx==0){
//                        c.vsltag = LDvsl.vsl.setVSL;
//                   }else
//                       c.vsltag = this.u;
//
//                   //---
//               }else
//                   c.vsltag = this.u;
//
////               c.tempvsltag = c.vsltag;
//               c.CountedVSL = (int)LDvsl.vsl.variablespeedlimitId;
//           }


       }catch(Exception e){
           System.out.println("Error in updating DB for LD "+e.getMessage());
       }
       return x;
   }

   public double getLaneV(double x0, int j){       
       //Min speed in segment of length sd d/s of x0; j first veh dows of x0
       double v = u;
       double s=99999;
       Vehicle c;
       int i = 0;
       if(this.cars.size()>0){
           if (j != -2 ){
               c = this.cars.get(j);
               s= c.getX()-x0;
           }                      
           else{
               i=1;
               c = this.cars.get(0).ldr;
               if (c!=null)
                   s = c.getX()+this.length-x0;
           }        
           while (s<sd){           
               v = Math.min(v,c.getV());
               if (c.ldr !=null){
                   if(c.ldr.getLane().section.ConnId!=c.getLane().section.ConnId)
                       if(i==1)
                           i = 2; 
                       else
                           i=1;
                   double leng= c.getLane().length;
                   c = c.ldr;                    
                   if (i==1)
                       s = c.getX()+this.length-x0;
                   else if(i==0)
                       s = c.getX()-x0;
                   else
                       s = c.getX()+this.length+leng-x0;                   
               }
               else
                   break;
           }           
       }
       return v;
   }
       

   // Remove an exiting car - the car that goes reaches destination
   private void removeExitingCars(){
       try{
           while ((cars.size() > 0) && (cars.getFirst().getX() > length)){                   
               TrafficSimulationDesktopView.TravelTime+=(TrafficSimulationDesktopView.t-cars.getFirst().entryTime);
               cars.removeFirst();
                   TrafficSimulationDesktopView.vehsExited++;
           }
       }
      catch (Exception e){//Catch exception if any
      System.err.println("Error in removeexitingcars: " + e.getMessage());
      }
   }

   private void transferToNextSection()
   {       
       try{
       while ((cars.size() > 0) && (cars.getFirst().getX() > length)) 
       {           
           Vehicle currV = cars.getFirst();
           
           if(currV.getCurrSegmentIndx() < currV.getPath().size()-1)
           {             
               Lane newLane = null;
               Connection thisCon = this.section;
               Connection nextCon = currV.getPath().get(currV.getCurrSegmentIndx()+1);
               
               
           
               

               if(thisCon.Target==Intersection.EXIT3 && nextCon.isOffRamp!=1 && this.idx!=1){
                   newLane = nextCon.lanes[this.idx-2];
               }
               else if (thisCon.Target == Intersection.EXIT3 && nextCon.isOffRamp != 1 && this.idx == 1) {   // 161201 by Hyun
                   newLane = nextCon.lanes[this.idx-1];
               }
               else if (thisCon.Target == Intersection.SIGNALIZED && nextCon.isOffRamp != 1 && this.idx == 1) {   // 180228 by Hyun
                   newLane = nextCon.lanes[this.idx-1];
               }
               else if (thisCon.Target == Intersection.SIGNALIZED && nextCon.isOffRamp != 1 && this.idx == 0) {   // 180228 by Hyun
                   newLane = nextCon.lanes[this.idx];
               }

               else if (((thisCon.Target==Intersection.EXIT2 && nextCon.isOffRamp!=1)||
                        (thisCon.Target==Intersection.EXIT && nextCon.isOffRamp!=1)||
                        (thisCon.Source==Intersection.RAMP && nextCon.Target!=Intersection.EXIT)
                       )&& this.idx>0){
                   newLane = nextCon.lanes[this.idx-1];    
               }
               else if((thisCon.isOnRamp!=1 && (nextCon.Source==Intersection.RAMP||nextCon.Source==Intersection.RAMP2))||
                       (thisCon.Source!=Intersection.RAMP && nextCon.Target==Intersection.EXIT)                       
                       ){
                   newLane = nextCon.lanes[this.idx+1];
               }
               else{
                   newLane = nextCon.lanes[this.idx];
               }
               
               if((newLane.idx !=this.idx)&& !(newLane.section.lane0Type==Lane.AuxLane_EXIT ||newLane.section.lane0Type==Lane.AuxLane_RAMP)){
                   int i2= (newLane.section.lane0Type==Lane.AuxLane_EXIT ||newLane.section.lane0Type==Lane.AuxLane_RAMP) ? newLane.idx-1: newLane.idx;
                       currV.distanceFromExitThreshold=0.75*(1+(((double)i2)/newLane.section.nLanes));
               }
               
                                                             
               currV.setCurrSegmentIndx(currV.getCurrSegmentIndx() + 1);
               currV.setCurrSegment(nextCon);
               currV.setX(currV.getX() - length);                               //set the x0 of the car for the new segment
               
               currV.setLane(newLane);
               newLane.cars.add(currV);
               currV.setTargetLane(newLane);
               currV.setTargetFollower(newLane.cars.size()-1);
               
               if(nextCon.isOnRamp==1 && newLane.nextToStop==null)   //&& nextToStop != null  removed
                  newLane.nextToStop = newLane.cars.getLast();
               
           this.cars.remove(currV);
           }           
       }
       }catch (Exception e){
           System.out.println("error in transferToNextSection"  +" Veh ID : "+cars.getFirst().getVehId());
       }
   }
   
   public int hasMB(){
       int i = -1;
       for (Vehicle c : cars){
           if (c.getvMB() != 999)
               i=cars.indexOf(c);
       }
       return i;
   }

   private double updateV (Vehicle c){
       if (TrafficSimulationDesktopView.jrRelax)
           return  updateVrelax(c);
       else
           return  updateVcfl(c);
   }

 

   private double updateVrelax (Vehicle c){

       double v = u;
              
       if ((c.dn == 1) || (c.ldr == null)){
           v = updateVcfl(c);
       } else {
           double Vlid = c.ldr.getV();
           double V0lid = c.ldr.v0;
           double Xlid = c.ldr.getX();
           double Kvb = kj * w / (w + Vlid);
           double Kvb0 = kj * w / (w + V0lid);
           double Dv = c.dn * (Vlid - V0lid);
           
           if(c.getLane().section.ConnId!=c.ldr.getLane().section.ConnId)
               Xlid = c.getCurrSegment().length+c.ldr.getX();
           
           c.dn = Math.min(1, c.dn * Kvb / Kvb0 + (Dv + epsilon) * Kvb * dt);

           double Xcg = Xlid + Vlid * dt - c.dn / Kvb;
           double Xff = c.getX() + myMath.min(c.getV() + c.getAcc()*dt, u) * dt;
           double x = Math.min(Xff, Xcg);

           v = (x - c.getX()) / dt;
           if (v < 0)
               v = 0;

           if (x <= Xlid - 1/kj)
               c.dn = 1;
       }

      return v;
   }

   @Override
   public String toString(){
       return("Con: "+this.section+", idx: "+this.idx + ", LDlist " + this.LDlist);//+", Type: "+this.type);
   }
}

/*
 * Class that represents the prototype of vehicles generated from this lane
 */
class VehiclePrototype{
   Intersection origin;
   Intersection destination;
   java.util.List<Connection> path;
   double q;

   // Constructor
   public VehiclePrototype(Intersection origin, Intersection destination, java.util.List<Connection> path, double q){
       this.origin = origin;
       this.destination = destination;
       this.path = path;
       this.q = q;
   }
   
   @Override
   public String toString(){
       return("Q: "+this.q+"["+this.origin+"-->"+this.destination/*+"]. Path = "+path*/);
   }
}

 //--- OLD


// private double updateVcfl (Vehicle c){
//       double v = u;
//       double s = 999;
//
//       // if car c has a leader
//       if (c.ldr != null){
//          if(c.ldr.getLane().section.ConnId!=c.getLane().section.ConnId)
//               s = c.ldr.getX()+c.getCurrSegment().length- c.getX();
//           else
//               s = c.ldr.getX()- c.getX();
//       }
//       v = c.getV();
//
////       v = myMath.min(v + c.getAcc()*dt, this.u , myMath.Veq(s, c.getLane().w,c.getLane().kj, c.getLane().u), c.getvMB());
//
//       v = myMath.min(v + c.getAcc()*dt, this.u,  myMath.Veq(s, c.getLane().w,c.getLane().kj, c.getLane().u), c.getvMB(),c.vsltag);
//
////       *** Ramp Meter Light ***
//
//
//       if(this.section.isOnRamp==1&&(nextToStop != null)){
//
//           if (c == nextToStop)
//           {
//               if (c.getX() >= this.section.length-0.6)   // The ramp meter is located at "this.section.length/2"
//               {
//
//                   if(!this.section.RM.QFlushFlag ){        // if flush flag is false
//                       if (TrafficSimulationDesktopView.t >= nextDep)
//                       {
//                           this.nextDep = TrafficSimulationDesktopView.t + 1/(this.section.RM.meterRate*1.2);
//                           int j = cars.indexOf(c);
//                           nextToStop = (j != cars.size()-1)? cars.get(j+1) : null;
//                       }
//                       else
//                           v = 0;
//                   }
//
//                   else{
//
////                       int j = cars.indexOf(c);
////                       nextToStop = (j != cars.size()-1)? cars.get(j+1) : null;
//
//                       //--------
////
//                       if((Math.round((TrafficSimulationDesktopView.t)*3000)) < 3000){
//                               if (TrafficSimulationDesktopView.t >= nextDep)
//                               {
//
////                                    double rand1 = TrafficSimulationDesktopView.vehGenR.nextDouble();
////
////                                   if(myMath.min(900,this.section.stations[0].calculateCF()) *this.section.dt > rand1 ){
////
////                                       this.nextDep = TrafficSimulationDesktopView.t + this.section.dt;
////                                       int j = cars.indexOf(c);
////                                        nextToStop = (j != cars.size()-1)? cars.get(j+1) : null;
////
////                                   }else
////                                     this.nextDep = TrafficSimulationDesktopView.t + 1/(myMath.max(900,this.section.stations[0].calculateCF())*1.2);
//                                     this.nextDep = TrafficSimulationDesktopView.t + 1/(900*1.2);
//
////                                   this.nextDep = TrafficSimulationDesktopView.t + 2*this.section.dt;
//                                   int j = cars.indexOf(c);
//                                   nextToStop = (j != cars.size()-1)? cars.get(j+1) : null;
//                               }
//                               else{
//                                   v = 0;
//                               }
//                        }
//                       else if(this.section.RM.ConnDS.stations[1].calculateCD() < 20){
//                            int j = cars.indexOf(c);
//                            nextToStop = (j != cars.size()-1)? cars.get(j+1) : null;
//
//                       }
//                       else{
//                            int j = cars.indexOf(c);
//                            nextToStop = (j != cars.size()-1)? cars.get(j+1) : null;
//                       }
//
//
//                       //--------
//
//                   }
//
//               }
//           }
//       }
//
//
//       //----------RM
//       return v;
//   }
