package EZShare;

public class Response extends JsonModel {
	
	public String reponse;
	public String errorMessage;
	public Integer resultSize;
	
	@Override
	public JsonModel fromJson(String json) {
		return this.g.fromJson(json, Response.class);
	}
}
