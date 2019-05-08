  /*
   * Set the intersection spe
   */
package trafficsimulationdesktop;

import java.awt.*;
import java.util.*;

/*
 * Vehicle Class
 */

public class Vehicle {

   private double x;                                                               // Vehicle distance from the start of the section
   private double v;                                                               // Vehicle velocity
   public  double a0;                                                              // Accelaration?? Not sure  ***
   public  double d0;                                                              // Deceleration
   private int vMB = 999;                                                          // Regular vehs             ***
   private Lane targetLane;                                                        // Target lane
   private Lane lane;                                                              // Current lane of the vehicle
   private int targetLeader = 0;                                                    // Leader of the Target
   private int targetFollower = -1;                                                 // Follower of the Target
   private double targetSpacing;                                                   // Spacing of the Target
   public Color clr;                                                               // Color
   public double changingUntil;                                                    // Time(absolute) till the lane changing goes on
   public int omd;                                                                 // idx ori + des : to paint this car when changing track ***
   public double dn = 1;                                                           // used for relaxation in the car following model***
   public Vehicle ldr=null;
   public double v0=0;                                                               // v step anterior ***
   public int CountedAt = -1;                                                      // used to ensure lane changes are not double counted at the lane detectors
   public double vehDistanceFromExit =0;                                           //distance measured only till the exit ramp on the fwy. does not include offramp length
   public double distanceFromExitThreshold;                                //this is used as a criteria to set when the vehicles need to start lane changes to exit smoothly
   public static double distFromExitThreshold_LLanes= 0.3;                               //this is used as a criteria to move vehicles that are far from exit from moving to leftlanes when the speeds on right lanes are lower
   public static double nearestExitThreshold = 0.3;                               //this is used as a criteria to move vehicles that are far from exit from moving to leftlanes when the speeds on right lanes are lower


   public double nearestExit =0;                                           //distance measured only till the next exit ramp on the fwy. used for if lane change is required.

   // Added by Bharat
   private Intersection origin;
   private Intersection destination;
   private java.util.List<Connection> path;                                        // The shortest path the car would follow
   private Connection currSegment;                                                 // The current segment where the car is moving
   private int currSegmentIndx;                                                    // The index in the path of the current segment
   private long vehId;                                                             // Vehicle Id for tracking purposes
   
   private boolean exitingVehicle;                                           // Vehicle is exiting in the next Exit
   private boolean enteringVehicle;                                                // Vehicle needs to move to main lane from Ramp
//   public int exitLCflag;                                                // flag used to see if on-ramp lane addition happened before the nearest exit. so vehicle does not need to move to leftlanes to avoid being on the exitLane
   public int missedLCchances;                                                //counter for missed LC opportunities due to small target spacing. will be used for mandatory lanechanges (exit) only
   public double fi;                                                           //probability of changing lane based on distance from exit 
   public double entryTime;                                                   //Time of the vehicle's entry into the network
   public boolean isHOTveh;
   public int CountedVSL = -1;
   public double vsltag;

   public double tempvsltag;


  /*
   * Constructor
   */
   public Vehicle(double x0, Lane ln) {
       x = x0;
       lane = ln;
       targetLane = ln;
       a0 = (2+1*(TrafficSimulationDesktopView.vehAccR.nextDouble()))*3600*3.6; //100 prev was 3600    // 2 -> 4 Hyun 20160329
       d0 = -((TrafficSimulationDesktopView.vehAccR.nextDouble()))*3600*3.6;
       clr = new Color((int)(TrafficSimulationDesktopView.vehAccR.nextDouble()*255),(int)(TrafficSimulationDesktopView.vehAccR.nextDouble()*255),(int)(TrafficSimulationDesktopView.vehAccR.nextDouble()*255));
       v = lane.u;
       vsltag = lane.u;

       tempvsltag = lane.u;
              
       exitingVehicle = false;
       enteringVehicle = false;
       isHOTveh=false;
       missedLCchances = 0;
       fi=0;
   }

  /*
   * Constructor
   */
   public Vehicle(double x0, Lane ln, Intersection origin, Intersection destination, java.util.List<Connection> shortestPath) {

       this(x0, ln);

       this.origin = origin;
       this.destination = destination;
       this.path = shortestPath;
       this.entryTime = TrafficSimulationDesktopView.t;

       this.currSegment = path.get(0);
       this.currSegmentIndx = 0;
       this.distanceFromExitThreshold=0.75*(1+((double)ln.idx)/ln.section.nLanes);
       if(!(this.currSegment.lane0Type==Lane.AuxLane_RAMP && this.lane==this.currSegment.lanes[0] && this.lane.cars.size()==0))
//       clr = new Color((int)((this.getPath().get(0).ConnId)*255/30),255-(int)((this.getPath().get(this.getPath().size()-1).ConnId)*255/33),(int)((this.getPath().get(this.getPath().size()-1).ConnId)*255/33));//(int)((this.getPath().get(0).ConnId+(this.getPath().get(this.getPath().size()-1).ConnId))*255/63));   /// there are 33 segments. So we use unique color coding for each OD pair
        CalcExitDistNearestExit();
  }

   /*
   * Returns the leader of this car
   */
   public Vehicle getlid(){    // see the getLid_transferNextSection.xlsx in the jorge Laval folder for the logic
   Vehicle lid= null;
       int i = lane.cars.indexOf(this);       
       if (i>0)
           lid = lane.cars.get(i-1);
       
       else{
           try{
               if(this.getLane().exitLane && this.getPath().get(this.getPath().size()-1).isOffRamp!=1){
                   lid = null;
               }
               else if((this.getCurrSegmentIndx() < this.getPath().size()-1)&&                // the second condition is incorporated such that if the vehicle is in the exit lane and is the first vehicle, then the next segment may not have the idx same as the current ids and hence will give arrayoutofbound error
                   !((this.getCurrSegmentIndx() == this.getPath().size()-2)&& this.exitingVehicle &&
                       ((this.currSegment.Target==Intersection.EXIT && this.getLane().idx>0)
                       ||(this.currSegment.Target==Intersection.EXIT2 && this.getLane().idx>0)
                       ||(this.currSegment.Target==Intersection.EXIT3 && this.getLane().idx>1))&& 
                       this.getPath().get(this.getPath().size()-1).isOffRamp==1) 
                       ){
                   Connection thisCon = this.currSegment;
                   Connection nextCon = this.getPath().get(this.getCurrSegmentIndx()+1);

//                   if(thisCon.Target==Intersection.EXIT3 && nextCon.isOffRamp!=1){   //Oriiginal
//                       if(nextCon.lanes[this.getLane().idx-2].cars.size()>0)
//                           lid = nextCon.lanes[this.getLane().idx-2].cars.getLast();
//                   }
                  if(thisCon.Target==Intersection.EXIT3 && nextCon.isOffRamp!=1 && this.getLane().idx!=1){   // vehicle's lanes 0
                       if(nextCon.lanes[this.getLane().idx-2].cars.size()>0)
                           lid = nextCon.lanes[this.getLane().idx-2].cars.getLast();
                   }
                  else if(thisCon.Target==Intersection.EXIT3 && nextCon.isOffRamp!=1 && this.getLane().idx==1){   // check vehicle's lanes 1
                       if(nextCon.lanes[this.getLane().idx-1].cars.size()>0)
                           lid = nextCon.lanes[this.getLane().idx-1].cars.getLast();
                   }

                   else if (((thisCon.Target==Intersection.EXIT2 && nextCon.isOffRamp!=1)||
                            (thisCon.Target==Intersection.EXIT && nextCon.isOffRamp!=1)||
                            (thisCon.Source==Intersection.RAMP && nextCon.Target!=Intersection.EXIT)
                           )&& this.getLane().idx>0){
                       if(nextCon.lanes[this.getLane().idx-1].cars.size()>0){
                           lid = nextCon.lanes[this.getLane().idx-1].cars.getLast(); 
                       }
                   }
                   else if((thisCon.isOnRamp!=1 && (nextCon.Source==Intersection.RAMP||nextCon.Source==Intersection.RAMP2))||
                           (thisCon.Source!=Intersection.RAMP && nextCon.Target==Intersection.EXIT)                       
                           ){
                       if (nextCon.lanes[this.getLane().idx +1].cars.size() >0)
                              lid = nextCon.lanes[this.getLane().idx+1].cars.getLast();
                   }
                   else{
                       if(thisCon.Target==Intersection.EXIT && nextCon.isOffRamp==1 && this.getLane().idx>0){
                           if(nextCon.lanes[0].cars.size()>0)
                               lid=nextCon.lanes[0].cars.getLast();
                       }
                       else if(nextCon.lanes[this.getLane().idx].cars.size() > 0)
                           lid= nextCon.lanes[this.getLane().idx].cars.getLast();
                   }
               }
           } catch(Exception a){
           System.err.println("ERROR in getLid: "+a.getMessage());
           }
       }       
       return lid;
   }
   
   //Added by Shalini
   public Vehicle getFollower()
   {
       Vehicle fid = null;
       int i = lane.cars.indexOf(this);
       
       if(i+1 < lane.cars.size())
           fid = lane.cars.get(i+1);
       
       return fid;
   }
      
   /*
    * Returns the target spacing in a new lane given the leader
    */
   public double getTargetSpacing(Lane ln, int j) {
       calcTargetSpacing (ln, j);
       return this.targetSpacing;
   }

    public void CalcExitDistNearestExit() {
        int counter = this.getCurrSegmentIndx() + 1;
        int flag = 0;
        while (counter < this.getPath().size() - 1) {
            if (flag == 0) {
                if (!(this.getPath().get(counter).Target == Intersection.EXIT2 || this.getPath().get(counter).Target == Intersection.EXIT3)) {
                    nearestExit = nearestExit + this.getPath().get(counter).length;
                } else {
                   nearestExit = nearestExit+this.getPath().get(counter).length;
                    flag = 1;
                }
            }
            vehDistanceFromExit = vehDistanceFromExit + this.getPath().get(counter).length;
            counter = counter + 1;
        }
        vehDistanceFromExit = vehDistanceFromExit + this.getLane().length - this.getX();
        if(!(this.getCurrSegment().Target==Intersection.EXIT2||this.getCurrSegment().Target==Intersection.EXIT3))
            nearestExit = nearestExit + this.getLane().length - this.getX();
        else{
            nearestExit = this.getLane().length - this.getX();
        }
    }

   /*
    * Returns the target spacing in a new lane given the leader
    */
   private void calcTargetSpacing(Lane ln, int j) {
       targetSpacing = 99999;
       if (j!=-2){           
           if (j!=ln.cars.size()-1) 
               targetSpacing = ln.cars.get(j).getX() - ln.cars.get(j+1).getX();
           else 
               targetSpacing = ln.cars.get(j).getX();           
       }
       else if (j==-2&&ln.cars.size()>0)
           if (ln.cars.get(0).getlid()!=null)                             
               targetSpacing = ln.cars.get(0).getlid().getX()+ln.length - ln.cars.get(0).getX();                  
   }

   /*
    * Returns the leader in the target lane
    */
   public int getTargetLeader(Lane ln) {
       if(ln.cars.size()>0)       
           calcTargetLeader(ln);       
       else
           this.targetLeader = -2;
       return this.targetLeader;
   }

   /*
    * Returns the target spacing in a new lane given the leader
    */
   private void calcTargetLeader (Lane ln){
       targetLeader = ln.cars.size()-1;
       Vehicle c;
       Iterator itr = ln.cars.iterator();       
       for (int i = 0; i< ln.cars.size(); i++){
           c = (Vehicle)itr.next();
           if ( x > c.getX()){
               targetLeader = i-1;
               break;
           }
       }
       if (targetLeader == -1)
           targetLeader=-2;
   }

  /*
   * Get the Acceleration
   */
   public double getAcc(){
       return  a0 * (1 - getV() / this.lane.u) - 9.81*3600*3.6 * this.lane.section.G; ////100 prev was 3600
   }

   //---- Getters and Setters ----
   public double getX(){
       return x;
   }

   public void setX(double x0){
       x = x0;
   }

   public double getV(){
       return v;
   }

   public void setV(double vv){
       v0 = v;
       v = vv;
   }

   public void setvMB(int v0){
       vMB = v0;
   }

   public int getvMB(){
       return vMB;
   }

   public Lane getTargetLane() {
       return this.targetLane;
   }

   public void setTargetLane(Lane targetLane) {
       this.targetLane = targetLane;
   }

   public Lane getLane() {
       return this.lane;
   }

   public void setLane(Lane newLane) {
       try{
           changingUntil = TrafficSimulationDesktopView.t + lane.tau;
       omd = lane.idx + newLane.idx;
       this.lane = newLane;
       } catch (Exception e)
       {
           System.err.println("Error in setLane: " + e.getMessage());}
   }

   public int getTargetFollower () {
       return this.targetFollower;
  }

   public void setTargetFollower (int targetFollower) {
       this.targetFollower = targetFollower;
   }

   public Connection getCurrSegment(){
       return currSegment;
   }

   public void setCurrSegment(Connection newSegment){
       this.currSegment = newSegment;
   }

   public int getCurrSegmentIndx(){
       return this.currSegmentIndx;
   }

   public void setCurrSegmentIndx(int indx){
       this.currSegmentIndx = indx;
   }

   public java.util.List<Connection> getPath(){
       return this.path;
   }

   public void setPath(java.util.List<Connection> path){
       this.path = path;
   }

   public long getVehId(){
       return this.vehId;
   }

   public void setVehId(long vehId){
       this.vehId = vehId;
   }
   
   public void setExitingVehicle(){
       this.exitingVehicle = true;
   }

   public void resetExitingVehicle(){
       this.exitingVehicle = false;
   }

   public boolean isExiting(){
       return this.exitingVehicle;
   }

   public void setEnteringVehicle(){
       this.enteringVehicle = true;
   }

   public void resetEnteringingVehicle(){
       this.enteringVehicle = false;
   }

   public boolean isEntering(){
       return this.enteringVehicle;
   }

   @Override
   public String toString(){
       //return("Veh - Id:"+this.vehId+"["+this.origin+"-->"+this.destination+"]");
//       return("V Id: "+this.vehId+" [X = "+Math.floor(this.getX()*100)/100+", V = "+Math.floor(this.getV()*100)/100+/*", A = "+Math.floor(this.getAcc()*100)/100+*/"]"+", ConnId: "+this.lane.section.ConnId+", Lane Idx: "+this.getLane().idx+", ldr id: "+((this.ldr != null)? this.ldr.vehId : "null"));
//       return("V Id: "+this.vehId+" [X = "+Math.floor(this.getX()*100)/100+", V = "+Math.floor(this.getV()*100)/100+/*", A = "+Math.floor(this.getAcc()*100)/100+*/"]");//+", ConnId: "+this.lane.section.ConnId+", Lane Idx: "+this.getLane().idx+", Exiting : "+this.exitingVehicle+", Entering : "+this.enteringVehicle+", c.dn: "+Math.floor(this.dn*100)/100+", DistFromExit: "+Math.floor(this.vehDistanceFromExit*100)/100+", NearestExit: "+Math.floor(this.nearestExit*100)/100+", ldr id: "+((this.ldr != null)? this.ldr.vehId : "null"));
//       return(this.vehId+"         "+this.lane.section.ConnId);//+", Exiting : "+this.exitingVehicle+", Entering : "+this.enteringVehicle+", c.dn: "+this.dn);
         return( "V Id: "+this.vehId +" speed: "+this.v  +"vsl speed: "+this.vsltag +" conn:"+this.getCurrSegment()+ "X :"+this.getX()+ "TargetSpacing: "+ this.targetSpacing);
   }
}    
