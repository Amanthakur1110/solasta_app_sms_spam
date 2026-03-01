package com.example.spamscan.ml

import android.content.Context
import android.util.Log
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.regex.Pattern

data class SpamResult(val probability: Float, val isSpam: Boolean, val message: String)

object SpamDetector {
    private const val TAG = "SpamDetector"
    private const val MAX_LEN = 165
    private var interpreter: Interpreter? = null
    private var wordIndex: Map<String, Int>? = null
    private var currentModelConfig: String = "default"

    fun initialize(context: Context, useCustom: Boolean = false) {
        val configKey = if (useCustom) "custom" else "default"
        
        if (interpreter == null || currentModelConfig != configKey) {
            interpreter?.close()
            interpreter = loadModel(context, useCustom)
            currentModelConfig = configKey
        }
        
        if (wordIndex == null) {
            wordIndex = loadWordIndex(context)
        }
    }

    fun forceReload(context: Context, useCustom: Boolean) {
        interpreter?.close()
        interpreter = loadModel(context, useCustom)
        currentModelConfig = if (useCustom) "custom" else "default"
    }

    private fun loadModel(context: Context, useCustom: Boolean): Interpreter {
        val options = Interpreter.Options()
        
        return if (useCustom) {
            val customFile = java.io.File(context.filesDir, "custom_model.tflite")
            if (customFile.exists()) {
                Interpreter(customFile, options)
            } else {
                // Fallback to default if custom file missing
                loadDefaultModel(context, options)
            }
        } else {
            loadDefaultModel(context, options)
        }
    }

    private fun loadDefaultModel(context: Context, options: Interpreter.Options): Interpreter {
        val assetFileDescriptor = context.assets.openFd("model.tflite")
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        return Interpreter(mappedByteBuffer, options)
    }

    private fun loadWordIndex(context: Context): Map<String, Int> {
        return try {
            val stream = context.assets.open("word_index.json")
            val typeRef = object : TypeReference<Map<String, Int>>() {}
            ObjectMapper().readValue(stream.bufferedReader(), typeRef)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load word_index.json", e)
            emptyMap()
        }
    }

    private fun createAdditionalFeatures(message: String): List<Float> {
        fun countSymbols(msg: String): Double {
            val symbols = """!"#$%&'()*+,-./:;<=>?@[\]^_`{|}~""".toSet()
            val count = msg.count { it in symbols }
            return count.toDouble() / msg.length
        }

        fun countCapitals(msg: String): Double {
            val count = msg.count { it.isUpperCase() }
            return count.toDouble() / msg.length
        }

        fun countNumbers(msg: String): Double {
            val digitCount = msg.count { it.isDigit() }
            return digitCount.toDouble() / msg.length
        }

        if (message.isEmpty()) return listOf(0f, 0f, 0f, 0f, 0f)

        val messageLength = message.length.toFloat()
        val tokenLength = message.split(" ").size.toFloat()
        val numSymbols = countSymbols(message).toFloat()
        val numCapitals = countCapitals(message).toFloat()
        val numNumbers = countNumbers(message).toFloat()

        return listOf(messageLength, tokenLength, numSymbols, numCapitals, numNumbers)
    }

    private fun cleanText(message: String): String {
        // Replace URLs
        val urlPattern = Pattern.compile("https?://.*\\S+|www\\.\\S+")
        var processed = message.split(" ").joinToString(" ") { token ->
            if (urlPattern.matcher(token).matches()) "urltoken" else token
        }

        // Remove punctuation
        processed = processed.filter { it !in """${Regex.escape("""!"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~""")}""" }.lowercase()

        // Replace Leetspeak
        processed = processed.split(" ").joinToString(" ") { token ->
            if (Regex("[^\\w\\s]|\\d").containsMatchIn(token) && !token.all { it.isDigit() }) "leettoken" else token
        }

        // Replace Numbers
        processed = processed.split(" ").joinToString(" ") { token ->
            if (token.all { it.isDigit() }) "numbertoken" else token
        }

        // Remove Non-letters
        val lowercaseAlphabet = ('a'..'z').joinToString("") + " "
        processed = processed.filter { it in lowercaseAlphabet }
        processed = processed.split(" ").joinToString(" ") { token ->
            when (token) {
                "urltoken" -> "<URL>"
                "numbertoken" -> "<NUMBER>"
                "leettoken" -> "<LEET>"
                else -> token
            }
        }
        
        return processed.split(" ").filter { it.length > 1 }.joinToString(" ")
    }

    private fun tokenizeAndPadSequence(text: String): List<Int> {
        val indexMap = wordIndex ?: return List(MAX_LEN) { 0 }
        
        // Tokenize
        val sequence = text.split(" ").map { word -> indexMap.getOrDefault(word, 1) }
        
        // Pad
        val sequenceLength = minOf(sequence.size, MAX_LEN)
        val paddedSequence = MutableList(0) { 0 }
        val padding = MutableList(MAX_LEN) { 0 }
        
        paddedSequence.addAll(padding.subList(0, MAX_LEN - sequenceLength))
        paddedSequence.addAll(sequence.subList(0, sequenceLength))
        
        return paddedSequence
    }

    fun classify(context: Context, rawMessage: String, threshold: Float = 0.5f, useCustom: Boolean = false): SpamResult {
        initialize(context, useCustom)
        
        val additionalFeatures = createAdditionalFeatures(rawMessage)
        val cleanedText = cleanText(rawMessage)
        val paddedSequence = tokenizeAndPadSequence(cleanedText)
        
        val prob = runInference(paddedSequence, additionalFeatures)
        return SpamResult(prob, prob >= threshold, rawMessage)
    }

    private fun runInference(textInputSequence: List<Int>, additionalFeatures: List<Float>): Float {
        val tflite = interpreter ?: return 0f
        
        val textInputArray = textInputSequence.map { it.toFloat() }.toFloatArray()
        val additionalFeaturesArray = additionalFeatures.toFloatArray()

        val outputs: MutableMap<Int, Any> = mutableMapOf()
        outputs[0] = Array(1) { FloatArray(1) }

        val textInputBuffer = ByteBuffer.allocateDirect(textInputArray.size * 4).order(ByteOrder.nativeOrder())
        val additionalFeaturesBuffer = ByteBuffer.allocateDirect(additionalFeaturesArray.size * 4).order(ByteOrder.nativeOrder())

        textInputBuffer.asFloatBuffer().put(textInputArray)
        additionalFeaturesBuffer.asFloatBuffer().put(additionalFeaturesArray)

        tflite.runForMultipleInputsOutputs(
            arrayOf(textInputBuffer, additionalFeaturesBuffer),
            outputs
        )

        val outputArray = (outputs[0] as Array<*>)[0] as FloatArray
        return outputArray[0]
    }
}
