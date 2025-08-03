package metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.RequestHandledEvent;

@Component
public class PerformanceMetrics {
    private final MeterRegistry meterRegistry;

    public PerformanceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @EventListener
    public void handleRequest(RequestHandledEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("http.requests")
                .tag("description", event.getDescription())
                .tag("time", String.valueOf(event.getProcessingTimeMillis()))
                .register(meterRegistry));
    }
}
