package com.appacoustic.cointester.libFramework.extension

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

fun EditText.setOnTextChangedListener(onTextChanged: (charSequence: CharSequence?) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(
            charSequence: CharSequence?,
            start: Int,
            count: Int,
            after: Int
        ) {
        }

        override fun onTextChanged(
            charSequence: CharSequence?,
            start: Int,
            before: Int,
            count: Int
        ) {
            onTextChanged(charSequence)
        }

        override fun afterTextChanged(editable: Editable?) {}
    })
}
