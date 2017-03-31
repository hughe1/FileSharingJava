package EZShare;

public class Response extends JsonModel {

	public String response;
	public String errorMessage;
	public Integer resultSize;

	@Override
	public Response fromJson(String json) {
		return g.fromJson(json, Response.class);
	}
}
