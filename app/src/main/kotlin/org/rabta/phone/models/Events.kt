package org.rabta.phone.classic.models

sealed class Events {
    data object RefreshCallLog : Events()
}
