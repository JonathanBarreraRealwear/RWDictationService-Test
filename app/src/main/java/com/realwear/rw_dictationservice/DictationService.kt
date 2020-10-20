package com.realwear.rw_dictationservice

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionService
import android.util.Log
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognitionEventArgs
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig

/**
 * Custom RecognitionService for Dictation.
 */
class DictationService : RecognitionService() {
    override fun onStartListening(intent: Intent, callback: Callback) {
        val speechConfig =
            SpeechConfig.fromSubscription(SpeechSubscriptionKey, SpeechRegion)
        val audioInput = AudioConfig.fromDefaultMicrophoneInput()
        val speechRecognizer = SpeechRecognizer(speechConfig, audioInput)

        speechRecognizer.recognizing.addEventListener {
                _: Any?, speechRecognitionResultEventArgs: SpeechRecognitionEventArgs ->
            callback.partialResults(bundleResults(speechRecognitionResultEventArgs))
        }

        speechRecognizer.recognized.addEventListener {
                _: Any?, speechRecognitionResultEventArgs: SpeechRecognitionEventArgs ->
            callback.endOfSpeech()
            callback.results(bundleResults(speechRecognitionResultEventArgs))
        }
    }

    /**
     * Method to bundle dictation results from speech recognizer.
     * @return Bundle with string array list holding recognition results.
     */
    private fun bundleResults(speechRecognitionResultEventArgs: SpeechRecognitionEventArgs): Bundle {
        val text = speechRecognitionResultEventArgs.result.text
        val bundle = Bundle()
        val arrayList = ArrayList<String>()
        arrayList.add(text)
        bundle.putStringArrayList(
            android.speech.SpeechRecognizer.RESULTS_RECOGNITION,
            arrayList
        )
        return bundle
    }

    override fun onStopListening(p0: Callback?) {
        Log.d(TAG, "onStopListening called.")
    }

    override fun onCancel(p0: Callback?) {
        Log.d(TAG, "onCancel called.")
    }

    companion object {
        private val TAG = DictationService::class.java.simpleName
        private const val SpeechSubscriptionKey = "226314cc4670432e87d9f80f805bffea"
        private const val SpeechRegion = "westus2"
    }
}
