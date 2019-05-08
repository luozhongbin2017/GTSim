package trafficsimulationdesktop;
//import java.util.*;
//import java.sql.*;
//import com.mysql.jdbc.Connection;
//import com.mysql.jdbc.Statement;
//import java.sql.DriverManager;
//import java.sql.ResultSet;

/**
 *
 * @author Bhargava Chilukuri & Hyun Cho
 */
public class RampMeter implements java.io.Serializable
{
    int nRampLanes;
    trafficsimulationdesktop.Connection ConnUS, ConnDS, ConnRM;
    double StorageBegin, AbsMaxRate, AbsMinRate, LPV=0.00762;       //ramp length is the storage area;
    double SW1LocalMin, SW1LocalMax, SW2LocalMin, SW2LocalMax, SW1Rate, SW2Rate; //TOD variables are contol parameters
    double Ramp15Vol;
    double RMMinRate;
    int timeperiods = 45;                                                      //15 minutes of data
    double distanceToNextRamp;
//    long tempConId;
    public boolean QFlushFlag;

    public boolean QFlushFlag_2;

    public int flushCount;                                        //this is the total number of flushes during simulation
    public double flushStartTime;
    public double flushUpOcc;
    public double flushDownOcc;
    double meterRate;

//    LinkedList<Double> MRdata;

    public RampMeter(){
        this.AbsMaxRate= 1800;
        this.AbsMinRate = 600;
        this.meterRate = 1000;
        this.flushUpOcc=67.5;
        this.flushDownOcc=37.5;
        this.StorageBegin =  0.5;   //ramp meter is half way on the ramp

    }

    public RampMeter(trafficsimulationdesktop.Connection c1, trafficsimulationdesktop.Connection c2, trafficsimulationdesktop.Connection cab)
    {
        this.ConnUS = c1;
        this.ConnDS = c2;
        this.ConnRM = cab;
        if(ConnRM!=null)
            this.nRampLanes = ConnRM.lanes.length;
        this.meterRate = 1000;
//    }
//    public RampMeter(){


        this.AbsMaxRate= 1800;
        this.AbsMinRate = 600;
        this.flushUpOcc=67.5;
        this.flushDownOcc=37.5;
        this.StorageBegin =  this.ConnRM.length/2;   //ramp meter is half way on the ramp
    }
    
    public void setR(double rate){
        this.meterRate = rate;
    }
    
    public double getR(){
        return this.meterRate;
    }
    
    public double getMinimumRate(int i)
    {
        RampMeter RM = Simulation.allRMs.get(i-1);
        return RM.RMMinRate;
    }
    
    public void calcMinimumRate()
    {
           if(this.Ramp15Vol == 0)
               this.RMMinRate = (this.AbsMaxRate + this.AbsMinRate)/2;
           else
           {
               double tempstorage=0.2;
               this.RMMinRate = (3*this.Ramp15Vol/this.nRampLanes - tempstorage/LPV/15)*this.nRampLanes;
               this.RMMinRate = myMath.middle(this.AbsMinRate,this.RMMinRate,this.AbsMaxRate);
           }
    }

    public void min15Data() {     //to smooth data for multiple timeperiods    
        Ramp15Vol=0;                
        double tempMaxPoints = myMath.min(timeperiods,Math.floor(TrafficSimulationDesktopView.t*3600)/DetectorStation.AggregationTime, this.ConnRM.stations[0].stndata.size());
        for(int i=0; i<tempMaxPoints;i++){            
            Ramp15Vol = Ramp15Vol + this.ConnRM.stations[1].stndata.get(i)[0];
        }
            Ramp15Vol = Ramp15Vol/tempMaxPoints;
    } 

    @Override
    public String toString() {
        return ("Conn:"+this.ConnRM.ConnId+" MR: "+this.meterRate);
    }   
}