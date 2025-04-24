package no.nav.pia.sykefravarsstatistikk.api.auditlog

import io.ktor.http.HttpMethod
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.receiveText
import io.ktor.server.request.uri
import io.ktor.util.toMap
import no.nav.pia.sykefravarsstatistikk.Clusters
import no.nav.pia.sykefravarsstatistikk.Systemmiljø
import org.slf4j.LoggerFactory
import java.util.UUID

@Suppress("ktlint:standard:enum-entry-name-case")
enum class AuditType {
    access,
    update,
    create,
    delete,
}

private fun HttpMethod.tilAuditType(): AuditType =
    when (this) {
        HttpMethod.Get -> AuditType.access
        HttpMethod.Post -> AuditType.create
        HttpMethod.Put -> AuditType.update
        HttpMethod.Delete -> AuditType.delete
        else -> AuditType.access
    }

enum class Tillat(
    val tillat: String,
) {
    Ja("Permit"),
    Nei("Deny"),
}

private val auditLog = LoggerFactory.getLogger("auditLogger")
private val applikasjonsLog = LoggerFactory.getLogger("applikasjonsLogger")
const val APP_IDENTIFIKATOR = "pia-sykefravarsstatistikk"

fun ApplicationCall.auditLogVedUkjentOrgnummer(
    fnr: String,
) {
    this.auditLog(
        fnr = fnr,
        tillat = Tillat.Nei,
        beskrivelse = "finner ikke organisjasjonsnummeret i requesten fra bruker $fnr",
    )
}

fun ApplicationCall.auditLogVedUgyldigOrgnummer(
    fnr: String,
    orgnr: String?,
) {
    this.auditLog(
        fnr = fnr,
        tillat = Tillat.Nei,
        beskrivelse = "ugyldig organisjasjonsnummer $orgnr i requesten fra bruker $fnr",
    )
}

fun ApplicationCall.auditLogVedIkkeTilgangTilOrg(
    fnr: String,
    orgnr: String,
) {
    this.auditLog(
        fnr = fnr,
        orgnummer = orgnr,
        tillat = Tillat.Nei,
        beskrivelse = "$fnr har ikke tilgang til organisasjonsnummer $orgnr",
    )
}

suspend fun ApplicationCall.auditLogVedOkKall(
    fnr: String,
    orgnr: String,
) {
    this.auditLog(
        fnr = fnr,
        orgnummer = orgnr,
        tillat = Tillat.Ja,
        beskrivelse = "$fnr har utført følgende kall mot organisajonsnummer $orgnr " +
            "path: ${this.request.path()} " +
            "arg: ${this.request.queryParameters.toMap()} " +
            "body: ${this.receiveText()}",
    )
}

private fun ApplicationCall.auditLog(
    fnr: String,
    orgnummer: String? = null,
    tillat: Tillat,
    beskrivelse: String,
) {
    val auditType = this.request.httpMethod.tilAuditType()
    val method = this.request.httpMethod.value
    val uri = this.request.uri
    val severity = if (orgnummer.isNullOrEmpty()) "WARN" else "INFO"
    val logstring =
        "CEF:0|$APP_IDENTIFIKATOR|auditLog|1.0|audit:${auditType.name}|Sporingslogg|$severity|end=${System.currentTimeMillis()} " +
            "suid=$fnr " +
            (orgnummer?.let { "duid=$it " } ?: "") +
            "sproc=${UUID.randomUUID()} " +
            "requestMethod=$method " +
            "request=${
                uri.substring(
                    0,
                    uri.length.coerceAtMost(70),
                )
            } " +
            "flexString1Label=Decision " +
            "flexString1=${tillat.tillat} " +
            "msg=$beskrivelse "

    when (Systemmiljø.cluster) {
        Clusters.PROD_GCP.clusterId -> auditLog.info(logstring)
        Clusters.DEV_GCP.clusterId -> auditLog.info(logstring)
        Clusters.LOKAL.clusterId -> applikasjonsLog.info(logstring)
    }
}
