package com.coachroebuck.uploadexample

import android.app.Activity
import android.content.Context
import android.content.Intent

class MyPresenter(private val interactor: MyInteractor = MyInteractor()) {
    fun onActivityResult(context: Context, requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            interactor.upload(
                cr = context.contentResolver,
                data = data
            )
        }
    }
}
