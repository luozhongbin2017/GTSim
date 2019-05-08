package trafficsimulationdesktop;
//import java.util.*;
/**
 *
 * @author Rama Chilukuri
 */
public class SWARM1 {
	public static void calculateLocalMinMax(){
            RampMeter RM;
            for(int i=0;i<Simulation.allRMs.size();i++) {
                RM = Simulation.allRMs.get(i);
                if(ModeManager.SWARM1 || ModeManager.SWARM1AND2){
                    if(ModeManager.SWARM1)
                        RM.SW1LocalMax = RM.AbsMaxRate;
                    if(ModeManager.SWARM1AND2)
                        RM.SW1LocalMax = RM.SW2Rate;
                    RM.SW1LocalMin = RM.RMMinRate;
                }
                else if(ModeManager.ALINEA)
                    RM.SW1LocalMax = RM.SW1LocalMin = RM.SW2Rate;
            }
	}
        
	public static void SWARM1Apportionment()
	{
            double UpRate = TrafficSimulationDesktopView.UpRate;
            double DownRate = TrafficSimulationDesktopView.DownRate;
            double InterSectionPropagationFactor = TrafficSimulationDesktopView.InterSectionPropagationFactor;            
            double Excess=0, DesiredRate, DeltaDen, TargetRate, SmoothedRate;
            
            double rateChange=0;
            RampMeter RM;
            DetectorStation v;
            for(int j=Simulation.allRMs.size()-1;j>=0;j--) {
                    RM=Simulation.allRMs.get(j);
                    v=RM.ConnDS.stations[0];

                    Excess *= InterSectionPropagationFactor;

                    DeltaDen = (v.RMMLDen * v.conn.nLanes)  - (ModeManager.SWARM_bottleneck.RD * ModeManager.SWARM_bottleneck.LnDetectors.length);
                    DesiredRate = RM.AbsMaxRate - (DeltaDen * RM.distanceToNextRamp) - (Excess*Math.pow(InterSectionPropagationFactor, RM.distanceToNextRamp));

                    TargetRate = myMath.middle(RM.SW1LocalMin, DesiredRate, RM.SW1LocalMax);

                    rateChange = TargetRate-RM.meterRate;

                    if(rateChange>=0)
                        SmoothedRate = RM.meterRate+(UpRate*RM.nRampLanes> rateChange ? rateChange:UpRate*RM.nRampLanes);
                    else
                        SmoothedRate = RM.meterRate-(DownRate*RM.nRampLanes> -1*rateChange ? DownRate*RM.nRampLanes: -1*rateChange);

                    RM.SW1Rate = myMath.middle(RM.SW1LocalMin, SmoothedRate, RM.SW1LocalMax );
                    Excess = (RM.SW1Rate - DesiredRate) * Math.pow(InterSectionPropagationFactor ,RM.distanceToNextRamp);
                }
        }            
}
