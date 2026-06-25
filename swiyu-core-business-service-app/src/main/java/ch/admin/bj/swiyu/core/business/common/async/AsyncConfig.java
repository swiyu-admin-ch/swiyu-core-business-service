package ch.admin.bj.swiyu.core.business.common.async;

import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@Slf4j
@Configuration
@EnableAsync // @Async: e.g. for Demo data imports on startup
public class AsyncConfig implements AsyncConfigurer {

    private final AsyncTaskExecutor applicationTaskExecutor;

    public AsyncConfig(@Qualifier("applicationTaskExecutor") AsyncTaskExecutor applicationTaskExecutor) {
        this.applicationTaskExecutor = applicationTaskExecutor;
    }

    /**
     * Gibt einen Executor vom Typ DelegatingSecurityContextAsyncTaskExecutor zurück. Dieser stellt sicher, dass der
     * Security context "vererbt" wird beim Aufruf von Methoden via @Async.
     */
    @Override
    public Executor getAsyncExecutor() {
        return new DelegatingSecurityContextAsyncTaskExecutor(applicationTaskExecutor);
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) ->
            log.error("Async method {} threw an uncaught exception", method.getName(), throwable);
    }
}
