package ch.admin.bj.swiyu.core.business.common.exceptions;

public class ServiceUnhealthyException extends RuntimeException {

    public ServiceUnhealthyException(String message, ExternalSystem externalSystem) {
        super(String.format("System '%s' unhealthy: %s", externalSystem, message));
    }
}
