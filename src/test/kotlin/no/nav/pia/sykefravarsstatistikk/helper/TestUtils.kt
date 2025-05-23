package no.nav.pia.sykefravarsstatistikk.helper

import io.kotest.inspectors.forExactly

inline fun <T, C : Collection<T>> C.forExactlyOne(fn: (T) -> Unit): C = this.forExactly(1, fn)
