package no.nav.pia.sykefravarsstatistikk.domene

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AltinnOrganisasjon(
    @SerialName("Name")
    val name: String,
    @SerialName("OrganizationNumber")
    val organizationNumber: String,
    @SerialName("OrganizationForm")
    val organizationForm: String,
    @SerialName("ParentOrganizationNumber")
    val parentOrganizationNumber: String,
)
