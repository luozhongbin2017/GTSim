/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package trafficsimulationdesktop;

/**
 *
 * @author hcho95
 */
public class TrafficLight {

    int nSignalLanes;
    trafficsimulationdesktop.Connection ConnLeft, ConnRight, ConnThrough, ConnSig;
    
    double distanceToNextSignal;
//    long tempConId;


    double greenTime, redTime, offset, cycleTime;
    public boolean RedLightFlag;

    public double eventtime;


//    LinkedList<Double> MRdata;

    public TrafficLight(){

       this.greenTime = greenTime;
       this.redTime = redTime;
       this.offset = offset;
       this.cycleTime = greenTime + redTime + offset;

       this.eventtime = 0;
    }


    public TrafficLight(trafficsimulationdesktop.Connection cs,double greenTime, double redTime, double offset)
    {
       this.ConnSig = cs;
       this.greenTime = greenTime;
       this.redTime = redTime;
       this.offset = offset;
       this.cycleTime = greenTime + redTime + offset;

       this.eventtime = 0;
    }


    public TrafficLight(trafficsimulationdesktop.Connection c1, trafficsimulationdesktop.Connection c2, trafficsimulationdesktop.Connection cab,trafficsimulationdesktop.Connection cs)
    {
        this.ConnLeft = c1;
        this.ConnRight = c2;
        this.ConnThrough = cab;
        this.ConnSig = cs;
        if(ConnThrough!=null)
            this.nSignalLanes = ConnThrough.lanes.length;


       this.greenTime = greenTime;
       this.redTime = redTime;
       this.offset = offset;
       this.cycleTime = greenTime + redTime + offset;

       this.eventtime = 0;
    }

    public void setRedLight(TrafficLight tl1, double currTime){

        double stampTime = 0;

        if(currTime == 0 || currTime % this.cycleTime ==0){
            stampTime = currTime;
            this.eventtime = stampTime ;
        }
        else
            stampTime = this.eventtime;

        double eventtimeR = stampTime + this.redTime ;
        double eventtimeC = stampTime + this.cycleTime ;

        if(currTime > eventtimeR && currTime <= eventtimeC)
            tl1.RedLightFlag=true;
        else
            tl1.RedLightFlag=false;

    }


//    public void setR(double rate){
//        this.meterRate = rate;
//    }
//

//    public double getR(){
//        return this.meterRate;
//    }
//
//    public double getMinimumRate(int i)
//    {
//        RampMeter RM = Simulation.allRMs.get(i-1);
//        return RM.RMMinRate;
//    }
//
//    public void calcMinimumRate()
//    {
//           if(this.Ramp15Vol == 0)
//               this.RMMinRate = (this.AbsMaxRate + this.AbsMinRate)/2;
//           else
//           {
//               double tempstorage=0.2;
//               this.RMMinRate = (3*this.Ramp15Vol/this.nRampLanes - tempstorage/LPV/15)*this.nRampLanes;
//               this.RMMinRate = myMath.middle(this.AbsMinRate,this.RMMinRate,this.AbsMaxRate);
//           }
//    }
//
//    public void min15Data() {     //to smooth data for multiple timeperiods
//        Ramp15Vol=0;
//        double tempMaxPoints = myMath.min(timeperiods,Math.floor(TrafficSimulationDesktopView.t*3600)/DetectorStation.AggregationTime, this.ConnThrough.stations[0].stndata.size());
//        for(int i=0; i<tempMaxPoints;i++){
//            Ramp15Vol = Ramp15Vol + this.ConnThrough.stations[1].stndata.get(i)[0];
//        }
//            Ramp15Vol = Ramp15Vol/tempMaxPoints;
//    }

    @Override
    public String toString() {
        return ("Conn:"+this.ConnThrough.ConnId+" MR: "+this.cycleTime);
    }
}