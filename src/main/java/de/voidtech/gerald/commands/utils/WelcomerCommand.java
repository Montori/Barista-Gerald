package main.java.de.voidtech.gerald.commands.utils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import main.java.de.voidtech.gerald.annotations.Command;
import main.java.de.voidtech.gerald.commands.AbstractCommand;
import main.java.de.voidtech.gerald.commands.CommandCategory;
import main.java.de.voidtech.gerald.entities.JoinLeaveMessage;
import main.java.de.voidtech.gerald.entities.Server;
import main.java.de.voidtech.gerald.service.ServerService;
import main.java.de.voidtech.gerald.util.MRESameUserPredicate;
import main.java.de.voidtech.gerald.util.ParsingUtils;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@Command
public class WelcomerCommand extends AbstractCommand{

	@Autowired
	private ServerService serverService;
	
	@Autowired
	private SessionFactory sessionFactory;
	
	@Autowired
	private EventWaiter waiter;

	private boolean customMessageEnabled(long guildID) {
		try(Session session = sessionFactory.openSession())
		{
			JoinLeaveMessage joinLeaveMessage = (JoinLeaveMessage) session.createQuery("FROM JoinLeaveMessage WHERE ServerID = :serverID")
                    .setParameter("serverID", guildID)
                    .uniqueResult();
			return joinLeaveMessage != null;
		}
	}
	
	private void deleteCustomMessage(long guildID) {
		try(Session session = sessionFactory.openSession())
		{
			session.getTransaction().begin();
			session.createQuery("DELETE FROM JoinLeaveMessage WHERE ServerID = :guildID")
				.setParameter("guildID", guildID)
				.executeUpdate();
			session.getTransaction().commit();
		}
	}
	
	private boolean channelExists (String channel, Message message) {
		if (ParsingUtils.isInteger(channel)) {
			GuildChannel guildChannel = message.getJDA().getGuildChannelById(Long.parseLong(channel));
			return guildChannel != null;	
		} else {
			return false;
		}
	}
	
	private void addJoinLeaveMessage(long serverID, String channel, String joinMessage, String leaveMessage) {
		try (Session session = sessionFactory.openSession()) {
			session.getTransaction().begin();
		
			JoinLeaveMessage joinLeaveMessage = new JoinLeaveMessage(serverID, channel, joinMessage, leaveMessage);
			
			session.saveOrUpdate(joinLeaveMessage);
			session.getTransaction().commit();
		}
		
	}
	
	private JoinLeaveMessage getJoinLeaveMessageEntity(long guildID) {
		try(Session session = sessionFactory.openSession())
		{
			JoinLeaveMessage joinLeaveMessage = (JoinLeaveMessage) session.createQuery("FROM JoinLeaveMessage WHERE ServerID = :serverID")
                    .setParameter("serverID", guildID)
                    .uniqueResult();
			return joinLeaveMessage;
		}
	}
	
	private void updateChannel(long serverID, String channel, Message message) {
		JoinLeaveMessage joinLeaveMessage = getJoinLeaveMessageEntity(serverID);
		
		try (Session session = sessionFactory.openSession()) {
			session.getTransaction().begin();

			joinLeaveMessage.setChannelID(channel);
			
			session.saveOrUpdate(joinLeaveMessage);
			session.getTransaction().commit();
		}
	}
	
	private void updateJoinMessage(long serverID, String joinMessage, Message message) {
		JoinLeaveMessage joinLeaveMessage = getJoinLeaveMessageEntity(serverID);
		
		try (Session session = sessionFactory.openSession()) {
			session.getTransaction().begin();

			joinLeaveMessage.setJoinMessage(joinMessage);
			
			session.saveOrUpdate(joinLeaveMessage);
			session.getTransaction().commit();
		}
	}
	
	private void updateLeaveMessage(long serverID, String leaveMessage, Message message) {
		JoinLeaveMessage joinLeaveMessage = getJoinLeaveMessageEntity(serverID);
		
		try (Session session = sessionFactory.openSession()) {
			session.getTransaction().begin();

			joinLeaveMessage.setLeaveMessage(leaveMessage);
			
			session.saveOrUpdate(joinLeaveMessage);
			session.getTransaction().commit();
		}
	}
	
	private void clearWelcomer(Server server, Message message) {
		if (customMessageEnabled(server.getId())) {
			deleteCustomMessage(server.getId());
			message.getChannel().sendMessage("**The Welcomer has been disabled.**").queue();
		} else {
			message.getChannel().sendMessage("**The Welcomer has not been set up yet!**").queue();
		}
	}
	
	private void continueToLeaveMessage(Message message, Server server, String channel, String welcomeMessage) {
		message.getChannel().sendMessage("**Please enter your leave message:**").queue();
		waiter.waitForEvent(MessageReceivedEvent.class,
				new MRESameUserPredicate(message.getAuthor()),
				leaveMessageEvent -> {
					String leaveMessage = leaveMessageEvent.getMessage().getContentRaw();
					
					addJoinLeaveMessage(server.getId(), channel, welcomeMessage, leaveMessage);
					message.getChannel().sendMessage("**The Welcomer has been set up!**\n\n"
							+ "Channel: <#" + channel + ">\n"
							+ "Join message: " + welcomeMessage + "\n"
							+ "Leave message: " + leaveMessage).queue();
					
				}, 60, TimeUnit.SECONDS, 
				() -> message.getChannel().sendMessage("**No input has been supplied, cancelling.**").queue());	
	}
	
	
	private void continueToWelcomeMessage(Message message, Server server, String channel) {
		message.getChannel().sendMessage("**Please enter your welcome message:**").queue();
		waiter.waitForEvent(MessageReceivedEvent.class,
				new MRESameUserPredicate(message.getAuthor()),
				welcomeMessageInputEvent -> {
					String welcomeMessage = welcomeMessageInputEvent.getMessage().getContentRaw();
					continueToLeaveMessage(message, server, channel, welcomeMessage);
				}, 60, TimeUnit.SECONDS, 
				() -> message.getChannel().sendMessage("**No input has been supplied, cancelling.**").queue());	
	}
	
	private void beginSetup(Message message, Server server) {
		message.getChannel().sendMessage("**Enter the ID or a mention of the channel you wish to use:**").queue();
		
		waiter.waitForEvent(MessageReceivedEvent.class,
				new MRESameUserPredicate(message.getAuthor()),
				channelEntryEvent -> {
					String channel = ParsingUtils.filterSnowflake(channelEntryEvent.getMessage().getContentRaw());
					
					if (channelExists(channel, message)) {
						continueToWelcomeMessage(message, server, channel);
					} else {
						message.getChannel().sendMessage("**You need to mention a channel or use its ID!**").queue();
					}
					
				}, 60, TimeUnit.SECONDS, 
				() -> message.getChannel().sendMessage("**No input has been supplied, cancelling.**").queue());	
	}
	
	private void setupWelcomer(Server server, Message message) {
		if (customMessageEnabled(server.getId())) {
			message.getChannel().sendMessage("**The Welcomer is already set up!**").queue();
		} else {
			beginSetup(message, server);
		}	
	}
	
	private void changeChannel(Server server, Message message, List<String> args) {
		if (customMessageEnabled(server.getId())) {
			String channel = ParsingUtils.filterSnowflake(args.get(1));
			
			if (channelExists(channel, message)) {
				updateChannel(server.getId(), channel, message);
				message.getChannel().sendMessage("**The channel has been changed to** <#" + channel + ">").queue();
			} else {
				message.getChannel().sendMessage("**You need to mention a channel or use its ID!**").queue();
			}
		} else {
			message.getChannel().sendMessage("**The Welcomer has not been set up yet! See below:\n\n**" + this.getUsage()).queue();
		}
	}
	
	private void changeWelcomeMessage(Server server, Message message, List<String> args) {
		if (customMessageEnabled(server.getId())) {
			
			String joinMessage = "";
			
			for (int i = 1; i < args.size(); i++) {
				joinMessage = joinMessage + args.get(i);
			}
		
			updateJoinMessage(server.getId(), joinMessage, message);
			message.getChannel().sendMessage("**The join message has been changed to** " + joinMessage).queue();

		} else {
			message.getChannel().sendMessage("**The Welcomer has not been set up yet! See below:\n\n**" + this.getUsage()).queue();
		}
	}
	
	private void changeLeaveMessage(Server server, Message message, List<String> args) {
		if (customMessageEnabled(server.getId())) {
			
			String leaveMessage = "";
			
			for (int i = 1; i < args.size(); i++) {
				leaveMessage = leaveMessage + args.get(i);
			}
			
			updateLeaveMessage(server.getId(), leaveMessage, message);
			message.getChannel().sendMessage("**The leave message has been changed to** " + leaveMessage).queue();

		} else {
			message.getChannel().sendMessage("**The Welcomer has not been set up yet! See below:\n\n**" + this.getUsage()).queue();
		}
	}
	
	@Override
	public void executeInternal(Message message, List<String> args) {
		
		Server server = serverService.getServer(message.getGuild().getId());
		switch(args.get(0)) {
		case "clear":
			clearWelcomer(server, message);
			break;
		
		case "setup":
			setupWelcomer(server, message);
			break;
		
		case "channel":
			changeChannel(server, message, args);
			break;
		
		case "joinmsg":
			changeWelcomeMessage(server, message, args);
			break;
		
		case "leavemsg":
			changeLeaveMessage(server, message, args);
			break;
		}
		
	}

	@Override
	public String getDescription() {
		return "This command allows you to set up a customiseable join/leave message system. Simply choose a channel, join message and leave message and you're ready! Note: You MUST seperate the arguments for this command with a dash (-) See usage for details";
	}

	@Override
	public String getUsage() {
		return "welcomer setup (then follow the steps you are shown)\n\n"
				+ "welcomer channel #welcome-new-members (to change the channel)\n\n"
				+ "welcomer joinmsg welcome to our server! (to change the welcome message)\n\n"
				+ "welcomer leavemsg we will miss you! (to change the leave message)\n\n"
				+ "welcomer clear";
	}

	@Override
	public String getName() {
		return "welcomer";
	}

	@Override
	public CommandCategory getCommandCategory() {
		return CommandCategory.UTILS;
	}

	@Override
	public boolean isDMCapable() {
		return false;
	}

	@Override
	public boolean requiresArguments() {
		return true;
	}
	
	@Override
	public String[] getCommandAliases() {
		String[] aliases = {"jm", "joinmessage"};
		return aliases;
	}
	
	@Override
	public boolean canBeDisabled() {
		return true;
	}

}