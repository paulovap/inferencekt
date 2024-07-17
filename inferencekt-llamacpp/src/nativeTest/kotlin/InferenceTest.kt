import app.cash.turbine.test
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.pinelang.inferencekt.ModelStatus
import org.pinelang.inferencekt.llamacpp.LlammaCPPInferenceEngine
import org.pinelang.inferencekt.llamacpp.platformCreateDefaultModel
import org.pinelang.inferencekt.llamacpp.platformInitBackend
import org.pinelang.inferencekt.llamacpp.platformLoadModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InferenceTest {

    @Test
    fun loadModelTest() = runTest {
        flowOf("one", "two").test {
            assertEquals("one", awaitItem())
            assertEquals("twso", awaitItem())
            awaitComplete()
        }
//        val model = platformCreateDefaultModel()
//        val engine = LlammaCPPInferenceEngine()
//        assertEquals(engine.loadModel(model), ModelStatus.Loaded)
//        val f = engine.generateText("What is the top 5 most used programming languages?")
//        f.test {
//            //assertEquals("first", awaitItem())
//            awaitComplete()
//        }
//        f.collect {
//            println(it)
//        }
    }
}