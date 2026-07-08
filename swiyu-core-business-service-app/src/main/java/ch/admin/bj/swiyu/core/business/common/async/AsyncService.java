package ch.admin.bj.swiyu.core.business.common.async;

import io.micrometer.tracing.annotation.NewSpan;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class AsyncService {

    @Async
    @NewSpan
    public void run(final Runnable runnable) {
        runnable.run();
    }
}
