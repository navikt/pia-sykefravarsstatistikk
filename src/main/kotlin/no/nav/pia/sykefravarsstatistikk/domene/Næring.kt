package no.nav.pia.sykefravarsstatistikk.domene

// TODO: Flytt til ia-felles
class Næring(
    val tosifferIdentifikator: String,
) {
    val navn: String = næringer[tosifferIdentifikator]
        ?: throw IllegalArgumentException("Fant ingen næring med identifikator '$tosifferIdentifikator'.")
}

private val næringer = mapOf(
    "00" to "Ukjent næringskode",
    "01" to "Jordbruk og tjenester tilknyttet jordbruk, jakt og viltstell",
    "02" to "Skogbruk og tjenester tilknyttet skogbruk",
    "03" to "Fiske, fangst og akvakultur",
    "05" to "Bryting av steinkull og brunkull",
    "06" to "Utvinning av råolje og naturgass",
    "07" to "Bryting av metallholdig malm",
    "08" to "Bryting og bergverksdrift ellers",
    "09" to "Tjenester tilknyttet bergverksdrift og utvinning",
    "10" to "Produksjon av nærings- og nytelsesmidler",
    "11" to "Produksjon av drikkevarer",
    "12" to "Produksjon av tobakksvarer",
    "13" to "Produksjon av tekstiler",
    "14" to "Produksjon av klær",
    "15" to "Produksjon av lær og lærvarer",
    "16" to "Produksjon av trelast og varer av tre, kork, strå og flettematerialer, unntatt møbler",
    "17" to "Produksjon av papir og papirvarer",
    "18" to "Trykking og reproduksjon av innspilte opptak",
    "19" to "Produksjon av kull- og raffinerte petroleumsprodukter",
    "20" to "Produksjon av kjemikalier og kjemiske produkter",
    "21" to "Produksjon av farmasøytiske råvarer og preparater",
    "22" to "Produksjon av gummi- og plastprodukter",
    "23" to "Produksjon av andre ikke-metallholdige mineralprodukter",
    "24" to "Produksjon av metaller",
    "25" to "Produksjon av metallvarer, unntatt maskiner og utstyr",
    "26" to "Produksjon av datamaskiner og elektroniske og optiske produkter",
    "27" to "Produksjon av elektrisk utstyr",
    "28" to "Produksjon av maskiner og utstyr til generell bruk, ikke nevnt annet sted",
    "29" to "Produksjon av motorvogner og tilhengere",
    "30" to "Produksjon av andre transportmidler",
    "31" to "Produksjon av møbler",
    "32" to "Annen industriproduksjon",
    "33" to "Reparasjon og installasjon av maskiner og utstyr",
    "35" to "Elektrisitets-, gass-, damp- og varmtvannsforsyning",
    "36" to "Uttak fra kilde, rensing og distribusjon av vann",
    "37" to "Oppsamling og behandling av avløpsvann",
    "38" to "Innsamling, behandling, disponering og gjenvinning av avfall",
    "39" to "Miljørydding, miljørensing og lignende virksomhet",
    "41" to "Oppføring av bygninger",
    "42" to "Anleggsvirksomhet",
    "43" to "Spesialisert bygge- og anleggsvirksomhet",
    "45" to "Handel med og reparasjon av motorvogner",
    "46" to "Agentur- og engroshandel, unntatt med motorvogner",
    "47" to "Detaljhandel, unntatt med motorvogner",
    "49" to "Landtransport og rørtransport",
    "50" to "Sjøfart",
    "51" to "Lufttransport",
    "52" to "Lagring og andre tjenester tilknyttet transport",
    "53" to "Post og distribusjonsvirksomhet",
    "55" to "Overnattingsvirksomhet",
    "56" to "Serveringsvirksomhet",
    "58" to "Forlagsvirksomhet",
    "59" to "Film-, video- og fjernsynsprogramproduksjon, utgivelse av musikk- og lydopptak",
    "60" to "Radio- og fjernsynskringkasting",
    "61" to "Telekommunikasjon",
    "62" to "Tjenester tilknyttet informasjonsteknologi",
    "63" to "Informasjonstjenester",
    "64" to "Finansieringsvirksomhet",
    "65" to "Forsikringsvirksomhet og pensjonskasser, unntatt trygdeordninger underlagt offentlig forvaltning",
    "66" to "Tjenester tilknyttet finansierings- og forsikringsvirksomhet",
    "68" to "Omsetning og drift av fast eiendom",
    "69" to "Juridisk og regnskapsmessig tjenesteyting",
    "70" to "Hovedkontortjenester, administrativ rådgivning",
    "71" to "Arkitektvirksomhet og teknisk konsulentvirksomhet, og teknisk prøving og analyse",
    "72" to "Forskning og utviklingsarbeid",
    "73" to "Annonse- og reklamevirksomhet og markedsundersøkelser",
    "74" to "Annen faglig, vitenskapelig og teknisk virksomhet",
    "75" to "Veterinærtjenester",
    "77" to "Utleie- og leasingvirksomhet",
    "78" to "Arbeidskrafttjenester",
    "79" to "Reisebyrå- og reisearrangørvirksomhet og tilknyttede tjenester",
    "80" to "Vakttjeneste og etterforsking",
    "81" to "Tjenester tilknyttet eiendomsdrift",
    "82" to "Annen forretningsmessig tjenesteyting",
    "84" to "Offentlig administrasjon og forsvar, og trygdeordninger underlagt offentlig forvaltning",
    "85" to "Undervisning",
    "86" to "Helsetjenester",
    "87" to "Pleie- og omsorgstjenester i institusjon",
    "88" to "Sosiale omsorgstjenester uten botilbud",
    "90" to "Kunstnerisk virksomhet og underholdningsvirksomhet",
    "91" to "Drift av biblioteker, arkiver, museer og annen kulturvirksomhet",
    "92" to "Lotteri og totalisatorspill",
    "93" to "Sports- og fritidsaktiviteter og drift av fornøyelsesetablissementer",
    "94" to "Aktiviteter i medlemsorganisasjoner",
    "95" to "Reparasjon av datamaskiner, husholdningsvarer og varer til personlig bruk",
    "96" to "Annen personlig tjenesteyting",
    "97" to "Lønnet arbeid i private husholdninger",
    "99" to "Internasjonale organisasjoner og organer",
)
