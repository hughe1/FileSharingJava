import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class EZShare {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		// TODO
		JSONObject obj = new JSONObject();
		obj.put("hello", "bob");
		System.out.println(obj);
		
		JSONParser parser = new JSONParser();
		String s = "{\"hello\":\"bob\"}";
		try {
			obj = (JSONObject) parser.parse(s);
			System.out.println(obj);
			System.out.println(obj.get("hello5"));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
