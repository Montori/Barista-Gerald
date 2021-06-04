package main.java.de.voidtech.gerald.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "StarboardMessage")

public class StarboardMessage {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	//TODO: REVIEW should be unique
	@Column
	private String originMessageID;
	
	//TODO: REVIEW should this be unique?
	@Column
	private String selfMessageID;
	
	@Column
	private long serverID;
	
	@Deprecated
	//ONLY FOR HIBERNATE, DO NOT USE
	StarboardMessage() {
	}
	
	public StarboardMessage(String originMessageID, String selfMessageID, long serverID)
	{
		this.originMessageID = originMessageID;
		this.selfMessageID = selfMessageID;
		this.serverID = serverID;
	}
	
	public String getOriginMessageID() {
		return originMessageID;
	}
	
	public void setOriginMessageID(String ID) {
		this.originMessageID = ID;
	}
	
	public String getSelfMessageID() {
		return selfMessageID;
	}
	
	public void setSelfMessageID(String ID) {
		this.selfMessageID = ID;
	}
	
	public long getServerID() {
		return serverID;
	}
	
	public void setServerID(long ID) {
		this.serverID = ID;
	}	
}