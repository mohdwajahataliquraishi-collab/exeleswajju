package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class VoiceAction {
    data class CreateBox(val personName: String) : VoiceAction()
    data class InsertDate(val date: String) : VoiceAction()
    data class InsertAmount(val field: String, val amount: Double) : VoiceAction()
    data class AddToAmount(val amount: Double) : VoiceAction()
    data class SetDescription(val description: String) : VoiceAction()
    data class FormatCell(val property: String, val value: String) : VoiceAction()
    data class QueryTotal(val targetPerson: String? = null) : VoiceAction()
    data class OpenMonth(val monthName: String) : VoiceAction()
    object Undo : VoiceAction()
    object Redo : VoiceAction()
    data class CalculatorOp(val operation: String, val amount: Double) : VoiceAction()
    object CompleteMonthHisaab : VoiceAction() // Opens Monthly Screenshot Gallery
    object FinalizeMonthValidation : VoiceAction() // Runs Month-End validation
    data class Ambiguous(val question: String) : VoiceAction()
    data class Unknown(val rawText: String) : VoiceAction()
}

data class VoiceState(
    val isListening: Boolean = false,
    val lastTranscript: String = "",
    val intendedActionDescription: String = "",
    val executionResultText: String = "",
    val isSpeaking: Boolean = false,
    val speechError: String? = null
)

class VoiceAssistantManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val _voiceState = MutableStateFlow(VoiceState())
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    var onActionDetected: ((VoiceAction, String) -> Unit)? = null

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale("en", "IN"))
            }
            ttsReady = true
        }
    }

    fun speak(text: String) {
        if (ttsReady && tts != null) {
            _voiceState.value = _voiceState.value.copy(isSpeaking = true)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "VOICE_REPLY")
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _voiceState.value = _voiceState.value.copy(
                speechError = "Speech recognition is not available on this device"
            )
            return
        }

        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _voiceState.value = _voiceState.value.copy(isListening = true, speechError = null)
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _voiceState.value = _voiceState.value.copy(isListening = false)
                    }

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Audio permission needed"
                            SpeechRecognizer.ERROR_NETWORK -> "Network connection required"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Please speak clearly."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer is busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input received"
                            else -> "Recognition error: $error"
                        }
                        _voiceState.value = _voiceState.value.copy(isListening = false, speechError = message)
                    }

                    override fun onResults(results: Bundle?) {
                        _voiceState.value = _voiceState.value.copy(isListening = false)
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognized = matches[0]
                            processVoiceInput(recognized)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!partials.isNullOrEmpty()) {
                            _voiceState.value = _voiceState.value.copy(lastTranscript = partials[0])
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "EXELESWAJJU PRO Voice Assistant (Hindi / English / Hinglish)...")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _voiceState.value = _voiceState.value.copy(
                isListening = false,
                speechError = e.localizedMessage ?: "Failed to start voice recognition"
            )
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _voiceState.value = _voiceState.value.copy(isListening = false)
    }

    /**
     * Interprets Roman Hindi, Hindi, Hyderabadi Hinglish, and English voice commands.
     */
    fun processVoiceInput(rawText: String) {
        val trimmed = rawText.trim()
        val lower = trimmed.lowercase(Locale.ROOT)
        val action = parseHinglishCommand(lower, trimmed)

        val intentDesc = when (action) {
            is VoiceAction.CreateBox -> "Create accounting box for: ${action.personName}"
            is VoiceAction.InsertDate -> "Set date to: ${action.date}"
            is VoiceAction.InsertAmount -> "Write ₹${action.amount} in ${action.field}"
            is VoiceAction.AddToAmount -> "Add ₹${action.amount} to current amount"
            is VoiceAction.SetDescription -> "Write description: ${action.description}"
            is VoiceAction.FormatCell -> "Format ${action.property} -> ${action.value}"
            is VoiceAction.QueryTotal -> "Calculate total balance ${action.targetPerson?.let { "for $it" } ?: ""}"
            is VoiceAction.OpenMonth -> "Open archive for ${action.monthName}"
            is VoiceAction.Undo -> "Undo last change"
            is VoiceAction.Redo -> "Redo change"
            is VoiceAction.CalculatorOp -> "Calculator: ${action.operation} ${action.amount}"
            is VoiceAction.CompleteMonthHisaab -> "Generate Month-End Screenshot Gallery"
            is VoiceAction.FinalizeMonthValidation -> "Finalize & Verify Monthly Hisaab"
            is VoiceAction.Ambiguous -> "Clarification needed: ${action.question}"
            is VoiceAction.Unknown -> "General query: $rawText"
        }

        _voiceState.value = _voiceState.value.copy(
            lastTranscript = trimmed,
            intendedActionDescription = intentDesc,
            speechError = null
        )

        onActionDetected?.invoke(action, trimmed)
    }

    private fun parseHinglishCommand(lower: String, original: String): VoiceAction {
        // 1. Month-End Gallery Command: "Mahine ka poora hisaab complete karo" / "Screenshot banao"
        if (lower.contains("complete karo") || lower.contains("poora hisaab") || lower.contains("screenshot") || lower.contains("gallery")) {
            return VoiceAction.CompleteMonthHisaab
        }

        // 2. Month-End Validation: "Mahine ka hisaab final karo" / "Audit karo" / "Verify karo"
        if (lower.contains("final karo") || lower.contains("hisaab final") || lower.contains("check karo") || lower.contains("verify")) {
            return VoiceAction.FinalizeMonthValidation
        }

        // 3. Undo / Redo
        if (lower.contains("undo") || lower.contains("wapas lo") || lower.contains("peechhe karo")) {
            return VoiceAction.Undo
        }
        if (lower.contains("redo") || lower.contains("aage karo")) {
            return VoiceAction.Redo
        }

        // 4. Create Box: "Salman ka box banao" / "Naya box banao, naam Salman" / "Ek box banao Imran"
        val boxRegex1 = Regex("(?:naya\\s+)?box\\s+banao[\\s,]+(?:naam\\s+)?([a-zA-Z\\s]+)", RegexOption.IGNORE_CASE)
        val boxRegex2 = Regex("([a-zA-Z\\s]+?)\\s+ka\\s+box\\s+banao", RegexOption.IGNORE_CASE)
        val boxRegex3 = Regex("create\\s+(?:a\\s+)?box\\s+(?:for\\s+)?([a-zA-Z\\s]+)", RegexOption.IGNORE_CASE)

        boxRegex1.find(original)?.let { match ->
            val name = match.groupValues[1].trim()
            if (name.isNotBlank()) return VoiceAction.CreateBox(name.capitalizeWords())
        }
        boxRegex2.find(original)?.let { match ->
            val name = match.groupValues[1].trim()
            if (name.isNotBlank()) return VoiceAction.CreateBox(name.capitalizeWords())
        }
        boxRegex3.find(original)?.let { match ->
            val name = match.groupValues[1].trim()
            if (name.isNotBlank()) return VoiceAction.CreateBox(name.capitalizeWords())
        }

        // 5. Date command: "Date 12 October daalo" / "Taarikh 15 October likho" / "Aaj ki date daal"
        if (lower.contains("aaj ki date") || lower.contains("today's date") || lower.contains("aaj ki tarikh")) {
            val today = SimpleDateFormat("dd MMM", Locale.US).format(Date())
            return VoiceAction.InsertDate(today)
        }
        val dateRegex = Regex("(?:date|tarikh|taarikh)\\s+(\\d{1,2}(?:\\s+[a-zA-Z]+)?(?:\\s+\\d{4})?)", RegexOption.IGNORE_CASE)
        dateRegex.find(original)?.let { match ->
            return VoiceAction.InsertDate(match.groupValues[1].trim())
        }

        // 6. Charbi amount: "Charbi mein 250 likho" / "Charbi 250 karo"
        if (lower.contains("charbi")) {
            val amt = extractNumber(lower)
            if (amt != null) {
                return VoiceAction.InsertAmount("Charbi", amt)
            }
        }

        // 7. Jama / Cash amount: "Jama mein 5000 likho" / "Cash 2000 jama karo" / "Jama 500"
        if (lower.contains("jama") || lower.contains("credit") || lower.contains("cash")) {
            val amt = extractNumber(lower)
            if (amt != null) {
                return VoiceAction.InsertAmount("Jama", amt)
            }
        }

        // 8. Add to current amount: "Is amount mein 500 aur add karo" / "500 aur jod" / "500 plus kar"
        if (lower.contains("add karo") || lower.contains("aur jod") || lower.contains("plus kar") || lower.contains("aur add")) {
            val amt = extractNumber(lower)
            if (amt != null) {
                return VoiceAction.AddToAmount(amt)
            }
        }

        // 9. Calculator Commands: "500 add kar" / "Ab 250 aur jod" / "Result selected cell mein daal"
        if (lower.contains("result selected cell mein") || lower.contains("cell mein daal") || lower.contains("insert result")) {
            return VoiceAction.CalculatorOp("insert_to_cell", 0.0)
        }
        if (lower.contains("minus kar") || lower.contains("ghata") || lower.contains("kam kar")) {
            val amt = extractNumber(lower)
            if (amt != null) return VoiceAction.CalculatorOp("minus", amt)
        }
        if (lower.contains("multiply") || lower.contains("guna kar")) {
            val amt = extractNumber(lower)
            if (amt != null) return VoiceAction.CalculatorOp("multiply", amt)
        }

        // 10. Formatting Commands:
        // "Heading ka colour violet kar" / "Heading violet karo"
        if (lower.contains("heading") && (lower.contains("colour") || lower.contains("color") || lower.contains("violet") || lower.contains("blue"))) {
            val color = if (lower.contains("violet")) "violet" else if (lower.contains("blue")) "blue" else if (lower.contains("emerald") || lower.contains("green")) "emerald" else "midnight"
            return VoiceAction.FormatCell("header_color", color)
        }
        // "Background light blue kar" / "Cell yellow kar" / "Background green kar"
        if (lower.contains("background") || lower.contains("bg")) {
            val color = when {
                lower.contains("blue") -> "soft_blue"
                lower.contains("green") || lower.contains("emerald") -> "soft_green"
                lower.contains("yellow") || lower.contains("amber") -> "soft_yellow"
                lower.contains("violet") || lower.contains("purple") -> "soft_violet"
                else -> "white"
            }
            return VoiceAction.FormatCell("background", color)
        }
        // "Font size 16 kar" / "Font 16 karo"
        if (lower.contains("font size") || lower.contains("font")) {
            val size = extractNumber(lower)?.toInt()
            if (size != null && size in 8..36) {
                return VoiceAction.FormatCell("font_size", size.toString())
            }
            if (lower.contains("bold")) return VoiceAction.FormatCell("bold", "true")
            if (lower.contains("italic")) return VoiceAction.FormatCell("italic", "true")
        }
        if (lower.contains("bold kar") || lower.contains("make bold")) {
            return VoiceAction.FormatCell("bold", "true")
        }

        // 11. Queries: "Is mahine ka total bata" / "Salman ka balance kitna hai" / "Total bata"
        if (lower.contains("total bata") || lower.contains("total kitna") || lower.contains("balance bata")) {
            val person = when {
                lower.contains("salman") -> "Salman"
                lower.contains("naveed") -> "Naveed Quraishi"
                lower.contains("imran") -> "Imran"
                else -> null
            }
            return VoiceAction.QueryTotal(person)
        }

        // 12. Month navigation: "September ka purana hisaab kholo" / "October ka hisaab dikhao"
        val months = listOf("january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december")
        for (m in months) {
            if (lower.contains(m)) {
                return VoiceAction.OpenMonth(m.capitalizeWords())
            }
        }

        // If numbers only
        val singleNum = extractNumber(lower)
        if (singleNum != null) {
            return VoiceAction.AddToAmount(singleNum)
        }

        return VoiceAction.Unknown(original)
    }

    private fun extractNumber(text: String): Double? {
        val match = Regex("(\\d+(?:\\.\\d+)?)").find(text)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
