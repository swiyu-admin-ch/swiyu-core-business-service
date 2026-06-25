package ch.admin.bj.swiyu.core.business.common.security;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
@UtilityClass
public class SystemUserAuthenticationSupport {

    public static void setSystemSecurityContext() {
        log.info("Setting system user security context...");
        SecurityContextHolder.getContext().setAuthentication(new SystemUserAuthentication());
    }
}
