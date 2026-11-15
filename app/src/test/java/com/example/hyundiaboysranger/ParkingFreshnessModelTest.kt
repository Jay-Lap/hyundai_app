package com.example.hyundiaboysranger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Test-only conservative model, not the app's FeatureGate or an Android Car contract. */
class ParkingFreshnessModelTest {
    private enum class Status { AVAILABLE, UNAVAILABLE, ERROR }
    private enum class Gear { PARK, DRIVE }
    private enum class Reason { DISCONNECTED, SESSION_CHANGED, INVALID_TIME, FUTURE_PROPERTY, UNAVAILABLE, ERROR, DRIVING, NOT_CONFIRMED, HYPOTHETICAL_PARKED }
    private data class Signal<T>(val value: T, val status: Status, val timestampNs: Long)
    private data class Link(val ready: Boolean, val generation: Int)
    // Fictional test inputs, not capabilities confirmed in Pleos or Android Car.
    private data class Hypothesis(
        val contractAvailable: Boolean = false,
        val monitorActive: Boolean = false,
        val monitorGeneration: Int? = null,
        val evidenceVerifiedGeneration: Int? = null,
    )
    private data class Sample(
        // Gear is ONCHANGE: an unchanged timestamp is not proof of an old value.
        val gear: Signal<Gear> = Signal(Gear.PARK, Status.AVAILABLE, 1_000),
        // Speed is CONTINUOUS, but a freshness deadline is not established here.
        val speed: Signal<Float> = Signal(0f, Status.AVAILABLE, 1_000),
        val readStartNs: Long = 1_100,
        val readEndNs: Long = 1_200,
        val before: Link = Link(true, 1),
        val during: Link = Link(true, 1),
        val after: Link = Link(true, 1),
        val uxAllowed: Boolean? = true,
        val hypothesis: Hypothesis = Hypothesis(),
    )
    private class FakeCar(var sample: Sample) {
        fun read() = sample
    }

    // Inputs describe the result of a fake poll and the independently observed current link.
    // Only a fictional injected contract can attest freshness and cross-property consistency.
    private fun evaluate(sample: Sample, current: Link, nowNs: Long): Reason {
        if (listOf(sample.before, sample.during, sample.after, current).any { !it.ready }) return Reason.DISCONNECTED
        if (listOf(sample.before, sample.during, sample.after, current).map { it.generation }.distinct().size != 1) return Reason.SESSION_CHANGED
        if (sample.readStartNs < 0 || sample.readEndNs < sample.readStartNs || nowNs < sample.readEndNs ||
            sample.gear.timestampNs < 0 || sample.speed.timestampNs < 0) return Reason.INVALID_TIME
        if (sample.gear.timestampNs > nowNs || sample.speed.timestampNs > nowNs) return Reason.FUTURE_PROPERTY
        if (sample.gear.status == Status.ERROR || sample.speed.status == Status.ERROR) return Reason.ERROR
        if (sample.gear.status != Status.AVAILABLE || sample.speed.status != Status.AVAILABLE || sample.uxAllowed != true) return Reason.UNAVAILABLE
        if (sample.gear.value != Gear.PARK || !sample.speed.value.isFinite() || sample.speed.value != 0f) return Reason.DRIVING
        val h = sample.hypothesis
        return if (h.contractAvailable && h.monitorActive && h.monitorGeneration == current.generation &&
            h.evidenceVerifiedGeneration == current.generation) Reason.HYPOTHETICAL_PARKED else Reason.NOT_CONFIRMED
    }

    private fun allows(reason: Reason) = when (reason) {
        Reason.DISCONNECTED, Reason.SESSION_CHANGED, Reason.INVALID_TIME, Reason.FUTURE_PROPERTY,
        Reason.UNAVAILABLE, Reason.ERROR, Reason.DRIVING, Reason.NOT_CONFIRMED -> false
        Reason.HYPOTHETICAL_PARKED -> true
    } // Fictional inputs only; not the operational gate.

    private inner class FakeGate {
        var open = false
            private set
        fun update(sample: Sample, current: Link, nowNs: Long) {
            open = allows(evaluate(sample, current, nowNs))
        }
    }

    @Test fun parkedEvenWithMatchingTimestampsRemainsUnconfirmed() {
        val car = FakeCar(Sample())
        val result = evaluate(car.read(), Link(true, 1), 1_300)
        assertEquals(Reason.NOT_CONFIRMED, result)
        assertFalse(allows(result))
    }

    @Test fun hypotheticalVerifiedContractOpensAndLossesRelock() {
        val assumed = Hypothesis(contractAvailable = true, monitorActive = true,
            monitorGeneration = 1, evidenceVerifiedGeneration = 1)
        // An unchanged ONCHANGE gear timestamp remains acceptable only because the
        // hypothetical contract explicitly attests this sample, not because of its age.
        val parked = Sample(gear = Signal(Gear.PARK, Status.AVAILABLE, 1), hypothesis = assumed)
        val gate = FakeGate()
        val current = Link(true, 1)
        val unsafe = listOf(
            parked.copy(gear = parked.gear.copy(value = Gear.DRIVE)),
            parked.copy(speed = parked.speed.copy(value = 1f)),
            parked.copy(gear = parked.gear.copy(status = Status.ERROR)),
            parked.copy(speed = parked.speed.copy(status = Status.ERROR)),
            parked.copy(uxAllowed = false),
            parked.copy(hypothesis = assumed.copy(monitorActive = false)),
        )
        for (sample in unsafe) {
            gate.update(parked, current, 1_300)
            assertTrue(gate.open)
            gate.update(sample, current, 1_300)
            assertFalse(gate.open)
        }
        gate.update(parked, current, 1_300)
        assertTrue(gate.open)
        gate.update(parked, Link(false, 1), 1_300)
        assertFalse(gate.open)
        // A new connection cannot reuse the previous generation's attestation.
        gate.update(parked, Link(true, 2), 1_300)
        assertFalse(gate.open)
        val newLink = Link(true, 2)
        val reconnected = parked.copy(before = newLink, during = newLink, after = newLink,
            hypothesis = assumed.copy(monitorGeneration = 2, evidenceVerifiedGeneration = 2))
        gate.update(reconnected, newLink, 1_300)
        assertTrue(gate.open)
    }

    @Test fun absentHypothesesOrIncompleteEvidenceNeverOpen() {
        val assumed = Hypothesis(contractAvailable = true, monitorActive = true,
            monitorGeneration = 1, evidenceVerifiedGeneration = 1)
        val parked = Sample(hypothesis = assumed)
        val uncertain = listOf(
            parked.copy(hypothesis = assumed.copy(contractAvailable = false)),
            parked.copy(hypothesis = assumed.copy(monitorActive = false)),
            parked.copy(hypothesis = assumed.copy(monitorGeneration = null)),
            parked.copy(hypothesis = assumed.copy(evidenceVerifiedGeneration = null)),
            parked.copy(gear = parked.gear.copy(status = Status.UNAVAILABLE)),
            parked.copy(speed = parked.speed.copy(status = Status.UNAVAILABLE)),
            parked.copy(uxAllowed = null),
            parked.copy(gear = parked.gear.copy(timestampNs = 1_301)),
        )
        for (sample in uncertain) {
            assertFalse(allows(evaluate(sample, Link(true, 1), 1_300)))
        }
    }

    @Test fun drivingAndBothPropertyStatusesCannotBecomeParked() {
        val base = Sample()
        assertEquals(Reason.DRIVING, evaluate(base.copy(gear = base.gear.copy(value = Gear.DRIVE)), base.after, 1_300))
        assertEquals(Reason.DRIVING, evaluate(base.copy(speed = base.speed.copy(value = 1f)), base.after, 1_300))
        for (gear in listOf(true, false)) for (status in listOf(Status.UNAVAILABLE, Status.ERROR)) {
            val sample = if (gear) base.copy(gear = base.gear.copy(status = status)) else base.copy(speed = base.speed.copy(status = status))
            val result = evaluate(sample, base.after, 1_300)
            assertEquals(if (status == Status.ERROR) Reason.ERROR else Reason.UNAVAILABLE, result)
            assertFalse(allows(result))
        }
        assertEquals(Reason.UNAVAILABLE, evaluate(base.copy(uxAllowed = null), base.after, 1_300))
    }

    @Test fun oldAndDifferentPropertyTimesStayUnconfirmedWithoutAssumptions() {
        val base = Sample()
        val oldGear = base.copy(gear = base.gear.copy(timestampNs = 1))
        val oldSpeed = base.copy(speed = base.speed.copy(timestampNs = 1))
        val distinct = base.copy(gear = base.gear.copy(timestampNs = 900), speed = base.speed.copy(timestampNs = 1_100))
        for (sample in listOf(oldGear, oldSpeed, distinct)) {
            assertEquals(Reason.NOT_CONFIRMED, evaluate(sample, base.after, 1_300))
        }
        // Re-reading an unchanged ONCHANGE gear timestamp is not classified as stale.
        assertEquals(Reason.NOT_CONFIRMED, evaluate(oldGear, base.after, 5_000))
    }

    @Test fun futurePropertyAndInvalidPollTimesAreUnconfirmed() {
        val base = Sample()
        val futureSamples = listOf(
            base.copy(gear = base.gear.copy(timestampNs = 1_301)),
            base.copy(speed = base.speed.copy(timestampNs = 1_301)),
        )
        for (sample in futureSamples) {
            assertEquals(Reason.FUTURE_PROPERTY, evaluate(sample, base.after, 1_300))
        }
        assertEquals(Reason.INVALID_TIME, evaluate(base.copy(readEndNs = 1_000), base.after, 1_300))
        assertEquals(Reason.INVALID_TIME, evaluate(base, base.after, 1_199))
    }

    @Test fun disconnectDuringOrAfterReadAndReconnectionDiscardOldSession() {
        val car = FakeCar(Sample())
        val base = car.read()
        assertEquals(Reason.DISCONNECTED, evaluate(base.copy(before = Link(false, 1)), base.after, 1_300))
        assertEquals(Reason.DISCONNECTED, evaluate(base.copy(during = Link(false, 1)), base.after, 1_300))
        assertEquals(Reason.DISCONNECTED, evaluate(base.copy(after = Link(false, 1)), Link(false, 1), 1_300))
        assertEquals(Reason.DISCONNECTED, evaluate(base, Link(false, 1), 1_300))
        assertEquals(Reason.SESSION_CHANGED, evaluate(base, Link(true, 2), 1_300))
        assertEquals(Reason.SESSION_CHANGED, evaluate(base.copy(during = Link(true, 2), after = Link(true, 2)), Link(true, 2), 1_300))
        car.sample = base.copy(before = Link(true, 2), during = Link(true, 2), after = Link(true, 2))
        assertEquals(Reason.NOT_CONFIRMED, evaluate(car.read(), Link(true, 2), 1_300))
    }
}
