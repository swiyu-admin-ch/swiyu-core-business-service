package ch.admin.bj.swiyu.core.business.common.security;

import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AbstractAuthenticationToken;

@Slf4j
public class SystemUserAuthentication extends AbstractAuthenticationToken {

    private static final String SYSTEM_USER = "SYSTEM";

    public SystemUserAuthentication() {
        super(new ArrayList<>() /* here we could later even define the permissions */);
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return SYSTEM_USER;
    }
}
