package main.java.de.voidtech.gerald.commands.info;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;

import main.java.de.voidtech.gerald.GlobalConstants;
import main.java.de.voidtech.gerald.annotations.Command;
import main.java.de.voidtech.gerald.commands.AbstractCommand;
import main.java.de.voidtech.gerald.commands.CommandCategory;
import main.java.de.voidtech.gerald.routines.AbstractRoutine;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

@Command
public class InfoCommand extends AbstractCommand {

	@Autowired
	private List<AbstractCommand> commands;
	
	@Autowired
	private List<AbstractRoutine> routines;
	
	@Autowired
	private SessionFactory sessionFactory;
	
	private long getEmoteCount(JDA jda) {
		try(Session session = sessionFactory.openSession())
		{
			@SuppressWarnings("rawtypes")
			Query query = session.createQuery("select count(*) from NitroliteEmote");
			long count = ((long)query.uniqueResult()) + jda.getEmoteCache().size();
			session.close();
			return count;
		}
	}
	
	private static final String JENKINS_LATEST_BUILD_URL = "https://jenkins.voidtech.de/job/Barista%20Gerald/lastSuccessfulBuild/buildNumber";
	
	private String getLatestBuild() {
		Document doc = null;
		try {
			doc = Jsoup.connect(JENKINS_LATEST_BUILD_URL).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return doc.select("body").text();
	}
	
	@Override
	public void executeInternal(Message message, List<String> args) {
		long guildCount = message.getJDA().getGuildCache().size();
		long memberCount = message.getJDA().getGuildCache().stream().mapToInt(Guild::getMemberCount).sum();
		long emoteCount = getEmoteCount(message.getJDA());
		
		MessageEmbed informationEmbed = new EmbedBuilder()
				.setColor(Color.ORANGE)
				.setTitle("Barista Gerald - A Java Discord Bot", GlobalConstants.LINKTREE_URL)
				.addField("Gerald Owner", "```ElementalMP4#7458```", false)
				.addField("Barista Gerald Developers", "```\n"
						+ "ElementalMP4#7458\r\n"
						+ "Montori#4707\r\n"
						+ "0xffset#2267\r\n"
						+ "Scot_Survivor#8625```", false)
				.addField("Gerald Guild Count", "```" + String.valueOf(guildCount) + "```", true)
				.addField("Gerald Member Count", "```" + String.valueOf(memberCount) + "```", true)
				.addField("Nitrolite Emote Count", "```" + String.valueOf(emoteCount) + "```", false)
				.addField("Latest Build Number", "```" + getLatestBuild() + "```", true)
				.addField("Active Threads", "```" + Thread.activeCount() + "```", true)
				.addField("Latest Release", "```"+ GlobalConstants.VERSION +"```", false)
				.setThumbnail(message.getJDA().getSelfUser().getAvatarUrl())
				.setFooter("Command Count: " + commands.size() + "\nRoutine Count: " + routines.size(), message.getJDA().getSelfUser().getAvatarUrl())
				.build();
		message.getChannel().sendMessageEmbeds(informationEmbed).queue();
	}

	@Override
	public String getDescription() {
		return "Provides information about the Barista Gerald project and the developers who made it!";
	}

	@Override
	public String getUsage() {
		return "info";
	}

	@Override
	public String getName() {
		return "info";
	}

	@Override
	public CommandCategory getCommandCategory() {
		return CommandCategory.INFO;
	}

	@Override
	public boolean isDMCapable() {
		return true;
	}

	@Override
	public boolean requiresArguments() {
		return false;
	}
	
	@Override
	public String[] getCommandAliases() {
		String[] aliases = {"botinfo", "botstats", "bi", "bs", "stats"};
		return aliases;
	}
	
	@Override
	public boolean canBeDisabled() {
		return true;
	}

}
