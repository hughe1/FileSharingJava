package EZShare;

import java.util.ArrayList;

/**
 * The Resource class represents the record of a resource OR a resource template 
 * which may be sent to a server.
 * 
 * The class implements hashCode and equals so that objects can be easily hashed 
 * and compared against other Resource objects - the tuple (channel, uri) is used 
 * as the PrimaryKey to identify a Resource object.
 */

public class Resource extends JsonModel{
	
	//Default values specified for optional fields in a resource template
	public static final String DEFAULT_NAME = "";
	public static final String DEFAULT_DESCRIPTION = "";
	public static final String DEFAULT_CHANNEL = "";
	public static final String DEFAULT_OWNER = "";
	public static final String DEFAULT_EZSERVER = ""; //TODO see below
	public static final String DEFAULT_URI = "";
	
	private String name;
	private ArrayList<String> tags;
	private String description;
	private String uri; // use URI class here
	private String channel;
	private String owner;
	private String ezserver;
	private Long resourceSize;

	/**
	 * Default constructor
	 */
	public Resource() {

	}

	/**
	 * Initialize a Resource object based on the client arguments - if an option is
	 * optional and not set in the client argument, the default value for that option
	 * is used in the construction.
	 * 
	 * @param clientArgs
	 * 				a list of client arguments parsed against EZShare server commands/
	 * 				options
	 */
	public Resource(ClientArgs clientArgs) {
		name = clientArgs.getOptionValue(ClientArgs.NAME_OPTION, DEFAULT_NAME);
		
		addTags(clientArgs.getOptionValue(ClientArgs.TAGS_OPTION));
		
		description = clientArgs.getOptionValue(ClientArgs.DESCRIPTION_OPTION, DEFAULT_DESCRIPTION);
		
		uri = clientArgs.getOptionValue(ClientArgs.URI_OPTION, DEFAULT_URI);	
		
		channel = clientArgs.getOptionValue(ClientArgs.CHANNEL_OPTION, DEFAULT_CHANNEL);		
		
		owner = clientArgs.getOptionValue(ClientArgs.OWNER_OPTION, DEFAULT_OWNER);	
		
		//TODO AZ the default server is not an empty string, but actually null!
		//need to enable parsing null fields when converting resource object to JSON!
		ezserver = clientArgs.getOptionValue(ClientArgs.EZSERVER_OPTION, DEFAULT_EZSERVER);
	}

	/**
	 * The addTags method adds all comma-delimited tags in the input string
	 * to the calling Resource object's tags list.
	 * 
	 * @param tags_string
	 *            a string of the form "tag1,tag2,tag3,..."
	 */
	public void addTags(String tags_string) {
		tags = new ArrayList<String>();
		
		//return default empty list if the tag option is simply not set
		if (tags == null)
			return;
		
		//add each tag delimited by "," to the tags list
		String[] tokens = tags_string.split(",");
		for (String token : tokens) {
			this.tags.add(token);
		}
	}
	/**
	 * The hasURI method checks if the object is has an URI
	 * 
	 * @return true if the calling object has a URI
	 */
	public boolean hasURI(){
		return uri == null || uri.isEmpty();
	}
	
	/*
	 * Getters for accessing instance variables
	 */
	public String getName() {
		return name;
	}

	public ArrayList<String> getTags() {
		return tags;
	}

	public String getDescription() {
		return description;
	}

	public String getURI() {
		return uri;
	}

	public String getChannel() {
		return channel;
	}

	public String getOwner() {
		return owner;
	}

	public String getEzserver() {
		return ezserver;
	}
	
	public Long getResourceSize() {
		return resourceSize;
	}


	/*
	 * Setters used by Server to alter isntance variable's values
	 */
	public void setOwner(String owner) {
		this.owner = owner;
	}
	
	public void setChannel(String channel) {
		this.channel = channel;
		
	}
	public void setEzserver(String ezserver) {
		this.ezserver = ezserver;
	}
	
	public void setResourceSize(Long resourceSize) {
		this.resourceSize = resourceSize;
	}

	
	@Override
	public Resource fromJson(String json) {
		return g.fromJson(json, Resource.class);
	}

	
	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((channel == null) ? 0 : channel.hashCode());
		result = PRIME * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}
	
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		//Safe to cast as Resource object now
		Resource other = (Resource) obj;
		if (channel == null) {
			if (other.channel != null)
				return false;
		} else if (!channel.equals(other.channel))
			return false;
		
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		
		//Matching channel and URI values
		return true;
	}

	
	/**
	 * The getSafeOwner method is used to obtain the owner from an incoming
	 * Resource objects (i.e. converted by fromJson method, where owner may
	 * be null).
	 * 
	 * @return "" if owner is null, otherwise return the owner
	 */
	public String getSafeOwner() {
		if (this.owner == null)
			return DEFAULT_OWNER;
		else
			return this.owner;
	}

	

}
