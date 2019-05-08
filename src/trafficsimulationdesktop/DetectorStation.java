package trafficsimulationdesktop;

import java.util.*;
import java.io.*;

/**
 * @author Rama Chilukuri
 */
 public class DetectorStation {
	trafficsimulationdesktop.Connection conn;
        double RMMLVol, RMMLSpeed, RMMLDen, RMMLSatDensity;
        double CD, SD, FD, RD;
        double tempdata[] = new double[4];
        LinkedList<double[]> stndata;
        long stationid;
        double location;
        double corridorlocation;
        public LaneDetector[] LnDetectors;
        public static int stdId = 1;

        int N;                                                                  // cumulative counts at the location
        int N0;                                                                 // cumulative counts till previous aggregation period (20 secs) used to to get the increase in N during this aggregation period


        int period = 50;

        double cumVel;


        int currentTimeStamp = 0;


        // this variable is use to calculate average vel during an aggregation period
        public static int AggregationTime = 20;                                //30 seconds aggregation for the data
        




//        public Lane lane;

	DetectorStation(trafficsimulationdesktop.Connection conn, double location)
	{
            this.RMMLVol = 0;
            this.RMMLSpeed =0;
            this.RMMLDen =0;


            LnDetectors=new LaneDetector[conn.laneCount];
            //initializations take place here
            this.stationid = stdId++;
            this.location = location;
            this.conn = conn;
            this.N=0;
            this.N0=0;
            this.cumVel=0;

            this.conn.t=1;
//            int i2 = conn.nLanes==conn.laneCount? 0:1;           //to ensure there are no detectors in the auxillary lane
            
            int i2 = 0; // Detector on the auxillary lane is needed !!! 030216 Hyun

            for(int i=i2;i<conn.laneCount;i++){
                this.LnDetectors[i] = new LaneDetector(this, conn.lanes[i]);
                conn.lanes[i].LDlist.add(this.LnDetectors[i]);
            }
            this.RMMLSatDensity = TrafficSimulationDesktopView.SatDen;
            this.stndata = new LinkedList<double[]>();

	}

        DetectorStation(trafficsimulationdesktop.Connection conn, double location, int vdsid, double cordlocation)
	{
            this.RMMLVol = 0;
            this.RMMLSpeed =0;
            this.RMMLDen =0;


            LnDetectors=new LaneDetector[conn.laneCount];
            //initializations take place here
            this.stationid = vdsid;
            this.location = location;
            this.corridorlocation = cordlocation;

            this.conn = conn;
            this.N=0;
            this.N0=0;
            this.cumVel=0;
//            int i2 = conn.nLanes==conn.laneCount? 0:1;           //to ensure there are no detectors in the auxillary lane

            int i2 = 0; // Detector on the auxillary lane is needed !!! 030216 Hyun

            for(int i=i2;i<conn.laneCount;i++){
                this.LnDetectors[i] = new LaneDetector(this, conn.lanes[i]);
                conn.lanes[i].LDlist.add(this.LnDetectors[i]);
            }
            this.RMMLSatDensity = TrafficSimulationDesktopView.SatDen;
            this.stndata = new LinkedList<double[]>();

            this.conn.t=1;
	}



        public void calculateSD(){
            if(this.RMMLVol >= TrafficSimulationDesktopView.MinVol && this.RMMLVol <= TrafficSimulationDesktopView.MaxVol){
                if(this.RMMLSpeed >= TrafficSimulationDesktopView.MinSpeed && this.RMMLSpeed <= TrafficSimulationDesktopView.MaxSpeed){
                    if(this.RMMLDen >= TrafficSimulationDesktopView.MinDen && this.RMMLDen <= TrafficSimulationDesktopView.MaxDen)
                        this.RMMLSatDensity = TrafficSimulationDesktopView.q * this.RMMLDen + (1-TrafficSimulationDesktopView.q) * this.RMMLSatDensity;
                }
            }
        }

	public double calculateCD()
	{
            //called in SWARM computations at each Bottleneck
            smoothData();
            return RMMLDen;
	}
        public double calculateCS()
	{
            //called in SWARM computations at each Bottleneck
            smoothData();
            return RMMLSpeed;
	}
          public double calculateCF()
	{
            //called in SWARM computations at each Bottleneck
            smoothData();
            return RMMLVol *(3600/AggregationTime);
	}


         public void calculateFDandRD()
        {
            double tempden;
            double DenSampSize = myMath.min(TrafficSimulationDesktopView.DenSampSize,Math.floor(TrafficSimulationDesktopView.t*3600)/AggregationTime, stndata.size());
            double ForecastLeadTime = TrafficSimulationDesktopView.ForecastLeadTime;
            double Sumt =0, Sumt2=0, SumD=0, SumDt=0, denom, m, b;

            calculateCD();
            calculateSD();

            for(int j=0; j<DenSampSize; j++)
            {
                tempden = (stndata.get(j)[1]==0? (stndata.get(j)[0]==0? 0:this.conn.kj):((double)stndata.get(j)[0]*(3600/AggregationTime)/(stndata.get(j)[1]*this.LnDetectors.length)));
                Sumt += j;
                Sumt2 += j*j;
                SumD += tempden;
                SumDt += tempden * j;
            }
            denom = DenSampSize * Sumt2 - Sumt * Sumt;
            m = denom>0 ? (DenSampSize * SumDt - Sumt * SumD) / denom:0;
            b = denom>0 ?(SumD * Sumt2 - Sumt * SumDt) / denom:0;
            this.FD = b + m * ForecastLeadTime;

            if(this.FD > this.RMMLSatDensity){
                if(this.RMMLDen > this.RMMLSatDensity)
                    this.RD = 2 * this.RMMLSatDensity - this.FD;
                else
                    this.RD = this.RMMLSatDensity - (this.FD - this.RMMLSatDensity)/ForecastLeadTime;
            }
            this.RD = this.RD> this.RMMLSatDensity/2 ? this.RD: this.RMMLSatDensity/2;
        }

	private void smoothData()     //to smooth data for multiple timeperiods
	{
            double sumVol=0, sumVSpd=0;
            double tempMaxPoints = myMath.min(TrafficSimulationDesktopView.maxpoints,Math.floor(TrafficSimulationDesktopView.t*3600)/AggregationTime);
            for(int i=0;i<tempMaxPoints;i++){
                sumVol = sumVol+stndata.get(i)[0];
                sumVSpd = sumVSpd + stndata.get(i)[0]*stndata.get(i)[1];
            }

            // Per Lane

            if(this.conn.lane0Type==Lane.AuxLane_RAMP){
                RMMLVol = sumVol/tempMaxPoints/(this.LnDetectors.length-1) ;
            }else
                RMMLVol = sumVol/tempMaxPoints/this.LnDetectors.length ;

            //----// Shoulder Lane Detector

//            RMMLVol = sumVol/tempMaxPoints;

            //------------------

            RMMLSpeed = sumVol>0 ? sumVSpd/sumVol:this.conn.u;
            RMMLDen = RMMLSpeed==0? (RMMLVol==0? 0:this.conn.kj):((double)RMMLVol*(3600/AggregationTime)/RMMLSpeed);   //multiplied by 180 because, the volume is 20 seconds data.

        }



        public void insertIntoDB() throws Exception{


            if(stndata.size()== 12){                                            //saves the last 120 seconds of data only
                stndata.remove(11);
            }
            tempdata[0] = N-N0; // # vehicle during the period
            tempdata[1] = cumVel==0? 0:(N-N0)/cumVel;
            stndata.addFirst(tempdata);
            N0=N;

//           --------------Heat Map Detectors
//
            if(this.LnDetectors[0].lane.section.isOffRamp==0){
            FileWriter fw = new FileWriter("C:\\Users\\txu81\\Desktop\\VSLsim\\SingleMerge\\Results\\station"+this.stationid+".txt",true); //the true will append the new data
            fw.write(Simulation.currTime*3600+"\t"+this.location+"\t"+tempdata[1]+"\t"+tempdata[0]);
            fw.write("\n");
            fw.close();
            }


            cumVel = 0;


        }

   

@Override
   public String toString(){
       return("StationID: "+this.stationid+", Location: "+this.location+" , Connection: "+this.conn);
    }
}
