package PhotoForge.backend.exceptions;

public class ImageKitUploadException extends RuntimeException{
  public ImageKitUploadException(String message){
    super(message);
  }

  public ImageKitUploadException(String message, Throwable cause){
    super(message, cause);
  }
}
