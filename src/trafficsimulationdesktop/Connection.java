/*
 * The Connection class repesents a link between intersections.
 */

package trafficsimulationdesktop;

//import com.mysql.jdbc.Statement;
import org.jgrapht.graph.DefaultWeightedEdge;
//import java.awt.*;
//import java.sql.DriverManager;
import java.util.LinkedList;
import java.util.*;
import java.io.*;

/**
 * @author Bharat R
 */

public class Connection extends DefaultWeightedEdge implements java.io.Serializable{

   public Lane[] lanes;
   public Lane[] lLanes;
   public Lane[] rLanes;
   public DetectorStation[] stations;
//   public DetectorStation[] IOstation;
   public int nLanes;                                                            //# of thru lanes
   public int leftLanes;
   public int rightLanes;
   public int isOnRamp;
   public int isOffRamp;
   public int isSignalized;
   public int hasAccLane;
   public int hasDecLane;   
   public int RightLaneDrop;   
   public int LeftLaneDrop;   
   public int laneCount;                                                         //laneCount is the manipulated value
   public double length;
   public double leftLaneLength;
   public double rightLaneLength;
   public double AccLaneLength;
   public double DecLaneLength;
   public double u;
   public double kj;
   public double w;
   public double tau;
   public double dt;                                                                 // Calculated
   public double qr;                                                                 // --
   public double G;
   public double LC;                                                                 // --# of LaneChanges
   public double sd;
   public double epsilon;                                                           // used for lane changing
   public LinkedList<double[]> vehInfoForTT;
   private static long SecId = 0;                                                   // Static count of the connections added so far - used to specify unique Id for each connections
   public long ConnId;                                                             // unique Id for each connection
   public int type;
   public static int NORMAL = 0;                                                      // Normal connection
   public static int ORIGIN = 1;                                                      // Connects to a "Origin" intersection
   public static int DESTINATION = 2;                                                 // Connects to a "Destination" intersection
   public static double minSpeedDiff = 15;                                         //used for deciding lane changes
   public static double opening = 0.1;                                            //opening/width of an entrance or exit.Will get 2 time steps options for each vehicle to exit the fwy
   
   public int lane0Type;                                                              // The type of the lane 0 for this segment
   public RampMeter RM;                                                         // ramp meter on this Connection if this Connection is an on-ramp
   public int Source;                                                           //u/s end intersection type
   public int Target;                                                           //d/s end intersection type
   public double t;
   public TrafficLight TL;

   public int vslnumber;
   
   public VariableSpeedLimit[] vsls;
   public DetectorStation[] vds;

//   public int isVDSexist;

   // Constructor
   public Connection(int nLanes, double length){// throws IOException{
       this.nLanes = nLanes;
       this.rightLanes = 0;
       this.leftLanes = 0;
       this.length = length/1000.0;
       this.leftLaneLength = 0.0;
       this.rightLaneLength = 0.0;
       this.u = 100;     // 100 -> 112  160129 Hyun
       this.kj = 150;
       this.w = 20;      // 20 ->22     160129 Hyun
       this.tau = 4/3600.0;  
       this.dt = 1/(kj*w);
       this.qr = 0;
       this.G = 0;//2/100.0;
       this.LC = 0;
       this.sd = 200/1000.0;
       this.epsilon = 2;   // 2->3
       this.isOnRamp = 0;
       this.isOffRamp = 0;
       this.isSignalized = 0;
       stations = null;
       lanes = null;
       lLanes = null;
       rLanes = null;
       this.t=0;

       RM  = new RampMeter();
       TL = new TrafficLight();

       this.vslnumber = 0;

   }

   // Constructor
   public Connection(){// throws IOException{
       this(3,1000.0);
   }

   // Ramp-merging segment - no lane addition => both u/s and d/s freeway has same number of lanes
   // |____________________|2
   // |_______    _________|1
   // |__________/         |0

   public void iniR(double qA, double qr, int type, double gore){
       this.type = type;
       this.lane0Type = Lane.AuxLane_RAMP;
       this.laneCount = this.nLanes + 1;            // Note: Adding an extra merging lane
       this.Source = Intersection.RAMP;
       this.Target = Intersection.GENERIC;
       
       System.out.println("In iniR");
       System.out.println("number of lanes: "+nLanes);
       
       lanes = new Lane[laneCount];
       lanes[0] = new Lane(0,length, u, kj, w, tau, dt, epsilon, sd, qr, this, Lane.AuxLane_RAMP);
       lanes[0].gore = gore;
//       lanes[0].meter = meterRate;
       
       for(int i = 1; i < laneCount; i++){
           lanes[i] = new Lane(i, length, u, kj, w, tau, dt, epsilon, sd, qA, this, Lane.NORMAL);
           lanes[i].gore = gore;
//           lanes[i].meter = meterRate;
       }
       this.ConnId = SecId;
       SecId++;
}
     
   // Ramp-merging segment - lane addition=> d/s freeway has an extra lane compared to u/s freeway
   // |_->_________________|2
   // |_->_________________|1
   // |__________/ _ _ _ _ |0
   // |_->____->__/

   
   public void iniR2(double qA, double qr, int type, double gore){
       this.type = type;
       this.lane0Type = Lane.NORMAL;
       this.laneCount = this.nLanes ;            
       this.Source = Intersection.RAMP2;
       this.Target = Intersection.GENERIC;
       
       System.out.println("In iniR2");
       System.out.println("number of lanes: "+nLanes);
       
       lanes = new Lane[laneCount];
       
       for(int i = 0; i < laneCount; i++)
           lanes[i] = new Lane(i, length, u, kj, w, tau, dt, epsilon, sd, qA, this, Lane.NORMAL);
       
       lanes[0].gore = gore;
//       lanes[0].meter = meterRate;
       
       this.ConnId = SecId;
       SecId++;

//       if(this.ConnId==16){
//           lanes[0].exitLane= true;
//           lanes[1].exitLane= true;
//           lanes[2].exitLane= true;
//       }
}   

   // Exit segment - the exit is shared. so there is no lane drop 
   // |____________________|2
   // |_________     ______|1
   // |         \__________|0

   public void iniE(double qA, double qr, int type, double gore){
       this.type = type;
       this.lane0Type = Lane.AuxLane_EXIT;
       this.laneCount = this.nLanes + 1;
       this.Source = Intersection.GENERIC;
       this.Target = Intersection.EXIT;
       
       System.out.println("In iniE");
       System.out.println("number of lanes: "+nLanes);
       
       lanes = new Lane[laneCount];
       lanes[0] = new Lane(0, length, u, kj, w, tau, dt, epsilon, sd, qr, this, Lane.AuxLane_EXIT);
       lanes[0].gore = gore;
//       lanes[0].meter = meterRate;
       
       for(int i = 1; i < laneCount; i++){
           lanes[i] = new Lane(i, length, u, kj, w, tau, dt, epsilon, sd, qA, this, Lane.NORMAL);
           lanes[i].gore = gore;
//           lanes[i].meter = meterRate;
       }
       this.ConnId = SecId;
       SecId++;

   }
  
   // Exit segment - the right lane exits => lane drop at the exit
   // |____________________|2
   // |_->____->___ _ _____|1
   // |_______ExitLane_->__|0

   public void iniE2(double qA, double qr, int type, double gore){
       this.type = type;
       this.lane0Type = Lane.NORMAL;
       this.laneCount = this.nLanes;  
       this.Source = Intersection.GENERIC;
       this.Target = Intersection.EXIT2;
       
       System.out.println("In iniE2");
       System.out.println("number of lanes: "+nLanes);
       
       lanes = new Lane[laneCount];       
       for(int i = 0; i < laneCount; i++)
           lanes[i] = new Lane(i, length, u, kj, w, tau, dt, epsilon, sd, qA, this, Lane.NORMAL);
       
       lanes[0].gore = gore;
       lanes[0].exitLane= true;
       this.ConnId = SecId;
       SecId++;

 

   }

   // Exit segment - the right two lanes exit => two lanes drop at the exit
   // |_->_________________|3
   // |_->____->___ _ _____|2
   // |_______ExitLane_->__|1
   // |_______ExitLane_->__|0

   public void iniE3(double qA, double qr, int type, double gore){
       this.type = type;
       this.lane0Type = Lane.NORMAL;
       this.laneCount = this.nLanes;
       this.Source = Intersection.GENERIC;
       this.Target = Intersection.EXIT3;
       
       System.out.println("In iniE3");
       System.out.println("number of lanes: "+nLanes);
       
       lanes = new Lane[laneCount];       
       for(int i = 0; i < laneCount; i++)
           lanes[i] = new Lane(i, length, u, kj, w, tau, dt, epsilon, sd, qA, this, Lane.NORMAL);
       
       lanes[0].gore = gore;
       lanes[0].exitLane= true;       
       lanes[1].gore = gore;
       lanes[1].exitLane= true;
       this.ConnId = SecId;
       SecId++;
   }

   // Normal segment
   // |____________________|2
   // |____________________|1
   // |____________________|0

   public void iniN(double qA, double qr, int type, double gore){
       this.type = type;
       this.lane0Type = Lane.NORMAL;
       this.laneCount = this.nLanes;
       this.Source = Intersection.GENERIC;
       this.Target = Intersection.GENERIC;
       this.vehInfoForTT = new LinkedList<double[]>();
       System.out.println("In iniN");
       System.out.println("number of lanes: "+nLanes);
       lanes = new Lane[nLanes];
       
       for(int i = 0; i < nLanes; i++){
           lanes[i] = new Lane(i, length, u, kj, w, tau, dt, epsilon, sd, qA, this, Lane.NORMAL);
           lanes[i].gore = gore;
       }
       this.ConnId = SecId;
       SecId++;


       this.RM = new RampMeter();
//      if(this.isOnRamp==1){
//          this.RM = new RampMeter();
//          lanes[0].meter = meterRate;
//      }
   }
   
   public void iniS(double qA, double qr, int type, double gore){
       this.type = type;
       this.lane0Type = Lane.NORMAL;
       this.laneCount = this.nLanes + this.leftLanes + this.rightLanes;
       this.Target = Intersection.SIGNALIZED;
       this.isSignalized =1;

       System.out.println("In iniS");
       System.out.println("number of lanes: "+nLanes);
       lanes = new Lane[laneCount];
       
       for(int i = 0; i < leftLanes; i++){
           lanes[i] = new Lane(i, leftLaneLength, u, kj, w, tau, dt, epsilon, sd, qA, this, Lane.NORMAL);
           lanes[i].gore = gore;
//           lanes[i].meter = meterRate;
       }
       for(int i = leftLanes; i < (leftLanes+nLanes); i++){
           lanes[i] = new Lane(i, length, u, kj, w, tau, dt, epsilon, sd, qA, this, Lane.NORMAL);
           lanes[i].gore = gore;
//           lanes[i].meter = meterRate;
       }
       for(int i = (leftLanes+nLanes); i < (leftLanes+nLanes+rightLanes); i++){
           lanes[i] = new Lane(i, rightLaneLength, u, kj, w, tau, dt, epsilon, sd, qA, this, Lane.NORMAL);
           lanes[i].gore = gore;
//           lanes[i].meter = meterRate;
       }
       this.ConnId = SecId;
       this.TL = new TrafficLight();
       SecId++;
   }

   public void updateLC() throws Exception{
       if (!TrafficSimulationDesktopView.noLaneChange){
           laneChanges();
           makeChanges();
       }   
   }
   
//The leader of each car is determined        
   public void updateLid(){
       for (int ii = 0; ii < laneCount; ii++){
           int i0 = (this.lane0Type==Lane.AuxLane_RAMP && ii==0)? 1 : 0;            // vehicle 0 on every onramp is no updated. vehicle 0 was inserted such that vehicles behind it does not continue on lane0       
           for (int jj = i0; jj < this.lanes[ii].cars.size(); jj++){
               Vehicle c=this.lanes[ii].cars.get(jj);
               if(jj==0)
                       c.ldr=c.getlid();
               else
                       c.ldr=this.lanes[ii].cars.get(jj-1);               
           }
       }
   }
   
   public void updateV(){
       for (int i = 0 ; i < laneCount; i++)
           lanes[i].updateAllV();
   }

   //UpdateX function called continuously
   public void updateX() throws IOException{
       for (int i = 0 ; i < laneCount; i++)
          lanes[i].updateAllX();
   }
   
   //UpdateTransfer function called continuously
   public void updateTransfer(){
       for (int i = 0 ; i < laneCount; i++)
           lanes[i].updateAllTransfer();       
   }
   
//   public void updateStop(){
//       for (int i = 0 ; i < laneCount; i++)
//           lanes[i].updateAllStop();
//   }
//   
   
   

   /*
   * Try changing from i to j
   */
   public void laneChanges(){
       for (int i = 0 ; i < laneCount; i++) 
           for (int j = i - 1; j<=i+1 && j < laneCount; j+=2) 
               if((j>=0))  
                   checkLC (i,j);       
   }

   /*
   * Try changing from i to j - for all cars in i
   */
   
   private void checkLC(int i, int j){       
       Lane ori = lanes[i];
       Lane des = lanes[j];
       double s = 999;
       int i0 = (this.lane0Type==Lane.AuxLane_RAMP && i==0)? 1 : 0;            // vehicle 0 on every onramp is no updated. vehicle 0 was inserted such that vehicles behind it does not continue on lane0       
       for (int k = i0; k < ori.cars.size(); k++){
           double temp1 = 0;
           double temp2 = 0;
           Vehicle c = ori.cars.get(k);
           int jj = c.getTargetLeader(des); 
           double Vori = c.getLane().u; 
           double Vdes = des.getLaneV(c.getX(), jj);
           double Vds = ori.getLaneV(c.getX(), ori.cars.indexOf(c));  //velocity d/s in its lane
           int i1=(this.lane0Type==Lane.AuxLane_RAMP||this.lane0Type==Lane.AuxLane_EXIT)? 1:0;  //used to move to leftlanes to avoid nearestexit

           if(c.ldr!=null){
                   if(c.ldr.getLane().section.ConnId!= this.ConnId)
                        s = c.ldr.getX()+this.length- c.getX();
                    else
                        s = c.ldr.getX()- c.getX();
                    Vori = myMath.Veq(s, c.getLane().w,c.getLane().kj, c.getLane().u);
           }
           if(c.getLane().exitLane && !c.isExiting()){                                            // first priority is that vehicles in the exit lane are not supposed to be there if it is not exit vehicles.
//                   if((c.getCurrSegmentIndx()<c.getPath().size()-2)&& j>i && jj!=des.cars.size()-1){
                   if((c.vehDistanceFromExit > c.nearestExitThreshold )&&(c.getCurrSegmentIndx()<c.getPath().size()-2) && j>i && jj!=des.cars.size()-1){
                       if(c.nearestExit< 0.04 )  // 0.04 -> 0.1
                           c.setTargetLane(des);
                       else if(Vds<u-minSpeedDiff && Vdes>Vds+minSpeedDiff){  // Added by Hyun 170201
                           c.setTargetLane(des);
                       }
                       else{
                           temp1 = c.vehDistanceFromExit;
                           temp2 = c.distanceFromExitThreshold;
                           c.vehDistanceFromExit = c.nearestExit;                 //veh distance from exit temporarilly changed to treat the vehicle in the exitlane as a mandatory lanechange
                           c.distanceFromExitThreshold = Vehicle.nearestExitThreshold;
//                           c.setTargetLane( LCmodel(ori, des, Vori, Vdes, c, jj));
                           c.setTargetLane(Exit_relax(ori, des, Vds, Vdes, c, jj));
                           c.vehDistanceFromExit = temp1;
                           c.distanceFromExitThreshold = temp2;
                           temp1=0;
                           temp2=0;
                       }
                   }
           }

           else if (c.nearestExit<Vehicle.nearestExitThreshold && i==i1 && j>i && c.vehDistanceFromExit > c.distanceFromExitThreshold && c.nearestExit<c.vehDistanceFromExit && c.nearestExit>0 /*&& c.exitLCflag==0*/){

                       if(Vds<u-minSpeedDiff && Vdes>Vds+minSpeedDiff){  // Added by Hyun 170201
                           c.setTargetLane(des);
                       }
                       else{
                           temp1 = c.vehDistanceFromExit;
                           temp2 = c.distanceFromExitThreshold;
                           c.vehDistanceFromExit = c.nearestExit;                 //veh distance from exit temporarilly changed to treat the vehicle in the exitlane as a mandatory lanechange
                           c.distanceFromExitThreshold = Vehicle.nearestExitThreshold;
                           c.setTargetLane(Exit_relax(ori, des, Vds, Vdes, c, jj));
                           c.vehDistanceFromExit = temp1;
                           c.distanceFromExitThreshold = temp2;
                           temp1=0;
                           temp2=0;
                      }
           }
           else if(!c.isEntering()&& !c.isExiting()&& c.vehDistanceFromExit>Vehicle.distFromExitThreshold_LLanes
                       && Vds<u-minSpeedDiff && Vdes>Vds+minSpeedDiff  && j>i ) {    //if the exit is far, vehicles tend to move to the left lanes if the speed is lower on the right lanes     // && (this.ConnId!= 1 || (this.ConnId == 1 && i !=1 ) is added 030816    //    && ((this.ConnId!= 1 || (this.ConnId == 1 && i != 0 )) && (this.ConnId!=2 || (this.ConnId==2 && c.getX() < this.opening + 0.07 && i!= 1 ) ) )
                   if(TrafficSimulationDesktopView.relaxR.nextDouble()<(1-j/this.laneCount) )                          //to ensure that all the vehicles do not end up in the left-most lane.
                       c.setTargetLane(LCmodel(ori, des, Vds, Vdes, c, jj));
            }
//            else if(c.getvMB()== 999 && (c.ldr!=null||c.isExiting()) ){           //the logic is to start with i==0 and all possible combinations of Lane0Type
            else if(c.ldr!=null||c.isExiting() ){
                    if(i==0 && this.lane0Type==Lane.AuxLane_RAMP){                    //i=0 is an on-ramp lane
                       c.setTargetLane( LCmodel(ori, des, 0, 0, c, jj));            //merge model
                    }
                    else if (i==0 && this.lane0Type==Lane.AuxLane_EXIT){}            //i=0 is an off-ramp lane => do nothing
                    else if( i==0 && this.lane0Type==Lane.NORMAL && c.getCurrSegmentIndx()< c.getPath().size()-2){                    //i=0 is a normal lane
                       if (Vori<u-minSpeedDiff && Vdes>Vori + minSpeedDiff && !(jj==des.cars.size()-1 && c.getX()<(des.u/des.cap))&& c.dn==1  )      // jj!=0 is added such that if a vehicle is inserted at as the first vehicle in the d/s segment, then the dn of the first vehicle in the upstream link not be changed so will give rise to s<0 errors          ////  && ((this.ConnId!= 1 || (this.ConnId == 1 && i != 0 )) && (this.ConnId!=2 || (this.ConnId==2 && c.getX() < this.opening + 0.07 && i!= 1 ) ) )      170130
                           c.setTargetLane( LCmodel(ori, des, Vori, Vdes, c, jj));  //discretionary lane change                                 c.vehDistanceFromExit > 1 is Inserted not to make congestion at the end of the exit.
                    }
                    else if( i==0 && this.lane0Type==Lane.NORMAL && c.getCurrSegmentIndx()== c.getPath().size()-2){                    //i=0 is a normal lane
                    }  //Do nothing
                    else if( i==0 && c.getLane().exitLane && c.getCurrSegmentIndx()== c.getPath().size()-4){                    //i=0 is a normal lane , need to be corrected

                    }  //Do nothing
                    else if( (i==1||i==2) && c.getLane().exitLane && c.getCurrSegmentIndx() < c.getPath().size()-2){                    //i=0 is a normal lane
                    }


                   else{                                                           //if the i!=0 then
                       if(c.isExiting()  ){
                           if(j<i){ //  && c.getLane().exitLane==false is added to keep vehicle at the same lane when it is on exit lane
                            if(  ((c.getCurrSegmentIndx()==c.getPath().size()-2  ) ) && i>0   ){  // 0.04 -> 0.1
                                    c.setTargetLane(Exit_relax(ori, des, Vds, Vdes, c, jj));
                             }
                            if(  ((c.getCurrSegmentIndx()==c.getPath().size()-2  ) ) && i>0   ){  // 0.04 -> 0.1
                                    c.setTargetLane(Exit_relax(ori, des, Vds, Vdes, c, jj));
                             }
                            if(  ((c.getCurrSegmentIndx()==c.getPath().size()-2  ))  && i>0   ){  // 0.04 -> 0.1
                                if(c.nearestExit<0.1)
                                    c.setTargetLane(des);
                                else
                                    c.setTargetLane(Exit_relax(ori, des, Vds, Vdes, c, jj));
                             }

                             else if((((des.type == Lane.AuxLane_EXIT)&& c.getCurrSegmentIndx()==c.getPath().size()-2)||(!(jj==des.cars.size()-1 && c.getX()<(des.u/des.cap))&& des.type==Lane.NORMAL))  ){   // && !ori.exitLane
                                       c.setTargetLane(Exit_relax(ori, des, Vds, Vdes, c, jj));    //exit model
                               }
                           }
                       }

                       else{
//                           if( !(this.ConnId==1 && i==0 ) &&  Vori<u-minSpeedDiff && Vdes>Vori+minSpeedDiff &&(des.type==Lane.NORMAL)&& !(jj==des.cars.size()-1 && c.getX()<(des.u/des.cap))&& c.dn==1   )     //if the exit is far, vehicles tend to move to the left lanes if the speed is lower on the right lanes   //   && ((this.ConnId!= 1 || (this.ConnId == 1 && i != 0 )) && (this.ConnId!=2 || (this.ConnId==2 && c.getX() < this.opening + 0.07 && i!= 1 ) ) )
                           if(  Vori<u-minSpeedDiff && Vdes>Vori+minSpeedDiff &&(des.type==Lane.NORMAL)&& !(jj==des.cars.size()-1 && c.getX()<(des.u/des.cap))&& c.dn==1   )     //if the exit is far, vehicles tend to move to the left lanes if the speed is lower on the right lanes   //   && ((this.ConnId!= 1 || (this.ConnId == 1 && i != 0 )) && (this.ConnId!=2 || (this.ConnId==2 && c.getX() < this.opening + 0.07 && i!= 1 ) ) )

                               c.setTargetLane( LCmodel(ori, des, Vori, Vdes, c, jj));  //discretionary lane change
                        }
                   }
             }
       }
      
   }

   private Lane LCmodel(Lane ori, Lane des, double Vori, double Vdes, Vehicle c, int jj){
       Lane L = ori;

       if (TrafficSimulationDesktopView.jrRelax){
           L = relax(ori, des, Vori, Vdes, c, jj);
       } else if (TrafficSimulationDesktopView.jrGapacc1){
           L = gapacc1(ori, des, Vori, Vdes, c, jj);
       } else if (TrafficSimulationDesktopView.jrGapacc2){
           L = gapacc2(ori, des, Vori, Vdes, c, jj);
       }
       return L;
   }

  /*
   * Lane changing model
   */
   private Lane gapacc1(Lane ori, Lane des, double Vori, double Vdes, Vehicle c, int jj){
       Lane L = ori;
       
       double p = (Vdes - Vori)/u/tau*dt;
       if ((TrafficSimulationDesktopView.GapAcc1R.nextDouble()<p) && c.getTargetSpacing(des, jj) > 2/kj)
           L = des;
       System.out.println("In gapacc1, L: " + L);
       return L;
   }

  /*
   * Lane changing model
   */
   private Lane gapacc2(Lane ori, Lane des, double Vori, double Vdes, Vehicle c, int jj){
       Lane L = ori;

       double p = (Vdes - Vori)/u/tau*dt;
       double s = 1/kj;
       
       if ((TrafficSimulationDesktopView.GapAcc2R.nextDouble()<p) && c.getTargetSpacing(des, jj) > s)
           L = des;
       System.out.println("In gapacc2, L: " + L);
       return L;
   }

  /*
   * Actually changes the lane of a car -  Take it from Origin Lane to Target Lane
   */

   public void makeChanges() throws Exception {
       for(int li = 0; li < lanes.length; li++){
           Lane ori = lanes[li];
           int i0 = (this.lane0Type==Lane.AuxLane_RAMP && li==0)? 0 : -1;            // vehicle 0 on every onramp is no updated. vehicle 0 was inserted such that vehicles behind it does not continue on lane0              
           for (int i = ori.cars.size()-1; i > i0; i--){
               Vehicle c = ori.cars.get(i);
               Lane des = c.getTargetLane();

               if (des != ori){
                   if(c.isEntering()){
                       TrafficSimulationDesktopView.RampTT+=(TrafficSimulationDesktopView.t-c.entryTime);      
                       c.resetEnteringingVehicle();  
                   }
                   int j = c.getTargetLeader(des);
                   Vehicle tmp = c;
                   
                   if (j != -2 ||(j==-2 && des.cars.size()>0 && des.cars.getFirst().ldr!=null)){                                                 //j == -2 implies no target leader

                       if (TrafficSimulationDesktopView.jrRelax)
                           setDn0(c, j, des);                           
                       tmp.setLane(tmp.getTargetLane());
                       if(i<ori.cars.size()-1)                       
                           ori.cars.get(i+1).ldr = ori.cars.get(i).ldr;         //leader of vehicle i is not changed yet
                       if(j!=-2)
                       {
                           des.cars.add(j+1,tmp);
                           des.cars.get(j+1).ldr = des.cars.get(j);
                          if (j<des.cars.size()-2)                           
                               des.cars.get(j+2).ldr = des.cars.get(j+1);       //if the inserted vehicle is the last vehicle, does not need to change the leader of the follower
                       }
                       else
                       {
                           des.cars.add(0,tmp);
                           des.cars.getFirst().ldr = des.cars.get(1).ldr;       //leader of vehicle 1 is not changed yet
                           des.cars.get(1).ldr = des.cars.getFirst();
                       }
                       ori.cars.remove(c);                                           
                       LC++;
                   }
                   else{                      
                       tmp.setLane (tmp.getTargetLane());
                       if(ori.cars.size()>1)
                           ori.cars.get(1).ldr = ori.cars.get(0).ldr;         //leader of vehicle i is not changed yet
                       ori.cars.remove(c);                                           
                       des.cars.add(0,tmp);
                       if(des.cars.size()>2)
                       {
                       des.cars.getFirst().ldr = des.cars.get(1).ldr;       //leader of vehicle 1 is not changed yet
                       des.cars.get(1).ldr = des.cars.getFirst();
                       }
                       else
                           des.cars.getFirst().ldr = null; 
                       LC++;
                   }
                   if((c.vehDistanceFromExit<Vehicle.distFromExitThreshold_LLanes && c.vehDistanceFromExit>0)||c.nearestExit<Vehicle.nearestExitThreshold){        //calibration parameter
                       int i2= (this.lane0Type==Lane.AuxLane_EXIT ||this.lane0Type==Lane.AuxLane_RAMP) ? c.getLane().idx-1: c.getLane().idx;
                           c.distanceFromExitThreshold=0.75*(1+(((double)i2)/this.nLanes));
                   }                    
                  if(c.vehDistanceFromExit>c.distanceFromExitThreshold && c.isExiting()==true)           
                       c.resetExitingVehicle();                  
               }
           }
       }

//       FileWriter fw = new FileWriter("C:\\Users\\hcho95\\Desktop\\VSLsim\\Results\\ConnID" + this.ConnId +".txt",true); //the true will append the new data
//       fw.write(Simulation.currTime*3600+"\t"+LC);
//       fw.write("\n");
//       fw.close();
   }
   



      
    private Lane relax(Lane ori, Lane des, double Vori, double Vdes, Vehicle c, int jj){
        Lane L = ori;  
        double sL = 999; 
        
        if(c.ldr != null)
        {
           if(c.ldr.getCurrSegment().ConnId==c.getCurrSegment().ConnId)
               sL = c.ldr.getX() - c.getX();
           else
               sL = c.ldr.getX()+c.getCurrSegment().length - c.getX();                
        }
        double dem_L = myMath.demand(1/sL, ori.u, ori.cap/ori.u);
        double sLL = c.getTargetSpacing(des, jj);
        double kLL = 1 / sLL;
        double dem_LL = myMath.demand(kLL, des.u, des.cap/des.u);
        double sup_LL = myMath.supply(kLL, des.cap, des.w, des.kj);
        double piL_LL = (Vdes - Vori)/u/tau;//OJO NO TIENE *dt

        if ((ori == lanes[0]) && c.isEntering()){            
            dem_L = Math.min(dem_L, this.lanes[0].zeta*ori.cap);
            double xx = ori.gore;
            double yy = ori.gore+opening+this.AccLaneLength;
            piL_LL = (c.getX() > xx && c.getX() < yy)? 1 / dt : 0;
        }

        if (piL_LL > 0) {
            if (dem_LL > 0) {            
                double gamma = Math.min(1, sup_LL / dem_LL);
                c.fi = gamma * piL_LL * dem_L / u ;// 'paper eq (15)
                c.fi = c.fi * dt * sL;              
            } else {
                    c.fi = piL_LL * dt;
            }
            
//            if ((ori == lanes[0]) && c.isEntering() && dem_LL  <= sup_LL  && 1/sL <=25){      // Note
//                L =des;
//            }
//            else if (TrafficSimulationDesktopView.relaxR.nextDouble() < c.fi)
//                L = des;

   
           if (TrafficSimulationDesktopView.relaxR.nextDouble() < c.fi)
                L = des;
           
        }    
        return L;
    }   

    /*
    *  The relax model for lane changing
    */
   private Lane Exit_relax(Lane ori, Lane des, double Vds, double Vdes, Vehicle c, int x){
       Lane L=ori;                                                              // used to check LC based on random number                                                                 
       double piL_LL = 1/dt;
       int jj = x;
       double s;
       double rn;
       
       if(des.type == Lane.AuxLane_EXIT && jj==des.cars.size()-1)
           s = c.getTargetSpacing(des, jj)-(des.length-des.gore-this.DecLaneLength-opening);
       else
           s = c.getTargetSpacing(des, jj);
              
       // This is for the AuxLane_EXIT
       if (des.type == Lane.AuxLane_EXIT){
           double xx = des.length-des.gore;
           double yy = des.length-des.gore-this.DecLaneLength-opening;         //exit Opening added by Rama                  
           piL_LL = ((c.getX() > yy)&&(c.getX() < xx))? 1 / dt : 0;            //changed as < from > by Rama
       }
       
       if (piL_LL > 0){
//          double c.fi = Math.pow(1-(c.vehDistanceFromExit/c.distanceFromExitThreshold),2);
           
          c.fi = (1-(c.vehDistanceFromExit/c.distanceFromExitThreshold));
          rn=TrafficSimulationDesktopView.ExRelaxR.nextDouble();
//          if((1 / s) < des.kj/3){
//            c.fi =Math.pow(1-(c.vehDistanceFromExit/c.distanceFromExitThreshold),1);
//          }
//          else{
//             c.fi =Math.pow(1-(c.vehDistanceFromExit/c.distanceFromExitThreshold),1);
//          }
//          if(ori.idx==4 && (1/s) > des.kj/5){
//              c.fi =Math.pow(1-(c.vehDistanceFromExit/c.distanceFromExitThreshold),0.5);
//          }
         // System.out.println("veh Id: "+c.getVehId()+" ori "+ori.idx+" des "+des.idx+" c.fi: "+c.fi+ "  rn: "+rn);
          if ((rn < c.fi && (1 / s) < des.kj))
              L=des;
          else if(c.vehDistanceFromExit<0.17 && c.vehDistanceFromExit>0 ){   //0.07 -> 0.17
              if(des.type==Lane.AuxLane_EXIT ||des.exitLane)
                  L=des;
              else 
                  L=this.lanes[0];   
//             System.out.println(c+"\t forcebly moved to exit lane: "+L);
          }
          else{
              if(des.type==Lane.AuxLane_EXIT){
                 if(1/s<des.kj/2)              
                     L=des;                     
                 else
                     c.setV(0); 
              }              
          }
           if(Vds<u-minSpeedDiff && Vdes>Vds+minSpeedDiff && L==ori)           //to make sure vehicles will change lanes if their lane is congested and desired lane is free flowing
                L =relax(ori, des, Vds, Vdes, c, x);                                     
           if(L!=ori)
               c.missedLCchances=0;
           else
               c.missedLCchances=1;           
       }
       return L;
   }   

  /*
   * Sets the dn0 of the the current car and the one that will follow it in the destination lane
   */
   private void setDn0(Vehicle c, int jj, Lane des){
       double xd;
       double xu=0;
       
       if(jj!=-2)
       {
           xd = des.cars.get(jj).getX();
           if (jj<des.cars.size()-1)
               xu = des.cars.get(jj+1).getX();
       }
       else
       {
           xd = des.cars.getFirst().ldr.getX()+des.length;
           xu = des.cars.getFirst().getX();
       }
       
       double st = xd - xu;
       double k0 = 1 / st;
       double x = xd - c.getX ();
       
       c.dn = Math.min(x * k0, 1);             
       x = c.getX () - xu;       
       if(jj!=-2 && jj<des.cars.size()-1)       
           des.cars.get(jj+1).dn = Math.min(x * k0, 1);
       
       else if (jj==-2)       
           des.cars.getFirst().dn = Math.min(x * k0, 1);       //if the vehicle is inserted as the last vehicle in the lane, then the "dn" of the u/s vehicle is changed during the updateX method. 
   }

  /*
   * java.io.Serializable overridden functions - do not delete
   */
   private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException{      //override method for file reading and saving
//       out.writeLong(ConnId);
//       out.writeInt(type);
       out.writeInt(nLanes);
       out.writeDouble(length);
       out.writeInt(leftLanes);
       out.writeDouble(leftLaneLength);
       out.writeInt(rightLanes);
       out.writeDouble(rightLaneLength);
       out.writeInt(hasAccLane);
       out.writeDouble(AccLaneLength);
       out.writeInt(hasDecLane);
       out.writeDouble(DecLaneLength);
       out.writeInt(isOnRamp);
       out.writeInt(isOffRamp);
       out.writeDouble(u);
       out.writeDouble(kj);
       out.writeDouble(w);
       out.writeDouble(tau);
       out.writeDouble(dt);
       out.writeDouble(qr);
       out.writeDouble(G);
       out.writeDouble(LC);
       out.writeDouble(sd);
       out.writeDouble(epsilon);
       out.writeObject(RM);
   }

   private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException{   //override method for file reading and saving
//       ConnId = in.readLong();
//       type = in.readInt();
       this.nLanes = in.readInt();
       this.length = in.readDouble();
       this.leftLanes = in.readInt();
       this.leftLaneLength = in.readDouble();
       this.rightLanes = in.readInt();
       this.rightLaneLength = in.readDouble();
       this.hasAccLane = in.readInt();
       this.AccLaneLength = in.readDouble();
       this.hasDecLane = in.readInt();
       this.DecLaneLength = in.readDouble();
       this.isOnRamp = in.readInt();
       this.isOffRamp = in.readInt();
       this.u = in.readDouble();
       this.kj = in.readDouble();
       this.w = in.readDouble();
       this.tau = in.readDouble();
       this.dt = in.readDouble();
       this.qr = in.readDouble();
       this.G = in.readDouble();
       this.LC = in.readDouble();
       this.sd = in.readDouble();
       this.epsilon = in.readDouble();
       this.RM = (RampMeter)in.readObject();
   }
   
  /*
   * java.lang.Object overridden functions
   */
   @Override
   public String toString(){
       return("ConnId: "+this.ConnId);//+" "+nLanes+" lanes ("+length+" km),  Connector Type: "+this.type);
   } 
}