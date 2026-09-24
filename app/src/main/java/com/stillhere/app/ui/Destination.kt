package com.stillhere.app.ui

/**
 * The three sections the web has, so both products share one mental model.
 * Which of them a build shows is its flavor's call — see `Features.destinations`.
 */
enum class Destination(val label: String) {
    Record("Record live"),
    Watch("Auto-record"),
    Save("Save post"),
}
