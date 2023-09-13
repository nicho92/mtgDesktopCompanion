package org.magic.servers.impl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.config.CoreAddressConfiguration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.message.impl.CoreMessage;
import org.apache.activemq.artemis.core.postoffice.RoutingStatus;
import org.apache.activemq.artemis.core.security.CheckType;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.security.SecurityAuth;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.apache.activemq.artemis.core.server.impl.ActiveMQServerImpl;
import org.apache.activemq.artemis.core.server.plugin.ActiveMQServerPlugin;
import org.apache.activemq.artemis.core.transaction.Transaction;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager;
import org.apache.commons.lang3.ArrayUtils;
import org.magic.api.beans.messages.TalkMessage;
import org.magic.api.exports.impl.JsonExport;
import org.magic.api.interfaces.MTGNetworkClient;
import org.magic.api.interfaces.abstracts.AbstractMTGServer;
import org.magic.api.network.impl.ActiveMQNetworkClient;
import org.magic.game.model.Player;
import org.magic.services.MTGConstants;
import org.magic.services.TechnicalServiceManager;
import org.magic.services.network.URLTools;
import org.magic.services.tools.BeanTools;

public class ActiveMQServer extends AbstractMTGServer {

	private static final String LISTENERS_TCP = "LISTENERS_TCP";
	private static final String LOG_DIR = "LOG_DIR";
	public static final String DEFAULT_ADDRESS = "welcome";
	private ActiveMQServerImpl server;
	private ActiveMQServerPlugin plug;
	private MTGNetworkClient client;
	
	public ActiveMQServer() {
		super();
		server = new ActiveMQServerImpl(new ConfigurationImpl());
		client = new ActiveMQNetworkClient();
		
	}
	
	@Override
	public Map<String, String> getDefaultAttributes() {
		var m = new HashMap<String,String>();
			 m.put("ENABLE_JMX_MNG", "true");
			 m.put(LISTENERS_TCP, "tcp://"+URLTools.getInternalIP()+":61616");
			 m.put("SECURITY_ENABLED", "false");
			 m.put(LOG_DIR, new File(MTGConstants.DATA_DIR,"activemq").getAbsolutePath());
			 m.put("ADRESSES", "trade,news");
			 m.put("RETENTION_DAYS", "7");
			 m.put("AUTOSTART", "false");
			 return m;
	}
	
	
	private void init() throws IOException 
	{
			try {
				
				for(int i=0;i<getArray(LISTENERS_TCP).length;i++)
					server.getConfiguration().addAcceptorConfiguration("tcp-"+i, getArray(LISTENERS_TCP)[i]);
				
				
				server.getConfiguration().setSecurityEnabled(getBoolean("SECURITY_ENABLED"));
				server.getConfiguration().setJMXManagementEnabled(getBoolean("ENABLE_JMX_MNG"));
				server.getConfiguration().setJournalRetentionPeriod(TimeUnit.DAYS, getInt("RETENTION_DAYS"));
				server.getConfiguration().setJournalDirectory(getString(LOG_DIR));
				server.getConfiguration().setPagingDirectory(getString(LOG_DIR));
				server.getConfiguration().setNodeManagerLockDirectory(getString(LOG_DIR));
				server.getConfiguration().setLargeMessagesDirectory(getString(LOG_DIR));
				server.getConfiguration().setBindingsDirectory(getString(LOG_DIR));
				
				
				for(String add : ArrayUtils.add(getArray("ADRESSES"),DEFAULT_ADDRESS))
				{
					var addr = new CoreAddressConfiguration();
					addr.setName(add);
					addr.addRoutingType(RoutingType.MULTICAST);
					server.getConfiguration().addAddressConfiguration(addr);
				}
				
				server.setSecurityManager(new ActiveMQSecurityManager() {
					@Override
					public boolean validateUserAndRole(String user, String password, Set<Role> roles, CheckType checkType) {
						return true;
					}
					@Override
					public boolean validateUser(String user, String password) {
						return true;
					}
				});
				
				
				
				plug = new ActiveMQServerPlugin() {
					
					JsonExport serializer = new JsonExport();
					Set<Player> onlines = new HashSet<>();
					Set<Queue> queues = new HashSet<>();
					
					
					public Set<Player> getOnlines() {
						return onlines;
					}
					
					public Set<Queue> getQueues() {
						return queues;
					}
					
					
					@Override
					public void afterCreateSession(ServerSession session) throws ActiveMQException {
						logger.info("new connection from user : {} with id {}", session.getUsername(), session.getRemotingConnection().getClientID());
					}
					
					@Override
					public void afterCloseSession(ServerSession session, boolean failed) throws ActiveMQException {
						logger.info("disconnection from user : {}", BeanTools.describe(session));
						onlines.removeIf(p->session.getRemotingConnection().getClientID().equals(p.getId().toString()));
					}
					
					
					private byte[] removeNullByte(byte[] input) {
				        int outputLength = (input.length + 1) / 2; 
				        byte[] output = new byte[outputLength];

				        for (int i = 0; i < outputLength; i++) {
				            output[i] = input[i * 2];  
				        }

				        return output;
				    }
					
					@Override
					public void afterSend(ServerSession session, Transaction tx, Message message, boolean direct,boolean noAutoCreateQueue, RoutingStatus result) throws ActiveMQException {
					
						var cmsg = ((CoreMessage)message);
						var databuff = cmsg.getDataBuffer();
						var size = databuff.readableBytes();
						var bytes = new byte[size];
						databuff.readBytes(bytes);
						var s = new String(removeNullByte(bytes));
						s = s.substring(s.indexOf("{"));
						var jmsg = serializer.fromJson(s, TalkMessage.class);
						
						
						onlines.add(jmsg.getAuthor());
						
						TechnicalServiceManager.inst().store(jmsg);
						logger.info("user {} : {}", session.getUsername(),jmsg.getMessage());		
						
					}
					
					@Override
					public void afterCreateQueue(Queue queue) throws ActiveMQException {
						logger.debug("Queue created {}",queue);
						queues.add(queue);
					}
					
					@Override
					public void afterDestroyQueue(Queue queue, SimpleString address, SecurityAuth session,boolean checkConsumerCount, boolean removeConsumers, boolean autoDeleteAddress) throws ActiveMQException {
						logger.debug("Queue deleted {}",queue);
						queues.remove(queue);
					}
					
				};
				
				server.registerBrokerPlugin(plug);
				
				
			} catch (Exception e) {
				throw new IOException(e);
			}
	}
	
	
	public ActiveMQServerPlugin getPlug() {
		return plug;
	}
	
	
	@Override
	public void start() throws IOException {
		try {
			init();
			server.start();
			client.join(new Player("Admin"), getArray(LISTENERS_TCP)[0], DEFAULT_ADDRESS);
			logger.info("{} is started", getName());
		} catch (Exception e) {
			throw new IOException(e);
		}

	}

	@Override
	public void stop() throws IOException {
		try {
			server.stop();
		} catch (Exception e) {
			throw new IOException(e);
		}

	}

	@Override
	public boolean isAlive() {
		return server.isStarted();
	}

	@Override
	public boolean isAutostart() {
		return getBoolean("AUTOSTART");
	}

	@Override
	public String description() {
		return "Messaging server";
	}

	@Override
	public String getVersion() {
		return server.getVersion().getFullVersion();
	}
	
	@Override
	public String getName() {
		return "ActiveMQ";
	}
	
}
