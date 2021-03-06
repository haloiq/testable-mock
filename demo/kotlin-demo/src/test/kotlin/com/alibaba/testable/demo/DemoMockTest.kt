package com.alibaba.testable.demo

import com.alibaba.testable.core.annotation.TestableMock
import com.alibaba.testable.core.matcher.InvokeVerifier.verify
import com.alibaba.testable.core.tool.TestableTool.*
import com.alibaba.testable.demo.model.BlackBox
import com.alibaba.testable.demo.model.ColorBox
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors

internal class DemoMockTest {

    @TestableMock(targetMethod = CONSTRUCTOR)
    private fun createBlackBox(text: String) = BlackBox("mock_$text")

    @TestableMock
    private fun innerFunc(self: DemoMock, text: String) = "mock_$text"

    @TestableMock
    private fun trim(self: BlackBox) = "trim_string"

    @TestableMock(targetMethod = "substring")
    private fun sub(self: BlackBox, i: Int, j: Int) = "sub_string"

    @TestableMock
    private fun startsWith(self: BlackBox, s: String) = false

    @TestableMock
    private fun secretBox(ignore: BlackBox): BlackBox {
        return BlackBox("not_secret_box")
    }

    @TestableMock
    private fun createBox(ignore: ColorBox, color: String, box: BlackBox): BlackBox {
        return BlackBox("White_${box.get()}")
    }

    @TestableMock
    private fun callFromDifferentMethod(self: DemoMock): String {
        return if (TEST_CASE == "should_able_to_get_test_case_name") {
            "mock_special"
        } else {
            when (SOURCE_METHOD) {
                "callerOne" -> "mock_one"
                else -> "mock_others"
            }
        }
    }

    private val demoMock = DemoMock()

    @Test
    fun should_able_to_mock_new_object() {
        assertEquals("mock_something", demoMock.newFunc())
        verify("createBlackBox").with("something")
    }

    @Test
    fun should_able_to_mock_member_method() {
        assertEquals("{ \"res\": \"mock_hello\"}", demoMock.outerFunc("hello"))
        verify("innerFunc").with("hello")
    }

    @Test
    fun should_able_to_mock_common_method() {
        assertEquals("trim_string__sub_string__false", demoMock.commonFunc())
        verify("trim").withTimes(1)
        verify("sub").withTimes(1)
        verify("startsWith").withTimes(1)
    }

    @Test
    fun should_able_to_mock_static_method() {
        assertEquals("White_not_secret_box", demoMock.getBox().get())
        verify("secretBox").withTimes(1)
        verify("createBox").withTimes(1)
    }

    @Test
    fun should_able_to_get_source_method_name() {
        // synchronous
        assertEquals("mock_one_mock_others", demoMock.callerOne() + "_" + demoMock.callerTwo())
        // asynchronous
        assertEquals("mock_one_mock_others", Executors.newSingleThreadExecutor().submit<String> {
            demoMock.callerOne() + "_" + demoMock.callerTwo()
        }.get())
        verify("callFromDifferentMethod").withTimes(4)
    }

    @Test
    fun should_able_to_get_test_case_name() {
        // synchronous
        assertEquals("mock_special", demoMock.callerOne())
        // asynchronous
        assertEquals("mock_special", Executors.newSingleThreadExecutor().submit<String> {
            demoMock.callerOne()
        }.get())
        verify("callFromDifferentMethod").withTimes(2)
    }
}
