/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package trafficsimulationdesktop;
import java.util.*;
import java.io.*;
/**
 *
 * @author hcho95
 */
public class VariableSpeedLimit {


	trafficsimulationdesktop.Connection conn;
        double RMMLVol, RMMLSpeed, RMMLDen, RMMLSatDensity;
        double CD, SD, FD, RD;
        double tempdata[] = new double[2];

        LinkedList<double[]> vdsdata;
        double tempvdsdata[] = new double[3];

        long variablespeedlimitId;


        int numberofvdsofvsl;
        int connectedvdsid[] = new int[6];

        double location;
        double corvsllocation;
        public LaneDetector[] VSLDetectors;
        public static int vslId;
        int vdsnumber;
        int N;                                                                  // cumulative counts at the location
        int N0;                                                                 // cumulative counts till previous aggregation period (20 secs) used to to get the increase in N during this aggregation period
        double cumVel;                                                         // this variable is use to calculate average vel during an aggregation period
        public static int AggregationTime = 120;                                //120 seconds aggregation for the data
        double aveVel;
        double VSL0;
        double setVSL;
        double w;
        double kj;

        trafficsimulationdesktop.DetectorStation VDS0;
        trafficsimulationdesktop.DetectorStation VDS1;
        trafficsimulationdesktop.DetectorStation VDS2;
        trafficsimulationdesktop.DetectorStation VDS3;
        trafficsimulationdesktop.DetectorStation VDS4;


        trafficsimulationdesktop.VariableSpeedLimit VSLus;
        trafficsimulationdesktop.VariableSpeedLimit VSLds;



	VariableSpeedLimit(trafficsimulationdesktop.Connection conn, double location , int vslid , int vdsnumber)
	{
            this.RMMLVol = 0;
            this.RMMLSpeed =0;
            this.RMMLDen =0;
            //initializations take place here

            VSLDetectors=new LaneDetector[conn.laneCount];

            this.numberofvdsofvsl = vdsnumber;
            this.variablespeedlimitId = vslid;
            this.location = location;
            this.conn = conn;
            this.N=0;
            this.N0=0;
            this.cumVel=0;
            this.aveVel=0;
            this.VSL0=100;
            this.setVSL=100;
            this.w = 20;
            this.kj=150;

            int i2 = conn.nLanes==conn.laneCount? 0:1;

            for(int i= i2; i<conn.laneCount;i++){
                this.VSLDetectors[i] = new LaneDetector(this, conn.lanes[i]);
                conn.lanes[i].VSLlist.add(this.VSLDetectors[i]);
            }

            this.RMMLSatDensity = TrafficSimulationDesktopView.SatDen;
            this.vdsdata = new LinkedList<double[]>();
	}
        
        VariableSpeedLimit(trafficsimulationdesktop.Connection conn, double location , int vslid , double loc, int vdsnumber, int vdsid0, int vdsid1)
	{
            this.RMMLVol = 0;
            this.RMMLSpeed =0;
            this.RMMLDen =0;
            //initializations take place here

            VSLDetectors=new LaneDetector[conn.laneCount];

            this.numberofvdsofvsl = vdsnumber;
            this.connectedvdsid[0] = vdsid0;
            this.connectedvdsid[1] = vdsid1;
            
            
            this.variablespeedlimitId = vslid;
            this.location = location;
            this.corvsllocation = loc;
            this.conn = conn;
            this.N=0;
            this.N0=0;
            this.cumVel=0;
            this.aveVel=0;
            this.VSL0=100;
            this.setVSL=100;
            this.w = 20;
            this.kj=150;

            if(this.conn.ConnId==8){
                this.conn.vslnumber = 2;
            }else
                this.conn.vslnumber = 1;

            int i2 = conn.nLanes==conn.laneCount? 0:1;

            for(int i= i2; i<conn.laneCount;i++){
                this.VSLDetectors[i] = new LaneDetector(this, conn.lanes[i]);
                conn.lanes[i].VSLlist.add(this.VSLDetectors[i]);
            }

            this.RMMLSatDensity = TrafficSimulationDesktopView.SatDen;
            this.vdsdata = new LinkedList<double[]>();
	}

        VariableSpeedLimit(trafficsimulationdesktop.Connection conn, double location , int vslid ,double loc, int vdsnumber, int vdsid0, int vdsid1, int vdsid2)
	{
            this.RMMLVol = 0;
            this.RMMLSpeed =0;
            this.RMMLDen =0;
            //initializations take place here

            VSLDetectors=new LaneDetector[conn.laneCount];

            this.numberofvdsofvsl = vdsnumber;
            this.connectedvdsid[0] = vdsid0;
            this.connectedvdsid[1] = vdsid1;
            this.connectedvdsid[2] = vdsid2;

            this.conn.vslnumber = 1;

            this.variablespeedlimitId = vslid;
            this.location = location;
            this.corvsllocation = loc;
            this.conn = conn;
            this.N=0;
            this.N0=0;
            this.cumVel=0;
            this.aveVel=0;
            this.VSL0=100;
            this.setVSL=100;
            this.w = 20;
            this.kj=150;

            int i2 = conn.nLanes==conn.laneCount? 0:1;

            for(int i= i2; i<conn.laneCount;i++){
                this.VSLDetectors[i] = new LaneDetector(this, conn.lanes[i]);
                conn.lanes[i].VSLlist.add(this.VSLDetectors[i]);
            }

            this.RMMLSatDensity = TrafficSimulationDesktopView.SatDen;
            this.vdsdata = new LinkedList<double[]>();
	}

        VariableSpeedLimit(trafficsimulationdesktop.Connection conn, double location , int vslid , double loc, int vdsnumber, int vdsid0, int vdsid1, int vdsid2, int vdsid3)
	{
            this.RMMLVol = 0;
            this.RMMLSpeed =0;
            this.RMMLDen =0;
            //initializations take place here

            VSLDetectors=new LaneDetector[conn.laneCount];

            this.numberofvdsofvsl = vdsnumber;
            this.connectedvdsid[0] = vdsid0;
            this.connectedvdsid[1] = vdsid1;
            this.connectedvdsid[2] = vdsid2;
            this.connectedvdsid[3] = vdsid3;

            this.conn.vslnumber = 1;

            this.variablespeedlimitId = vslid;
            this.location = location;
            this.corvsllocation = loc;
            this.conn = conn;
            this.N=0;
            this.N0=0;
            this.cumVel=0;
            this.aveVel=0;
            this.VSL0=100;
            this.setVSL=100;
            this.w = 20;
            this.kj=150;

            int i2 = conn.nLanes==conn.laneCount? 0:1;

            for(int i= i2; i<conn.laneCount;i++){
                this.VSLDetectors[i] = new LaneDetector(this, conn.lanes[i]);
                conn.lanes[i].VSLlist.add(this.VSLDetectors[i]);
            }

            this.RMMLSatDensity = TrafficSimulationDesktopView.SatDen;
            this.vdsdata = new LinkedList<double[]>();
	}

        VariableSpeedLimit(trafficsimulationdesktop.Connection conn, double location , int vslid , double loc, int vdsnumber, int vdsid0, int vdsid1, int vdsid2, int vdsid3, int vdsid4)
	{
            this.RMMLVol = 0;
            this.RMMLSpeed =0;
            this.RMMLDen =0;
            //initializations take place here

            VSLDetectors=new LaneDetector[conn.laneCount];

            this.numberofvdsofvsl = vdsnumber;
            this.connectedvdsid[0] = vdsid0;
            this.connectedvdsid[1] = vdsid1;
            this.connectedvdsid[2] = vdsid2;
            this.connectedvdsid[3] = vdsid3;
            this.connectedvdsid[4] = vdsid4;

            this.conn.vslnumber = 1;

            this.variablespeedlimitId = vslid;
            this.location = location;
            this.corvsllocation = loc;
            this.conn = conn;
            this.N=0;
            this.N0=0;
            this.cumVel=0;
            this.aveVel=0;
            this.VSL0=100;
            this.setVSL=100;
            this.w = 20;
            this.kj=150;

            int i2 = conn.nLanes==conn.laneCount? 0:1;

            for(int i= i2; i<conn.laneCount;i++){
                this.VSLDetectors[i] = new LaneDetector(this, conn.lanes[i]);
                conn.lanes[i].VSLlist.add(this.VSLDetectors[i]);
            }

            this.RMMLSatDensity = TrafficSimulationDesktopView.SatDen;
            this.vdsdata = new LinkedList<double[]>();
	}
        VariableSpeedLimit(trafficsimulationdesktop.Connection conn, int vslid, double location , int vdsid0, int vdsid1, int vdsid2, int vdsid3, int vdsid4)
	{
            this.RMMLVol = 0;
            this.RMMLSpeed =0;
            this.RMMLDen =0;
            //initializations take place here

            VSLDetectors=new LaneDetector[conn.laneCount];

            this.numberofvdsofvsl = 5;
            this.connectedvdsid[0] = vdsid0;
            this.connectedvdsid[1] = vdsid1;
            this.connectedvdsid[2] = vdsid2;
            this.connectedvdsid[3] = vdsid3;
            this.connectedvdsid[4] = vdsid4;



            this.variablespeedlimitId = vslid;
            this.location = location;

            this.conn = conn;
            this.N=0;
            this.N0=0;
            this.cumVel=0;
            this.aveVel=0;
            this.VSL0=100;
            this.setVSL=100;
            this.w = 20;
            this.kj=150;

            this.conn.vslnumber = 1;

            int i2 = conn.nLanes==conn.laneCount? 0:1;

            for(int i= i2; i<conn.laneCount;i++){
                this.VSLDetectors[i] = new LaneDetector(this, conn.lanes[i]);
                conn.lanes[i].VSLlist.add(this.VSLDetectors[i]);
            }

            this.RMMLSatDensity = TrafficSimulationDesktopView.SatDen;
            this.vdsdata = new LinkedList<double[]>();
	}

        public void calculateVSL(double timestep, trafficsimulationdesktop.DetectorStation VDS0, trafficsimulationdesktop.DetectorStation VDS1,trafficsimulationdesktop.DetectorStation VDS2, trafficsimulationdesktop.DetectorStation VDS3, trafficsimulationdesktop.DetectorStation VDS4) throws Exception{

            double Qt = 2500;
            int ln = 0;
            
            if(VDS0.conn.lane0Type==Lane.AuxLane_RAMP){
                ln = VDS0.LnDetectors.length-1;
            }else
                ln = VDS0.LnDetectors.length ;

//            if(VDS0.calculateCF()  + VDS2.calculateCF() >= VDS1.calculateCF() || VDS1.calculateCD() > kj/5){

//                setVSL = w * ((kj/(kj - ((Qt- VDS1.calculateCF() )/w)))- 1);
//                setVSL = w * ((kj/(kj - ((Qt- VDS1.conn.RM.meterRate )/w)))- 1); //RM

//                setVSL = 1 * w * ((ln * kj/(ln * kj - ((ln*Qt- VDS2.conn.RM.meterRate )/w)))- 1); //RM
                
//                setVSL = 1 * w * (( ln*kj/(ln* kj - ((ln*Qt *(7500/7500) - VDS2.conn.stations[3].calculateCF())/w)))- 1); //RM

//                double A = (ln*Qt * TrafficSimulationDesktopView.alpha - VDS2.conn.stations[0].calculateCF()*TrafficSimulationDesktopView.beta );
//                setVSL = A/(ln*kj-A/w);

                double A = (Qt * TrafficSimulationDesktopView.gamma - VDS2.conn.stations[0].calculateCF()*TrafficSimulationDesktopView.beta );

                if(VDS4.conn.stations[0].calculateCF()<A && VDS4.conn.stations[9].calculateCF()>1000){
                    setVSL=100;
                }else
                    setVSL = A/(kj-A/w);

               

                if(setVSL < 0)
                    setVSL = 100;

//            FileWriter fw = new FileWriter("C:\\Users\\hcho95\\Desktop\\VSLsim\\SingleMerge\\Results\\VSL"+this.vslId+".txt",true); //the true will append the new data
//            fw.write(Simulation.currTime*3600+"\t"+this.setVSL);
//            fw.write("\n");
//            fw.close();


//
//            }else
//                setVSL = 100;
        }

       public void calculateVSL2(double timestep, trafficsimulationdesktop.DetectorStation VDS0, trafficsimulationdesktop.DetectorStation VDS1,trafficsimulationdesktop.DetectorStation VDS2, trafficsimulationdesktop.DetectorStation VDS3, trafficsimulationdesktop.DetectorStation VDS4) throws Exception{

            double Qt = 2500;
            int ln = 0;

            if(VDS0.conn.lane0Type==Lane.AuxLane_RAMP){
                ln = VDS0.LnDetectors.length-1;
            }else
                ln = VDS0.LnDetectors.length ;

//                double A = (ln*Qt * TrafficSimulationDesktopView.alpha - VDS2.conn.stations[0].calculateCF() );
//
//                setVSL = A/(ln*kj-A/w);


            double A = (Qt * TrafficSimulationDesktopView.gamma - VDS2.conn.stations[0].calculateCF() );
                if(VDS4.conn.stations[0].calculateCF()<A&& VDS4.conn.stations[9].calculateCF()>1000){
                    setVSL=100;
                }else
                    setVSL = A/(kj-A/w);

                 if(setVSL < 0)
                    setVSL = 100;


//            FileWriter fw = new FileWriter("C:\\Users\\hcho95\\Desktop\\VSLsim\\SingleMerge\\Results\\VSL"+this.vslId+".txt",true); //the true will append the new data
//            fw.write(Simulation.currTime*3600+"\t"+this.setVSL);
//            fw.write("\n");
//            fw.close();

               

//                if(setVSL < 40)
//                    setVSL = 40;
//
        }


         public void bufferVSL(double timestep, trafficsimulationdesktop.VariableSpeedLimit VSLus, trafficsimulationdesktop.VariableSpeedLimit VSLds) throws Exception{

            if( VSLus.setVSL - 10 > VSLds.setVSL){
                setVSL = VSLds.setVSL + 10 ;
            }

        }


@Override
   public String toString(){
       return("VSLID: "+this.variablespeedlimitId+ "VSL: " + this.setVSL+ "Location: "+this.location+" , Connection: "+this.conn);
    }
}
