package EZShare;

import java.util.ArrayList;

/**
 * The class models the Resource json object as specified in the assignment. the
 * class implements hashCode and equals so that objects can be easily
 * hashed/compared when comparing against other Resource objects. Here the tuple
 * (channel,uri) is used as a PrimaryKey to identify the object.
 * 
 * Class additionally extends JsonModel for for easy to/from JSON functionality.
 * 
 * @author Koteski, B
 */

public class Resource extends JsonModel {
	public String name;
	public ArrayList<String> tags;
	public String description;
	public String uri; // use URI class here
	public String channel;
	public String owner;
	public String ezserver;
	public int resourceSize;

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
		String name = clientArgs.getOptionValue(Constants.nameOption);		
		this.name = name == null ? Constants.emptyString : name;

		this.addTags(clientArgs.getOptionValue(Constants.tagsOption));
		
		String description = clientArgs.getOptionValue(Constants.descriptionOption);		
		this.description = description == null ? Constants.emptyString : description;
		
		String uri = clientArgs.getOptionValue(Constants.uriOption);		
		this.uri = uri == null ? Constants.emptyString : uri;
		
		String channel = clientArgs.getOptionValue(Constants.channelOption);		
		this.channel = channel == null ? Constants.emptyString : channel;
		
		String owner = clientArgs.getOptionValue(Constants.ownerOption);		
		this.owner = owner == null ? Constants.emptyString : owner;
		
		String ezserver = clientArgs.getOptionValue(Constants.ezserverOption);		
		this.ezserver = ezserver == null ? Constants.emptyString : ezserver;
	}

	/**
	 * 
	 */
	@Override
	public Resource fromJson(String json) {
		return g.fromJson(json, Resource.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((channel == null) ? 0 : channel.hashCode());
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
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
		return true;
	}

	/**
	 * This method spits tags with delimiter "," and loops through each
	 * resulting token and adds it to the tags list if tokens exist.
	 * 
	 * @param tags
	 *            has form tag1,tag2,tag3,...
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
			return Constants.emptyString;
		else
			return this.owner;
	}

}
