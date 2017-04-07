package EZShare;

public class Response extends JsonModel {

	public String response;
	public String errorMessage;
	public Integer resultSize;
	
	@Override
	public Response fromJson(String json) {
		return g.fromJson(json, Response.class);
	}
	
	/**
	 * 
	 * @return
	 */
	public Response success() {
		this.response = Constants.success;
		return this;
	}
	
	/**
	 * 
	 * @param resultSize
	 * @return
	 */
	public Response size(int resultSize) {
		this.resultSize = resultSize;
		return this;
	}
	
	/**
	 * 
	 * @param msg
	 * @return
	 */
	public Response error(String msg) {
		this.response = Constants.error;
		this.errorMessage = msg;
		return this;
	}
}
