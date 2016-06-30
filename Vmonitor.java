/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package vMon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TelnetNotificationHandler;
import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.TerminalTypeOptionHandler;
import org.apache.commons.net.telnet.SuppressGAOptionHandler;
import org.apache.commons.net.telnet.InvalidTelnetOptionException;

import vMon.VCommand;
import vMon.RestInterface;

/***
	Vmonitor connects to the vcontrold via telnet
	It fetches all the availanle cmds and cyclic asks their values
	The values are sent to the openhab rest interface
 ***/
public class Vmonitor implements TelnetNotificationHandler
{
    static TelnetClient tc = null;
    static OutputStream mOutputStream = null;
    static String 		mLastCommand = new String();
    static List<VCommand> mCommands = new ArrayList<VCommand>();
    private static boolean mReconnect = false;
    private static int getAvailableCommands()
    {
    	mCommands.clear();
    	String result = sendCommand("commands",5000);
    	String lines[] = result.split("\\r?\\n");
    	
    	for (String command: lines)
    	{
    		if (command.contains("vctrld>"))
    			continue;
    		VCommand vc = new VCommand(command);
    		mCommands.add(vc);
    	}
    	
    	return mCommands.size();
    }
    
    private static String sendCommand(String theCommand, int theTimeout)
    {
    	theCommand += "\r\n";
    	String aCommand = new String(theCommand);
    	try {
    		mOutputStream.write(aCommand.getBytes(), 0 , aCommand.length());
    		mOutputStream.flush();
		} catch (IOException e) {
			mReconnect = true;
			System.err.println(e.getMessage());
		}
    	mLastCommand = theCommand;
    	try {
			Thread.sleep(theTimeout);
		} catch (InterruptedException e) {
			mReconnect = true;
			System.err.println(e.getMessage());
		} //sleep a while to let the vmonitor answer
    	return read();
    	
    }
    /***
     * Main for the Vmonitor.
     * @param args input params
     * @throws Exception on error
     ***/
    public static void main(String[] args) throws Exception
    {
        

        if(args.length < 3)
        {
            System.err.println("Usage: Vmonitor <vctrl remote-ip> <vctrl remote-port>"
            		+ "<openhab rest uri>");
            System.exit(1);
        }

        String remoteip = args[0];

        int remoteport;

        remoteport = (new Integer(args[1])).intValue();
    
        String openhabUri = args[2];

        tc = new TelnetClient();

        TerminalTypeOptionHandler ttopt = new TerminalTypeOptionHandler("VT100", false, false, true, false);
        EchoOptionHandler echoopt = new EchoOptionHandler(true, false, true, false);
        SuppressGAOptionHandler gaopt = new SuppressGAOptionHandler(true, true, true, true);

        try
        {
            tc.addOptionHandler(ttopt);
            tc.addOptionHandler(echoopt);
            tc.addOptionHandler(gaopt);
        }
        catch (InvalidTelnetOptionException e)
        {
            System.err.println("Error registering option handlers: " + e.getMessage());
        }

    	while (true)
        {
    		try
            {
                
	            tc.connect(remoteip, remoteport);
	            tc.registerNotifHandler(new Vmonitor());
	            System.out.println("Vmonitor connected");            
	            mReconnect = false;
	            mOutputStream = tc.getOutputStream();
	            sendCommand("",1000); //workaround: a bug in the telnet client adds some chars to the first cmd 
	            int count = getAvailableCommands();
	            System.out.println("Got " + count + " commands" );
	            String aResult = new String();
	            while (true)
	            {
	                for (VCommand vc :mCommands )
	                {
	                	aResult = sendCommand(vc.getmName(),5000);
	                	vc.setmValue(aResult);
	                	System.out.println(vc.getmName() + ": " + vc.getmValue());
	                	RestInterface.sendPostRequest(openhabUri,vc.getmName(),vc.getmValue());
	                }
	            	if (mReconnect) break;
	            	Thread.sleep(5000);
	            }//inner while loop
	            tc.disconnect();
            
            
	        }//try/catch
		        catch (ConnectException e)
		        {
		                System.err.println("Exception while connecting:" + e.getMessage());
		                mReconnect = true;
		        }
		        catch (IOException e)
		        {
		                System.err.println("Exception while connecting:" + e.getMessage());
		                mReconnect = true;
		        }
    		Thread.sleep(5000);
        }//outer while loop
        
    }


    /***
     * Callback method called when TelnetClient receives an option
     * negotiation command.
     *
     * @param negotiation_code - type of negotiation command received
     * (RECEIVED_DO, RECEIVED_DONT, RECEIVED_WILL, RECEIVED_WONT, RECEIVED_COMMAND)
     * @param option_code - code of the option negotiated
     ***/
//    @Override
    public void receivedNegotiation(int negotiation_code, int option_code)
    {
        String command = null;
        switch (negotiation_code) {
            case TelnetNotificationHandler.RECEIVED_DO:
                command = "DO";
                break;
            case TelnetNotificationHandler.RECEIVED_DONT:
                command = "DONT";
                break;
            case TelnetNotificationHandler.RECEIVED_WILL:
                command = "WILL";
                break;
            case TelnetNotificationHandler.RECEIVED_WONT:
                command = "WONT";
                break;
            case TelnetNotificationHandler.RECEIVED_COMMAND:
                command = "COMMAND";
                break;
            default:
                command = Integer.toString(negotiation_code); // Should not happen
                break;
        }
        System.out.println("Received " + command + " for option code " + option_code);
   }

    /***
     * Reader thread.
     * Reads lines from the TelnetClient and echoes them
     * on the screen.
     ***/
    static private String read()
    {
        InputStream instr = tc.getInputStream();
        BufferedReader  buf = new BufferedReader(new InputStreamReader (instr));
        try
        {
            char[] buffer = new char[1024];
            int ret_read = 0;
            String aReturnString = new String("");
            String aPartString = new String("");
            while (buf.ready())
            {
                ret_read = buf.read(buffer);
                
                if(ret_read > 0)
                {
                    aPartString =  new String(buffer,0,ret_read);
                    if (aPartString.contains("vctrld>"))
                    {
                    	aPartString = aPartString.replace("vctrld>", "");
                    }
                	aReturnString += aPartString; 
                }
            }
            //System.out.print(new String(aReturnString));
            return aReturnString;
        }
        catch (IOException e)
        {
            System.err.println("Exception while reading socket:" + e.getMessage());
        }

        try
        {
            tc.disconnect();
        }
        catch (IOException e)
        {
            System.err.println("Exception while closing telnet:" + e.getMessage());
        }
        return "";
    }

}

