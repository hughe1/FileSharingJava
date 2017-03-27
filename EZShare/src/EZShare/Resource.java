package EZShare;

import java.util.ArrayList;

public class Resource extends JsonModel {
	public String name;
	public ArrayList<String> tags;
	public String description;
	public String uri; // use URI class here
	public String channel;
	public String owner;
	public String ezserver;
	public Integer resourceSize;
	
	@Override
	public JsonModel fromJson(String json) {
		return g.fromJson(json, Resource.class);
	}
}
