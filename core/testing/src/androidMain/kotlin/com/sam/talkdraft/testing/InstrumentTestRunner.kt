package com.sam.talkdraft.testing

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class InstrumentTestRunner : AndroidJUnitRunner() {

    override fun newApplication(classLoader: ClassLoader?, className: String?, context: Context?): Application? {
        return super.newApplication(classLoader, TalkDraftTestApplication::class.java.name, context)
    }
}
