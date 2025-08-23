package tech.arcent.stats

/*
 * Simple unit test verifying counter increments in StatsViewModel.
 */

import org.junit.Assert.assertEquals
import org.junit.Test

class StatsViewModelTest {
    @Test
    fun counterStartsAtZeroAndIncrements() {
        val vm = StatsViewModel()
        assertEquals(0, vm.counter.value)
        vm.increment()
        assertEquals(1, vm.counter.value)
        vm.increment()
        assertEquals(2, vm.counter.value)
    }
}
