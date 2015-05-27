package com.ccompass.netty.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ccompass.netty.client.ClientInitializer;
import com.ccompass.netty.client.NettyClient;

public class ProxyFrontendHandler extends ChannelInboundHandlerAdapter {
	private static final Log log = LogFactory
			.getLog(ProxyFrontendHandler.class);

	// 心跳检查
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
			throws Exception {
		// TODO Auto-generated method stub

		Channel channel = ctx.channel();
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			if (event.state().equals(IdleState.READER_IDLE)) {
				channel.close();
				log.info("**********心跳检测读失败，强制关闭终端连接");
			} else if (event.state().equals(IdleState.WRITER_IDLE)) {
				channel.close();
				log.info("**********心跳检测写失败，强制关闭终端连接");
			} else if (event.state().equals(IdleState.ALL_IDLE)) {
				log.info("**********心跳检测，检测数据发送");
				// 发送心跳
				ctx.channel().write("ping\n");
			}
		}
		super.userEventTriggered(ctx, evt);
	}

	private final String remoteHost;
	private final int remotePort;
	private volatile Channel outboundChannel;
	private volatile List<Channel> sinkChannelList = new ArrayList<Channel>();

	public ProxyFrontendHandler(String remoteHost, int remotePort) {
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
		// 连接数量
		NettyClient.connects++;
	}

	// 连接服务器
	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		NettyClient.activeConnects++;
		final Channel inboundChannel = ctx.channel();
		// Start the connection attempt.
		createMainChannel(ctx, inboundChannel);
		int i=0;
		//循环创建从链路
		for(String item:ProxyConfig.config.branchList)
	    {
	       Channel channel=createSinkChannel(ctx, inboundChannel, item,i);
	       //讲各个从链路分别存放到各自的group中
	       NettyClient.sinkGroups.get(i).add(channel);
	       i++;
	    }
	}
	private void createMainChannel(ChannelHandlerContext ctx,
			final Channel inboundChannel) {
		// Start the connection attempt.
		Bootstrap b = new Bootstrap();
		b.group(inboundChannel.eventLoop()).channel(ctx.channel().getClass())
				.handler(new ProxyBackendHandler(inboundChannel))
				.option(ChannelOption.AUTO_READ, false);
		ChannelFuture f = b.connect(remoteHost, remotePort);
		outboundChannel = f.channel();
		NettyClient.group.add(outboundChannel);
		f.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) {
				if (future.isSuccess()) {
					// connection complete start to read first data
					inboundChannel.read();
				} else {
					// Close the connection if the connection attempt has
					// failed.
					inboundChannel.close();
				}
			}
		});
	}
	private Channel createSinkChannel(ChannelHandlerContext ctx,final Channel inboundChannel, String addressPort,int i) {
		// Start the connection attempt.       
        Channel sinkChannel=null;
        if (ProxyConfig.config.branchNumbers < 0) {
        	sinkChannel =createOneChannel(ctx, inboundChannel, addressPort);
        	sinkChannelList.add(sinkChannel);			
		} else if (ProxyConfig.config.branchNumbers > 0) {			
			if (NettyClient.getInstance().getSinkChannels().get(i).size() <= 0) {	
				sinkChannel=createOneChannel(ctx, inboundChannel,addressPort);
				NettyClient.getInstance().getSinkChannels().get(i).add(sinkChannel);
			} else {
				if (NettyClient.getInstance().getSinkChannels().get(i).size() < ProxyConfig.config.branchNumbers) {
					sinkChannel=createOneChannel(ctx, inboundChannel,addressPort);
					NettyClient.getInstance().getSinkChannels().get(i).add(sinkChannel);
				} else {
					// 每次链接检测从链接是否断开
					int j = 0;
					for (Channel channel : NettyClient.getInstance().getSinkChannels().get(i)) {
						if (channel == null || (!channel.isOpen())) {
							// log.info("*****创建链接，链接列表中如果有断开的，创建链接");
							NettyClient.getInstance().getSinkChannels().get(i).remove(j);
							sinkChannel = createOneChannel(ctx, inboundChannel,addressPort);
							NettyClient.getInstance().getSinkChannels().get(i).add(sinkChannel);
						}
						j++;
					}
				}
			}
		}        
        return sinkChannel;
	}
	/**
	 * 通过自己的客户端创建从链路
	 * @param ctx
	 * @param inboundChannel
	 * @param addressPort
	 * @param i
	 * @return
	 */
	private Channel createSinkChannelWithClient(ChannelHandlerContext ctx,final Channel inboundChannel, String addressPort,int i) {
		// Start the connection attempt.
        Channel sinkChannel=null;
        if (ProxyConfig.config.branchNumbers < 0) {
        	sinkChannel =createOneChannelWithClient(ctx, inboundChannel, addressPort);
        	sinkChannelList.add(sinkChannel);			
		} else if (ProxyConfig.config.branchNumbers > 0) {			
			if (NettyClient.getInstance().getSinkChannels().get(i).size() <= 0) {	
				sinkChannel=createOneChannelWithClient(ctx, inboundChannel,addressPort);
				NettyClient.getInstance().getSinkChannels().get(i).add(sinkChannel);
			} else {
				if (NettyClient.getInstance().getSinkChannels().get(i).size() < ProxyConfig.config.branchNumbers) {
					sinkChannel=createOneChannelWithClient(ctx, inboundChannel,addressPort);
					NettyClient.getInstance().getSinkChannels().get(i).add(sinkChannel);
				} else {
					// 每次链接检测从链接是否断开
					int j = 0;
					for (Channel channel : NettyClient.getInstance().getSinkChannels().get(i)) {
						if (channel == null || (!channel.isOpen())) {
							// log.info("*****创建链接，链接列表中如果有断开的，创建链接");
							NettyClient.getInstance().getSinkChannels().get(i).remove(j);
							sinkChannel = createOneChannelWithClient(ctx, inboundChannel,addressPort);
							NettyClient.getInstance().getSinkChannels().get(i).add(sinkChannel);
						}
						j++;
					}
				}
			}
		}        
        return sinkChannel;
	}
	// （从链路固定）创建一个链接
	private Channel createOneChannel(ChannelHandlerContext ctx,final Channel inboundChannel,String addressPort) {
		 Bootstrap b = new Bootstrap();
	     b.group(inboundChannel.eventLoop())
	         .channel(ctx.channel().getClass())
	         .handler(new ProxyBackendHandler(inboundChannel))
	         .option(ChannelOption.AUTO_READ, false);
	     int n = addressPort.indexOf(':');        
	     ChannelFuture f = b.connect(addressPort.substring(0, n), Integer.parseInt(addressPort.substring(n + 1)));
	     Channel sinkChannel = f.channel();
	     f.addListener(new ChannelFutureListener() {
	    	@Override
	        public void operationComplete(ChannelFuture future) throws Exception {
	    		if (future.isSuccess()) {
	                    // connection complete start to read first data
	             	inboundChannel.read();
	    		} else {
	                    // Close the connection if the connection attempt has failed.
	               	inboundChannel.close();
	            }
	         }
	     });
	     return sinkChannel;
	}
	private Channel createOneChannelWithClient(ChannelHandlerContext ctx,final Channel inboundChannel,String addressPort) {		
		EventLoopGroup group = new NioEventLoopGroup();
		Bootstrap b = new Bootstrap();
		b.group(group).channel(NioSocketChannel.class).handler(new ClientInitializer());
		b.option(ChannelOption.SO_KEEPALIVE, true);
        int n = addressPort.indexOf(':');        
        ChannelFuture f = b.connect(addressPort.substring(0, n), Integer.parseInt(addressPort.substring(n + 1)));
        Channel sinkChannel = f.channel();
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    // connection complete start to read first data
                    inboundChannel.read();
                } else {
                    // Close the connection if the connection attempt has failed.
                    inboundChannel.close();
                }
            }
        });
        return sinkChannel;
	}
	// 客户端发送数据
	@Override
	public void channelRead(final ChannelHandlerContext ctx, Object msg) {
		// 请求数量
		NettyClient.requests++;

		ByteBuf buf = (ByteBuf)msg;
    	List<ByteBuf> bufList = new ArrayList<ByteBuf>();
    	for(int i=0; i<ProxyConfig.config.branchList.size(); i++)
    	{
    		bufList.add(buf.copy());
    	}
		
		if (outboundChannel.isActive()&&outboundChannel.isOpen()) {
			outboundChannel.writeAndFlush(msg).addListener(
					new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture future) {
							if (future.isSuccess()) {
								ctx.channel().read();
							} else {
								future.channel().close();
							}
						}
					});
		}else{
			final Channel inboundChannel = ctx.channel();
			// Start the connection attempt.
			Bootstrap b = new Bootstrap();
			b.group(inboundChannel.eventLoop()).channel(ctx.channel().getClass())
					.handler(new ProxyBackendHandler(inboundChannel))
					.option(ChannelOption.AUTO_READ, false);
			ChannelFuture f = b.connect(remoteHost, remotePort);
			outboundChannel = f.channel();
			f.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) {
					if (future.isSuccess()) {
						// connection complete start to read first data
						inboundChannel.read();
					} else {
						// Close the connection if the connection attempt has
						// failed.
						inboundChannel.close();
					}
				}
			});			
			outboundChannel.writeAndFlush(msg).addListener(
					new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture future) {
							if (future.isSuccess()) {
								ctx.channel().read();
							} else {
								future.channel().close();
							}
					}
			});
			
		}
		final Channel inboundChannel = ctx.channel();
		// 从连接发送数据
		if (msg != null) {
			if (ProxyConfig.config.branchNumbers < 0) {
				try {
					for(int i=0; i<sinkChannelList.size(); i++)
			        {
			        	Channel ch = sinkChannelList.get(i);
				        if (ch.isActive()) {
				        	ch.writeAndFlush(bufList.get(i)).addListener(new ChannelFutureListener() {
				                @Override
				                public void operationComplete(ChannelFuture future) throws Exception {
				                    if (future.isSuccess()) {
				                        // was able to flush out data, start to read the next chunk
				                        ctx.channel().read();
				                    } else {
				                        future.channel().close();
				                    }
				                }
				            });
				        }else{
				        	sinkChannelList.remove(i);
				        	String channelStr=ProxyConfig.config.branchList.get(i);
				        	ch=createSinkChannel(ctx, inboundChannel, channelStr,i);
				        	//讲各个从链路分别存放到各自的group中
				        	NettyClient.sinkGroups.get(i).add(ch); 
				        	ch.writeAndFlush(bufList.get(i)).addListener(new ChannelFutureListener() {
				                @Override
				                public void operationComplete(ChannelFuture future) throws Exception {
				                    if (future.isSuccess()) {
				                        // was able to flush out data, start to read the next chunk
				                        ctx.channel().read();
				                    } else {
				                        future.channel().close();
				                    }
				                }
				            });
				        }
			        }
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (ProxyConfig.config.branchNumbers > 0) {
				for(int i=0; i<ProxyConfig.config.branchList.size(); i++)
		    	{
					if (NettyClient.getInstance().getSinkChannels().get(i).size() <= 0) {
						String channelStr=ProxyConfig.config.branchList.get(i);
						Channel channel = createOneChannel(ctx, inboundChannel,channelStr);						
						NettyClient.getInstance().getSinkChannels().get(i).add(channel);						
						try {
							channel.writeAndFlush(bufList.get(i)).sync();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						// 随机取一个链路
						int theChannel = new Random().nextInt(NettyClient.getInstance().getSinkChannels().get(i).size());
						// log.info("*****读数据，链接数："+
						// NettyClient.getInstance().getChannels().size());
						Channel channel = NettyClient.getInstance().getSinkChannels().get(i).get(theChannel);
						if (channel == null || (!channel.isOpen())|| (!channel.isActive())) {
							// log.info("*****读数据，链接为空，或者关闭，打开链接");
							NettyClient.getInstance().getSinkChannels().get(i).remove(theChannel);
							String channelStr=ProxyConfig.config.branchList.get(i);
							channel = createOneChannel(ctx, inboundChannel,channelStr);
							NettyClient.getInstance().getSinkChannels().get(i).add(channel);
						}
						try {
							channel.writeAndFlush(bufList.get(i)).sync();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
		    	}
			}
		}
	}

	// 中断的连接
	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		NettyClient.inactiveConnects++;
		closeOnFlush(ctx.channel());
		if (outboundChannel!=null) {
			closeOnFlush(outboundChannel);
		}
		// 主从链路一对一
		if (ProxyConfig.config.branchNumbers < 0) {
			for(Channel channel:sinkChannelList){
				if (channel != null) {
					closeOnFlush(channel);
				}
			}
		}
	}

	// 出异常的连接
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		NettyClient.exceptions++;
		// cause.printStackTrace();//日志打印
		log.info("**********链路异常，异常信息：" + cause.getMessage());
		/*closeOnFlush(ctx.channel());
		// 出异常关闭主从链接
		if (outboundChannel != null) {
			closeOnFlush(outboundChannel);
		}*/
		// 主从链路一对一
		if (ProxyConfig.config.branchNumbers < 0) {
			for(Channel channel:sinkChannelList){
				if (channel != null) {
					closeOnFlush(channel);
				}
			}
		}
	}

	/**
	 * Closes the specified channel after all queued write requests are flushed.
	 */
	static void closeOnFlush(Channel ch) {
		if (ch.isActive()) {
			ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(
					ChannelFutureListener.CLOSE);
		}
	}
}