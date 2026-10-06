package org.fossify.phone.classic.models

sealed class Events {
    data object RefreshCallLog : Events()
}
