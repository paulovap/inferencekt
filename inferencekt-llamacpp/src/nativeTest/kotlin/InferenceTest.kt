import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
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
        val model = platformCreateDefaultModel()
        val engine = LlammaCPPInferenceEngine()
        val job = launch {
            assertEquals(engine.loadModel(model), ModelStatus.Loaded)
            val f = engine.generateText("What is the top 5 most used programming languages?")
            f.collect {
                println(it)
            }

            assertEquals(f.last(), "asdf")
        }
        job.join()
    }
}