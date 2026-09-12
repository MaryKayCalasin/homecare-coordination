package no.kommune.homecare.common.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String entityName, Object id) {
        return new ResourceNotFoundException("%s not found with id: %s".formatted(entityName, id));
    }
}
