/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package trafficsimulationdesktop;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.sql.*;
import java.sql.Connection;
import java.util.LinkedList;


/**
 *
 * @author Shalini Pothuru
 */
public class Logic 
{
    public static int StringToInt(String s)
    {
        int l = s.length();
        while(s.charAt(0) == ' ')
        {
           s = s.substring(1, l);
           l = s.length();
           if(l==0)
           {
               s=null;
               break;
           }
        }
        //System.out.println("s: " + s);
        if(s == null)
            return 0;
        return Integer.parseInt(s);
    }
    
    public static void convertAsciiToGraph(String fileName) throws Exception
    {
       int i=0;
       int upNode, downNode, ddNode, noOfLanes, node, X, Y;
       int isOnRamp = 0, isOffRamp = 0, type;
       double linkLength;
       
       System.out.println("filename in Logic(): " + fileName);
       LinkedList<String> recordType11 = new LinkedList<String>();
       LinkedList<String> recordType19 = new LinkedList<String>();
       LinkedList<String> recordType25 = new LinkedList<String>();
       LinkedList<String> recordType50 = new LinkedList<String>();
       LinkedList<String> recordType195 = new LinkedList<String>();
       String tempRecordHolder = null;
       File file = new File("C:\\" + fileName);
       FileInputStream fis = null;
       BufferedInputStream bis = null;
       DataInputStream dis = null;

       fis = new FileInputStream(file);
       bis = new BufferedInputStream(fis);
       dis = new DataInputStream(bis);
       
       String URL = "jdbc:microsoft:sqlserver://localhost:1433;DatabaseName=network"; 
       Class.forName("com.microsoft.jdbc.sqlserver.SQLServerDriver"); 
       Connection con = DriverManager.getConnection(URL,"sa","");//.getConnection(URL,"root","sairam");
       Statement st1 = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_UPDATABLE);
       st1.execute("use network");         
       ResultSet rs1;
       
       while (dis.available() != 0) 
       {
           tempRecordHolder = dis.readLine();
           type = StringToInt(tempRecordHolder.substring(77, 80));
           
           if(type == 11)
               recordType11.add(tempRecordHolder);
           if(type == 19)
               recordType19.add(tempRecordHolder);
           if(type == 25)
               recordType25.add(tempRecordHolder);
           if(type == 50)
               recordType50.add(tempRecordHolder);
           if(type == 195)
               recordType195.add(tempRecordHolder);
       }
       
       for(i=0;i<recordType195.size();i++)
       {
           node = StringToInt(recordType195.get(i).substring(0,4));
           X = StringToInt(recordType195.get(i).substring(6,12));
           Y = StringToInt(recordType195.get(i).substring(14,20));
           st1.execute("insert into nodeInfo values(" + node + "," + X + "," + Y + ",0,0)");
       }
       
       for(i=0;i<recordType11.size();i++)
       {   
           upNode = StringToInt(recordType11.get(i).substring(0,4));
           downNode = StringToInt(recordType11.get(i).substring(4,8));
           if(upNode>=8000 && downNode<8000)
               st1.execute("update nodeInfo set isOrigin=1 where node=" + upNode);
           else if(upNode<8000 && downNode>=8000)
               st1.execute("update nodeInfo set isDest=1 where node=" + downNode);
               
           linkLength = (double)StringToInt(recordType11.get(i).substring(8,12));
           linkLength *= 0.3048;
           if(linkLength == 0)
               linkLength = 300;
           noOfLanes = StringToInt(recordType11.get(i).substring(21,22));
           isOnRamp = 0;
           isOffRamp = 0;
           st1.execute("insert into linkInfo values(" + upNode + "," + downNode + "," +  linkLength 
                   + "," + noOfLanes + "," + isOnRamp + "," + isOffRamp + ",null,null)");
       }   
       
       for(i=0;i<recordType19.size();i++)
       {   
           upNode = StringToInt(recordType19.get(i).substring(0,4));
           downNode = StringToInt(recordType19.get(i).substring(4,8));
           
           if(upNode>=8000 && downNode<8000)
               st1.execute("update nodeInfo set isOrigin=1 where node=" + upNode);
           
           linkLength = (double)StringToInt(recordType19.get(i).substring(12,17));
           linkLength *= 0.3048;
           if(linkLength == 0)
               linkLength = 300;
           
           noOfLanes = StringToInt(recordType19.get(i).substring(19,20));
           
           if(StringToInt(recordType19.get(i).substring(20,22)) == 6 || 
                   StringToInt(recordType19.get(i).substring(20,22)) == 9)
               noOfLanes++;
           if(StringToInt(recordType19.get(i).substring(28,30)) == 7 ||
                   StringToInt(recordType19.get(i).substring(28,30)) == 10)
               noOfLanes++;
           if(StringToInt(recordType19.get(i).substring(36,38)) == 8 ||
                   StringToInt(recordType19.get(i).substring(36,38)) == 11 )
               noOfLanes++;

           isOnRamp = 0;
           isOffRamp = 0;
           if(StringToInt(recordType19.get(i).substring(17,18)) == 1)
              isOnRamp = 1;

           rs1 = st1.executeQuery("select count(*) from linkInfo where upNode=" + upNode + " and downNode=" + downNode);
           rs1.next();
           if(rs1.getInt(1)==0)
               st1.execute("insert into linkInfo values(" + upNode + "," + downNode + "," +  linkLength 
                   + "," + noOfLanes + "," + isOnRamp + "," + isOffRamp + ",100,0)");
           
       }
       
       for(i=0;i<recordType25.size();i++)
       {
           if(StringToInt(recordType25.get(i).substring(16,20)) > 0)
           {
               isOffRamp = 1;
               isOnRamp = 0;
           
               st1.execute("update linkInfo set isOnRamp=" + isOnRamp + ", isOffRamp=" + isOffRamp + 
                   ",percentVehiclesPerHour= " + StringToInt(recordType25.get(i).substring(12,16)) + 
                   ",percentVehiclesOnOffRamp= " + StringToInt(recordType25.get(i).substring(20,24)) + 
                   " where upNode=" + StringToInt(recordType25.get(i).substring(4,8)) + " and downNode=" + 
                   StringToInt(recordType25.get(i).substring(16,20)));
           }
       }
       
       for(i=0;i<recordType50.size();i++)
       {   
           upNode = StringToInt(recordType50.get(i).substring(0,4));
           downNode = StringToInt(recordType50.get(i).substring(4,8));
           linkLength = 300;
           noOfLanes = 3;
           isOnRamp = 0;
           isOffRamp = 0;
           
           rs1 = st1.executeQuery("select count(*) from linkInfo where upNode=" + upNode + " and downNode=" + downNode);
           rs1.next();
           if(rs1.getInt(1)==0)
               st1.execute("insert into linkInfo values(" + upNode + "," + downNode + "," +  linkLength 
                   + "," + noOfLanes + "," + isOnRamp + "," + isOffRamp + ",100,0)");
       }
       
       for(i=0;i<recordType19.size();i++)
       {   
           upNode = StringToInt(recordType19.get(i).substring(0,4));
           downNode = StringToInt(recordType19.get(i).substring(4,8));
           ddNode = StringToInt(recordType19.get(i).substring(8,12));
           if(ddNode > 0)
           {
               if(downNode>=8000 && ddNode<8000)
                   st1.execute("update nodeInfo set isOrigin=1 where node=" + downNode);
               else if(downNode<8000 && ddNode>=8000)
                   st1.execute("update nodeInfo set isDest=1 where node=" + ddNode);

               linkLength = 300;
               noOfLanes = StringToInt(recordType19.get(i).substring(19,20)); 
               isOnRamp = 0;
               isOffRamp = 0;

               rs1 = st1.executeQuery("select count(*) from linkInfo where upNode=" + downNode + " and downNode=" + ddNode);
               rs1.next();
               if(rs1.getInt(1)==0)
                   st1.execute("insert into linkInfo values(" + downNode + "," + ddNode + "," +  linkLength 
                       + "," + noOfLanes + "," + isOnRamp + "," + isOffRamp + ",null,null)");
           }
       }
       
       for(i=0;i<recordType11.size();i++)
       {   
           upNode = StringToInt(recordType11.get(i).substring(0,4));
           downNode = StringToInt(recordType11.get(i).substring(4,8));
           ddNode = StringToInt(recordType11.get(i).substring(40,44));
           if(ddNode > 0)
           {
               if(downNode>=8000 && ddNode<8000)
                   st1.execute("update nodeInfo set isOrigin=1 where node=" + downNode);
               else if(downNode<8000 && ddNode>=8000)
                   st1.execute("update nodeInfo set isDest=1 where node=" + ddNode);

               linkLength = 300;
               noOfLanes = StringToInt(recordType11.get(i).substring(21,22)); 
               isOnRamp = 0;
               isOffRamp = 0;

               rs1 = st1.executeQuery("select count(*) from linkInfo where upNode=" + downNode + " and downNode=" + ddNode);
               rs1.next();
               if(rs1.getInt(1)==0)
                   st1.execute("insert into linkInfo values(" + downNode + "," + ddNode + "," +  linkLength 
                       + "," + noOfLanes + "," + isOnRamp + "," + isOffRamp + ",null,null)");
           }
       }
       
       System.out.println("Stored ASCII data into database");
       st1.close();
       con.close();
       fis.close();
       bis.close();
       dis.close();

    }
    
}
