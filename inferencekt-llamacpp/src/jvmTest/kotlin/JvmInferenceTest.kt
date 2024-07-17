import kotlinx.coroutines.test.runTest
import org.pinelang.inferencekt.ModelStatus
import org.pinelang.inferencekt.llamacpp.LlammaCPPInferenceEngine
import org.pinelang.inferencekt.llamacpp.platformCreateDefaultModel
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmInferenceTest {

    @Test
    fun loadModelTest() = runTest {
        val model = platformCreateDefaultModel()
        val engine = LlammaCPPInferenceEngine()
        assertEquals(engine.loadModel(model), ModelStatus.Loaded)
        val f = engine.generateText("What is the top 5 most used programming languages?")
        f.collect {
            print(it)
        }
    }
}