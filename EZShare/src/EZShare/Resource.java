package EZShare;

import java.util.ArrayList;

/**
 * The class models the Resource json object as specified in the assignment. the
 * class implements hashCode and equals so that objects can be easily hashed 
 * when comparing against other Resource objects. Here the tuple (channel,uri) is used as a PrimaryKey to identify the object.
 */

public class Resource extends JsonModel{
	public static final String DEFAULT_STRING = "";
			
	public String name;
	public ArrayList<String> tags;
	public String description;
	public String uri; // use URI class here
	public String channel;
	public String owner;
	public String ezserver;
	public Long resourceSize;

	/**
	 * Default constructor
	 */
	public Resource() {

	}

	/**
	 * Convenient constructor for Client that will make a resource based on the
	 * user inputs.
	 * 
	 * @param clientArgs
	 */
	public Resource(ClientArgs clientArgs) {
		String name = clientArgs.getOptionValue(Command.NAME_OPTION);		
		this.name = name == null ? DEFAULT_STRING : name;

		this.addTags(clientArgs.getOptionValue(Command.TAGS_OPTION));
		
		String description = clientArgs.getOptionValue(Command.DESCRIPTION_OPTION);		
		this.description = description == null ? DEFAULT_STRING : description;
		
		String uri = clientArgs.getOptionValue(Command.URI_OPTION);		
		this.uri = uri == null ? DEFAULT_STRING : uri;
		
		String channel = clientArgs.getOptionValue(Command.CHANNEL_OPTION);		
		this.channel = channel == null ? DEFAULT_STRING : channel;
		
		String owner = clientArgs.getOptionValue(Command.OWNER_OPTION);		
		this.owner = owner == null ? DEFAULT_STRING : owner;
		
		String ezserver = clientArgs.getOptionValue(Command.EZSERVER_OPTION);		
		this.ezserver = ezserver == null ? DEFAULT_STRING : ezserver;
	}

	
	@Override
	public Resource fromJson(String json) {
		return g.fromJson(json, Resource.class);
	}

	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((channel == null) ? 0 : channel.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
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
	 * This method spits tags with delimiter "," and loops through each
	 * resulting token and adds it to the tags list if tokens exist.
	 * 
	 * @param tags
	 *            has the form tag1,tag2,tag3,...
	 */
	public void addTags(String tags) {
		this.tags = new ArrayList<String>();
		if (tags == null)
			return;
		String[] tokens = tags.split(",");
		for (String token : tokens) {
			this.tags.add(token);
		}
	}

	/**
	 * Convenience method for Server. Can call this to easily compare incoming
	 * resource objects from client.
	 * 
	 * @return "" if owner is null, otherwise return the owner
	 */
	public String getSafeOwner() {
		if (this.owner == null)
			return DEFAULT_STRING;
		else
			return this.owner;
	}

}
