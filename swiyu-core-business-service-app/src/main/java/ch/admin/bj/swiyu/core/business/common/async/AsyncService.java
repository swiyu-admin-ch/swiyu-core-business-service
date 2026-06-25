package ch.admin.bj.swiyu.core.business.common.async;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class AsyncService {

    @Async
    public void run(final Runnable runnable) {
        runnable.run();
    }
}
