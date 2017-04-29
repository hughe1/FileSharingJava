package EZShare;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The JsonModel abstract class is intended to be extended by all classes which
 * requires conversion to/from JSON during communication (i.e. Command,
 * Response, Resource classes in the current project).
 * 
 * This abstract class provides the implementation of methods for converting
 * JsonModel objects to JSON string and an abstract method for converting from
 * JSON to a JsonModel object.
 * 
 * The JSON conversion is implemented with reference to the Gson library
 * ("gson-2.8.0.jar"), which is available from:
 * https://mvnrepository.com/artifact/com.google.code.gson/gson
 * 
 */

public abstract class JsonModel {

	protected transient Gson gPretty = new GsonBuilder().setPrettyPrinting().create();
	protected transient Gson g = new Gson();

	/**
	 * @return the calling object as a one line JSON String. Null fields are not
	 *         included in the JSON String.
	 */
	public String toJson() {
		return g.toJson(this);
	}

	/**
	 * @return the calling object as a pretty JSON String (formatted for
	 *         printing to console). Null fields are not included in the JSON
	 *         String.
	 */
	public String toJsonPretty() {
		return gPretty.toJson(this);
	}

	/**
	 * The fromJson method converts a JSON string to a JsonModel object.
	 * 
	 * @param json
	 *            A JSON string containing fields that correspond to the fields
	 *            declared in the calling subclass
	 * 
	 * @return an object of the calling subclass, where each field's value is
	 *         set to the corresponding field's value in the input JSON string
	 */
	public abstract JsonModel fromJson(String json);
}
