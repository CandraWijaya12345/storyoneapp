package com.dicoding.storyoneapp

import android.content.Context
import android.graphics.Canvas
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatEditText
import com.dicoding.storyoneapp.R

class MyEditText @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs) {

    init {
        // Set input type to password
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        // Add text watcher to validate password length
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validatePasswordLength()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Validate password length as soon as the view is initialized
        validatePasswordLength()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        hint = context.getString(R.string.password_hint)
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
    }

    private fun validatePasswordLength() {
        // Check if the password length is at least 8 characters
        val isPasswordValid = (text?.length ?: 0) >= 8

        // Show error message if the password is invalid
        if (!isPasswordValid) {
            error = context.getString(R.string.password_error)

            // Add bottom padding to avoid error message overlapping the text field
            setPadding(paddingLeft, paddingTop, paddingRight, 40) // Adjust bottom padding
        } else {
            error = null

            // Reset bottom padding if no error is present
            setPadding(paddingLeft, paddingTop, paddingRight, 0)
        }

        // Find the parent view and enable/disable the button based on validity
        (parent as? View)?.findViewById<MyButton>(R.id.my_button)?.isEnabled = isPasswordValid
    }
}
