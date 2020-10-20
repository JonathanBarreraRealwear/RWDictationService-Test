package com.realwear.rw_dictationservice

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionService
import android.util.Log
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognitionEventArgs
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Custom subclass of RecognitionService to handle dictation.
 */
class DictationService : RecognitionService() {
    private val sExecutorService = Executors.newCachedThreadPool()

    override fun onStartListening(intent: Intent, callback: Callback) {
        val speechConfig =
            SpeechConfig.fromSubscription(SpeechSubscriptionKey, SpeechRegion)
        val audioInput = AudioConfig.fromDefaultMicrophoneInput()
        val speechRecognizer = SpeechRecognizer(speechConfig, audioInput)
        val task: Future<SpeechRecognitionResult> = speechRecognizer.recognizeOnceAsync()
        callback.readyForSpeech(Bundle())

        speechRecognizer.recognizing.addEventListener {
                _: Any?, speechRecognitionResultEventArgs: SpeechRecognitionEventArgs ->
            val text = speechRecognitionResultEventArgs.result.text
            val bundle = Bundle()
            val arrayList = ArrayList<String>()
            arrayList.add(text)
            bundle.putStringArrayList(
                android.speech.SpeechRecognizer.RESULTS_RECOGNITION,
                arrayList
            )
            callback.partialResults(bundle)
        }

        setOnTaskCompletedListener(task,
            object : OnTaskCompletedListener<SpeechRecognitionResult> {
                override fun onCompleted(taskResult: SpeechRecognitionResult) {
                    callback.endOfSpeech()

                    val text = taskResult.text
                    val bundle = Bundle()
                    val arrayList = ArrayList<String>()
                    arrayList.add(text)
                    bundle.putStringArrayList(
                        android.speech.SpeechRecognizer.RESULTS_RECOGNITION,
                        arrayList
                    )
                    callback.results(bundle)
                }
            })
    }

    override fun onStopListening(p0: Callback?) {
        Log.d(TAG, "onStopListening called.")
    }

    override fun onCancel(p0: Callback?) {
        Log.d(TAG, "onCancel called.")
    }

    /**
     * Method to set an OnTaskCompletedListener on a Future task.
     */
    private fun <T> setOnTaskCompletedListener(
        task: Future<T>,
        listener: OnTaskCompletedListener<T>
    ) {
        sExecutorService.submit(
            Callable<Any?> {
                val result = task.get()
                listener.onCompleted(result)
                null
            })
    }

    /**
     * A custom listener for task result.
     */
    private interface OnTaskCompletedListener<T> {
        fun onCompleted(taskResult: T)
    }

    companion object {
        private val TAG = DictationService::class.java.simpleName
        private const val SpeechSubscriptionKey = "226314cc4670432e87d9f80f805bffea"
        private const val SpeechRegion = "westus2"
    }
}
