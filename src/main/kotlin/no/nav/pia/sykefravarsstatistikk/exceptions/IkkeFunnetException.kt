package no.nav.pia.sykefravarsstatistikk.exceptions

sealed class IkkeFunnetException(
    id: String,
    type: String,
) : Exception("$type med id $id ikke funnet")
