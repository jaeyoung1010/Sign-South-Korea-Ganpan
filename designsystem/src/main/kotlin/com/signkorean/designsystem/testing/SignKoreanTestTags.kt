package com.signkorean.designsystem.testing

object SignKoreanTestTags {
    const val LEARN_CARD_STACK = "learn.card.stack"
    const val LEARN_ACTIVE_CARD = "learn.card.active"
    const val LEARN_PHOTO = "learn.card.photo"
    const val LEARN_FOCUS_FRAME = "learn.card.focus-frame"
    const val LEARN_REVEAL_ANSWER = "learn.answer.reveal"
    const val LEARN_QUESTION = "learn.card.question"
    const val LEARN_ANSWER = "learn.card.answer"
    const val LEARN_INFO_PANEL = "learn.card.info"
    const val LEARN_DRAG_OVERLAY = "learn.card.drag-overlay"
    const val LEARN_KEYBOARD_STAGE = "learn.card.keyboard-stage"
    const val LEARN_PLAY_AUDIO = "learn.audio.play"
    const val LEARN_ZOOM_IMAGE = "learn.image.zoom"
    const val LEARN_IMAGE_VIEWER = "learn.image.viewer"
    const val LEARN_IMAGE_VIEWER_CLOSE = "learn.image.viewer.close"
    const val LEARN_CLOSE_SESSION = "learn.session.close"
    const val GLOBAL_FEEDBACK = "global.feedback"
    const val LEARN_RATING_TOAST = "learn.rating.toast"
    const val LEARN_UNDO_RATING = "learn.rating.undo"
    const val LEARN_KEEP_LEARNING = "learn.session.keep-learning"
    const val LEARN_LEAVE_FOR_NOW = "learn.session.leave-for-now"
    const val SETTINGS_VOICE = "settings.voice"
    const val DONE_REVIEW_CARDS = "done.review-cards"
    const val DONE_BACK_HOME = "done.back-home"

    fun learnCard(cardId: String): String = "learn.card.$cardId"
}
