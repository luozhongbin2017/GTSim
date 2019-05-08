package trafficsimulationdesktop;
//import java.util.*;

/**
 *
 * @author Rama Chilukuri
 */
public class SWARM2 {
    public static void calculateMinMax()
    {
        RampMeter RM;
        for(int i=0; i< Simulation.allRMs.size();i++){
            RM = Simulation.allRMs.get(i);
            RM.SW2LocalMax = RM.AbsMaxRate;
            RM.SW2LocalMin = RM.RMMinRate <= RM.AbsMinRate? RM.RMMinRate: RM.AbsMinRate;
        }
    }	
    
    public static void SWARM2_Alinea()
    {
        double UpRate = TrafficSimulationDesktopView.UpRate;
        double DownRate = TrafficSimulationDesktopView.DownRate;
        double TempRate=0;//a, b;
        RampMeter RM;
        double SmoothedRate;
        for(int i=0;i<Simulation.allRMs.size();i++){
            RM = Simulation.allRMs.get(i);
            double currDen =  RM.ConnDS.stations[0].RMMLDen;
            double optDen = RM.ConnDS.kj*RM.ConnDS.w/(RM.ConnDS.u+RM.ConnDS.w);          
            TempRate = Alinea.AlineaMR(RM.meterRate, optDen, currDen,TrafficSimulationDesktopView.KR);
            TempRate = myMath.middle(RM.SW2LocalMin, TempRate, RM.SW2LocalMax);
            double rateChange = TempRate-RM.meterRate;

            if(rateChange>=0)
                SmoothedRate = RM.meterRate+(UpRate*RM.nRampLanes> rateChange ? rateChange:UpRate*RM.nRampLanes);
            else
                SmoothedRate = RM.meterRate-(DownRate*RM.nRampLanes> -1*rateChange ? DownRate*RM.nRampLanes: -1*rateChange);

            RM.SW2Rate = myMath.middle(RM.SW2LocalMin, SmoothedRate, RM.SW2LocalMax );
        }      
    }        
}
