package com.ccompass.netty.proxy;

import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.List;

public class SingletonChannel{
	
	private static List<Channel> sinkChannelList = null;
	
	public static Channel channel=null;
	
	private static int i=1;
    private SingletonChannel() {
    	
    }  
   
    public static List<Channel> getChannelInstance() {
       if (sinkChannelList == null) {  
    	   sinkChannelList = new ArrayList<Channel>();  
    	   System.out.println(i++);
       }  
       
     //  System.out.println("+++++++++++listsize+"+sinkChannelList.size());
       return sinkChannelList;  
    }

}
