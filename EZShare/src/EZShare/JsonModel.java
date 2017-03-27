package EZShare;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Intended to be extended by Command and Response classes. It abstracts away
 * common functionality to both sub classes. Mainly converting a class object
 * to a JSON string. Two options available, 1. to standard one line JSON and
 * for convenience options 2. toJsonPretty() for printing to the console.
 * 
 * gson-2.8.0.jar required in project. available from 
 * https://mvnrepository.com/artifact/com.google.code.gson/gson
 * 
 * @author Koteski, B
 */

public abstract class JsonModel {
	
	protected transient Gson gPretty = new GsonBuilder().setPrettyPrinting().create();
	protected transient Gson g = new Gson();
	
	/**
	 * @return this object as a one liner JSON String
	 */
	public String toJson() {
		return g.toJson(this);
	}
	
	/**
	 * @return this object as a pretty JSON String
	 */
	public String toJsonPretty() {
		return gPretty.toJson(this);
	}
	
	/**
	 * @param json String that has fields resembling the subclass
	 * @return an object of the class that extends JsonModel
	 */
	public abstract JsonModel fromJson(String json);
}
