package mpc.exception;

public interface ICleanMessage {
	String getCleanMessage();

	default String getMessage() {
		return getCleanMessage();
	}

}
