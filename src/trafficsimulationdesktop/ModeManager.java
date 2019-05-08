package trafficsimulationdesktop;

//import java.io.BufferedWriter;
import java.io.FileWriter;
//import java.util.*;
//import org.jgrapht.graph.*;
/**
 *
 * @author Bhargava Chilukuri
 */
public class ModeManager {
    public static boolean ALINEA=true;
    public static boolean SWARM1;
    public static boolean SWARM1AND2;

    public static DetectorStation SWARM_bottleneck;
    public static DetectorStation[] VDS;

    public static DetectorStation RampDetector;



    public static int AvgIntervalsFlush=6;                                     //average of 6 intevals is used to determine flush
    public static boolean QueueFlush=true;


    public static void QueueFlush(){
      try{
//        FileWriter gw = new FileWriter("Z:\\RampMeteringProject\\PhaseII\\OptimizationResults\\FlushTiming.txt",true); //the true will append the new data                
        double multiplier = 3600/(DetectorStation.AggregationTime*AvgIntervalsFlush);
        RampMeter RM;
        
        for(int i=0; i<Simulation.allRMs.size(); i++){
            int vol=0;
            double vel=0;
            double den=0;
            RM = Simulation.allRMs.get(i);
            for(int j=0;j<Math.min(RM.ConnRM.stations[0].stndata.size(),AvgIntervalsFlush);j++){
                vol+= RM.ConnRM.stations[0].stndata.get(j)[0];
                vel+= RM.ConnRM.stations[0].stndata.get(j)[0]*RM.ConnRM.stations[0].stndata.get(j)[1];  // space mean speed
            }            
            vel = vol>0? vel/vol:RM.ConnRM.u;
            
            if(vel<RM.ConnRM.u*0.99)
                den=RM.ConnRM.kj-(vol*multiplier/RM.ConnRM.w);
            else
               den = vol*multiplier/vel; 

            if(den>RM.flushUpOcc && !RM.QFlushFlag){  //averaging over a 2 minute period
                RM.QFlushFlag=true;
                RM.flushCount++;
                RM.flushStartTime = TrafficSimulationDesktopView.t;
//                gw.write(RM+"\t"+TrafficSimulationDesktopView.t+"\t");//appends the string to the file
            }
            if(den<RM.flushDownOcc && RM.QFlushFlag){
                RM.QFlushFlag=false;
//                gw.write(RM+"\t End Time: \t"+TrafficSimulationDesktopView.t+"\t Duration: \t"+(TrafficSimulationDesktopView.t-RM.flushStartTime));//appends the string to the file
            }
        }
//        gw.write("\n");
//        gw.close();               
      } catch (Exception e){System.out.println("Error in writing FlushTiming file: "+e.getMessage());}
    }

    public static void ALINEA(){
        RampMeter RM;
        double tempMR=0;
        double tempDen = 0;
        int kr = 0;

        try{
//            FileWriter fw = new FileWriter("Z:\\RampMeteringProject\\PhaseII\\OptimizationResults\\ALINEA_AllMR.txt",true); //the true will append the new data                    
            for(int i=0; i<Simulation.allRMs.size(); i++){
                RM = Simulation.allRMs.get(i);                
//                double currDen = RM.ConnDS.stations[0].calculateCD();
                double currDen = RM.ConnDS.stations[0].calculateCD();
//                double optDen = RM.ConnDS.kj*RM.ConnDS.w/(RM.ConnDS.u+RM.ConnDS.w);

                tempDen = 25; //20
                kr = TrafficSimulationDesktopView.KR;

                double optDen = tempDen;

                tempMR=  myMath.middle(RM.AbsMinRate, Alinea.AlineaMR(RM.meterRate, optDen, currDen, kr),RM.AbsMaxRate);
                RM.meterRate =tempMR;
            }
//            fw.write("\n");
//            fw.close();               
        }catch (Exception e){
            System.out.println("Error in ALINEA: "+e.getMessage() + e.toString());}
    }
    
    public static void SWARM1(){
        RampMeter RM;
        try{
            SWARM_bottleneck.calculateFDandRD();
            for(int i=0; i<Simulation.allRMs.size(); i++){
                RM = Simulation.allRMs.get(i);
                RM.ConnDS.stations[0].calculateCD();
                RM.ConnDS.stations[0].calculateSD();
                RM.calcMinimumRate();
            }
            trafficsimulationdesktop.SWARM1.calculateLocalMinMax();
            trafficsimulationdesktop.SWARM1.SWARM1Apportionment();
        }catch (Exception e){System.out.println("Error in SWARM1: "+e.getMessage());}        
    } 

    public static void SWARM1AND2(){
        RampMeter RM;
        try{
            SWARM_bottleneck.calculateFDandRD();
            for(int i=0; i<Simulation.allRMs.size(); i++){
                RM = Simulation.allRMs.get(i);
                RM.ConnDS.stations[0].calculateCD();
                RM.ConnDS.stations[0].calculateSD();
                RM.min15Data();
                RM.calcMinimumRate();
            }
            trafficsimulationdesktop.SWARM2.SWARM2_Alinea();
            trafficsimulationdesktop.SWARM1.calculateLocalMinMax();
            trafficsimulationdesktop.SWARM1.SWARM1Apportionment();
            trafficsimulationdesktop.SWARM2.calculateMinMax();
            ModeOpsMgr();
        }catch (Exception e){System.out.println("Error in SWARM1AND2: "+e.getMessage());}
    } 
    
    private static void ModeOpsMgr(){
        RampMeter RM;
        double tempMR=0;
        try{            
            FileWriter fw = new FileWriter("Z:\\RampMeteringProject\\PhaseII\\OptimizationResults\\"+TrafficSimulationDesktopView.meteringRate+"-AllMR.txt",true); //the true will append the new data                                
            for(int i=0; i<Simulation.allRMs.size(); i++){
                RM = Simulation.allRMs.get(i);
                
                if(SWARM1)
                    tempMR = (int)RM.SW1Rate;
                else if(SWARM1AND2)
                    tempMR = (int)(RM.SW1Rate>RM.SW2Rate ? RM.SW2Rate: RM.SW1Rate);                
                RM.meterRate = tempMR;                
                fw.write(tempMR+"\t");//appends the string to the file                                                        
            }
            fw.write("\n");
            fw.close();              
        }catch (Exception e){System.out.println("Error in ModeOpsMgr: "+e.getMessage());}
    }     
}

