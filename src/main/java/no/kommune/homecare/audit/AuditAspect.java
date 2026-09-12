package no.kommune.homecare.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Intercepts every {@link Auditable} service method and writes an
 * {@link AuditLog} entry once the method has returned successfully, so that
 * access to and changes of patient data are always traceable.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void auditMethod(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            Object entityId = resolveEntityId(joinPoint, result);
            auditService.record(auditable.action(), auditable.entityType(), entityId, auditable.details());
        } catch (Exception ex) {
            log.warn("Failed to write audit log for {}", joinPoint.getSignature(), ex);
        }
    }

    private Object resolveEntityId(JoinPoint joinPoint, Object result) {
        Object fromArgs = firstUuidArgument(joinPoint);
        if (fromArgs != null) {
            return fromArgs;
        }
        return idFromReturnValue(result);
    }

    private Object firstUuidArgument(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?>[] paramTypes = signature.getParameterTypes();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < paramTypes.length; i++) {
            if (paramTypes[i].equals(UUID.class) && args[i] != null) {
                return args[i];
            }
        }
        return null;
    }

    private Object idFromReturnValue(Object result) {
        if (result == null) {
            return null;
        }
        try {
            Method getId = result.getClass().getMethod("getId");
            return getId.invoke(result);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }
}
