 /*
  * Copyright 2012 The Netty Project
  *
  * The Netty Project licenses this file to you under the Apache License,
  * version 2.0 (the "License"); you may not use this file except in compliance
  * with the License. You may obtain a copy of the License at:
  *
  *   http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  * License for the specific language governing permissions and limitations
  * under the License.
  */
 package com.ccompass.netty.proxy;
 
 import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import com.ccompass.netty.client.CheckSinkChannel;
import com.ccompass.netty.client.NettyClient;
 
 public final class Proxy { 
     public static void main(String[] args) throws Exception {
         ProxyConfig.loadConfig();
         EventLoopGroup bossGroup = new NioEventLoopGroup(1);
         EventLoopGroup workerGroup = new NioEventLoopGroup();         
         checkOtherChannel(ProxyConfig.config.checktimes);
         //初始化从链路grops
         for(int i=0;i<ProxyConfig.config.branchList.size();i++){
        	 ChannelGroup group=new DefaultChannelGroup("server-group", null);
        	 NettyClient.sinkGroups.add(group);
         }
         //如果重联路限定，初始化从链路对象存储列表
         if(ProxyConfig.config.branchNumbers > 0){
        	 List<List<Channel>> list=new ArrayList();
        	 for(int i=0;i<ProxyConfig.config.branchList.size();i++){
        		 List<Channel> channels=new ArrayList<Channel>();
        		 list.add(channels);
        	 }
        	 NettyClient.setSinkChannels(list);
         }
         try {
             ServerBootstrap b = new ServerBootstrap();
             b.group(bossGroup, workerGroup)
              .channel(NioServerSocketChannel.class)
          //    .handler(new LoggingHandler(LogLevel.INFO))
              .childHandler(new ProxyInitializer(ProxyConfig.config.mainIP, ProxyConfig.config.mainPort))
              .childOption(ChannelOption.AUTO_READ, false)
              .bind(ProxyConfig.config.proxyPort).sync().channel().closeFuture().sync();
         } finally {
             bossGroup.shutdownGracefully();
             workerGroup.shutdownGracefully();
         }
     }
     
     
     public static void checkOtherChannel(long period){
     	Timer timer = new Timer();
 		long delay = 1 * 1000;
 		String sinkChannel=ProxyConfig.config.branchList.get(0);
 		String[] sinkCh=sinkChannel.split(":");
 		//创建从连接
 		/*NettyClient client=NettyClient.getInstance();
     	client.setHost(sinkCh[0]);
     	client.setPort(Integer.parseInt(sinkCh[1]));
 		client.connect();*/
 		//每一段时间检查从连接是否连接
 		timer.schedule(new CheckSinkChannel(sinkCh[0],Integer.parseInt(sinkCh[1])), delay, period);
     }
 }