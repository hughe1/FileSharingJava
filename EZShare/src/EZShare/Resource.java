package EZShare;

import java.util.ArrayList;

/**
 * The class models the Resource json object as specified in the assignment.
 * the class implements hashCode and equals so that objects can be easily
 * hashed/compared when comparing against other Resource objects. Here the tuple
 * (owner,channel,uri) is used as a PrimaryKey to identify the object.
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
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
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
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
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

}
