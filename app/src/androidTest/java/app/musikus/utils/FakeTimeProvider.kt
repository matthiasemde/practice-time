package app.musikus.utils

import java.time.ZonedDateTime
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class FakeTimeProvider : TimeProvider {
    val startTime: ZonedDateTime = ZonedDateTime.parse("1969-07-20T20:17:40Z[UTC]")
    private var _currentDateTime = startTime

    override fun now(): ZonedDateTime {
        return _currentDateTime
    }

    fun setCurrentDateTime(dateTime: ZonedDateTime) {
        _currentDateTime = dateTime
    }

    fun advanceTimeBy(duration: Duration) {
        _currentDateTime = _currentDateTime.plus(duration.toJavaDuration())
    }

    fun revertTimeBy(duration: Duration) {
        _currentDateTime = _currentDateTime.minus(duration.toJavaDuration())
    }

}