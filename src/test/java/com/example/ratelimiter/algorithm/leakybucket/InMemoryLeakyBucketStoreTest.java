package com.example.ratelimiter.algorithm.leakybucket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryLeakyBucketStoreTest {
    @Test
    void acceptsUntilFullThenAllowsAgainWhenOldestEntryLeaks() {
        InMemoryLeakyBucketStore store = new InMemoryLeakyBucketStore();

        LeakyBucketResult first = store.tryAcquire("consumer-a", 2, 10, 1_000);
        LeakyBucketResult second = store.tryAcquire("consumer-a", 2, 10, 1_050);
        LeakyBucketResult rejected = store.tryAcquire("consumer-a", 2, 10, 1_075);
        LeakyBucketResult allowedAfterLeak = store.tryAcquire("consumer-a", 2, 10, 1_100);

        assertTrue(first.isAllowed());
        assertEquals(1, first.getCurrentLevel());
        assertTrue(second.isAllowed());
        assertEquals(2, second.getCurrentLevel());
        assertFalse(rejected.isAllowed());
        assertEquals(2, rejected.getCurrentLevel());
        assertEquals(25, rejected.getRetryAfterMillis());
        assertTrue(allowedAfterLeak.isAllowed());
        assertEquals(2, allowedAfterLeak.getCurrentLevel());
        assertEquals(0, allowedAfterLeak.getRetryAfterMillis());
    }

    @Test
    void keepsEachConsumerInAnIndependentQueue() {
        InMemoryLeakyBucketStore store = new InMemoryLeakyBucketStore();

        assertTrue(store.tryAcquire("consumer-a", 1, 10, 2_000).isAllowed());
        assertFalse(store.tryAcquire("consumer-a", 1, 10, 2_001).isAllowed());
        assertTrue(store.tryAcquire("consumer-b", 1, 10, 2_001).isAllowed());
    }

    @Test
    void leaksAllExpiredEntriesInFifoOrder() {
        InMemoryLeakyBucketStore store = new InMemoryLeakyBucketStore();
        store.tryAcquire("consumer-a", 3, 10, 3_000);
        store.tryAcquire("consumer-a", 3, 10, 3_010);
        store.tryAcquire("consumer-a", 3, 10, 3_020);

        LeakyBucketResult result = store.tryAcquire("consumer-a", 3, 10, 3_320);

        assertTrue(result.isAllowed());
        assertEquals(1, result.getCurrentLevel());
        assertEquals(2, result.getRemainingCapacity());
    }

    @Test
    void keepsBucketsIndependentPerConsumer() {
        InMemoryLeakyBucketStore store = new InMemoryLeakyBucketStore();
        long bucketCapacity = 15;
        long leakRatePerSecond = 10;
        long consumerBFirstRequestArrivalMillis = 5_000;
        long consumerBSecondRequestArrivalMillis = 10_000;
        long consumerBThirdRequestArrivalMillis = 15_000;
        long consumerAFirstRequestArrivalMillis = 20_000;

        LeakyBucketResult consumerBAt5Seconds =
                store.tryAcquire(
                        "consumer-b", bucketCapacity, leakRatePerSecond,
                        consumerBFirstRequestArrivalMillis);
        LeakyBucketResult consumerBAt10Seconds =
                store.tryAcquire(
                        "consumer-b", bucketCapacity, leakRatePerSecond,
                        consumerBSecondRequestArrivalMillis);
        LeakyBucketResult consumerBAt15Seconds =
                store.tryAcquire(
                        "consumer-b", bucketCapacity, leakRatePerSecond,
                        consumerBThirdRequestArrivalMillis);
        LeakyBucketResult consumerAAt20Seconds =
                store.tryAcquire(
                        "consumer-a", bucketCapacity, leakRatePerSecond,
                        consumerAFirstRequestArrivalMillis);

        assertTrue(consumerBAt5Seconds.isAllowed());
        assertEquals(1, consumerBAt5Seconds.getCurrentLevel());
        assertEquals(14, consumerBAt5Seconds.getRemainingCapacity());
        assertTrue(consumerBAt10Seconds.isAllowed());
        assertEquals(1, consumerBAt10Seconds.getCurrentLevel());
        assertEquals(14, consumerBAt10Seconds.getRemainingCapacity());
        assertTrue(consumerBAt15Seconds.isAllowed());
        assertEquals(1, consumerBAt15Seconds.getCurrentLevel());
        assertEquals(14, consumerBAt15Seconds.getRemainingCapacity());
        assertTrue(consumerAAt20Seconds.isAllowed());
        assertEquals(1, consumerAAt20Seconds.getCurrentLevel());
        assertEquals(14, consumerAAt20Seconds.getRemainingCapacity());
    }

    @Test
    void handlesBurstRejectionAndMultipleLeaksOverTime() {
        InMemoryLeakyBucketStore store = new InMemoryLeakyBucketStore();
        long bucketCapacity = 3;
        long leakRatePerSecond = 4;

        LeakyBucketResult firstRequest =
                store.tryAcquire("consumer-c", bucketCapacity, leakRatePerSecond, 10_000);
        LeakyBucketResult secondRequest =
                store.tryAcquire("consumer-c", bucketCapacity, leakRatePerSecond, 10_050);
        LeakyBucketResult thirdRequest =
                store.tryAcquire("consumer-c", bucketCapacity, leakRatePerSecond, 10_100);
        LeakyBucketResult rejectedWhileFull =
                store.tryAcquire("consumer-c", bucketCapacity, leakRatePerSecond, 10_150);
        LeakyBucketResult acceptedAfterOldestRequestLeaks =
                store.tryAcquire("consumer-c", bucketCapacity, leakRatePerSecond, 10_250);
        LeakyBucketResult acceptedAfterNextLeak =
                store.tryAcquire("consumer-c", bucketCapacity, leakRatePerSecond, 10_300);
        LeakyBucketResult acceptedAfterAllRemainingRequestsLeak =
                store.tryAcquire("consumer-c", bucketCapacity, leakRatePerSecond, 10_600);


        assertEquals(1,firstRequest.getCurrentLevel());
        assertEquals(2,firstRequest.getRemainingCapacity());

        assertEquals(2,secondRequest.getCurrentLevel());
        assertEquals(1,secondRequest.getRemainingCapacity());

        assertEquals(3,thirdRequest.getCurrentLevel());
        assertEquals(0,thirdRequest.getRemainingCapacity());

        assertFalse(rejectedWhileFull.isAllowed());
        assertEquals(100, rejectedWhileFull.getRetryAfterMillis());

        assertTrue(acceptedAfterOldestRequestLeaks.isAllowed());
        assertEquals(3,acceptedAfterOldestRequestLeaks.getCurrentLevel());
        assertEquals(0,acceptedAfterOldestRequestLeaks.getRemainingCapacity());

        assertTrue(acceptedAfterNextLeak.isAllowed());
        assertEquals(3,acceptedAfterNextLeak.getCurrentLevel());
        assertEquals(0,acceptedAfterNextLeak.getRemainingCapacity());

        assertTrue(acceptedAfterAllRemainingRequestsLeak.isAllowed());
        assertEquals(1,acceptedAfterAllRemainingRequestsLeak.getCurrentLevel());
        assertEquals(2,acceptedAfterAllRemainingRequestsLeak.getRemainingCapacity());


    }

    @Test
    void supportsLeakRatesAboveMillisecondResolution() {
        InMemoryLeakyBucketStore store = new InMemoryLeakyBucketStore();

        assertTrue(store.tryAcquire("fast-consumer", 1, 2_000, 1_000).isAllowed());
        assertFalse(store.tryAcquire("fast-consumer", 1, 2_000, 1_000).isAllowed());
        assertTrue(store.tryAcquire("fast-consumer", 1, 2_000, 1_001).isAllowed());
    }
}
