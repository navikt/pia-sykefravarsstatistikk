package no.nav.pia.sykefravarsstatistikk.api.auth

import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.AltinnrettigheterProxyKlient
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.model.AltinnReportee
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.model.ServiceCode
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.model.ServiceEdition
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.model.Subject
import no.nav.arbeidsgiver.altinnrettigheter.proxy.klient.model.TokenXToken
import no.nav.pia.sykefravarsstatistikk.Systemmiljø

fun AltinnrettigheterProxyKlient.hentTilknyttedeVirksomheter(
    token: String,
    subject: String,
): List<AltinnReportee> =
    hentOrganisasjoner(
        selvbetjeningToken = TokenXToken(value = token),
        subject = Subject(subject),
        filtrerPåAktiveOrganisasjoner = true,
    )

fun AltinnrettigheterProxyKlient.hentEnkelrettighetVirksomheter(
    token: String,
    subject: String,
): List<AltinnReportee> =
    hentOrganisasjoner(
        selvbetjeningToken = TokenXToken(value = token),
        subject = Subject(subject),
        serviceCode = ServiceCode(Systemmiljø.serviceCodeSykefraværsstatistikk),
        serviceEdition = ServiceEdition(Systemmiljø.serviceEditionSykefraværsstatistikk),
        filtrerPåAktiveOrganisasjoner = true,
    )
