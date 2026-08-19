package com.distributed.scheduler.retry.strategy;

import com.distributed.scheduler.retry.entity.RetryPolicyEntity;
import org.springframework.stereotype.Component;

/**
 * Computes the number of seconds to wait before the next retry attempt,
 * based on the retry policy strategy and current attempt number.
 *
 * <p>Strategies:
 * <ul>
 *   <li>FIXED:               delay = initialInterval</li>
 *   <li>LINEAR_BACKOFF:      delay = initialInterval * attemptNumber</li>
 *   <li>EXPONENTIAL_BACKOFF: delay = min(initialInterval * multiplier^(attempt-1), maxInterval)</li>
 * </ul>
 */
@Component
public class BackoffCalculator {

    /**
     * @param policy        the retry policy to evaluate
     * @param attemptNumber the 1-based current retry attempt number (1 = first retry)
     * @return delay in seconds before the next attempt
     */
    public long computeDelaySeconds(RetryPolicyEntity policy, int attemptNumber) {
        int initial = policy.getInitialIntervalSeconds();
        int max = policy.getMaxIntervalSeconds();
        double multiplier = policy.getBackoffMultiplier();

        long delay = switch (policy.getStrategy()) {
            case FIXED -> initial;
            case LINEAR_BACKOFF -> (long) initial * attemptNumber;
            case EXPONENTIAL_BACKOFF -> {
                double computed = initial * Math.pow(multiplier, attemptNumber - 1);
                yield (long) Math.min(computed, max);
            }
        };

        // Always at least 1 second, at most maxInterval
        return Math.max(1, Math.min(delay, max));
    }
}
