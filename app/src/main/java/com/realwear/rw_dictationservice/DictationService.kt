package com.realwear.rw_dictationservice

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionService
import android.util.Log
import com.microsoft.cognitiveservices.speech.*
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

class DictationService : RecognitionService() {
    private val TAG = DictationService::class.java.simpleName
    private lateinit var mCallback: Callback
    private val SpeechSubscriptionKey = "226314cc4670432e87d9f80f805bffea"
    private val SpeechRegion = "westus2"
    private val s_executorService = Executors.newCachedThreadPool()

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Jonathan Service started")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Jonathan Service stopped")
    }

    override fun onStartListening(intent: Intent, callback: Callback) {
        Log.d(TAG, "jonathan start listening")

        mCallback = callback
        val extras = intent.extras ?: Bundle()
        mCallback.readyForSpeech(extras)

        startDictation()
    }

    private fun startDictation() {
        val speechConfig =
            SpeechConfig.fromSubscription(SpeechSubscriptionKey, SpeechRegion)

        val audioInput = AudioConfig.fromDefaultMicrophoneInput()

        val speechRecognizer = SpeechRecognizer(speechConfig, audioInput)

        val task: Future<SpeechRecognitionResult> = speechRecognizer.recognizeOnceAsync()
        setOnTaskCompletedListener(task,
            object : OnTaskCompletedListener<SpeechRecognitionResult> {
                override fun onCompleted(taskResult: SpeechRecognitionResult) {
                    val text = taskResult.text
                    Log.d(TAG, "jonathan results: " + text)
                    speechRecognizer.close()
                    onStopListening(mCallback)

                    val bundle = Bundle()
                    val arrayList = ArrayList<String>()
                    arrayList.add(text)
                    bundle.putStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION, arrayList)
                    mCallback.results(bundle)
                }
            })
    }

    override fun onStopListening(callback: Callback) {
        Log.d(TAG, "jonathan stop listening")
    }

    override fun onCancel(p0: Callback?) {
        Log.d(TAG, "Jonathan recog Service Callback Canceled.")
    }

    private fun <T> setOnTaskCompletedListener(
        task: Future<T>,
        listener: OnTaskCompletedListener<T>
    ) {
        s_executorService.submit(
            Callable<Any?> {
                val result = task.get()
                listener.onCompleted(result)
                null
            })
    }

    private interface OnTaskCompletedListener<T> {
        fun onCompleted(taskResult: T)
    }

}
