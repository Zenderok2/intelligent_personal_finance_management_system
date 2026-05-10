package com.example.project5.utils

import android.view.View

fun View.clickWithScale(action: () -> Unit) {
    val anim = this.animate()

    anim.scaleX(0.9f)
        .scaleY(0.9f)
        .apply { duration = 80 }
        .withEndAction {
            anim.scaleX(1f)
                .scaleY(1f)
                .apply { duration = 80 }
        }

    action()
}