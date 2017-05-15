package Test;

import java.util.HashMap;

import EZShare.Resource;

public class HashTest {

	public static void main(String[] args) {
		HashMap<Resource,String> map = new HashMap<>();
		map.put(new Resource().fromJson("{\"uri\":\"koteski\"}"), "whatever");
		map.put(new Resource().fromJson("{\"uri\":\"koteski\"}"), "whatever");
		map.put(new Resource().fromJson("{\"name\":\"koteski2\"}"), "whatever");
		map.put(new Resource().fromJson("{\"name\":\"koteski\"}"), "whatever");
		map.put(new Resource().fromJson("{\"uri\":\"google.com\"}"), "whatever");
		map.put(new Resource().fromJson("{\"uri\":\"google.com\", \"channel\":\"myself\"}"), "whatever");
		map.put(new Resource().fromJson("{\"uri\":\"google.com\", \"channel\":\"myself2\"}"), "whatever");
		map.put(new Resource().fromJson("{\"uri\":\"google.com\", \"channel\":\"myself2\",\"owner\":\"myself\"}"), "whatever");
		
		for (Resource r : map.keySet()) {
			System.out.println(r.toJson() + " " + map.get(r));
		}
	}

}
