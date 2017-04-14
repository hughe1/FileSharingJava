package EZShare;
/**
 * The Response class represents a message that may be sent by a server.
 * 
 * Only three formats of response message are possible - success, error or result-size.
 * Fields can only be populated using the setter methods to ensure correct formating.
 * 
 * Note:
 * Upon construction, all fields are set to null. Null fields are NOT part of the 
 * message and will be disregarded when converting to JSON.
 */
public class Response extends JsonModel {
	
	/* Defined response texts */
	public static final String SUCCESS_TEXT = "success";
	public static final String ERROR_TEXT = "error";
	
	private String response;
	private String errorMessage;
	private Integer resultSize;
	
	/*
	 * Setters for success, error and result-size messages. This is the preferred
	 * approach as it is more readable than an overloaded constructor.
	 */
	public void setToSuccess() {
		this.response = SUCCESS_TEXT;
		this.errorMessage = null;
		this.resultSize = null;
	}

	public void setToError(String errorMessage) {
		this.response = ERROR_TEXT;
		this.errorMessage = errorMessage;
		this.resultSize = null;
	}

	public void setToResultSize(int resultSize) {
		this.response = null;
		this.errorMessage = null;
		this.resultSize = resultSize;
	}
	
	/* Getters for response fields. */
	
	public String getErrorMessage() {
		return errorMessage;
	}
	
	public Integer getResultSize() {
		return resultSize;
	}
	
	/**
	 * Check if the response is a success response.
	 * @return true if the response is a success
	 */
	public boolean isSuccess(){
		return response.equals(SUCCESS_TEXT);
	}
	
	@Override
	public Response fromJson(String json) {
		return g.fromJson(json, Response.class);
	}
}
