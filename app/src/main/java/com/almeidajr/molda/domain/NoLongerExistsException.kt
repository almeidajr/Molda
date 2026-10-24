package com.almeidajr.molda.domain

/**
 * A command or load reached a stored row, such as a profile or a project, that no longer exists:
 * it was deleted meanwhile. Storage expects it, and tells it apart from other storage failures.
 */
class NoLongerExistsException(message: String) : Exception(message)
