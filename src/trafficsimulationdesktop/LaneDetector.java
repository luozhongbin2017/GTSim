/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package trafficsimulationdesktop;

//import com.mysql.jdbc.Connection;
//import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
/**
 *
 * @author Rama Chilukuri
 */
public class LaneDetector// implements Runnable
{
    public DetectorStation station;
    public VariableSpeedLimit vsl;
    public Lane lane;
//    protected float tempData[][]= new float[1000][3];
    protected int tempVol=0;
    protected double tempVel=0;
    int i;
    public Vehicle lastCar;                                                   //last car counted by this detector
    public double X;

    
    public LaneDetector(DetectorStation station, Lane lane)
    {
        this.station = station;
        this.lane = lane;
        this.lastCar = null; //new Vehicle(0, lane);
        this.X = this.station.location;
        this.vsl = null;
        i=0;        
    }

    public LaneDetector(VariableSpeedLimit vsl, Lane lane)
    {
        this.station = null;
        this.vsl = vsl;
        this.lane = lane;
        this.lastCar = null; //new Vehicle(0, lane);
        this.X = this.vsl.location;
        i=0;
    }



    
//    public void insertIntoDB(Vehicle c) throws Exception
//    {
//        try{
//            this.tempVol++;
//            this.tempVel = this.tempVel +c.getV();
//            if((Math.round(TrafficSimulationDesktopView.t*3600))%12 ==0){
//                Connection con;
//                Statement st;
//
//                Class.forName("com.mysql.jdbc.Driver");
//                con = (Connection) DriverManager.getConnection("jdbc:mysql://localhost/loopDetectorInfo","root","srivani");
//                st = (Statement) con.createStatement();
//
//                    int Station = (int)this.station.stationid;
//                    int Detector = (int)this.lane.idx;
//                    double Time = Math.round(TrafficSimulationDesktopView.t*3600);
//                    int VehId = tempVol;
//                    double Speed = tempVel/tempVol;
//                    String insert = "INSERT INTO run2 VALUES('"+Station+"','"+Detector+"','"+Time+"','"+VehId+"','"+Speed+"')";
//                    st.executeUpdate(insert);
//                tempVel=0;
//                tempVol=0;
//                st.close();
//                con.close();
//
//            }
//      } catch(Exception e){System.out.println("error in insert into Db"+e.getMessage());}
//    }
            
@Override
   public String toString(){
       return("Station: "+this.station+" VSL: "+this.vsl+", lane: "+this.lane+" , Location: "+this.X  );
   }
}
