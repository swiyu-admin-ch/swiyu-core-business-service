package ch.admin.bj.swiyu.core.business.common.utils.testSpan;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

public class TestSpanExtension implements BeforeEachCallback, AfterEachCallback {

    public static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(
        TestSpanExtension.class
    );

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        var tracer = SpringExtension.getApplicationContext(context).getBean(Tracer.class);
        var spanName = resolveSpansName(context);

        var span = tracer.nextSpan().name(spanName).start();
        var scope = tracer.withSpan(span);

        context.getStore(NAMESPACE).put("span", span);
        context.getStore(NAMESPACE).put("scope", scope);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        var span = context.getStore(NAMESPACE).get("span", Span.class);
        var scope = context.getStore(NAMESPACE).get("scope", Tracer.SpanInScope.class);

        scope.close();
        span.end();
    }

    private String resolveSpansName(ExtensionContext context) {
        var annotation = context.getRequiredTestMethod().getAnnotation(WithTestSpan.class);
        var configuredName = (annotation != null) ? annotation.value() : "";
        return configuredName.isBlank() ? context.getRequiredTestMethod().getName() : configuredName;
    }
}
