package EZShare;

import java.util.ArrayList;

/**
 * The class models the Resource json object as specified in the assignment.
 * the class implements hashCode and equals so that objects can be easily
 * hashed/compared when comparing against other Resource objects. Here the tuple
 * (channel,uri) is used as a PrimaryKey to identify the object.
 * 
 * Class additionally extends JsonModel for for easy to/from JSON functionality.
 * 
 * @author Koteski, B
 */

public class Resource extends JsonModel {
	public String name;
	private ArrayList<String> tags;
	public String description;
	public String uri; // use URI class here
	public String channel;
	public String owner;
	public String ezserver;
	public Integer resourceSize;
	
	/**
	 * Default constructor
	 */
	public Resource() {
		
	}
	
	/**
	 * Convenient constructor for Client that will make a resource based on
	 * the user inputs.
	 * 
	 * @param clientArgs
	 */
	public Resource(ClientArgs clientArgs) {
		this.name = clientArgs.getOptionValue("name");
		this.addTags(clientArgs.getOptionValue("tags"));
		this.description = clientArgs.getOptionValue("description");
		this.uri = clientArgs.getOptionValue("uri");
		this.channel = clientArgs.getOptionValue("channel");
		this.owner = clientArgs.getOptionValue("owner");
		this.ezserver = clientArgs.getOptionValue("ezserver");
	}
	
	/**
	 * 
	 */
	@Override
	public Resource fromJson(String json) {
		return g.fromJson(json, Resource.class);
	}

	
	/* (non-Javadoc)
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

	/* (non-Javadoc)
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
	 * @param tags has form tag1,tag2,tag3,...
	 */
	public void addTags(String tags) {
		if(tags == null) return;
		String[] tokens = tags.split(",");
		this.tags = new ArrayList<String>();
		for (String token : tokens) {
			this.tags.add(token);
		}
	}
	
	/**
	 * Convenience method for Server. Can call this to easily compare 
	 * incoming resource objects from client.
	 * 
	 * @return "" if owner is null, otherwise return the owner
	 */
	public String getSafeOwner() {
		if (this.owner==null) return "";
		else return this.owner;
	}

}
